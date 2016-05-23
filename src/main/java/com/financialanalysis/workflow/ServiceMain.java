package com.financialanalysis.workflow;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.questrade.Questrade;
import com.financialanalysis.reports.Emailer;
import com.financialanalysis.reports.Reporter;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.financialanalysis.strategy.AbstractStrategy;
import com.financialanalysis.strategy.StrategyOutput;
import com.financialanalysis.updater.StockMerger;
import com.financialanalysis.updater.StockPuller;
import com.financialanalysis.updater.StockRetriever;
import com.financialanalysis.updater.StockUpdater;
import com.financialanalysis.updater.SymbolUpdater;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.jfree.data.time.Day;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log4j
public class ServiceMain implements Runnable {
    private final Analysis analysis;
    private final StockPuller stockPuller;
    private final StockRetriever stockRetriever;
    private final StockStore stockStore;
    private final StockUpdater stockUpdater;
    private final StockMerger stockMerger;
    private final Questrade questrade;
    private final SymbolUpdater symbolUpdater;
    private final StrategyRunner strategyRunner;
    private final Reporter reporter;
    private final SymbolStore symbolStore;
    private final Emailer emailer;

    @Inject
    public ServiceMain(Analysis analysis,
                       StockPuller stockPuller,
                       StockRetriever stockRetriever,
                       StockStore stockStore,
                       StockUpdater stockUpdater,
                       StockMerger stockMerger,
                       Questrade questrade,
                       SymbolUpdater symbolUpdater,
                       StrategyRunner strategyRunner,
                       Reporter reporter,
                       SymbolStore symbolStore,
                       Emailer emailer) {
        this.analysis = analysis;
        this.stockPuller = stockPuller;
        this.stockRetriever = stockRetriever;
        this.stockStore = stockStore;
        this.stockUpdater = stockUpdater;
        this.stockMerger = stockMerger;
        this.questrade = questrade;
        this.symbolUpdater = symbolUpdater;
        this.strategyRunner = strategyRunner;
        this.reporter = reporter;
        this.symbolStore = symbolStore;
        this.emailer = emailer;
    }

    /**
     1) Update SymbolStore
     2) Update StockStore
     3) Run all stratgies
     4) Generate report
     5) Email report
     */
    @Override
    @SneakyThrows
    public void run() {
        log.info("Starting ServiceMain");
        long start = System.nanoTime();

//        symbolUpdater.refresh();
//        stockUpdater.update();
//        List<StrategyOutput> allResults = strategyRunner.run();
//        List<StrategyOutput> allResults = strategyRunner.runOnStocks(stockRetriever.getUniqueRandomStocks(100));
//        reporter.generateReports(allResults);
//        emailer.emailReports();

        runAll();
//        runUntil(20);
//        runOn();
//        cleanStockStore();

        long elapsedTime = System.nanoTime() - start;
        double seconds = (double)elapsedTime / 1000000000.0;
        log.info(String.format("Took %d seconds", (int) seconds));
    }

    public void runAll() {
        List<StrategyOutput> outputs = strategyRunner.run();
        reporter.generateAverageAccountSummary(outputs);

    }

    public void runUntil(int numFound) {
        List<StrategyOutput> outputs = new ArrayList<>();

        do {
            outputs.addAll(strategyRunner.runOnStocks(stockRetriever.getUniqueRandomStocks(1)));
        } while(outputs.size() < numFound);

        reporter.generateIndividualAccountSummary(outputs);
        reporter.generateAverageAccountSummary(outputs);
    }

    @SneakyThrows
    public void runOn() {
        List<StrategyOutput> outputs = new ArrayList<>();
        List<Symbol> symbols = symbolStore.load(Arrays.asList("UWHR"));
        List<StockFA> stocks = new ArrayList<>(stockStore.load(symbols).values());
        outputs.addAll(strategyRunner.runOnStocks(stocks));

        reporter.generateIndividualAccountSummary(outputs);
        reporter.generateAverageAccountSummary(outputs);
    }

    @SneakyThrows
    public void cleanStockStore() {
        log.info("Cleaning stock store");
        List<Symbol> allSymbols = symbolStore.load();
        List<List<Symbol>> lists = Lists.partition(allSymbols, 100);

        lists.forEach(list -> {
            Map<Symbol, StockFA> stockMap = stockStore.load(list);
            Collection<StockFA> stocks = stockMap.values();
            stocks.forEach(s -> {
                clean(s);
            });
        });
    }

    public void clean(StockFA stock) {
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
//            Map<Symbol, StockFA> toStore = new HashMap<>();
//            toStore.put(stock.getSymbol(), new StockFA(stock.getSymbol(), new ArrayList<>(map.values())));
//            stockStore.store(toStore);
        }
    }

//    private void runOnRand(int num) {
//        List<StockFA> randStocks;
//        int processed = 0;
//        int total = num;
//        double totalTime = 0;
//        do {
//            long start = System.nanoTime();
//
//            randStocks = stockRetriever.getUniqueRandomStocks(BACK_TEST_BATCH_SIZE);
//            analysis.analyzeStocks(randStocks);
//
//            long end = System.nanoTime();
//            long elapsedTime = end - start;
//            double seconds = (double)elapsedTime / 1000000000.0;
//            totalTime += seconds;
//            processed += randStocks.size();
//            double percentage = processed * 100.0 / total;
//            log.info(String.format("Processed %d/%d %.2f%% took %.2f sec. Total %.2f",
//                    processed, total, percentage, seconds, totalTime));
//        } while(!randStocks.isEmpty() && processed < num);
//    }

//    private void performBackTest() {
//        backTestAll();
////        backTestNum(10000);
//        analysis.reportResultsByStock();
//    }
//
//    private void backTest(List<Symbol> symbols) {
//        List<StockFA> stockList = stockRetriever.getStocks(symbols);
//        analysis.analyzeStocks(stockList);
//    }
//
//    private int BACK_TEST_BATCH_SIZE = 100;
//    private void backTestAll() {
//        log.info("Beginning backTestAll");
//        List<StockFA> randStocks;
//        int processed = 0;
//        int total = stockRetriever.getNumAvailableStocks();
//        double totalTime = 0;
//        do {
//            long start = System.nanoTime();
//
//            randStocks = stockRetriever.getUniqueRandomStocks(BACK_TEST_BATCH_SIZE);
//            analysis.analyzeStocks(randStocks);
//
//            long end = System.nanoTime();
//            long elapsedTime = end - start;
//            double seconds = (double)elapsedTime / 1000000000.0;
//            totalTime += seconds;
//            processed += randStocks.size();
//            double percentage = processed * 100.0 / total;
//            log.info(String.format("Processed %d/%d %.2f%% took %.2f sec. Total %.2f",
//                    processed, total, percentage, seconds, totalTime));
//        } while(!randStocks.isEmpty());
//    }
//

}