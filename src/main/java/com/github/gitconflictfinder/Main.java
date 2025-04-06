package com.github.gitconflictfinder;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        GitConflictFinder finder = new GitConflictFinder();
        String ownerName = "tamaracvjetkovic";
        String repoName = "git-test-project";
        String accessToken = "";

        String localRepoPath = "C:\\Users\\cvlad\\Desktop\\FTN SIIT\\Random\\JetBrains, 2025\\1. Improvements of managing infrastructure code in TeamCity\\TestProject1\\git-test-project";
        //String localRepoPath = "C:\\Users\\cvlad\\Desktop\\FTN SIIT\\Random\\JetBrains, 2025\\1. Improvements of managing infrastructure code in TeamCity\\TestProject2\\git-test-project";
        //String localRepoPath = "C:\\Users\\cvlad\\Desktop\\FTN SIIT\\Random\\JetBrains, 2025\\1. Improvements of managing infrastructure code in TeamCity\\TestProject3\\git-test-project";

        String branchA = "main";
        String branchB = "dev";

        try {
            ArrayList<String> conflictedFiles = GitConflictFinder.findConflicts(ownerName, repoName, accessToken, localRepoPath, branchA, branchB);
            if (!conflictedFiles.isEmpty()) {
                for (String file : conflictedFiles) {
                    System.out.println(file);
                }
            } else {
                System.out.println("No conflicts found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}