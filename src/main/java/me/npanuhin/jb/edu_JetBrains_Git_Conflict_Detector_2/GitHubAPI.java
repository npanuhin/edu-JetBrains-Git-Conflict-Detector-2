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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GitHubAPI {

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

    static String getLastCommitOnBranch(String owner, String repo, String branch, String access_token)
            throws RuntimeException, URISyntaxException {

        String url = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, getBranchName(branch));

        try {
            return fetchURL(url, access_token).get("sha").asText();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch latest commit from GitHub API. Consider providing a GitHub access token (--access-token).", e);
        }
    }

    private static Map<String, String> fetchGitHubTree(String owner, String repo, String sha, String access_token)
            throws IOException, URISyntaxException {

        // Method 1: Automatic recursion
        JsonNode tree = fetchURL(
                String.format("https://api.github.com/repos/%s/%s/git/trees/%s", owner, repo, sha),
                access_token
        );

        if (tree.get("truncated").asBoolean()) {
            // Method 2: Manual recursion
            tree = fetchURL(
                    String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1", owner, repo, sha),
                    access_token
            );

            if (tree.get("truncated").asBoolean()) {
                throw new RuntimeException("GitHub API returned a truncated tree. There are too many files in one of the repository's folders");
            }
        }

        Map<String, String> files = new HashMap<>();

        for (JsonNode item : tree.get("tree")) {
            if (item.get("type").asText().equals("blob")) {
                files.put(item.get("path").asText(), item.get("sha").asText());
            } else if (item.get("type").asText().equals("tree")) {
                Map<String, String> subTreeFiles = fetchGitHubTree(owner, repo, item.get("sha").asText(), access_token);
                for (Map.Entry<String, String> entry : subTreeFiles.entrySet()) {
                    files.put(item.get("path").asText() + "/" + entry.getKey(), entry.getValue());
                }
            } else {
                throw new RuntimeException("Unsupported GitHub tree item type: " + item.get("type").asText());
            }
        }

        return files;
    }

    static List<FileChange> compareTrees(
            String owner,
            String repo,
            String mergeBaseSha,
            String headSha,
            String access_token
    ) throws IOException, URISyntaxException {

        Map<String, String> mergeBaseFiles = fetchGitHubTree(owner, repo, mergeBaseSha, access_token);
        Map<String, String> headFiles = fetchGitHubTree(owner, repo, headSha, access_token);

        List<FileChange> result = new ArrayList<>();

        for (String path : mergeBaseFiles.keySet()) {
            if (!headFiles.containsKey(path)) {
                result.add(new FileChange(FileStatus.REMOVED, path, null));
            }
        }

        for (String path : headFiles.keySet()) {
            if (!mergeBaseFiles.containsKey(path)) {
                result.add(new FileChange(FileStatus.ADDED, path, null));
            }
        }

        for (Map.Entry<String, String> entry : mergeBaseFiles.entrySet()) {
            String path = entry.getKey();
            String mergeBaseFileSha = entry.getValue();
            String headFileSha = headFiles.get(path);

            if (headFileSha != null && !headFileSha.equals(mergeBaseFileSha)) {
                result.add(new FileChange(FileStatus.MODIFIED, path, null));
            }
        }

        return result;
    }

    private static void addFileChanges(JsonNode filesNode, List<FileChange> changes) {
        for (JsonNode fileNode : filesNode) {
            String filename = fileNode.get("filename").asText();
            String rawStatus = fileNode.get("status").asText();
            JsonNode previousFilenameNode = fileNode.get("previous_filename");
            String previousFilename = previousFilenameNode == null ? null : previousFilenameNode.asText();

            changes.add(new FileChange(
                    FileStatus.fromGitHub(rawStatus),
                    filename,
                    previousFilename
            ));
        }
    }

    static List<FileChange> compareCommits(
            String owner,
            String repo,
            String base,
            String head,
            String access_token
    ) throws RuntimeException, URISyntaxException {

        String url = String.format(
                "https://api.github.com/repos/%s/%s/compare/%s...%s?per_page=100",
                owner, repo, base, head
        );

        try {
            List<FileChange> fileChanges = new ArrayList<>();

            JsonNode root = fetchURL(url, access_token);

            JsonNode filesNode = root.get("files");
            if (filesNode.size() >= 300) {
                System.err.println("GitHub compare API returned 300 files, falling back to per-commit analysis...");

                int pageNum = 1;
                while (root.has("commits")) {
                    for (JsonNode commitNode : root.get("commits")) {
                        String sha = commitNode.get("sha").asText();
                        String commitUrl = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, sha);

                        addFileChanges(fetchURL(commitUrl, access_token).get("files"), fileChanges);
                    }

                    url = String.format(
                            "https://api.github.com/repos/%s/%s/compare/%s...%s?per_page=100&page=%d",
                            owner, repo, base, head, ++pageNum
                    );
                    root = fetchURL(url, access_token);
                }
            } else {
                addFileChanges(filesNode, fileChanges);
            }

            return fileChanges;

        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch file changes from GitHub API. Please ensure that the repository owner and name are correct and match the local path.", e);
        }
    }
}
