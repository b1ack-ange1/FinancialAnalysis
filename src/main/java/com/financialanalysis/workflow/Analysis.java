package com.financialanalysis.workflow;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.graphing.LineChart;
import com.financialanalysis.strategy.StrategyOutput;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Log4j
public class Analysis {
    private final Account account;

    private String symbols = "";
    private ExecutorService executorService = Executors.newFixedThreadPool(50);

    // synchronized variable that all threads store results in
    private static List<StrategyOutput> strategyOutputs = new ArrayList<>();

    @Inject
    public Analysis(Account account) {
        this.account = account;
    }

    public void analyzeStocks(List<StockFA> stocks) {
//        StrategyRunner runner = new StrategyRunner(account.copy());
//        runner.setStockFAs(stocks);
//        executorService.execute(runner);
    }

    public void reportResultsByStock() {
//        // Wait for everthing to be finished
//        executorService.shutdown();
//        try {
//            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//        } catch (InterruptedException e) {
//            // Stop everything and report what we have
//            log.error("All threads did not shut down, killing them ...");
//            executorService.shutdownNow();
//        }
//
//        // Report results
//        List<Account> allAccounts = new ArrayList<>();
//        for(StrategyOutput result : getOutput()) {
//            Map<Symbol, Account> accounts = result.getAccounts();
//            List<Account> accountList = new ArrayList<>(accounts.values());
//            for(Account account : accountList) {
//                if(!account.getActivity().isEmpty()) {
//                    allAccounts.add(account);
//                    symbols += "\"" + account.getSymbol() + "\",";
//                }
//            }
//        }
//
//        Collections.sort(allAccounts, (a1, a2) -> (int) ( (a1.getPercentageGainLoss() * 100.0) - (a2.getPercentageGainLoss() * 100.0)));
//        for(Account account : allAccounts) {
//            if(!account.getActivity().isEmpty()) {
////                reportAccount(account);
//            }
//        }
//        log.info(symbols.replaceAll("\n", ""));
//        reportActivityDates(allAccounts);
//        reportAverage(allAccounts);
//        reportReturnsPercentiles(allAccounts);
    }

    private void reportAccount(Account account) {
        if(!account.getActivity().isEmpty()) {
            log.info(account.getAll());
        }
    }

    private void reportAverage(List<Account> accounts) {
        double gainNotZero = 0;
        double numNotZero = 0;

        double gain = 0;
        double num = 0;
        double posGain = 0;
        for(Account account : accounts) {
            double gainLossP = account.getPercentageGainLoss();

            if(account.getActivity().isEmpty()) continue;
            if(gainLossP > 0) posGain++;
            gain += gainLossP;
            num++;

            if(gainLossP < -2.0 || 2.0 < gainLossP) {
                gainNotZero += gainLossP;
                numNotZero++;
            }
        }
        if(num == 0) return;
        log.info(String.format("Positive Return:                %.2f%%", posGain / num * 100));
        log.info(String.format("Gain/Loss Ave:                  %.2f%%", gain/num));
        log.info(String.format("Gain/Loss NOT -2%% to 2%% Ave:    %.2f%%", gainNotZero/numNotZero));
        log.info(String.format("Number of meaningful trades:    %.2f%% %d/%d ", numNotZero * 100.0 / num, (int) numNotZero, (int) num));
    }

    private void reportActivityDates(List<Account> accounts) {
        Map<Integer, Integer> map = new HashMap<>();
        int sumDays = 0;
        int total = 0;
        for(Account account : accounts) {
            int numDays = account.getDayBetweenFirstAndLastAction();
            if(numDays > 60) continue; // Probably not a valid data point
            sumDays += numDays;
            total++;
            if(!map.containsKey(numDays)) {
                map.put(numDays, 1);
            } else {
                int val = map.get(numDays) + 1;
                map.put(numDays, val);
            }
        }

        List<Integer> numDaysList = new ArrayList<>(map.keySet());
        Collections.sort(numDaysList, (d1, d2) -> d1 - d2);

        for(Integer numDays : numDaysList) {
            double percentage = (double) map.get(numDays) * 100.0 / (total);
            String info = String.format("Num Days: %2d %4.2f%% %4d/%d", numDays, percentage, map.get(numDays), total);
            log.info(info);
        }
        log.info(String.format("Average Days: %.2f", (double) sumDays / (double) total));
    }

    private void reportReturnsPercentiles(List<Account> accounts) {
        if(accounts.isEmpty() || accounts.size() < 2) return;

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for(Account account : accounts) {
            int p = (int) (account.getPercentageGainLoss());
            if(p < min) min = p;
            if(p > max) max = p;
        }

        int numPoints = max - min + 1;
        double[] percentile;
        int[] freq;

        // Case where min and max are greater than 0
        if(min > 0) {
            freq = new int[max + numPoints];
            percentile = new double[max + numPoints];
        } else {
        // Case where min is less than 0 and max is greater than 0
            freq = new int[numPoints];
            percentile = new double[numPoints];
        }

        for(Account account : accounts) {
            int index = (int) account.getPercentageGainLoss();
            freq[Math.abs(min) + index]++;
        }

        for(int i = 0; i < freq.length; i++) {
            percentile[i] = freq[i] * 100.0 / accounts.size();
        }

        for(int i = 0; i < percentile.length; i++) {
            if(percentile[i] == 0.0) continue;
            String format = String.format("Percentile: %3d%% %3.2f%% %3d/%d", i - Math.abs(min), percentile[i], freq[i], accounts.size());
            log.info(format);
        }

        double[] x = new double[numPoints];
        double[] y = new double[numPoints];

        int xIndex = 0;
        for(int i = min; i <= max; i++) {
            x[xIndex] = i;
            y[xIndex] = percentile[xIndex];
            xIndex++;
        }

        LineChart lineChart = new LineChart("Distribution of Returns");
        lineChart.setXAxis("Percentage Gain/Loss");
        lineChart.setYAxis("%");
        lineChart.addXYLine(x, y, "Percentage Gain/Loss");
        lineChart.render();
    }

    public static synchronized void addOutput(List<StrategyOutput> results) {
        strategyOutputs.addAll(results);
    }

    public static synchronized List<StrategyOutput> getOutput() {
        return strategyOutputs;
    }
}












