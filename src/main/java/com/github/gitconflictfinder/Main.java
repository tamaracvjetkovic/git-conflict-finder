package com.github.gitconflictfinder;

public class Main {
    public static void main(String[] args) {
        GitConflictFinder finder = new GitConflictFinder();
        String ownerName = "tamaracvjetkovic";
        String repoName = "git-test-project";
        String accessToken = "";
        String localRepoPath = "C:\\Users\\cvlad\\Desktop\\FTN SIIT\\Random\\JetBrains, 2025\\1. Improvements of managing infrastructure code in TeamCity\\TestProject\\git-test-project";
        String branchA = "main";
        String branchB = "dev";

        try {
            finder.findConflicts(ownerName, repoName, accessToken, localRepoPath, branchA, branchB);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}