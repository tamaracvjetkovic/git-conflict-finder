package com.github.gitconflictfinder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GitConflictResolverTest {
    @Mock
    private GitCommandClient gitClient;
    @Mock
    private GitHubApiClient githubClient;
    @Mock
    private GitHubRepoContext context;
    private GitConflictResolver resolver;

    @BeforeEach
    void setUp() {
        when(githubClient.getContext()).thenReturn(context);
        resolver = new GitConflictResolver(gitClient, githubClient);
    }

    @Test
    void findConflicts_ValidInputs_ReturnsConflictedFiles() throws Exception {
        String mockOwnerName = "owner";
        String mockRepoName = "repo";
        String mockLocalRepoPath = "/repo/path";
        String mockBranchA = "main";
        String mockBranchB = "feature";

        when(context.getOwnerName()).thenReturn(mockOwnerName);
        when(context.getRepoName()).thenReturn(mockRepoName);
        when(context.getLocalRepoPath()).thenReturn(mockLocalRepoPath);
        when(context.getBranchA()).thenReturn(mockBranchA);
        when(context.getBranchB()).thenReturn(mockBranchB);

        String mockBaseMergeCommit = "a123456";
        String mergeBaseCommand = "git merge-base " + context.getBranchB() + " " + context.getBranchA();
        when(gitClient.runCommand(mergeBaseCommand, context.getLocalRepoPath())).thenReturn(mockBaseMergeCommit);

        String mockChangedFilesLocal = "file1.txt\nfile2.txt\nfile3.txt";
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

        assertEquals(conflictedFiles.size(), 2);
        assertEquals(List.of("file2.txt", "file3.txt"), conflictedFiles);
    }
}
