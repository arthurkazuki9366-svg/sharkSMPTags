package com.sharksmp.tags;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class TagCommand implements CommandExecutor, TabCompleter {

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,16}");
    private static final String NOT_OP = "§cApenas OPs podem usar este comando.";

    private final SharkTags plugin;

    public TagCommand(SharkTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        TagManager tm = plugin.getTags();
        String sub = args.length == 0 ? "menu" : args[0].toLowerCase();

        switch (sub) {
            case "menu" -> {
                if (sender instanceof Player p) plugin.getMenu().open(p);
                else sender.sendMessage("Apenas jogadores.");
            }
            case "clear" -> {
                if (sender instanceof Player p) {
                    tm.unequip(p.getUniqueId());
                    plugin.refreshTab(p);
                    p.sendMessage(plugin.prefix() + "§7Tag removida.");
                } else sender.sendMessage("Apenas jogadores.");
            }
            case "create", "delete", "give", "take", "list" -> {
                if (!sender.isOp()) {
                    sender.sendMessage(plugin.prefix() + NOT_OP);
                    return true;
                }
                admin(sender, sub, args);
            }
            default -> help(sender);
        }
        return true;
    }

    private void admin(CommandSender sender, String sub, String[] args) {
        TagManager tm = plugin.getTags();

        switch (sub) {
            case "create" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.prefix() + "§cUso: /tag create <id> <texto com &cores>");
                    sender.sendMessage("§7Ex: §f/tag create vip &6&l[VIP]  §7ou com hex: §f&#FF5500[Shark]");
                    return;
                }
                String id = args[1].toLowerCase();
                if (!ID.matcher(id).matches()) {
                    sender.sendMessage(plugin.prefix() + "§cO ID deve ter 1-16 caracteres: letras minúsculas, números ou _.");
                    return;
                }
                if (tm.exists(id)) {
                    sender.sendMessage(plugin.prefix() + "§cJá existe uma tag com esse ID. Use /tag delete " + id + " antes.");
                    return;
                }
                String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                tm.create(id, text);
                sender.sendMessage(plugin.prefix() + "§aTag criada: §f" + id + " §7→ " + TagManager.color(text));
            }

            case "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + "§cUso: /tag delete <id>");
                    return;
                }
                String id = args[1].toLowerCase();
                if (!tm.exists(id)) {
                    sender.sendMessage(plugin.prefix() + "§cEssa tag não existe.");
                    return;
                }
                tm.delete(id);
                for (Player p : Bukkit.getOnlinePlayers()) plugin.refreshTab(p);
                sender.sendMessage(plugin.prefix() + "§7Tag §f" + id + " §7deletada (e removida de todos os jogadores).");
            }

            case "give", "take" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.prefix() + "§cUso: /tag " + sub + " <jogador> <id>");
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(plugin.prefix() + "§cJogador não encontrado.");
                    return;
                }
                String id = args[2].toLowerCase();
                if (!tm.exists(id)) {
                    sender.sendMessage(plugin.prefix() + "§cEssa tag não existe. Veja /tag list");
                    return;
                }
                UUID uuid = target.getUniqueId();
                String preview = TagManager.color(tm.rawText(id));
                String name = target.getName() == null ? args[1] : target.getName();

                if (sub.equals("give")) {
                    if (!tm.give(uuid, id)) {
                        sender.sendMessage(plugin.prefix() + "§e" + name + " já possui essa tag.");
                        return;
                    }
                    sender.sendMessage(plugin.prefix() + "§7Você deu a tag " + preview + " §7para §f" + name + "§7.");
                    Player online = target.getPlayer();
                    if (online != null) {
                        online.sendMessage(plugin.prefix() + "§aVocê recebeu a tag " + preview + "§a! Use §f/tag §apara equipar.");
                    }
                } else {
                    if (!tm.take(uuid, id)) {
                        sender.sendMessage(plugin.prefix() + "§e" + name + " não possui essa tag.");
                        return;
                    }
                    sender.sendMessage(plugin.prefix() + "§7Você removeu a tag " + preview + " §7de §f" + name + "§7.");
                    Player online = target.getPlayer();
                    if (online != null) {
                        plugin.refreshTab(online);
                        online.sendMessage(plugin.prefix() + "§cA tag " + preview + " §cfoi removida de você.");
                    }
                }
            }

            case "list" -> {
                List<String> ids = tm.ids();
                if (ids.isEmpty()) {
                    sender.sendMessage(plugin.prefix() + "§7Nenhuma tag criada ainda. Use /tag create");
                    return;
                }
                sender.sendMessage("§b§lSharkSMP §8- §7Tags (" + ids.size() + ")");
                for (String id : ids) {
                    sender.sendMessage("§8• §f" + id + " §7→ " + TagManager.color(tm.rawText(id)));
                }
            }
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§b§lSharkSMP §8- §7Tags");
        sender.sendMessage("§f/tag §7- abre o menu das suas tags");
        sender.sendMessage("§f/tag clear §7- remove a tag equipada");
        if (sender.isOp()) {
            sender.sendMessage("§e/tag create <id> <texto> §7- cria uma tag");
            sender.sendMessage("§e/tag delete <id> §7- deleta uma tag");
            sender.sendMessage("§e/tag give <jogador> <id> §7- dá uma tag");
            sender.sendMessage("§e/tag take <jogador> <id> §7- remove uma tag");
            sender.sendMessage("§e/tag list §7- lista todas as tags");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        boolean op = sender.isOp();
        if (args.length == 1) {
            List<String> subs = op
                    ? List.of("menu", "clear", "create", "delete", "give", "take", "list")
                    : List.of("menu", "clear");
            return filter(subs, args[0]);
        }
        if (!op) return new ArrayList<>();

        String sub = args[0].toLowerCase();
        if (args.length == 2 && sub.equals("delete")) return filter(plugin.getTags().ids(), args[1]);
        if (args.length == 2 && (sub.equals("give") || sub.equals("take"))) return null; // jogadores
        if (args.length == 3 && (sub.equals("give") || sub.equals("take"))) return filter(plugin.getTags().ids(), args[2]);
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String start) {
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.startsWith(start.toLowerCase())) out.add(o);
        return out;
    }
}
