package com.financialanalysis.yahoo;


import yahoofinance.Utils;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class YahooImpl {
//    public List<HistoricalQuote> getResult() throws IOException {
//        ArrayList result = new ArrayList();
//        LinkedHashMap params = new LinkedHashMap();
//        params.put("s", this.symbol);
//        params.put("a", String.valueOf(this.from.get(2)));
//        params.put("b", String.valueOf(this.from.get(5)));
//        params.put("c", String.valueOf(this.from.get(1)));
//        params.put("d", String.valueOf(this.to.get(2)));
//        params.put("e", String.valueOf(this.to.get(5)));
//        params.put("f", String.valueOf(this.to.get(1)));
//        params.put("g", this.interval.getTag());
//        params.put("ignore", ".csv");
//        String url = "http://ichart.yahoo.com/table.csv?" + getURLParameters(params);
//        YahooFinance.logger.log(Level.INFO, "Sending request: " + url);
//        URL request = new URL(url);
//        URLConnection connection = request.openConnection();
//        InputStreamReader is = new InputStreamReader(connection.getInputStream());
//        BufferedReader br = new BufferedReader(is);
//        br.readLine();
//
//        for(String line = br.readLine(); line != null; line = br.readLine()) {
//            YahooFinance.logger.log(Level.INFO, "Parsing CSV line: " + Utils.unescape(line));
//            HistoricalQuote quote = this.parseCSVLine(line);
//            result.add(quote);
//        }
//
//        return result;
//    }
//
//    public static String getURLParameters(Map<String, String> params) {
//        StringBuilder sb = new StringBuilder();
//
//        String key;
//        String value;
//        for(Iterator var2 = params.entrySet().iterator(); var2.hasNext(); sb.append(String.format("%s=%s", new Object[]{key, value}))) {
//            Map.Entry entry = (Map.Entry)var2.next();
//            if(sb.length() > 0) {
//                sb.append("&");
//            }
//
//            key = (String)entry.getKey();
//            value = (String)entry.getValue();
//
//            try {
//                key = URLEncoder.encode(key, "UTF-8");
//                value = URLEncoder.encode(value, "UTF-8");
//            } catch (UnsupportedEncodingException var7) {
//                YahooFinance.logger.log(Level.SEVERE, var7.getMessage(), var7);
//            }
//        }
//
//        return sb.toString();
//    }
}
