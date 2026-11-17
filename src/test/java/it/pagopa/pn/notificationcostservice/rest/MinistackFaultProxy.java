package it.pagopa.pn.notificationcostservice.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

final class MinistackFaultProxy implements AutoCloseable {
    private static final Set<String> HOP_HEADERS = Set.of(
            "host", "content-length", "connection", "transfer-encoding", "upgrade", "expect", "http2-settings");
    record Call(String operation, String body) {}

    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_1_1).build();
    private final URI backend;
    private volatile Call fault;
    private final AtomicInteger injected = new AtomicInteger();
    final CopyOnWriteArrayList<Call> calls = new CopyOnWriteArrayList<>();

    MinistackFaultProxy(URI backend) throws IOException {
        this.backend = backend;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/", this::forward);
        server.start();
    }

    URI endpoint() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    void fail(String operation, String bodyContains) {
        injected.set(0);
        fault = new Call(operation, bodyContains);
    }

    int recover() {
        fault = null;
        return injected.get();
    }

    private void forward(HttpExchange exchange) throws IOException {
        try (exchange) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String operation = exchange.getRequestHeaders().getFirst("X-Amz-Target");
            var call = new Call(operation == null ? "" : operation, new String(body, StandardCharsets.UTF_8));
            calls.add(call);
            Call active = fault;
            if (active != null && call.operation().endsWith("." + active.operation())
                    && call.body().contains(active.body())) {
                injected.incrementAndGet();
                byte[] error = "{\"__type\":\"InternalFailure\",\"message\":\"Controlled contract-test fault\"}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/x-amz-json-1.0");
                exchange.getResponseHeaders().set("x-amzn-ErrorType", "InternalFailure");
                exchange.sendResponseHeaders(500, error.length);
                exchange.getResponseBody().write(error);
                return;
            }
            String query = exchange.getRequestURI().getRawQuery();
            URI destination = URI.create(backend + exchange.getRequestURI().getRawPath()
                    + (query == null ? "" : "?" + query));
            var request = HttpRequest.newBuilder(destination)
                    .timeout(Duration.ofSeconds(30)).method(exchange.getRequestMethod(),
                            HttpRequest.BodyPublishers.ofByteArray(body));
            exchange.getRequestHeaders().forEach((name, values) -> {
                if (!HOP_HEADERS.contains(name.toLowerCase(java.util.Locale.ROOT))) {
                    values.forEach(value -> request.header(name, value));
                }
            });
            HttpResponse<byte[]> response;
            try {
                response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("MiniStack proxy interrupted", interrupted);
            }
            response.headers().map().forEach((name, values) -> {
                if (!HOP_HEADERS.contains(name.toLowerCase(java.util.Locale.ROOT))) {
                    exchange.getResponseHeaders().put(name, values);
                }
            });
            exchange.sendResponseHeaders(response.statusCode(), response.body().length);
            exchange.getResponseBody().write(response.body());
        } catch (IOException failure) {
            Logger.getLogger(MinistackFaultProxy.class.getName()).log(Level.SEVERE,
                    "MiniStack proxy transport failed", failure);
            throw failure;
        }
    }

    @Override
    public void close() {
        server.stop(0);
        client.close();
        executor.close();
    }
}
