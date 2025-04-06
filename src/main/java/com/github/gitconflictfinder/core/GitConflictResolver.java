package com.github.gitconflictfinder.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gitconflictfinder.GitConflictFinder;
import com.github.gitconflictfinder.exceptions.GitHubApiException;
import com.github.gitconflictfinder.clients.GitCommandClient;
import com.github.gitconflictfinder.clients.GitHubApiClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Core logic for detecting file conflicts between two Git branches.
 * AUTHOR NOTE: the core logic was intentionally separated from the main {@link GitConflictFinder} class for easier testing! :)
 *
 * How does it work?
 * - gets the merge base commit SHA
 * - runs a command to get all local changes from the branchB since merge base commit,
 * - gets all remote changes via GitHub API since the merge base commit,
 * - compares the local and remote changes and returns the overlapping files as conflicts.
 *
 * Used internally by {@link GitConflictFinder}.
 */
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
        try {
            return new ArrayList<>(Arrays.asList(changedFilesLocal.split("\n")));

        } catch (Exception e) {
            throw new IOException("Could not get changed files from local repository", e);
        }
    }

    private ArrayList<String> getRemoteChangedFiles(String baseMergeCommit) throws GitHubApiException, JsonProcessingException {
        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + baseMergeCommit + "..." + context.getBranchA();
        String changedFilesRemoteJson = githubClient.fetchJsonData(branchComparisonUrl);

        return extractConflictedFiles(changedFilesRemoteJson);
    }

    private ArrayList<String> extractConflictedFiles(String filesJsonData) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
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

        } catch (Exception e ) {
            throw new JsonProcessingException("Error extracting the conflicted files") {};
        }
    }
}
