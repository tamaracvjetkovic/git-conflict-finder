package com.github.gitconflictfinder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class GitConflictFinder {
    private static GitHubRepoContext context;
    private static String baseMergeCommit;

    public ArrayList<String> findConflicts(String ownerName, String repoName, String accessToken, String localRepoPath, String branchA, String branchB) throws GitHubApiException, IOException, InterruptedException {
        context = new GitHubRepoContext(ownerName, repoName, accessToken, localRepoPath, branchA);

        baseMergeCommit = GitCommandClient.runCommand("git merge-base " + branchB + " " + branchA, localRepoPath);

        ArrayList<String> changedFilesLocal = getLocalChangedFiles();
        ArrayList<String> changedFilesRemote = getRemoteChangedFiles();

        changedFilesLocal.retainAll(new HashSet<String>(changedFilesRemote));
        return changedFilesLocal;
    }

    private ArrayList<String> getLocalChangedFiles() throws IOException, InterruptedException {
        String changedFileNamesLocal = GitCommandClient.runCommand("git diff --name-only " + baseMergeCommit, context.getLocalRepoPath());
        return new ArrayList<>(Arrays.asList(changedFileNamesLocal.split("\n")));
    }

    private ArrayList<String> getRemoteChangedFiles() throws GitHubApiException, JsonProcessingException {
        GitHubApiClient githubClient = new GitHubApiClient(context);

        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + baseMergeCommit + "..." + context.getBranchA();
        String comparisonJsonData = githubClient.fetchJsonData(branchComparisonUrl);

        return extractConflictedFiles(comparisonJsonData);
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
