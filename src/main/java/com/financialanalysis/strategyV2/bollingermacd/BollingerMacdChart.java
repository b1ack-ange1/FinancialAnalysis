package com.financialanalysis.strategyV2.bollingermacd;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.Account;
import com.financialanalysis.data.Action;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.graphing.StockChart;
import com.financialanalysis.strategyV2.Chart;
import org.joda.time.DateTime;

import java.util.List;

import static com.financialanalysis.analysis.AnalysisFunctions.bollingerBands;
import static com.financialanalysis.analysis.AnalysisFunctions.macd;
import static com.financialanalysis.analysis.AnalysisTools.addConst;
import static com.financialanalysis.analysis.AnalysisTools.ave;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getVolume;
import static com.financialanalysis.analysis.AnalysisTools.max;
import static com.financialanalysis.analysis.AnalysisTools.min;
import static com.financialanalysis.analysis.AnalysisTools.mult;

public class BollingerMacdChart extends Chart {
    private static final int BB_PERIOD = 21;

    private final static int DEFAULT_FAST_PERIOD = 12;
    private final static int DEFAULT_SLOW_PERIOD = 26;
    private final static int DEFAULT_SIGNAL_PERIOD = 9;

    @Override
    public StockChart getChart(StockFA stock, Account account) {
        List<DateTime> dates = getDates(stock.getHistory());
        double[] volume = getVolume(stock.getHistory());
        double[] closingPrices = getClosingPrices(stock.getHistory());
        double min = min(closingPrices);
        double max = max(closingPrices);
        double ave = ave(closingPrices);
        double[] aveA = new double[closingPrices.length];
        for(int i = 0; i < aveA.length; i++) {aveA[i] = ave;}

        AnalysisFunctionResult result = bollingerBands(closingPrices, BB_PERIOD);
        double[] high = result.getBbHigh();
        double[] mid = result.getBbMid();
        double[] low = result.getBbLow();

        AnalysisFunctionResult results = macd(closingPrices, DEFAULT_FAST_PERIOD, DEFAULT_SLOW_PERIOD, DEFAULT_SIGNAL_PERIOD);
        double[] macd = addConst(mult(results.getMacd(), 2.0), ave);
        double[] macdSignal = addConst(mult(results.getMacdSignal(), 2.0), ave);

        String info = String.format("%s[%.2f]_[%.2f]", stock.getSymbol(), account.getPercentageGainLoss(), account.getAverageWeight());
        StockChart stockChart = new StockChart("BollingerMacd_" + info);
        stockChart.setYAxis("Price");
        stockChart.setXAxis("Date");
        stockChart.addCandles(stock.getHistory());
        stockChart.addVolume(dates, volume);
        stockChart.addXYLine(dates, high, "High");
        stockChart.addXYLine(dates, mid, "Mid");
        stockChart.addXYLine(dates, low, "Low");
        stockChart.addXYLine(dates, macd, "MACD");
        stockChart.addXYLine(dates, macdSignal, "Signal");

        for(Action action : account.getActivity()) {
            stockChart.addAction(action);
        }

        stockChart.addXYLine(dates, aveA, "Average");

        return stockChart;
    }
}
