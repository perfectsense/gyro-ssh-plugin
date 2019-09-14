package gyro.plugin.ssh;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroInstance;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableInternals;
import gyro.core.scope.DiffableScope;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

@Command(name = "ssh", description = "SSH to a running instance.")
public class SshCommand extends AbstractInstanceCommand {

    private static final Table SSH_TABLE = new Table()
        .addColumn("#", 3)
        .addColumn("Location", 10)
        .addColumn("Name", 54)
        .addColumn("Hostname", 55);

    @Option(name = { "-e", "--execute" }, description = "Command to execute on host(s).")
    public String command;

    @Option(name = { "-c", "--continue" }, description = "Ignore exit code and continue running -e command to run on all hosts.")
    public boolean force;

    @Option(name = { "--tmux" }, description = "Open a tmux session with each host.")
    public boolean useTmux;

    @Inject
    public SshOptions sshOptions;

    public String command() {
        return command;
    }

    public boolean force() {
        return force;
    }

    public boolean useTmux() {
        return useTmux;
    }

    @Override
    public void doExecute(List<GyroInstance> instances) throws Exception {
        if (command != null) {
            for (GyroInstance instance : instances) {
                GyroCore.ui().write("Executing @|green %s|@ on @|yellow %s|@\n", command, instance.getHostname());

                int exitCode = SshOptions.createProcessBuilder(sshOptions, instance, pickNearestJumpHost(instances, instance, sshOptions), command).inheritIO().start().waitFor();

                if (exitCode != 0 && !force) {
                    GyroCore.ui().write("@|red Command failed!|@\n");
                    return;
                }
            }
        } else if (useTmux) {
            String tmuxScript = "#!/bin/sh\n";
            tmuxScript += "SESSION=`tmux new-session -d -P`\n";

            for (GyroInstance instance : instances) {
                List<String> arguments = SshOptions.createArgumentsList(sshOptions, instance, pickNearestJumpHost(instances, instance, sshOptions));

                String sshCommand = "";
                for (String arg : arguments) {
                    if (arg.contains(" ")) {
                        sshCommand += "\'" + arg + "\'";
                    } else {
                        sshCommand += arg;
                    }

                    sshCommand += " ";
                }

                tmuxScript += "tmux new-window -t ${SESSION}" + " -n " + instance.getInstanceId() + " -- " + sshCommand + "\n";
            }

            tmuxScript += "tmux kill-window -t ${SESSION}1\n";
            tmuxScript += "tmux move-window -rt ${SESSION}\n";
            tmuxScript += "tmux attach-session -t ${SESSION}\n";

            File temp = File.createTempFile("tmux", "beam");
            temp.setExecutable(true);
            temp.deleteOnExit();

            Writer out = new FileWriter(temp);
            out.write(tmuxScript);
            out.close();

            new ProcessBuilder(temp.toString()).inheritIO().start().waitFor();
        } else if (instances.size() == 1) {
            GyroInstance instance = instances.get(0);
            GyroInstance jumpHost = sshOptions != null && sshOptions.useJumpHost ? pickNearestJumpHost(instances, instance, sshOptions) : null;

            SshOptions.createProcessBuilder(sshOptions, instance, jumpHost)
                .inheritIO()
                .start()
                .waitFor();

        } else {
            GyroInstance instance = pickInstance(instances);
            GyroInstance jumpHost = sshOptions != null && sshOptions.useJumpHost ? pickNearestJumpHost(instances, instance, sshOptions) : null;

            SshOptions.createProcessBuilder(sshOptions, instance, jumpHost)
                .inheritIO()
                .start()
                .waitFor();
        }

    }

    public static GyroInstance pickInstance(List<GyroInstance> instances) throws IOException {
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

    public static GyroInstance pickNearestJumpHost(List<GyroInstance> allInstances, GyroInstance gyroInstance, SshOptions options) throws Exception {
        GyroInstance jumpHost;
        List<GyroInstance> jumpHosts = allInstances.stream().filter(SshCommand::isJumpHost).collect(Collectors.toList());
        jumpHost = jumpHosts.stream().filter(o -> o.getLocation().equals(gyroInstance.getLocation())).findFirst().orElse(null);
        if (jumpHost == null && !jumpHosts.isEmpty()) {
            jumpHost = jumpHosts.get(0);
        }

        return jumpHost;
    }

    private static boolean isJumpHost(Object resource) {
        if (resource instanceof GyroInstance) {
            return DiffableInternals.getScope((Diffable) resource)
                .getRootScope()
                .getSettings(JumpHostSettings.class)
                .getJumpHosts()
                .contains(resource);
        }
        return false;
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
