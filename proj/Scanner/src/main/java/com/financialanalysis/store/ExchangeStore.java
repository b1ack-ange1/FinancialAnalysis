package com.financialanalysis.store;

import java.util.Arrays;
import java.util.List;

public class ExchangeStore {
    public List<String> getExchangeList() {
        return Arrays.asList("NYSE", "NASDAQ", "OTC", "TSX");
    }
}
