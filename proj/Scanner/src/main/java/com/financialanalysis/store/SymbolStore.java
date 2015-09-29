package com.financialanalysis.store;

import com.financialanalysis.data.Symbol;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SymbolStore {
    private final String FILE_NAME = "symbols";

    private int size = -1;

    static {
        createSymbolStore();
    }

    @SneakyThrows
    private static void createSymbolStore() {
        Path path = Paths.get(getSymbolStoreDir());
        Files.createDirectories(path);
    }

    private static String getSymbolStoreDir() {
        return "var/symbols/";
    }

    @SneakyThrows
    public List<Symbol> load() {
        File symbolFile = new File(getSymbolStoreDir() + FILE_NAME);
        if(!symbolFile.exists()) {
            return new ArrayList<>();
        }

        String json = FileUtils.readFileToString(symbolFile);
        Type listType = new TypeToken<ArrayList<Symbol>>() {}.getType();
        List<Symbol> symbols = new Gson().fromJson(json, listType);
        size = symbols.size();

        return symbols;
    }

    @SneakyThrows
    public void store(List<Symbol> store) {
        File symbolFile = new File(getSymbolStoreDir() + FILE_NAME);
        Gson gson = new Gson();
        String json = gson.toJson(store);
        FileUtils.writeStringToFile(symbolFile, json);
        size = store.size();
    }

    public void delete(List<Symbol> delete) {
        List<Symbol> loaded = load();
        for(Symbol s : delete) {
            if(loaded.contains(s)) {
                loaded.remove(s);
            }
        }
        store(loaded);
    }

    public int size() {
        if(size == -1) {
            List<Symbol> loaded = load();
            return loaded.size();
        } else {
            return size;
        }
    }
}
