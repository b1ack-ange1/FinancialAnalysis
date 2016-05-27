package com.financialanalysis.updater;

import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.SymbolStore;
import com.financialanalysis.workflow.SymbolBlackList;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;

import java.util.List;
import java.util.stream.Collectors;

@Log4j
public class SymbolUpdater {
    private final SymbolStore symbolStore;
    private final SymbolPuller symbolPuller;
    private final SymbolBlackList symbolBlackList;

    @Inject
    public SymbolUpdater(SymbolStore symbolStore,
                         SymbolPuller symbolPuller,
                         SymbolBlackList symbolBlackList) {
        this.symbolStore = symbolStore;
        this.symbolPuller = symbolPuller;
        this.symbolBlackList = symbolBlackList;
    }

    public void refresh() {
        List<Symbol> allSymbols = symbolPuller.getAllSymbols();
        log.info("Found " + allSymbols.size() + " symbols");

        List<Symbol> validSymbols = allSymbols.stream()
                .filter(s -> s.getIsTradable().equalsIgnoreCase("true"))
                .filter(s -> !symbolBlackList.contains(s))
                .collect(Collectors.toList());

        log.info("Found " + validSymbols.size() + " valid symbols");
        symbolStore.store(validSymbols);
    }
}
