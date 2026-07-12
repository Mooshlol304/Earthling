/*
 * Earthling
 * Copyright (c) 2025 Moosh
 *
 * Earthling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Earthling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Earthling. If not, see
 * <https://www.gnu.org/licenses/>.
 */

package xyz.moosh.earthling.client.network;

import xyz.moosh.earthling.client.EarthlingClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thin async HTTP wrapper used by {@link EarthMCApi}.
 * Supports GET and POST with JSON body.
 * Auto-retries up to 3x with exponential back-off.
 */
public class HttpClient {

    private static final int      MAX_RETRIES = 3;
    private static final Duration TIMEOUT     = Duration.ofSeconds(5);
    private static final long     RETRY_DELAY = 500L;

    private final java.net.http.HttpClient client;
    private final ExecutorService          executor;

    public HttpClient() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        client   = java.net.http.HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .executor(executor)
                .build();
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Async GET — returns raw response body. */
    public CompletableFuture<String> getRaw(String url) {
        return CompletableFuture.supplyAsync(() -> retry(url, null, 0), executor);
    }

    /**
     * Async POST with a JSON string body — returns raw response body.
     *
     * @param url  full URL
     * @param json JSON request body
     */
    public CompletableFuture<String> postRaw(String url, String json) {
        return CompletableFuture.supplyAsync(() -> retry(url, json, 0), executor);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private String retry(String url, String jsonBody, int attempt) {
        try {
            return fetch(url, jsonBody);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                throw new RuntimeException(e);
            }

            if (attempt < MAX_RETRIES) {
                sleep(RETRY_DELAY * (1L << attempt));
                return retry(url, jsonBody, attempt + 1);
            }
            EarthlingClient.LOGGER.warn("HTTP request failed after {} attempts: {} — {}",
                    MAX_RETRIES, url, e.getMessage());
            throw new RuntimeException("HTTP request failed: " + url, e);
        }
    }

    private String fetch(String url, String jsonBody) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", "Earthling-Mod/1.0");

        if (jsonBody != null) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            builder.GET();
        }

        HttpResponse<String> response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status == 429) throw new IOException("Rate limited (429)");
        if (status == 503) throw new IOException("Service unavailable (503)");
        if (status < 200 || status >= 300) throw new IOException("HTTP " + status);

        return response.body();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}