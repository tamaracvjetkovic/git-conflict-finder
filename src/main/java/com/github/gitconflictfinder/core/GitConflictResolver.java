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
    /*
        // 1. retrieve all commit SHAs with compare API:
        https://api.github.com/repos/tamaracvjetkovic/git-test-project/compare/c9c15bd5a98f3f42c26a93284106910542ff5b5b...main
        - number of total commits for following limitations:
            1) without access token: 60 requests/hour
            --> max. 20 commits, if we assume we will fetch max 2 pages for files (1 - 600 max files)
            2) with access token: 5000 requests/hour
            --> max. 999 commits, assuming: max 4 pages of files (1 - 1200 files) (4995 + (4 - 1) requests for 2nd, 3rd and 4th page of commits)
            --> max. 555 commits, assuming: max 8 pages of files (1 - 2400 files) (4995 + (2 - 1) request for 2nd page of commits)

        // 2. go through every commit, and retrieve the files with commit API:
        https://api.github.com/repos/tamaracvjetkovic/git-test-project/commits/203dd5af8971ee91198b8f5bef8feedc93de9088?page=1
        - check total (number of files / 300) and see if it fits the max number of pages above per case - nope??? doesn't exist
      */

    public ArrayList<String> findConflicts() throws IOException, InterruptedException, GitHubApiException {
        String baseMergeCommit = cmdClient.runCommand("git merge-base " + context.getBranchB() + " " + context.getBranchA(), context.getLocalRepoPath());

        ArrayList<String> changedFilesLocal = getLocalChangedFiles(baseMergeCommit);
        HashSet<String> changedFilesRemote = getRemoteChangedFiles(baseMergeCommit);

        changedFilesLocal.retainAll(changedFilesRemote);
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

    private HashSet<String> getRemoteChangedFiles(String baseMergeCommit) throws GitHubApiException, JsonProcessingException {
        githubClient.validateAccessToken();

        HashSet<String> remoteChangedFiles = new HashSet<>();

        ArrayList<String> commits = getCommits(baseMergeCommit);
        for (String sha : commits) {
            updateRemoteChangedFiles(sha, remoteChangedFiles);
        }

        return remoteChangedFiles;
    }

    private ArrayList<String> getCommits(String baseMergeCommit) throws GitHubApiException, JsonProcessingException {
        String branchComparisonApiPaged = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + baseMergeCommit + "..." + context.getBranchA() + "?per_page=";
        String commitsRemoteJson = githubClient.fetchJsonData(branchComparisonApiPaged + "1");

        // NOTE: I gotta find per page 250, then the next page.

        int totalCommits = getTotalCommits(commitsRemoteJson);
        int totalPages = (totalCommits + 249) / 250;

        ArrayList<String> commits = new ArrayList<>(totalCommits);
        updateCommitsPerPage(commitsRemoteJson, commits);

        for (int i = 1; i < totalPages; i++) {
            commitsRemoteJson = githubClient.fetchJsonData(branchComparisonApiPaged + (i + 1));
            updateCommitsPerPage(commitsRemoteJson, commits);
        }

        return commits;
    }

    private int getTotalCommits(String commitsRemoteJson) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(commitsRemoteJson);
            JsonNode totalCommits = rootNode.path("total_commits");

            return totalCommits.asInt();

        } catch (Exception e) {
            throw new JsonProcessingException("Error getting the total commits.") {};
        }
    }

    private void updateCommitsPerPage(String commitsRemoteJson, ArrayList<String> commits) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(commitsRemoteJson);

            JsonNode filesNode = rootNode.path("commits");
            if (!filesNode.isArray()) {
                throw new JsonProcessingException("Error extracting the conflicted files.") {};
            }

            for (JsonNode fileNode : filesNode) {
                JsonNode filenameNode = fileNode.path("sha");
                if (filenameNode.isTextual()) {
                    commits.add(filenameNode.asText());
                }
            }

        } catch (Exception e) {
            throw new JsonProcessingException("Error extracting the conflicted files.") {};
        }
    }

    private void updateRemoteChangedFiles(String sha, HashSet<String> remoteChangedFiles) throws GitHubApiException, JsonProcessingException {
        String commitFilesApiPaged = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/commits/" + sha + "?page=";
        int page = 1;

        while (true) {
            String filesRemoteJson = githubClient.fetchJsonData(commitFilesApiPaged + page);

            Boolean updated = updateRemoteChangedFilesPerPage(filesRemoteJson, remoteChangedFiles);
            if (!updated) {
                break;
            }

            page++;
        }
    }

    private Boolean updateRemoteChangedFilesPerPage(String filesJsonData, HashSet<String> remoteChangedFiles) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(filesJsonData);

            JsonNode filesNode = rootNode.path("files");
            if (!filesNode.isArray()) {
                throw new JsonProcessingException("Error extracting the conflicted files.") {};
            }

            if (filesNode.isEmpty()) {
                return false;
            }

            for (JsonNode fileNode : filesNode) {
                JsonNode filenameNode = fileNode.path("filename");
                if (filenameNode.isTextual()) {
                    remoteChangedFiles.add(filenameNode.asText());
                }
            }

            return true;

        } catch (Exception e) {
            throw new JsonProcessingException("Error extracting the conflicted files.") {};
        }
    }
}
