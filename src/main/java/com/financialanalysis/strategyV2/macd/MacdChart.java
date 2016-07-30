package com.financialanalysis.strategyV2.macd;

import com.beust.jcommander.internal.Lists;
import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.Account;
import com.financialanalysis.data.Action;
import com.financialanalysis.data.StockFA;
import com.financialanalysis.graphing.StockChart;
import com.financialanalysis.strategyV2.Chart;
import lombok.extern.log4j.Log4j;
import org.joda.time.DateTime;

import java.util.List;

import static com.financialanalysis.analysis.AnalysisFunctions.macd;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getVolume;
import static com.financialanalysis.analysis.AnalysisTools.ave;
import static com.financialanalysis.analysis.AnalysisTools.addConst;

@Log4j
public class MacdChart extends Chart {
    private final static int DEFAULT_FAST_PERIOD = 12;
    private final static int DEFAULT_SLOW_PERIOD = 26;
    private final static int DEFAULT_SIGNAL_PERIOD = 9;

    @Override
    public List<StockChart> getCharts(StockFA stock, Account account) {
        List<DateTime> dates = getDates(stock.getHistory());
        double[] volume = getVolume(stock.getHistory());
        double[] closingPrices = getClosingPrices(stock.getHistory());
        double ave = ave(closingPrices);

        AnalysisFunctionResult results = macd(closingPrices, DEFAULT_FAST_PERIOD, DEFAULT_SLOW_PERIOD, DEFAULT_SIGNAL_PERIOD);
        double[] macd = addConst(results.getMacd(), ave);
        double[] macdSignal = addConst(results.getMacdSignal(), ave);

        String info = String.format("%s_%.2f", stock.getSymbol(), account.getPercentageGainLoss());
        StockChart stockChart = new StockChart("MACD_" + info);
        stockChart.setYAxis("Price");
        stockChart.setXAxis("Date");
        stockChart.addCandles(stock.getHistory());
        stockChart.addVolume(dates, volume);
        stockChart.addXYLine(dates, macd, "MACD");
        stockChart.addXYLine(dates, macdSignal, "Signal");
        stockChart.addHorizontalLine(ave, "Ave");

        for(Action action : account.getActivity()) {
            stockChart.addVerticalLine(action.getDate(), action.getAction(), action);
        }

        List<StockChart> list = Lists.newArrayList();
        list.add(stockChart);

        return list;
    }
}
