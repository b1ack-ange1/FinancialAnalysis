package com.financialanalysis.data;

import lombok.Data;

@Data
public class Symbol {
    private final String symbol;

    public static Symbol get(String symbol) {
        return new Symbol(symbol);
    }
}
