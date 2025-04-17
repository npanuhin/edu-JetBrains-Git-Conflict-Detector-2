package me.npanuhin.jb.edu_JetBrains_Git_Conflict_Detector_2;

public enum FileStatus {
    ADDED,
    REMOVED,
    MODIFIED,
    RENAMED,
    COPIED,
    CHANGED,
    UNCHANGED;

    public static FileStatus fromGit(String status) {
        char code = status.charAt(0);
        return switch (code) {
            case 'A' -> ADDED;
            case 'D' -> REMOVED;
            case 'M' -> MODIFIED;
            case 'R' -> RENAMED;
            case 'C' -> COPIED;
            case 'T' -> CHANGED;
            case 'U' -> UNCHANGED;
            default -> throw new IllegalArgumentException("Unknown git status: " + code);
        };
    }

    public static FileStatus fromGitHub(String status) {
        return FileStatus.valueOf(status.toUpperCase());
    }

    @Override
    public String toString() {
        String lowercase = name().toLowerCase();
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1);
    }
}
