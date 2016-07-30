package com.financialanalysis.graphing;

import lombok.Setter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.util.Map;
import java.util.Set;

public class LineChart extends ApplicationFrame {
    private final String title;

    private XYSeriesCollection xYDataSet = new XYSeriesCollection();

    @Setter private String yAxis = "Y-Axis";
    @Setter private String xAxis = "X-Axis";

    public LineChart(String title) {
        super(title);
        this.title = title;
    }

    public void addXYLine(double[] x, double[] y, String name) {
        XYSeries series = new XYSeries(name);

        for(int i = 0; i < x.length; i++) {
            series.add(x[i], y[i]);
        }

        xYDataSet.addSeries(series);
    }

    public void addXYLine(Map<Double, Double> data, String name) {
        XYSeries series = new XYSeries(name);

        Set<Double> keys = data.keySet();
        for(Double key : keys) {
            series.add(key, data.get(key));
        }

        xYDataSet.addSeries(series);
    }

    public void render() {
        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxis, yAxis, xYDataSet);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(700, 470));
        setContentPane(chartPanel);
        pack();
        RefineryUtilities.centerFrameOnScreen(this);
        setVisible(true);
    }
}
