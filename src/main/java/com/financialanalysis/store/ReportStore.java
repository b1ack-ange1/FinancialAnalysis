package com.financialanalysis.store;

import com.financialanalysis.graphing.StockChart;
import lombok.SneakyThrows;
import org.jfree.chart.ChartUtilities;
import org.joda.time.DateTime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
     * Save to:
     * var/charts/<date>/<chart-name>
     */
    @SneakyThrows
    public void save(List<StockChart> charts, DateTime dateTime) {
        String date = dateTime.toString().split("T")[0];
        Path path = Paths.get(getReportStoreDir() + date);
        Files.createDirectories(path);

        for(StockChart chart : charts) {
            File file = new File(getReportStoreDir() + date + "/" + chart.getTitle().replaceAll(" ", "_") + ".jpg");
            ChartUtilities.saveChartAsJPEG(file, 1.0f, chart.getChart(), 1920, 1080);
        }
    }

    public List<File> loadFromDate(DateTime dateTime) {
        String date = dateTime.toString().split("T")[0];
        Path path = Paths.get(getReportStoreDir() + date);
        File dir = path.toFile();

        if(!dir.exists()) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(dir.listFiles()).stream().filter(f -> f.getName().contains(".jpg")).collect(Collectors.toList());
        }
    }
}
