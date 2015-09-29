package com.financialanalysis.strategy;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.Signal;
import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import lombok.SneakyThrows;
import org.joda.time.DateTime;
import yahoofinance.Stock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.financialanalysis.analysis.AnalysisFunctions.bollingerBands;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getOpenPrices;
import static com.financialanalysis.analysis.AnalysisTools.getVolume;

/**
 * When volatility has increased greater than X% of open, buy the close of that day.
 */
public class BollingerBandsVolatilityStrategy extends AbstractStrategy {
    private static final int DEFAULT_PERIOD = 21;

    public BollingerBandsVolatilityStrategy(Account account) {
        super(account);
    }

    @Override
    @SneakyThrows
    public StrategyOutput runStrategy(StrategyInput input) {
        Map<String, Account> accounts = new HashMap<>();
        Map<String, List<Signal>> signals = new HashMap<>();

        for(StockFA stock : input.getStocks()) {
            Account runAccount = account.copy();
            runAccount.setSymbol(stock.getSymbol()); // This probably isn't required

            double[] closingPrices = getClosingPrices(stock.getHistory());
            double[] openPrices = getOpenPrices(stock.getHistory());
            double[] volume = getVolume(stock.getHistory());
            List<DateTime> dates = getDates(stock.getHistory());

            AnalysisFunctionResult results = bollingerBands(openPrices, DEFAULT_PERIOD);
            double[] high = results.getBbHigh();
            double[] mid = results.getBbMid();
            double[] low = results.getBbLow();

            List<Signal> signalList = new ArrayList<>();
            for(int i = 1; i < results.getBbMid().length; i++) {
                double currentPrice = closingPrices[i];
                double prevPrice = closingPrices[i-1];
                runAccount.rebalance(currentPrice);

                double prevVol = (high[i-1] - low[i-1]) / mid[i-1];
                double curVol = (high[i] - low[i]) / mid[i];

                // Get this signal for this day
                Signal signal = checkIfSignalExists(
                        prevVol,
                        curVol,
                        volume[i-1],
                        volume[i],
                        prevPrice,
                        currentPrice,
                        currentPrice,
                        dates.get(i)
                );
                if(signal == null) continue;
                signalList.add(signal);

                if(signal.isLong()) {
                    runAccount.buyAll(currentPrice, dates.get(i), stock.getSymbol());
                } else if(signal.isShort()) {
                    runAccount.sellAll(currentPrice, dates.get(i), stock.getSymbol());
                }

                //Sort list so its in chrono order
                Collections.sort(signalList, (s1, s2) -> (int) (s1.getDate().isBefore(s2.getDate()) ? -1 : 1));

                signals.put(stock.getSymbol(), signalList);
                accounts.put(stock.getSymbol(), runAccount);
            }
        }

        StrategyOutput output = new StrategyOutput(accounts, signals, null);
        return output;
    }

    private Signal checkIfSignalExists(double prevVol,
                                       double curVol,
                                       double prevVolume,
                                       double curVolume,
                                       double prevPrice,
                                       double curPrice,
                                       double tradePrice,
                                       DateTime date) {
        boolean LONG = (prevVol < 0.1) && (curVol >= 0.1) && (prevVolume < curVolume) && (prevPrice < curPrice);
        boolean SHORT = (prevVolume > curVolume) && (prevPrice > curPrice);

        if(LONG) {
            return new Signal(Signal.LONG, tradePrice, date);
        } else if(SHORT) {
            return new Signal(Signal.SHORT, tradePrice, date);
        }
        return null;
    }
}
