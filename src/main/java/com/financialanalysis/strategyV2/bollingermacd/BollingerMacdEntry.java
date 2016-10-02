package com.financialanalysis.strategyV2.bollingermacd;

import com.financialanalysis.data.StockPrice;
import com.financialanalysis.strategyV2.Entry;
import com.financialanalysis.strategyV2.EntryDecision;
import com.financialanalysis.strategyV2.bollinger.BollingerEntry;
import com.financialanalysis.strategyV2.macd.MacdEntry;
import lombok.extern.log4j.Log4j;

import java.util.List;

@Log4j
public class BollingerMacdEntry extends Entry {
    @Override
    public EntryDecision getEntryDecision(List<StockPrice> stockPrices) {
        MacdEntry macdEntry = new MacdEntry();
        BollingerEntry bollingerEntry = new BollingerEntry();

        for(int i = 0; i < 3; i++) {
            List<StockPrice> upToA = stockPrices.subList(0, stockPrices.size() - i);
            EntryDecision macd = macdEntry.getEntryDecision(upToA);

            for(int j = 0; j < 3; j++) {
                List<StockPrice> upToB = stockPrices.subList(0, stockPrices.size() - j);
                EntryDecision bollinger = bollingerEntry.getEntryDecision(upToB);

                if(macd.isEntry() && bollinger.isEntry()) {
                    return new EntryDecision(true, 1.0);
                }
            }
        }

        return new EntryDecision(false, 0.0);
    }
}
