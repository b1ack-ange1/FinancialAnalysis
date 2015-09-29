package com.financialanalysis.strategy;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.Account;
import com.financialanalysis.data.Signal;
import com.financialanalysis.data.StockFA;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.financialanalysis.analysis.AnalysisFunctions.adx;
import static com.financialanalysis.analysis.AnalysisFunctions.macd;
import static com.financialanalysis.analysis.AnalysisFunctions.pvo;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getHighPrices;
import static com.financialanalysis.analysis.AnalysisTools.getLowPrices;
import static com.financialanalysis.analysis.AnalysisTools.getOpenPrices;
import static com.financialanalysis.analysis.AnalysisTools.getVolume;

/**
 *
 */
@Log4j
public class AMVStrategy extends AbstractStrategy {
    private final static int DEFAULT_FAST_PERIOD = 12;
    private final static int DEFAULT_SLOW_PERIOD = 26;
    private final static int DEFAULT_SIGNAL_PERIOD = 9;
    private final static int ADX_PERIOD = 14;
    private final static int EVENT_PERIOD = 2;

    public AMVStrategy(Account account) {
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
            double[] lowPrices = getLowPrices(stock.getHistory());
            double[] highPrices = getHighPrices(stock.getHistory());
            double[] volume = getVolume(stock.getHistory());
            List<DateTime> dates = getDates(stock.getHistory());

            AnalysisFunctionResult resultsMacd = macd(closingPrices, DEFAULT_FAST_PERIOD, DEFAULT_SLOW_PERIOD, DEFAULT_SIGNAL_PERIOD);
            double[] macd = resultsMacd.getMacd();
            double[] macdSignal = resultsMacd.getMacdSignal();
            double[] macdHist = resultsMacd.getMacdHist();

            AnalysisFunctionResult resultAdx = adx(lowPrices, highPrices, closingPrices, ADX_PERIOD);
            double[] adx = resultAdx.getAdx();
            double[] dmPlus = resultAdx.getDmPlus();
            double[] dmMinus = resultAdx.getDmMinus();

            //PVO is basically macd of the volume
            AnalysisFunctionResult resultsPvo = pvo(volume, DEFAULT_FAST_PERIOD, DEFAULT_SLOW_PERIOD, DEFAULT_SIGNAL_PERIOD);
            double[] pvo = resultsPvo.getPvo();
            double[] pvoSignal = resultsPvo.getPvoSignal();
            double[] pvoHist = resultsPvo.getPvoHist();

            int startIndex = 80;

            Map<Integer, DateTime> pvoCross = new HashMap<>();
            Map<Integer, DateTime> macdCross = new HashMap<>();
            Map<Integer, DateTime> adxCross = new HashMap<>();
            for(int i = startIndex; i < closingPrices.length; i++) {
                if(macd[i] > macdSignal[i] && macd[i-1] < macdSignal[i-1]) {
                    macdCross.put(i, dates.get(i));
                }

                if(pvo[i] > pvoSignal[i] && pvo[i-1] < pvoSignal[i-1]) {
                    pvoCross.put(i, dates.get(i));
                }

                if(dmPlus[i] > dmMinus[i] && dmPlus[i-1] < dmMinus[i-1]) {
                    adxCross.put(i, dates.get(i));
                }
            }

            List<Signal> signalList = new ArrayList<>();
            // Backtest just this indicator and generate signals
            for(int i = startIndex; i < closingPrices.length; i++) {
                double currentPrice = closingPrices[i];
                runAccount.rebalance(currentPrice);

                // Get this signal for this day

                //If there has been crosses in the last EVENT_PERIOD and no signal in last event period
                boolean LONG  = (pvo[i]     > pvoSignal[i])     && (pvo[i-1]       < pvoSignal[i-1])    && (pvo[i] < 0) &&
                                (dmPlus[i]  > dmMinus[i])       && (dmPlus[i-1]    < dmMinus[i-1])      &&
                                (macd[i]    > macdSignal[i])    && (macd[i-1]      < macdSignal[i-1])   && (macd[i] < 0);
                if(LONG) {
                    signalList.add(new Signal(Signal.LONG, currentPrice, dates.get(i)));
                }
            }

            //Sort list so its in chrono order
            Collections.sort(signalList, (s1, s2) -> (int) (s1.getDate().isBefore(s2.getDate()) ? -1 : 1));

            signals.put(stock.getSymbol(), signalList);

//            for(int i = startIndex; i < closingPrices.length; i++) {
//                double currentPrice = closingPrices[i];
//                runAccount.rebalance(currentPrice);
//
////                boolean LONG  = (pvo[i] > pvoSignal[i]) && (pvo[i-1] < pvoSignal[i-1]) && (dmPlus[i] > dmMinus[i]) && (macd[i] > macdSignal[i]);
//                boolean LONG  = (pvo[i]     > pvoSignal[i])     && (pvo[i-1]       < pvoSignal[i-1])    && (pvo[i] < 0) &&
//                                (dmPlus[i]  > dmMinus[i])       && (dmPlus[i-1]    < dmMinus[i-1])      &&
//                                (macd[i]    > macdSignal[i])    && (macd[i-1]      < macdSignal[i-1])   && (macd[i] < 0) &&
//                                (1 - (pvo[i]/pvoSignal[i]) > 0.13) &&
//                                (1 - (macd[i]/macdSignal[i]) > 0.13) &&
//                                (adx[i] > 20);
//
//
//                boolean SHORT = (dmPlus[i] < dmMinus[i]) || (macd[i] < macdSignal[i]) ||
//                                (closingPrices[i] < openPrices[i] && highPrices[i] > openPrices[i] * 1.1) ||    // Detect long upper shadow
//                                (closingPrices[i] < openPrices[i] * 0.97);                                    // Prices close more than 3% down
//
//                if(LONG) {
//                    runAccount.buyAll(currentPrice, dates.get(i), stock.getSymbol());
//                } else if(SHORT) {
//                    runAccount.sellAll(currentPrice, dates.get(i), stock.getSymbol());
//                }
//            }
//            accounts.put(stock, runAccount);
        }

        StrategyOutput output = new StrategyOutput(accounts, signals, null);
        return output;
    }
}
