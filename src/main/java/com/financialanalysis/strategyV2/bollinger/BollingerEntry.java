package com.financialanalysis.strategyV2.bollinger;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.strategyV2.Entry;
import com.financialanalysis.strategyV2.EntryDecision;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.List;

import static com.financialanalysis.analysis.AnalysisFunctions.bollingerBands;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisBaseFunctions.sma;

public class BollingerEntry extends Entry {
    private static final int BB_PERIOD = 21;

    @Override
    public EntryDecision getEntryDecision(List<StockPrice> stockPrices) {
        double[] closingPrices = getClosingPrices(stockPrices);
        AnalysisFunctionResult result = bollingerBands(closingPrices, BB_PERIOD);
        double[] high = result.getBbHigh();
        double[] mid = result.getBbMid();
        double[] low = result.getBbLow();
        double[] sma100 = sma(closingPrices, 100);

        int startIndex = closingPrices.length - 1;
        double weight = getWeight(high, mid, low);

        /**
         * Buy if
         * 1) Hit the low mark
         * 2) Move up through mid mark
         */

        if(closingPrices[startIndex] <= low[startIndex] && closingPrices[startIndex] > sma100[startIndex]) {
            return new EntryDecision(true, weight);
        }

//        if(closingPrices[startIndex] >= mid[startIndex]) {
//            for(int i = startIndex; i > startIndex - 4; i--) {
//                if(closingPrices[i] <= low[i]) {
//                    return new EntryDecision(true, weight);
//                }
//            }
//        }

        return new EntryDecision(false, weight);
    }

    private double getWeight(double[] high, double[] mid, double[] low) {
        int i = high.length - 1;

        double highP = high[i] / mid[i] * 100;
        double lowP = low[i] / mid[i] * 100;

        return highP - lowP;
    }

    private boolean isTrendingUp(double[] data) {
        int max = data.length - 1;
        SimpleRegression sr = new SimpleRegression();
        for(int i = max; i > 0 && i > max - BB_PERIOD; i--) {
            sr.addData(i, data[i]);
        }

        if(sr.getRSquare() >= 0.8 && sr.getSlope() >= 0.02) {
            return true;
        } else {
            return false;
        }
    }
}
