package com.github.gitconflictfinder;

public class GitHubRepoContext {
    private final String ownerName;
    private final String repoName;
    private final String accessToken;
    private final String branchA;
    private final String branchB;
    private final String localRepoPath;

    public GitHubRepoContext(String ownerName, String repoName, String accessToken, String localRepoPath, String branchA, String branchB) {
        this.ownerName = ownerName;
        this.repoName = repoName;
        this.accessToken = accessToken;
        this.branchA = branchA;
        this.localRepoPath = localRepoPath;
        this.branchB = branchB;
    }

    public String getLocalRepoPath() {
        return localRepoPath;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getBranchA() {
        return branchA;
    }

    public String getBranchB() {
        return branchB;
    }

    public String getAuthorizationHeader() {
        return "Bearer " + accessToken;
    }
}
