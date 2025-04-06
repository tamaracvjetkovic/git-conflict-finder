package com.github.gitconflictfinder;

import java.io.IOException;
import java.util.ArrayList;

public class GitConflictFinder {
    public static ArrayList<String> findConflicts(String ownerName, String repoName, String accessToken, String localRepoPath, String branchA, String branchB) throws GitHubApiException, IOException, InterruptedException {
        GitCommandClient cmdClient = new GitCommandClient();

        GitHubRepoContext context = new GitHubRepoContext(ownerName, repoName, accessToken, localRepoPath, branchA, branchB);
        GitHubApiClient gitHubApiClient = new GitHubApiClient(context);

        return new GitConflictResolver(cmdClient, gitHubApiClient).findConflicts();
    }
}
