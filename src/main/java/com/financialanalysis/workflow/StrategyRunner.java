package com.financialanalysis.workflow;

import com.financialanalysis.common.DateTimeUtils;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.financialanalysis.strategy.FlagConfig;
import com.financialanalysis.strategy.StrategyOutput;
import com.financialanalysis.strategy.FlagStrategyInput;
import com.financialanalysis.strategy.FlagStrategy;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.financialanalysis.workflow.Main.*;

@Log4j
@Singleton
public class StrategyRunner {
    private final SymbolStore symbolStore;
    private final StockStore stockStore;

//    private final DateTime start = DEFAULT_START_DATE;
    private final DateTime backTestStartDate = new DateTime("2015-01-01", DateTimeZone.forID("America/Toronto")).withTimeAtStartOfDay();
    private final DateTime runStrategiesStartDate = DateTimeUtils.getToday().minusDays(200);
    private final DateTime end = DateTimeUtils.getToday();
    private final ExecutorService exector;
    private final FlagConfig config;
    private final int MAX_BATCH_SIZE = 100;

    @Inject
    public StrategyRunner(SymbolStore symbolStore, StockStore stockStore) {
        this.symbolStore = symbolStore;
        this.stockStore = stockStore;
        this.exector = Executors.newFixedThreadPool(10);
        this.config = FlagConfig.readFromFile();
    }

    /**
     * Run's all strategies on select stocks
     */
    @SneakyThrows
    public List<StrategyOutput> runOnStocks(List<StockFA> stocks) {
        log.info("Beginning to run on select stocks.");

        List<StrategyOutput> results = new LinkedList<>();

        stocks.forEach(s -> {
            StrategyOutput output = runStock(s);
            if(!output.isEmpty()) {
                results.add(output);
            }
        });

        return results;
    }

    /**
     * Run's all strategies on all stocks
     */
    @SneakyThrows
    public List<StrategyOutput> run() {
        log.info("Beginning to run all stocks.");
        List<Symbol> allSymbols = symbolStore.load();

        List<StrategyOutput> results = Lists.newArrayList();
        List<List<Symbol>> lists = Lists.partition(allSymbols, MAX_BATCH_SIZE);

        lists.forEach(list -> {
            Map<Symbol, StockFA> stockMap = stockStore.load(list);
            Collection<StockFA> stocks = stockMap.values();

            // List of futures to be return from stocks
            List<Future<StrategyOutput>> futures = Lists.newArrayList();

            // For each stock, submit it to the exector
            stocks.forEach(s -> {
                futures.add(runStockFuture(s));
            });

            futures.forEach(f -> {
                try {
                    StrategyOutput output = f.get();
                    if(!output.isEmpty()) {
                        results.add(output);
                    }
                } catch (InterruptedException e) {
                    log.error("InterruptedException: ", e);
                } catch (ExecutionException e) {
                    log.error("ExecutionException: ", e);
                }
            });
        });

        exector.shutdown();

        return results;
    }

    private Future<StrategyOutput> runStockFuture(StockFA stock) {
        DateTime start;
        if(runStrategies) {
            start = runStrategiesStartDate;
        } else {
            start = backTestStartDate;
        }

        FlagStrategyInput input = new FlagStrategyInput(stock, config, start, end);
        FlagStrategy flagStrategy = new FlagStrategy();

        return exector.submit(() -> flagStrategy.runStrategy(input));
    }

    private StrategyOutput runStock(StockFA stock) {
        DateTime start;
        if(runStrategies) {
            start = runStrategiesStartDate;
        } else {
            start = backTestStartDate;
        }

        FlagStrategyInput input = new FlagStrategyInput(stock, config, start, end);
        FlagStrategy flagStrategy = new FlagStrategy();

        return flagStrategy.runStrategy(input);
    }
}
