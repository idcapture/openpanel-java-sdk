package fr.idcapture.openpanel.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.idcapture.openpanel.OpenPanelOptions;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal HTTP layer. Not part of the public API.
 *
 * <p>Handles building and sending requests to the OpenPanel /track endpoint
 * using OkHttp asynchronous calls wrapped in {@link CompletableFuture}.
 */
public final class HttpTracker {

    private static final Logger LOG = Logger.getLogger(HttpTracker.class.getName());
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String TRACK_PATH = "/track";
    private static final String SDK_NAME = "java";
    private static final String SDK_VERSION = "0.4.0";

    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final OpenPanelOptions options;

    public HttpTracker(OpenPanelOptions options) {
        this.options = options;
        this.mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(options.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(options.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }

    /**
     * Sends a typed event payload asynchronously with retry on failure.
     *
     * @param type    the event type (track, identify, increment, decrement, group, assign_group)
     * @param payload the payload object (will be serialized to JSON)
     * @return a {@link CompletableFuture} that completes when the request finishes
     */
    public CompletableFuture<Void> send(String type, Object payload) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        body.put("payload", payload);

        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Failed to serialize payload", e));
            return future;
        }

        if (options.isVerbose()) {
            LOG.log(Level.INFO, "OpenPanel [{0}] POST {1}{2} — {3}",
                    new Object[]{type, options.getApiUrl(), TRACK_PATH, json});
        }

        Request request = buildRequest(json);
        return executeWithRetry(request, type, 0);
    }

    private Request buildRequest(String json) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(options.getApiUrl() + TRACK_PATH)
                .header("Content-Type", "application/json")
                .header("openpanel-client-id", options.getClientId())
                .header("openpanel-sdk-name", SDK_NAME)
                .header("openpanel-sdk-version", SDK_VERSION)
                .post(RequestBody.create(json, JSON));

        if (options.getClientSecret() != null) {
            requestBuilder.header("openpanel-client-secret", options.getClientSecret());
        }

        return requestBuilder.build();
    }

    private CompletableFuture<Void> executeWithRetry(Request request, String type, int attempt) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handleFailure(future, request, type, attempt, e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        if (options.isVerbose()) {
                            LOG.log(Level.INFO, "OpenPanel [{0}] OK ({1})",
                                    new Object[]{type, response.code()});
                        }
                        future.complete(null);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "(empty)";
                        OpenPanelApiException ex = new OpenPanelApiException(response.code(), errorBody);

                        if (response.code() >= 500) {
                            handleFailure(future, request, type, attempt, ex);
                        } else {
                            if (options.isVerbose()) {
                                LOG.log(Level.WARNING, "OpenPanel [{0}] failed: {1}",
                                        new Object[]{type, ex.getMessage()});
                            }
                            future.completeExceptionally(ex);
                        }
                    }
                } catch (IOException e) {
                    handleFailure(future, request, type, attempt, e);
                }
            }
        });

        return future;
    }

    private void handleFailure(CompletableFuture<Void> future, Request request,
                               String type, int attempt, Exception e) {
        int maxRetries = options.getMaxRetries();
        if (attempt < maxRetries) {
            long delay = options.getInitialRetryDelayMs() * (1L << attempt);
            if (options.isVerbose()) {
                LOG.log(Level.WARNING, "OpenPanel [{0}] attempt {1}/{2} failed, retrying in {3}ms: {4}",
                        new Object[]{type, attempt + 1, maxRetries, delay, e.getMessage()});
            }
            client.dispatcher().executorService().execute(() -> {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    future.completeExceptionally(e);
                    return;
                }
                executeWithRetry(request, type, attempt + 1)
                        .whenComplete((v, ex) -> {
                            if (ex != null) future.completeExceptionally(ex);
                            else future.complete(null);
                        });
            });
        } else {
            if (options.isVerbose()) {
                LOG.log(Level.SEVERE, "OpenPanel [{0}] failed after {1} attempts: {2}",
                        new Object[]{type, maxRetries + 1, e.getMessage()});
            }
            future.completeExceptionally(e);
        }
    }

    /**
     * Shuts down the underlying OkHttp dispatcher. Call this when the SDK is no longer needed.
     */
    public void shutdown() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    /**
     * Exception thrown when the OpenPanel API returns a non-2xx response.
     */
    public static final class OpenPanelApiException extends RuntimeException {

        private final int statusCode;

        public OpenPanelApiException(int statusCode, String body) {
            super("OpenPanel API error " + statusCode + ": " + body);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
