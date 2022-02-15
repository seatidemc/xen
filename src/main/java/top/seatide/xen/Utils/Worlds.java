package top.seatide.xen.Utils;

import org.bukkit.*;
import org.jetbrains.annotations.Nullable;
import top.seatide.xen.Main;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Worlds {
    public static Main plug;

    public static void init(Main plugin) {
        plug = plugin;
    }

    /**
     * 尝试加载所有 <code>worlds.yml</code> 中的 <code>key</code> 所指向的世界。
     * 加载过程中会将文件中所规定的各项应用到世界。
     */
    @SuppressWarnings("unchecked")
    public static void loadAll() {
        var worldNames = getAllRecordedWorldNames();
        if (worldNames.size() == 0) return;
        worldNames.forEach(worldName -> {
            var env = Files.worlds.getString(worldName + ".enviroment");
            var type = Files.worlds.getString(worldName + ".type");
            var spawnX = Files.worlds.getInt(worldName + ".spawn.x");
            var spawnY = Files.worlds.getInt(worldName + ".spawn.y");
            var spawnZ = Files.worlds.getInt(worldName + ".spawn.z");
            var difficulty = Files.worlds.getString(worldName + ".difficulty");
            var gamerules = Files.worlds.getConfigurationSection(worldName + ".rules");
            if (env != null && type != null) {
                var world = createOrLoadWorld(worldName, WorldType.getByName(type), getEnvironmentByName(env));
                if (world == null) {
                    LogUtil.error("加载世界 &e" + worldName + "&r 过程中出现问题。");
                    LogUtil.error("若该世界的相关文件已被删除，请在 worlds.yml 中去掉相关信息。");
                } else {
                    if (Files.worlds.contains(worldName + ".spawn.x")
                            && Files.worlds.contains(worldName + ".spawn.y")
                            && Files.worlds.contains(worldName + ".spawn.z")) world.setSpawnLocation(spawnX, spawnY, spawnZ);
                    if (difficulty != null) world.setDifficulty(Difficulty.valueOf(difficulty));
                    if (gamerules != null) {
                        for (String rule : gamerules.getKeys(false)) {
                            if (gamerules.isBoolean(rule)) {
                                var inst = (GameRule<Boolean>) GameRule.getByName(rule);
                                if (inst == null) {
                                    LogUtil.warn("worlds.yml 中的 " + worldName +".rules." + rule + " 的值无效，已跳过。");
                                    continue;
                                }
                                var value = gamerules.getBoolean(rule);
                                world.setGameRule(inst, value);
                            } else if (gamerules.isInt(rule)) {
                                var inst = (GameRule<Integer>) GameRule.getByName(rule);
                                if (inst == null) {
                                    LogUtil.warn("worlds.yml 中的 " + worldName +".rules." + rule + " 的值无效，已跳过。");
                                    continue;
                                }
                                var value = gamerules.getInt(rule);
                                world.setGameRule(inst, value);
                            } else {
                                LogUtil.warn("worlds.yml 中的 " + worldName +".rules." + rule + " 的值无效，已跳过。");
                            }
                        }
                    }
                    LogUtil.success("成功加载世界 &e" + worldName + "。");
                }
            } else {
                LogUtil.error("由于相关配置信息缺失，无法正确加载世界 &e" + worldName + "&r。");
            }
        });
    }

    /**
     * 保存所有 <code>worlds.yml</code> 中有记录的世界。
     */
    public static void saveAllRecordedWorld() {
       var targets = getAllRecordedWorldNames();
       if (targets.size() == 0) return;
       targets.forEach(name -> {
           var world = Bukkit.getWorld(name);
           if (world != null) {
               world.save();
               LogUtil.success("已保存世界 &a" + name + "&r。");
           } else {
               LogUtil.error("无法获取世界 &c" + name + "&r。");
           }
       });
    }

    /**
     * 获取所有 <code>worlds.yml</code> 中的世界名称。
     * @return 世界名称列表
     */
    public static Set<String> getAllRecordedWorldNames() {
        return Files.worlds.getKeys(false);
    }

    /**
     * 将世界信息存储至 <code>worlds.yml</code>
     * @param name 世界名称，作为 key
     * @param type 世界类型
     * @param env 世界环境类型
     */
    public static void saveAsRecord(String name, String type, String env) {
        var cs = Files.worlds.createSection(name);
        cs.set("type", type.toUpperCase());
        cs.set("enviroment", env.toUpperCase());
        Files.worlds.set(name, cs);
        Files.save(Files.worlds, "./worlds.yml");
    }

    /**
     * （同步）创建或加载一个世界。当世界已经存在但未加载时为加载，不存在时为创建。
     * @param name 世界名称
     * @param type 世界类型
     * @param env 世界环境类型
     * @return 已加载或者创建的世界
     */
    public static @Nullable World createOrLoadWorld(String name, WorldType type, World.Environment env) {
        return new WorldCreator(name).type(type == null ? WorldType.NORMAL : type).environment(env == null ? World.Environment.NORMAL : env).createWorld();
    }

    public static void createOrLoadWorldAsync(String name, WorldType type, World.Environment env, Consumer<World> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plug, () -> {
            var result = new WorldCreator(name).type(type == null ? WorldType.NORMAL : type).environment(env == null ? World.Environment.NORMAL : env).createWorld();
            callback.accept(result);
        });
    }

    /**
     * 获取当前服务器已加载的所有世界名称列表。
     *
     * @param includeDimension 是否包含 DIM-1、DIM1 等非独立世界维度
     * @return 世界名称列表
     */
    public static List<String> getLoadedWorldList(boolean includeDimension) {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .distinct()
                .filter(s -> includeDimension || !List.of("DIM1", "DIM-1").contains(s))
                .collect(Collectors.toList());
    }

    /**
     * 通过名称获取环境枚举项
     * @param name 名称，大写
     * @return 枚举项
     */
    public static World.Environment getEnvironmentByName(String name) {
        switch (name) {
            case "NORMAL": return World.Environment.NORMAL;
            case "THE_END": return World.Environment.THE_END;
            case "NETHER": return World.Environment.NETHER;
            default: throw new IllegalArgumentException();
        }
    }
}
