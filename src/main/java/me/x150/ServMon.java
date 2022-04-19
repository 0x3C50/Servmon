package me.x150;

import me.x150.commands.CommandRegistry;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.concurrent.Callable;

public class ServMon {
    static void printHelp(CommandRegistry reg) {
        Logger.error("Usage:");
        Logger.error("Servmon-1.0.jar [" + String.join(", ", reg.getCommandNames()) + "]");
        System.exit(1);
    }

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        CommandRegistry reg = new CommandRegistry();
        if (args.length < 1) {
            printHelp(reg);
            return;
        }
        String name = args[0];
        Callable<Integer> cmd = reg.getByName(name);
        if (cmd == null) {
            printHelp(reg);
            return;
        }
        String[] restArgs = Arrays.copyOfRange(args, 1, args.length);
        int exit = new CommandLine(cmd).execute(restArgs);
        if (exit != 0) {
            Logger.info("Exited with exit code " + exit + ". Make sure all errors above are corrected, before trying again.");
        }
        System.exit(1);
    }
}
