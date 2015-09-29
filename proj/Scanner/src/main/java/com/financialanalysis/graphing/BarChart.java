package com.financialanalysis.graphing;

import lombok.Setter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class BarChart extends ApplicationFrame {
    private final String title;

    private XYSeriesCollection xYDataSet = new XYSeriesCollection();
    private CategoryDataset barDataSet = new DefaultCategoryDataset();
    private HistogramDataset histogramDataset = new HistogramDataset();;

    @Setter
    private String yAxis = "Y-Axis";
    @Setter private String xAxis = "X-Axis";

    public BarChart(String title) {
        super(title);
        this.title = title;
    }

//    public void addBars(double[] x, double[] y, String name) {
//        XYSeries series = new XYSeries(name);
//
//        for(int i = 0; i < x.length; i++) {
//            series.add(x[i], y[i]);
//        }
//
//        histogramDataset.
//        barDataSet.addSeries(series);
//        barDataSet.
//    }
//
//    public void render() {
////        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxis, yAxis, xYDataSet);
//        JFreeChart chart = ChartFactory.createBarChart(title, xAxis, yAxis, xYDataSet)
//
//        ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setPreferredSize(new java.awt.Dimension(700, 470));
//        setContentPane(chartPanel);
//        pack();
//        RefineryUtilities.centerFrameOnScreen(this);
//        setVisible(true);
//    }
}
