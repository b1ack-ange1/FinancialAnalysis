package com.financialanalysis.workflow;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.store.SymbolStore;
import com.financialanalysis.strategy.AbstractStrategy;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.financialanalysis.updater.StockPuller.DEFAULT_START_DATE;

@Log4j
@Singleton
public class StrategyRunner {
    private final Account account;
    private final SymbolStore symbolStore;
    private final StockStore stockStore;

    private final DateTime start = DEFAULT_START_DATE;
    private final DateTime end = DateTime.now(DateTimeZone.forID("America/Toronto"));
    private final int MAX_BATCH_SIZE = 100;

    private Map<AbstractStrategy, List<StrategyOutput>> allResults = new ConcurrentHashMap<>();


    @Inject
    public StrategyRunner(SymbolStore symbolStore, StockStore stockStore) {
        this.account = Account.createDefaultAccount();
        this.symbolStore = symbolStore;
        this.stockStore = stockStore;
    }

    @SneakyThrows
    public void runAllStrategies() {
        log.info("Beginning to run all strategies.");
        List<Symbol> allSymbols = symbolStore.load();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<List<Symbol>> lists = Lists.partition(allSymbols, MAX_BATCH_SIZE);
        for(List<Symbol> symbols : lists) {
            Map<Symbol, StockFA> stocks = stockStore.load(symbols);
            executorService.execute(() -> runBatch(stocks.values()));
        }

        // Wait for everything to be finished
        log.info("Waiting for strategies to finish.");
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // Stop everything and report what we have
            log.error("All threads did not shut down, killing them ...");
            executorService.shutdownNow();
        }
        log.info("Run strategies has finished.");
    }

    private void runBatch(Collection<StockFA> batch) {
        StrategyInput input = new StrategyInput(new LinkedList<>(batch), start, end, null);
        for(AbstractStrategy strategy : getStrategies()) {
            StrategyOutput output = strategy.runStrategy(input);
            updateResults(strategy, output);
        }
    }

    private List<AbstractStrategy> getStrategies() {
        List<AbstractStrategy> list = new LinkedList<>();
        list.add(new FlagStrategy());
        return list;
    }

    private synchronized void updateResults(AbstractStrategy strategy, StrategyOutput output) {
        if(allResults.containsKey(strategy)) {
            List<StrategyOutput> list = allResults.get(strategy);
            list.add(output);
            allResults.put(strategy, list);
        } else {
            List<StrategyOutput> list = new LinkedList<>();
            list.add(output);
            allResults.put(strategy, list);
        }
    }

    public synchronized Map<AbstractStrategy, List<StrategyOutput>> getAllResults() {
        return allResults;
    }
}
