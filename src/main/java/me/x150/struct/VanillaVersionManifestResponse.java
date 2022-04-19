package me.x150.struct;

public class VanillaVersionManifestResponse {
    public DownloadsMap downloads;

    @Override
    public String toString() {
        return "VanillaVersionManifestResponse{" +
                "downloads=" + downloads +
                '}';
    }

    public static class DownloadsMap {
        public VersionEntry client, client_mappings, server, server_mappings;

        @Override
        public String toString() {
            return "DownloadsMap{" +
                    "client=" + client +
                    ", client_mappings=" + client_mappings +
                    ", server=" + server +
                    ", server_mappings=" + server_mappings +
                    '}';
        }
    }

    public static class VersionEntry {
        public String sha1, url;
        public long size;

        @Override
        public String toString() {
            return "VersionEntry{" +
                    "sha1='" + sha1 + '\'' +
                    ", url='" + url + '\'' +
                    ", size=" + size +
                    '}';
        }
    }
}
