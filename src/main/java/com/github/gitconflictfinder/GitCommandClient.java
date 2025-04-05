package com.github.gitconflictfinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class GitCommandClient {
    public static String runCommand(String command, String localRepoPath) throws IOException, InterruptedException {
        File repoDirectory = new File(localRepoPath).getAbsoluteFile();

        if (!isGitRepo(repoDirectory)) {
            throw new IOException();
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        List<String> shellCommand = isWindows ? Arrays.asList("cmd.exe", "/c", command) : Arrays.asList("bash", "-c", command);

        ProcessBuilder builder = new ProcessBuilder(shellCommand);
        builder.directory(repoDirectory);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException();
        }

        return output.toString().trim();
    }

    public static boolean isGitRepo(File repoDirectory) {
        if (repoDirectory == null || !repoDirectory.exists()) {
            return false;
        }

        File gitDir = new File(repoDirectory, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }
}
