package edu.upenn.cis.precise.openice.coreapps.sysmon.demo;

import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.*;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

public class DetailPane extends GridPane {
    private String[] fields;
    private List<Label> labels;

    public DetailPane(Metric metric) {
        super();
        if (MetricType.CONNECTIONS.equals(metric)) {
            fields = ConnectionInfo.required;
        } else if (MetricType.CHANNELS.equals(metric)) {
            fields = ChannelInfo.required;
        } else {
            fields = new String[0];
        }
        labels = new ArrayList<>();
        addLabels();
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
    }
}

