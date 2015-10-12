package com.financialanalysis.graphing;

import com.financialanalysis.data.Action;
import com.financialanalysis.data.StockPrice;
import lombok.Data;
import org.jfree.chart.JFreeChart;
import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;

@Data
public class StockChart {
    private String title = "Title";
    private String yAxis = "Y-Axis";
    private String xAxis = "X-Axis";
    private String gainLoss = "";
    private String numDays = "";

    public StockChart(String title) {
        this.title = title;
    }

    private List<XYLine1> xyLine1List = new LinkedList<>();
    public void addXYLine(List<DateTime> dates, double[] x, double[] y, String name) {
        xyLine1List.add(new XYLine1(dates, x, y, name));
    }

    private List<XYLine2> xyLine2List = new LinkedList<>();
    public void addXYLine(List<DateTime> x, double[] y, String name) {
        xyLine2List.add(new XYLine2(x, y , name));
    }

    private List<Candles> candles = new LinkedList<>();
    public void addCandles(List<StockPrice> priceList) {
        candles.add(new Candles(priceList));
    }

    private List<VerticalLine> verticalLineList = new LinkedList<>();
    public void addVerticalLine(DateTime x, String name, Action action) {
        verticalLineList.add(new VerticalLine(x, name, action));
    }

    private List<HorizontalLine> horizontalLineList = new LinkedList<>();
    public void addHorizontalLine(double y, String label) {
        horizontalLineList.add(new HorizontalLine(y, label));
    }

    private List<Volume> volumeList = new LinkedList<>();
    public void addVolume(List<DateTime> x, double[] volume) {
        volumeList.add(new Volume(x, volume));
    }

    public void render() {
        StockChartCore core = buildStockChartCore();
        core.render();
    }

    public JFreeChart getChart() {
        StockChartCore core = buildStockChartCore();
        return core.getChart();
    }

    private StockChartCore buildStockChartCore() {
        StockChartCore core = new StockChartCore(title);
        core.setXAxis(xAxis);
        core.setYAxis(yAxis);
        core.setNumDays(numDays);
        core.setGainLoss(gainLoss);

        if(!xyLine1List.isEmpty()) {
            for(XYLine1 line : xyLine1List) {
                core.addXYLine(line.getDates(), line.getX(), line.getY(), line.getName());
            }
        }

        if(!xyLine2List.isEmpty()) {
            for(XYLine2 line : xyLine2List) {
                core.addXYLine(line.getX(), line.getY(), line.getName());
            }
        }

        if(!candles.isEmpty()) {
            for(Candles candle : candles) {
                core.addCandles(candle.getPriceList());
            }
        }

        if(!verticalLineList.isEmpty()) {
            for(VerticalLine line : verticalLineList) {
                core.addVerticalLine(line.getX(), line.getName(), line.getAction());
            }
        }

        if(!horizontalLineList.isEmpty()) {
            for(HorizontalLine line : horizontalLineList) {
                core.addHorizontalLine(line.getY(), line.getLabel());
            }
        }

        if(!volumeList.isEmpty()) {
            for(Volume volume : volumeList) {
                core.addVolume(volume.getX(), volume.getVolume());
            }
        }
        return core;
    }

    @Data
    private class XYLine1 {
        private final List<DateTime> dates;
        private final double[] x;
        private final double[] y;
        private final String name;
    }

    @Data
    private class XYLine2 {
        private final List<DateTime> x;
        private final double[] y;
        private final String name;
    }

    @Data
    private class Candles {
        private final List<StockPrice> priceList;
    }

    @Data
    private class VerticalLine {
        private final DateTime x;
        private final String name;
        private final Action action;
    }

    @Data
    private class HorizontalLine {
        private final double y;
        private final String label;
    }

    @Data
    private class Volume {
        private final List<DateTime> x;
        private final double[] volume;
    }
}
