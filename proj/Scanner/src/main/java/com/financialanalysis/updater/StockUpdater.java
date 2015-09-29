package com.financialanalysis.updater;

import com.financialanalysis.csv.SymbolRetriever;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.updater.StockPuller;
import com.financialanalysis.workflow.Analysis;
import com.google.inject.Inject;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.financialanalysis.analysis.AnalysisTools.deepCopyStringList;

@Log4j
public class StockUpdater {
    private final StockPuller stockPuller;
    private final StockStore stockStore;
    private final StockMerger stockMerger;
    private final SymbolRetriever symbolRetriever;

    @Inject
    public StockUpdater(StockPuller stockPuller,
                        StockStore stockStore,
                        StockMerger stockMerger,
                        SymbolRetriever symbolRetriever) {
        this.stockPuller = stockPuller;
        this.stockStore = stockStore;
        this.stockMerger = stockMerger;
        this.symbolRetriever = symbolRetriever;
    }

    /**
     * For all the symbols, determine what is missing and merge than
     * into existing stockStore. If symbol is not in stockStore, then
     * add it to the stockStore.
     */
    public void updateStockStore() {
        String[] allSymbols = symbolRetriever.getAllSymbols();
        List<String> batch = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        int num = 0;
        for(String symbol : allSymbols) {
            batch.add(symbol);
            num++;
            if(num >= 50) {
                num = 0;
//                updateStocks(batch);
                Runnable run = () -> updateStocks(deepCopyStringList(batch));
                executorService.submit(run);
                batch.clear();
            }
        }
//        updateStocks(batch);
        Runnable lastRun = () -> updateStocks(deepCopyStringList(batch));
        executorService.submit(lastRun);

        // Wait for everthing to be finished
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
     * Pull all the symbols we have add then to the stockStore.
     * If already in stockStore, do nothing.
     */
    public void buildLocalStore() {
        String[] allSymbols = symbolRetriever.getAllSymbols();
        List<String> symbols = Arrays.asList(allSymbols);
        List<String> buf = new ArrayList<>();
        int num = 0;
        for(String symbol : symbols) {
            buf.add(symbol);
            num++;
            if(num == 50) {
                addStocksToStore(buf);
                buf.clear();
                num = 0;
            }
        }
        addStocksToStore(buf);
    }

    /**
     * If symbol is not already in stockStore, pull it from network and store it
     */
    public void addStocksToStore(List<String> symbols) {
        Map<String, StockFA> storedStocks = stockStore.load(symbols);
        boolean storedStocksChanged = false;

        // If we don't have symbol in stock store, go get it and put it in the store
        for(String symbol : symbols) {
            if(!storedStocks.containsKey(symbol)) {
                StockFA stock = stockPuller.getStock(symbol);
                if(stock != null) {
                    storedStocks.put(symbol, stock);
                    storedStocksChanged = true;
                } else {
                    // This means the stock can't be pulled, store in, should save in a file
                    // that can be cached and used as a black list
                }
            }
        }

        if(storedStocksChanged) {
            stockStore.store(storedStocks);
        }
    }

    /**
     * For stocks in stockStore as listed in symbols, update them
     */
    public void updateStocks(List<String> symbols) {
        // Pull all stocks in store
        Map<String, StockFA> loadedStocks = stockStore.load(symbols);

        // Get updates for them and store them back into store
        Map<String, StockFA> updatedStocks = getUpdates(loadedStocks);
        Map<String, StockFA> mergedStocks = stockMerger.merge(loadedStocks, updatedStocks);
        stockStore.store(mergedStocks);

        // For any missing symbols, add them to the store
        List<String> missingSymbols = new ArrayList<>();
        for(String symbol : symbols) {
            if(!loadedStocks.containsKey(symbol)) {
                missingSymbols.add(symbol);
            }
        }
        addStocksToStore(missingSymbols);
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
    private Map<String, StockFA> getUpdates(Map<String, StockFA> loadedStocks) {
        DateTime currentDate = new DateTime();
        DateTime cutOffTIme = new DateTime();

        //Set cut off time to 4:00 pm on Bay/Wall Street timezone
        cutOffTIme.withZone(DateTimeZone.forID("America/Toronto"));
        cutOffTIme.withHourOfDay(16);
        cutOffTIme.withMinuteOfHour(0);

        List<StockToPull> stocksToPull = new ArrayList<>();
        Collection<StockFA> stocks = loadedStocks.values();
        for(StockFA stock : stocks) {
            if(stock.getMostRecentDate().isBefore(cutOffTIme)) {
                stocksToPull.add(new StockToPull(stock.getSymbol(), stock.getMostRecentDate().plusDays(1), currentDate));
            }
        }

        Map<String, StockFA> updatedStocks = new HashMap<>();
        for(StockToPull stockToPull : stocksToPull) {
            StockFA stockFa = stockPuller.getStock(stockToPull.getSymbol(), stockToPull.getFrom(), stockToPull.getTo());
            if(stockFa != null) {
                updatedStocks.put(stockFa.getSymbol(), stockFa);
            }
        }

        return updatedStocks;
    }

    @Data
    private class StockToPull {
        private final String symbol;
        private final DateTime from;
        private final DateTime to;
    }
}
