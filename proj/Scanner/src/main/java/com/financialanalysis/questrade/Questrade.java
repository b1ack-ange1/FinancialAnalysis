package com.financialanalysis.questrade;

import com.financialanalysis.data.Symbol;
import com.financialanalysis.questrade.response.MarketCandlesResponse;
import com.financialanalysis.questrade.response.SymbolsIdResponse;
import com.financialanalysis.questrade.response.SymbolsSearchResponse;
import org.joda.time.DateTime;

import java.util.List;

public interface Questrade {
    public SymbolsSearchResponse symbolSearch(String prefix) throws Exception;

    public SymbolsIdResponse getSymbolsId(List<String> ids) throws Exception;

    public MarketCandlesResponse getMarketCandles(Symbol symbol, DateTime start, DateTime end,
                                                  HistoricDataGranularity interval) throws Exception;
}
