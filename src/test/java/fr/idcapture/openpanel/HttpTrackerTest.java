package fr.idcapture.openpanel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.idcapture.openpanel.internal.HttpTracker;
import fr.idcapture.openpanel.model.AssignGroupPayload;
import fr.idcapture.openpanel.model.GroupPayload;
import fr.idcapture.openpanel.model.IdentifyPayload;
import fr.idcapture.openpanel.model.IncrementPayload;
import fr.idcapture.openpanel.model.TrackPayload;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link HttpTracker} using OkHttp {@link MockWebServer}.
 *
 * <p>These tests verify that the correct HTTP requests (headers, body, path)
 * are sent without hitting a real OpenPanel server.
 */
class HttpTrackerTest {

    private MockWebServer server;
    private HttpTracker tracker;
    private OpenPanel openPanel;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("").toString();
        // Strip trailing slash to match how OpenPanelOptions.getApiUrl() is used
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        OpenPanelOptions opts = OpenPanelOptions.builder()
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .apiUrl(baseUrl)
                .build();

        tracker = new HttpTracker(opts);
        openPanel = OpenPanel.create(opts);
    }

    @AfterEach
    void tearDown() throws Exception {
        openPanel.close();
        tracker.shutdown();
        server.shutdown();
    }

    // -------------------------------------------------------------------------
    // Headers
    // -------------------------------------------------------------------------

    @Test
    void track_sendsCorrectHeaders() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("track", new TrackPayload("test_event", null)).get();

        RecordedRequest req = server.takeRequest();
        assertEquals("test-client-id", req.getHeader("openpanel-client-id"));
        assertEquals("test-client-secret", req.getHeader("openpanel-client-secret"));
        assertTrue(req.getHeader("Content-Type").startsWith("application/json"));
    }

    @Test
    void track_pathIsSlashTrack() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("track", new TrackPayload("test_event", null)).get();

        RecordedRequest req = server.takeRequest();
        assertEquals("/track", req.getPath());
        assertEquals("POST", req.getMethod());
    }

    @Test
    void noClientSecret_headerNotSent() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        OpenPanelOptions opts = OpenPanelOptions.builder()
                .clientId("client-only")
                .apiUrl(server.url("").toString().replaceAll("/$", ""))
                .build();
        HttpTracker noSecretTracker = new HttpTracker(opts);

        noSecretTracker.send("track", new TrackPayload("e", null)).get();

        RecordedRequest req = server.takeRequest();
        assertNull(req.getHeader("openpanel-client-secret"));
        noSecretTracker.shutdown();
    }

    // -------------------------------------------------------------------------
    // track payload
    // -------------------------------------------------------------------------

    @Test
    void track_bodyContainsTypeAndName() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("track", new TrackPayload("button_clicked", Map.of("id", "submit"))).get();

        RecordedRequest req = server.takeRequest();
        JsonNode body = mapper.readTree(req.getBody().readUtf8());

        assertEquals("track", body.get("type").asText());
        assertEquals("button_clicked", body.get("payload").get("name").asText());
        assertEquals("submit", body.get("payload").get("properties").get("id").asText());
    }

    @Test
    void track_withProfileIdAndGroups() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("track", new TrackPayload("page_view", null, "user123", List.of("org_acme"))).get();

        RecordedRequest req = server.takeRequest();
        JsonNode payload = mapper.readTree(req.getBody().readUtf8()).get("payload");

        assertEquals("user123", payload.get("profileId").asText());
        assertEquals("org_acme", payload.get("groups").get(0).asText());
    }

    @Test
    void track_noProperties_propertiesFieldAbsent() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("track", new TrackPayload("simple_event", null)).get();

        RecordedRequest req = server.takeRequest();
        JsonNode payload = mapper.readTree(req.getBody().readUtf8()).get("payload");

        assertTrue(payload.get("properties") == null || payload.get("properties").isNull());
    }

    // -------------------------------------------------------------------------
    // identify payload
    // -------------------------------------------------------------------------

    @Test
    void identify_bodyContainsAllFields() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("identify", new IdentifyPayload(
                "user123", "John", "Doe", "john@example.com",
                Map.of("tier", "premium")
        )).get();

        RecordedRequest req = server.takeRequest();
        JsonNode body = mapper.readTree(req.getBody().readUtf8());

        assertEquals("identify", body.get("type").asText());
        JsonNode payload = body.get("payload");
        assertEquals("user123", payload.get("profileId").asText());
        assertEquals("John", payload.get("firstName").asText());
        assertEquals("Doe", payload.get("lastName").asText());
        assertEquals("john@example.com", payload.get("email").asText());
        assertEquals("premium", payload.get("properties").get("tier").asText());
    }

    // -------------------------------------------------------------------------
    // increment / decrement
    // -------------------------------------------------------------------------

    @Test
    void increment_bodyCorrect() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("increment", new IncrementPayload("user1", "login_count", 1)).get();

        RecordedRequest req = server.takeRequest();
        JsonNode body = mapper.readTree(req.getBody().readUtf8());

        assertEquals("increment", body.get("type").asText());
        assertEquals("user1", body.get("payload").get("profileId").asText());
        assertEquals("login_count", body.get("payload").get("property").asText());
        assertEquals(1, body.get("payload").get("value").asInt());
    }

    @Test
    void decrement_bodyCorrect() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("decrement", new fr.idcapture.openpanel.model.DecrementPayload("user1", "credits", 5)).get();

        RecordedRequest req = server.takeRequest();
        JsonNode body = mapper.readTree(req.getBody().readUtf8());

        assertEquals("decrement", body.get("type").asText());
        assertEquals(5, body.get("payload").get("value").asInt());
    }

    // -------------------------------------------------------------------------
    // group / assign_group
    // -------------------------------------------------------------------------

    @Test
    void group_bodyCorrect() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("group", new GroupPayload(
                "org_acme", "company", "Acme Inc", Map.of("plan", "enterprise")
        )).get();

        RecordedRequest req = server.takeRequest();
        JsonNode body = mapper.readTree(req.getBody().readUtf8());

        assertEquals("group", body.get("type").asText());
        JsonNode payload = body.get("payload");
        assertEquals("org_acme", payload.get("id").asText());
        assertEquals("company", payload.get("type").asText());
        assertEquals("Acme Inc", payload.get("name").asText());
        assertEquals("enterprise", payload.get("properties").get("plan").asText());
    }

    @Test
    void assignGroup_bodyCorrect() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        tracker.send("assign_group", new AssignGroupPayload("user123", List.of("org_acme", "team_alpha"))).get();

        RecordedRequest req = server.takeRequest();
        JsonNode body = mapper.readTree(req.getBody().readUtf8());

        assertEquals("assign_group", body.get("type").asText());
        assertEquals("user123", body.get("payload").get("profileId").asText());
        assertEquals(2, body.get("payload").get("groupIds").size());
    }

    // -------------------------------------------------------------------------
    // Error handling
    // -------------------------------------------------------------------------

    @Test
    void apiError_4xx_futureFailsWithException() {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\": \"Invalid client credentials\"}"));

        CompletableFuture<Void> future = tracker.send("track", new TrackPayload("event", null));

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(HttpTracker.OpenPanelApiException.class, ex.getCause());
        assertEquals(401, ((HttpTracker.OpenPanelApiException) ex.getCause()).getStatusCode());
    }

    @Test
    void apiError_5xx_futureFailsWithException() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        CompletableFuture<Void> future = tracker.send("track", new TrackPayload("event", null));

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(HttpTracker.OpenPanelApiException.class, ex.getCause());
        assertEquals(500, ((HttpTracker.OpenPanelApiException) ex.getCause()).getStatusCode());
    }

    // -------------------------------------------------------------------------
    // globalProperties merged in OpenPanel
    // -------------------------------------------------------------------------

    @Test
    void globalProperties_mergedIntoTrackPayload() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        openPanel.setGlobalProperties(Map.of("env", "production", "version", "2.0"));
        openPanel.track("checkout", Map.of("amount", 49.99)).get();

        RecordedRequest req = server.takeRequest();
        JsonNode props = mapper.readTree(req.getBody().readUtf8())
                .get("payload").get("properties");

        assertEquals("production", props.get("env").asText());
        assertEquals("2.0", props.get("version").asText());
        assertEquals(49.99, props.get("amount").asDouble(), 0.001);
    }

    @Test
    void callerProperties_overrideGlobalProperties() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        openPanel.setGlobalProperties(Map.of("env", "production"));
        openPanel.track("event", Map.of("env", "staging")).get();

        RecordedRequest req = server.takeRequest();
        JsonNode props = mapper.readTree(req.getBody().readUtf8())
                .get("payload").get("properties");

        // Caller value "staging" must win over global "production"
        assertEquals("staging", props.get("env").asText());
    }
}
