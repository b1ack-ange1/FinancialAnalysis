package com.financialanalysis.strategy;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.Signal;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.graphing.StockChart;
import com.financialanalysis.graphing.Line;
import com.financialanalysis.graphing.Point;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.financialanalysis.analysis.AnalysisBaseFunctions.highest;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.lowest;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.max;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.min;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.sma;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getHighPrices;
import static com.financialanalysis.analysis.AnalysisTools.getLowPrices;
import static com.financialanalysis.analysis.AnalysisTools.getOpenPrices;

@Log4j
public class SRStrategy extends AbstractStrategy {
    private static final int DEFAULT_SMA_PERIOD = 20;
    private static final int LOOK_AROUND_PERIOD = 8;
    private static final int SHORT_TERM = 10;
    private static final int INTERMEDIATE_TERM = 20; //60;
    private static final int LONG_TERM = 60;

    public SRStrategy(Account account) {
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
            List<DateTime> dates = getDates(stock.getHistory());

            double[] highest = highest(highPrices, LOOK_AROUND_PERIOD);
            double[] lowest = lowest(lowPrices, LOOK_AROUND_PERIOD);
            double[] sma = sma(closingPrices, DEFAULT_SMA_PERIOD);

            Map<Integer, Point> maxPoints = max(highPrices, 3, 3);
            Map<Integer, Point> minPoints = min(lowPrices, 3, 3);

//            int trendLength = 60;
//            for(int i = trendLength; i < closingPrices.length; i++) {
//                SimpleRegression lows = getTrend(minPoints, i, trendLength);
//                Line lowsTrendLine = new Line(lows);
//
//                if(lows.getRSquare() > 0.9) {
//                    log.info("RSquare: " + lows.getRSquare());
//                    StockChart graph = new StockChart("S/R " + stock.getSymbol() + " " + lows.getRSquare());
//                    graph.addXYLine(closingPrices, "Close");
//                    graph.addXYLine(
//                            lowsTrendLine.generateXValues(i - trendLength, i),
//                            lowsTrendLine.generateYValues(i - trendLength, i),
//                            "Trend-" + i
//                    );
//                    graph.render();
//                }
//            }

            int startIdx = 130;
            SimpleRegression lowsLT = getTrend(minPoints, startIdx, LONG_TERM);
            SimpleRegression highsLT = getTrend(maxPoints, startIdx, LONG_TERM);
            Line lowsLTSR = new Line(lowsLT);
            Line highsLTSR = new Line(highsLT);

            int startIdxIT = startIdx;
            SimpleRegression lowsIT = getTrend(minPoints, startIdxIT, INTERMEDIATE_TERM);
            SimpleRegression highsIT = getTrend(maxPoints, startIdxIT, INTERMEDIATE_TERM);
            Line lowsITSR = new Line(lowsIT);
            Line highsITSR = new Line(highsIT);

            int startIdxST = startIdxIT + SHORT_TERM;
            SimpleRegression lowsST = getTrend(minPoints, startIdxST, SHORT_TERM);
            SimpleRegression highsST = getTrend(maxPoints, startIdxST, SHORT_TERM);
            Line lowsSTTR = new Line(lowsST);
            Line highsSTTY = new Line(highsST);

            log.info(stock.getSymbol());
            if(lowsLT.getRSquare() > 0.9 && lowsLT.getSlope() > 0) {

                if (0.3 < lowsIT.getSlope() && lowsIT.getSlope() < 0.6 && lowsIT.getRSquare() > 0.9) {

                    if (lowsST.getSlope() < 0 && lowsIT.getRSquare() > 0.9) {

                        String title = String.format(
                                "%s %s Low (r2: %.2f) (m: %.2f) High (r2: %.2f) (m: %.2f)\n" +
                                "%s %s Low (r2: %.2f) (m: %.2f) High (r2: %.2f) (m: %.2f)\n" +
                                "%s %s Low (r2: %.2f) (m: %.2f) High (r2: %.2f) (m: %.2f)",
                                stock.getSymbol(), "Short", lowsIT.getRSquare(), lowsIT.getSlope(), highsIT.getRSquare(), highsIT.getSlope(),
                                stock.getSymbol(), "Inter", lowsST.getRSquare(), lowsST.getSlope(), highsST.getRSquare(), highsST.getSlope(),
                                stock.getSymbol(), "Long", lowsLT.getRSquare(), lowsLT.getSlope(), highsLT.getRSquare(), highsLT.getSlope());


                        StockChart stockChart = new StockChart(title);
//                        stockChart.addXYLine(closingPrices, "Close");
//                        stockChart.addXYLine(
//                                lowsLTSR.generateXValues(startIdx - LONG_TERM, startIdx),
//                                lowsLTSR.generateYValues(startIdx - LONG_TERM, startIdx),
//                                "Long"
//                        );
//                        stockChart.addXYLine(
//                                lowsITSR.generateXValues(startIdxIT - INTERMEDIATE_TERM, startIdxIT),
//                                lowsITSR.generateYValues(startIdxIT - INTERMEDIATE_TERM, startIdxIT),
//                                "Inter"
//                        );
//                        stockChart.addXYLine(
//                                lowsSTTR.generateXValues(startIdxST - SHORT_TERM, startIdxST),
//                                lowsSTTR.generateYValues(startIdxST - SHORT_TERM, startIdxST),
//                                "Short"
//                        );
                        stockChart.render();
                    }

                }
            }







//            int[] trendLengths = {SHORT_TERM, INTERMEDIATE_TERM, LONG_TERM};
//            for(int i = 0; i < trendLengths.length; i++) {
//                int len = trendLengths[i];
//                SimpleRegression lows = getTrend(minPoints, startIdx, len);
//                SimpleRegression highs = getTrend(maxPoints, startIdx, len);
//
//                log.info(lows.getSlope());
//                if(!(0.4 < lows.getSlope() && lows.getSlope() < 0.5)) continue;
//
//                if(lows.getRSquare() > 0.9) {
//                    Line lowsTR = new Line(lows);
//                    Line highsTR = new Line(highs);
//
//                    String title = String.format("%s %d Low (r2: %.2f) (m: %.2f) High (r2: %.2f) (m: %.2f)",
//                            stock.getSymbol(), len, lows.getRSquare(), lows.getSlope(), highs.getRSquare(), highs.getSlope());
//                    StockChart graph = new StockChart(title);
//                    graph.addXYLine(closingPrices, "Close");
//                    graph.addXYLine(
//                            lowsTR.generateXValues(startIdx - len, startIdx),
//                            lowsTR.generateYValues(startIdx - len, startIdx),
//                            "LowTrend-" + i
//                    );
//                    graph.addXYLine(
//                            highsTR.generateXValues(startIdx - len, startIdx),
//                            highsTR.generateYValues(startIdx - len, startIdx),
//                            "HighTrend-" + i
//                    );
//                    graph.render();
//                }
//            }

            accounts.put(stock.getSymbol(), runAccount);
        }

        StrategyOutput output = new StrategyOutput(accounts, signals, null);
        return output;
    }

    private SimpleRegression getTrend(Map<Integer, Point> points, int startIdx, int period) {
        SimpleRegression sr = new SimpleRegression();

        for(int i = startIdx; i >= 0 && i >= startIdx - period ; i--) {
            if(points.containsKey(i)) {
                sr.addData(points.get(i).getX(), points.get(i).getY());
            }
        }

        return sr;
    }
}
