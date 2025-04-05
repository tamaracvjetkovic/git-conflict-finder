package com.github.gitconflictfinder;

public class GitHubRepoContext {
    private String ownerName;
    private String repoName;
    private String accessToken;
    private String branchA;
    private String localRepoPath;

    public GitHubRepoContext(String ownerName, String repoName, String accessToken, String localRepoPath, String branchA) {
        this.ownerName = ownerName;
        this.repoName = repoName;
        this.accessToken = accessToken;
        this.branchA = branchA;
        this.localRepoPath = localRepoPath;
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

    public String getAuthorizationHeader() {
        return "Bearer " + accessToken;
    }
}
