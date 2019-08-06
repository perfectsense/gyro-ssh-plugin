package gyro.plugin.ssh;

import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

public class JumpHostDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public String getName() {
        return "jump-host";
    }

    @Override
    public void process(DiffableScope scope, DirectiveNode node) throws Exception {
        Boolean isJumpHost = getArgument(scope, node, Boolean.class, 0);
        scope.getSettings(JumpHostSettings.class)
                .setJumpHost(isJumpHost == null
                        ? false
                        : isJumpHost);
        scope.getStateNodes().add(node);
    }
}
