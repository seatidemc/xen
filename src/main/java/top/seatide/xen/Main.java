package top.seatide.xen;

import org.bukkit.plugin.java.JavaPlugin;
import top.seatide.xen.Utils.Files;
import top.seatide.xen.Utils.LogUtil;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        Files.init(this);
        LogUtil.success("Xen 已启用。");
    }

    @Override
    public void onDisable() {
        LogUtil.success("Xen 已停用。");
    }
}
