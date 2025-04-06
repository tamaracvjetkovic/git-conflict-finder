package com.github.gitconflictfinder;

import com.github.gitconflictfinder.clients.GitCommandClient;
import com.github.gitconflictfinder.clients.GitHubApiClient;
import com.github.gitconflictfinder.core.GitConflictResolver;
import com.github.gitconflictfinder.core.GitHubRepoContext;
import com.github.gitconflictfinder.exceptions.GitHubApiException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Main entry point for finding file conflicts between two branches.
 *
 * How to use?
 * - call {@code findConflicts()} with provided parameters:
 * 1) owner name (your GitHub username),
 * 2) repo name,
 * 3) access token (if the repo is private, otherwise empty string - "" is okay),
 * 4) local repo path,
 * 5) branchA - remote branch that exists on remotely and locally,
 * 6) branchB - local branch, branched from the branchA.
 *
 * How to get the access token?
 * - In the upper-right corner of any page on GitHub, click your profile photo, then click Settings.
 *   In the left sidebar, click Developer settings. In the left sidebar, under Personal access tokens,
 *   click Tokens (classic). Select Generate new token, then click Generate new token (classic).
 */
public class GitConflictFinder {
    public static ArrayList<String> findConflicts(String ownerName, String repoName, String accessToken, String localRepoPath, String branchA, String branchB) throws GitHubApiException, IOException, InterruptedException {
        GitCommandClient cmdClient = new GitCommandClient();

        GitHubRepoContext context = new GitHubRepoContext(ownerName, repoName, accessToken, localRepoPath, branchA, branchB);
        GitHubApiClient gitHubApiClient = new GitHubApiClient(context);

        return new GitConflictResolver(cmdClient, gitHubApiClient).findConflicts();
    }
}
