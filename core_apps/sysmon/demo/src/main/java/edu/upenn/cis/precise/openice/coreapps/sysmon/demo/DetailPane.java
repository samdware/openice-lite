package edu.upenn.cis.precise.openice.coreapps.sysmon.demo;

import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.*;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

public class DetailPane extends GridPane {
    private Metric metric;
    private String[] fields;
    private List<Label> labels;
    private RateLineChart chart;

    public DetailPane(Metric metric) {
        super();
        this.metric = metric;
        chart = new RateLineChart();
        labels = new ArrayList<>();

        if (MetricType.CONNECTIONS.equals(metric)) {
            fields = ConnectionInfo.required;
        } else if (MetricType.CHANNELS.equals(metric)) {
            fields = ChannelInfo.required;
        } else {
            fields = new String[0];
        }
        addLabels();

        chart.addLine("Send");
        chart.addLine("Recv");
        this.add(chart.getNode(), 3, 0);
    }

    private void addLabels() {
        for (int i=0; i<fields.length; i++) {
            this.add(new Label(fields[i] + ": "), 0, i);
            Label label = new Label();
            labels.add(label);
            this.add(label, 1, i);
        }
    }

    protected void updatePane(Info info) {
        for (int i = 0; i < fields.length; i++) {
            labels.get(i).setText(info.getAsString(fields[i]));
        }
        if (metric.equals(MetricType.CONNECTIONS)) {
            chart.addData("Send", info.getAsString("time"), info.getAsDouble("send_oct_rate"));
            chart.addData("Recv", info.getAsString("time"), info.getAsDouble("recv_oct_rate"));
        } else if (metric.equals(MetricType.CHANNELS)) {
            chart.addData("Send", info.getAsString("time"), info.getAsDouble("publish_rate"));
            chart.addData("Recv", info.getAsString("time"), info.getAsDouble("ack_rate"));
        }
    }
}

