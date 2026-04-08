package dev.openpanel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Payload for a "track" event.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackPayload {

    private final String name;
    private final Map<String, @Nullable Object> properties;
    private final String profileId;
    private final List<String> groups;

    public TrackPayload(String name, Map<String, @Nullable Object> properties, String profileId, List<String> groups) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Event name must not be null or empty");
        }
        this.name = name;
        this.properties = properties != null ? Collections.unmodifiableMap(properties) : null;
        this.profileId = profileId;
        this.groups = groups != null ? Collections.unmodifiableList(groups) : null;
    }

    public TrackPayload(String name, Map<String, @Nullable Object> properties) {
        this(name, properties, null, null);
    }

    public String getName() {
        return name;
    }

    public Map<String, @Nullable Object> getProperties() {
        return properties;
    }

    public String getProfileId() {
        return profileId;
    }

    public List<String> getGroups() {
        return groups;
    }
}
