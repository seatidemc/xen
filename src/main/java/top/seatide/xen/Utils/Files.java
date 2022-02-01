package top.seatide.xen.Utils;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import top.seatide.xen.Main;

public final class Files {
    public static String cwd;
    public static FileConfiguration cfg;
    public static FileConfiguration worlds;

    public static void init(Main plugin) {
        cwd = plugin.getDataFolder().getPath();
        cfg = plugin.getConfig();
        worlds = load(".", "worlds.yml");
    }

    public static File getFile(File folder, String name) throws IOException {
        File file = new File(folder, name);
        if (!folder.exists()) {
            var state = folder.mkdir();
            if (!state) {
                throw new IOException("cannot create file directory automatically");
            }
        }
        if (!file.exists()) {
            try {
                var state = file.createNewFile();
                if (!state) {
                    throw new IOException("cannot create file automatically.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static FileConfiguration load(String path, String name) {
        try {
            return YamlConfiguration.loadConfiguration(getFile(
                    new File(path.replace(path.length() == 1 ? "." : "./", path.length() == 1 ? cwd : cwd + "/")),
                    name));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isEmptyConfiguration(ConfigurationSection config) {
        if (config == null)
            return true;
        return config.getKeys(false).size() == 0;
    }

    public static void save(FileConfiguration data, String targetFile) {
        try {
            data.save(targetFile.replace("./", cwd + "/"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reload() {
        cfg = load(".", "config.yml");
        worlds = load(".", "worlds.yml");
    }

    public static void saveWorld(String name, String type, String env) {
        var cs = worlds.createSection(name);
        cs.set("type", type);
        cs.set("enviroment", env);
        worlds.set(name, cs);
        save(worlds, "./worlds.yml");
    }
}