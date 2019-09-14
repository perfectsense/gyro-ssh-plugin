package gyro.plugin.ssh;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gyro.core.GyroException;
import gyro.core.GyroInstance;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshOptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshOptions.class);

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

    public static List<String> createArgumentsList(SshOptions sshOptions, GyroInstance instance, GyroInstance jumpHost, String... additionalArguments) throws Exception {
        String hostname = instance.getPrivateIpAddress();

        if (sshOptions == null || !sshOptions.useJumpHost) {
            try {
                InetAddress inet = InetAddress.getByName(hostname);
                if (!hasService(inet, 22)) {
                    hostname = instance.getPublicIpAddress();
                }
            } catch (Exception ex) {
                hostname = instance.getPublicIpAddress();
            }
        }

        if (sshOptions != null && sshOptions.useJumpHost && jumpHost == null) {
            throw new GyroException("A jump host is required to use -j. No jump host could be found.");
        }

        if (hostname == null) {
            throw new GyroException(String.format(
                "Unable to find a public IP for instance '%s'. Make sure you are on the VPN or specify -j to use a jump host.",
                instance.getName()));
        }

        List<String> arguments = new ArrayList<>();

        arguments.add("ssh");

        if (sshOptions != null && sshOptions.useJumpHost) {
            arguments.add("-o");
            arguments.add("ForwardAgent yes");

            sshOptions.quiet = true;
        }

        if (sshOptions != null && sshOptions.keyfile != null) {
            arguments.add("-i");
            arguments.add(sshOptions.keyfile);
        }

        if (sshOptions != null && sshOptions.useJumpHost) {
            String KEY_FILE = "";
            String REMOTE_HOST = jumpHost.getPublicIpAddress();
            if (REMOTE_HOST == null) {
                throw new GyroException("Unable to determine the public IP address of the jump host.");
            }

            if (sshOptions.user != null) {
                REMOTE_HOST = String.format("%s@%s", sshOptions.user, jumpHost.getPublicIpAddress());
            }

            if (sshOptions.keyfile != null) {
                KEY_FILE = "-i " + sshOptions.keyfile;
            }

            arguments.add("-o");
            arguments.add("ProxyCommand ssh {KEY_FILE} -W %h:%p {REMOTE_HOST}".
                    replace("{REMOTE_HOST}", REMOTE_HOST).
                    replace("{KEY_FILE}", KEY_FILE));

            arguments.add("-o");
            arguments.add("StrictHostKeychecking=no");
        }

        if (sshOptions != null && sshOptions.quiet) {
            arguments.add("-q");
        }

        if (sshOptions != null && sshOptions.user != null) {
            arguments.add(String.format("%s@%s", sshOptions.user, hostname));

        } else {
            arguments.add(hostname);
        }

        if (sshOptions != null && sshOptions.options != null) {
            for (String option : Arrays.asList(sshOptions.options.split(","))) {
                arguments.add("-o");
                arguments.add(option);
            }
        }

        if (additionalArguments != null) {
            Collections.addAll(arguments, additionalArguments);
        }

        return arguments;
    }

    public static ProcessBuilder createProcessBuilder(SshOptions sshOptions, GyroInstance instance, GyroInstance jumpHost, String... additionalArguments) throws Exception {
        return new ProcessBuilder(createArgumentsList(sshOptions, instance, jumpHost, additionalArguments));
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
}
