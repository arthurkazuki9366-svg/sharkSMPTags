package com.sharksmp.tags;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TagMenu implements Listener {

    private static final int SLOT_CLEAR = 49;

    private final SharkTags plugin;

    public TagMenu(SharkTags plugin) {
        this.plugin = plugin;
    }

    public static class TagHolder implements InventoryHolder {
        final Map<Integer, String> slots = new HashMap<>();
        Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public void open(Player player) {
        TagHolder holder = new TagHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, "§8Suas Tags");
        holder.inventory = inv;
        fill(player, holder);
        player.openInventory(inv);
    }

    private void fill(Player player, TagHolder holder) {
        Inventory inv = holder.inventory;
        inv.clear();
        holder.slots.clear();

        TagManager tm = plugin.getTags();
        UUID uuid = player.getUniqueId();
        String eq = tm.equippedId(uuid);

        int slot = 0;
        for (String id : tm.ownedBy(uuid)) {
            if (slot >= 45) break;
            boolean isEquipped = id.equals(eq);

            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(TagManager.color(tm.rawText(id)));
                meta.setLore(Arrays.asList(
                        "§7ID: §f" + id,
                        "",
                        isEquipped ? "§a✔ Equipada" : "§7Não equipada",
                        isEquipped ? "§eClique para remover" : "§eClique para equipar"));
                if (isEquipped) {
                    meta.addEnchant(Enchantment.LURE, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            holder.slots.put(slot, id);
            slot++;
        }

        if (slot == 0) {
            inv.setItem(22, named(Material.PAPER, "§cVocê ainda não tem tags",
                    "§7Apenas OPs podem dar tags.", "§7Fale com a staff!"));
        }

        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int s = 45; s < 54; s++) inv.setItem(s, filler);
        inv.setItem(SLOT_CLEAR, named(Material.BARRIER, "§cRemover tag equipada",
                "§7Fica sem nenhuma tag."));
    }

    private ItemStack named(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof TagHolder holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getInventory()) return;

        TagManager tm = plugin.getTags();
        UUID uuid = player.getUniqueId();
        int slot = e.getSlot();

        if (slot == SLOT_CLEAR) {
            tm.unequip(uuid);
            plugin.refreshTab(player);
            player.sendMessage(plugin.prefix() + "§7Tag removida.");
            fill(player, holder);
            return;
        }

        String id = holder.slots.get(slot);
        if (id == null) return;

        if (id.equals(tm.equippedId(uuid))) {
            tm.unequip(uuid);
            player.sendMessage(plugin.prefix() + "§7Tag removida.");
        } else if (tm.owns(uuid, id)) {
            tm.equip(uuid, id);
            player.sendMessage(plugin.prefix() + "§7Você equipou a tag " + TagManager.color(tm.rawText(id)) + "§7.");
        }
        plugin.refreshTab(player);
        fill(player, holder);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof TagHolder) e.setCancelled(true);
    }
}
