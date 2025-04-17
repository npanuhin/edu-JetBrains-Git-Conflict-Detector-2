package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

public class FileChange {
    private final FileStatus status;
    private final String path;
    private final String oldPath;

    public FileChange(FileStatus status, String path, String oldPath) {
        this.status = status;
        this.path = path;
        this.oldPath = oldPath;
    }

    public static FileChange fromGit(String rawStatus, String[] paths) {
        FileStatus status = FileStatus.fromGit(rawStatus);
        if ((status == FileStatus.RENAMED || status == FileStatus.COPIED) && paths.length == 2) {
            return new FileChange(status, paths[1], paths[0]);
        } else if (paths.length == 1) {
            return new FileChange(status, paths[0], null);
        } else {
            throw new IllegalArgumentException("Unexpected number of paths for status: " + status);
        }
    }

    public static FileChange fromGitHub(String rawStatus, String filename, String previousFilename) {
        FileStatus status = FileStatus.fromGitHub(rawStatus);
        if ((previousFilename != null) != (status == FileStatus.RENAMED || status == FileStatus.COPIED)) {
            throw new IllegalArgumentException("Mismatched previous filename for status: " + status);
        }
        return new FileChange(status, filename, previousFilename);
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        String prefix = (oldPath != null) ? oldPath + " -> " : "";
        return String.format("%-10s  %s%s", status.name(), prefix, path);
    }
}
