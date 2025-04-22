package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileChangeTest {

    @Test
    void testFromGit() {
        FileChange change = new FileChange(FileStatus.fromGit("M"), "file.txt", null);
        assertEquals(FileStatus.MODIFIED, change.status());
        assertEquals("file.txt", change.path());

        assertThrows(IllegalArgumentException.class,
                () -> new FileChange(FileStatus.fromGit("M"), null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new FileChange(FileStatus.fromGit("A"), "file.txt", "new.txt"));

        assertThrows(IllegalArgumentException.class,
                () -> new FileChange(FileStatus.fromGit("R"), "file.txt", null));
    }

    @Test
    void testFromGitHub() {
        FileChange added = new FileChange(FileStatus.fromGitHub("added"), "file.txt", null);
        assertEquals(FileStatus.ADDED, added.status());

        FileChange renamed = new FileChange(FileStatus.fromGitHub("renamed"), "new.txt", "old.txt");
        assertEquals(FileStatus.RENAMED, renamed.status());
        assertEquals("new.txt", renamed.path());
        assertEquals("old.txt", renamed.oldPath());

        assertThrows(IllegalArgumentException.class,
                () -> new FileChange(FileStatus.fromGitHub("modified"), null, null));

        assertThrows(IllegalArgumentException.class,
                () -> new FileChange(FileStatus.fromGitHub("added"), "file.txt", "wrong.txt"));

        assertThrows(IllegalArgumentException.class,
                () -> new FileChange(FileStatus.fromGitHub("renamed"), "file.txt", null));
    }
}
