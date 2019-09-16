package gyro.plugin.ssh;

import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroInstance;
import gyro.core.GyroInstances;
import gyro.core.LocalFileBackend;
import gyro.core.command.AbstractCommand;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.FileScope;
import gyro.core.scope.RootScope;
import io.airlift.airline.Arguments;
import io.airlift.airline.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractInstanceCommand extends AbstractCommand {

    private List<GyroInstance> instances = new ArrayList<>();
    private List<GyroInstance> scopedInstances = new ArrayList<>();

    @Option(name = { "-r", "--refresh" }, description = "Refresh instance data from the cloud provider.")
    public boolean refresh;

    @Arguments
    private List<String> files;

    protected RootScope current;

    public boolean refresh() {
        return refresh;
    }

    public abstract void doExecute(List<GyroInstance> instances, List<GyroInstance> scopedInstances) throws Exception;

    @Override
    protected void doExecute() throws Exception {
        Path rootDir = GyroCore.getRootDirectory();

        if (rootDir == null) {
            throw new GyroException("Not a gyro project directory, use 'gyro init <plugins>...' to create one. See 'gyro help init' for detailed usage.");
        }

        Set<String> loadFiles;

        if (files == null) {
            loadFiles = null;

        } else {
            Map<Boolean, Set<String>> p = files.stream()
                .map(f -> f.endsWith(".gyro") ? f : f + ".gyro")
                .map(f -> rootDir.relativize(Paths.get("").toAbsolutePath().resolve(f)).normalize().toString())
                .collect(Collectors.partitioningBy(
                    f -> Files.exists(rootDir.resolve(f)),
                    Collectors.toCollection(LinkedHashSet::new)));

            Set<String> nonexistent = p.get(Boolean.FALSE);

            if (nonexistent.isEmpty()) {
                loadFiles = p.get(Boolean.TRUE);

            } else {
                throw new GyroException(String.format(
                    "Files not found! %s",
                    nonexistent.stream()
                        .map(f -> String.format("@|bold %s|@", f))
                        .collect(Collectors.joining(", "))));
            }
        }

        current = new RootScope(
            "../../" + GyroCore.INIT_FILE,
            new LocalFileBackend(rootDir.resolve(".gyro/state")),
            null,
            loadFiles);

        current.evaluate();

        GyroCore.ui().write("\n");

        List<FileScope> fileScopes = current.getFileScopes()
            .stream()
            .filter(f -> current.getLoadFiles().contains(f.getFile()))
            .collect(Collectors.toList());

        if (fileScopes.isEmpty()) {
            fileScopes = current.getFileScopes();
        }

        for (Resource resource : current.findResources()) {
            boolean scoped = fileScopes.contains(DiffableInternals.getScope(resource).getFileScope());

            if (GyroInstance.class.isAssignableFrom(resource.getClass())) {
                instances.add((GyroInstance) resource);

                if (scoped) {
                    scopedInstances.add((GyroInstance) resource);
                }

                if (refresh()) {
                    GyroCore.ui().write(
                        "@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...",
                        DiffableType.getInstance(resource.getClass()).getName(),
                        DiffableInternals.getName(resource));

                    resource.refresh();
                    GyroCore.ui().write("\n");
                }
            } else if (GyroInstances.class.isAssignableFrom(resource.getClass())) {
                instances.addAll(((GyroInstances) resource).getInstances());

                if (scoped) {
                    scopedInstances.addAll(((GyroInstances) resource).getInstances());
                }
            }
        }

        if (instances.isEmpty()) {
            GyroCore.ui().write("@|red No instances found.|@\n");
            return;
        }

        doExecute(instances, scopedInstances);
    }

}
