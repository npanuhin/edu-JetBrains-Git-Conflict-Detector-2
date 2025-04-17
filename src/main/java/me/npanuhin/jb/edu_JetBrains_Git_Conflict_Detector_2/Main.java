package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java -jar git-conflict-detector.jar <localBranch> <remoteBranch> <owner> <repo> [accessToken]");
            return;
        }

        String localBranch = args[0];
        String remoteBranch = args[1];
        String owner = args[2];
        String repo = args[3];
        String accessToken = args.length >= 5 ? args[4] : null;
        String repoPath = ".";

        if (localBranch.contains("/")) {
            System.out.println("Local branch names should not contain \"/\" character");
            return;
        }

        if (!remoteBranch.contains("/")) {
            remoteBranch = "origin/" + remoteBranch;
        }

        try {
            String mergeBase = GitUtils.getMergeBase(repoPath, localBranch, remoteBranch);

            List<FileChange> localChanges = GitUtils.getModifiedFiles(repoPath, mergeBase, localBranch);

            String remoteHead = GitHubAPI.getLatestCommitSha(owner, repo, remoteBranch, accessToken);

            List<FileChange> remoteChanges = GitHubAPI.getModifiedFiles(owner, repo, mergeBase, remoteHead, accessToken);

            System.out.println("\n--- Potential Conflicts ---\n");

            int maxBranchNameLength = Math.max(remoteBranch.length(), localBranch.length()) + 1;

            Map<String, FileChange> localMap = localChanges.stream()
                    .collect(Collectors.toMap(FileChange::getPath, fc -> fc));

            for (FileChange remoteChange : remoteChanges) {
                String path = remoteChange.getPath();
                if (localMap.containsKey(path)) {
                    FileChange localChange = localMap.get(path);

                    System.out.printf(
                            "Conflict: %s\n" +
                                    "  %-" + maxBranchNameLength + "s %s\n" +
                                    "  %-" + maxBranchNameLength + "s %s\n\n",
                            path,
                            remoteBranch + ":", remoteChange,
                            localBranch + ":", localChange
                    );
                }
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Error: " + e.getMessage());
        }
    }
}
