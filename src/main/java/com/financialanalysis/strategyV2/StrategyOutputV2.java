package com.financialanalysis.strategyV2;

import com.financialanalysis.data.Account;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.graphing.StockChart;
import lombok.Data;

import java.util.Optional;

@Data
public class StrategyOutputV2 {
    private final Symbol symbol;
    private final Account account;
    private final Optional<StockChart> chart;
    private final String strategyName;

    public boolean isEmpty() {
        return account.getActivity().isEmpty();
    }
}
