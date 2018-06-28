package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import com.google.gson.*;
import edu.upenn.cis.precise.openicelite.coreapps.sysmon.api.Info;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class InfoParserTest {
    private static JsonObject sampleConnection;
    private static JsonObject sampleChannel;
    private List<Info> result;
    private JsonArray array;

    @BeforeClass
    public static void setupSample() throws IOException {
        JsonParser parser = new JsonParser();
        JsonElement e = parser.parse(new FileReader(new File("src/test/resources/sampleConnection.json")));
        sampleConnection = e.getAsJsonObject();
        e = parser.parse(new FileReader(new File("src/test/resources/sampleChannel.json")));
        sampleChannel = e.getAsJsonObject();
    }

    @Before
    public void init() {
        array = new JsonArray();
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseList_nullMetric_NPE() {
        InfoParser.parseList(null, new JsonArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseList_unsupportedMetric_IllegalArgumentException() {
        InfoParser.parseList("blahhh", new JsonArray());
    }

    @Test(expected = NullPointerException.class)
    public void parseList_nullArray_NPE() {
        InfoParser.parseList("connections", null);
    }

    @Test
    public void parseList_emptyArray_returnEmptyList() {
        result = InfoParser.parseList("connections", new JsonArray());
        assertEquals(0, result.size());
    }

    @Test
    public void parseList_arrayContainsPrimitive_ignorePrimitive() {
        array.add(sampleConnection);
        array.add(new JsonPrimitive("primitive"));
        result = InfoParser.parseList("connections", array);
        assertEquals(1, result.size());
        assertEquals("127.0.0.1:6532 -> 127.0.0.1:1883", result.get(0).getAsString("name"));
    }

    @Test
    public void getAsX_missingField_fillEmptyValue() {
        JsonObject object = sampleConnection.deepCopy();
        object.remove("name");
        array.add(object);
        result = InfoParser.parseList("connections", array);
        assertEquals(1, result.size());
        assertEquals("", result.get(0).getAsString("name"));
    }

    @Test
    public void addCountRate_missingCount_fillEmptyValue() {
        JsonObject object = sampleChannel.deepCopy();
        System.out.println(object.entrySet());
        object.getAsJsonObject("message_stats").remove("deliver_get");
        array.add(object);
        result = InfoParser.parseList("channels", array);
        assertEquals(0, result.get(0).getAsLong("deliver_get_count"));
    }

    @Test
    public void addCountRate_missingRate_fillEmptyValue() {
        JsonObject object = sampleChannel.deepCopy();
        object.getAsJsonObject("message_stats").getAsJsonObject("ack_details")
                .remove("rate");
        array.add(object);
        result = InfoParser.parseList("channels", array);
        assertEquals(0, result.get(0).getAsDouble("ack_rate"), 0.001);
    }

}