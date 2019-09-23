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

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeReference;
import gyro.core.GyroInstance;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.RootScope;
import gyro.lang.ast.block.DirectiveNode;

import java.util.List;

@Type("jump-host")
public class JumpHostDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        validateArguments(node, 0, 0);
        scope.getSettings(JumpHostSettings.class).setRegions(
            ObjectUtils.to(
                new TypeReference<List<String>>() {},
                evaluateBody(scope, node).get("regions")));

        scope.getSettings(JumpHostSettings.class).setJumpHosts(
            ObjectUtils.to(
                new TypeReference<List<GyroInstance>>() {},
                evaluateBody(scope, node).get("jump-hosts")));
    }
}
