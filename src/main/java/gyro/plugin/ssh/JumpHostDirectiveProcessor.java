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
