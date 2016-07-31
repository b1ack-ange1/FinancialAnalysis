package com.financialanalysis.strategyV2.bollinger;

import com.financialanalysis.analysis.AnalysisFunctionResult;
import com.financialanalysis.data.StockPrice;
import com.financialanalysis.strategyV2.Exit;

import java.util.List;

import static com.financialanalysis.analysis.AnalysisFunctions.bollingerBands;
import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;

public class BollingerExit extends Exit {
    private static final int BB_PERIOD = 21;

    @Override
    public boolean shouldExit(List<StockPrice> stockPrices) {
        double[] closingPrices = getClosingPrices(stockPrices);
        AnalysisFunctionResult result = bollingerBands(closingPrices, BB_PERIOD);
        double[] high = result.getBbHigh();
        double[] mid = result.getBbMid();
        double[] low = result.getBbLow();

        int i = closingPrices.length - 1;

        if(closingPrices[i] >= high[i]) {
            return true;
        }

        return false;
    }
}
