package com.financialanalysis.strategy;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.Signal;
import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.google.inject.Inject;
import flanagan.analysis.CurveSmooth;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import yahoofinance.Stock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.financialanalysis.analysis.AnalysisBaseFunctions.averageTrueRange;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.highest;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.lowest;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.sma;
import static com.financialanalysis.analysis.AnalysisFunctions.bollingerBands;
import static com.financialanalysis.analysis.AnalysisTools.add;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getHighPrices;
import static com.financialanalysis.analysis.AnalysisTools.getLowPrices;
import static com.financialanalysis.analysis.AnalysisTools.getOpenPrices;
import static com.financialanalysis.analysis.AnalysisTools.mult;
import static com.financialanalysis.analysis.AnalysisTools.sub;

@Log4j
public class SqueezeStrategy extends AbstractStrategy {
    private static final int DEFAULT_PERIOD = 20;
    private static final double MULT_KC = 1.5;

    @Inject
    public SqueezeStrategy(Account account) {
        super(account);
    }

    @Override
    @SneakyThrows
    public StrategyOutput runStrategy(StrategyInput input) {
        Map<String, Account> accounts = new HashMap<>();
        Map<String, List<Signal>> signals = new HashMap<>();

        for(StockFA stock : input.getStocks()) {
            Account runAccount = account.copy();
            runAccount.setSymbol(stock.getSymbol());

            double[] closingPrices = getClosingPrices(stock.getHistory());
            double[] openPrices = getOpenPrices(stock.getHistory());
            double[] highPrices = getHighPrices(stock.getHistory());
            double[] lowPrices = getLowPrices(stock.getHistory());
            List<DateTime> dates = getDates(stock.getHistory());

            // Calculate bb
            AnalysisFunctionResult results = bollingerBands(closingPrices, DEFAULT_PERIOD);
            double[] upperBB = results.getBbHigh();
            double[] lowerBB = results.getBbLow();

            // Calculate KC
            double[] ma = sma(closingPrices, DEFAULT_PERIOD);
            double[] range = averageTrueRange(highPrices, lowPrices, DEFAULT_PERIOD);
//            double[] range = sub(openPrices, closingPrices);
            double[] rangeMa = sma(range, DEFAULT_PERIOD);

            double[] tmp = mult(rangeMa, MULT_KC);
            double[] upperKC = add(ma, tmp);
            double[] lowerKC = sub(ma, tmp);

            // Calculate squeeze
            boolean[] sqzOn = getSqzOn(lowerBB, lowerKC, upperBB, upperKC);
            boolean[] sqzOff = getSqzOff(lowerBB, lowerKC, upperBB, upperKC);

            // Do something??
            double[] highest = highest(highPrices, DEFAULT_PERIOD);
            double[] lowest = lowest(lowPrices, DEFAULT_PERIOD);
            double[] y = new double[openPrices.length];
            double[] x = new double[openPrices.length];
            for(int i = 0; i < openPrices.length; i++) {
                y[i] = closingPrices[i] - avg(highest[i], lowest[i], ma[i]);
                x[i] = i;
            }

            // Smooth previous data
            CurveSmooth curveSmooth = new CurveSmooth(x, y);
            double[] smoothed = curveSmooth.savitzkyGolay(DEFAULT_PERIOD);
//            double[] smoothed = curveSmooth.movingAverage(DEFAULT_PERIOD);

//            for(int i = 0; i < smoothed.length; i++) {
//                String format = String.format("[%3d] %2.2f %s", i, smoothed[i], dates.get(i));
//                log.info(format);
//            }

            List<Signal> signalList = new ArrayList<>();
            for(int i = 1; i < y.length; i++) {
                double currentPrice = closingPrices[i];
                runAccount.rebalance(currentPrice);

//                String format = String.format("[%3d] %2.2f %s %s", i, smoothed[i], dates.get(i), sqzOn[i] ? "true" : "false");
//                log.info(format);

                Signal signal = checkIfSignalExists(smoothed[i-1], smoothed[i], sqzOn[i-1], sqzOn[i], currentPrice, dates.get(i));
                if(signal == null) continue;
                signalList.add(signal);

                if(signal.isLong()) {
                    runAccount.buyAll(currentPrice, dates.get(i), stock.getSymbol());
                } else if(signal.isShort()) {
                    runAccount.sellAll(currentPrice, dates.get(i), stock.getSymbol());
                }
            }

            //Sort list so its in chrono order
            Collections.sort(signalList, (s1, s2) -> (int) (s1.getDate().isBefore(s2.getDate()) ? -1 : 1));

            signals.put(stock.getSymbol(), signalList);
            accounts.put(stock.getSymbol(), runAccount);
        }
        StrategyOutput output = new StrategyOutput(accounts, signals, null);
        return output;
    }

    private Signal checkIfSignalExists(double prevS, double curS, boolean prevSqzOn, boolean sqzOn, double price, DateTime date) {
        // If momentum is positive and rising and squeeze has just released
        if(curS > 0 && curS > prevS && prevSqzOn == true && sqzOn == false) {
            return new Signal(Signal.LONG, price, date);

        // If the squeeze momentum dips below 3% of prevS
        } else if(curS > 0 && curS < prevS && (curS * 0.9 < prevS)) {
            return new Signal(Signal.SHORT, price, date);
        }


//        if(curS > 0) {
//            if(curS > prevS && prevSqzOn == true && sqzOn == false) {
//                return new Signal(Signal.LONG, price, date, this);
//            }
//        } else if(curS < 0) {
//            if(curS < prevS) {
//                return new Signal(Signal.SHORT, price, date, this);
//            }
//        }
        return null;
    }

    private boolean[] getSqzOn(double[] lowerBB, double[] lowerKC, double[] upperBB, double[] upperKC) {
        int length = lowerBB.length;
        boolean[] res = new boolean[length];
        for(int i = 0; i < length; i++) {
            res[i] = (lowerBB[i] > lowerKC[i]) && (upperBB[i] < upperKC[i]);
            String format = String.format("%s = (%2.2f > %2.2f) && (%2.2f < %2.2f)",
                    res[i] ? "true" : "false", lowerBB[i], lowerKC[i], upperBB[i], upperKC[i]);
//            log.info(format);
//            log.info(i + " " + res[i]);
        }
        return res;
    }

    private boolean[] getSqzOff(double[] lowerBB, double[] lowerKC, double[] upperBB, double[] upperKC) {
        int length = lowerBB.length;
        boolean[] res = new boolean[length];
        for(int i = 0; i < length; i++) {
            res[i] = (lowerBB[i] < lowerKC[i]) && (upperBB[i] > upperKC[i]);
        }
        return res;
    }

    private double avg(double a, double b, double c) {
        return (a + b + c) / 3.0;
    }
}






