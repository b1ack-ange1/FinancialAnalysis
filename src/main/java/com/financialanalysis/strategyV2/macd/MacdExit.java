package com.financialanalysis.strategyV2.macd;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.strategyV2.Exit;

import java.util.List;

import static com.financialanalysis.analysis.AnalysisFunctions.macd;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;

public class MacdExit extends Exit {
    private final static int DEFAULT_FAST_PERIOD = 12;
    private final static int DEFAULT_SLOW_PERIOD = 26;
    private final static int DEFAULT_SIGNAL_PERIOD = 9;

    @Override
    public boolean shouldExit(List<StockPrice> stockPrices) {
        double[] closingPrices = getClosingPrices(stockPrices);
        AnalysisFunctionResult results = macd(closingPrices, DEFAULT_FAST_PERIOD, DEFAULT_SLOW_PERIOD, DEFAULT_SIGNAL_PERIOD);
        double[] macd = results.getMacd();
        double[] macdSignal = results.getMacdSignal();
        double[] macdHist = results.getMacdHist();

        int i = macd.length - 1;

        if((macd[i-1] > macdSignal[i-1]) && (macd[i] < macdSignal[i])) {
            return true;
        }

        return false;
    }
}
