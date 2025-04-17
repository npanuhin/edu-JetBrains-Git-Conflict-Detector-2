package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import java.util.Arrays;

public class FileChange {
    private final FileStatus status;
    private final String path;
    private final String oldPath;

    public FileChange(FileStatus status, String path, String oldPath) {
        this.status = status;
        this.path = path;
        this.oldPath = oldPath;
    }

    public static FileChange fromGit(String rawStatus, String path, String newPath) {
        FileStatus status = FileStatus.fromGit(rawStatus);

        if ((status == FileStatus.RENAMED || status == FileStatus.COPIED) && newPath != null) {
            return new FileChange(status, newPath, path);
        } else if ((status != FileStatus.RENAMED && status != FileStatus.COPIED) && newPath == null) {
            return new FileChange(status, path, null);
        } else {
            throw new IllegalArgumentException("Invalid path or new path for status: " + status);
        }
    }

    public static FileChange fromGitHub(String rawStatus, String filename, String previousFilename) {
        FileStatus status = FileStatus.fromGitHub(rawStatus);
        if ((previousFilename == null) == (status == FileStatus.RENAMED || status == FileStatus.COPIED)) {
            throw new IllegalArgumentException("Mismatched previous filename for status: " + status);
        }
        return new FileChange(status, filename, previousFilename);
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        int maxStatusLength = Arrays.stream(FileStatus.values())
                .mapToInt(status -> status.toString().length())
                .max()
                .orElse(0);

        String statusStr = String.format("%-" + maxStatusLength + "s", status.toString());
        String prefix = (oldPath != null) ? oldPath + " -> " : "";
        return String.format("%s  %s%s", statusStr, prefix, path);
    }
}
