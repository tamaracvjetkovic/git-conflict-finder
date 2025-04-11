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
    private final HttpClient client;
    private final GitHubRepoContext context;

    public static Boolean isAccessTokenValid = null;
    public static int requestCnt = 0;

    public GitHubApiClient(GitHubRepoContext gitHubRepoContext) {
        this.client = HttpClient.newHttpClient();
        this.context = gitHubRepoContext;
    }

    public GitHubRepoContext getContext() {
        return context;
    }

    public String fetchJsonData(String api) throws GitHubApiException {
        if (isAccessTokenValid) {
            return fetchWithAccessToken(api);
        }

        return fetchWithoutAccessToken(api);
    }

    private String fetchWithoutAccessToken(String api) throws GitHubApiException {
        if (requestCnt > 60) {
            throw new GitHubApiException("Rate limit exceeded. The maximum number of requests without a valid access token is 60.");
        }

        try {
            HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(api)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );

            requestCnt++;

            if (response.statusCode() == 200) {
                return response.body();
            }

            throw new GitHubApiException("Unexpected status code: " + response.statusCode() + ".\n\nPlease check if any of these may be the cause of the error:\n1) the repository does not exist,\n2) the repository is private, while no access token was provided,\n3) invalid access token was provided.\n");

        } catch (IOException | InterruptedException e) {
            throw new GitHubApiException("Error fetching with access token.", e);
        }
    }

    private String fetchWithAccessToken(String api) throws GitHubApiException {
        if (requestCnt > 5000) {
            throw new GitHubApiException("Rate limit exceeded. The maximum number of requests with a valid access token is 5000.");
        }

        try {
            HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(api)).header("Authorization", context.getAuthorizationHeader()).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );

            requestCnt++;

            if (response.statusCode() == 200) {
                isAccessTokenValid = true;
                return response.body();
            }

            throw new GitHubApiException("Unexpected status code: " + response.statusCode() + ".\n\nPlease check if any of these may be the cause of the error:\n1) the repository does not exist,\n2) the repository is private, while no access token was provided,\n3) invalid access token was provided.\n");

        } catch (IOException | InterruptedException e) {
            throw new GitHubApiException("Error fetching with access token.", e);
        }
    }

    public Boolean validateAccessToken() throws GitHubApiException {
        if (isAccessTokenValid != null) {
            return isAccessTokenValid;
        }

        if (context.getAuthorizationHeader() == null) {
            isAccessTokenValid = false;
            return isAccessTokenValid;
        }

            String testApi = "https://api.github.com/user";
        try {
            HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(testApi)).header("Authorization", context.getAuthorizationHeader()).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            );

            requestCnt++;
            isAccessTokenValid = (response.statusCode() == 200);
            return isAccessTokenValid;

        } catch (IOException | InterruptedException e) {
            throw new GitHubApiException("Failed to validate the access token", e);
        }
    }
}
