package edu.upenn.cis.precise.openice.coreapps.sysmon.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.ConnectionInfo;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class ConnectionDetailPane extends GridPane {

	private HashMap<String, Integer> fieldIndex;
	private List<Label> labels;
	
	public ConnectionDetailPane() {
		super();
		fieldIndex = new HashMap<>();
		labels = new ArrayList<>();
		setupLayout();
	}
	
	private void setupLayout() {
		String[] fields = ConnectionInfo.required; 
//				new ArrayList<String>(Arrays.asList(
//				"Name", "Id", "Domain Id", "Connection", "Recv Rate", "Send Rate"
//				));
		for (int i=0; i<fields.length; i++) {
			this.add(new Label(fields[i] + ": "), 0, i);
			Label value = new Label();
			this.add(value, 1, i);
			labels.add(value);
			fieldIndex.put(fields[i], i);
		}
	}

	protected void updatePane(ConnectionInfo info) {
		for (String s : ConnectionInfo.required) {
			updateLabel(s, info.getAsString(s));
		} 
	}
	
	private void updateLabel(String field, String value) {
		labels.get(fieldIndex.get(field)).setText(value);
	}
}
