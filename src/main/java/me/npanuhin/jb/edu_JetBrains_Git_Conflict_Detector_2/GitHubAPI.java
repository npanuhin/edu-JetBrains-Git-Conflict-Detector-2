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

    private static JsonNode fetchURL(String urlString, String access_token) throws IOException, URISyntaxException {

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

    public static String getLatestCommitSha(String owner, String repo, String branch, String access_token)
            throws IOException, URISyntaxException {

        String url = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, getBranchName(branch));

        return fetchURL(url, access_token).get("sha").asText();
    }

    public static List<FileChange> getModifiedFiles(String owner, String repo, String base, String head, String access_token)
            throws IOException, URISyntaxException {

        String url = String.format(
                "https://api.github.com/repos/%s/%s/compare/%s...%s",
                owner, repo, base, head
        );

        List<FileChange> fileChanges = new ArrayList<>();

        for (JsonNode fileNode : fetchURL(url, access_token).get("files")) {
            String filename = fileNode.get("filename").asText();
            String rawStatus = fileNode.get("status").asText();

            JsonNode previousFilenameNode = fileNode.get("previous_filename");
            String previousFilename = previousFilenameNode == null ? null : previousFilenameNode.asText();

            fileChanges.add(FileChange.fromGitHub(rawStatus, filename, previousFilename));
        }

        return fileChanges;
    }
}
