package fr.idcapture.openpanel;

import fr.idcapture.openpanel.internal.HttpTracker;
import fr.idcapture.openpanel.model.AssignGroupPayload;
import fr.idcapture.openpanel.model.DecrementPayload;
import fr.idcapture.openpanel.model.GroupPayload;
import fr.idcapture.openpanel.model.IdentifyPayload;
import fr.idcapture.openpanel.model.IncrementPayload;
import fr.idcapture.openpanel.model.TrackPayload;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main entry point for the OpenPanel Java SDK.
 *
 * <p>Create an instance via the builder, then call tracking methods:
 * <pre>{@code
 * OpenPanel op = OpenPanel.builder()
 *     .clientId("YOUR_CLIENT_ID")
 *     .clientSecret("YOUR_CLIENT_SECRET")
 *     .build();
 *
 * // Fire-and-forget
 * op.track("button_clicked", Map.of("button_id", "submit"));
 *
 * // With callback on error
 * op.track("purchase", Map.of("amount", 99.99))
 *   .exceptionally(e -> { logger.error("Tracking failed", e); return null; });
 *
 * // Shutdown when done (e.g. on app stop)
 * op.close();
 * }</pre>
 *
 * <p>All methods are async and return a {@link CompletableFuture}. You can safely
 * ignore the future (fire-and-forget) or chain callbacks on it.
 *
 * <p>This class is thread-safe.
 */
public final class OpenPanel implements AutoCloseable {

    private final OpenPanelOptions options;
    private final HttpTracker tracker;
    private volatile Map<String, @Nullable Object> globalProperties = Collections.emptyMap();

