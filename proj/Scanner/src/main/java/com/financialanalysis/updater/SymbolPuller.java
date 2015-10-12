package com.financialanalysis.updater;

import com.financialanalysis.data.DetailedSymbol;
import com.financialanalysis.data.Symbol;
import com.financialanalysis.questrade.Questrade;
import com.financialanalysis.questrade.response.SymbolsIdResponse;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.financialanalysis.analysis.AnalysisTools.deepCopyStringList;
import static com.financialanalysis.analysis.AnalysisTools.deepCopySymbolList;

@Log4j
public class SymbolPuller {
    private final Questrade questrade;

    //Experimentally determined 46997 to be the max number of stock ids
    private final static int MAX_SYMBOL_ID = 46997;
    private final static int BATCH_SIZE = 100;

    private List<Symbol> found = new LinkedList<>();

    @Inject
    public SymbolPuller(Questrade questrade) {
        this.questrade = questrade;
    }

    @SneakyThrows
    public List<Symbol> getAllSymbols() {
        List<String> batch = new ArrayList<>();
        for(int symbolId = 1; symbolId <= MAX_SYMBOL_ID; symbolId++) {
            batch.add(Integer.toString(symbolId));

            if(batch.size() >= BATCH_SIZE) {
                getSymbols(deepCopyStringList(batch));
                batch.clear();
            }
        }
        getSymbols(deepCopyStringList(batch));

        return getFound();
    }

    @SneakyThrows
    private void getSymbols(List<String> ids) {
        if(ids.isEmpty()) return;

        SymbolsIdResponse response = questrade.getSymbolsId(ids);
        List<Symbol> symbols = response.getSymbols().stream().map(DetailedSymbol::convertToSymbol).collect(Collectors.toList());
        addFound(symbols);

        for(Symbol s : symbols) log.info(s.getSymbol() + " " + s.getDescription());
        log.info(String.format("%d/%d", getNumFound(), MAX_SYMBOL_ID));
    }

    private synchronized void addFound(List<Symbol> symbols) {
        found.addAll(symbols);
    }

    private synchronized List<Symbol> getFound() {
        return deepCopySymbolList(found);
    }

    private synchronized int getNumFound() {
        return found.size();
    }
}
