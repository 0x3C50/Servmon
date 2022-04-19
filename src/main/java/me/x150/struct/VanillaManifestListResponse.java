package me.x150.struct;

import java.util.Arrays;

public class VanillaManifestListResponse {
    public LatestStats latest;
    public VersionEntry[] versions;

    @Override
    public String toString() {
        return "VanillaManifestListResponse{" +
                "latest=" + latest +
                ", versions=" + Arrays.toString(versions) +
                '}';
    }

    public static class VersionEntry {
        public String id, type, url, time, releaseTime;

        @Override
        public String toString() {
            return "VersionEntry{" +
                    "id='" + id + '\'' +
                    ", type='" + type + '\'' +
                    ", url='" + url + '\'' +
                    ", time='" + time + '\'' +
                    ", releaseTime='" + releaseTime + '\'' +
                    '}';
        }
    }

    public static class LatestStats {
        public String release, snapshot;

        @Override
        public String toString() {
            return "LatestStats{" +
                    "release='" + release + '\'' +
                    ", snapshot='" + snapshot + '\'' +
                    '}';
        }
    }
}
