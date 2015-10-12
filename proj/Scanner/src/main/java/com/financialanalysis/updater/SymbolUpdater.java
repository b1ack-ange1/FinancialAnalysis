package com.financialanalysis.updater;

import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.ExchangeStore;
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
    private final ExchangeStore exchangeStore;
    private final SymbolBlackList symbolBlackList;

    @Inject
    public SymbolUpdater(SymbolStore symbolStore,
                         SymbolPuller symbolPuller,
                         ExchangeStore exchangeStore,
                         SymbolBlackList symbolBlackList) {
        this.symbolStore = symbolStore;
        this.symbolPuller = symbolPuller;
        this.exchangeStore = exchangeStore;
        this.symbolBlackList = symbolBlackList;
    }

    /**
     1) Pull every symbol from all exchanges in ExchangeStore
     2) Check if it is valid
     - If symbol is not in SymbolBackList
     - If symbol is tradeable
     - If ... ?
     3) If not valid
     - Remove from SymbolStore
     - Add to SymbolBlackList
     4) If is valid, add to SymbolStore
     */
    public void refresh() {
        if(true) return;

        List<Symbol> allSymbols = symbolPuller.getAllSymbols();

        List<Symbol> validSymbols = allSymbols.stream()
                .filter(s -> s.getIsTradable().equalsIgnoreCase("true"))
                .filter(s -> !symbolBlackList.contains(s))
                .collect(Collectors.toList());

        log.info("Found " + validSymbols.size() + " valid symbols");
        symbolStore.store(validSymbols);
    }
}
