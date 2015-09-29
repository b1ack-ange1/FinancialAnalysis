package com.financialanalysis.graphing;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public class GraphWrapper {
//    private final String title;
//
//    List<GraphSeries> series;
//
//    public GraphWrapper(String title) {
//        this.title = title;
//        this.series = new ArrayList<>();
//    }
//
//    public void addDataSet(double[] y, String name) {
//        double[] x = new double[y.length];
//        for(int i = 0; i < y.length; i++) x[i] = i;
//
//        addDataSet(x, y, name);
//    }
//
//    public void addDataSet(double[] x, double[] y, String name) {
//        GraphSeries gs = new GraphSeries();
//        gs.setName(name);
//        gs.setX(x);
//        gs.setY(y);
//        series.add(gs);
//    }
//
//    public void render() {
//        StockChart graph = new StockChart(title);
//
//        for(GraphSeries gs : series) {
//            graph.addXYLine(gs.getX(), gs.getY(), gs.getName());
//        }
//
//        graph.render();
//    }
//
//    @Data
//    private class GraphSeries {
//        double[] x;
//        double[] y;
//        String name;
//    }
}
