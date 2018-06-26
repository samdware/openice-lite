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

public class InfoParser {
    private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static LocalDateTime time;


    protected static List<Info> parseList(String metric, JsonArray array) {
        List<Info> result = new ArrayList<>();
        Iterator<JsonElement> it = array.iterator();
        time = LocalDateTime.now();
        while (it.hasNext()) {
            JsonObject object = it.next().getAsJsonObject();
            if (metric.equals("connections")) {
                result.add(parseConnection(object));
            } else if (metric.equals("channels")) {
                result.add(parseChannel(object));
            }
        };
        return result;
    }

    private static ConnectionInfo parseConnection(JsonObject object) {
        ConnectionInfo result = new ConnectionInfo();
        result.add("time", timeFormatter.format(time));
        result.add("name", object.getAsJsonPrimitive("name").getAsString());
        result.add("username", object.getAsJsonPrimitive("user").getAsString());
        result.add("state", object.getAsJsonPrimitive("state").getAsString());
        result.add("ssl", object.getAsJsonPrimitive("ssl").getAsString());
        result.add("channels", object.getAsJsonPrimitive("channels").getAsLong());
        result.add("recv_count", object.getAsJsonPrimitive("recv_oct").getAsLong());
        result.add("recv_rate", object.getAsJsonObject("recv_oct_details").getAsJsonPrimitive("rate").getAsDouble());
        result.add("send_count", object.getAsJsonPrimitive("send_oct").getAsLong());
        result.add("send_rate", object.getAsJsonObject("send_oct_details").getAsJsonPrimitive("rate").getAsDouble());

        return result;
    }

    private static ChannelInfo parseChannel(JsonObject object) {
        ChannelInfo result = new ChannelInfo();
        result.add("time", timeFormatter.format(time));
        result.add("name", object.getAsJsonPrimitive("name").getAsString());
        result.add("username", object.getAsJsonPrimitive("user").getAsString());
        result.add("connection", object.getAsJsonObject("connection_details").getAsJsonPrimitive("name").getAsString());
        result.add("state", object.getAsJsonPrimitive("state").getAsString());
        result.add("messages_unacknowledged", object.getAsJsonPrimitive("messages_unacknowledged").getAsLong());
        JsonObject messageStats = object.getAsJsonObject("message_stats");
        if (messageStats.has("publish")) {
            result.add("publish_count", messageStats.getAsJsonPrimitive("publish").getAsLong());
            result.add("publish_rate", messageStats.getAsJsonObject("publish_details").getAsJsonPrimitive("rate").getAsDouble());
        } else {
            addCountRate(result, messageStats, "deliver_get");
            addCountRate(result, messageStats, "ack");
            addCountRate(result, messageStats, "redeliver");
        }
        return result;
    }

    private static void addCountRate(Info result, JsonObject object, String field) {
        result.add(field+"_count", object.getAsJsonPrimitive(field).getAsLong());
        result.add(field+"_rate", object.getAsJsonObject(field+"_details").getAsJsonPrimitive("rate").getAsDouble());
    }
}
