package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import org.apache.commons.cli.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("C", "repo-path", true, "Repository path");
        options.addOption("t", "token", true, "GitHub access token");
        options.addOption("T", "use-trees", false, "Compare using GitHub trees instead of commits");

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);

            String[] remainingArgs = cmd.getArgs();
            if (remainingArgs.length < 4) {
                System.out.println("Usage: java -jar git-conflict-detector.jar " +
                        "<localBranch> <remoteBranch> <owner> <repo> " +
                        "[--repo-path <path>] [--token <token>] [--use-trees]");
                return;
            }

            String localBranch = remainingArgs[0];
            String remoteBranch = remainingArgs[1];
            String owner = remainingArgs[2];
            String repo = remainingArgs[3];

            String repoPath = cmd.getOptionValue("C", ".");
            String accessToken = cmd.getOptionValue("t");
            boolean compareUsingTrees = cmd.hasOption("T");

            if (localBranch.contains("/")) {
                System.out.println("Local branch names should not contain \"/\" character");
                return;
            }

            if (!remoteBranch.contains("/")) {
                remoteBranch = "origin/" + remoteBranch;
            }

            String mergeBase = GitUtils.getMergeBase(repoPath, localBranch, remoteBranch);

            List<FileChange> localChanges = GitUtils.getModifiedFiles(repoPath, mergeBase, localBranch);

            String remoteHead = GitHubAPI.getLastCommitOnBranch(owner, repo, remoteBranch, accessToken);

            List<FileChange> remoteChanges;
            if (compareUsingTrees) {
                System.out.println("Comparing using trees...");
                remoteChanges = GitHubAPI.compareTrees(owner, repo, mergeBase, remoteHead, accessToken);
            } else {
                System.out.println("Comparing using commits...");
                remoteChanges = GitHubAPI.compareCommits(owner, repo, mergeBase, remoteHead, accessToken);
            }

            Map<String, FileChange> remoteMap = new HashMap<>();
            for (FileChange fc : remoteChanges) {
                remoteMap.put(fc.path(), fc);
            }
            Collections.sort(localChanges);
            int maxBranchNameLength = Math.max(remoteBranch.length(), localBranch.length()) + 1;

            System.out.println("\n--- Potential Conflicts ---\n");

            for (FileChange localChange : localChanges) {
                String path = localChange.path();
                FileChange remoteChange = remoteMap.get(path);
                if (remoteChange != null) {
                    System.out.printf(
                            "Conflict: %s\n" +
                                    "  %-" + maxBranchNameLength + "s %s\n" +
                                    "  %-" + maxBranchNameLength + "s %s\n\n",
                            path,
                            localBranch + ":", localChange,
                            remoteBranch + ":", remoteChange
                    );
                }
            }
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
