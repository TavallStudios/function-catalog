package org.tavall.ai.review.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.review.PublishRepositoryReviewRequest;
import org.tavall.ai.review.RepositoryPublishedReview;
import org.tavall.ai.review.RepositoryReviewComment;
import org.tavall.ai.review.RepositoryReviewCoordinates;
import org.tavall.ai.review.RepositoryReviewDecision;
import org.tavall.ai.review.RepositoryReviewProvider;
import org.tavall.ai.review.RepositoryReviewSnapshot;
import org.tavall.ai.review.RepositoryReviewThread;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** GitHub REST source/publisher for exact-head review transport. */
public final class GitHubRepositoryReviewProvider implements RepositoryReviewProvider {
    private static final String JSON_ACCEPT = "application/vnd.github+json";
    private static final String DIFF_ACCEPT = "application/vnd.github.v3.diff";

    private final GitHubRepositoryReviewConfiguration configuration;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GitHubRepositoryReviewProvider(GitHubRepositoryReviewConfiguration configuration) {
        this(configuration, new ObjectMapper().findAndRegisterModules(),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    GitHubRepositoryReviewProvider(GitHubRepositoryReviewConfiguration configuration, ObjectMapper objectMapper, HttpClient httpClient) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public static GitHubRepositoryReviewProvider fromEnvironment() {
        return new GitHubRepositoryReviewProvider(GitHubRepositoryReviewConfiguration.fromEnvironment());
    }

    @Override
    public RepositoryReviewSnapshot inspectPullRequest(RepositoryReviewCoordinates repository, int pullRequestNumber) {
        requireAllowed(repository);
        String pullPath = repoPath(repository) + "/pulls/" + pullRequestNumber;
        JsonNode pull = sendJson("GET", pullPath, null, 200);
        String diff = sendText("GET", pullPath, null, DIFF_ACCEPT, 200);
        List<String> changedFiles = new ArrayList<>();
        for (int page = 1; page <= 30; page++) {
            JsonNode files = sendJson("GET", pullPath + "/files?per_page=100&page=" + page, null, 200);
            int count = 0;
            for (JsonNode file : files) {
                changedFiles.add(file.path("filename").asText());
                count++;
            }
            if (count < 100) break;
        }
        return new RepositoryReviewSnapshot(
                repository,
                pullRequestNumber,
                pull.path("base").path("ref").asText(),
                pull.path("base").path("sha").asText(),
                pull.path("head").path("ref").asText(),
                pull.path("head").path("sha").asText(),
                diff,
                changedFiles
        );
    }

    @Override
    public RepositoryReviewSnapshot inspectRange(RepositoryReviewCoordinates repository, String base, String head) {
        requireAllowed(repository);
        JsonNode baseCommit = sendJson("GET", repoPath(repository) + "/commits/" + path(base), null, 200);
        JsonNode headCommit = sendJson("GET", repoPath(repository) + "/commits/" + path(head), null, 200);
        String baseSha = baseCommit.path("sha").asText();
        String headSha = headCommit.path("sha").asText();
        String comparePath = repoPath(repository) + "/compare/" + path(baseSha) + "..." + path(headSha);
        JsonNode compare = sendJson("GET", comparePath, null, 200);
        String diff = sendText("GET", comparePath, null, DIFF_ACCEPT, 200);
        List<String> files = new ArrayList<>();
        for (JsonNode file : compare.path("files")) files.add(file.path("filename").asText());
        return new RepositoryReviewSnapshot(repository, 0, base, baseSha, head, headSha, diff, files);
    }

    @Override
    public List<RepositoryReviewThread> listReviewThreads(RepositoryReviewCoordinates repository, int pullRequestNumber) {
        requireAllowed(repository);
        List<RepositoryReviewThread> threads = new ArrayList<>();
        String root = repoPath(repository) + "/pulls/" + pullRequestNumber + "/comments";
        for (int page = 1; page <= 30; page++) {
            JsonNode payload = sendJson("GET", root + "?per_page=100&page=" + page, null, 200);
            int count = 0;
            for (JsonNode comment : payload) {
                JsonNode replyNode = comment.path("in_reply_to_id");
                Long replyTo = replyNode.isMissingNode() || replyNode.isNull() ? null : replyNode.asLong();
                int line = comment.path("line").isInt() ? comment.path("line").asInt() : comment.path("original_line").asInt(0);
                threads.add(new RepositoryReviewThread(
                        comment.path("id").asLong(),
                        comment.path("path").asText(""),
                        line,
                        comment.path("body").asText(""),
                        replyTo,
                        comment.path("position").isNull()
                ));
                count++;
            }
            if (count < 100) break;
        }
        return List.copyOf(threads);
    }

    @Override
    public RepositoryPublishedReview publishReview(PublishRepositoryReviewRequest request) {
        requireAllowed(request.repository());
        ObjectNode payload = objectMapper.createObjectNode()
                .put("commit_id", request.exactHeadSha())
                .put("event", event(request.decision()));
        if (!request.summary().isBlank()) payload.put("body", request.summary());
        ArrayNode comments = payload.putArray("comments");
        for (RepositoryReviewComment comment : request.comments()) {
            comments.addObject()
                    .put("path", comment.path())
                    .put("line", comment.line())
                    .put("side", comment.side())
                    .put("body", comment.body());
        }
        JsonNode response = sendJson("POST", repoPath(request.repository()) + "/pulls/" + request.pullRequestNumber() + "/reviews", payload, 200);
        return new RepositoryPublishedReview(
                response.path("id").asLong(), request.pullRequestNumber(), request.exactHeadSha(), request.decision());
    }

    private String event(RepositoryReviewDecision decision) {
        return switch (decision) {
            case APPROVE -> "APPROVE";
            case COMMENT -> "COMMENT";
            case REQUEST_CHANGES -> "REQUEST_CHANGES";
        };
    }

    private JsonNode sendJson(String method, String path, JsonNode body, int expectedStatus) {
        String response = sendText(method, path, body, JSON_ACCEPT, expectedStatus);
        try {
            return objectMapper.readTree(response);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse GitHub response for " + method + " " + path, exception);
        }
    }

    private String sendText(String method, String path, JsonNode body, String accept, int expectedStatus) {
        HttpResponse<String> response = send(method, path, body, accept);
        if (response.statusCode() != expectedStatus) {
            throw new IllegalStateException("GitHub API returned " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private HttpResponse<String> send(String method, String path, JsonNode body, String accept) {
        try {
            URI uri = configuration.apiBase().resolve(path);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", accept)
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Authorization", "Bearer " + configuration.token());
            String payload = body == null ? "" : objectMapper.writeValueAsString(body);
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(payload));
            if (body != null) builder.header("Content-Type", "application/json");
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new IllegalStateException("GitHub request failed: " + method + " " + path, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub request interrupted: " + method + " " + path, exception);
        }
    }

    private String repoPath(RepositoryReviewCoordinates repository) {
        return "/repos/" + path(repository.owner()) + "/" + path(repository.name());
    }

    private void requireAllowed(RepositoryReviewCoordinates repository) {
        if (!configuration.allowedRepositories().contains(repository)) {
            throw new SecurityException("Repository is outside the configured review allowlist: " + repository.fullName());
        }
    }

    private static String path(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
