package com.financialanalysis.csv;

import com.google.inject.Inject;
import com.opencsv.CSVReader;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SymbolRetriever {
    private final CSVReader companyListReader;
    private final Random random;

    private String[] allSymbols;

    @Inject
    public SymbolRetriever(CSVReader csvReader) {
        this.companyListReader = csvReader;
        this.random = new Random();
        buildCache();
    }

    public String[] getAllSymbols() {
        return getRandomSymbols(allSymbols.length);
    }

    public String[] getRandomSymbols(int amount) {
        Map<String, Boolean> found = new HashMap<>();
        String[] array = new String[amount];
        for(int i = 0; i < amount; i++) {
            String symbol = allSymbols[randInt(0, allSymbols.length-1)];
            if(found.containsKey(symbol)) {
                i--;
            } else {
                array[i] = symbol;
                found.put(symbol, true);
            }
        }
        return array;
    }

    @SneakyThrows
    private void buildCache() {
        List<String[]> stocks = companyListReader.readAll();
        List<String> symbols = new ArrayList<>();
        for(int i = 1; i < stocks.size(); i++) {
            symbols.add(stocks.get(i)[0]);
        }

        allSymbols = new String[symbols.size()];
        allSymbols = symbols.toArray(allSymbols);
    }

    private int randInt(int min, int max) {
        int randomNum = random.nextInt((max - min) + 1) + min;
        return randomNum;
    }
}
