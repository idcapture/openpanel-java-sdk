package fr.idcapture.openpanel.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 * Payload for assigning a user to one or more groups.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignGroupPayload {

    private final String profileId;
    private final List<String> groupIds;

    public AssignGroupPayload(String profileId, List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            throw new IllegalArgumentException("groupIds must not be null or empty");
        }
        this.profileId = profileId;
        this.groupIds = Collections.unmodifiableList(groupIds);
    }

    public String getProfileId() {
        return profileId;
    }

    public List<String> getGroupIds() {
        return groupIds;
    }
}
