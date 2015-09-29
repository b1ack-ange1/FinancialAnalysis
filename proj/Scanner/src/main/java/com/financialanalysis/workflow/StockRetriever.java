package com.financialanalysis.workflow;

import com.financialanalysis.csv.SymbolRetriever;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.updater.StockUpdater;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockRetriever {
    private final SymbolRetriever symbolRetriever;
    private final StockStore stockStore;
    private final StockUpdater stockUpdater;

    private static String[] allSymbols;

    private Map<String, Boolean> returnedStocks;
    private int lastIndex = -1;

    @Inject
    public StockRetriever(SymbolRetriever symbolRetriever,
                          StockStore stockStore,
                          StockUpdater stockUpdater) {
        this.symbolRetriever = symbolRetriever;
        this.stockStore = stockStore;
        this.stockUpdater = stockUpdater;
        allSymbols = symbolRetriever.getAllSymbols();
        returnedStocks = new HashMap<>();
    }

    /**
     * Will return locally stored stock if exists, if not
     * then will pull from network and store locally and return it
     */
    public List<StockFA> getStocks(List<String> symbols) {
        List<StockFA> stockList = new ArrayList<>();
        Map<String, StockFA> storedStocks = stockStore.load(symbols);
        List<String> missingSymbols = new ArrayList<>();

        for(String symbol : symbols) {
            //If we have it stored, return it
            if(storedStocks.containsKey(symbol)) {
                stockList.add(storedStocks.get(symbol));

            //If not then pull it and store it to stockStore
            } else {
                missingSymbols.add(symbol);
            }
        }

        if(!missingSymbols.isEmpty()) {
            // Add missing symbols to the store
            stockUpdater.addStocksToStore(missingSymbols);

            // Get them from the store
            Map<String, StockFA> newlyAdded = stockStore.load(missingSymbols);
            stockList.addAll(newlyAdded.values());
        }

        return stockList;
    }

    /**
     * Will return num random stocks from the local store.
     *
     * Will NOT call across network.
     *
     * Stock will be unique, meaning if retrieved in previous
     * call, they will not be retrieved again. Will return empty
     * list if not more to return.
     */
    public List<StockFA> getUniqueRandomStocks(int num) {
        List<String> symbols = new ArrayList<>();

        int tmp = lastIndex + 1;
        for(int i = lastIndex + 1; i < allSymbols.length && i < lastIndex + 1 + num; i++) {
            symbols.add(allSymbols[i]);
            tmp = i;
        }
        lastIndex = tmp;

        Map<String, StockFA> storedStocks = stockStore.load(symbols);
        List<StockFA> stockList = new ArrayList<>(storedStocks.values());

        return stockList;
    }

    public int getNumAvailableStocks() {
        return allSymbols.length;
    }
}
