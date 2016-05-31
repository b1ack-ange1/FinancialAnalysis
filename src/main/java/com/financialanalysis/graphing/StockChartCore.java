package com.financialanalysis.graphing;

import com.financialanalysis.data.Action;
import com.financialanalysis.data.StockPrice;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimePeriodAnchor;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCItem;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.ui.RectangleAnchor;
import org.joda.time.DateTime;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.financialanalysis.analysis.AnalysisTools.getClosingPrices;
import static com.financialanalysis.analysis.AnalysisTools.getDates;
import static com.financialanalysis.analysis.AnalysisTools.getHighPrices;
import static com.financialanalysis.analysis.AnalysisTools.getLowPrices;
import static com.financialanalysis.analysis.AnalysisTools.getOpenPrices;


@Log4j
public class StockChartCore /*extends ApplicationFrame*/ {
    private final String title;

    private List<Marker> domainMarkers = new ArrayList<>();
    private List<Marker> rangeMarkers = new ArrayList<>();
    private TimeSeriesCollection volumeDataset = new TimeSeriesCollection();
    private TimeSeriesCollection xYDataset = new TimeSeriesCollection();
    private OHLCSeriesCollection candleDataset = new OHLCSeriesCollection();
    private double min = 1000000000000.0;
    private double max = -1000000000000.0;

    @Setter private String yAxis = "Y-Axis";
    @Setter private String xAxis = "X-Axis";
    @Setter private String gainLoss = "";
    @Setter private String numDays = "";

    public StockChartCore(String title) {
//        super(title);
        this.title = title;
    }

    /**
     * Will take sublist of x values from dates and create line with them. If x
     * extends beyond dates, then dates will extended as needed
     */
    public void addXYLine(List<DateTime> dates, double[] x, double[] y, String name) {
        List<DateTime> validDates = new ArrayList<>();

        int i = 0;
        for(;i < x.length && x[i] < dates.size(); i++) {
            validDates.add(dates.get((int) x[i]));
        }

        if(i < x.length) {
            DateTime lastDate = dates.get(dates.size()-1);
            for(; i < x.length; i++) {
                lastDate = lastDate.plusDays(1);
                validDates.add(lastDate);
            }
        }

        addXYLine(validDates, y, name);
    }

    /**
     * Will add a line with x and y values
     */
    public void addXYLine(List<DateTime> x, double[] y, String name) {
        if(x.size() == 0 || y.length == 0) return;
        updateMaxMin(y);

        TimeSeries series = new TimeSeries(name);
        for(int i = 0; i < x.size(); i++){
            series.add(getDay(x.get(i)), y[i]);
        }

        xYDataset.addSeries(series);
        xYDataset.setXPosition(TimePeriodAnchor.START);
    }

    /**
     * Adds candles to the graph
     */
    public void addCandles(List<StockPrice> priceList) {
        if(priceList.size() == 0) return;
        double[] open = getOpenPrices(priceList);
        double[] high = getHighPrices(priceList);
        double[] low = getLowPrices(priceList);
        double[] close = getClosingPrices(priceList);
        List<DateTime> dates = getDates(priceList);

        updateMaxMin(high);
        updateMaxMin(low);

        OHLCSeries series = new OHLCSeries("Price");
        for(int i = 0; i < close.length; i++) {
            OHLCItem item = new OHLCItem(getDay(dates.get(i)), open[i], high[i], low[i], close[i]);
            series.add(item);
        }

        candleDataset.addSeries(series);
        candleDataset.setXPosition(TimePeriodAnchor.START);
    }

    public void addVerticalLine(DateTime x, String name, Action action) {
        //TODO: Figure out why we need to minus 1 day here!!
        RegularTimePeriod day = new Day(x.minusDays(1).toDate());
        Marker marker  = new ValueMarker(day.getLastMillisecond());

        marker.setLabel(name);
        marker.setStroke(new BasicStroke(1.1F));

        // Alternate between top and bottom for label
        if(domainMarkers.isEmpty()) {
            marker.setLabelAnchor(RectangleAnchor.TOP);
        } else {
            if(domainMarkers.get(domainMarkers.size()-1).getLabelAnchor() == RectangleAnchor.TOP) {
                marker.setLabelAnchor(RectangleAnchor.BOTTOM);
            } else {
                marker.setLabelAnchor(RectangleAnchor.TOP);
            }
        }

        if(action == null) {
            marker.setPaint(Color.BLACK);
        } else if(action.getAction().equalsIgnoreCase("buy")) {
            marker.setPaint(Color.BLUE);
        } else {
            marker.setPaint(Color.GREEN);
        }

        domainMarkers.add(marker);
    }

    public void addHorizontalLine(double y, String label) {
        Marker marker = new ValueMarker(y);
        marker.setLabel(label);
        marker.setStroke(new BasicStroke(1.0F));
        marker.setPaint(Color.RED);
        rangeMarkers.add(marker);
    }

