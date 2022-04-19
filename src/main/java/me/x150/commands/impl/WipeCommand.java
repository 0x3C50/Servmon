package me.x150.commands.impl;

import me.x150.Logger;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "wipe", mixinStandardHelpOptions = true, description = "Wipes a server's slate clean, deletes logs, crash reports and caches")
public class WipeCommand implements Callable<Integer> {
    String[] pathsToDelete = new String[] {
        "cache", "libraries", "logs", "versions", "usercache.json"
    };

    void delete(File f) {
        Logger.info("Deleting "+f.getPath());
        f.delete();
    }

    @Override
    public Integer call() throws Exception {
        File pwd = new File(".");
        for (String s : pathsToDelete) {
            File f = new File(pwd, s);
            if (f.exists()) {
                if (f.isFile()) delete(f);
                else {
                    Files.walkFileTree(f.toPath(), new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            delete(file.toFile());
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            delete(dir.toFile());
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }
        }
        return 0;
    }
}
