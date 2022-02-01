package top.seatide.xen.Utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class Worlds {

    /**
     * 尝试加载所有 <code>worlds.yml</code> 中的 <code>key</code> 所指向的世界。
     */
    public static void loadAll() {
        Files.worlds.getKeys(false).forEach(worldName -> {
            var env = Files.worlds.getString(worldName + ".enviroment");
            var type = Files.worlds.getString(worldName + ".type");
            if (env != null && type != null) {
                if (createOrLoadWorld(worldName, WorldType.getByName(type), getEnvironmentByName(env)) == null) {
                    LogUtil.warn("加载世界 &e" + worldName + "&r 过程中出现问题。");
                }
            } else {
                LogUtil.error("由于相应配置信息缺失，无法正确加载世界 &e" + worldName + "&r。");
            }
        });
    }

    /**
     * 将世界信息存储至 <code>worlds.yml</code>
     * @param name 世界名称，作为 key
     * @param type 世界类型
     * @param env 世界环境类型
     */
    public static void save(String name, String type, String env) {
        var cs = Files.worlds.createSection(name);
        cs.set("type", type.toUpperCase());
        cs.set("enviroment", env.toUpperCase());
        Files.worlds.set(name, cs);
        Files.save(Files.worlds, "./worlds.yml");
    }

    /**
     * 创建或加载一个世界。当世界已经存在但未加载时为加载，不存在时为创建。
     * @param name 世界名称
     * @param type 世界类型
     * @param env 世界环境类型
     * @return 已加载或者创建的世界
     */
    public static @Nullable World createOrLoadWorld(String name, WorldType type, World.Environment env) {
        return new WorldCreator(name).type(type == null ? WorldType.NORMAL : type).environment(env == null ? World.Environment.NORMAL : env).createWorld();
    }

    /**
     * 获取当前已加载的所有世界名称列表。
     *
     * @param includeDimension 是否包含 DIM-1、DIM1 等非独立世界维度
     * @return 世界名称列表
     */
    public static List<String> getWorldNameList(boolean includeDimension) {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .distinct()
                .filter(s -> includeDimension || !List.of("DIM1", "DIM-1").contains(s))
                .collect(Collectors.toList());
    }

    public static World.Environment getEnvironmentByName(String name) {
        switch (name) {
            case "normal": return World.Environment.NORMAL;
            case "the-end": return World.Environment.THE_END;
            case "nether": return World.Environment.NETHER;
            default: throw new IllegalArgumentException();
        }
    }
}
