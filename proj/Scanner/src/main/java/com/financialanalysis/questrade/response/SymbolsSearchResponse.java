package com.financialanalysis.questrade.response;

import com.financialanalysis.data.Symbol;
import lombok.Data;

import java.util.List;

/**
 * Response object of Questrade symbol/search
 */
@Data
public class SymbolsSearchResponse {
    private final List<Symbol> symbols;
}
