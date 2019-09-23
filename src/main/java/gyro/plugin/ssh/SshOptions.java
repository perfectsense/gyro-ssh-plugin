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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroInstance;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableInternals;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshOptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshOptions.class);

    private static final Table SSH_TABLE = new Table()
        .addColumn("#", 3)
        .addColumn("Location", 10)
        .addColumn("Name", 54)
        .addColumn("Hostname", 55);

    @Option(name = {"-u", "--user"}, description = "User to log in as.")
    public String user;

    @Option(name = {"-k", "--keyfile"}, description = "Private key to use (i.e. ssh -i ~/.ssh/id_rsa).")
    public String keyfile;

    @Option(name = {"-q", "--quiet"}, description = "Quiet mode.")
    public boolean quiet;

    @Option(name = {"-o", "--options"}, description = "Options pass to ssh -o option.")
    public String options;

    @Option(name = { "-j", "--jumphost" }, description = "Jump through jump host.")
    public boolean useJumpHost;

    private List<GyroInstance> jumpHosts = new ArrayList<>();
    private List<GyroInstance> instances = new ArrayList<>();

    public List<GyroInstance> getJumpHosts() {
        return jumpHosts;
    }

    public void setJumpHosts(List<GyroInstance> jumpHosts) {
        this.jumpHosts = jumpHosts;
    }

    public List<GyroInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<GyroInstance> instances) {
        this.instances = instances;
    }

    public List<String> createArgumentsList(GyroInstance instance, String... additionalArguments) throws Exception {
        String hostname = instance.getPrivateIpAddress();

        if (!useJumpHost) {
            try {
                InetAddress inet = InetAddress.getByName(hostname);
                if (!hasService(inet, 22)) {
                    hostname = instance.getPublicIpAddress();
                }
            } catch (Exception ex) {
                hostname = instance.getPublicIpAddress();
            }
        }

        if (hostname == null) {
            hostname = instance.getPrivateIpAddress();
            useJumpHost = true;
        }

        GyroInstance jumpHost = null;
        if (useJumpHost) {
            jumpHost = pickNearestJumpHost(instance);
        }

        List<String> arguments = new ArrayList<>();

        arguments.add("ssh");

        if (useJumpHost) {
            arguments.add("-o");
            arguments.add("ForwardAgent yes");

            quiet = true;
        }

        if (keyfile != null) {
            arguments.add("-i");
            arguments.add(keyfile);
        }

        if (useJumpHost) {
            String KEY_FILE = "";
            String REMOTE_HOST = jumpHost.getPublicIpAddress();
            if (REMOTE_HOST == null) {
                throw new GyroException("Unable to determine the public IP address of the jump host.");
            }

            if (user != null) {
                REMOTE_HOST = String.format("%s@%s", user, jumpHost.getPublicIpAddress());
            }

            if (keyfile != null) {
                KEY_FILE = "-i " + keyfile;
            }

            arguments.add("-o");
            arguments.add("ProxyCommand ssh {KEY_FILE} -W %h:%p {REMOTE_HOST}".
                    replace("{REMOTE_HOST}", REMOTE_HOST).
                    replace("{KEY_FILE}", KEY_FILE));

            arguments.add("-o");
            arguments.add("StrictHostKeychecking=no");
        }

        if (quiet) {
            arguments.add("-q");
        }

        if (user != null) {
            arguments.add(String.format("%s@%s", user, hostname));

        } else {
            arguments.add(hostname);
        }

        if (options != null) {
            for (String option : Arrays.asList(options.split(","))) {
                arguments.add("-o");
                arguments.add(option);
            }
        }

        if (additionalArguments != null) {
            Collections.addAll(arguments, additionalArguments);
        }

        return arguments;
    }

    public ProcessBuilder createProcessBuilder(GyroInstance instance, String... additionalArguments) throws Exception {
        return new ProcessBuilder(createArgumentsList(instance, additionalArguments));
    }

    public static boolean hasService(InetAddress host, int port) {
        Socket sock = new Socket();

        try {
            sock.setSoTimeout(5000);
            sock.connect(new InetSocketAddress(host, port), 1000);
            if (sock.isConnected()) {
                byte[] buffer = new byte[3];
                if (sock.getInputStream().read(buffer) != 3) {
                    return false;
                }

                if (new String(buffer).equalsIgnoreCase("ssh")) {
                    return true;
                }
            }

        } catch (Exception ex) {
            return false;

        } finally {
            try {
                sock.close();
            } catch (IOException ioe) {
                // Ignore
            }
        }

        return false;
    }

    public GyroInstance pickInstance(List<GyroInstance> instances) throws IOException {
        SSH_TABLE.writeHeader(GyroCore.ui());

        int index = 0;

        for (GyroInstance instance : instances) {
            ++ index;

            SSH_TABLE.writeRow(
                GyroCore.ui(),
                index,
                instance.getLocation(),
                reduceString(instance.getName(), 50),
                !ObjectUtils.isBlank(instance.getHostname()) ? instance.getHostname() : instance.getPrivateIpAddress());
        }

        SSH_TABLE.writeFooter(GyroCore.ui());

        int pick = ObjectUtils.to(int.class, GyroCore.ui().readText("\nMore than one instance matched your criteria, pick one to log into: "));

        if (pick > instances.size() || pick <= 0) {
            throw new GyroException(String.format("Must pick a number between 1 and %d!", instances.size()));
        }

        return instances.get(pick - 1);
    }

    public GyroInstance randomJumpHost() {
        return getJumpHosts().stream().findFirst()
            .orElseThrow(() -> new GyroException("Unable to find a jump host."));
    }

    public GyroInstance pickNearestJumpHost(GyroInstance gyroInstance) throws Exception {
        return getJumpHosts()
            .stream()
            .filter(o -> o.getLocation().equals(gyroInstance.getLocation()))
            .findFirst()
            .orElse(randomJumpHost());
    }

    private static String reduceString(String message, int max) {
        if (message.length() <= max + 3) {
            return message;
        }

        int overage = message.length() - max;
        int start = overage / 2;
        int end = start + overage;

        return String.format("%s .. %s",
            message.substring(0, start),
            message.substring(end, message.length() - 1));
    }

}
