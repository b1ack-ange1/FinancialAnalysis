package com.financialanalysis.updater;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockRetriever {
    private final StockStore stockStore;
    private final StockUpdater stockUpdater;
    private final SymbolStore symbolStore;

    private List<Symbol> allSymbols;
    private Map<String, Boolean> returnedStocks;
    private int lastIndex = -1;

    @Inject
    public StockRetriever(StockStore stockStore,
                          StockUpdater stockUpdater,
                          SymbolStore symbolStore) {
        this.stockStore = stockStore;
        this.stockUpdater = stockUpdater;
        this.symbolStore = symbolStore;
        allSymbols = symbolStore.load();
        returnedStocks = new HashMap<>();
    }

    /**
     * Will return locally stored stock if exists, if not
     * then will pull from network and store locally and return it
     */
    public List<StockFA> getStocks(List<Symbol> symbols) {
        List<StockFA> stockList = new ArrayList<>();
        Map<Symbol, StockFA> storedStocks = stockStore.load(symbols);
        List<Symbol> missingSymbols = new ArrayList<>();

        for(Symbol symbol : symbols) {
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
            Map<Symbol, StockFA> newlyAdded = stockStore.load(missingSymbols);
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
        List<Symbol> symbols = new ArrayList<>();

        int tmp = lastIndex + 1;
        for(int i = lastIndex + 1; i < allSymbols.size() && i < lastIndex + 1 + num; i++) {
            symbols.add(allSymbols.get(i));
            tmp = i;
        }
        lastIndex = tmp;

        Map<Symbol, StockFA> storedStocks = stockStore.load(symbols);
        List<StockFA> stockList = new ArrayList<>(storedStocks.values());

        return stockList;
    }

    public int getNumAvailableStocks() {
        return allSymbols.size();
    }
}
