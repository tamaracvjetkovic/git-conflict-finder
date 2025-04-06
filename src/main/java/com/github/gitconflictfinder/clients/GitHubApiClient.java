package com.github.gitconflictfinder.clients;

import com.github.gitconflictfinder.core.GitConflictResolver;
import com.github.gitconflictfinder.exceptions.GitHubApiException;
import com.github.gitconflictfinder.core.GitHubRepoContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Client responsible for managing GitHub API calls to fetch remote file data.
 * It handles authentication and checking the status codes.
 *
 * How does it work?
 * - tries accessing the repo file data,
 * - if the repo is private, tries accessing again, but with access token,
 * - returns the JSON response if the access is successful
 * - throws {@link GitHubApiException} if there is an error.
 *
 * Used by {@link GitConflictResolver} to compare remote file changes.
 */
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

            throw new GitHubApiException("Unexpected status code: " + response.statusCode() + ".\n\nPlease check if any of these may be the cause of the error:\n1) the repository does not exist,\n2) the repository is private, while no access token was provided,\n3) invalid access token was provided.\n");

        } catch (IOException | InterruptedException e) {
            throw new GitHubApiException("Error fetching with access token", e);
        }
    }
}
