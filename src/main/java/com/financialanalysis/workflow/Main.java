package com.financialanalysis.workflow;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.financialanalysis.guice.FAModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.log4j.Log4j;

@Log4j
public class Main {
    @Parameter(names={"--backtest", "-b"}, description = "Run a back test.")
    public static boolean backtest = false;

    @Parameter(names={"--backtestNum", "-n"}, description = "Run a back test until be get this many successful signals.")
    public static int backtestNum;

    @Parameter(names={"--backtestStock", "-bs"}, description = "Run a back test on this comma seperated list of stocks.")
    public static String backtestStocks;

    @Parameter(names={"--updateSymbols", "-usym"}, description = "Update the stored symbols from Questrade.")
    public static boolean updateSymbols;

    @Parameter(names={"--updateStocks", "-usto"}, description = "Update the stored stock date.")
    public static boolean updateStocks;

    @Parameter(names={"--runStrategies", "-rs"}, description = "Run strategies for today.")
    public static boolean runStrategies;

    @Parameter(names={"--chart", "-c"}, description = "Save charts from run.")
    public static boolean saveCharts;

    @Parameter(names={"--percentiles", "-p"}, description = "Show percentiles.")
    public static boolean percentiles;

    public static void main(String[] args) {
        Main main = new Main();
        new JCommander(main, args);
        main.run();
    }

    private void run() {
        Injector injector = Guice.createInjector(new FAModule());
        FAService faService = injector.getInstance(FAService.class);
        faService.start();
        faService.stop();
    }
}
