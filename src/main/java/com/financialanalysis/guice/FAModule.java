package com.financialanalysis.guice;

import com.financialanalysis.questrade.Questrade;
import com.financialanalysis.questrade.QuestradeImpl;
import com.google.inject.AbstractModule;

public class FAModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Questrade.class).to(QuestradeImpl.class);
    }
}
