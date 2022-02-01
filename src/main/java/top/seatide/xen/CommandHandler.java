package top.seatide.xen;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.seatide.xen.Utils.Files;
import top.seatide.xen.Utils.LogLevel;
import top.seatide.xen.Utils.LogUtil;

import java.util.*;
import java.util.stream.Collectors;

public class CommandHandler implements TabExecutor {
    public final static String[] ARGS = { "load", "unload", "create", "tp", "list" };

    public List<String> getResult(String arg, List<String> commands, CommandSender sender) {
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(arg, commands, result);
        Collections.sort(result);
        return result;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        var result = new ArrayList<String>();
        if (command.getName().equalsIgnoreCase("xen")) {
            if (args.length == 1) {
                var candy = new ArrayList<String>();
                for (var m : ARGS) {
                    if (sender.hasPermission("xen.command." + m)) {
                        candy.add(m);
                    }
                }
                return getResult(args[0], candy, sender);
            }
        }
        return result;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("xen")) {
            if (!Arrays.asList(ARGS).contains(label)) {
                LogUtil.send(sender, LogLevel.ERROR, "无效操作。");
                LogUtil.send(sender, LogLevel.INFO, "输入 &e/xen help &r查看帮助。");
                return true;
            }
            var allowDimension = Files.cfg.getBoolean("allow-dimension");
            switch (label) {
                /* xen [load|create] <world-name> [type: normal|flat|large-biomes|amplified] [env: normal|nether|the-end] */
                case "load":
                case "create": {
                    if (args.length == 0) {
                        LogUtil.send(sender, LogLevel.WARNING, "请提供世界名称。");
                        return true;
                    }
                    var name = args[0];
                    if (Bukkit.getWorld(name) != null) {
                        LogUtil.send(sender, LogLevel.WARNING, "此世界已存在且被加载。");
                        return true;
                    }
                    WorldType type;
                    World.Environment env;
                    try {
                        type = args.length == 2 ? WorldType.getByName(args[1].toUpperCase()) : WorldType.NORMAL;
                        env = args.length == 3 ? getEnvironmentByName(args[2]) : World.Environment.NORMAL;
                        assert type != null;
                    } catch (IllegalArgumentException | AssertionError e) {
                        LogUtil.send(sender, LogLevel.ERROR, "无效的世界类型或者环境类型。");
                        LogUtil.send(sender, LogLevel.INFO, "输入 &e/xen help worldtype&8|&eenvtype &r查看可用的类型。");
                        return true;
                    }
                    var world = new WorldCreator(name).type(type).environment(env).createWorld();
                    if (world == null) {
                        LogUtil.send(sender, LogLevel.ERROR, label.equals("load") ? "加载" : "创建" + "失败，无法获取世界。");
                        return true;
                    }
                    Files.saveWorld(name, type.name(), env.name());
                    LogUtil.send(sender, LogLevel.SUCCESS, "成功" + (label.equals("load") ? "加载" : "创建") + "世界 &a" + name + "&r。");
                    return true;
                }


                /* xen unload <world-name> */
                case "unload": {
                    if (args.length == 0) {
                        LogUtil.send(sender, LogLevel.ERROR, "请提供世界名称。");
                        return true;
                    }

                    var name = args[0];
                    if (Bukkit.getWorld(name) == null) {
                        LogUtil.send(sender, LogLevel.WARNING, "此世界还没有被加载。");
                        return true;
                    }
                    if (Bukkit.unloadWorld(name, true)) {
                        LogUtil.send(sender, LogLevel.SUCCESS, "成功卸载世界 &a" + name + "&r。");
                    } else {
                        LogUtil.send(sender, LogLevel.ERROR, "无法卸载世界 &c" + name + "&r。");
                    }
                    return true;
                }


                /* xen tp <world-name> */
                case "tp": {
                    if (!(sender instanceof Player)) {
                        LogUtil.send(sender, LogLevel.ERROR, "此指令只能由玩家执行。");
                        return true;
                    }

                    if (args.length == 0) {
                        LogUtil.send(sender, LogLevel.ERROR, "请提供世界名称。");
                        return true;
                    }

                    var name = args[0];
                    if (!allowDimension && List.of("DIM1", "DIM-1").contains(name)) {
                        LogUtil.send(sender, LogLevel.ERROR, "不能进行维度传送。");
                        return true;
                    }
                    var world = Bukkit.getWorld(name);
                    if (world == null) {
                        LogUtil.send(sender, LogLevel.WARNING, "世界不存在。");
                        return true;
                    }
                    if (!((Player) sender).teleport(world.getSpawnLocation())) {
                        LogUtil.send(sender, LogLevel.ERROR, "传送失败。");
                    }
                    return true;
                }

                /* xen list */
                case "list": {
                    var worlds = Bukkit.getWorlds().stream()
                            .map(World::getName)
                            .distinct()
                            .filter(s -> allowDimension || !List.of("DIM1", "DIM-1").contains(s))
                            .collect(Collectors.toList());
                    LogUtil.send(sender, LogLevel.INFO, "当前可传送世界（共 &a" + worlds.size() + "&r 个）：");
                    LogUtil.send(sender, LogLevel.INFO, "&a" + StringUtils.join(worlds, ", "));
                    return true;
                }

                /* xen reload */
                case "reload": {
                    Files.reload();
                    LogUtil.send(sender, LogLevel.SUCCESS, "重载成功。");
                }
            }
        }
        return true;
    }

    private World.Environment getEnvironmentByName(String name) {
        switch (name) {
            case "normal": return World.Environment.NORMAL;
            case "the-end": return World.Environment.THE_END;
            case "nether": return World.Environment.NETHER;
            default: throw new IllegalArgumentException();
        }
    }
}
