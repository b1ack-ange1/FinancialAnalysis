package com.financialanalysis.data;

import com.google.gson.JsonObject;
import lombok.Data;
import org.joda.time.DateTime;

@Data
public class StockPrice {
    private final DateTime date;
    private final double open;
    private final double low;
    private final double high;
    private final double close;
    private final double volume;

    public String getJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("date", date.toString());
        jsonObject.addProperty("open", open);
        jsonObject.addProperty("low", low);
        jsonObject.addProperty("high", high);
        jsonObject.addProperty("close", close);
        jsonObject.addProperty("volume", volume);
        return jsonObject.getAsString();
    }
}
