package edu.upenn.cis.precise.openice.coreapps.sysmon.demo;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.HashMap;
import java.util.Map;

public class RateLineChart {
    private final CategoryAxis xAxis;
    private final NumberAxis yAxis;
    private final LineChart<String, Number> lineChart;
    private Map<String, XYChart.Series> map;

    RateLineChart() {
        xAxis = new CategoryAxis();
        xAxis.setLabel("Time");
        yAxis = new NumberAxis();
        lineChart = new LineChart<>(xAxis, yAxis);
        map = new HashMap<>();

        lineChart.setTitle("Message rates");
    }

    public void addLine(String name) {
        XYChart.Series s = new XYChart.Series();
        s.setName(name);
        map.put(name, s);
        lineChart.getData().add(s);
    }

    public void addData(String name, String time, double data) {
        XYChart.Series s = map.get(name);
        ObservableList<XYChart.Data<String, Number>> dataList = s.getData();
        if (dataList.size() > 10) {
            dataList.remove(0);
        }
        dataList.add(new XYChart.Data<>(time, data));
    }

    public Node getNode() {
        return lineChart;
    }
}
