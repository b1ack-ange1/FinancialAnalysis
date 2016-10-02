package com.financialanalysis.workflow;

import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.questrade.Questrade;
import com.financialanalysis.reports.Emailer;
import com.financialanalysis.reports.Report;
import com.financialanalysis.reports.Reporter;
import com.financialanalysis.store.ChartStore;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.financialanalysis.strategy.FlagConfig;
import com.financialanalysis.strategyV2.StrategyOutputV2;
import com.financialanalysis.updater.StockRetriever;
import com.financialanalysis.updater.StockUpdater;
import com.financialanalysis.updater.SymbolUpdater;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.financialanalysis.workflow.Main.*;

@Log4j
public class ServiceMain implements Runnable {
    private final StockRetriever stockRetriever;
    private final StockStore stockStore;
    private final StockUpdater stockUpdater;
    private final Questrade questrade;
    private final SymbolUpdater symbolUpdater;
    private final StrategyRunner strategyRunner;
    private final Reporter reporter;
    private final ChartStore chartStore;
    private final SymbolStore symbolStore;
    private final Emailer emailer;

    @Inject
    public ServiceMain(StockRetriever stockRetriever,
                       StockStore stockStore,
                       StockUpdater stockUpdater,
                       Questrade questrade,
                       SymbolUpdater symbolUpdater,
                       StrategyRunner strategyRunner,
                       Reporter reporter,
                       ChartStore chartStore,
                       SymbolStore symbolStore,
                       Emailer emailer) {
        this.stockRetriever = stockRetriever;
        this.stockStore = stockStore;
        this.stockUpdater = stockUpdater;
        this.questrade = questrade;
        this.symbolUpdater = symbolUpdater;
        this.strategyRunner = strategyRunner;
        this.reporter = reporter;
        this.chartStore = chartStore;
        this.symbolStore = symbolStore;
        this.emailer = emailer;
    }

    @Override
    @SneakyThrows
    public void run() {
        log.info("Starting ServiceMain");
        long start = System.nanoTime();

        if(flagConfig) {
            FlagConfig.saveDefault();
            return;
        }

        if(updateStocks || updateSymbols) {
            questrade.authenticate();
        }

        if(updateSymbols) {
            symbolUpdater.refresh();
        }

        if(updateStocks) {
            stockUpdater.update();
        }

        if(runStrategies) {
            List<StrategyOutputV2> allResults = strategyRunner.run();
            List<Report> reports = reporter.generateReports(allResults);
            Path path = chartStore.save(reports);
            emailer.emailReports(path);
        }

        if(backtest && !runStrategies) {
            if(!Strings.isNullOrEmpty(backtestStocks)) {
                backTestOn(backtestStocks);
            } else if(backtestNum > 0) {
                backtestUntil(backtestNum);
            } else {
                backtestAll();
            }
        }

        if(!Strings.isNullOrEmpty(export.trim())) {
            exportStock(export.trim());
        }

        long elapsedTime = System.nanoTime() - start;
        double seconds = (double)elapsedTime / 1000000000.0;
        log.info(String.format("Took %d seconds", (int) seconds));
    }

    public void backtestAll() {
        // Run on all stocks and save results
        List<StrategyOutputV2> outputs = strategyRunner.run();
        List<Report> reports = reporter.generateReports(outputs);

        doReportingForBacktest(outputs, reports);

//        List<String> symbolNames = outputs.stream().map(o -> o.getSymbol().getSymbol()).collect(Collectors.toList());
//        log.info(String.join(",", symbolNames));
//        log.info(symbolNames.size());
    }

    public void backtestUntil(int numFound) {
        // Run until we have found numFound flag
        List<StrategyOutputV2> outputs = Lists.newArrayList();
        StringBuilder buf = new StringBuilder();
        do {
            List<StockFA> stock = stockRetriever.getUniqueRandomStocks(1);
            List<StrategyOutputV2> newOutputs = strategyRunner.runOnStocks(stock);
            if(!newOutputs.isEmpty()) {
                buf.append(newOutputs.get(0).getSymbol().getSymbol() + ",");
            }
            outputs.addAll(newOutputs);
        } while(outputs.size() < numFound);

        List<Report> reports = reporter.generateReports(outputs);
        doReportingForBacktest(outputs, reports);
        log.info(buf.toString());
    }

    public void backTestOn(String commaSeperatedSymbols) {
        List<String> inputSymbols = Lists.newArrayList(commaSeperatedSymbols.split(","));
        List<Symbol> symbols = symbolStore.load(inputSymbols);

        List<StockFA> stocks = Lists.newArrayList(stockStore.load(symbols).values());
        List<StrategyOutputV2> outputs = Lists.newArrayList(strategyRunner.runOnStocks(stocks));

        List<Report> reports = reporter.generateReports(outputs);
        doReportingForBacktest(outputs, reports);

        List<String> symbolNames = outputs.stream().map(o -> o.getSymbol().getSymbol()).collect(Collectors.toList());
        log.info(String.join(",", symbolNames));
        log.info("Stocks Num: " + symbolNames.size());
    }

    private void doReportingForBacktest(List<StrategyOutputV2> outputs, List<Report> reports) {
        if(saveCharts) {
            chartStore.saveBackTestCharts(reports);
        }
        if(percentiles) {
            reporter.generatePercentilesChart(outputs);
        }
        if(detailedSummary) {
            reporter.generateIndividualAccountSummary(outputs);
        }
        if(detailedEntire) {
            reporter.generateDetailedAccountSummary(outputs);
        }
        if(GLvsW) {
            reporter.generateGLvsWeight(outputs);
        }
        reporter.generateAverageAccountSummary(outputs);
    }

    @SneakyThrows
    private void exportStock(String symbol) {
        Collection<StockFA> stocks = stockStore.load(symbolStore.load(Lists.newArrayList(symbol))).values();
        StockFA stockFA = stocks.iterator().next();

        String outputDir = "var/output/";
        StringBuilder builder = new StringBuilder();
        String header = "Date, Volume, Low, High, Open, Close\n";
        builder.append(header);
        for(StockPrice sp : stockFA.getHistory()) {
            String date = sp.getDate().toString().split("T")[0];
            builder.append(date+","+sp.getVolume()+","+sp.getLow()+","+sp.getHigh()+","+sp.getOpen()+","+sp.getClose()+"\n");
        }

        File output = new File(outputDir + stockFA.getSymbol().getSymbol() + ".csv");
        FileUtils.writeStringToFile(output, builder.toString());
    }
}