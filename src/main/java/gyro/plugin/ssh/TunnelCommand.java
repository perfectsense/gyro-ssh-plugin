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

import javax.inject.Inject;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import gyro.core.GyroCore;
import gyro.core.GyroException;
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
        sshOptions.setJumpHosts(current.getSettings(JumpHostSettings.class).getJumpHosts());
        sshOptions.setInstances(instances);

        if (instances.size() > 1) {
            instance = sshOptions.pickInstance(instances);
        } else if (instances.size() != 0) {
            instance = instances.get(0);
        }

        GyroInstance jumpHost = sshOptions.pickNearestJumpHost(instance);
        ProcessBuilder processBuilder = tunnel(instance, jumpHost);

        GyroCore.ui().write("Tunneling local port %s to %s on %s\n\n", localPort, remotePort, instance.getGyroInstanceId());
        GyroCore.ui().write("http://localhost:%s\n", localPort);

        Process process = processBuilder.inheritIO().start();

        if (!noBrowser) {
            Desktop.getDesktop().browse(new URI("http://localhost:" + localPort));
        }

        process.waitFor();
    }

    private ProcessBuilder tunnel(GyroInstance instance, GyroInstance jumpHost) {
        if (jumpHost == null) {
            throw new GyroException("No jump host found.");
        }

        List<String> arguments = new ArrayList<>();

        String remoteHost = instance.getGyroInstancePrivateIpAddress();
        String jumpHostIp = jumpHost.getGyroInstancePublicIpAddress();

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
            jumpHostIp = String.format("%s@%s", sshOptions.user, jumpHost.getGyroInstancePublicIpAddress());
        }
        arguments.add(jumpHostIp);

        return new ProcessBuilder(arguments);
    }
}
