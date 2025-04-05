package com.github.gitconflictfinder;

public class GitHubRepoContext {
    private String accessToken;
    private String branchA;
    private String branchB;
    private String repoUrl;

    public GitHubRepoContext(String ownerName, String repoName, String accessToken, String branchA, String branchB) {
        this.accessToken = accessToken;
        this.branchA = branchA;
        this.branchB = branchB;
        this.repoUrl = "https://api.github.com/repos/" + ownerName + "/" + repoName;
    }

    public String generateBranchComparisonUrl() {
        // https://api.github.com/repos/kzi-nastava/iss-project-event-planner-siit-2024-team-11/compare/main...dev
        return repoUrl + "/compare/" + branchA + "..." + branchB;
    }

    public String generateCommitsSinceUrl(String mergeBaseCommitDate) {
        // https://api.github.com/repos/kzi-nastava/iss-project-event-planner-siit-2024-team-11/commits/main?since=2024-11-27T23:14:11Z
        return repoUrl + "/commits/" + branchA + "?since=" + mergeBaseCommitDate;
    }

    public String getAuthorizationHeader() {
        return "Bearer " + accessToken;
    }
}
