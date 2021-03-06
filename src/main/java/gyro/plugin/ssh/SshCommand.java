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

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

import gyro.core.GyroCore;
import gyro.core.GyroInstance;
import gyro.core.command.VersionCommand;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "ssh",
    header = "SSH to a running instance.",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    description = "Connect to instances defined in gyro configuration using ssh. If multiple instances are found "
        + "a list will be presented to chose from will. If only one instance is found it will be connected to "
        + "automatically.",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    usageHelpWidth = 100,
    mixinStandardHelpOptions = true,
    versionProvider = VersionCommand.class
)
public class SshCommand extends AbstractInstanceCommand {

    @Option(names = { "-e", "--execute" }, description = "Command to execute on host(s).")
    public String command;

    @Option(names = { "-c", "--continue" }, description = "Ignore exit code and continue running -e command to run on all hosts.")
    public boolean force;

    @Option(names = { "--tmux" }, description = "Open a tmux session with each host.")
    public boolean useTmux;

    @ArgGroup(exclusive = false)
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
        if (sshOptions == null) {
            sshOptions = new SshOptions();
        }

        sshOptions.setInstances(instances);
        sshOptions.setJumpHosts(current.getSettings(JumpHostSettings.class).getJumpHosts());

        if (command != null) {
            for (GyroInstance instance : instances) {
                GyroCore.ui().write("Executing @|green %s|@ on @|yellow %s|@\n", command, instance.getGyroInstanceHostname());

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

                tmuxScript += "tmux new-window -t ${SESSION}" + " -n " + instance.getGyroInstanceId() + " -- " + sshCommand + "\n";
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
            sshOptions.createProcessBuilder(instance)
                .inheritIO()
                .start()
                .waitFor();

        } else {
            GyroInstance instance = sshOptions.pickInstance(instances);

            sshOptions.createProcessBuilder(instance)
                .inheritIO()
                .start()
                .waitFor();
        }

    }

}
