package top.seatide.xen;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
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

import java.util.*;
import java.util.stream.Collectors;

public class CommandHandler implements TabExecutor {
    public final static String[] ARGS = {"load", "unload", "create", "tp", "list", "rule", "diff", "difficulty", "help", "reloadPlugin"};

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
                        if (sender.hasPermission("xen.command.difficulty")) {
                            candy.add("diff");
                        }
                        if (sender.hasPermission("xen.command." + m)) {
                            candy.add(m);
                        }
                    }
                    return getResult(args[0], candy);
                case 2:
                    if (args[0].equals("load") || args[0].equals("create") || args[0].equals("unload") || args[0].equals("tp")) {
                        return Bukkit.getWorlds().stream().map(World::getName).distinct().collect(Collectors.toList());
                    }
                    if (args[0].equals("rule") || args[0].equals("mode") || args[0].equals("diff") || args[0].equals("difficulty"))
                        return List.of("get", "set");
                    break;
                case 3:
                    if (args[0].equals("load") || args[0].equals("create")) {
                        return List.of("NORMAL", "FLAT", "LARGE_BIOMES", "AMPLIFIED");
                    }
                    if (args[0].equals("rule"))
                        return Bukkit.getWorlds().stream().map(World::getName).distinct().collect(Collectors.toList());
                    break;
                case 4:
                    if (args[0].equals("load") || args[0].equals("create")) {
                        return List.of("NORMAL", "NETHER", "THE_END");
                    }
                    if (args[0].equals("rule"))
                        return Arrays.stream(GameRule.values()).map(GameRule::getName).distinct().collect(Collectors.toList());
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
                LogUtil.send(sender, LogLevel.WARNING, "???????????????????????????");
                return true;
            }
            if (!Arrays.asList(ARGS).contains(args[0])) {
                LogUtil.send(sender, LogLevel.ERROR, "???????????????");
                LogUtil.send(sender, LogLevel.INFO, "?????? &e/xen help &r???????????????");
                return true;
            }
            var allowDimension = Files.cfg.getBoolean("allow-dimension");
            switch (args[0]) {
                /* xen [load|create] <world-name> [type: normal|flat|large-biomes|amplified] [env: normal|nether|the-end] */
                case "load":
                case "create": {
                    if (args.length == 1) {
                        LogUtil.send(sender, LogLevel.WARNING, "????????????????????????");
                        return true;
                    }
                    var name = args[1];
                    if (Bukkit.getWorld(name) != null) {
                        LogUtil.send(sender, LogLevel.WARNING, "?????????????????????????????????");
                        return true;
                    }
                    WorldType type;
                    World.Environment env;
                    try {
                        type = args.length == 3 ? WorldType.getByName(args[2].toUpperCase()) : WorldType.NORMAL;
                        env = args.length == 4 ? Worlds.getEnvironmentByName(args[3]) : World.Environment.NORMAL;
                        assert type != null;
                    } catch (IllegalArgumentException | AssertionError e) {
                        LogUtil.send(sender, LogLevel.ERROR, "??????????????????????????????????????????");
                        LogUtil.send(sender, LogLevel.INFO, "?????? &e/xen help worldtype&8|&eenvtype &r????????????????????????");
                        return true;
                    }
                    Worlds.createOrLoadWorldAsync(name, type, env, r -> {
                        if (r == null) {
                            LogUtil.send(sender, LogLevel.ERROR, args[0].equals("load") ? "??????" : "??????" + "??????????????????????????????");
                        } else {
                            Worlds.saveAsRecord(name, type.name(), env.name());
                            LogUtil.send(sender, LogLevel.SUCCESS, "??????" + (args[0].equals("load") ? "??????" : "??????") + "?????? &a" + name + "&r???");
                        }
                    });
                    break;
                }


                /* xen unload <world-name> */
                case "unload": {
                    if (args.length == 1) {
                        LogUtil.send(sender, LogLevel.ERROR, "????????????????????????");
                        return true;
                    }

                    var name = args[1];
                    if (Bukkit.getWorld(name) == null) {
                        LogUtil.send(sender, LogLevel.WARNING, "??????????????????????????????");
                        return true;
                    }
                    if (Bukkit.unloadWorld(name, true)) {
                        LogUtil.send(sender, LogLevel.SUCCESS, "?????????????????? &a" + name + "&r???");
                    } else {
                        LogUtil.send(sender, LogLevel.ERROR, "?????????????????? &c" + name + "&r???");
                        LogUtil.send(sender, LogLevel.ERROR, "????????????????????????????????????????????????????????????????????????");
                    }
                    break;
                }


                /* xen tp <world-name> */
                case "tp": {
                    if (!(sender instanceof Player)) {
                        LogUtil.send(sender, LogLevel.ERROR, "?????????????????????????????????");
                        return true;
                    }

                    if (args.length == 1) {
                        LogUtil.send(sender, LogLevel.ERROR, "????????????????????????");
                        return true;
                    }

                    var name = args[1];
                    if (!allowDimension && List.of("DIM1", "DIM-1").contains(name)) {
                        LogUtil.send(sender, LogLevel.ERROR, "????????????????????????????????????");
                        return true;
                    }
                    var world = Bukkit.getWorld(name);
                    if (world == null) {
                        LogUtil.send(sender, LogLevel.WARNING, "??????????????????");
                        return true;
                    }
                    if (!((Player) sender).teleport(world.getSpawnLocation())) {
                        LogUtil.send(sender, LogLevel.ERROR, "???????????????");
                    }
                    break;
                }

                /* xen list */
                case "list": {
                    var worlds = Worlds.getLoadedWorldList(allowDimension);
                    LogUtil.send(sender, LogLevel.INFO, "??????????????????????????? &a" + worlds.size() + "&r ?????????");
                    LogUtil.send(sender, LogLevel.INFO, "&a" + StringUtils.join(worlds, ", "));
                    break;
                }

                /* xen reloadPlugin */
                case "reloadPlugin": {
                    Files.reload();
                    LogUtil.send(sender, LogLevel.SUCCESS, "???????????????");
                    break;
                }

                /* xen [diff|difficulty] [set|get] <world-name> <value> */
                case "diff":
                case "difficulty": {
                    if (args.length == 1) {
                        LogUtil.send(sender, LogLevel.WARNING, "????????????????????????&eget &r??? &eset&r??????");
                        return true;
                    }
                    if (args.length == 2) {
                        LogUtil.send(sender, LogLevel.ERROR, "????????????????????????");
                        return true;
                    }
                    if (args.length == 3) {
                        LogUtil.send(sender, LogLevel.ERROR, "????????????????????????");
                        return true;
                    }
                    var worldname = args[2];
                    switch (args[1]) {
                        case "set": {
                            var value = args[3].toUpperCase();
                            if (List.of("0", "1", "2", "3").contains(value)) {
                                switch (value) {
                                    case "0":
                                        value = "PEACEFUL";
                                        break;

                                    case "1":
                                        value = "EASY";
                                        break;

                                    case "2":
                                        value = "NORMAL";
                                        break;

                                    case "3":
                                        value = "HARD";
                                        break;
                                }
                            }
                            if (!List.of("PEACEFUL", "EASY", "NORMAL", "HARD").contains(value)) {
                                LogUtil.send(sender, LogLevel.ERROR, "&c" + value + "&r ??????????????????????????????");
                                return true;
                            }
                            var world = Bukkit.getWorld(worldname);
                            if (world == null) {
                                LogUtil.send(sender, LogLevel.ERROR, "?????? &e" + worldname + " &r????????????");
                                return true;
                            }
                            world.setDifficulty(Difficulty.valueOf(value));
                            Files.worlds.set(worldname + ".difficulty", value);
                            Files.save(Files.worlds, "./worlds.yml");
                            LogUtil.send(sender, LogLevel.SUCCESS, "???????????? &e" + worldname + "&r ?????????????????? &e" + value + "&r???");
                            return true;
                        }

                        case "get": {
                            var world = Bukkit.getWorld(worldname);
                            if (world == null) {
                                LogUtil.send(sender, LogLevel.ERROR, "?????? &e" + worldname + " &r????????????");
                                return true;
                            }
                            var diff = world.getDifficulty().toString();
                            LogUtil.send(sender, LogLevel.INFO, "?????? &e" + worldname + "&r ???????????? &e" + diff + "&r???");
                            return true;
                        }

                        default: {
                            LogUtil.send(sender, LogLevel.WARNING, "??????????????????????????????????????? &eget &r??? &eset&r???");
                            return true;
                        }
                    }
                }

                /* xen setworldspawn */
                // this is the alternative for /setworldspawn, which can be invalid sometimes for no reason.
                case "setworldspawn": {
                    if (!(sender instanceof Player)) {
                        LogUtil.send(sender, LogLevel.ERROR, "?????????????????????????????????");
                        return true;
                    }
                    var p = (Player) sender;
                    var location = p.getLocation();
                    var world = location.getWorld();
                    try {
                        Objects.requireNonNull(world).setSpawnLocation(location);
                    } catch (NullPointerException | IllegalStateException e) {
                        e.printStackTrace();
                        LogUtil.send(sender, LogLevel.ERROR, "?????????????????????");
                        return true;
                    }
                    Files.worlds.set(world.getName() + ".spawn.x", location.getX());
                    Files.worlds.set(world.getName() + ".spawn.y", location.getY());
                    Files.worlds.set(world.getName() + ".spawn.z", location.getZ());
                    Files.save(Files.worlds, "./worlds.yml");
                    LogUtil.success("??????????????? &e" + world.getName() + "&r ??????????????????????????? &a"
                            + location.getX()
                            + ", "
                            + location.getY()
                            + ", "
                            + location.getZ()
                            + " ["
                            + location.getYaw()
                            + "]");
                }

                /* xen rule [set|get] <world-name> <rule> <value> */
                case "rule": {
                    if (args.length == 1) {
                        LogUtil.send(sender, LogLevel.WARNING, "????????????????????????&eget &r??? &eset&r??????");
                        return true;
                    }
                    if (args.length == 2) {
                        LogUtil.send(sender, LogLevel.ERROR, "????????????????????????");
                        return true;
                    }
                    if (args.length == 3) {
                        LogUtil.send(sender, LogLevel.ERROR, "????????????????????????????????????");
                        return true;
                    }
                    // ????????????????????????????????? 4 ???????????? value ??????????????????
                    var worldname = args[2];
                    var rule = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, args[3]);
                    var target = GameRule.getByName(rule);
                    var world = Bukkit.getWorld(worldname);
                    if (world == null) {
                        LogUtil.send(sender, LogLevel.ERROR, "?????? &e" + worldname + " &r????????????");
                        return true;
                    }
                    switch (args[1]) {
                        case "set": {
                            if (target == null) {
                                LogUtil.send(sender, LogLevel.WARNING, "???????????? &e" + rule + "&r????????????");
                                return true;
                            }
                            if (target.getType() == Integer.class) {
                                Integer value = args.length == 5 ? Integer.parseInt(args[4]) : 1;
                                if (world.setGameRule((GameRule<Integer>) target, value)) {
                                    Files.worlds.set(worldname + ".rules." + rule, value);
                                    Files.save(Files.worlds, "./worlds.yml");
                                    LogUtil.send(sender, LogLevel.SUCCESS, "??????????????? &b" + worldname + "&r ????????? &e" + rule + "&r ?????? &a" + value + "&r???");
                                    return true;
                                } else {
                                    LogUtil.send(sender, LogLevel.ERROR, "???????????????");
                                }
                            } else if (target.getType() == Boolean.class) {
                                Boolean value = args.length != 5 || Boolean.parseBoolean(args[4]);
                                if (world.setGameRule((GameRule<Boolean>) target, value)) {
                                    Files.worlds.set(worldname + ".rules." + rule, value);
                                    Files.save(Files.worlds, "./worlds.yml");
                                    LogUtil.send(sender, LogLevel.SUCCESS, "??????????????? &b" + worldname + "&r ????????? &e" + rule + "&r ?????? &" + (value ? "atrue" : "cfalse") + "&r???");
                                    return true;
                                } else {
                                    LogUtil.send(sender, LogLevel.ERROR, "???????????????");
                                }
                            }
                            break;
                        }

                        case "get": {
                            if (target == null) {
                                LogUtil.send(sender, LogLevel.WARNING, "???????????? &e" + rule + "&r????????????");
                                return true;
                            }
                            var value = world.getGameRuleValue(target);
                            if (value == null) {
                                LogUtil.send(sender, LogLevel.ERROR, "????????????????????????");
                                return true;
                            }
                            LogUtil.send(sender, LogLevel.INFO, value.toString());
                            break;
                        }

                        default: {
                            LogUtil.send(sender, LogLevel.WARNING, "??????????????????????????????????????? &eget &r??? &eset&r???");
                            return true;
                        }
                    }
                }
            }
        }
        return true;
    }


}
