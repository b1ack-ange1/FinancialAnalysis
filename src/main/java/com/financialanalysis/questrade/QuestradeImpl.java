package com.financialanalysis.questrade;

import com.financialanalysis.data.Symbol;
import com.financialanalysis.questrade.response.MarketCandlesResponse;
import com.financialanalysis.questrade.response.SymbolsIdResponse;
import com.financialanalysis.questrade.response.SymbolsSearchResponse;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicHeader;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j
@Singleton
public class QuestradeImpl implements Questrade {
    private static String AUTH_URL = "https://login.questrade.com/oauth2/token";
    private static int CONNECTION_TIMEOUT_MS = 2000;
    private static int SOCKET_TIMEOUT_MS = 2000;
    private static int MAX_TRIES = 3;

    private AuthenticationTokens authTokens;
    private Header authenticationHeader;

    static {
        createAuthStore();
        //authenticate();
    }

    @SneakyThrows
    private static void createAuthStore() {
        Path path = Paths.get(getAuthStoreDir());
        if(!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private static String getAuthStoreDir() {
        return "var/auth/";
    }

    /**
     * Must authenticate before making any calls
     */
    @Override
    @SneakyThrows
    public void authenticate() {
        Gson gson = new Gson();
        File credFile = new File(getAuthStoreDir() + "cred");
        File tokenFile = new File(getAuthStoreDir() + "token");
        String refreshToken;

        // If file exists, use the existing refresh token
        if(credFile.exists()) {
            log.info("Found credentials");
            String json = FileUtils.readFileToString(credFile);
            AuthenticationTokens authenticationTokens = gson.fromJson(json, AuthenticationTokens.class);
            refreshToken = authenticationTokens.getRefresh_token();
        } else {
            log.info("Did not find credential file. Looking for token file.");
            if(tokenFile.exists()) {
                log.info("Found token file");
                refreshToken = FileUtils.readFileToString(tokenFile);
            } else {
                log.error("Did not find authentication token");
                throw new Exception("Did not find authentication token");
            }
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        String urlParamsEncoded = encodeURLParams(params);
        String fullUrl = AUTH_URL + "?" + urlParamsEncoded;

        Request request = Request.Get(fullUrl);
        request.connectTimeout(CONNECTION_TIMEOUT_MS);
        request.socketTimeout(SOCKET_TIMEOUT_MS);
        HttpResponse response = request.execute().returnResponse();
        String json = IOUtils.toString(response.getEntity().getContent(), "UTF-8");

        if(response.getStatusLine().getStatusCode() != 200){
            log.error("Authentication failed " + response.toString());
            throw new Exception("Authentication failed " + response.toString());
        }

        authTokens = gson.fromJson(json, AuthenticationTokens.class);
        authenticationHeader = new BasicHeader(
                "Authorization",
                authTokens.getToken_type() + " " + authTokens.getAccess_token()
        );
        FileUtils.writeStringToFile(credFile, json);
    }

    /**
     * Searchs for all symbols with a prefix
     */
    @Override
    public SymbolsSearchResponse symbolSearch(String prefix) throws Exception {
        String api = "v1/symbols/search";
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("prefix", prefix);

        String json = doGet(api, params, Arrays.asList(authenticationHeader));

        SymbolsSearchResponse response = new Gson().fromJson(json, SymbolsSearchResponse.class);
        return response;
    }

    /**
     * Get the symbol information for a symbol with id in ids
     */
    @Override
    public SymbolsIdResponse getSymbolsId(List<String> ids) throws IOException {
        String api = "v1/symbols";
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("ids", ids);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", authTokens.getToken_type() + " " + authTokens.getAccess_token());

        String json = doGet(api, params, Arrays.asList(authenticationHeader));
        SymbolsIdResponse response = new Gson().fromJson(json, SymbolsIdResponse.class);

        return response;
    }

    /**
     * Gets candles for a symbol
     */
    @Override
    public MarketCandlesResponse getMarketCandles(Symbol symbol,
                                                  DateTime start,
                                                  DateTime end,
                                                  HistoricDataGranularity interval) throws IOException {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

        String api = "v1/markets/candles/" + symbol.getSymbolId();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("startTime", fmt.print(start));
        params.put("endTime", fmt.print(end));
        params.put("interval", interval.toString());

        String json = doGet(api, params, Arrays.asList(authenticationHeader));
        MarketCandlesResponse response = new Gson().fromJson(json, MarketCandlesResponse.class);

        return response;
    }

    private String doGet(String apiUrl, Map<String, Object> urlParams, List<Header> headers) throws IOException {
        String urlParamsEncoded = encodeURLParams(urlParams);
        String fullUrl = authTokens.getApi_server() + apiUrl + "?" + urlParamsEncoded;

        Request request = Request.Get(fullUrl);
        request.connectTimeout(CONNECTION_TIMEOUT_MS);
        request.socketTimeout(SOCKET_TIMEOUT_MS);
        headers.forEach(i -> request.addHeader(i));

        HttpResponse response = performGetWithRetry(request);

        String json = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
        return json;
    }

    private HttpResponse performGetWithRetry(Request request) throws IOException {
        /**
         * Retry policy:
         * - Max tries = 3
         * - Retry only if 500 error is thrown
         * - If max tries is exceeded, propigate the exception upwards
         * - Exponential backoff
         *   5, 25
         */

        HttpResponse response = null;
        for(int attempt = 1; attempt <= MAX_TRIES; attempt++) {
            response = request.execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            String info = String.format("%d %s", statusCode, response.getStatusLine().getReasonPhrase());
            log.info(info);

            if(100 <= statusCode && statusCode < 200) {
                break; // Informational, don't retry
            }

            if(200 <= statusCode && statusCode < 300) {
                return response; // If status return is 200 -> just return
            }

            if(300 <= statusCode && statusCode < 400) {
                break; // Redirection, don't retry
            }

            if(400 <= statusCode && statusCode < 500) {
                log.error(response.toString());
                break; // Client error, don't retry
            }

            if(500 <= statusCode && statusCode < 600) {
                // Server error, RETRY
            }

            if(attempt < MAX_TRIES) {
                int secs = (int) Math.pow(5, attempt);
                log.info("Sleeping for " + secs + " seconds.");
                try {Thread.sleep((long) (secs * 1000));} catch (InterruptedException e) {}
            }
        }

        if(response != null) {
            StatusLine statusLine = response.getStatusLine();
            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
        throw new IOException("Request failed to execute.");
    }

    @SneakyThrows
    private static String encodeURLParams(Map<String, Object> params) {
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');

            if(param.getValue() instanceof List) {
                List<String> list = (List<String>) param.getValue();
                String value = StringUtils.join(list, ",");
                postData.append(value);
            } else {
                postData.append(String.valueOf(param.getValue()));
            }
        }
        return postData.toString();
    }

}