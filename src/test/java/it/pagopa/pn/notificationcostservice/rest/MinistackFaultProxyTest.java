package it.pagopa.pn.notificationcostservice.rest;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MinistackFaultProxyTest {
    @Test
    @Timeout(10)
    void faultsOnlyTheSelectedOperationAndScenarioThenRestoresForwarding() throws Exception {
        var forwarded = new AtomicInteger();
        var backend = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        backend.createContext("/aws", exchange -> {
            try (exchange) {
                forwarded.incrementAndGet();
                byte[] body = exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/x-amz-json-1.0");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        backend.start();
        try (var proxy = new MinistackFaultProxy(URI.create("http://127.0.0.1:" + backend.getAddress().getPort()));
             var client = HttpClient.newHttpClient()) {
            proxy.fail("UpdateItem", "scenario");
            assertEquals(200, send(client, proxy, "GetItem", "scenario").statusCode(),
                    "The preliminary existence lookup must not fail");
            var otherScenario = send(client, proxy, "UpdateItem", "other");
            assertEquals(200, otherScenario.statusCode());
            assertEquals("other", otherScenario.body());
            assertEquals("application/x-amz-json-1.0", otherScenario.headers().firstValue("Content-Type").orElseThrow());
            var failed = send(client, proxy, "UpdateItem", "scenario");
            assertEquals(500, failed.statusCode());
            assertTrue(failed.body().contains("Controlled contract-test fault"));
            assertEquals(2, forwarded.get(), "Faulted writes must not reach MiniStack");
            assertEquals(1, proxy.recover());
            var recovered = send(client, proxy, "UpdateItem", "scenario");
            assertEquals(200, recovered.statusCode());
            assertEquals("scenario", recovered.body());
            assertEquals(3, forwarded.get());
        } finally {
            backend.stop(0);
        }
    }

    private HttpResponse<String> send(HttpClient client, MinistackFaultProxy proxy, String operation, String body)
            throws Exception {
        return client.send(HttpRequest.newBuilder(proxy.endpoint().resolve("/aws?fixture=1"))
                .timeout(Duration.ofSeconds(3)).header("X-Amz-Target", "DynamoDB_20120810." + operation)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
