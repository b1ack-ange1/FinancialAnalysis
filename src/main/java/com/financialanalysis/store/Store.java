package com.financialanalysis.store;

import java.util.List;

public interface Store {
    <T> List<T> loadAll();

    <T> List<T> load(List<T> load);

    <T> void store(List<T> store);

    <T> void delete(List<T> delete);

    int size();
}
