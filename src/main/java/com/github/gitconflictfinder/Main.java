package com.github.gitconflictfinder;

public class Main {
    public static void main(String[] args) {
        GitConflictFinder finder = new GitConflictFinder();
        String ownerName = "kzi-nastava";
        String repoName = "iss-project-event-planner-siit-2024-team-11";
        String accessToken = "";
        String localRepoPath = "";
        String branchA = "main";
        String branchB = "dev";

        try {
            finder.findConflicts(ownerName, repoName, accessToken, localRepoPath, branchA, branchB);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}