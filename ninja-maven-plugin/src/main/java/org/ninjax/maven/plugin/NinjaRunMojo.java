package org.ninjax.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Runs conf.Application and restarts it when target/ changes.
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class NinjaRunMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Component
    private MavenProject project;

    /**
     * The Maven session.
     */
    @Component
    private MavenSession session;

    /**
     * Main class to run.
     */
    @Parameter(defaultValue = "conf.Application", property = "ninja.mainClass")
    private String mainClass;

    /**
     * Directory to watch for changes.
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File targetDirectory;

    /**
     * Extra JVM arguments (optional).
     */
    @Parameter(property = "ninja.jvmArgs")
    private List<String> jvmArgs;

    /**
     * Whether to log more verbosely.
     */
    @Parameter(property = "ninja.verbose", defaultValue = "false")
    private boolean verbose;

    private volatile Process currentProcess;
    private volatile boolean keepRunning = true;

    @Override
    public void execute() {
        if (project == null) {
            getLog().error("No project found – are you running inside a Maven project?");
            return;
        }

        if (targetDirectory == null || !targetDirectory.exists()) {
            getLog().info("Target directory does not exist yet. Creating: " + targetDirectory);
            if (!targetDirectory.mkdirs()) {
                getLog().warn("Could not create target directory, watch may fail.");
            }
        }

        try {
            runWithRestartOnChange();
        } catch (Exception e) {
            getLog().error("Error while running conf.Application with hot-reload", e);
        } finally {
            stopCurrentProcess();
        }
    }

    private void runWithRestartOnChange() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopCurrentProcess));

        // Start app initially
        startApplication();

        // Start watch loop (blocking)
        watchForChangesAndRestart();
    }

    private void startApplication() throws IOException, DependencyResolutionRequiredException {
        stopCurrentProcess();

        List<String> cmd = new ArrayList<>();

        // Use same java as running Maven, if possible
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        cmd.add(javaBin);

        if (jvmArgs != null) {
            cmd.addAll(jvmArgs);
        }

        // Build classpath from the project
        String classpath = buildClasspath();

        getLog().info("----- Ninja run classpath -----");
        for (String entry : classpath.split(File.pathSeparator)) {
            getLog().info(entry);
        }
        getLog().info("----- end classpath -----");

        cmd.add("-cp");
        cmd.add(classpath);

        cmd.add(mainClass);

        if (verbose) {
            getLog().info("Starting application with command: " + cmd);
        } else {
            getLog().info("Starting application: " + mainClass);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(project.getBasedir());
        pb.inheritIO(); // Attach to Maven's stdout/stderr

        currentProcess = pb.start();
    }

    private String buildClasspath() throws DependencyResolutionRequiredException {
        @SuppressWarnings("unchecked")
        List<String> elements = project.getRuntimeClasspathElements();
        String sep = File.pathSeparator;
        StringBuilder cp = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            cp.append(elements.get(i));
            if (i < elements.size() - 1) {
                cp.append(sep);
            }
        }
        return cp.toString();
    }

    private void watchForChangesAndRestart() throws Exception {
        Path root = targetDirectory.toPath();
        getLog().info("Watching directory tree for changes: " + root.toAbsolutePath());

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Map<WatchKey, Path> keyToDir = new HashMap<>();

            // Register a directory and all its subdirectories
            registerAll(root, watchService, keyToDir);

            while (keepRunning) {
                WatchKey key = watchService.take(); // blocks
                Path dir = keyToDir.get(key);
                if (dir == null) {
                    getLog().warn("WatchKey not recognized!");
                    if (!key.reset()) {
                        break;
                    }
                    continue;
                }

                boolean changed = false;

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path name = ev.context();
                    Path child = dir.resolve(name);
                    changed = true;

                    if (verbose) {
                        getLog().info("Detected change: " + child + " (" + kind.name() + ")");
                    }

                    // If a directory is created, register it and its subdirectories
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {

                        if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                            registerAll(child, watchService, keyToDir);
                        }

                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    keyToDir.remove(key);
                    if (keyToDir.isEmpty()) {
                        getLog().warn("No more directories to watch. Stopping watch loop.");
                        break;
                    }
                }

                if (changed) {
                    getLog().info("Changes detected under target/, restarting application...");
                    startApplication();
                }
            }
        }
    }

    private void registerAll(Path start, WatchService watchService,
            Map<WatchKey, Path> keyToDir) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                );
                keyToDir.put(key, dir);
                if (verbose) {
                    getLog().debug("Watching: " + dir.toAbsolutePath());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private synchronized void stopCurrentProcess() {
        if (currentProcess != null && currentProcess.isAlive()) {
            getLog().info("Stopping running application...");
            currentProcess.destroy();
            try {
                if (!currentProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    getLog().warn("Application did not exit gracefully, forcing termination.");
                    currentProcess.destroyForcibly();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                currentProcess.destroyForcibly();
            }
        }
        currentProcess = null;
    }
}
