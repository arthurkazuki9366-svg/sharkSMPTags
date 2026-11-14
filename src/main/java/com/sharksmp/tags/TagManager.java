package com.sharksmp.tags;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagManager {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final SharkTags plugin;
    private final File file;

    // id -> texto bruto (com &) | ConcurrentHashMap porque o chat roda em outra thread
    private final Map<String, String> tags = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> owned = new HashMap<>();
    private final Map<UUID, String> equipped = new ConcurrentHashMap<>();

    public TagManager(SharkTags plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    // ---------- Cores ----------

    /** Converte &a, &l e tambem cores hex no formato &#RRGGBB. */
    public static String color(String s) {
        Matcher m = HEX.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, net.md_5.bungee.api.ChatColor.of("#" + m.group(1)).toString());
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    // ---------- Definicoes de tags ----------

    public boolean exists(String id) {
        return tags.containsKey(id);
    }

    public String rawText(String id) {
        return tags.get(id);
    }

    public List<String> ids() {
        List<String> list = new ArrayList<>(tags.keySet());
        Collections.sort(list);
        return list;
    }

    public void create(String id, String text) {
        tags.put(id, text);
        save();
    }

    public void delete(String id) {
        tags.remove(id);
        for (Set<String> set : owned.values()) set.remove(id);
        equipped.values().removeIf(v -> v.equals(id));
        save();
    }

    // ---------- Tags dos jogadores ----------

    public boolean owns(UUID uuid, String id) {
        return owned.getOrDefault(uuid, Collections.emptySet()).contains(id);
    }

    /** Retorna false se o jogador ja tinha a tag. */
    public boolean give(UUID uuid, String id) {
        boolean added = owned.computeIfAbsent(uuid, k -> new LinkedHashSet<>()).add(id);
        if (added) save();
        return added;
    }

    /** Retorna false se o jogador nao tinha a tag. */
    public boolean take(UUID uuid, String id) {
        Set<String> set = owned.get(uuid);
        if (set == null || !set.remove(id)) return false;
        if (id.equals(equipped.get(uuid))) equipped.remove(uuid);
        save();
        return true;
    }

    public List<String> ownedBy(UUID uuid) {
        List<String> list = new ArrayList<>();
        for (String id : owned.getOrDefault(uuid, Collections.emptySet())) {
            if (tags.containsKey(id)) list.add(id);
        }
        return list;
    }

    public void equip(UUID uuid, String id) {
        equipped.put(uuid, id);
        save();
    }

    public void unequip(UUID uuid) {
        equipped.remove(uuid);
        save();
    }

    public String equippedId(UUID uuid) {
        return equipped.get(uuid);
    }

    /** Texto colorido da tag equipada, ou "" se nao houver. */
    public String display(UUID uuid) {
        String id = equipped.get(uuid);
        if (id == null) return "";
        String text = tags.get(id);
        return text == null ? "" : color(text);
    }

    // ---------- Arquivo ----------

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection t = y.getConfigurationSection("tags");
        if (t != null) {
            for (String id : t.getKeys(false)) {
                String text = t.getString(id);
                if (text != null) tags.put(id, text);
            }
        }

        ConfigurationSection p = y.getConfigurationSection("players");
        if (p != null) {
            for (String key : p.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    owned.put(uuid, new LinkedHashSet<>(p.getStringList(key + ".owned")));
                    String eq = p.getString(key + ".equipped");
                    if (eq != null) equipped.put(uuid, eq);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, String> e : tags.entrySet()) {
            y.set("tags." + e.getKey(), e.getValue());
        }
        for (Map.Entry<UUID, Set<String>> e : owned.entrySet()) {
            y.set("players." + e.getKey() + ".owned", new ArrayList<>(e.getValue()));
        }
        for (Map.Entry<UUID, String> e : equipped.entrySet()) {
            y.set("players." + e.getKey() + ".equipped", e.getValue());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Nao foi possivel salvar data.yml: " + ex.getMessage());
        }
    }
}
