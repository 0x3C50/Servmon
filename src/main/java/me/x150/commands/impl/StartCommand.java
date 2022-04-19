package me.x150.commands.impl;

import me.x150.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "start", mixinStandardHelpOptions = true, description = "Runs the current server on the PWD, or somewhere else specified")
public class StartCommand implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "Where the server is (has to be a folder)", defaultValue = ".")
    File path = new File(".");

    @CommandLine.Option(names = {"-a", "--args"}, description = "Extra arguments to the server process", defaultValue = "")
    String extraArgs = "";

    @Override
    public Integer call() throws Exception {
        if (!path.isDirectory()) {
            Logger.error("Path is not a directory, aborting");
            return 1;
        }
        File startScript = new File(path, "start.sh");
        if (startScript.exists()) {
            Logger.info("Starting start script");
            Process start = new ProcessBuilder(startScript.getAbsolutePath(), extraArgs).inheritIO().directory(path).start();
            int exitCode = start.waitFor();
            if (exitCode != 0) {
                Logger.warning("Start script returned exit code " + exitCode + ", check for errors above. Press enter to continue");
                System.console().readLine();
            }
        } else {
            Logger.warning("Start script does not exist, will not run start script. Will run server directly instead");
            List<File> potentialEntryPoints = Arrays.stream(Objects.requireNonNull(path.listFiles())).filter(file -> file.getName().endsWith(".jar")).toList();
            if (potentialEntryPoints.size() == 0) {
                Logger.error("Could not find any jarfile in this directory. Are you sure this is a server?");
                return 1;
            } else if (potentialEntryPoints.size() > 1) {
                Logger.error("Found more than one potential entry point. Cannot proceed, please make a start.sh script that points to the correct one.");
                return 1;
            } else {
                Logger.info("Running jarfile with java path on JAVA_HOME");
                String jHome = System.getProperty("java.home");
                String runtime;
                if (jHome == null || jHome.isEmpty()) {
                    Logger.warning("JAVA_HOME is empty, using default java");
                    runtime = "java";
                } else {
                    File home = new File(jHome);
                    File java = new File(home, "/bin/java");
                    runtime = java.getAbsolutePath();
                }
                Process p = new ProcessBuilder(runtime, "-jar", potentialEntryPoints.get(0).getName(), extraArgs).directory(path).inheritIO().start();
                int ex = p.waitFor();
                Logger.info("Server process exited with exit code " + ex);
            }
        }
        return 0;
    }
}
