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
    public void doExecute(List<GyroInstance> instances, List<GyroInstance> scopedInstances) throws Exception {
        if (sshOptions == null) {
            sshOptions = new SshOptions();
        }

        sshOptions.setInstances(instances);
        sshOptions.setScopedInstances(scopedInstances);

        if (command != null) {
            for (GyroInstance instance : instances) {
                GyroCore.ui().write("Executing @|green %s|@ on @|yellow %s|@\n", command, instance.getHostname());

                int exitCode = sshOptions.createProcessBuilder(instance, command)
                    .inheritIO()
                    .start()
                    .waitFor();

                if (exitCode != 0 && !force) {
                    GyroCore.ui().write("@|red Command failed!|@\n");
                    return;
                }
            }
        } else if (useTmux) {
            String tmuxScript = "#!/bin/sh\n";
            tmuxScript += "SESSION=`tmux new-session -d -P`\n";

            for (GyroInstance instance : instances) {
                List<String> arguments = sshOptions.createArgumentsList(instance);

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
        } else if (scopedInstances.size() == 1) {
            GyroInstance instance = scopedInstances.get(0);
            sshOptions.createProcessBuilder(instance)
                .inheritIO()
                .start()
                .waitFor();

        } else {
            GyroInstance instance = sshOptions.pickInstance(scopedInstances);

            sshOptions.createProcessBuilder(instance)
                .inheritIO()
                .start()
                .waitFor();
        }

    }

}
