package org.tavall.ai.staging.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.staging.RepositoryCheck;
import org.tavall.ai.staging.RepositoryCheckStatus;
import org.tavall.ai.staging.RepositoryCoordinates;
import org.tavall.ai.staging.RepositoryPullRequest;
import org.tavall.ai.staging.RepositoryStagingProvider;

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
import java.util.Optional;

/** GitHub REST provider for repository staging. Repository scope is explicit and fail-closed. */
public final class GitHubRepositoryStagingProvider implements RepositoryStagingProvider {
    private final GitHubRepositoryStagingConfiguration configuration;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GitHubRepositoryStagingProvider(GitHubRepositoryStagingConfiguration configuration) {
        this(configuration, new ObjectMapper().findAndRegisterModules(), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)).build());
    }

    GitHubRepositoryStagingProvider(
            GitHubRepositoryStagingConfiguration configuration,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public static GitHubRepositoryStagingProvider fromEnvironment() {
        return new GitHubRepositoryStagingProvider(GitHubRepositoryStagingConfiguration.fromEnvironment());
    }

    @Override
    public List<RepositoryPullRequest> listOpenPullRequests(RepositoryCoordinates repository) {
        requireAllowed(repository);
        List<RepositoryPullRequest> pulls = new ArrayList<>();
        for (int page = 1; page <= 10; page++) {
            JsonNode payload = sendJson("GET", repoPath(repository) + "/pulls?state=open&per_page=100&page=" + page, null, 200);
            if (!payload.isArray()) throw new IllegalStateException("GitHub pulls response must be an array");
            int count = 0;
            for (JsonNode node : payload) {
                pulls.add(toPullRequest(node));
                count++;
            }
            if (count < 100) break;
        }
        return List.copyOf(pulls);
    }

    @Override
    public Optional<String> branchHead(RepositoryCoordinates repository, String branch) {
        requireAllowed(repository);
        HttpResponse<String> response = send("GET", repoPath(repository) + "/git/ref/heads/" + path(branch), null);
        if (response.statusCode() == 404) return Optional.empty();
        requireStatus(response, 200);
        try {
            return Optional.of(objectMapper.readTree(response.body()).path("object").path("sha").asText());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse GitHub branch response", exception);
        }
    }

    @Override
    public void createBranch(RepositoryCoordinates repository, String branch, String sha) {
        requireAllowed(repository);
        ObjectNode body = objectMapper.createObjectNode().put("ref", "refs/heads/" + branch).put("sha", sha);
        sendJson("POST", repoPath(repository) + "/git/refs", body, 201);
    }

    @Override
    public RepositoryPullRequest createPullRequest(
            RepositoryCoordinates repository,
            String title,
            String headBranch,
            String baseBranch,
            String body,
            boolean draft
    ) {
        requireAllowed(repository);
        ObjectNode payload = objectMapper.createObjectNode()
                .put("title", title).put("head", headBranch).put("base", baseBranch).put("body", body).put("draft", draft);
        return toPullRequest(sendJson("POST", repoPath(repository) + "/pulls", payload, 201));
    }

    @Override
    public RepositoryPullRequest updatePullRequest(
            RepositoryCoordinates repository,
            int pullRequestNumber,
            Optional<String> baseBranch,
            Optional<String> body
    ) {
        requireAllowed(repository);
        ObjectNode payload = objectMapper.createObjectNode();
        baseBranch.ifPresent(value -> payload.put("base", value));
        body.ifPresent(value -> payload.put("body", value));
        return toPullRequest(sendJson("PATCH", repoPath(repository) + "/pulls/" + pullRequestNumber, payload, 200));
    }

    @Override
    public List<RepositoryCheck> checksForHead(RepositoryCoordinates repository, String headSha) {
        requireAllowed(repository);
        JsonNode payload = sendJson("GET", repoPath(repository) + "/commits/" + path(headSha) + "/check-runs?per_page=100", null, 200);
        List<RepositoryCheck> checks = new ArrayList<>();
        for (JsonNode node : payload.path("check_runs")) {
            checks.add(new RepositoryCheck(
                    node.path("name").asText(),
                    parseStatus(node.path("status").asText()),
                    node.path("conclusion").isNull() ? "" : node.path("conclusion").asText("")
            ));
        }
        return List.copyOf(checks);
    }

    private RepositoryPullRequest toPullRequest(JsonNode node) {
        return new RepositoryPullRequest(
                node.path("number").asInt(),
                node.path("title").asText(),
                node.path("body").isNull() ? "" : node.path("body").asText(""),
                node.path("head").path("ref").asText(),
                node.path("head").path("sha").asText(),
                node.path("base").path("ref").asText(),
                node.path("base").path("sha").asText(""),
                node.path("draft").asBoolean(false)
        );
    }

    private RepositoryCheckStatus parseStatus(String value) {
        return switch (value == null ? "" : value.trim().toLowerCase()) {
            case "completed" -> RepositoryCheckStatus.COMPLETED;
            case "in_progress" -> RepositoryCheckStatus.IN_PROGRESS;
            default -> RepositoryCheckStatus.QUEUED;
        };
    }

    private JsonNode sendJson(String method, String path, JsonNode body, int expectedStatus) {
        HttpResponse<String> response = send(method, path, body);
        requireStatus(response, expectedStatus);
        try {
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse GitHub response for " + method + " " + path, exception);
        }
    }

    private HttpResponse<String> send(String method, String path, JsonNode body) {
        try {
            URI uri = configuration.apiBase().resolve(path);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Authorization", "Bearer " + configuration.token());
            String payload = body == null ? "" : objectMapper.writeValueAsString(body);
            builder.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(payload));
            if (body != null) builder.header("Content-Type", "application/json");
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new IllegalStateException("GitHub request failed: " + method + " " + path, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub request interrupted: " + method + " " + path, exception);
        }
    }

    private void requireStatus(HttpResponse<String> response, int expected) {
        if (response.statusCode() != expected) {
            throw new IllegalStateException("GitHub API returned " + response.statusCode() + ": " + response.body());
        }
    }

    private String repoPath(RepositoryCoordinates repository) {
        return "/repos/" + path(repository.owner()) + "/" + path(repository.name());
    }

    private void requireAllowed(RepositoryCoordinates repository) {
        if (!configuration.allowedRepositories().contains(repository)) {
            throw new SecurityException("Repository is outside the configured staging allowlist: " + repository.fullName());
        }
    }

    private static String path(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
