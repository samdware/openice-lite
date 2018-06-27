package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class MqttSysmonTest {
    private MqttSysmon sysmon;

    @Before
    public void initTest() {
        sysmon = new MqttSysmon();
    }

    @Test(expected = NullPointerException.class)
    public void init_nullArgument_NPEThrown() {
        sysmon.init(null);
    }

    @Test
    public void init_emptyProperties_initByDefaultValues() {

    }

    @Test
    public void init_someMissingProperties_fillWithDefault() {

    }

    @Test(expected = IllegalArgumentException.class)
    public void setInterval_nonPositiveArgument_IllegalArgumentException() {
        Properties p = new Properties();
        p.setProperty("interval", "0");
        sysmon.init(p);
    }

}