    private OpenPanel(OpenPanelOptions options) {
        this.options = options;
        this.tracker = new HttpTracker(options);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Returns a new builder backed by {@link OpenPanelOptions.Builder}.
     */
    public static OpenPanelOptions.Builder builder() {
        return OpenPanelOptions.builder();
    }

    /**
     * Creates an {@link OpenPanel} instance from the given options.
     */
    public static OpenPanel create(OpenPanelOptions options) {
        return new OpenPanel(options);
    }

    // -------------------------------------------------------------------------
    // Global properties
    // -------------------------------------------------------------------------

    /**
     * Sets properties that are automatically merged into every {@link #track} call.
     * Calling this method again replaces all existing global properties.
     */
    public void setGlobalProperties(@Nullable Map<String, @Nullable Object> properties) {
        if (properties == null || properties.isEmpty()) {
            this.globalProperties = Collections.emptyMap();
        } else {
            this.globalProperties = Collections.unmodifiableMap(new HashMap<>(properties));
        }
    }

    /**
     * Returns an unmodifiable view of the current global properties.
     */
    public Map<String, @Nullable Object> getGlobalProperties() {
        return globalProperties;
    }

    // -------------------------------------------------------------------------
    // track
    // -------------------------------------------------------------------------

    /**
     * Tracks an event with no additional properties.
     */
    public CompletableFuture<Void> track(String eventName) {
        return track(eventName, null, null, null);
    }

    /**
     * Tracks an event with custom properties.
     */
    public CompletableFuture<Void> track(String eventName, @Nullable Map<String, @Nullable Object> properties) {
        return track(eventName, properties, null, null);
    }

    /**
     * Tracks an event associated with a specific user.
     */
    public CompletableFuture<Void> track(String eventName, @Nullable Map<String, @Nullable Object> properties, @Nullable String profileId) {
        return track(eventName, properties, profileId, null);
    }

    /**
     * Tracks an event associated with a user and one or more groups.
     *
     * <p>Note: groups are <strong>not</strong> populated automatically even if the profile
     * has been assigned via {@link #assignGroup}. Pass them explicitly on each call.
     */
    public CompletableFuture<Void> track(String eventName, @Nullable Map<String, @Nullable Object> properties,
                                         @Nullable String profileId, @Nullable List<String> groups) {
        if (isFiltered(eventName)) {
            return CompletableFuture.completedFuture(null);
        }

        Map<String, Object> merged = mergeWithGlobal(properties);
        TrackPayload payload = new TrackPayload(eventName, merged, profileId, groups);
        return send("track", payload);
    }

    // -------------------------------------------------------------------------
    // identify
    // -------------------------------------------------------------------------

    /**
     * Identifies a user with optional custom properties.
     */
    public CompletableFuture<Void> identify(String profileId, @Nullable Map<String, @Nullable Object> properties) {
        return identify(profileId, null, null, null, properties);
    }

    /**
     * Identifies a user with standard profile fields and optional custom properties.
     */
    public CompletableFuture<Void> identify(String profileId, @Nullable String firstName, @Nullable String lastName,
                                            @Nullable String email, @Nullable Map<String, @Nullable Object> properties) {
        return identify(profileId, firstName, lastName, email, null, properties);
    }

    /**
     * Identifies a user with standard profile fields, avatar URL, and optional custom properties.
     */
    public CompletableFuture<Void> identify(String profileId, @Nullable String firstName, @Nullable String lastName,
                                            @Nullable String email, @Nullable String avatar,
                                            @Nullable Map<String, @Nullable Object> properties) {
        if (isDisabled()) {
            return CompletableFuture.completedFuture(null);
        }
        IdentifyPayload payload = new IdentifyPayload(profileId, firstName, lastName, email, avatar, properties);
        return send("identify", payload);
    }

    // -------------------------------------------------------------------------
    // increment / decrement
    // -------------------------------------------------------------------------

    /**
     * Increments a numeric property on a user profile.
     */
    public CompletableFuture<Void> increment(String profileId, String property, Number value) {
        if (isDisabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return send("increment", new IncrementPayload(profileId, property, value));
    }

    /**
     * Decrements a numeric property on a user profile.
     */
    public CompletableFuture<Void> decrement(String profileId, String property, Number value) {
        if (isDisabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return send("decrement", new DecrementPayload(profileId, property, value));
    }

    // -------------------------------------------------------------------------
    // revenue
    // -------------------------------------------------------------------------

    /**
     * Tracks a revenue event. Shorthand for tracking a "revenue" event with a {@code __revenue} property.
     */
    public CompletableFuture<Void> revenue(Number amount) {
        return revenue(amount, null, null);
    }

    /**
     * Tracks a revenue event associated with a specific user.
     */
    public CompletableFuture<Void> revenue(Number amount, @Nullable String profileId) {
        return revenue(amount, profileId, null);
    }

    /**
     * Tracks a revenue event with additional properties.
     */
    public CompletableFuture<Void> revenue(Number amount, @Nullable String profileId,
                                           @Nullable Map<String, @Nullable Object> properties) {
        Map<String, @Nullable Object> merged = new HashMap<>();
        if (properties != null) {
            merged.putAll(properties);
        }
        merged.put("__revenue", amount);
        return track("revenue", merged, profileId, null);
    }

    // -------------------------------------------------------------------------
    // group / assign_group
    // -------------------------------------------------------------------------

    /**
     * Creates or updates a group (e.g. a company, workspace, or team).
     */
    public CompletableFuture<Void> group(String groupId, String type, String name,
                                         @Nullable Map<String, @Nullable Object> properties) {
        if (isDisabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return send("group", new GroupPayload(groupId, type, name, properties));
    }

    /**
     * Assigns a user profile to one or more groups.
     *
     * <p>This updates the profile record. You still need to pass {@code groups} explicitly
     * on each {@link #track} call — they are not attached automatically.
     */
    public CompletableFuture<Void> assignGroup(String profileId, List<String> groupIds) {
        if (isDisabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return send("assign_group", new AssignGroupPayload(profileId, groupIds));
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Shuts down the underlying HTTP client. Call this when the SDK is no longer needed
     * (e.g. on application shutdown) to release threads and connections cleanly.
     *
     * <p>Implements {@link AutoCloseable} so it works with try-with-resources.
     */
    @Override
    public void close() {
        tracker.shutdown();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private CompletableFuture<Void> send(String type, Object payload) {
        if (isDisabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return tracker.send(type, payload);
    }

    private boolean isDisabled() {
        return options.isDisabled();
    }

    private boolean isFiltered(String eventName) {
        if (isDisabled()) {
            return true;
        }
        if (options.getFilter() != null) {
            return !options.getFilter().test(eventName);
        }
        return false;
    }

    /**
     * Merges caller-supplied properties with global properties.
     * Caller properties take precedence over global ones.
     */
    private @Nullable Map<String, @Nullable Object> mergeWithGlobal(@Nullable Map<String, @Nullable Object> properties) {
        Map<String, @Nullable Object> global = this.globalProperties;
        if (global.isEmpty()) {
            return properties;
        }
        Map<String, @Nullable Object> merged = new HashMap<>(global);
        if (properties != null) {
            merged.putAll(properties);
        }
        return merged;
    }
}
