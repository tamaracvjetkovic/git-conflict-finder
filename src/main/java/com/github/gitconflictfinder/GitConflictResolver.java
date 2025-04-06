package com.github.gitconflictfinder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class GitConflictResolver {
    private final GitCommandClient cmdClient;
    private final GitHubApiClient githubClient;
    private final GitHubRepoContext context;

    public GitConflictResolver(GitCommandClient cmdClient, GitHubApiClient githubClient) {
        this.cmdClient = cmdClient;
        this.githubClient = githubClient;
        this.context = githubClient.getContext();
    }

    public ArrayList<String> findConflicts() throws IOException, InterruptedException, GitHubApiException {
        String baseMergeCommit = cmdClient.runCommand("git merge-base " + context.getBranchB() + " " + context.getBranchA(), context.getLocalRepoPath());

        ArrayList<String> changedFilesLocal = getLocalChangedFiles(baseMergeCommit);
        ArrayList<String> changedFilesRemote = getRemoteChangedFiles(baseMergeCommit);

        changedFilesLocal.retainAll(new HashSet<>(changedFilesRemote));
        return changedFilesLocal;
    }

    private ArrayList<String> getLocalChangedFiles(String baseMergeCommit) throws IOException, InterruptedException {
        String changedFilesLocal = cmdClient.runCommand("git diff --name-only " + baseMergeCommit, context.getLocalRepoPath());
        return new ArrayList<>(Arrays.asList(changedFilesLocal.split("\n")));
    }

    private ArrayList<String> getRemoteChangedFiles(String baseMergeCommit) throws GitHubApiException, JsonProcessingException {
        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + baseMergeCommit + "..." + context.getBranchA();
        String changedFilesRemoteJson = githubClient.fetchJsonData(branchComparisonUrl);

        return extractConflictedFiles(changedFilesRemoteJson);
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
}
