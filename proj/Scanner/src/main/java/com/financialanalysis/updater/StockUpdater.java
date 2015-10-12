package com.financialanalysis.updater;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private AtomicInteger totalStocks = new AtomicInteger(1);
    private Map<Symbol, Boolean> found = new ConcurrentHashMap<>();

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
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<List<Symbol>> lists = Lists.partition(allSymbols, BATCH_SIZE);
        for(List<Symbol> list : lists) executorService.execute(() -> updateStocks(list));

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

        for(int i = 0; i < symbols.size(); i++) {
            Symbol s = symbols.get(i);
            if(found.containsKey(s)) {
                log.info("Already found: " + s + " skipping.");
                symbols.remove(i);
                i--;
            }
        }

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

        for(Symbol s : symbols) found.put(s, true);
        numProcessed.addAndGet(symbols.size());
        log.info(String.format("Processed %d/%d", numProcessed.get(), totalStocks.get()));

        long end = System.nanoTime();
        long elapsedTime = end - start;
        double seconds = (double)elapsedTime / 1000000000.0;
        log.info(String.format("Processed %d stocks in %.2f seconds", symbols.size(), seconds));
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
                    // TODO: This means the stock can't be pulled, store in, should save in a file
                    // that can be cached and used as a black list
                }
            }
        }

        if(storedStocksChanged) {
            stockStore.store(storedStocks);
        }
    }

    /**
     * Will delete symbols from stockStore, repull them from the network and
     * save them back into stockStore. This happens usually, if a stock become
     * corrupted. ie. malformed json
     * @param symbols
     */
    public void refreshStock(List<String> symbols) {

    }

    /**
     * Determines what updates are needed and goes and pulls them from the network
     */
    private Map<Symbol, StockFA> getUpdates(Map<Symbol, StockFA> loadedStocks) {
        DateTime currentDate = new DateTime();
        DateTime cutOffTIme = new DateTime();

        //Set cut off time to 4:00 pm on Bay/Wall Street timezone
        cutOffTIme.withZone(DateTimeZone.forID("America/Toronto"));
        cutOffTIme.withHourOfDay(16);
        cutOffTIme.withMinuteOfHour(0);

        Map<Symbol, StockToPull> stocksToPull = new LinkedHashMap<>();
        Collection<Symbol> symbols = loadedStocks.keySet();
        for(Symbol symbol : symbols) {
            StockFA stock = loadedStocks.get(symbol);
            if(stock.getMostRecentDate().isBefore(cutOffTIme)) {
                stocksToPull.put(symbol, new StockToPull(symbol, stock.getMostRecentDate().plusDays(1), currentDate));
            }
        }

        Map<Symbol, StockFA> updatedStocks = new HashMap<>();
        symbols = stocksToPull.keySet();
        for(Symbol symbol : symbols) {
            StockToPull stockToPull = stocksToPull.get(symbol);
            try {
                StockFA stockFa = stockPuller.getStock(stockToPull.getSymbol(), stockToPull.getFrom(), stockToPull.getTo());
                updatedStocks.put(symbol, stockFa);
            } catch (Exception e) {
                log.error("Update failed: " + stockToPull.getSymbol());
            }
        }

        return updatedStocks;
    }

    @Data
    private class StockToPull {
        private final Symbol symbol;
        private final DateTime from;
        private final DateTime to;
    }
}
