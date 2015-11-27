package com.financialanalysis.workflow;

import com.financialanalysis.questrade.Questrade;
import com.financialanalysis.reports.Emailer;
import com.financialanalysis.reports.Reporter;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.financialanalysis.updater.StockMerger;
import com.financialanalysis.updater.StockPuller;
import com.financialanalysis.updater.StockRetriever;
import com.financialanalysis.updater.StockUpdater;
import com.financialanalysis.updater.SymbolUpdater;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;

import java.util.Arrays;

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
        stockUpdater.update();
        strategyRunner.runAllStrategies();
        reporter.generateReports();
        emailer.emailReports();

        long elapsedTime = System.nanoTime() - start;
        double seconds = (double)elapsedTime / 1000000000.0;
        log.info(String.format("Took %d seconds", (int) seconds));
    }

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
//    private void backTestNum(int num) {
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
}