package com.financialanalysis.workflow;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;
import com.financialanalysis.guice.FAModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.log4j.Log4j;

import java.util.Arrays;
import java.util.List;

@Log4j
public class Main {
    @Parameter(names={"--backtest", "-b"}, description = "Run a back test.")
    public static boolean backtest = false;

    @Parameter(names={"--backtestNum", "-n"}, description = "Run a back test until be get this many successful signals.")
    public static int backtestNum;

    @Parameter(names={"--backtestStock", "-bs"}, description = "Run a back test on this comma seperated list of stocks.")
    public static String backtestStocks;

    @Parameter(names={"--backtestStart", "-bStart"}, description = "Back test start date")
    public static String backtestStart;

    @Parameter(names={"--backtestEnd", "-bEnd"}, description = "Back test start end. If empty, use today")
    public static String backtestEnd;

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

    @Parameter(names={"-d"}, description = "Show summary report for each trade.")
    public static boolean detailedSummary;

    @Parameter(names={"-dd"}, description = "Show entire report for each trade.")
    public static boolean detailedEntire;

    @Parameter(names={"-GLvsW"}, description = "Show Gain Loss vs Weight chart.")
    public static boolean GLvsW;

    @Parameter(names={"--flagConfig", "-fc"}, description = "Restore default flag config file.")
    public static boolean flagConfig;

    @Parameter(names={"--help", "-h"})
    public static boolean help;

    public static void main(String[] args) {
        Main main = new Main();
        JCommander jCommander = new JCommander(main, args);
        jCommander.setProgramName("Scanner");

        if(main.help) {
            jCommander.usage();
        } else {
            main.run();
        }
    }

    private void run() {
            Injector injector = Guice.createInjector(new FAModule());
        FAService faService = injector.getInstance(FAService.class);
        faService.start();
        faService.stop();
    }
}
