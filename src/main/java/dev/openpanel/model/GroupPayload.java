package dev.openpanel.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.Map;

/**
 * Payload for creating or updating a group.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupPayload {

    private final String id;
    private final String type;
    private final String name;
    private final Map<String, Object> properties;

    public GroupPayload(String id, String type, String name, Map<String, Object> properties) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("group id must not be null or empty");
        }
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("group type must not be null or empty");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("group name must not be null or empty");
        }
        this.id = id;
        this.type = type;
        this.name = name;
        this.properties = properties != null ? Collections.unmodifiableMap(properties) : null;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
