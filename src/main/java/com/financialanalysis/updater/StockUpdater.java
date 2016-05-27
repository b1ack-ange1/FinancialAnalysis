package com.financialanalysis.updater;

import com.financialanalysis.common.DateTimeUtils;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j
public class StockUpdater {
    private final StockPuller stockPuller;
    private final StockStore stockStore;
    private final StockMerger stockMerger;
    private final SymbolStore symbolStore;

    private static final int BATCH_SIZE = 100;

    private AtomicInteger numProcessed = new AtomicInteger(0);
    private AtomicInteger totalStocks = new AtomicInteger(0);

    @Inject
    public StockUpdater(StockPuller stockPuller,
                        StockStore stockStore,
                        StockMerger stockMerger,
                        SymbolStore symbolStore) {
        this.stockPuller = stockPuller;
        this.stockStore = stockStore;
        this.stockMerger = stockMerger;
        this.symbolStore = symbolStore;
    }

    /**
     * For all the symbols, determine what is missing and merge than
     * into existing stockStore. If symbol is not in stockStore, then
     * add it to the stockStore.
     */
    public void update() {
        log.info("Updating stocks");
        List<Symbol> allSymbols = symbolStore.load();
        totalStocks.set(allSymbols.size());
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        List<List<Symbol>> lists = Lists.partition(allSymbols, BATCH_SIZE);
        for(List<Symbol> list : lists) {
            executorService.execute(() -> updateStocks(list));
        }

        // Wait for everything to be finished
        log.info("Waiting for update to finish.");
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // Stop everything and report what we have
            log.error("All threads did not shut down, killing them ...");
            executorService.shutdownNow();
        }
        log.info("Update has finished.");
    }

    /**
     * For stocks in stockStore as listed in symbols, update them
     */
    public void updateStocks(List<Symbol> symbols) {
        long start = System.nanoTime();

        // Pull all stocks in store
        Map<Symbol, StockFA> loadedStocks = stockStore.load(symbols);

        // Get updates for them and store them back into store
        Map<Symbol, StockFA> updatedStocks = getUpdates(loadedStocks);
        Map<Symbol, StockFA> mergedStocks = stockMerger.merge(loadedStocks, updatedStocks);
        stockStore.store(mergedStocks);

        // For any missing symbols, add them to the store
        List<Symbol> missingSymbols = new ArrayList<>();
        for(Symbol symbol : symbols) {
            if(!loadedStocks.containsKey(symbol)) {
                missingSymbols.add(symbol);
            }
        }
        addStocksToStore(missingSymbols);

        numProcessed.addAndGet(symbols.size());
        log.info(String.format("Processed %d/%d", numProcessed.get(), totalStocks.get()));

        long end = System.nanoTime();
        long elapsedTime = end - start;
        double seconds = (double)elapsedTime / 1000000000.0;
        log.info(String.format("Processed %d stocks in %d seconds", symbols.size(), (int) seconds));
    }

    /**
     * If symbol is not already in stockStore, pull it from network and store it
     */
    public void addStocksToStore(List<Symbol> symbols) {
        Map<Symbol, StockFA> storedStocks = stockStore.load(symbols);
        boolean storedStocksChanged = false;

        // If we don't have symbol in stock store, go get it and put it in the store
        for(Symbol symbol : symbols) {
            if(!storedStocks.containsKey(symbol)) {
                try {
                    StockFA stock = stockPuller.getStock(symbol);
                    storedStocks.put(symbol, stock);
                    storedStocksChanged = true;
                } catch (Exception e) {
                    log.error("Add to store failed: " + symbol.getSymbol());
                }
            }
        }

        if(storedStocksChanged) {
            stockStore.store(storedStocks);
        }
    }

    /**
     * Determines what updates are needed and goes and pulls them from the network
     */
    private Map<Symbol, StockFA> getUpdates(Map<Symbol, StockFA> loadedStocks) {
        DateTime currentDay = DateTimeUtils.getToday();

        Map<Symbol, StockToPull> stocksToPull = new LinkedHashMap<>();
        Collection<Symbol> symbols = loadedStocks.keySet();
        for(Symbol symbol : symbols) {
            StockFA stock = loadedStocks.get(symbol);

            try {
                // Get the most recent date we have stock data for
                DateTime mostRecentDay = stock.getMostRecentDate();

                // If mostRecentDay is before the current day then there is new data to pull
                if(mostRecentDay.isBefore(currentDay)) {
                    // Start grabbing data from the day after mostRecentDay till last millisecond of currentDay
                    DateTime startDay = mostRecentDay.plusDays(1).withTimeAtStartOfDay();
                    DateTime endDay = currentDay.plusDays(1).withTimeAtStartOfDay().minusMillis(1);
                    stocksToPull.put(symbol, new StockToPull(symbol, startDay, endDay, true));
                }
            } catch (Exception e) {
                // Doesn't have a most recent date, so attempt to pull the entire stock
                stocksToPull.put(symbol, new StockToPull(symbol, null, currentDay, false));
            }
        }

        List<Symbol> failedSymbols = Lists.newArrayList();
        Map<Symbol, StockFA> updatedStocks = new HashMap<>();
        symbols = stocksToPull.keySet();
        for(Symbol symbol : symbols) {
            StockToPull stockToPull = stocksToPull.get(symbol);
            try {
                StockFA stockFa;
                if(stockToPull.getHasFrom()) {
                    stockFa = stockPuller.getStock(stockToPull.getSymbol(), stockToPull.getFrom(), stockToPull.getTo());
                } else {
                    stockFa = stockPuller.getStock(stockToPull.getSymbol());
                }
                updatedStocks.put(symbol, stockFa);
            } catch (Exception e) {
                log.error("Update failed: " + stockToPull.getSymbol());
                failedSymbols.add(symbol);
            }
        }
        log.info("Failed to pull: " + failedSymbols.size());

        return updatedStocks;
    }

    @Data
    private class StockToPull {
        private final Symbol symbol;
        private final DateTime from;
        private final DateTime to;
        private final Boolean hasFrom;
    }
}
