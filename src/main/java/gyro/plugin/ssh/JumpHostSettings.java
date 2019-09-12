package gyro.plugin.ssh;

import gyro.core.GyroInstance;
import gyro.core.scope.Settings;

import java.util.ArrayList;
import java.util.List;

public class JumpHostSettings extends Settings {

    private List<GyroInstance> jumpHosts;

    private List<String> regions;

    public List<GyroInstance> getJumpHosts() {
        return jumpHosts == null ? new ArrayList<>() : jumpHosts;
    }

    public void setJumpHosts(List<GyroInstance> jumpHosts) {
        this.jumpHosts = jumpHosts;
    }

    public List<String> getRegions() {
        return regions == null ? new ArrayList<>() : regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }
}
