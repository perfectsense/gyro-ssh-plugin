package gyro.plugin.ssh;

import gyro.commands.AbstractConfigCommand;
import gyro.core.BeamCore;
import gyro.core.BeamInstance;
import gyro.core.BeamInstances;
import gyro.lang.Resource;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.State;
import io.airlift.airline.Option;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractInstanceCommand extends AbstractConfigCommand {

    private List<BeamInstance> instances = new ArrayList<>();

    @Option(name = { "-r", "--refresh" }, description = "Refresh instance data from the cloud provider.")
    public boolean refresh;

    public boolean refresh() {
        return refresh;
    }

    public abstract void doExecute(List<BeamInstance> instances) throws Exception;

    @Override
    protected void doExecute(RootScope current, RootScope pending, State state) throws Exception {

        BeamCore.ui().write("\n");

        for (Resource resource : current.findAllResources()) {
            if (BeamInstance.class.isAssignableFrom(resource.getClass())) {
                instances.add((BeamInstance) resource);

                if (refresh()) {
                    BeamCore.ui().write("@|bold,blue Refreshing|@: @|yellow %s|@ -> %s...", resource.resourceType(), resource.resourceIdentifier());
                    resource.refresh();

                    pending.getBackend().save(current);
                    BeamCore.ui().write("\n");
                }
            } else if (BeamInstances.class.isAssignableFrom(resource.getClass())) {
                instances.addAll(((BeamInstances) resource).getInstances());
            }
        }

        if (instances.isEmpty()) {
            BeamCore.ui().write("@|red No instances found.|@\n");
            return;
        }

        doExecute(instances);
    }

}
