package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.localserver.LocalServerTestBase;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;


public class HttpConnTest extends LocalServerTestBase {
    private HttpConn conn;

    @Override
    public void setUp() throws Exception {
        final SocketConfig socketConfig = SocketConfig.custom()
                          .setSoTimeout(15000)
                          .build();
        this.serverBootstrap = ServerBootstrap.bootstrap()
                .setSocketConfig(socketConfig)
                .setServerInfo(ORIGIN)
                .registerHandler("*", new TestHandler());
        this.connManager = new PoolingHttpClientConnectionManager();
        this.clientBuilder = HttpClientBuilder.create()
                .setDefaultSocketConfig(socketConfig)
                .setConnectionManager(this.connManager);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullHost_NPE() {
        conn = new HttpConn(null, 15672, "", "");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullUser_NPE() {
        conn = new HttpConn("localhost", 15672, null, "");
    }

    // Allow null passwords

    @Test(expected = IllegalArgumentException.class)
    public void constructor_negPort_IllegalArgumentException() {
        conn = new HttpConn("localhost", -1212, "", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_portTooLarge_IllegalArgumentException() {
        conn = new HttpConn("localhost", 65536, "", "");
    }

    @Test(expected = NullPointerException.class)
    public void get_nullMetric_NPE() throws IOException {
        conn = new HttpConn("localhost", 15672, "guest", "guest");
        conn.get(null);
    }

    @Test
    public void get_errorResponse_returnNull() throws IOException {
        conn = getTestConn();
        assertEquals(null, conn.get("400"));
    }

    @Test
    public void get_okResponse_returnParsedJson() throws IOException{
        conn = getTestConn();
        JsonElement resp = conn.get("200");
        JsonObject expected = new JsonObject();
        expected.addProperty("dummy", 1234);
        assertEquals(expected, resp);
    }

    private HttpConn getTestConn() {
        try {
            this.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HttpConn("localhost", this.server.getLocalPort(), "", "");
    }

    private class TestHandler implements HttpRequestHandler {
        // Uri: "/api/statusCode"
        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            String[] s = request.getRequestLine().getUri().split("/");
            BasicHttpEntity entity = new BasicHttpEntity();
            response.setStatusCode(Integer.parseInt(s[2]));
            entity.setContent(new ByteArrayInputStream("{dummy:1234}".getBytes()));
            response.setEntity(entity);
        }
    }
}