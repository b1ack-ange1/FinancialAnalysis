package com.financialanalysis.updater;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StockRetriever {
    private final StockStore stockStore;

    private List<Symbol> allSymbolsRandomized;
    private int lastIndex = -1;

    @Inject
    public StockRetriever(StockStore stockStore,
                          SymbolStore symbolStore) {
        this.stockStore = stockStore;
        allSymbolsRandomized = symbolStore.load();
        Collections.shuffle(allSymbolsRandomized, new Random(System.nanoTime()));
    }

    /**
     * Will return locally stored stock if exists
     */
    public List<StockFA> getStocks(List<Symbol> symbols) {
        Map<Symbol, StockFA> storedStocks = stockStore.load(symbols);
        return new ArrayList<>(storedStocks.values());
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
        for(int i = lastIndex + 1; i < allSymbolsRandomized.size() && i < lastIndex + 1 + num; i++) {
            symbols.add(allSymbolsRandomized.get(i));
            tmp = i;
        }
        lastIndex = tmp;

        Map<Symbol, StockFA> storedStocks = stockStore.load(symbols);
        List<StockFA> stockList = new ArrayList<>(storedStocks.values());

        return stockList;
    }

    public int getNumAvailableStocks() {
        return allSymbolsRandomized.size();
    }
}
