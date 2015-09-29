package com.financialanalysis.updater;

import com.financialanalysis.data.Symbol;
import com.financialanalysis.store.ExchangeStore;
import com.financialanalysis.store.SymbolStore;
import com.google.inject.Inject;

import java.util.List;

public class SymbolUpdater {
    private final SymbolStore symbolStore;
    private final SymbolPuller symbolPuller;
    private final ExchangeStore exchangeStore;

    @Inject
    public SymbolUpdater(SymbolStore symbolStore, SymbolPuller symbolPuller, ExchangeStore exchangeStore) {
        this.symbolStore = symbolStore;
        this.symbolPuller = symbolPuller;
        this.exchangeStore = exchangeStore;
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
    public void update() {
        List<String> exchanges = exchangeStore.getExchangeList();

        List<Symbol> allSymbols = symbolPuller.getAllSymbols();
    }
}
