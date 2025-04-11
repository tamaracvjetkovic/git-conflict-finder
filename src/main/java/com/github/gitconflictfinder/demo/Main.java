package com.github.gitconflictfinder.demo;

import com.github.gitconflictfinder.GitConflictFinder;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        String ownerName = "tamaracvjetkovic";
        String repoName = "git-test-project";
        String accessToken = "";

        String localRepoPath = "C:\\Users\\my_user\\Desktop\\FTN SIIT\\Random\\JetBrains, 2025\\1. Improvements of managing infrastructure code in TeamCity\\TestProject1\\git-test-project";

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