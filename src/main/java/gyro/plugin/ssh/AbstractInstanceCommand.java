package gyro.plugin.ssh;

import java.util.ArrayList;
import java.util.List;

import gyro.core.GyroCore;
import gyro.core.GyroInstance;
import gyro.core.GyroInstances;
import gyro.core.command.AbstractConfigCommand;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.State;
import io.airlift.airline.Option;

public abstract class AbstractInstanceCommand extends AbstractConfigCommand {

    private List<GyroInstance> instances = new ArrayList<>();

    @Option(name = { "-r", "--refresh" }, description = "Refresh instance data from the cloud provider.")
    public boolean refresh;

    public boolean refresh() {
        return refresh;
    }

    public abstract void doExecute(List<GyroInstance> instances) throws Exception;

    @Override
    protected void doExecute(RootScope current, RootScope pending, State state) throws Exception {

        GyroCore.ui().write("\n");

        for (Resource resource : current.findResources()) {
            if (GyroInstance.class.isAssignableFrom(resource.getClass())) {
                instances.add((GyroInstance) resource);

                if (refresh()) {
                    GyroCore.ui().write(
                        "@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...",
                        DiffableType.getInstance(resource.getClass()).getName(),
                        resource.name());

                    resource.refresh();
                    GyroCore.ui().write("\n");
                }
            } else if (GyroInstances.class.isAssignableFrom(resource.getClass())) {
                instances.addAll(((GyroInstances) resource).getInstances());
            }
        }

        if (instances.isEmpty()) {
            GyroCore.ui().write("@|red No instances found.|@\n");
            return;
        }

        doExecute(instances);
    }

}
