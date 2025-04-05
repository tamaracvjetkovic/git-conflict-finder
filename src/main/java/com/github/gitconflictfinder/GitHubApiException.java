package com.github.gitconflictfinder;

public class GitHubApiException extends Exception {
    public GitHubApiException(String message) {
        super(message);
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
