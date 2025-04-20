package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GitUtils {

    public static String getMergeBase(String repoPath, String branchA, String branchB) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(repoPath);
        command.add("merge-base");
        command.add(branchA);
        command.add(branchB);

        return runGitCommand(command);
    }

    public static List<FileChange> getModifiedFiles(String repoPath, String oldCommit, String newCommit) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(repoPath);
        command.add("diff");
        command.add("--name-status");
        command.add(oldCommit);
        command.add(newCommit);

        List<FileChange> modifiedFiles = new ArrayList<>();
        String[] lines = runGitCommand(command).split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\t");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid git diff line: " + line);
            }
            modifiedFiles.add(FileChange.fromGit(parts[0], parts[1], parts.length == 3 ? parts[2] : null));
        }

        return modifiedFiles;
    }

    private static String runGitCommand(List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Git command \"" + String.join(" ", command) + "\" failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git command execution interrupted", e);
        }

        return output.toString().trim();
    }
}
