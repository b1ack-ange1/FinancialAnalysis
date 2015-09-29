package com.financialanalysis.updater;

import com.financialanalysis.data.Symbol;
import com.financialanalysis.externalapi.QuestradeApi;
import com.google.inject.Inject;

import java.util.List;

public class SymbolPuller {
    private final QuestradeApi questrade;

    @Inject
    public SymbolPuller(QuestradeApi questradeApi) {
        questrade = questradeApi;
    }

    public List<Symbol> getAllSymbols() {
        return null;
    }
}
