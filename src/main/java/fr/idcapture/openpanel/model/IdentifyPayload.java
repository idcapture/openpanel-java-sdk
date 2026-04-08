package fr.idcapture.openpanel.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Payload for an "identify" call.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentifyPayload {

    private final String profileId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final Map<String, @Nullable Object> properties;

    public IdentifyPayload(String profileId, String firstName, String lastName, String email,
                           Map<String, @Nullable Object> properties) {
        if (profileId == null || profileId.isEmpty()) {
            throw new IllegalArgumentException("profileId must not be null or empty");
        }
        this.profileId = profileId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.properties = properties != null ? Collections.unmodifiableMap(properties) : null;
    }

    public IdentifyPayload(String profileId, Map<String, @Nullable Object> properties) {
        this(profileId, null, null, null, properties);
    }

    public String getProfileId() {
        return profileId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Map<String, @Nullable Object> getProperties() {
        return properties;
    }
}
