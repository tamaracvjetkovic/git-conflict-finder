package com.github.gitconflictfinder;

import com.github.gitconflictfinder.clients.GitCommandClient;
import com.github.gitconflictfinder.clients.GitHubApiClient;
import com.github.gitconflictfinder.core.GitConflictResolver;
import com.github.gitconflictfinder.core.GitHubRepoContext;
import com.github.gitconflictfinder.exceptions.GitHubApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GitConflictResolver}.
 *
 * These tests cover various situations the resolver might face, including:
 * - Conflict scenarios between local and remote files
 * - No-conflict cases where only one side has changes
 * - Proper handling of exceptions (API failures, missing repos)
 * - Happy path cases where everything works :)
 *
 * Mocked dependencies: {@link GitCommandClient} and {@link GitHubApiClient}.
 * The goal is to simulate realistic conflict situations and ensure correct behavior.
 */
@ExtendWith(MockitoExtension.class)
public class GitConflictResolverTest {
    @Mock
    private GitCommandClient gitClient;
    @Mock
    private GitHubApiClient githubClient;

    private GitHubRepoContext context;
    private String mockBaseMergeCommit = "a123456";

    private GitConflictResolver resolver;

    @BeforeEach
    void setUp() {
        context = new GitHubRepoContext("ownerName", "repoName", "", "/local/repo/path", "main", "dev");
        when(githubClient.getContext()).thenReturn(context);
        resolver = new GitConflictResolver(gitClient, githubClient);
    }

    @Test
    void findConflicts_ConflictsExist_ReturnsConflictedFiles_Example1() throws Exception {
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenReturn(mockBaseMergeCommit);

        String mockChangedFilesLocal = "src/java/model/Event.java\nsrc/java/services/EventService.java\nsrc/java/Test.java";
        String gitDiffCommand = "git diff --name-only " + mockBaseMergeCommit;
        when(gitClient.runCommand(gitDiffCommand, context.getLocalRepoPath())).thenReturn(mockChangedFilesLocal);

        String mockChangedFilesRemoteJson = """
            {
              "files": [
                { "filename": "src/java/model/event/Event.java" },
                { "filename": "src/java/services/EventService.java" },
                { "filename": "src/Test.java" }
              ]
            }
        """;
        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + mockBaseMergeCommit + "..." + context.getBranchA();
        when(githubClient.fetchJsonData(branchComparisonUrl)).thenReturn(mockChangedFilesRemoteJson);

        ArrayList<String> conflictedFiles = resolver.findConflicts();

        assertEquals(conflictedFiles.size(), 1);
        assertEquals(List.of("src/java/services/EventService.java"), conflictedFiles);
    }

    @Test
    void findConflicts_ConflictsExist_ReturnsConflictedFiles_Example2() throws Exception {
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenReturn(mockBaseMergeCommit);

        String mockChangedFilesLocal = "file1.txt\nfiles/file1.txt";
        String gitDiffCommand = "git diff --name-only " + mockBaseMergeCommit;
        when(gitClient.runCommand(gitDiffCommand, context.getLocalRepoPath())).thenReturn(mockChangedFilesLocal);

        String mockChangedFilesRemoteJson = """
            {
              "files": [
                { "filename": "file2.txt" },
                { "filename": "files/file1.txt" }
              ]
            }
        """;
        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + mockBaseMergeCommit + "..." + context.getBranchA();
        when(githubClient.fetchJsonData(branchComparisonUrl)).thenReturn(mockChangedFilesRemoteJson);

        ArrayList<String> conflictedFiles = resolver.findConflicts();

        assertEquals(conflictedFiles.size(), 1);
        assertEquals(List.of("files/file1.txt"), conflictedFiles);
    }

    @Test
    void findConflicts_OnlyRemoteFilesChanged_ReturnsEmptyList() throws Exception {
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenReturn(mockBaseMergeCommit);

        String mockChangedFilesLocal = "";
        String gitDiffCommand = "git diff --name-only " + mockBaseMergeCommit;
        when(gitClient.runCommand(gitDiffCommand, context.getLocalRepoPath())).thenReturn(mockChangedFilesLocal);

        String mockChangedFilesRemoteJson = """
            {
              "files": [
                { "filename": "file2.txt" },
                { "filename": "file3.txt" },
                { "filename": "file4.txt" }
              ]
            }
        """;
        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + mockBaseMergeCommit + "..." + context.getBranchA();
        when(githubClient.fetchJsonData(branchComparisonUrl)).thenReturn(mockChangedFilesRemoteJson);

        ArrayList<String> conflictedFiles = resolver.findConflicts();

        assertTrue(conflictedFiles.isEmpty());
    }

