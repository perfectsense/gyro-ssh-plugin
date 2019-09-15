package gyro.plugin.ssh;

import java.util.List;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroCore;
import gyro.core.GyroInstance;
import io.airlift.airline.Command;

@Command(name = "list", description = "List instances found in provided config file.")
public class ListCommand extends AbstractInstanceCommand {

    private static final Table LIST_TABLE = new Table()
        .addColumn("Instance ID", 20)
        .addColumn("State", 12)
        .addColumn("Launch Date", 20)
        .addColumn("Hostname", 65);

    @Override
    public void doExecute(List<GyroInstance> instances, List<GyroInstance> scopedInstances) {
        LIST_TABLE.writeHeader(GyroCore.ui());

        for (GyroInstance instance : scopedInstances) {
            LIST_TABLE.writeRow(
                GyroCore.ui(),
                instance.getInstanceId(),
                instance.getState(),
                instance.getLaunchDate(),
                getHostname(instance)
            );
        }

        LIST_TABLE.writeFooter(GyroCore.ui());
    }

    public String getHostname(GyroInstance instance) {
        if (!ObjectUtils.isBlank(instance.getHostname())) {
            return instance.getHostname();
        }

        if (!ObjectUtils.isBlank(instance.getPublicIpAddress())) {
            return instance.getPublicIpAddress();
        }

        if (!ObjectUtils.isBlank(instance.getPrivateIpAddress())) {
            return instance.getPrivateIpAddress();
        }

        return "";
    }

}
