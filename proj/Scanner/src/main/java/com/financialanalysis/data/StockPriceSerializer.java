package com.financialanalysis.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class StockPriceSerializer implements JsonSerializer<StockPrice> {
    @Override
    public JsonElement serialize(StockPrice stockPrice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("date", stockPrice.getDate().toString());
        jsonObject.addProperty("open", stockPrice.getOpen());
        jsonObject.addProperty("low", stockPrice.getLow());
        jsonObject.addProperty("high", stockPrice.getHigh());
        jsonObject.addProperty("close", stockPrice.getClose());
        jsonObject.addProperty("volume", stockPrice.getVolume());
        return jsonObject;
    }
}
