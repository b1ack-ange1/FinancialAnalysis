package com.financialanalysis.strategy;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.Signal;
import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import yahoofinance.Stock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.financialanalysis.analysis.AnalysisBaseFunctions.sma;
import static com.financialanalysis.analysis.AnalysisFunctions.adx;
import static com.financialanalysis.analysis.AnalysisFunctions.macd;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getHighPrices;
import static com.financialanalysis.analysis.AnalysisTools.getLowPrices;
import static com.financialanalysis.analysis.AnalysisTools.getOpenPrices;

/**
 * Strategy:
 * When macdTA_LIB crosses above signal and macdTA_LIB < 0, buy at close of same day
 * When macdTA_LIB crosses below signal and macdTA_LIB > 0, sell at close of same day
 */
@Log4j
public class MACDStrategy extends AbstractStrategy {
    private final static int DEFAULT_FAST_PERIOD = 12;
    private final static int DEFAULT_SLOW_PERIOD = 26;
    private final static int DEFAULT_SIGNAL_PERIOD = 9;
    private final static int ADX_PERIOD = 14;

    @Inject
    public MACDStrategy(Account account) {
        super(account);
    }

    @Override
    @SneakyThrows
    public StrategyOutput runStrategy(StrategyInput input) {
        Map<String, Account> accounts = new HashMap<>();
        Map<String, List<Signal>> signals = new HashMap<>();

        for (StockFA stock : input.getStocks()) {
            Account runAccount = account.copy();
            runAccount.setSymbol(stock.getSymbol()); // This probably isn't required

            double[] closingPrices = getClosingPrices(stock.getHistory());
            double[] openPrices = getOpenPrices(stock.getHistory());
            double[] lowPrices = getLowPrices(stock.getHistory());
            double[] highPrices = getHighPrices(stock.getHistory());
            List<DateTime> dates = getDates(stock.getHistory());

            // Compute macd series
            AnalysisFunctionResult results = macd(closingPrices, DEFAULT_FAST_PERIOD, DEFAULT_SLOW_PERIOD, DEFAULT_SIGNAL_PERIOD);
            double[] macd = results.getMacd();
            double[] macdSignal = results.getMacdSignal();
            double[] macdHist = results.getMacdHist();

            AnalysisFunctionResult result = adx(lowPrices, highPrices, closingPrices, ADX_PERIOD);
            double[] adx = result.getAdx();
            double[] dmPlus = result.getDmPlus();
            double[] dmMinus = result.getDmMinus();

            double[] sma = sma(closingPrices, 100);

            int startIndex = Math.max(result.getBeginIndex(), results.getBeginIndex()) + 1;

            List<Signal> signalList = new ArrayList<>();
            // Backtest just this indicator and generate signals
            for (int i = startIndex; i < macd.length; i++) {
                double currentPrice = closingPrices[i];
                runAccount.rebalance(currentPrice);

                boolean aboveTrend = currentPrice > sma[i];
                boolean belowTrend = currentPrice < sma[i];

                boolean macdCrossLong = (macd[i] < 0) && (macd[i] > macdSignal[i]) && (macd[i - 1] < macdSignal[i - 1]);
                boolean macdCrossShort = (macd[i] > 0) && (macd[i - 1] > macdSignal[i - 1]) && (macd[i] < macdSignal[i]);

                boolean adxOver = adx[i] > 20;
                boolean adxCrossLong = (dmPlus[i] > dmMinus[i]) && (dmPlus[i - 1] < dmMinus[i - 1]);
                boolean adxCrossShort = (dmPlus[i - 1] > dmMinus[i - 1]) && (dmPlus[i] < dmMinus[i]);
                boolean adxPos = (dmPlus[i] > dmMinus[i]);

                boolean LONG = macdCrossLong && adxOver && adxPos && aboveTrend;
                boolean SHORT = macdCrossShort;

                if (LONG) {
                    runAccount.buyAll(currentPrice, dates.get(i), stock.getSymbol());
                    signalList.add(new Signal(Signal.LONG, currentPrice, dates.get(i)));
                } else if (SHORT) {
                    runAccount.sellAll(currentPrice, dates.get(i), stock.getSymbol());
                    signalList.add(new Signal(Signal.SHORT, currentPrice, dates.get(i)));
                }
            }

            //Sort list so its in chrono order
            Collections.sort(signalList, (s1, s2) -> (int) (s1.getDate().isBefore(s2.getDate()) ? -1 : 1));

            signals.put(stock.getSymbol(), signalList);
            accounts.put(stock.getSymbol(), runAccount);
        }

        StrategyOutput output = new StrategyOutput(accounts, signals, null);
        output.setStrategy("MACD_INDICATOR");
        return output;
    }
}
