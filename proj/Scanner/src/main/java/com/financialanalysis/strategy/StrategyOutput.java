package com.financialanalysis.strategy;

import com.financialanalysis.data.Signal;
import com.financialanalysis.data.Account;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StrategyOutput {
    private final Map<String, Account> accounts;
    private final Map<String, List<Signal>> signals;
    private final Map<String, String> findings;
    private String strategy;
}
