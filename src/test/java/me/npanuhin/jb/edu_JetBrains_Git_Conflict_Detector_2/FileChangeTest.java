package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileChangeTest {

    @Test
    void testFromGit() {
        FileChange change = FileChange.fromGit("M", "file.txt", null);
        assertEquals(FileStatus.MODIFIED, change.status());
        assertEquals("file.txt", change.path());

        assertThrows(IllegalArgumentException.class,
                () -> FileChange.fromGit("M", null, null));
    }

    @Test
    void testFromGitHubValid() {
        FileChange added = FileChange.fromGitHub("added", "file.txt", null);
        assertEquals(FileStatus.ADDED, added.status());

        FileChange renamed = FileChange.fromGitHub("renamed", "new.txt", "old.txt");
        assertEquals(FileStatus.RENAMED, renamed.status());
        assertEquals("new.txt", renamed.path());
        assertEquals("old.txt", renamed.oldPath());
    }

    @Test
    void testFromGitHubInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> FileChange.fromGitHub("added", "file.txt", "wrong.txt"));

        assertThrows(IllegalArgumentException.class,
                () -> FileChange.fromGitHub("renamed", "file.txt", null));
    }
}
