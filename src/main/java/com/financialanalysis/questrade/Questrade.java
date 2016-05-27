package com.financialanalysis.questrade;

import com.financialanalysis.data.Symbol;
import com.financialanalysis.questrade.response.MarketCandlesResponse;
import com.financialanalysis.questrade.response.SymbolsIdResponse;
import com.financialanalysis.questrade.response.SymbolsSearchResponse;
import org.joda.time.DateTime;

import java.util.List;

public interface Questrade {
    void authenticate();

    SymbolsSearchResponse symbolSearch(String prefix) throws Exception;

    SymbolsIdResponse getSymbolsId(List<String> ids) throws Exception;

    MarketCandlesResponse getMarketCandles(Symbol symbol,
                                           DateTime start,
                                           DateTime end,
                                           HistoricDataGranularity interval) throws Exception;
}
