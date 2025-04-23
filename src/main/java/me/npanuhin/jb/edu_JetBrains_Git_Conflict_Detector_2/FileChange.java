package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import org.jetbrains.annotations.NotNull;

public record FileChange(FileStatus status, String path, String oldPath) implements Comparable<FileChange> {
    public FileChange {
        if (path == null) {
            throw new IllegalArgumentException("Main file path cannot be null");
        }

        if (status == FileStatus.RENAMED || status == FileStatus.COPIED) {
            if (oldPath == null) {
                throw new IllegalArgumentException("Old path cannot be null for status: " + status);
            }
        } else if (oldPath != null) {
            throw new IllegalArgumentException("Old path should be null for status: " + status);
        }
    }

    @Override
    public @NotNull String toString() {
        int maxStatusLength = 0;
        for (FileStatus fileStatus : FileStatus.values()) {
            maxStatusLength = Math.max(maxStatusLength, fileStatus.toString().length());
        }

        String statusStr = String.format("%-" + maxStatusLength + "s", status.toString());
        String prefix = (oldPath != null) ? oldPath + " -> " : "";
        return String.format("%s  %s%s", statusStr, prefix, path);
    }

    @Override
    public int compareTo(@NotNull FileChange other) {
        String thisKey = (this.oldPath != null) ? this.oldPath : this.path;
        String otherKey = (other.oldPath != null) ? other.oldPath : other.path;
        return thisKey.compareTo(otherKey);
    }
}
