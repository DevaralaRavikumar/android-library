/* Copyright 2018 Urban Airship and Contributors */

package com.urbanairship.remotedata;

import android.support.annotation.NonNull;

import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.BaseTestCase;
import com.urbanairship.TestRequest;
import com.urbanairship.UAirship;
import com.urbanairship.http.Request;
import com.urbanairship.http.RequestFactory;
import com.urbanairship.http.Response;
import com.urbanairship.json.JsonList;
import com.urbanairship.json.JsonMap;
import com.urbanairship.util.DateUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class RemoteDataApiClientTest extends BaseTestCase {

    private TestRequest testRequest;
    private RemoteDataApiClient client;

    @Before
    public void setUp() {
        AirshipConfigOptions configOptions = new AirshipConfigOptions.Builder()
                .setDevelopmentAppKey("appKey")
                .setDevelopmentAppSecret("appSecret")
                .setInProduction(false)
                .build();

        testRequest = new TestRequest();
        RequestFactory requestFactory = new RequestFactory() {
            @NonNull
            @Override
            public Request createRequest(String requestMethod, URL url) {
                testRequest.setRequestMethod(requestMethod);
                testRequest.setURL(url);
                return testRequest;
            }
        };

        client = new RemoteDataApiClient(configOptions, requestFactory);
    }

    /**
     * Test fetch remote data request on success
     */
    @Test
    public void testFetchRemoteDataRequestSuccess() throws Exception {
        String responseTimestamp = DateUtils.createIso8601TimeStamp(System.currentTimeMillis());

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Last-Modified", Arrays.asList(new String[] { responseTimestamp }));

        JsonMap map = JsonMap.newBuilder().put("foo", "bar").build();
        JsonMap payload = JsonMap.newBuilder().put("type", "test").put("timestamp", responseTimestamp).put("data", map).build();
        JsonList list = new JsonList(Arrays.asList(payload.toJsonValue()));

        testRequest.response = Response.newBuilder(HttpURLConnection.HTTP_OK)
                .setResponseHeaders(headers)
                .setResponseMessage("OK")
                .setResponseBody(list.toString())
                .build();

        String requestTimestamp = DateUtils.createIso8601TimeStamp(0);
        Response response = client.fetchRemoteData(requestTimestamp);

        assertEquals("https://remote-data.urbanairship.com/api/remote-data/app/appKey/android?sdk_version=" + UAirship.getVersion(), testRequest.getURL().toString());
        assertEquals("Headers should contain timestamp", testRequest.getRequestHeaders().get("If-Modified-Since"), requestTimestamp);
        assertNotNull("Response should not be null", response);
        assertEquals("Response status should be 200", HttpURLConnection.HTTP_OK, response.getStatus());
        assertEquals("Response should be the JSON list", response.getResponseBody(), list.toString());
        assertEquals("Last-Modified should match with timestamp", responseTimestamp, response.getResponseHeader("Last-Modified"));
    }

    /**
     * Test fetch remote data request on success with no timestamp
     */
    @Test
    public void testFetchRemoteDataRequestNoTimestamp() throws Exception {
        String responseTimestamp = DateUtils.createIso8601TimeStamp(System.currentTimeMillis());

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Last-Modified", Arrays.asList(new String[] { responseTimestamp }));

        JsonMap map = JsonMap.newBuilder().put("foo", "bar").build();
        JsonMap payload = JsonMap.newBuilder().put("type", "test").put("timestamp", responseTimestamp).put("data", map).build();
        JsonList list = new JsonList(Arrays.asList(payload.toJsonValue()));

        testRequest.response = Response.newBuilder(HttpURLConnection.HTTP_OK)
                .setResponseHeaders(headers)
                .setResponseMessage("OK")
                .setResponseBody(list.toString())
                .build();

        Response response = client.fetchRemoteData(null);

        assertNull("Headers should not contain timestamp", testRequest.getRequestHeaders().get("If-Modified-Since"));
        assertNotNull("Response should not be null", response);
        assertEquals("Response status should be 200", HttpURLConnection.HTTP_OK, response.getStatus());
        assertEquals("Response should be the JSON list", response.getResponseBody(), list.toString());
        assertEquals("Last-Modified should match with timestamp", responseTimestamp, response.getResponseHeader("Last-Modified"));
    }

    /**
     * Test fetch remote data request on failure
     */
    @Test
    public void testFetchRemoteDataRequestFailure() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();

        testRequest.response = Response.newBuilder(HttpURLConnection.HTTP_NOT_IMPLEMENTED)
                .setResponseHeaders(headers)
                .setResponseMessage("Not Implemented")
                .build();

        String requestTimestamp = DateUtils.createIso8601TimeStamp(0);
        Response response = client.fetchRemoteData(requestTimestamp);

        assertNotNull("Response should not be null", response);
        assertEquals("Response status should be 501", HttpURLConnection.HTTP_NOT_IMPLEMENTED, response.getStatus());
    }
}