    /**
     * Add scaled volume to the graph
     */
    public void addVolume(List<DateTime> x, double[] volume) {
        // Normalize it to min and max
        double[] normVolume = new double[volume.length];

        double volMin = 10000000000000.0;
        double volMax = -100000000000000.0;
        for(int i = 0; i < volume.length; i++){
            double cur = volume[i];
            if(cur > volMax) volMax = cur;
            if(cur < volMin) volMin = cur;
        }

        for(int i = 0; i < volume.length; i++) {
            normVolume[i] = scale(volume[i], volMax, volMin);
        }

        TimeSeries series = new TimeSeries("Volume");
        for(int i = 0; i < x.size(); i++){
            series.add(getDay(x.get(i)), normVolume[i]);
        }
        volumeDataset.addSeries(series);
    }

    /**
     * Scale volume from min+volumeOffset to min so not to overlap prices and volume
     */
    private double scale(double val, double volMax, double volMin) {
        double volumeHeight = (max - min) * 0.1;

        double tmp = (min + volumeHeight) - min;
        double tmp2 = val - volMin;
        double tmpTop = tmp * tmp2;

        double tmpBot = volMax - volMin;

        double tmpF = tmpTop / tmpBot;

        return tmpF + min;
    }

    private Day getDay(DateTime dateTime) {
        return new Day(dateTime.toDate());
    }

    /**
     * Updates the min/max local state values;
     */
    private void updateMaxMin(double[] values) {
        for(int i = 0; i < values.length; i++){
            if(values[i] > max) max = values[i];
            if(values[i] < min) min = values[i];
        }
    }

    public void render() {
        JFreeChart chart = ChartFactory.createCandlestickChart(
                title + " " + gainLoss + " " + numDays,
                xAxis,
                yAxis,
                candleDataset,
                true
        );

        // Get the chart plot
        XYPlot plot = chart.getXYPlot();

        // Remove weekends from the domain
//        SegmentedTimeline timeline = SegmentedTimeline.newMondayThroughFridayTimeline();
//        ((DateAxis) plot.getDomainAxis()).setTimeline(timeline);

        // Add the TimeSeries, lines and stuff
        plot.setDataset(1, xYDataset);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setBaseShapesVisible(false);
        plot.setRenderer(1, renderer);

        // Add the volume histogram
        plot.setDataset(2, volumeDataset);
        XYBarRenderer barRenderer = new XYBarRenderer();
//        BarRenderer barRenderer = new BarRenderer();
        barRenderer.setShadowVisible(false);
//        barRenderer.setMargin(0.1);
        barRenderer.setPaint(Color.DARK_GRAY);
//        barRenderer.setMaximumBarWidth(.5);
        plot.setRenderer(2, barRenderer);
//        plot.getRe

        // Add all the domainMarkers
        for(Marker marker : domainMarkers) {
            plot.addDomainMarker(marker);
        }

        for(Marker marker : rangeMarkers) {
            plot.addRangeMarker(marker);
        }

        // Set the range
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(min, max);

//        ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setPreferredSize(new java.awt.Dimension(700, 470));
//        setContentPane(chartPanel);
//        pack();
//        RefineryUtilities.centerFrameOnScreen(this);
//        setVisible(true);
    }

    public JFreeChart getChart() {
        JFreeChart chart = ChartFactory.createCandlestickChart(
                title + " " + gainLoss + " " + numDays,
                xAxis,
                yAxis,
                candleDataset,
                true
        );

        // Get the chart plot
        XYPlot plot = chart.getXYPlot();

        // Remove weekends from the domain
//        SegmentedTimeline timeline = SegmentedTimeline.newMondayThroughFridayTimeline();
//        ((DateAxis) plot.getDomainAxis()).setTimeline(timeline);

        // Add the TimeSeries, lines and stuff
        plot.setDataset(1, xYDataset);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setBaseShapesVisible(false);
        plot.setRenderer(1, renderer);

        // Add the volume histogram
        plot.setDataset(2, volumeDataset);
        XYBarRenderer barRenderer = new XYBarRenderer();
//        BarRenderer barRenderer = new BarRenderer();
        barRenderer.setShadowVisible(false);
//        barRenderer.setMargin(0.1);
        barRenderer.setPaint(Color.DARK_GRAY);
//        barRenderer.setMaximumBarWidth(.5);
        plot.setRenderer(2, barRenderer);
//        plot.getRe

        // Add all the domainMarkers
        for(Marker marker : domainMarkers) {
            plot.addDomainMarker(marker);
        }

        for(Marker marker : rangeMarkers) {
            plot.addRangeMarker(marker);
        }

        // Set the range
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(min, max);

        return chart;
    }
}
