package com.financialanalysis.externalapi;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.Symbol;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class QuestradeApi {

    public void authenticate() {

    }

    public List<Symbol> symbolSearch(String prefix) {
        return null;
    }

    public void markets() {

    }

    public List<StockFA> marketsCandles(List<Symbol> symbols) {
        return null;
    }

    // HTTP GET request
//    private void sendGet() throws Exception {
//
//        String url = "http://www.google.com/search?q=mkyong";
//
//        URL obj = new URL(url);
//        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
//
//        // optional default is GET
//        con.setRequestMethod("GET");
//
//        //add request header
//        con.setRequestProperty("User-Agent", USER_AGENT);
//
//        int responseCode = con.getResponseCode();
//        System.out.println("\nSending 'GET' request to URL : " + url);
//        System.out.println("Response Code : " + responseCode);
//
//        BufferedReader in = new BufferedReader(
//                new InputStreamReader(con.getInputStream()));
//        String inputLine;
//        StringBuffer response = new StringBuffer();
//
//        while ((inputLine = in.readLine()) != null) {
//            response.append(inputLine);
//        }
//        in.close();
//
//        //print result
//        System.out.println(response.toString());
//    }
}
