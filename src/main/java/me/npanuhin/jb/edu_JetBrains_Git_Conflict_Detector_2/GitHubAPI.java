package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GitHubAPI {

    static JsonNode fetchURL(String urlString, String access_token) throws IOException, URISyntaxException {

        URI uri = new URI(urlString);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        if (access_token != null) {
            connection.setRequestProperty("Authorization", "token " + access_token);
        }
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestProperty("User-Agent", "JetBrains-Git-Conflict-Detector");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return new ObjectMapper().readTree(reader);
        } finally {
            connection.disconnect();
        }
    }

    private static String getBranchName(String branch) {
        String[] parts = branch.split("/");
        return parts[parts.length - 1];
    }

    public static String getLastCommitOnBranch(String owner, String repo, String branch, String access_token)
            throws RuntimeException, URISyntaxException {

        String url = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, getBranchName(branch));

        try {
            return fetchURL(url, access_token).get("sha").asText();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch latest commit from GitHub API. Consider providing a GitHub access token (--access-token).", e);
        }
    }

    private static void parseCommits(JsonNode fileNode, List<FileChange> changes) {
        String filename = fileNode.get("filename").asText();
        String rawStatus = fileNode.get("status").asText();
        JsonNode previousFilenameNode = fileNode.get("previous_filename");
        String previousFilename = previousFilenameNode == null ? null : previousFilenameNode.asText();

        changes.add(FileChange.fromGitHub(rawStatus, filename, previousFilename));
    }

    private static List<FileChange> analyzeAllCommits(JsonNode commitsArray, String owner, String repo, String access_token)
            throws IOException, URISyntaxException {

        List<FileChange> changes = new ArrayList<>();

        for (JsonNode commitNode : commitsArray) {
            String sha = commitNode.get("sha").asText();
            String commitUrl = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, sha);

            for (JsonNode fileNode : fetchURL(commitUrl, access_token).get("files")) {
                parseCommits(fileNode, changes);
            }
        }

        return changes;
    }

    public static List<FileChange> compareCommits(String owner, String repo, String base, String head, String access_token)
            throws RuntimeException, URISyntaxException {

        String url = String.format(
                "https://api.github.com/repos/%s/%s/compare/%s...%s",
                owner, repo, base, head
        );

        try {
            JsonNode root = fetchURL(url, access_token);

            JsonNode filesNode = root.get("files");
            if (filesNode.size() >= 300) {
                System.err.println("GitHub compare API returned 300 files, falling back to per-commit analysis...");
                return analyzeAllCommits(root.get("commits"), owner, repo, access_token);
            }

            List<FileChange> fileChanges = new ArrayList<>();
            for (JsonNode fileNode : filesNode) {
                parseCommits(fileNode, fileChanges);
            }
            return fileChanges;

        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch file changes from GitHub API. Please ensure that the repository owner and name are correct and match the local path.", e);
        }
    }
}
