package edu.upenn.cis.precise.openice.coreapps.sysmon.demo;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.List;
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

    void fixHeight(DoubleBinding height) {
        lineChart.prefHeightProperty().bind(height);
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

    public void addDataList(String name, List<Pair<String, Double>> data) {
        XYChart.Series s = map.get(name);
        s.getData().clear();
        for (int i= data.size() < 10 ? 0 : data.size() - 10; i< data.size(); i++) {
            s.getData().add(new XYChart.Data<>(data.get(i).getKey(), data.get(i).getValue()));
        }
    }

    public Node getNode() {
        return lineChart;
    }
}
