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
        String mergeBaseCommit = cmdClient.runCommand("git merge-base " + context.getBranchB() + " " + context.getBranchA(), context.getLocalRepoPath());

        ArrayList<String> changedFilesLocal = getLocalChangedFiles(mergeBaseCommit);
        HashSet<String> changedFilesRemote = getRemoteChangedFiles(mergeBaseCommit);

        changedFilesLocal.retainAll(changedFilesRemote);
        return changedFilesLocal;
    }

    private ArrayList<String> getLocalChangedFiles(String mergeBaseCommit) throws IOException, InterruptedException {
        String changedFilesLocal = cmdClient.runCommand("git diff --name-only " + mergeBaseCommit, context.getLocalRepoPath());
        try {
            return new ArrayList<>(Arrays.asList(changedFilesLocal.split("\n")));

        } catch (Exception e) {
            throw new IOException("Could not get changed files from local repository", e);
        }
    }

    private HashSet<String> getRemoteChangedFiles(String mergeBaseCommit) throws GitHubApiException, JsonProcessingException {
        githubClient.validateAccessToken();

        HashSet<String> remoteChangedFiles = new HashSet<>();

        ArrayList<String> commits = getCommits(mergeBaseCommit);
        for (String sha : commits) {
            updateRemoteChangedFiles(sha, remoteChangedFiles);
        }

        return remoteChangedFiles;
    }

    private ArrayList<String> getCommits(String mergeBaseCommit) throws GitHubApiException, JsonProcessingException {
        String mergeBaseCommitDate = getMergeBaseCommitDate(mergeBaseCommit);
        mergeBaseCommitDate = mergeBaseCommitDate.replace("\"", "");
        // get ONLY commits since the merge base date:
        // https://api.github.com/repos/tamaracvjetkovic/git-test-project/commits?sha=main&since=2024-01-01T00:00:00Z&per_page=250&page=1

        String branchCommitsApiPaged = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/commits?sha=" + context.getBranchA() + "&since=" + mergeBaseCommitDate + "?per_page=250&page=";
        int page = 1;

        ArrayList<String> commits = new ArrayList<>();

        while (true) {
            String commitsRemoteJson = githubClient.fetchJsonData(branchCommitsApiPaged + page);
            int updatedCommitsCnt = updateCommitsPerPage(commitsRemoteJson, commits);

            if (updatedCommitsCnt < 250) {
                break;
            }

            page++;
        }

        return commits;
    }

    private String getMergeBaseCommitDate(String baseMergeCommit) throws GitHubApiException, JsonProcessingException {
        // https://api.github.com/repos/tamaracvjetkovic/git-test-project/commits/785a7c4baddb9f8ade6efc80e07de702fb44aa3d?per_page=1&page=1
        // "commit" -> "author" -> "date" --> 2025-04-05T21:57:36Z
        String mergeBaseCommitDateApi = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/commits/" + baseMergeCommit + "?per_page=1&page=";
        String mergeBaseCommitDetailsJson = githubClient.fetchJsonData(mergeBaseCommitDateApi);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(mergeBaseCommitDetailsJson);
            JsonNode mergeBaseCommitDate = rootNode.path("commit").path("author").path("date");

            return mergeBaseCommitDate.toString();

        } catch (Exception e) {
            throw new JsonProcessingException("Error getting the merge base commit date.") {};
        }
    }

    private int updateCommitsPerPage(String commitsRemoteJson, ArrayList<String> commits) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode filesRootNode = objectMapper.readTree(commitsRemoteJson);

            if (!filesRootNode.isArray()) {
                throw new JsonProcessingException("Error extracting the conflicted files.") {};
            }

            for (JsonNode fileNode : filesRootNode) {
                JsonNode filenameNode = fileNode.path("sha");
                if (filenameNode.isTextual()) {
                    commits.add(filenameNode.asText());
                }
            }

            return filesRootNode.size();

        } catch (Exception e) {
            throw new JsonProcessingException("Error extracting the conflicted files.") {};
        }
    }

    private void updateRemoteChangedFiles(String sha, HashSet<String> remoteChangedFiles) throws GitHubApiException, JsonProcessingException {
        String commitFilesApiPaged = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/commits/" + sha + "?per_page=300&page=";
        int page = 1;

        while (true) {
            String filesRemoteJson = githubClient.fetchJsonData(commitFilesApiPaged + page);

            int updatedFilesCnt = updateRemoteChangedFilesPerPage(filesRemoteJson, remoteChangedFiles);
            if (updatedFilesCnt < 300) {
                break;
            }

            page++;
        }
    }

    private int updateRemoteChangedFilesPerPage(String filesJsonData, HashSet<String> remoteChangedFiles) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(filesJsonData);

            JsonNode filesNode = rootNode.path("files");
            if (!filesNode.isArray()) {
                throw new JsonProcessingException("Error extracting the conflicted files.") {};
            }

            for (JsonNode fileNode : filesNode) {
                JsonNode filenameNode = fileNode.path("filename");
                if (filenameNode.isTextual()) {
                    remoteChangedFiles.add(filenameNode.asText());
                }
            }

            return filesNode.size();

        } catch (Exception e) {
            throw new JsonProcessingException("Error extracting the conflicted files.") {};
        }
    }
}
