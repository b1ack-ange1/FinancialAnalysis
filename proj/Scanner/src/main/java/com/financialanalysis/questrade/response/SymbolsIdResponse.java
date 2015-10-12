package com.financialanalysis.questrade.response;

import com.financialanalysis.data.DetailedSymbol;
import lombok.Data;

import java.util.List;

@Data
public class SymbolsIdResponse {
    private final List<DetailedSymbol> symbols;
}






