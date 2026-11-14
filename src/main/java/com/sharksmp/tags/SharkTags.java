package com.sharksmp.tags;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SharkTags extends JavaPlugin implements Listener {

    private TagManager tags;
    private TagMenu menu;

    @Override
    public void onEnable() {
        tags = new TagManager(this);
        menu = new TagMenu(this);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(menu, this);

        PluginCommand cmd = getCommand("tag");
        if (cmd != null) {
            TagCommand handler = new TagCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        for (Player p : Bukkit.getOnlinePlayers()) refreshTab(p);
        getLogger().info("SharkSMP-Tags ativado!");
    }

    @Override
    public void onDisable() {
        if (tags != null) tags.save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        refreshTab(e.getPlayer());
    }

    /** Coloca a tag na frente do nome no chat. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        String tag = tags.display(e.getPlayer().getUniqueId());
        if (tag.isEmpty()) return;
        e.setFormat(tag.replace("%", "%%") + "§r " + e.getFormat());
    }

    /** Mostra a tag na lista de jogadores (TAB). */
    public void refreshTab(Player player) {
        String tag = tags.display(player.getUniqueId());
        player.setPlayerListName(tag.isEmpty() ? null : tag + "§r " + player.getName());
    }

    public String prefix() {
        return "§b§lSharkSMP §8» §7";
    }

    public TagManager getTags() { return tags; }
    public TagMenu getMenu() { return menu; }
}
