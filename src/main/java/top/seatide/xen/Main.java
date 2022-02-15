package top.seatide.xen;

import org.bukkit.plugin.java.JavaPlugin;
import top.seatide.xen.Utils.Files;
import top.seatide.xen.Utils.LogUtil;
import top.seatide.xen.Utils.Worlds;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        LogUtil.init();
        Files.init(this);
        Worlds.init(this);
        Worlds.loadAll();
        var command = this.getServer().getPluginCommand("xen");
        if (command == null) {
            LogUtil.error("无法找到插件指令。");
        } else {
            command.setExecutor(new CommandHandler());
            command.setTabCompleter(new CommandHandler());
        }
        LogUtil.success("Xen 已启用。");
    }

    @Override
    public void onDisable() {
        Worlds.saveAllRecordedWorld();
        LogUtil.success("Xen 已停用。");
    }
}
