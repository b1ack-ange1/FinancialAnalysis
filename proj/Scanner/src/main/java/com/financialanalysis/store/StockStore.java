package com.financialanalysis.store;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.data.StockPriceDeserializer;
import com.financialanalysis.data.StockPriceSerializer;
import com.financialanalysis.data.Symbol;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j
public class StockStore {
    static {
        createStockStore();
    }

    @SneakyThrows
    private static void createStockStore() {
        Path path = Paths.get(getStockStoreDir());
        Files.createDirectories(path);
    }

    private static String getStockStoreDir() {
        return "var/stocks/";
    }

    @SneakyThrows
    public Map<Symbol, StockFA> load(List<Symbol> symbols) {
        Gson gson = new GsonBuilder().registerTypeAdapter(StockPrice.class, new StockPriceDeserializer()).create();
//        Type listType = new TypeToken<ArrayList<StockFA>>() {}.getType();
        Map<Symbol, StockFA> map = new HashMap<>();

        for(Symbol symbol : symbols) {
            File stockFile = new File(getStockStoreDir() + symbol.getSymbol());
            if(!stockFile.exists()) continue;

            String json = FileUtils.readFileToString(stockFile);
            try {
                StockFA stock = gson.fromJson(json, StockFA.class);
                map.put(symbol, stock);
            } catch (Exception e) {
                log.error("Malformed json: " + symbol);
            }
        }

        return map;
    }

    @SneakyThrows
    public void store(Map<Symbol, StockFA> stocks) {
        Gson gson = new GsonBuilder().registerTypeAdapter(StockPrice.class, new StockPriceSerializer()).create();
        Set<Symbol> symbols = stocks.keySet();

        for(Symbol symbol : symbols) {
//            log.info("Storing: " + symbol);
            String json = gson.toJson(stocks.get(symbol));
            FileUtils.writeStringToFile(new File(getStockStoreDir() + symbol.getSymbol()), json);
        }
    }

    public void delete(List<Symbol> symbols) {
        for(Symbol symbol : symbols) {
            File stockFile = new File(getStockStoreDir() + symbol.getSymbol());
            if(!stockFile.exists()) continue;
            stockFile.delete();
        }
    }

    public int size() {
        return new File(getStockStoreDir()).listFiles().length;
    }
}
