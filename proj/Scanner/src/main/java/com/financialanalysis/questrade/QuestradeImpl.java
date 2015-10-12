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
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j
@Singleton
public class QuestradeImpl implements Questrade {
    private static String AUTH_URL = "https://login.questrade.com/oauth2/token";
    private static String REFRESH_TOKEN = "DEFAULT";

    private static AuthenticationTokens authTokens;
    private static boolean authenticated = false;

    static {
        createAuthStore();
        authenticate();
    }

    @SneakyThrows
    private static void createAuthStore() {
        Path path = Paths.get(getAuthStoreDir());
        Files.createDirectories(path);
    }

    private static String getAuthStoreDir() {
        return "var/auth/";
    }

    /**
     * Must authenticate before making any calls
     */
    @SneakyThrows
    private static void authenticate() {
        Gson gson = new Gson();
        File credFile = new File(getAuthStoreDir() + "cred");
        String refreshToken = REFRESH_TOKEN;

        // If file exists, use the existing refresh token
        if(credFile.exists()) {
            log.info("Found credentials");
            String json = FileUtils.readFileToString(credFile);
            AuthenticationTokens authenticationTokens = gson.fromJson(json, AuthenticationTokens.class);
            refreshToken = authenticationTokens.getRefresh_token();
        }

        URL url = new URL(AUTH_URL);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        byte[] urlParams = encodeURLParams(params).getBytes("UTF-8");

        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Length", String.valueOf(urlParams.length));
        con.setDoOutput(true);
        con.getOutputStream().write(urlParams);

        Reader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
        StringBuffer buf = new StringBuffer();
        for (int c = in.read(); c != -1; c = in.read()) {
            buf.append((char) c);
        }
        String json = buf.toString();

        authTokens = gson.fromJson(json, AuthenticationTokens.class);
        authenticated = true;
        FileUtils.writeStringToFile(credFile, json);
    }

    public SymbolsSearchResponse symbolSearch(String prefix) throws Exception {
        if(!authenticated) throw new Exception("Not Authenticated.");

        String api = "v1/symbols/search";
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("prefix", prefix);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", authTokens.getToken_type() + " " + authTokens.getAccess_token());

        String json = performGet(api, params, headers);

        SymbolsSearchResponse response = new Gson().fromJson(json, SymbolsSearchResponse.class);
        return response;
    }

    public SymbolsIdResponse getSymbolsId(List<String> ids) throws Exception {
        if(!authenticated) throw new Exception("Not Authenticated.");

        String api = "v1/symbols";
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("ids", ids);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", authTokens.getToken_type() + " " + authTokens.getAccess_token());

        String json = performGet(api, params, headers);
        SymbolsIdResponse response = new Gson().fromJson(json, SymbolsIdResponse.class);

        return response;
    }

    public void markets() throws Exception {
        if(!authenticated) throw new Exception("Not Authenticated.");

    }

    public MarketCandlesResponse getMarketCandles(Symbol symbol, DateTime start, DateTime end, HistoricDataGranularity interval) throws Exception {
        if(!authenticated) throw new Exception("Not Authenticated.");

        end = end.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

        String api = "v1/markets/candles/" + symbol.getSymbolId();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("startTime", fmt.print(start));
        params.put("endTime", fmt.print(end));
        params.put("interval", interval.toString());
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", authTokens.getToken_type() + " " + authTokens.getAccess_token());

        String json = performGet(api, params, headers);
        MarketCandlesResponse response = new Gson().fromJson(json, MarketCandlesResponse.class);

        return response;
    }

    private String performGet(String apiUrl, Map<String, Object> urlParams, Map<String, String> headers) throws IOException {
        String urlParamsEncoded = encodeURLParams(urlParams);
        URL url = new URL(authTokens.getApi_server() + apiUrl + "?" + urlParamsEncoded);

        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        Collection<String> headerKeys = headers.keySet();
        for(String key : headerKeys) {
            con.setRequestProperty(key, headers.get(key));
        }

        Reader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
        String json = readBuffer(in);

        return json;
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

    @SneakyThrows
    private String readBuffer(Reader in) {
        StringBuffer buf = new StringBuffer();
        for (int c = in.read(); c != -1; c = in.read()) {
            buf.append((char) c);
        }
        return buf.toString();
    }
}
