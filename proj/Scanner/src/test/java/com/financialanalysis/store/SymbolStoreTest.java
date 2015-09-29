package com.financialanalysis.store;

import com.financialanalysis.data.Symbol;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SymbolStoreTest {
    private SymbolStore symbolStore;
    private List<Symbol> symbols;

    @Before
    public void setup() {
        symbolStore = new SymbolStore();
        symbols = Arrays.asList(Symbol.get("AAPL"), Symbol.get("AMZN"), Symbol.get("GOOG"));
    }

    @Test
    public void whenStoreThenLoad_Equals() {
        symbolStore.store(symbols);

        List<Symbol> loaded = symbolStore.load();

        assertEquals(symbols.size(), loaded.size());
        assertEquals(symbols, loaded);

        symbolStore.delete(loaded);

        loaded = symbolStore.load();
        assertEquals(loaded.size(), 0);
    }
}
