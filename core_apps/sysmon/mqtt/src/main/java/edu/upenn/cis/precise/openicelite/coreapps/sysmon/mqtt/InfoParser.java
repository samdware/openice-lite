package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonParser {
    private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime time;


    protected List<Info> parseList(Metric metric, JsonArray array) {
        List<Info> result = new ArrayList<>();
        Iterator<JsonElement> it = array.iterator();
        time = LocalDateTime.now();
        while (it.hasNext()) {
            JsonObject object = it.next().getAsJsonObject();
            if (metric.equals(MetricType.CONNECTIONS)) {
                result.add(parseConnection(object));
            } else if (metric.equals(MetricType.CHANNELS)) {
                result.add(parseChannel(object));
            }
        };
        return result;
    }

    protected ConnectionInfo parseConnection(JsonObject object) {
        ConnectionInfo result = new ConnectionInfo();
        result.add("time", timeFormatter.format(time));
        result.add("name", object.getAsJsonPrimitive("name").getAsString());
        result.add("username", object.getAsJsonPrimitive("user").getAsString());
        result.add("state", object.getAsJsonPrimitive("state").getAsString());
        result.add("ssl", object.getAsJsonPrimitive("ssl").getAsString());
        result.add("channels", object.getAsJsonPrimitive("channels").getAsInt());


        return result;
    }

    protected ChannelInfo parseChannel(JsonObject object) {
        ChannelInfo result = new ChannelInfo();
        return result;
    }

}
