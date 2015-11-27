package com.financialanalysis.questrade;

import lombok.Data;

@Data
public class AuthenticationTokens {
    private final String access_token;
    private final String api_server;
    private final String expires_in;
    private final String refresh_token;
    private final String token_type;
}
