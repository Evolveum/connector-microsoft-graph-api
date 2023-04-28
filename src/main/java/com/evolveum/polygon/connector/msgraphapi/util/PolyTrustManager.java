package com.evolveum.polygon.connector.msgraphapi.util;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

import javax.net.ssl.X509TrustManager;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


public class PolyTrustManager implements X509TrustManager {

    protected static final Log LOG = Log.getLog(PolyTrustManager.class);

    final X509TrustManager primary;
    final X509TrustManager secondary;

    public PolyTrustManager(X509TrustManager primary, X509TrustManager secondary) {

        this.primary = primary;
        this.secondary = secondary;

    }

    public PolyTrustManager(X509TrustManager primary) {

        this(primary, null);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {

        return primary.getAcceptedIssuers();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain,
                                   String authType) throws CertificateException {
        try {

            primary.checkServerTrusted(chain, authType);

        } catch (RuntimeException e) {

            LOG.warn("Exception while using jvm defined trust store: {0}. Attempt to use secondary" +
                    "Trust manager if provided to handle exception.", e.getLocalizedMessage());
            if (e.getCause() instanceof InvalidAlgorithmParameterException) {

                if (secondary !=null) {

                    LOG.ok("Fail-over to custom defined thrust store.");

                    secondary.checkServerTrusted(chain, authType);
                } else {

                    LOG.warn("Secondary trust store either missing or configured to be omitted. About to rethrow " +
                            "exception");

                    throw new ConnectorException("InvalidAlgorithmParameterException exception while validating server " +
                            "with CA trust. " +e.getLocalizedMessage());
                }

            } else {

                throw new ConnectorException("Runtime exception while validating server with CA trust. " +
                        ""+e.getLocalizedMessage());
            }

        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain,
                                   String authType) throws CertificateException {

        primary.checkClientTrusted(chain, authType);
    }
}
