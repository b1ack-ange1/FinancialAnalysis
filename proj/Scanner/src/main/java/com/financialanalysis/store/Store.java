package com.financialanalysis.store;

import java.util.List;

public interface Store {
    public <T> List<T> loadAll();

    public <T> List<T> load(List<T> load);

    public <T> void store(List<T> store);

    public <T> void delete(List<T> delete);

    public int size();
}
