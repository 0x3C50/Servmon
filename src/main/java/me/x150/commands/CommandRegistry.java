package me.x150.commands;

import me.x150.commands.impl.SetupCommand;
import me.x150.commands.impl.StartCommand;
import me.x150.commands.impl.WipeCommand;
import picocli.CommandLine;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class CommandRegistry {
    List<Callable<Integer>> commands = new ArrayList<>();

    public CommandRegistry() {
        register(new StartCommand());
        register(new SetupCommand());
        register(new WipeCommand());
    }

    void register(Callable<Integer> cmd) {
        commands.add(cmd);
    }

    public String[] getCommandNames() {
        List<String> n = new ArrayList<>();
        for (Callable<Integer> command : commands) {
            for (Annotation declaredAnnotation : command.getClass().getDeclaredAnnotations()) {
                if (declaredAnnotation instanceof CommandLine.Command cmd) {
                    n.add(cmd.name());
                }
            }
        }
        return n.toArray(String[]::new);
    }

    public Callable<Integer> getByName(String name) {
        for (Callable<Integer> command : commands) {
            for (Annotation declaredAnnotation : command.getClass().getDeclaredAnnotations()) {
                if (declaredAnnotation instanceof CommandLine.Command cmd) {
                    if (cmd.name().equalsIgnoreCase(name)) {
                        return command;
                    }
                }
            }
        }
        return null;
    }
}
