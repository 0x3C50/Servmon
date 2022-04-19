package me.x150.struct;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public class PaperVersionGroupListResponse {
    public String project_id, project_name, version_group;
    public String[] versions;
    public BuildEntry[] builds;

    @Override
    public String toString() {
        return "PaperVersionGroupListResponse{" +
                "project_id='" + project_id + '\'' +
                ", project_name='" + project_name + '\'' +
                ", version_group='" + version_group + '\'' +
                ", versions=" + Arrays.toString(versions) +
                ", builds=" + Arrays.toString(builds) +
                '}';
    }

    public static class DownloadEntry {
        public String name, sha256;

        @Override
        public String toString() {
            return "DownloadEntry{" +
                    "name='" + name + '\'' +
                    ", sha256='" + sha256 + '\'' +
                    '}';
        }
    }

    public static class DownloadMap {
        public DownloadEntry application;
        @SerializedName("mojang-mappings")
        public DownloadEntry mojangMappings;

        @Override
        public String toString() {
            return "DownloadMap{" +
                    "application=" + application +
                    ", mojangMappings=" + mojangMappings +
                    '}';
        }
    }

    public static class CommitEntry {
        public String commit, summary, message;

        @Override
        public String toString() {
            return "CommitEntry{" +
                    "commit='" + commit + '\'' +
                    ", summary='" + summary + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    public static class BuildEntry {
        public String version, time, channel;
        public int build;
        public boolean promoted;
        public CommitEntry[] changes;
        public DownloadMap downloads;

        @Override
        public String toString() {
            return "BuildEntry{" +
                    "version='" + version + '\'' +
                    ", time='" + time + '\'' +
                    ", channel='" + channel + '\'' +
                    ", build=" + build +
                    ", promoted=" + promoted +
                    ", changes=" + Arrays.toString(changes) +
                    ", downloads=" + downloads.toString() +
                    '}';
        }
    }
}
