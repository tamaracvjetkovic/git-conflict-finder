package com.github.gitconflictfinder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

public class GitConflictFinder {
    private static GitHubRepoContext context;

    public ArrayList<String> findConflicts(String ownerName, String repoName, String accessToken, String localRepoPath, String branchA, String branchB) throws GitHubApiException, JsonProcessingException {
        context = new GitHubRepoContext(ownerName, repoName, accessToken, localRepoPath, branchA, branchB);

        ArrayList<String> changedFilesRemote = getRemoteChangedFiles();
        ArrayList<String> changedFilesLocal = getLocalChangedFiles();

        return null;
    }

    private ArrayList<String> getRemoteChangedFiles() throws GitHubApiException, JsonProcessingException {
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

        GitHubApiClient githubClient = new GitHubApiClient(context);

        // 1) get merge base commit date
        String branchComparisonUrl = context.generateBranchComparisonUrl();
        String comparisonJsonData = githubClient.fetchJsonData(branchComparisonUrl);

        String mergeBaseCommitDate = extractMergeBaseCommitDate(comparisonJsonData);

        // 2) get conflicted files since the merge base commit date
        String commitsSinceUrl = context.generateCommitsSinceUrl(mergeBaseCommitDate);
        String filesJsonData = githubClient.fetchJsonData(commitsSinceUrl);

        return extractConflictedFiles(filesJsonData);
    }

    private String extractMergeBaseCommitDate(String comparisonJsonData) throws JsonProcessingException, GitHubApiException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(comparisonJsonData);

        String mergeBaseCommitDate = rootNode
            .path("merge_base_commit")
            .path("commit")
            .path("author")
            .path("date")
            .asText();

        if (mergeBaseCommitDate == null || !mergeBaseCommitDate.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
            throw new GitHubApiException("Error extracting the merge base commit");
        }

        return mergeBaseCommitDate;
    }

    private ArrayList<String> extractConflictedFiles(String filesJsonData) throws JsonProcessingException, GitHubApiException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(filesJsonData);

        JsonNode filesNode = rootNode.path("files");
        if (!filesNode.isArray()) {
            throw new JsonProcessingException("Error extracting the conflicted files") {};
        }

        ArrayList<String> conflictedFiles = new ArrayList<>();

        for (JsonNode fileNode : filesNode) {
            JsonNode filenameNode = fileNode.path("filename");
            if (filenameNode.isTextual()) {
                conflictedFiles.add(filenameNode.asText());
            }
        }

        return conflictedFiles;
    }

    private ArrayList<String> getLocalChangedFiles() {
        // 2) LOCAL REPO

        // 1. check the Java ProcessBuilder for cmd commands

        return null;
    }

}
