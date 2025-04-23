package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class IntegrationTest {

    private static String generateCompareFilesJson(int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) sb.append(",");
            sb.append(String.format("""
                    { "filename": "file%d.txt", "status": "modified" }
                    """, i));
        }
        return sb.toString();
    }

    private static String generateCommitsArrayJson(int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) sb.append(",");
            sb.append(String.format("""
                    { "sha": "sha%d" }
                    """, i));
        }
        return sb.toString();
    }

    @Test
    void testManyFiles() throws IOException, URISyntaxException {
        final int NUM_PAGES = 3;
        final int COMMITS_PER_PAGE = 300;
        final int FILES_PER_COMMIT = 1;

        try (var gitHubMock = mockStatic(GitHubAPI.class, CALLS_REAL_METHODS)) {
            ObjectMapper mapper = new ObjectMapper();

            // Mock the first page
            gitHubMock.when(() ->
                    GitHubAPI.fetchURL(eq("https://api.github.com/repos/owner/repo/compare/base...head?per_page=100"), any())
            ).thenReturn(mapper.readTree(String.format("""
                            {
                              "files": [%s],
                              "commits": [%s]
                            }
                            """,
                    generateCompareFilesJson(0, FILES_PER_COMMIT * COMMITS_PER_PAGE * NUM_PAGES),
                    generateCommitsArrayJson(0, COMMITS_PER_PAGE)
            )));

            // Mock other pages
            for (int page = 2; page <= NUM_PAGES + 1; page++) {
                int from = (page - 1) * COMMITS_PER_PAGE;
                int to = Math.min(page * COMMITS_PER_PAGE, NUM_PAGES * COMMITS_PER_PAGE);
                String url = String.format("https://api.github.com/repos/owner/repo/compare/base...head?per_page=100&page=%d", page);

                JsonNode pageNode;
                if (from < to) {
                    pageNode = mapper.readTree(String.format("""
                            {
                              "commits": [%s]
                            }
                            """, generateCommitsArrayJson(from, to)));
                } else {
                    pageNode = mapper.readTree("{}");
                }

                gitHubMock.when(() -> GitHubAPI.fetchURL(eq(url), any())).thenReturn(pageNode);
            }

            // Mock SHAs
            for (int i = 0; i < NUM_PAGES * COMMITS_PER_PAGE; i++) {
                final int commitNum = i;
                gitHubMock.when(() ->
                        GitHubAPI.fetchURL(eq(String.format("https://api.github.com/repos/owner/repo/commits/sha%d", commitNum)), any())
                ).thenReturn(mapper.readTree(String.format("""
                        {
                          "files": [%s]
                        }
                        """, generateCompareFilesJson(commitNum, commitNum + 1))));
            }

            // Expected result
            List<FileChange> expected = new ArrayList<>();
            for (int i = 0; i < NUM_PAGES * COMMITS_PER_PAGE; i++) {
                expected.add(new FileChange(FileStatus.MODIFIED, "file" + i + ".txt", null));
            }

            List<FileChange> result = GitHubAPI.compareCommits("owner", "repo", "base", "head", null);
            assertEquals(expected, result);
        }
    }
}
