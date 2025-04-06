package com.github.gitconflictfinder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GitHubApiClient {
    private HttpClient client;
    private GitHubRepoContext context;
    
    public GitHubApiClient(GitHubRepoContext gitHubRepoContext) {
        this.client = HttpClient.newHttpClient();;
        this.context = gitHubRepoContext;
    }

    public GitHubRepoContext getContext() {
        return context;
    }

    public String fetchJsonData(String url) throws GitHubApiException {
        try {
            HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );

            return switch (response.statusCode()) {
                case 200 -> response.body();
                case 401, 403, 404 -> fetchWithAccessToken(url);
                default -> throw new GitHubApiException("Unexpected status code: " + response.statusCode());
            };

        } catch (IOException | InterruptedException | GitHubApiException e) {
            throw new GitHubApiException(e.getMessage());
        }
    }

    private String fetchWithAccessToken(String url) throws GitHubApiException {
        try {
            HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", context.getAuthorizationHeader()).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                return response.body();
            }

            throw new GitHubApiException("Unexpected status code: " + response.statusCode());

        } catch (IOException | InterruptedException e) {
            throw new GitHubApiException("Error fetching with access token", e);
        }
    }
}
