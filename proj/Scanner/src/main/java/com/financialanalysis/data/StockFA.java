package com.financialanalysis.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.List;

@Data
public class StockFA {
    private final String symbol;
    private final String name;
    private final String currency;
    private final String stockExchange;
    private final List<StockPrice> history;

    public String getJson() {
        Gson gson = new GsonBuilder().registerTypeAdapter(StockPrice.class, new StockPriceSerializer()).create();
        return gson.toJson(this);
    }

    public DateTime getMostRecentDate() {
        return history.get(history.size()-1).getDate();
    }
}
