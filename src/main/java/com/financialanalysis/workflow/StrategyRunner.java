package com.financialanalysis.workflow;

import com.financialanalysis.common.DateTimeUtils;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.financialanalysis.strategy.FlagConfig;
import com.financialanalysis.strategy.StrategyOutput;
import com.financialanalysis.strategy.FlagStrategyInput;
import com.financialanalysis.strategy.FlagStrategy;
import com.financialanalysis.strategyV2.StrategyInput;
import com.financialanalysis.strategyV2.StrategyOutputV2;
import com.financialanalysis.strategyV2.bollinger.BollingerStrategy;
import com.financialanalysis.strategyV2.bollingermacd.BollingerMacdChart;
import com.financialanalysis.strategyV2.bollingermacd.BollingerMacdStategy;
import com.financialanalysis.strategyV2.macd.MacdStrategy;
import com.google.common.base.Strings;
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

import static com.financialanalysis.analysis.AnalysisTools.getValidStockPrices;
import static com.financialanalysis.workflow.Main.*;

@Log4j
@Singleton
public class StrategyRunner {
    private final SymbolStore symbolStore;
    private final StockStore stockStore;
    private final MacdStrategy macdStrategy;
    private final BollingerStrategy bollingerStrategy;
    private final BollingerMacdStategy bollingerMacdStategy;

    private final DateTime startDefault = new DateTime("2015-01-01", DateTimeZone.forID("America/Toronto")).withTimeAtStartOfDay();
    private final DateTime runStrategiesStartDate = DateTimeUtils.getToday().minusDays(200);
    private final DateTime today = DateTimeUtils.getToday();
    private final int MAX_BATCH_SIZE = 100;

    private final ExecutorService exector;
    private final FlagConfig config;

    @Inject
    public StrategyRunner(SymbolStore symbolStore,
                          StockStore stockStore,
                          MacdStrategy macdStrategy,
                          BollingerStrategy bollingerStrategy,
                          BollingerMacdStategy bollingerMacdStategy) {
        this.symbolStore = symbolStore;
        this.stockStore = stockStore;
        this.macdStrategy = macdStrategy;
        this.bollingerStrategy = bollingerStrategy;
        this.bollingerMacdStategy = bollingerMacdStategy;

        this.exector = Executors.newFixedThreadPool(10);
        this.config = FlagConfig.readFromFile();
    }

    /**
     * Run's all strategies on select stocks
     */
    @SneakyThrows
    public List<StrategyOutputV2> runOnStocks(List<StockFA> stocks) {
        log.info("Beginning to run on select stocks.");

        List<StrategyOutputV2> results = new LinkedList<>();

        stocks.forEach(s -> {
            StrategyOutputV2 output = runStockV2(s);
            if (!output.isEmpty()) {
                results.add(output);
            }
        });

        return results;
    }

    /**
     * Run's all strategies on all stocks
     */
    @SneakyThrows
    public List<StrategyOutputV2> run() {
        log.info("Beginning to run all stocks.");
        List<Symbol> allSymbols = symbolStore.load();

        List<StrategyOutputV2> results = Lists.newArrayList();
        List<List<Symbol>> lists = Lists.partition(allSymbols, MAX_BATCH_SIZE);

        lists.forEach(list -> {
            Map<Symbol, StockFA> stockMap = stockStore.load(list);
            Collection<StockFA> stocks = stockMap.values();

            // List of futures to be return from stocks
            List<Future<StrategyOutputV2>> futures = Lists.newArrayList();

            // For each stock, submit it to the exector
            stocks.forEach(s -> {
                futures.add(runStockFutureV2(s));
            });

            futures.forEach(f -> {
                try {
                    StrategyOutputV2 output = f.get();
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

    private StrategyOutputV2 runStockV2(StockFA stock) {
        DateTime start = getStartDate();
        DateTime end = getEndDate();
        List<StockPrice> stockPrices = getValidStockPrices(stock.getHistory(), start, end);
        StockFA filteredStock = new StockFA(stock.getSymbol(), stockPrices);

        StrategyInput input = new StrategyInput(filteredStock);
        StrategyOutputV2 output = bollingerStrategy.run(input); //macdStrategy.run(input);
//        StrategyOutputV2 output = bollingerMacdStategy.run(input);

        return output;
    }

    private Future<StrategyOutputV2> runStockFutureV2(StockFA stock) {
        DateTime start = getStartDate();
        DateTime end = getEndDate();
        List<StockPrice> stockPrices = getValidStockPrices(stock.getHistory(), start, end);
        StockFA filteredStock = new StockFA(stock.getSymbol(), stockPrices);

        StrategyInput input = new StrategyInput(filteredStock);

//        return exector.submit(() -> macdStrategy.run(input));
        return exector.submit(() -> bollingerStrategy.run(input));
//        return exector.submit(() -> bollingerMacdStategy.run(input));
    }

    private DateTime getStartDate() {
        DateTime start;
        // If we are runStrategies, then just return default start and end
        if(runStrategies) {
            start = runStrategiesStartDate;
        } else {
            // If backtest start or end exists, use those
            if(!Strings.isNullOrEmpty(backtestStart)) {
                start = new DateTime(backtestStart, DateTimeZone.forID("America/Toronto")).withTimeAtStartOfDay();
            } else {
                start = startDefault;
            }
        }

        return start;
    }

    private DateTime getEndDate() {
        DateTime end;
        // If we are runStrategies, then just return default start and end
        if(runStrategies) {
            end = today;
        } else {
            // If backtest start or end exists, use those
            if(!Strings.isNullOrEmpty(backtestEnd)) {
                end = new DateTime(backtestEnd, DateTimeZone.forID("America/Toronto")).withTimeAtStartOfDay();
            } else {
                end = today;
            }
        }

        return end;
    }

    private Future<StrategyOutput> runStockFuture(StockFA stock) {
        FlagStrategyInput input = getFlagStrategyInput(stock);
        FlagStrategy flagStrategy = new FlagStrategy();
        return exector.submit(() -> flagStrategy.runStrategy(input));
    }

    private StrategyOutput runStock(StockFA stock) {
        FlagStrategyInput input = getFlagStrategyInput(stock);
        FlagStrategy flagStrategy = new FlagStrategy();
        return flagStrategy.runStrategy(input);
    }

    private FlagStrategyInput getFlagStrategyInput(StockFA stock) {
        DateTime start;
        DateTime end;
        // If we are runStrategies, then just return default start and end
        if(runStrategies) {
            start = runStrategiesStartDate;
            end = today;
        } else {
            // If backtest start or end exists, use those
            if(!Strings.isNullOrEmpty(backtestStart)) {
                start = new DateTime(backtestStart, DateTimeZone.forID("America/Toronto")).withTimeAtStartOfDay();
            } else {
                start = startDefault;
            }

            if(!Strings.isNullOrEmpty(backtestEnd)) {
                end = new DateTime(backtestEnd, DateTimeZone.forID("America/Toronto")).withTimeAtStartOfDay();
            } else {
                end = today;
            }
        }
        return new FlagStrategyInput(stock, config, start, end);
    }
}
