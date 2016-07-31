package com.financialanalysis.strategyV2.bollinger;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.strategyV2.Entry;
import com.financialanalysis.strategyV2.EntryDecision;

import java.util.List;

import static com.financialanalysis.analysis.AnalysisFunctions.bollingerBands;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;

public class BollingerEntry extends Entry {
    private static final int BB_PERIOD = 21;

    @Override
    public EntryDecision getEntryDecision(List<StockPrice> stockPrices) {
        double[] closingPrices = getClosingPrices(stockPrices);
        AnalysisFunctionResult result = bollingerBands(closingPrices, BB_PERIOD);
        double[] high = result.getBbHigh();
        double[] mid = result.getBbMid();
        double[] low = result.getBbLow();

        int i = closingPrices.length - 1;
        double weight = getWeight(high, mid, low);

        if(closingPrices[i] <= low[i]) {
            return new EntryDecision(true, weight);
        }

        return new EntryDecision(false, weight);
    }

    private double getWeight(double[] high, double[] mid, double[] low) {
        int i = high.length - 1;
        return high[i] - low[i];
    }
}
