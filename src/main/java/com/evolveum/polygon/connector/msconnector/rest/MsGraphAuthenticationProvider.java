package com.evolveum.polygon.connector.msconnector.rest;

import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.options.HeaderOption;

public final class MsGraphAuthenticationProvider implements IAuthenticationProvider {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String OAUTH_BEARER_PREFIX = "Bearer ";
    private final MsGraphAccessTokenProvider accessToken;

    private MsGraphAuthenticationProvider(MsGraphAccessTokenProvider accessToken) {
        this.accessToken = accessToken;
    }

    public static MsGraphAuthenticationProvider from(MsGraphAccessTokenProvider accessToken) {
        return new MsGraphAuthenticationProvider(accessToken);
    }

    @Override
    public void authenticateRequest(IHttpRequest request) {
        // If the request already has an authorization header, do not intercept it.
        for (final HeaderOption option : request.getHeaders()) {
            if (option.getName().equals(AUTHORIZATION_HEADER_NAME)) {
                // Found an existing authorization header so don't add another
                return;
            }
        }
        try {
            final String token = accessToken.get();
            request.addHeader(AUTHORIZATION_HEADER_NAME, OAUTH_BEARER_PREFIX + token);
        } catch (ClientException e) {
            final String message = "Unable to authenticate request, No active account found";
            throw new ClientException(message, e);
        }
    }

}