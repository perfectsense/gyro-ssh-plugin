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
    public void doExecute(List<GyroInstance> instances) {
        LIST_TABLE.writeHeader(GyroCore.ui());

        for (GyroInstance instance : instances) {
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
