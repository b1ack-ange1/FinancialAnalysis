package com.financialanalysis.strategy;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.StockFA;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;
import yahoofinance.Stock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getOpenPrices;

/**
 * Strategy:
 * Using open price, when macdTA_LIB crosses above signal and macdTA_LIB < 0, buy at close of same day
 * Using open price, when macdTA_LIB crosses below signal and macdTA_LIB > 0, sell at close of same day
 */
@Log4j
public class MACDStrategyOld extends AbstractStrategy {
    private final static int DEFAULT_FAST_PERIOD = 12;
    private final static int DEFAULT_SLOW_PERIOD = 26;
    private final static int DEFAULT_SIGNAL_PERIOD = 9;

    @Inject
    public MACDStrategyOld(Account account) {
        super(account);
    }

    @Override
    @SneakyThrows
    public StrategyOutput runStrategy(StrategyInput input) {
        Map<String, Account> accounts = new HashMap<>();
        for(StockFA stock : input.getStocks()) {
            Account runAccount = account.copy();

            double[] closingPrices = getClosingPrices(stock.getHistory());
            double[] openPrices = getOpenPrices(stock.getHistory());
            List<DateTime> dates = getDates(stock.getHistory());

            Account resAccount = performRunWithParams(
                    openPrices,
                    DEFAULT_FAST_PERIOD,
                    DEFAULT_SLOW_PERIOD,
                    DEFAULT_SIGNAL_PERIOD,
                    closingPrices,
                    runAccount,
                    dates
            );
            resAccount.setSymbol(stock.getSymbol());

            accounts.put(stock.getSymbol(), resAccount);
        }
        StrategyOutput output = new StrategyOutput(accounts, null, null);
        return output;
    }
    private Account performRunWithParams(double[] prices, int fastP, int slowP, int signalP,
                                         double[] tradeOnPrice, Account account, List<DateTime> dates) {

//        // I want to look at the past 150 days of activity
//        int startOfPeriod = dates.size() - 150;
//        if(startOfPeriod < 0) throw new RuntimeException("No enough data");
//
//        // End of the period is of course the last day we have prices for!
//        int endOfPeriod = prices.length;
//
//        for(int i = startOfPeriod; i < endOfPeriod; i++) {
//            AnalysisFunctionResult results = macdTA_LIB(prices, fastP, slowP, signalP, 0, i);
//            double currentPrice = tradeOnPrice[i];
//
//            if(buySignal(results.getMacd()[results.getEndIndex()-1], results.getMacdSignal()[results.getEndIndex()-1])) {
//                account.buyAll(currentPrice, dates.get(i));
//            } else if(sellSignal(results.getMacd()[results.getEndIndex()-1], results.getMacdSignal()[results.getEndIndex()-1])) {
//                account.sellAll(currentPrice, dates.get(i));
//            }
//            account.rebalance(currentPrice);
//        }
//        return account;


//        int length = prices.length;
//
//        double[] outMACD = new double[length];
//        double[] outMACDSignal = new double[length];
//        double[] outMACDHist = new double[length];
//
//        AnalysisFunctionResult results = macdTA_LIB(prices, fastP, slowP, signalP, 0, length - 1, outMACD, outMACDSignal, outMACDHist);
//
//        for(int i = results.getBeginIndex(); i < results.getEndIndex(); i++) {
//            double currentPrice = tradeOnPrice[i + results.getBeginIndex()];
//            DateTime currentDate = dates.get(i + results.getBeginIndex());
//
//            if(buySignal(outMACD[i], outMACDSignal[i])) {
//                account.buyAll(currentPrice, currentDate);
//            } else if(sellSignal(outMACD[i], outMACDSignal[i])) {
//                account.sellAll(currentPrice, currentDate);
//            }
//            account.rebalance(currentPrice);
//        }
//        return account;

//        AnalysisFunctionResult emaResults = emaTA_LIB(prices, slowP);

//        int length = prices.length; // + 33;
//
//        double[] outMACD = new double[length];
//        double[] outMACDSignal = new double[length];
//        double[] outMACDHist = new double[length];
//
//        double[] testPr = new double[length];
//        for(int i = 0; i < length; i++) {
//            if(i < prices.length) {
//                testPr[i] = prices[i];
//            }else{
//                testPr[i] = 0;
//            }
//        }
//
////        AnalysisFunctionResult results = macdTA_LIB(testPr, fastP, slowP, signalP, 0, length - 1, outMACD, outMACDSignal, outMACDHist);
//        MInteger outBegIdx = new MInteger();
//        MInteger outNBElement = new MInteger();
//
//        /**
//         * For the first period intervals, take the average of them and use as the first value of the said ema
//         */
//        macd(0, length - 1,prices, fastP, slowP, signalP, outBegIdx, outNBElement, outMACD, outMACDSignal, outMACDHist);
//
//
//        double sum = 0;
//        for(double d : outMACD) sum += d;
//        log.info("[SUM] " + sum);
//
//        for(int i = 0; i < /*prices.length*//*results.getEndIndex()*/outNBElement.value-1; i++) {
//            double currentPrice = tradeOnPrice[i];
//
//            if(buySignal(outMACD[i], outMACDSignal[i])) {
//                account.buyAll(currentPrice, dates.get(i));
//            } else if(sellSignal(outMACD[i], outMACDSignal[i])) {
//                account.sellAll(currentPrice, dates.get(i));
//            }
//            account.rebalance(currentPrice);
//        }
        return account;
    }

    private int getStartOfPeriod(List<DateTime> dates, int numDays) {
        return dates.size() - numDays;
    }

    // WHAT IS GOING ON HERE??
    private boolean buySignal(double macd, double macdSignal) {
        if(macd > 0 && macd < macdSignal) return true;
        else return false;
    }

    private boolean sellSignal(double macd, double macdSignal) {
        if(macd < 0 && macd > macdSignal) return true;
        else return false;
    }
}
