package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class IntegrationTest {

    @Test
    void testManyFiles() throws IOException, URISyntaxException {
        final int FILES_IN_COMMIT = 100;
        final int NUM_COMMITS = 10;

        try (var gitHubMock = mockStatic(GitHubAPI.class, CALLS_REAL_METHODS)) {
            ObjectMapper mapper = new ObjectMapper();

            StringBuilder compareFiles = new StringBuilder();
            StringBuilder commitsArray = new StringBuilder();
            List<String> filenames = new ArrayList<>();

            for (int i = 0; i < NUM_COMMITS * FILES_IN_COMMIT; i++) {
                if (i > 0) compareFiles.append(",");
                compareFiles.append(String.format("""
                            { "filename": "file%d.txt", "status": "modified" }
                        """, i));
                filenames.add(String.format("file%d.txt", i));

                if (i < NUM_COMMITS) {
                    if (i > 0) commitsArray.append(",");
                    commitsArray.append(String.format("""
                            { "sha": "sha%d" }
                            """, i));
                }
            }

            JsonNode compareResponse = mapper.readTree(String.format("""
                    {
                      "files": [%s],
                      "commits": [%s]
                    }
                    """, compareFiles, commitsArray));

            gitHubMock.when(() ->
                    GitHubAPI.fetchURL(eq("https://api.github.com/repos/owner/repo/compare/base...head"), any())
            ).thenReturn(compareResponse);

            for (int i = 0; i < NUM_COMMITS; i++) {
                final int commitNum = i;

                StringBuilder commitFiles = new StringBuilder();
                for (int j = commitNum * FILES_IN_COMMIT; j < (commitNum + 1) * FILES_IN_COMMIT; j++) {
                    if (j > commitNum * FILES_IN_COMMIT) commitFiles.append(",");
                    commitFiles.append(String.format("""
                                { "filename": "file%d.txt", "status": "modified" }
                            """, j));
                }

                gitHubMock.when(
                        () -> GitHubAPI.fetchURL(
                                eq(String.format("https://api.github.com/repos/owner/repo/commits/sha%d", commitNum)),
                                any()
                        )
                ).thenReturn(mapper.readTree(String.format("""
                        {
                          "files": [%s]
                        }
                        """, commitFiles)));
            }

            List<FileChange> expected = new ArrayList<>();
            for (String filename : filenames) {
                expected.add(new FileChange(FileStatus.MODIFIED, filename, null));
            }

            List<FileChange> result = GitHubAPI.compareCommits(
                    "owner", "repo", "base", "head", null
            );

            assertEquals(expected, result);
        }
    }
}
