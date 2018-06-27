package edu.upenn.cis.precise.openicelite.coreapps.sysmon.mqtt;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class HttpConn {
	private static Logger logger = LogManager.getLogger(HttpConn.class);
	
	private CloseableHttpClient client;
	private String brokerAddr;
	private int port;

	// TODO: plaintext credentials??
	public HttpConn(String host, int port, String user, String password) {
		this.brokerAddr = "http://" + host + ":" + port + "/api/";
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(host, port),
                // TODO: change default credentials
                new UsernamePasswordCredentials(user, password));
        client = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
	}
	
	/**
	 * Requests the metric to the monitoring api
	 * @param metric Name/path of metric to request. ex) connections
	 * @return Pretty-printed string of the json response
	 * @throws IOException
	 */
	public JsonElement get(String metric) throws IOException {
		HttpGet getReq = new HttpGet(brokerAddr + metric);
		CloseableHttpResponse response1 = client.execute(getReq);
		StatusLine status = response1.getStatusLine();
		
		System.out.println(status);
		if (status.getStatusCode() != 200) {
			logger.warn("No metric received: {} {}", status.getStatusCode(), status.getReasonPhrase());
			return null;
		}
		
		HttpEntity entity1 = response1.getEntity();
		JsonParser parser = new JsonParser();
		return parser.parse(new InputStreamReader(entity1.getContent()));
	}
	
}