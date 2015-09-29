package com.financialanalysis.data;

import com.financialanalysis.graphing.Point;
import lombok.Data;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;

import java.util.List;

@Data
public class Trend {
    private final SimpleRegression simpleRegression;
    private final List<Point> points;
    private final DateTime start;
    private final DateTime end;
}
