package fr.idcapture.openpanel;

import java.util.function.Predicate;

/**
 * Immutable configuration for the OpenPanel SDK.
 *
 * <p>Build instances via {@link Builder}:
 * <pre>{@code
 * OpenPanelOptions options = OpenPanelOptions.builder()
 *     .clientId("YOUR_CLIENT_ID")
 *     .clientSecret("YOUR_CLIENT_SECRET")
 *     .build();
 * }</pre>
 */
public final class OpenPanelOptions {

    private static final String DEFAULT_API_URL = "https://api.openpanel.dev";

    private final String clientId;
    private final String clientSecret;
    private final String apiUrl;
    private final boolean disabled;
    private final Predicate<String> filter;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;

    private OpenPanelOptions(Builder builder) {
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.apiUrl = builder.apiUrl != null ? builder.apiUrl : DEFAULT_API_URL;
        this.disabled = builder.disabled;
        this.filter = builder.filter;
        this.connectTimeoutSeconds = builder.connectTimeoutSeconds;
        this.readTimeoutSeconds = builder.readTimeoutSeconds;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Optional predicate that receives the event name.
     * Return {@code false} to drop the event before sending.
     */
    public Predicate<String> getFilter() {
        return filter;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String clientId;
        private String clientSecret;
        private String apiUrl;
        private boolean disabled = false;
        private Predicate<String> filter;
        private int connectTimeoutSeconds = 10;
        private int readTimeoutSeconds = 30;

        private Builder() {}

        /**
         * Required. Your OpenPanel client ID.
         */
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * Optional. Your OpenPanel client secret (required for server-side calls).
         */
        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        /**
         * Optional. Custom API base URL (e.g. for self-hosted instances).
         * Defaults to {@code https://api.openpanel.dev}.
         */
        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        /**
         * When {@code true}, no events are sent. Useful for local dev/tests.
         */
        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * Optional filter predicate on event names.
         * Return {@code false} to drop an event silently.
         */
        public Builder filter(Predicate<String> filter) {
            this.filter = filter;
            return this;
        }

        /**
         * HTTP connect timeout in seconds. Defaults to 10.
         */
        public Builder connectTimeoutSeconds(int seconds) {
            this.connectTimeoutSeconds = seconds;
            return this;
        }

        /**
         * HTTP read timeout in seconds. Defaults to 30.
         */
        public Builder readTimeoutSeconds(int seconds) {
            this.readTimeoutSeconds = seconds;
            return this;
        }

        public OpenPanelOptions build() {
            if (clientId == null || clientId.isEmpty()) {
                throw new IllegalStateException("clientId is required");
            }
            return new OpenPanelOptions(this);
        }
    }
}
