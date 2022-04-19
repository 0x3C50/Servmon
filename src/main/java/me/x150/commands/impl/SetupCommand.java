package me.x150.commands.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.x150.Logger;
import me.x150.struct.PaperVersionGroupListResponse;
import me.x150.struct.VanillaManifestListResponse;
import me.x150.struct.VanillaVersionManifestResponse;
import org.fusesource.jansi.Ansi;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "setup", mixinStandardHelpOptions = true, description = "Sets up a new server")
public class SetupCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-s", "--source"}, description = "Where to get the server from (paper, spigot, vanilla)", required = true)
    Source source;
    @CommandLine.Option(names = {"--serverVersion"}, description = "Which version to download from the source", required = true)
    String version;

    @Override
    public Integer call() throws Exception {
        File pwd = new File(".");
        File[] listed = pwd.listFiles();
        if (listed != null && listed.length > 0) {
            Logger.warning("Current directory is not empty. Make sure you're in the correct place, and press enter to continue.");
            System.console().readLine();
        }
        File serverJar = new File(pwd, "server.jar");
        if (serverJar.exists()) {
            Logger.warning("Server.jar already exists, press enter to continue and overwrite it.");
            System.console().readLine();
        }
        int e = switch (source) {
            case Paper -> downloadPaper(version, serverJar);
            case Spigot -> downloadSpigot(version, serverJar);
            case Vanilla -> downloadVanilla(version, serverJar);
        };
        if (e != 0) return e;
        makeNeededFiles(pwd);
        Logger.info("Set up new server. Start by running start.sh or start.bat");
        return 0;
    }

    void makeNeededFiles(File root) throws Exception {
        Files.writeString(new File(root, "eula.txt").toPath(), "eula=true", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        Path startSh = new File(root, "start.sh").toPath();
        Files.copy(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("startShTemplate.sh")), startSh, StandardCopyOption.REPLACE_EXISTING);
        Files.setPosixFilePermissions(startSh, Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE)); // standard chmod +x
    }

    int downloadVanilla(String ver, File to) throws Exception {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest manifestReq = HttpRequest.newBuilder().uri(URI.create("https://launchermeta.mojang.com/mc/game/version_manifest.json")).build();
        HttpResponse<String> s = client.send(manifestReq, HttpResponse.BodyHandlers.ofString());
        Gson gson = new Gson();
        VanillaManifestListResponse v = gson.fromJson(s.body(), VanillaManifestListResponse.class);
        VanillaManifestListResponse.VersionEntry wanted = Arrays.stream(v.versions).filter(versionEntry -> versionEntry.id.equals(ver)).findFirst().orElse(null);
        if (wanted == null) {
            Logger.error("Found no builds for version \"" + ver + "\". Make sure you made no typos.");
            return 1;
        }
        HttpRequest man = HttpRequest.newBuilder().uri(URI.create(wanted.url)).build();
        HttpResponse<String> manifest = client.send(man, HttpResponse.BodyHandlers.ofString());
        VanillaVersionManifestResponse mf = gson.fromJson(manifest.body(), VanillaVersionManifestResponse.class);
        String downloadUrl = mf.downloads.server.url;
        String expectedSha1 = mf.downloads.server.sha1;
        FileOutputStream fos = new FileOutputStream(to);
        URL u = new URL(downloadUrl);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        long targetLength = huc.getContentLengthLong();
        InputStream stream = huc.getInputStream();
        int readAmount;
        byte[] buffer = new byte[256];
        long read = 0;
        while ((readAmount = stream.read(buffer)) != -1) {
            read += readAmount;
            fos.write(buffer, 0, readAmount);
            double delta = read / (double) targetLength;
            Logger.logWithoutNewline(Ansi.ansi().eraseLine().cursorToColumn(0).fg(Ansi.Color.WHITE).a(String.format("[%s] %.2f%%",
                    getProgressBar(delta, 30),
                    delta * 100)));
        }
        fos.close();
        Logger.log("");
        String sha = hashSum("SHA-1", to);
        if (!sha.equals(expectedSha1)) {
            Logger.error("SHA1 mismatch: Expected " + expectedSha1 + ", got " + sha);
            Logger.error("I refuse to use this server jarfile, make a bug report about this.");
            if (!to.delete()) {
                Logger.warning("Please DO NOT USE THIS JARFILE UNDER ANY CIRCUMSTANCES, I failed to delete it.");
            }
            return 1;
        } else {
            Logger.info("Passed SHA1 match, all good.");
        }
        return 0;
    }

    int downloadSpigot(String ver, File to) throws Exception {
        String downloadUrl = "https://download.getbukkit.org/spigot/spigot-" + ver + ".jar";
        FileOutputStream fos = new FileOutputStream(to);
        URL u = new URL(downloadUrl);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        if (huc.getResponseCode() != 200) {
            Logger.error("Found no builds for version \"" + ver + "\". Make sure you made no typos.");
            return 1;
        }
        long targetLength = huc.getContentLengthLong();
        InputStream stream = huc.getInputStream();
        int readAmount;
        byte[] buffer = new byte[256];
        long read = 0;
        while ((readAmount = stream.read(buffer)) != -1) {
            read += readAmount;
            fos.write(buffer, 0, readAmount);
            double delta = read / (double) targetLength;
            Logger.logWithoutNewline(Ansi.ansi().eraseLine().cursorToColumn(0).fg(Ansi.Color.WHITE).a(String.format("[%s] %.2f%%",
                    getProgressBar(delta, 30),
                    delta * 100)));
        }
        fos.close();
        Logger.log("");
        return 0;
    }

    String hashSum(String alg, File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(alg);
        FileInputStream fis = new FileInputStream(file);
        byte[] byteArray = new byte[1024];
        int bytesCount;
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }

        fis.close();
        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    int downloadPaper(String ver, File to) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpRequest getPairs = HttpRequest.newBuilder().uri(URI.create("https://papermc.io/api/v2/projects/paper"))
                .build();
        HttpResponse<String> pairsResponse = client.send(getPairs, HttpResponse.BodyHandlers.ofString());
        String body = pairsResponse.body();
        JsonObject jobj = JsonParser.parseString(body).getAsJsonObject();
        String desiredGroup = null;
        for (JsonElement group : jobj.getAsJsonArray("version_groups")) {
            if (ver.startsWith(group.getAsString())) {
                desiredGroup = group.getAsString();
                break;
            }
        }
        if (desiredGroup == null) {
            Logger.error("Could not find version group for version \"" + ver + "\". Make sure you made no typos");
            return 1;
        }
        Logger.info("Determined version group to be " + desiredGroup);
        HttpRequest getBuilds = HttpRequest.newBuilder().uri(URI.create("https://papermc.io/api/v2/projects/paper/version_group/" + desiredGroup + "/builds")).build();
        HttpResponse<String> builds = client.send(getBuilds, HttpResponse.BodyHandlers.ofString());
        Gson gson = new Gson();
        PaperVersionGroupListResponse resp = gson.fromJson(builds.body(), PaperVersionGroupListResponse.class);
        PaperVersionGroupListResponse.BuildEntry[] buildsWeWant = Arrays.stream(resp.builds).filter(buildEntry -> buildEntry.version.equals(ver)).toList().toArray(PaperVersionGroupListResponse.BuildEntry[]::new);
        if (buildsWeWant.length == 0) {
            List<String> availBuilds = new ArrayList<>();
            for (PaperVersionGroupListResponse.BuildEntry build : resp.builds) {
                if (!availBuilds.contains(build.version)) availBuilds.add(build.version);
            }
            Logger.error("Found no builds for version \"" + ver + "\". Make sure you made no typos.");
            Logger.info("Available versions in this group are: " + String.join(", ", availBuilds));
            return 1;
        }
        PaperVersionGroupListResponse.BuildEntry targetEntry = buildsWeWant[buildsWeWant.length - 1];
        PaperVersionGroupListResponse.CommitEntry latestChange = targetEntry.changes[targetEntry.changes.length - 1];
        Logger.info("Downloading paper build " + targetEntry.build + " from paper-" + targetEntry.channel);
        Logger.info("Latest commit: " + latestChange.commit + " - " + latestChange.summary);
        String[] bruh = latestChange.message.split("\n");
        Arrays.stream(bruh).map(s -> s.indent(2)).forEach(Logger::info);
        // https://papermc.io/api/v2/projects/paper/versions/1.18.2/builds/295/downloads/paper-1.18.2-295.jar
        String downloadUrl = String.format("https://papermc.io/api/v2/projects/paper/versions/%s/builds/%s/downloads/%s", targetEntry.version, targetEntry.build, targetEntry.downloads.application.name);

        FileOutputStream fos = new FileOutputStream(to);
        URL u = new URL(downloadUrl);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        long targetLength = huc.getContentLengthLong();
        InputStream stream = huc.getInputStream();
        int readAmount;
        byte[] buffer = new byte[256];
        long read = 0;
        while ((readAmount = stream.read(buffer)) != -1) {
            read += readAmount;
            fos.write(buffer, 0, readAmount);
            double delta = read / (double) targetLength;
            Logger.logWithoutNewline(Ansi.ansi().eraseLine().cursorToColumn(0).a(String.format("[%s] %.2f%%",
                    getProgressBar(delta, 30),
                    delta * 100)));
        }
        fos.close();
        Logger.log("");
        String sha = hashSum("SHA-256", to);
        if (!sha.equals(targetEntry.downloads.application.sha256)) {
            Logger.error("SHA256 mismatch: Expected " + targetEntry.downloads.application.sha256 + ", got " + sha);
            Logger.error("I refuse to use this server jarfile, contact paper support about this.");
            if (!to.delete()) {
                Logger.warning("Please DO NOT USE THIS JARFILE UNDER ANY CIRCUMSTANCES, I failed to delete it.");
            }
            return 1;
        } else {
            Logger.info("Passed SHA256 match, all good.");
        }

        return 0;
    }

    String getProgressBar(double delta, int totalLength) {
        double deltaCpy = Math.min(1, Math.max(0, delta));
        double len = deltaCpy * totalLength;
        int guaranteedLen = (int) Math.floor(len);
        double whatsLeft = len - guaranteedLen;
        String partialBlock;
        if (whatsLeft <= 1 / 8d) partialBlock = "";
        else if (whatsLeft >= 7 / 8d) partialBlock = "▉";
        else if (whatsLeft >= 6 / 8d) partialBlock = "▊";
        else if (whatsLeft >= 5 / 8d) partialBlock = "▋";
        else if (whatsLeft >= 4 / 8d) partialBlock = "▌";
        else if (whatsLeft >= 3 / 8d) partialBlock = "▍";
        else if (whatsLeft >= 2 / 8d) partialBlock = "▎";
        else if (whatsLeft >= 1 / 8d) partialBlock = "▏";
        else partialBlock = "█";
        String s = "█".repeat(guaranteedLen) + partialBlock;
        return s + " ".repeat(totalLength - s.length());
    }

    enum Source {
        Paper, Spigot, Vanilla
    }
}
