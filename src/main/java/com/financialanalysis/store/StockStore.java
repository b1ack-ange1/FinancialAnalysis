package com.financialanalysis.store;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.data.StockPriceDeserializer;
import com.financialanalysis.data.StockPriceSerializer;
import com.financialanalysis.data.Symbol;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FileUtils;
import org.jfree.data.time.Day;
import org.joda.time.DateTime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        if(!Files.exists(path)) {
            Files.createDirectories(path);
        }
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
            String json = gson.toJson(stocks.get(symbol));
            File file = new File(getStockStoreDir() + symbol.getSymbol());
            FileUtils.writeStringToFile(file, json);
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

    @SneakyThrows
    public void cleanStockStore(List<Symbol> allSymbols) {
        log.info("Cleaning stock store");
        List<List<Symbol>> lists = Lists.partition(allSymbols, 100);

        lists.forEach(list -> {
            Map<Symbol, StockFA> stockMap = load(list);
            Collection<StockFA> stocks = stockMap.values();
            stocks.forEach(s -> {
                clean(s);
            });
        });
    }

    private void clean(StockFA stock) {
        // Make sure each day is unique
        Map<Day, StockPrice> map = new LinkedHashMap<>();
        boolean corrupt = false;
        for(StockPrice p : stock.getHistory()) {
            DateTime date = p.getDate();
            Day day = new Day(date.toDate());

            if(!map.containsKey(day)) {
                map.put(day, p);
            } else {
                corrupt = true;
                log.info(stock.getSymbol() + " corrupt " + map.get(day) + " : " + p);

                DateTime found = map.get(day).getDate();
                DateTime nextFound = date;

                if(found.hourOfDay().get() == 0) {
                    // Do nothing as we have the correct DateTime
                } else if(nextFound.hourOfDay().get() == 0) {
                    // Remove the found from map
                    map.remove(day);
                    // Put in this one
                    map.put(day, p);
                } else {
                    log.error("Found multiple corrupt entries");
                }
            }
        }
        if(corrupt) {
            Map<Symbol, StockFA> toStore = new HashMap<>();
            toStore.put(stock.getSymbol(), new StockFA(stock.getSymbol(), new ArrayList<>(map.values())));
            store(toStore);
        }
    }
}
