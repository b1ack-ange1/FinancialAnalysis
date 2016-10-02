package com.financialanalysis.strategyV2.macd;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.strategyV2.Entry;
import com.financialanalysis.strategyV2.EntryDecision;
import lombok.extern.log4j.Log4j;

import java.util.List;

import static com.financialanalysis.analysis.AnalysisFunctions.macd;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;

@Log4j
public class MacdEntry extends Entry {
    private final static int DEFAULT_FAST_PERIOD = 12;
    private final static int DEFAULT_SLOW_PERIOD = 26;
    private final static int DEFAULT_SIGNAL_PERIOD = 9;

    @Override
    public EntryDecision getEntryDecision(List<StockPrice> stockPrices) {
        double[] closingPrices = getClosingPrices(stockPrices);
        AnalysisFunctionResult results = macd(closingPrices, DEFAULT_FAST_PERIOD, DEFAULT_SLOW_PERIOD, DEFAULT_SIGNAL_PERIOD);
        double[] macd = results.getMacd();
        double[] macdSignal = results.getMacdSignal();
        double[] macdHist = results.getMacdHist();

        int i = macd.length - 1;
        double weight = getWeight(macd, macdSignal, macdHist);

        if((macd[i-1] < macdSignal[i-1]) && (macd[i] > macdSignal[i]) && macd[i] < -2.0) {
            return new EntryDecision(true, weight);
        }

        return new EntryDecision(false, weight);
    }

    private double getWeight(double[] macd, double[] macdSignal, double[] macdHist) {
        int i = macd.length - 1;
        return Math.abs(macdHist[i]);
    }
}
