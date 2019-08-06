package gyro.plugin.ssh;

import javax.inject.Inject;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import gyro.core.GyroCore;
import gyro.core.GyroInstance;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

@Command(name = "tunnel", description = "Tunnel to a running instance.")
public class TunnelCommand extends AbstractInstanceCommand {

    private static final Table TUNNEL_TABLE = new Table().
            addColumn("#", 2).
            addColumn("Instance ID", 20).
            addColumn("Environment", 15).
            addColumn("Location", 12).
            addColumn("Layer", 12).
            addColumn("State", 12).
            addColumn("Hostname", 65);

    @Option(name = {"--localPort"}, description = "Local port to listen on.")
    public Integer localPort;

    @Option(name = {"--remotePort"}, description = "Remote port to connect to.")
    public Integer remotePort;

    @Option(name = {"--nobrowser"}, description = "Don't open browser automatically.")
    public boolean noBrowser;

    @Inject
    public SshOptions sshOptions;

    @Override
    public void doExecute(List<GyroInstance> instances) throws Exception {
        GyroInstance instance = null;

        if (sshOptions == null) {
            sshOptions = new SshOptions();
        }
        sshOptions.useJumpHost = true;

        if (instances.size() > 1) {
            instance = SshCommand.pickInstance(instances);
        } else if (instances.size() != 0) {
            instance = instances.get(0);
        }

        GyroInstance jumpHost = SshCommand.pickNearestJumpHost(instances, instance, sshOptions);
        ProcessBuilder processBuilder = tunnel(instance, jumpHost);

        GyroCore.ui().write("Tunneling local port %s to %s on %s\n\n", localPort, remotePort, instance.getInstanceId());
        GyroCore.ui().write("http://localhost:%s\n", localPort);

        Process process = processBuilder.inheritIO().start();

        if (!noBrowser) {
            Desktop.getDesktop().browse(new URI("http://localhost:" + localPort));
        }

        process.waitFor();
    }

    private ProcessBuilder tunnel(GyroInstance instance, GyroInstance jumpHost) {
        List<String> arguments = new ArrayList<>();

        String remoteHost = instance.getPrivateIpAddress();
        String jumpHostIp = jumpHost.getPublicIpAddress();

        if (localPort == null) {
            localPort = 4000;
        }

        if (remotePort == null) {
            remotePort = 8080;
        }

        arguments.add("ssh");
        arguments.add("-nNT");

        if (sshOptions != null && sshOptions.keyfile != null) {
            arguments.add("-i");
            arguments.add(sshOptions.keyfile);
        }

        arguments.add("-L");
        arguments.add(localPort + ":" + remoteHost + ":" + remotePort);

        arguments.add("-o");
        arguments.add("StrictHostKeychecking=no");

        arguments.add("-o");
        arguments.add("ExitOnForwardFailure=yes");

        if (sshOptions != null && sshOptions.user != null) {
            jumpHostIp = String.format("%s@%s", sshOptions.user, jumpHost.getPublicIpAddress());
        }
        arguments.add(jumpHostIp);

        return new ProcessBuilder(arguments);
    }
}
