package top.seatide.xen;

import org.bukkit.plugin.java.JavaPlugin;
import top.seatide.xen.Utils.Files;
import top.seatide.xen.Utils.LogUtil;
import top.seatide.xen.Utils.Worlds;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        Files.init(this);
        Worlds.loadAll();
        LogUtil.success("Xen 已启用。");
    }

    @Override
    public void onDisable() {
        LogUtil.success("Xen 已停用。");
    }
}
