package top.seatide.xen;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
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
import top.seatide.xen.Utils.Worlds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements TabExecutor {
    public final static String[] ARGS = { "load", "unload", "create", "tp", "list", "rule", "mode", "diff", "difficulty", "help", "reloadPlugin" };

    public List<String> getResult(String arg, List<String> commands) {
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(arg, commands, result);
        Collections.sort(result);
        return result;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        var result = new ArrayList<String>();
        if (command.getName().equalsIgnoreCase("xen")) {
            switch (args.length) {
                case 1:
                    var candy = new ArrayList<String>();
                    for (var m : ARGS) {
                        if (sender.hasPermission("xen.command." + m)) {
                            candy.add(m);
                        }
                    }
                    return getResult(args[0], candy);
                case 2:
                    if (args[0].equals("load")|| args[0].equals("create") || args[0].equals("unload") || args[0].equals("tp")) {
                        return Bukkit.getWorlds().stream().map(World::getName).distinct().collect(Collectors.toList());
                    }
                    if (args[0].equals("rule")) return List.of("get", "set");
                    break;
                case 3:
                    if (args[0].equals("load") || args[0].equals("create")) {
                        return List.of("NORMAL", "FLAT", "LARGE_BIOMES", "AMPLIFIED");
                    }
                    if (args[0].equals("rule")) return Bukkit.getWorlds().stream().map(World::getName).distinct().collect(Collectors.toList());
                    break;
                case 4:
                    if (args[0].equals("load") || args[0].equals("create")) {
                        return List.of("NORMAL", "NETHER", "THE_END");
                    }
                    if (args[0].equals("rule")) return Arrays.stream(GameRule.values()).map(GameRule::getName).distinct().collect(Collectors.toList());
                    break;
                case 5:
                    if (args[0].equals("rule")) {
                        var rule = GameRule.getByName(args[3]);
                        if (rule != null) {
                            if (rule.getType() == Boolean.class) {
                                return List.of("true", "false");
                            }
                        }
                    }
                    break;
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("xen")) {
            if (args.length == 0) {
                LogUtil.send(sender, LogLevel.WARNING, "至少提供一个参数。");
                return true;
            }
            if (!Arrays.asList(ARGS).contains(args[0])) {
                LogUtil.send(sender, LogLevel.ERROR, "无效操作。");
                LogUtil.send(sender, LogLevel.INFO, "输入 &e/xen help &r查看帮助。");
                return true;
            }
            var allowDimension = Files.cfg.getBoolean("allow-dimension");
            switch (args[0]) {
                /* xen [load|create] <world-name> [type: normal|flat|large-biomes|amplified] [env: normal|nether|the-end] */
                case "load":
                case "create": {
                    if (args.length == 1) {
                        LogUtil.send(sender, LogLevel.WARNING, "请提供世界名称。");
                        return true;
                    }
                    var name = args[1];
                    if (Bukkit.getWorld(name) != null) {
                        LogUtil.send(sender, LogLevel.WARNING, "此世界已存在且被加载。");
                        return true;
                    }
                    WorldType type;
                    World.Environment env;
                    try {
                        type = args.length == 3 ? WorldType.getByName(args[2].toUpperCase()) : WorldType.NORMAL;
                        env = args.length == 4 ? Worlds.getEnvironmentByName(args[3]) : World.Environment.NORMAL;
                        assert type != null;
                    } catch (IllegalArgumentException | AssertionError e) {
                        LogUtil.send(sender, LogLevel.ERROR, "无效的世界类型或者环境类型。");
                        LogUtil.send(sender, LogLevel.INFO, "输入 &e/xen help worldtype&8|&eenvtype &r查看可用的类型。");
                        return true;
                    }
                    if (Worlds.createOrLoadWorld(name, type, env) == null) {
                        LogUtil.send(sender, LogLevel.ERROR, args[0].equals("load") ? "加载" : "创建" + "失败，无法获取世界。");
                        return true;
                    }
                    Worlds.saveAsRecord(name, type.name(), env.name());
                    LogUtil.send(sender, LogLevel.SUCCESS, "成功" + (args[0].equals("load") ? "加载" : "创建") + "世界 &a" + name + "&r。");
                    break;
                }


                /* xen unload <world-name> */
                case "unload": {
                    if (args.length == 1) {
                        LogUtil.send(sender, LogLevel.ERROR, "请提供世界名称。");
                        return true;
                    }

                    var name = args[1];
                    if (Bukkit.getWorld(name) == null) {
                        LogUtil.send(sender, LogLevel.WARNING, "此世界还没有被加载。");
                        return true;
                    }
                    if (Bukkit.unloadWorld(name, true)) {
                        LogUtil.send(sender, LogLevel.SUCCESS, "成功卸载世界 &a" + name + "&r。");
                    } else {
                        LogUtil.send(sender, LogLevel.ERROR, "无法卸载世界 &c" + name + "&r。");
                        LogUtil.send(sender, LogLevel.ERROR, "可能是由于该世界中仍然有玩家或者其它插件的影响。");
                    }
                    break;
                }


                /* xen tp <world-name> */
                case "tp": {
                    if (!(sender instanceof Player)) {
                        LogUtil.send(sender, LogLevel.ERROR, "此指令只能由玩家执行。");
                        return true;
                    }

                    if (args.length == 1) {
                        LogUtil.send(sender, LogLevel.ERROR, "请提供世界名称。");
                        return true;
                    }

                    var name = args[1];
                    if (!allowDimension && List.of("DIM1", "DIM-1").contains(name)) {
                        LogUtil.send(sender, LogLevel.ERROR, "维度之间的传送已被禁用。");
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
                    break;
                }

                /* xen list */
                case "list": {
                    var worlds = Worlds.getLoadedWorldList(allowDimension);
                    LogUtil.send(sender, LogLevel.INFO, "当前可传送世界（共 &a" + worlds.size() + "&r 个）：");
                    LogUtil.send(sender, LogLevel.INFO, "&a" + StringUtils.join(worlds, ", "));
                    break;
                }

                /* xen reloadPlugin */
                case "reloadPlugin": {
                    Files.reload();
                    LogUtil.send(sender, LogLevel.SUCCESS, "重载成功。");
                   break;
                }

                /* xen rule [set|get] <world-name> <rule> <value> */
                case "rule": {
                    if (args.length == 1) {
                        LogUtil.send(sender, LogLevel.WARNING, "请指定一个操作（&eget &r或 &eset&r）。");
                        return true;
                    }
                    if (args.length == 2) {
                        LogUtil.send(sender, LogLevel.ERROR, "请提供世界名称。");
                        return true;
                    }
                    if (args.length == 3) {
                        LogUtil.send(sender, LogLevel.ERROR, "请提供目标游戏规则名称。");
                        return true;
                    }
                    // Now, args.length is >= 4. Equals when and only when value is null.
                    var name = args[2];
                    var rule = args[3];
                    var target = GameRule.getByName(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name));
                    var world = Bukkit.getWorld(name);
                    if (world == null) {
                        LogUtil.send(sender, LogLevel.ERROR, "世界 &e" + name + " &r不存在。");
                        return true;
                    }
                    switch (args[1]) {
                        case "set": {
                            if (target == null) {
                                LogUtil.send(sender, LogLevel.WARNING, "游戏规则 &e" + rule + "&r不存在。");
                                return true;
                            }
                            if (target.getType() == Integer.class) {
                                Integer value = args.length == 5 ? Integer.parseInt(args[4]) : 1;
                                if (world.setGameRule((GameRule<Integer>) target, value)) {
                                    LogUtil.send(sender, LogLevel.SUCCESS, "成功将世界 &b" + name + "&r 的规则 &e" + rule + "&r 设为 &a" + value + "&r。");
                                    return true;
                                } else {
                                    LogUtil.send(sender, LogLevel.ERROR, "设置失败。");
                                }
                            } else if (target.getType() == Boolean.class) {
                                Boolean value = args.length != 5 || Boolean.parseBoolean(args[4]);
                                if (world.setGameRule((GameRule<Boolean>) target, value)) {
                                    LogUtil.send(sender, LogLevel.SUCCESS, "成功将世界 &b" + name + "&r 的规则 &e" + rule + "&r 设为 &" + (value ? "atrue" : "cfalse") + "&r。");
                                    return true;
                                } else {
                                    LogUtil.send(sender, LogLevel.ERROR, "设置失败。");
                                }
                            }
                            break;
                        }

                        case "get": {
                            if (target == null) {
                                LogUtil.send(sender, LogLevel.WARNING, "游戏规则 &e" + rule + "&r不存在。");
                                return true;
                            }
                            var value = world.getGameRuleValue(target);
                            if (value == null) {
                                LogUtil.send(sender, LogLevel.ERROR, "相应规则值为空。");
                                return true;
                            }
                            LogUtil.send(sender, LogLevel.INFO, value.toString());
                            break;
                        }

                        default: {
                            LogUtil.send(sender, LogLevel.WARNING, "无效操作。支持的操作仅包含 &eget &r和 &eset&r。");
                            return true;
                        }
                    }
                }
            }
        }
        return true;
    }


}
