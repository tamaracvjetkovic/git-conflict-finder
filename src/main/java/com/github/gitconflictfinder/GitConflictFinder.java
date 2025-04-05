package com.github.gitconflictfinder;

import java.util.ArrayList;

public class GitConflictFinder {

    // 1) REMOTE REPO:

    // 1. check if the repo is private od public?
    // --> use accessToken to access the private repo

    // 2.1 find the merge base of branchA and branchB with GitHub API
    // --> GET https://api.github.com/repos/kzi-nastava/iss-project-event-planner-siit-2024-team-11/compare/main...dev

    // 2.2. find the exact time it happened
    // --> merge_base_commit > commit > author > date

    // 3. find all file paths that were changed since the merge date
    // --> GET https://api.github.com/repos/kzi-nastava/iss-project-event-planner-siit-2024-team-11/commits/main?since=2024-11-27T23:14:11Z
    // --> file[i]["filename"]
    // **** CHECK IF THE FILES ARE RIGHT!!!

    public ArrayList<String> findConflicts(String ownerName, String repoName, String accessToken, String localRepoPath, String branchA, String branchB) {
        GitHubRepoContext context = new GitHubRepoContext(ownerName, repoName, accessToken, branchA, branchB);
        GitHubApiClient githubClient = new GitHubApiClient(context);

        // 1) get merge base commit date
        String branchComparisonUrl = context.generateBranchComparisonUrl();
        String comparisonData = githubClient.fetchJsonData(branchComparisonUrl);

        String mergeBaseCommitDate = extractMergeBaseCommitDate(comparisonData);

        // 2) get conflicted files since the merge base commit date
        String commitsSinceUrl = context.generateCommitsSinceUrl(mergeBaseCommitDate);
        String filesJson = githubClient.fetchJsonData(commitsSinceUrl);

        return extractConflictedFiles(filesJson);

        // --------------------------------

        // 2) LOCAL REPO

        // 1. check the Java ProcessBuilder for cmd commands
    }

    private String extractMergeBaseCommitDate(String comparisonData) {
        return null;
    }

    private ArrayList<String> extractConflictedFiles(String filesJson) {
        return null;
    }
}
