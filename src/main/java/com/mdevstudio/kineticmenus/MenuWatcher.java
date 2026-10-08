package com.mdevstudio.kineticmenus;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;

/**
 * Watches the menus folder and reports saved changes. Editors often write a file in several steps, so events
 * are collected until the folder has been quiet for a moment and reported once.
 */
final class MenuWatcher {

    private static final long QUIET_MILLIS = 400;

    private final Path folder;
    private final Logger logger;
    private final Runnable onChange;
    private WatchService service;
    private Thread thread;

    MenuWatcher(Path folder, Logger logger, Runnable onChange) {
        this.folder = folder;
        this.logger = logger;
        this.onChange = onChange;
    }

    void start() {
        try {
            service = FileSystems.getDefault().newWatchService();
            register();
        } catch (IOException e) {
            logger.warn("Cannot watch {} for changes, use /kmenu reload instead: {}", folder, e.getMessage());
            return;
        }
        thread = Thread.ofPlatform().daemon().name("KineticMenus file watcher").start(this::watch);
    }

    void stop() {
        if (service == null) {
            return;
        }
        try {
            service.close();
        } catch (IOException e) {
            logger.warn("Could not stop watching {}: {}", folder, e.getMessage());
        }
        thread.interrupt();
    }

    private void watch() {
        try {
            while (true) {
                WatchKey first = service.take();
                boolean menus = drain(first);
                WatchKey next;
                while ((next = service.poll(QUIET_MILLIS, TimeUnit.MILLISECONDS)) != null) {
                    menus |= drain(next);
                }
                if (menus) {
                    // New subfolders are only seen after they are registered too.
                    register();
                    onChange.run();
                }
            }
        } catch (InterruptedException | ClosedWatchServiceException e) {
            // stop() was called: the plugin is shutting down or auto-reload was turned off
        } catch (IOException e) {
            logger.warn("Stopped watching {} for changes: {}", folder, e.getMessage());
        }
    }

    private boolean drain(WatchKey key) {
        boolean menus = key.pollEvents().stream().anyMatch(event -> event.context() instanceof Path path
                && (path.toString().endsWith(".yml") || !path.toString().contains(".")));
        key.reset();
        return menus;
    }

    private void register() throws IOException {
        try (Stream<Path> folders = Files.walk(folder)) {
            for (Path path : folders.filter(Files::isDirectory).toList()) {
                path.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
            }
        }
    }
}
