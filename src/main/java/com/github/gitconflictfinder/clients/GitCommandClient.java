package com.github.gitconflictfinder.clients;

import com.github.gitconflictfinder.core.GitConflictResolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Client responsible for executing Git commands on the local file system.
 *
 * How does it work?
 * - runs the provided shell command in the given repo directory,
 * - returns the output if the command successful
 * - throws exception (IOException, NullPointerException) if there is an error.
 *
 * Used by {@link GitConflictResolver} to compare local file changes.
 */
public class GitCommandClient {
    public GitCommandClient() {}

    public String runCommand(String command, String localRepoPath) throws IOException, InterruptedException {
        if (localRepoPath == null || localRepoPath.isEmpty()) {
            throw new NullPointerException("localRepoPath is null");
        }

        File repoDirectory = new File(localRepoPath).getAbsoluteFile();

        if (!isGitRepo(repoDirectory)) {
            throw new IOException("The local repository is not a git repo");
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

    public boolean isGitRepo(File repoDirectory) {
        if (repoDirectory == null || !repoDirectory.exists()) {
            return false;
        }

        File gitDir = new File(repoDirectory, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }
}
