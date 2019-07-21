/*
 * @author Dave Moten
 *
 */

package com.evolveum.polygon.connector.msconnector.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

//ThreadSafe
public final class MsGraphAccessTokenProvider {

    private static final int OK = 200;
    private static final String POST = "POST";
    private static final String APPLICATION_JSON = "application/json";
    private static final String REQUEST_HEADER = "Accept";
    private static final String SCOPE_MS_GRAPH_DEFAULT = "https://graph.microsoft.com/.default";
    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    private static final String RESOURCE_MS_GRAPH = "https://graph.microsoft.com/";
    private static final String OAUTH2_TOKEN_URL_PREFIX = "https://login.windows.net/";
    private static final String OAUTH2_TOKEN_URL_SUFFIX = "/oauth2/token";

    private static final String PARAMETER_SCOPE = "scope";
    private static final String PARAMETER_CLIENT_SECRET = "client_secret";
    private static final String PARAMETER_GRANT_TYPE = "grant_type";
    private static final String PARAMETER_CLIENT_ID = "client_id";
    private static final String PARAMETER_RESOURCE = "resource";

    private final String tenantName;
    private final String clientId;
    private final String clientSecret;
    private final long refreshBeforeExpiryMs;

    private long expiryTime;
    private String accessToken;

    private MsGraphAccessTokenProvider(String tenantName, String clientId, String clientSecret,
                                       long refreshBeforeExpiryMs) {

        this.tenantName = tenantName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshBeforeExpiryMs = refreshBeforeExpiryMs;
    }

    public static Builder tenantName(String tenantName) {
        return new Builder(tenantName);
    }

    public synchronized String get() {
        long now = System.currentTimeMillis();
        if (accessToken != null && now < expiryTime - refreshBeforeExpiryMs) {
            return accessToken;
        } else {
            return refreshAccessToken();
        }
    }

    private String refreshAccessToken() {
        try {
            URL url = new URL(OAUTH2_TOKEN_URL_PREFIX + tenantName + OAUTH2_TOKEN_URL_SUFFIX);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod(POST);
            con.setRequestProperty(REQUEST_HEADER, APPLICATION_JSON);
            StringBuilder params = new StringBuilder();
            add(params, PARAMETER_RESOURCE, RESOURCE_MS_GRAPH);
            add(params, PARAMETER_CLIENT_ID, clientId);
            add(params, PARAMETER_GRANT_TYPE, GRANT_TYPE_CLIENT_CREDENTIALS);
            add(params, PARAMETER_CLIENT_SECRET, clientSecret);
            add(params, PARAMETER_SCOPE, SCOPE_MS_GRAPH_DEFAULT);
            con.setDoOutput(true);
            try (DataOutputStream dos = new DataOutputStream(con.getOutputStream())) {
                dos.writeBytes(params.toString());
            }
            int responseCode = con.getResponseCode();

            //?
            String json = IOUtils.toString(con.getInputStream());

            if (responseCode != OK) {
                throw new RuntimeException("Response code=" + responseCode + ", output=" + json);
            } else {
                JsonObject o = new JsonParser().parse(json).getAsJsonObject();
                // update the cached values
                expiryTime = o.get("expires_on").getAsLong() * 1000;
                accessToken = o.get("access_token").getAsString();
                return accessToken;
            }
        } catch (IOException e) {
            // reset stuff
            expiryTime = 0;
            accessToken = null;
            throw new RuntimeException(e);
        }
    }

    private static void add(StringBuilder params, String key, String value) {
        if (params.length() > 0) {
            params.append("&");
        }
        params.append(key);
        params.append("=");
        try {
            params.append(URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class Builder {
        final String tenantName;
        String clientId;
        String clientSecret;

        // default to refresh access token on every call of get()
        long refreshBeforeExpiryMs = Long.MAX_VALUE;

        Builder(String tenantName) {
            this.tenantName = tenantName;
        }

        public Builder2 clientId(String clientId) {
            this.clientId = clientId;
            return new Builder2(this);
        }

    }

    public static final class Builder2 {
        private final Builder b;

        Builder2(Builder b) {
            this.b = b;
        }

        public Builder3 clientSecret(String clientSecret) {
            b.clientSecret = clientSecret;
            return new Builder3(b);
        }

    }

    public static final class Builder3 {

        private final Builder b;

        Builder3(Builder b) {
            this.b = b;
        }

        /**
         * The access token is returned from AD with an expiry time. If you call
         * {@code get()} within {@code duration} of the expiry time then a refresh of
         * the access token will be performed. If this value is not set then the access
         * token is refreshed on every call of {@code get()}.
         *
         * @param duration
         * @param unit
         * @return
         */
        public Builder3 refreshBeforeExpiry(long duration, TimeUnit unit) {
            b.refreshBeforeExpiryMs = unit.toMillis(duration);
            return this;
        }

        public MsGraphAccessTokenProvider build() {
            return new MsGraphAccessTokenProvider(b.tenantName, b.clientId, b.clientSecret, b.refreshBeforeExpiryMs);
        }
    }

}