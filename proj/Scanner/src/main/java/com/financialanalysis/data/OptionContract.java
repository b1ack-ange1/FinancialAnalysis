package com.financialanalysis.data;

import lombok.Data;

import java.util.List;

@Data
public class OptionContract {
    private final List<String> underlyings;
    private final String cashInLieu;
}
