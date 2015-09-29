package com.financialanalysis.guice;

import com.financialanalysis.csv.SymbolRetriever;
import com.financialanalysis.externalapi.QuestradeApi;
import com.financialanalysis.store.StockStore;
import com.financialanalysis.updater.StockMerger;
import com.financialanalysis.workflow.Analysis;
import com.financialanalysis.analysis.AnalysisFunctions;
import com.financialanalysis.workflow.ServiceMain;
import com.financialanalysis.workflow.StockStrategyRunner;
import com.financialanalysis.data.Account;
import com.financialanalysis.updater.StockPuller;
import com.financialanalysis.workflow.StockRetriever;
import com.financialanalysis.updater.StockUpdater;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opencsv.CSVReader;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.financialanalysis.data.Account.createDefaultAccount;

public class FAModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    public ServiceMain getMainLoop() {
        return new ServiceMain(
                getAnalysis(),
                getStockNetWorkPuller(),
                getStockRetriever(),
                getStockStore(),
                getStockUpdater(),
                getMerger(),
                getQuestradeApi()
        );
    }

    @Provides
    public AnalysisFunctions getAnalysisUtils() { return new AnalysisFunctions(); }

    @Provides
    public Analysis getAnalysis() { return new Analysis(getAccount()); }

    @Provides
    public Account getAccount() {return createDefaultAccount(); }

    @Provides
    public String getSymbolsPath() {
        String s = File.separator;
        return "src" + s + "main" + s + "resources" + s + "companylist_nasdaq.csv";
    }

    @Provides
    @SneakyThrows
    public CSVReader getCSVReader() { return new CSVReader(new FileReader(new File(getSymbolsPath()))); }

    @Provides
    public SymbolRetriever getSymbolRetriever() { return new SymbolRetriever(getCSVReader());}

    @Provides
    public StockStrategyRunner getStockAnayizerRunner() {
        return new StockStrategyRunner(getAccount());
    }

    @Provides
    public StockRetriever getStockRetriever() {
        return new StockRetriever(getSymbolRetriever(), getStockStore(), getStockUpdater());
    }

    @Provides
    public StockStore getStockStore() {
        return new StockStore();
    }

    @Provides
    public StockPuller getStockNetWorkPuller() {
        return new StockPuller();
    }

    @Provides
    public StockUpdater getStockUpdater() {
        return new StockUpdater(
                getStockNetWorkPuller(),
                getStockStore(),
                getMerger(),
                getSymbolRetriever()
        );
    }

    @Provides
    public StockMerger getMerger() { return new StockMerger(); }

    @Provides
    public QuestradeApi getQuestradeApi() { return new QuestradeApi(); }
}
