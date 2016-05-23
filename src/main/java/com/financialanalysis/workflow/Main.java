package com.financialanalysis.workflow;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.financialanalysis.guice.FAModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.log4j.Log4j;

@Log4j
public class Main {
    @Parameter(names={"--backtest", "-b"})
    public static boolean backtest = false;

    @Parameter(names={"--backtestNum", "-n"})
    public static int backtestNum;

    @Parameter(names={"--updateSymbols", "-usym"})
    public static boolean updateSymbols;

    @Parameter(names={"--updateStocks", "-usto"})
    public static boolean updateStocks;

    @Parameter(names={"--runStrategies", "-rs"})
    public static boolean runStrategies;

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
