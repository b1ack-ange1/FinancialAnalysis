package com.financialanalysis.store;

import com.financialanalysis.data.Symbol;
import com.financialanalysis.graphing.StockChart;
import lombok.SneakyThrows;
import org.jfree.chart.ChartUtilities;
import org.joda.time.DateTime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReportStore {
    static {
        createReportStore();
    }

    @SneakyThrows
    private static void createReportStore() {
        Path path = Paths.get(getReportStoreDir());
        Files.createDirectories(path);
    }

    private static String getReportStoreDir() {
        return "var/charts/";
    }

    /**
     * var/charts/<date>/<chart-name>
     */
    @SneakyThrows
    public void save(Symbol symbol, List<StockChart> charts, DateTime dateTime) {
        String date = dateTime.toString().split("T")[0];
        Path path = Paths.get(getReportStoreDir() + date);
        Files.createDirectories(path);

        for(StockChart chart : charts) {
            File file = new File(getReportStoreDir() + date + "/" + chart.getTitle().replaceAll(" ", "_") + ".jpg");
            ChartUtilities.saveChartAsJPEG(file, 1.0f, chart.getChart(), 1920, 1080);
        }
    }
}
