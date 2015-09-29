package com.financialanalysis.strategy;

import com.financialanalysis.data.Account;

public abstract class AbstractStrategy {
    protected final Account account;

    public AbstractStrategy(Account account) {
        this.account = account;
    }

    public abstract StrategyOutput runStrategy(StrategyInput input);
}
