package com.financialanalysis.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.joda.time.DateTime;

import java.lang.reflect.Type;

public class StockPriceDeserializer implements JsonDeserializer<StockPrice> {
    @Override
    public StockPrice deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = (JsonObject) jsonElement;

        DateTime date = new DateTime(jsonObject.get("date").getAsString());
        double open = jsonObject.get("open").getAsDouble();
        double low = jsonObject.get("low").getAsDouble();
        double high = jsonObject.get("high").getAsDouble();
        double close = jsonObject.get("close").getAsDouble();
        double volume = jsonObject.get("volume").getAsDouble();

        StockPrice sp = new StockPrice(date, open, low, high, close, volume);
        return sp;
    }
}
