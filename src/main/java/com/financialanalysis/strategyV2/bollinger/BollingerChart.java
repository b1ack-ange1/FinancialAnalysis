package com.financialanalysis.strategyV2.bollinger;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.Account;
import com.financialanalysis.data.Action;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.graphing.StockChart;
import com.financialanalysis.strategyV2.Chart;
import org.joda.time.DateTime;

import java.util.List;

import static com.financialanalysis.analysis.AnalysisFunctions.bollingerBands;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getVolume;

public class BollingerChart extends Chart {
    private static final int BB_PERIOD = 21;

    @Override
    public StockChart getChart(StockFA stock, Account account) {
        List<DateTime> dates = getDates(stock.getHistory());
        double[] volume = getVolume(stock.getHistory());
        double[] closingPrices = getClosingPrices(stock.getHistory());

        AnalysisFunctionResult result = bollingerBands(closingPrices, BB_PERIOD);
        double[] high = result.getBbHigh();
        double[] mid = result.getBbMid();
        double[] low = result.getBbLow();

        String info = String.format("%s[%.2f]_[%.2f]", stock.getSymbol(), account.getPercentageGainLoss(), account.getAverageWeight());
        StockChart stockChart = new StockChart("Bollinger_" + info);
        stockChart.setYAxis("Price");
        stockChart.setXAxis("Date");
        stockChart.addCandles(stock.getHistory());
        stockChart.addVolume(dates, volume);
        stockChart.addXYLine(dates, high, "High");
        stockChart.addXYLine(dates, mid, "Mid");
        stockChart.addXYLine(dates, low, "Low");

        for(Action action : account.getActivity()) {
            stockChart.addAction(action);
        }

        return stockChart;
    }
}
