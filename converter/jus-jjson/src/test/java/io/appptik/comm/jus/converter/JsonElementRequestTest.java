/*
 * Copyright (C) 2015 AppTik Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appptik.comm.jus.converter;


import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import io.apptik.comm.jus.Jus;
import io.apptik.comm.jus.Request;
import io.apptik.comm.jus.RequestQueue;
import io.apptik.comm.jus.request.JJsonArrayRequest;
import io.apptik.comm.jus.request.JJsonObjectRequest;
import io.apptik.json.JsonArray;
import io.apptik.json.JsonObject;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonElementRequestTest {
    class Service {

        Request<JsonObject> aJsonObject(JsonObject jsonObject) throws IOException {
            return queue.add(new JJsonObjectRequest("POST", server.url("/").toString())
                    .setRequestData(jsonObject));
        }

        Request<JsonArray> aJsonArray(JsonArray jsonArray) throws IOException {
            return queue.add(new JJsonArrayRequest("POST", server.url("/").toString())
                    .setRequestData(jsonArray));
        }

        Request<JsonObject> aJsonObjectGET() throws IOException {
            return queue.add(new JJsonObjectRequest("GET", server.url("/").toString()));
        }

        Request<JsonArray> aJsonArrayGET() throws IOException {
            return queue.add(new JJsonArrayRequest("GET", server.url("/").toString()));
        }
    }

    @Rule
    public final MockWebServer server = new MockWebServer();

    private Service service;
    private RequestQueue queue;

    @Before
    public void setUp() {
        queue = Jus.newRequestQueue();

        service = new Service();
    }

    @Test
    public void aJsonObject() throws IOException, InterruptedException, ExecutionException {
        server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));

        JsonObject body = service.aJsonObject(new JsonObject().put("name", "value")).getFuture().get();
        assertThat(body.get("theName")).isEqualTo("value");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
        assertThat(request.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    public void aJsonArray() throws IOException, InterruptedException, ExecutionException {
        server.enqueue(new MockResponse().setBody("[\"theName\",\"value\"]"));

        JsonArray body = service.aJsonArray(new JsonArray().put("name").put("value"))
                .getFuture().get();
        assertThat(body.get(0)).isEqualTo("theName");
        assertThat(body.get(1)).isEqualTo("value");

        RecordedRequest request = server.takeRequest();

        assertThat(request.getBody().readUtf8()).isEqualTo("[\"name\",\"value\"]");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
        assertThat(request.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    public void aJsonObjectGET() throws IOException, InterruptedException, ExecutionException {
        server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));

        JsonObject body = service.aJsonObjectGET().getFuture().get();
        assertThat(body.get("theName")).isEqualTo("value");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getBody().size()).isEqualTo(0);
        assertThat(request.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    public void aJsonArrayGET() throws IOException, InterruptedException, ExecutionException {
        server.enqueue(new MockResponse().setBody("[\"theName\",\"value\"]"));

        JsonArray body = service.aJsonArrayGET()
                .getFuture().get();
        assertThat(body.get(0)).isEqualTo("theName");
        assertThat(body.get(1)).isEqualTo("value");

        RecordedRequest request = server.takeRequest();

        assertThat(request.getBody().size()).isEqualTo(0);
        assertThat(request.getHeader("Accept")).isEqualTo("application/json");
    }

    @After
    public void after() {
        queue.stopWhenDone();
    }
}
