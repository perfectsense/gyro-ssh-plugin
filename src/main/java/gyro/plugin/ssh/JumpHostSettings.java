package gyro.plugin.ssh;

import gyro.core.scope.Settings;

public class JumpHostSettings extends Settings {

    private boolean jumpHost;

    public boolean isJumpHost() {
        return jumpHost;
    }

    public void setJumpHost(boolean jumpHost) {
        this.jumpHost = jumpHost;
    }
}
