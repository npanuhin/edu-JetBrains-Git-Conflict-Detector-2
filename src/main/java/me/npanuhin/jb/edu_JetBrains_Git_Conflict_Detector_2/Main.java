package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import org.apache.commons.cli.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java -jar git-conflict-detector.jar <localBranch> <remoteBranch> <owner> <repo> [accessToken]");
            return;
        }

        Options options = new Options();

        options.addOption("C", "repo-path", true, "Repository path");
        options.addOption("t", "access-token", true, "GitHub access token");

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);

            String[] remainingArgs = cmd.getArgs();
            if (remainingArgs.length < 4) {
                System.out.println("Usage: java -jar git-conflict-detector.jar <localBranch> <remoteBranch> <owner> <repo> [--repo-path <path>] [--token <token>]");
                return;
            }

            String localBranch = remainingArgs[0];
            String remoteBranch = remainingArgs[1];
            String owner = remainingArgs[2];
            String repo = remainingArgs[3];

            String repoPath = cmd.getOptionValue("C", ".");
            String accessToken = cmd.getOptionValue("t");

            if (localBranch.contains("/")) {
                System.out.println("Local branch names should not contain \"/\" character");
                return;
            }

            if (!remoteBranch.contains("/")) {
                remoteBranch = "origin/" + remoteBranch;
            }

            String mergeBase = GitUtils.getMergeBase(repoPath, localBranch, remoteBranch);

            List<FileChange> localChanges = GitUtils.getModifiedFiles(repoPath, mergeBase, localBranch);

            String remoteHead = GitHubAPI.getLatestCommitSha(owner, repo, remoteBranch, accessToken);

            List<FileChange> remoteChanges = GitHubAPI.getModifiedFiles(owner, repo, mergeBase, remoteHead, accessToken);

            System.out.println("\n--- Potential Conflicts ---\n");

            int maxBranchNameLength = Math.max(remoteBranch.length(), localBranch.length()) + 1;

            Map<String, FileChange> localMap = localChanges.stream()
                    .collect(Collectors.toMap(FileChange::path, fc -> fc));

            for (FileChange remoteChange : remoteChanges) {
                String path = remoteChange.path();
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

        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
