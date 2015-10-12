package com.financialanalysis.workflow;

import com.financialanalysis.data.Symbol;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class SymbolBlackList {
    private Map<Symbol, Boolean> blackList;

    @SneakyThrows
    private static void createBlackList() {
        Path path = Paths.get(getBlackListDir());
        Files.createDirectories(path);
    }

    private static String getBlackListDir() {
        return "var/blacklist/";
    }

    @Inject
    public SymbolBlackList() {
        buildCache();
    }

    @SneakyThrows
    private void buildCache() {
        File blackListFile = new File(getBlackListDir() + "blacklist");
        if(!blackListFile.exists()) {
            blackList = new LinkedHashMap<>();
        } else {
            String json = FileUtils.readFileToString(blackListFile);
            Type mapType = new TypeToken<Map<Symbol, Boolean>>() {}.getType();
            blackList = new Gson().fromJson(json, mapType);
        }
    }

    public synchronized boolean contains(Symbol symbol) {
        return blackList.containsKey(symbol);
    }

    public synchronized void add(Symbol symbol) {
        blackList.put(symbol, true);
    }

    @SneakyThrows
    public synchronized void commitChanges() {
        File blackListFile = new File(getBlackListDir() + "blacklist");
        Gson gson = new Gson();
        String json = gson.toJson(blackList);
        FileUtils.writeStringToFile(blackListFile, json);
    }
}
