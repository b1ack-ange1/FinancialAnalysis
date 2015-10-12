package com.financialanalysis.data;

import lombok.Data;

@Data
public class Symbol {
    private final String symbol;
    private final String symbolId;
    private final String description;
    private final String securityType;
    private final String isTradable;
    private final String isQuotable;
    private final String currency;

    public Symbol clone() {
        return new Symbol(
                symbol,
                symbolId,
                description,
                securityType,
                isTradable,
                isQuotable,
                currency
        );
    }

    @Override
    public String toString() {
        return symbol;
    }
}
