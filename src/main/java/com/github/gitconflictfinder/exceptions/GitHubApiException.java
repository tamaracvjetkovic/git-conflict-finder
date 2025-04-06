package com.github.gitconflictfinder.exceptions;

/**
 * Custom exception thrown when GitHub API calls fail.
 */
public class GitHubApiException extends Exception {
    public GitHubApiException(String message) {
        super(message);
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
