package edu.upenn.cis.precise.openice.coreapps.sysmon.demo;

import java.util.ArrayList;
import java.util.List;

import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.DataListener;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.ISysmon;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.Info;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.ChannelInfo;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.ConnectionInfo;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt.MqttSysmon;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

public class Main extends Application {
	BorderPane borderPane;
	ISysmon sysmon;
	
	ConnectionDetailPane connectionPane;
	ChannelDetailPane channelPane;
	
	List<Info> connections, channels;
    ListView<String> connectionList, channelList, topicList;
	
	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Tabs");
        Group root = new Group();
        Scene scene = new Scene(root, 800, 500);
        borderPane = new BorderPane();
        sysmon = new MqttSysmon();
        sysmonSetup();

        TabPane tabPane = new TabPane();
        borderPane.setLeft(tabPane);
        
        connectionList = addTab("Connections", tabPane);
        channelList = addTab("Channels", tabPane);
        //topicList = addTab("Topics", tabPane);

        connectionPane = new ConnectionDetailPane();
        channelPane = new ChannelDetailPane();
        borderPane.setCenter(connectionPane);
                
        // bind to take available space
        borderPane.prefHeightProperty().bind(scene.heightProperty());
        borderPane.prefWidthProperty().bind(scene.widthProperty());
        
        borderPane.setLeft(tabPane);
        root.getChildren().add(borderPane);
        primaryStage.setScene(scene);
        primaryStage.show();
	}
	
	private void sysmonSetup() {
		sysmon.addMonitor("connections");
		sysmon.addMonitor("channels");
		sysmon.addListener("connections", new DataListener() {
			@Override
			public void onNotAvailable() {}
			@Override
			public void handleData(List<Info> data) {
				connections = data;
				ObservableList<String> names = extractNames(data);
				Platform.runLater(()->connectionList.setItems(names));
			}
		});
		sysmon.addListener("channels", new DataListener() {
			@Override
			public void onNotAvailable() {}

			@Override
			public void handleData(List<Info> data) {
				channels = data;
				ObservableList<String> names = extractNames(data);
				Platform.runLater(()->channelList.setItems(names));
			}
			
		});
		sysmon.start();
	}
	
	private ObservableList<String> extractNames(List<Info> list) {
		List<String> names = new ArrayList<String>();
		for (Info i : list) {
			names.add(i.getAsString("name"));
		}
		return FXCollections.observableArrayList(names);
	}
	
	private ListView<String> addTab(final String tabName, TabPane pane) {
		Tab connTab = new Tab();
        connTab.setText(tabName);
        
        ListView<String> list = new ListView<>();
        list.setOrientation(Orientation.VERTICAL);
        list.getSelectionModel().selectedIndexProperty().addListener(
        		new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
						updateDetailPanel(newValue.intValue(), tabName);
					}
        		});
        
        connTab.setContent(list);
        pane.getTabs().add(connTab);
        return list;
	}
	
	private void updateDetailPanel(int index, String tabName) {
		if (index < 0) return;
		if ("Connections".equals(tabName) && index < connections.size()) {
			connectionPane.updatePane((ConnectionInfo)connections.get(index));
			borderPane.setCenter(connectionPane);
		} else if ("Channels".equals(tabName) && index < channels.size()) {
			channelPane.updatePane((ChannelInfo)channels.get(index));
			borderPane.setCenter(channelPane);
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
