package com.financialanalysis.workflow;

import com.financialanalysis.common.DateTimeUtils;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.financialanalysis.strategy.StrategyOutput;
import com.financialanalysis.strategy.StrategyInput;
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
import java.util.concurrent.Future;

import static com.financialanalysis.workflow.Main.*;

@Log4j
@Singleton
public class StrategyRunner {
    private final FlagStrategy flagStrategy;
    private final SymbolStore symbolStore;
    private final StockStore stockStore;

//    private final DateTime start = DEFAULT_START_DATE;
    private final DateTime backTestStartDate = new DateTime("2015-01-01", DateTimeZone.forID("America/Toronto")).withTimeAtStartOfDay();
    private final DateTime runStrategiesStartDate = DateTimeUtils.getToday().minusDays(200);
    private final DateTime end = DateTimeUtils.getToday();
    private final int MAX_BATCH_SIZE = 100;

    @Inject
    public StrategyRunner(FlagStrategy flagStrategy, SymbolStore symbolStore, StockStore stockStore) {
        this.flagStrategy = flagStrategy;
        this.symbolStore = symbolStore;
        this.stockStore = stockStore;
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

        List<Future<StrategyOutput>> futures = new LinkedList<>();
        List<StrategyOutput> results = new LinkedList<>();
        List<List<Symbol>> lists = Lists.partition(allSymbols, MAX_BATCH_SIZE);

        lists.forEach(list -> {
            Map<Symbol, StockFA> stockMap = stockStore.load(list);
            Collection<StockFA> stocks = stockMap.values();
            stocks.forEach(s -> {
                StrategyOutput output = runStock(s);
                if(!output.isEmpty()) {
                    results.add(output);
                }
            });
        });

        return results;
    }

    private StrategyOutput runStock(StockFA stock) {
        StrategyInput input;

        if(runStrategies) {
            input = new StrategyInput(stock, runStrategiesStartDate, end);
        } else {
            input = new StrategyInput(stock, backTestStartDate, end);
        }

        return flagStrategy.runStrategy(input);
    }
}
