package com.financialanalysis.graphing;

import com.financialanalysis.data.StockPrice;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class StockChartTest {
    private final int NUM_DATA = 50;
    private List<DateTime> x;
    private double[] y;
    private List<StockPrice> stockPrices;

    @Before
    public void setup() {
        x = new LinkedList<>();
        y = new double[NUM_DATA];
        stockPrices = new LinkedList<>();


        for(int i = 0; i < NUM_DATA; i++) {
            x.add(DateTime.now().plusDays(i));
            y[i] = i;
            stockPrices.add(new StockPrice(x.get(i), 10, 9, 12, 11, 1000));
        }
    }

    @Test
    public void createWithTitle_andRender() throws Exception {
        StockChart stockChart = new StockChart("Test Title");
        stockChart.addXYLine(x, y, "Test Data");
        stockChart.addCandles(stockPrices);
        stockChart.render();

        StockChartCore stockChartCore = new StockChartCore("Stock Chart Core");
        stockChartCore.addXYLine(x, y, "Test Data");
        stockChartCore.addCandles(stockPrices);
        stockChartCore.render();


        Thread.sleep(20000);
    }
}
