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

/**
 * Internal HTTP layer. Not part of the public API.
 *
 * <p>Handles building and sending requests to the OpenPanel /track endpoint
 * using OkHttp asynchronous calls wrapped in {@link CompletableFuture}.
 */
public final class HttpTracker {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String TRACK_PATH = "/track";
    private static final String SDK_NAME = "java";
    private static final String SDK_VERSION = "0.3.0";

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
     * Sends a typed event payload asynchronously.
     *
     * @param type    the event type (track, identify, increment, decrement, group, assign_group)
     * @param payload the payload object (will be serialized to JSON)
     * @return a {@link CompletableFuture} that completes when the request finishes
     */
    public CompletableFuture<Void> send(String type, Object payload) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        body.put("payload", payload);

        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            future.completeExceptionally(new IllegalStateException("Failed to serialize payload", e));
            return future;
        }

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

        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        future.complete(null);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "(empty)";
                        future.completeExceptionally(
                                new OpenPanelApiException(response.code(), errorBody)
                        );
                    }
                } catch (IOException e) {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
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
