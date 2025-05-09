package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class GitUtils {

    static String getMergeBase(String repoPath, String branchA, String branchB) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(repoPath);
        command.add("merge-base");
        command.add(branchA);
        command.add(branchB);

        return runGitCommand(command);
    }

    static List<FileChange> getModifiedFiles(String repoPath, String oldCommit, String newCommit) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(repoPath);
        command.add("diff");
        command.add("--name-status");
        command.add(oldCommit);
        command.add(newCommit);

        String[] lines = runGitCommand(command).split("\n");

        List<FileChange> modifiedFiles = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split("\t");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid git diff line: " + line);
            }
            FileStatus status = FileStatus.fromGit(parts[0]);
            if (parts.length == 2) {
                modifiedFiles.add(new FileChange(status, parts[1], null));
            } else {
                modifiedFiles.add(new FileChange(status, parts[2], parts[1]));
            }
        }

        return modifiedFiles;
    }

    static String runGitCommand(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                output.append(line).append("\n");
            }

            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new IOException("Git failed with exit code " + exitCode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Git command execution interrupted", e);
            }

            return output.toString().trim();

        } catch (IOException e) {
            throw new RuntimeException("Failed to run git command: " + String.join(" ", command), e);
        }
    }
}