    @Test
    void findConflicts_OnlyLocalFilesChanged_ReturnsEmptyList() throws Exception {
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenReturn(mockBaseMergeCommit);

        String mockChangedFilesLocal = "file1.txt\nfile2.txt\nfile3.txt";
        String gitDiffCommand = "git diff --name-only " + mockBaseMergeCommit;
        when(gitClient.runCommand(gitDiffCommand, context.getLocalRepoPath())).thenReturn(mockChangedFilesLocal);

        String mockChangedFilesRemoteJson = """
            {
              "files": []
            }
        """;
        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + mockBaseMergeCommit + "..." + context.getBranchA();
        when(githubClient.fetchJsonData(branchComparisonUrl)).thenReturn(mockChangedFilesRemoteJson);

        ArrayList<String> conflictedFiles = resolver.findConflicts();

        assertTrue(conflictedFiles.isEmpty());
    }

    @Test
    void findConflicts_ChangesExistButNoConflicts_ReturnsEmptyList_Example1() throws Exception {
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenReturn(mockBaseMergeCommit);

        String mockChangedFilesLocal = "file1.txt\nfile2.txt";
        String gitDiffCommand = "git diff --name-only " + mockBaseMergeCommit;
        when(gitClient.runCommand(gitDiffCommand, context.getLocalRepoPath())).thenReturn(mockChangedFilesLocal);

        String mockChangedFilesRemoteJson = """
            {
              "files": [
                { "filename": "file3.txt" },
                { "filename": "file4.txt" }
              ]
            }
        """;
        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + mockBaseMergeCommit + "..." + context.getBranchA();
        when(githubClient.fetchJsonData(branchComparisonUrl)).thenReturn(mockChangedFilesRemoteJson);

        ArrayList<String> conflictedFiles = resolver.findConflicts();

        assertTrue(conflictedFiles.isEmpty());
    }

    @Test
    void findConflicts_ChangesExistButNoConflicts_ReturnsEmptyList_Example2() throws Exception {
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenReturn(mockBaseMergeCommit);

        String mockChangedFilesLocal = "file1.txt\nfiles/file.txt";
        String gitDiffCommand = "git diff --name-only " + mockBaseMergeCommit;
        when(gitClient.runCommand(gitDiffCommand, context.getLocalRepoPath())).thenReturn(mockChangedFilesLocal);

        String mockChangedFilesRemoteJson = """
            {
              "files": [
                { "filename": "file.txt" },
                { "filename": "files/file1.txt" }
              ]
            }
        """;
        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + mockBaseMergeCommit + "..." + context.getBranchA();
        when(githubClient.fetchJsonData(branchComparisonUrl)).thenReturn(mockChangedFilesRemoteJson);

        ArrayList<String> conflictedFiles = resolver.findConflicts();

        assertTrue(conflictedFiles.isEmpty());
    }

    @Test
    void findConflicts_LocalRepoNotGitRepo_RaisesIOException() throws Exception {
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenThrow(new IOException("The local repository is not a git repo"));

        IOException exception = assertThrows(IOException.class, () -> {
            resolver.findConflicts();
        });

        assertEquals("The local repository is not a git repo", exception.getMessage());
    }

    @Test
    void findConflicts_LocalRepoParameterNull_RaisesNullPointerException() throws Exception {
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenThrow(new NullPointerException("localRepoPath is null"));

        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            resolver.findConflicts();
        });

        assertEquals("localRepoPath is null", exception.getMessage());
    }

    @Test
    void findConflicts_fetchJsonError_RaisesGitHubApiException() throws Exception {
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenReturn(mockBaseMergeCommit);

        String mockChangedFilesLocal = "";
        String gitDiffCommand = "git diff --name-only " + mockBaseMergeCommit;
        when(gitClient.runCommand(gitDiffCommand, context.getLocalRepoPath())).thenReturn(mockChangedFilesLocal);

        String branchComparisonUrl = "https://api.github.com/repos/" + context.getOwnerName() + "/" + context.getRepoName() + "/compare/" + mockBaseMergeCommit + "..." + context.getBranchA();
        when(githubClient.fetchJsonData(branchComparisonUrl)).thenThrow(GitHubApiException.class);

        assertThrows(GitHubApiException.class, () -> resolver.findConflicts());
    }
}
