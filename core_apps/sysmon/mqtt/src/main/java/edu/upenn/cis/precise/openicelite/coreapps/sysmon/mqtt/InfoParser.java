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
        result.add("time",     timeFormatter.format(time));
        result.add("name",     getAsString(object, "name"));
        result.add("username", getAsString(object, "user"));
        result.add("state",    getAsString(object,"state"));
        result.add("ssl",      getAsString(object, "ssl"));
        result.add("channels", getAsLong(object, "channels"));
        addCountRate(result, object, "recv_oct");
        addCountRate(result, object, "send_oct");

        return result;
    }

    private static ChannelInfo parseChannel(JsonObject object) {
        ChannelInfo result = new ChannelInfo();
        result.add("time",     timeFormatter.format(time));
        result.add("name",     getAsString(object, "name"));
        result.add("username", getAsString(object, "user"));
        result.add("state",    getAsString(object, "state"));
        result.add("messages_unacknowledged", getAsLong(object, "messages_unacknowledged"));

        if (object.has("connection_details")) {
            result.add("connection", getAsString(object.getAsJsonObject("connection_details"), "name"));
        }

        if (object.has("message_stats")) {
            JsonObject messageStats = object.getAsJsonObject("message_stats");
            addCountRate(result, messageStats, "publish");
            addCountRate(result, messageStats, "deliver_get");
            addCountRate(result, messageStats, "ack");
            addCountRate(result, messageStats, "redeliver");
        }
        return result;
    }

    private static void addCountRate(Info result, JsonObject object, String field) {
        result.add(field + "_count", getAsLong(object, field));
        if (object.has(field + "_details")) {
            result.add(field + "_rate", getAsDouble(object.getAsJsonObject(field + "_details"), "rate"));
        } else {
            result.add(field + "_rate", 0);
        }
    }

    private static double getAsDouble(JsonObject object, String field) {
        if (object.has(field)) {
            return object.getAsJsonPrimitive(field).getAsDouble();
        }
        return 0;
    }

    private static long getAsLong(JsonObject object, String field) {
        if (object.has(field)) {
            return object.getAsJsonPrimitive(field).getAsLong();
        }
        return 0;
    }

    private static String getAsString(JsonObject object, String field) {
        if (object.has(field)) {
            return object.getAsJsonPrimitive(field).getAsString();
        }
        return "";
    }
}
