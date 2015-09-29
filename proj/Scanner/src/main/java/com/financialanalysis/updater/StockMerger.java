package com.financialanalysis.updater;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StockMerger {
    /**
     * Merge updatedStocks into loadedStocks, if updatedStock does not exist in loadedStocks, add it
     */
    public Map<String, StockFA> merge(Map<String, StockFA> loadedStocks, Map<String, StockFA> updatedStocks) {
        Set<String> updatedSymbols = updatedStocks.keySet();
        for(String symbol : updatedSymbols) {
            //Add it to existing stock
            if(loadedStocks.containsKey(symbol)) {
                StockFA oldStock = loadedStocks.get(symbol);
                StockFA newStock = updatedStocks.get(symbol);
                StockFA mergedStock = mergeStocks(oldStock, newStock);

                loadedStocks.remove(symbol);
                loadedStocks.put(symbol, mergedStock);
                //Create a new stock
            } else {
                loadedStocks.put(symbol, updatedStocks.get(symbol));
            }
        }

        return loadedStocks;
    }

    /**
     * Merge b into a
     */
    private StockFA mergeStocks(StockFA a, StockFA b) {
        // History is always ordered such that the first elements are earliest
        List<StockPrice> aHist = a.getHistory();
        List<StockPrice> bHist = b.getHistory();

        for(StockPrice bSP : bHist) {
            if(aHist.contains(bSP)) {
                throw new RuntimeException("Found overlapping stocks while merge updates." +
                        "Seems like something is wrong with your mergeing");
            }
        }

        List<StockPrice> hist = new ArrayList<>();
        hist.addAll(aHist);
        hist.addAll(bHist);

        return new StockFA(a.getSymbol(), a.getName(), a.getCurrency(), a.getStockExchange(), hist);
    }
}
