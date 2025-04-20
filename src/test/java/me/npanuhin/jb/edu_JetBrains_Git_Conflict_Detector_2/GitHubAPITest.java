package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class GitHubAPITest {

    @Test
    void testCompareCommitsParsing() throws Exception {
        String mockJson = """
                    {
                      "files": [
                        {
                          "filename": "file.txt",
                          "status": "added"
                        }
                      ]
                    }
                """;

        GitHubAPI realAPI = new GitHubAPI();
        GitHubAPI spyAPI = spy(realAPI);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode mockNode = mapper.readTree(mockJson);

        try (var mockStatic = mockStatic(GitHubAPI.class, CALLS_REAL_METHODS)) {
            mockStatic.when(() -> GitHubAPI.compareCommits(any(), any(), any(), any(), any())).thenCallRealMethod();

            mockStatic.when(() -> GitHubAPI.fetchURL(any(), any())).thenReturn(mockNode);

            List<FileChange> result = GitHubAPI.compareCommits("owner", "repo", "base", "head", "token");

            assertEquals(1, result.size());
            assertEquals("file.txt", result.getFirst().path());
            assertEquals(FileStatus.ADDED, result.getFirst().status());
        }
    }
}
