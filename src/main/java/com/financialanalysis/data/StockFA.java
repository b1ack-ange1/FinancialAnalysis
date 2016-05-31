package com.financialanalysis.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

@Data
public class StockFA {
    private final Symbol symbol;
    private final List<StockPrice> history;

    public String getJson() {
        Gson gson = new GsonBuilder().registerTypeAdapter(StockPrice.class, new StockPriceSerializer()).create();
        return gson.toJson(this);
    }

    public DateTime getMostRecentDate() throws Exception {
        if(history.isEmpty()) {
            throw new Exception("No most recent date.");
        }

        return history.get(history.size()-1).getDate();
    }

    public StockFA clone() {
        List<StockPrice> newHistory = new ArrayList<>(history.size());
        for(StockPrice s : history) {
            newHistory.add(s.clone());
        }

        return new StockFA(
                symbol.clone(),
                newHistory
        );
    }
}
