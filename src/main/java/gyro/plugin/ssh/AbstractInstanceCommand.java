/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.plugin.ssh;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public abstract class AbstractInstanceCommand extends AbstractCommand {

    private List<GyroInstance> instances = new ArrayList<>();

    @Option(names = { "-r", "--refresh" }, description = "Refresh instance data from the cloud provider.")
    public boolean refresh;

    @Parameters(description = "gyro configuration files to look for instances in.")
    private List<String> files;

    protected RootScope current;

    public boolean refresh() {
        return refresh;
    }

    public abstract void doExecute(List<GyroInstance> instances) throws Exception;

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

        for (Resource resource : current.findSortedResources()) {
            boolean scoped = fileScopes.contains(DiffableInternals.getScope(resource).getFileScope());

            if (GyroInstance.class.isAssignableFrom(resource.getClass())) {
                if (scoped) {
                    instances.add((GyroInstance) resource);
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
                if (scoped) {
                    instances.addAll(((GyroInstances) resource).getInstances());
                }
            }
        }

        if (instances.isEmpty()) {
            GyroCore.ui().write("@|red No instances found.|@\n");
            return;
        }

        doExecute(instances);
    }

}
