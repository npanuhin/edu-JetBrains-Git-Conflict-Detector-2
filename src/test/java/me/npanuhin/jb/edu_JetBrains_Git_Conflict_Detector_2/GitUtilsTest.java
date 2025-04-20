package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GitUtilsTest {

    @Test
    void testRunCmdSuccess() {
        String version = GitUtils.getMergeBase(".", "HEAD", "HEAD"); // Same ref
        assertFalse(version.isEmpty());
    }

    @Test
    void testRunCmdFailure() {
        List<String> badCommand = List.of("git", "badcommand");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            GitUtils.getMergeBase(".", "HEAD", "badbranchname");
        });
        assertTrue(exception.getMessage().contains("Failed to run git command"));
    }

    @Test
    void testGetMergeBaseMocked() {
        try (MockedStatic<GitUtils> mock = mockStatic(GitUtils.class)) {
            mock.when(() -> GitUtils.getMergeBase(".", "branchA", "branchB"))
                    .thenReturn("fake_merge_base");

            String base = GitUtils.getMergeBase(".", "branchA", "branchB");
            assertEquals("fake_merge_base", base);
        }
    }

    @Test
    void testGetModifiedFilesParsing() {
        String diffOutput = String.join("\n",
                "M\tfile.txt",
                "A\tnewfile.txt",
                "R100\told.txt\tnew.txt"
        );

        try (MockedStatic<GitUtils> mock = mockStatic(GitUtils.class, CALLS_REAL_METHODS)) {
            mock.when(() -> GitUtils.runGitCommand(any())).thenReturn(diffOutput);

            List<FileChange> result = GitUtils.getModifiedFiles(".", "abc", "def");
            assertEquals(3, result.size());
            assertEquals(FileStatus.MODIFIED, result.get(0).status());
            assertEquals("file.txt", result.get(0).path());
            assertEquals(FileStatus.ADDED, result.get(1).status());
            assertEquals(FileStatus.RENAMED, result.get(2).status());
            assertEquals("old.txt", result.get(2).oldPath());
            assertEquals("new.txt", result.get(2).path());
        }
    }
}
