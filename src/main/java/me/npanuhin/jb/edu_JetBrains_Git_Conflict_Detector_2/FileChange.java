package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record FileChange(FileStatus status, String path, String oldPath) {
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
        int maxStatusLength = Arrays.stream(FileStatus.values())
                .mapToInt(status -> status.toString().length())
                .max()
                .orElse(0);

        String statusStr = String.format("%-" + maxStatusLength + "s", status.toString());
        String prefix = (oldPath != null) ? oldPath + " -> " : "";
        return String.format("%s  %s%s", statusStr, prefix, path);
    }
}
