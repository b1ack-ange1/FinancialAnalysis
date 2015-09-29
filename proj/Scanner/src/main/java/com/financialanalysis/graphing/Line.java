package com.financialanalysis.graphing;

import lombok.Data;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class Line {
    private final double xIntercept;
    private final double slope;

    private List<Point> points;

    private Point singlePoint;

    private double[] xLine;
    private double[] yLine;

    public Line(double x, double y, double slope, int dataPoints) {
        double nextX = x + 1;
        double nextY = y + slope;
        SimpleRegression sr = new SimpleRegression();
        sr.addData(x, y);
        sr.addData(nextX, nextY);

        this.xIntercept = sr.getIntercept();
        this.xLine = new double[dataPoints];
        this.yLine = new double[dataPoints];

        this.slope = slope;
        this.singlePoint = new Point(x,y);

        int offset = 0;
        for(int i = 0; i < dataPoints; i++) {
            xLine[i] = x + offset;
            yLine[i] = y + (offset*slope);
            offset++;
        }
    }

    public Line(SimpleRegression sr) {
        this(sr.getSlope(), sr.getIntercept());
    }

    public Line(double slope, double xIntercept) {
        this.slope = slope;
        this.xIntercept = xIntercept;
    }

    public Line(Point p, double slope) {
        this(p.getX(), p.getY(), slope);
    }

    public Line(double x, double y, double slope) {
        double nextX = x + 1;
        double nextY = y + slope;
        SimpleRegression sr = new SimpleRegression();
        sr.addData(x, y);
        sr.addData(nextX, nextY);

        this.xIntercept = sr.getIntercept();
        this.singlePoint = new Point(x,y);
        this.slope = slope;
    }

    public Line(List<Point> points) {
        SimpleRegression sr = new SimpleRegression();
        for(Point point : points) {
            sr.addData(point.getX(), point.getY());
        }
        this.xIntercept = sr.getIntercept();
        this.slope = sr.getSlope();
        this.points = points;
    }

    public Line(Point a, Point b) {
        this(Arrays.asList(a, b));
    }

    public Point intersection(Line other) {
        // If slope of the two lines are really close, then they don't intersect
        if(Math.abs(slope - other.getSlope()) < 0.0001) return null;

        double x = (other.getXIntercept() - xIntercept) / (slope - other.getSlope());
        double y = (slope * x) - xIntercept;
        return new Point(x, y);
    }

    public Point getPointForX(double x) {
        return new Point(x, (slope * x) + xIntercept);
    }

    public double getYForX(double x) {
        return (slope * x) + xIntercept;
    }

    public double[] generateXValues(double x1, double x2) {
        int num = (int) (x2 - x1 + 1.0);
        double[] x = new double[num];
        int offset = 0;
        for(int i = 0; i < num; i++) {
            x[i]= x1 + offset;
            offset++;
        }
        return x;
    }

    public double[] generateXValues() {
        return generateXValues(points.get(0).getX(), points.get(points.size()-1).getX());
    }

    public double[] generateYValues(double x1, double x2) {
        double yStart = getPointForX(x1).getY();

        int num = (int) (x2 - x1 + 1.0);
        double[] y = new double[num];
        int offset = 0;
        for(int i = 0; i < num; i++) {
            y[i] = yStart + (offset * slope);
            offset++;
        }
        return y;
    }

    public double[] generateYValues() {
        return generateYValues(points.get(0).getX(), points.get(points.size()-1).getX());
    }

    public int lineDomain() {
        return (int) points.get(points.size()-1).getX() - (int) points.get(0).getX();
    }
}
