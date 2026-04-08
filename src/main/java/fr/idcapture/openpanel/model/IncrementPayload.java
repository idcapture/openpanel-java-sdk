package fr.idcapture.openpanel.model;

/**
 * Payload for an "increment" call.
 */
public class IncrementPayload {

    private final String profileId;
    private final String property;
    private final Number value;

    public IncrementPayload(String profileId, String property, Number value) {
        if (profileId == null || profileId.isEmpty()) {
            throw new IllegalArgumentException("profileId must not be null or empty");
        }
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("property must not be null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        this.profileId = profileId;
        this.property = property;
        this.value = value;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getProperty() {
        return property;
    }

    public Number getValue() {
        return value;
    }
}
