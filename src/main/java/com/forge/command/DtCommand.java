package com.forge.command;

import com.forge.ForgePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class DtCommand implements CommandExecutor, TabCompleter {
    private final ForgePlugin plugin;
    public DtCommand(ForgePlugin plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.msg().msg("only_player"));
                return true;
            }
            Player p = (Player) sender;
            plugin.templates().openSelectGui(p);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("at")) {
            if (!sender.hasPermission("forge.admin")) { sender.sendMessage(plugin.msg().msg("no_permission")); return true; }
            if (args.length >= 3 && args[1].equalsIgnoreCase("create") && sender instanceof Player) {
                Player p = (Player) sender;
                ItemStack hand = p.getInventory().getItemInMainHand();
                Material icon = hand == null || hand.getType() == Material.AIR ? Material.PAPER : hand.getType();
                String name = args[2];
                plugin.attributes().create(name, icon);
                sender.sendMessage(plugin.msg().msg("attr_created").replace("{name}", name));
                return true;
            }
            if (args.length >= 5 && args[1].equalsIgnoreCase("ys")) {
                String src = args[2];
                String dst = args[3];
                double percent = parseDouble(args[4], 100);
                plugin.mappings().map(src, dst, percent);
                sender.sendMessage(plugin.msg().msg("attr_mapped").replace("{src}", src).replace("{dst}", dst).replace("{percent}", String.valueOf(percent)));
                return true;
            }
            sender.sendMessage(plugin.msg().msg("invalid_args"));
            return true;
        }
        if (sub.equals("mb")) {
            if (!sender.hasPermission("forge.admin")) { sender.sendMessage(plugin.msg().msg("no_permission")); return true; }
            if (args.length >= 3 && args[1].equalsIgnoreCase("create") && sender instanceof Player) {
                Player p = (Player) sender;
                ItemStack hand = p.getInventory().getItemInMainHand();
                Material icon = (hand != null && hand.getType() != Material.AIR) ? hand.getType() : Material.PAPER;
                String name = args[2] + "的模板";
                plugin.templates().create(name, icon);
                sender.sendMessage(plugin.msg().msg("template_created").replace("{name}", name));
                // 直接打开模板编辑页面
                plugin.templates().openEditGui(p, name);
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("bj") && sender instanceof Player) {
                Player p = (Player) sender;
                String name = args[2];
                // Smart suffix: if 'name' doesn't exist but 'name的模板' exists, use that.
                if (!plugin.templates().exists(name) && plugin.templates().exists(name + "的模板")) {
                    name = name + "的模板";
                }
                plugin.templates().openEditGui(p, name);
                sender.sendMessage(plugin.msg().msg("template_edit_open").replace("{name}", name));
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("xx") && sender instanceof Player) {
                String name = args[2];
                // Smart suffix
                if (!plugin.templates().exists(name) && plugin.templates().exists(name + "的模板")) {
                    name = name + "的模板";
                }
                plugin.playerDao().unlockTemplate(((Player) sender).getUniqueId(), name);
                sender.sendMessage(plugin.msg().msg("template_learned").replace("{name}", name));
                return true;
            }
            if (args.length >= 4 && args[1].equalsIgnoreCase("st")) {
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage(plugin.msg().msg("player_not_found")); return true; }
                String tpl = args[3];
                plugin.playerDao().learnTemplate(target.getUniqueId(), tpl);
                sender.sendMessage(plugin.msg().msg("learned_template").replace("{name}", tpl));
                return true;
            }
            sender.sendMessage(plugin.msg().msg("invalid_args"));
            return true;
        }
        if (sub.equals("cl")) {
            if (!sender.hasPermission("forge.admin")) { sender.sendMessage(plugin.msg().msg("no_permission")); return true; }
            if (args.length >= 2 && args[1].equalsIgnoreCase("bj") && sender instanceof Player) {
                Player p = (Player) sender;
                plugin.materials().openEditGui(p);
                sender.sendMessage(plugin.msg().msg("material_edit_open"));
                return true;
            }
            sender.sendMessage(plugin.msg().msg("invalid_args"));
            return true;
        }
        if (sub.equals("pc")) {
            if (!sender.hasPermission("forge.admin")) { sender.sendMessage(plugin.msg().msg("no_permission")); return true; }
            if (args.length >= 4) {
                String a = args[1];
                String b = args[2];
                double v = parseDouble(args[3], 0);
                plugin.materials().setRepel(a, b, v);
                if (args.length >= 5 && args[4].equalsIgnoreCase("all")) {
                    plugin.materials().setRepel(b, a, v);
                }
                sender.sendMessage(plugin.msg().msg("saved"));
                return true;
            }
            sender.sendMessage(plugin.msg().msg("invalid_args"));
            return true;
        }
        if (sub.equals("gm")) {
            if (!sender.hasPermission("forge.admin")) { sender.sendMessage(plugin.msg().msg("no_permission")); return true; }
            // 新语法：/dt gm <材料星缀> <材料星缀2> <共鸣值> [all]
            if (args.length >= 4) {
                String a = args[1];
                String b = args[2];
                double v = parseDouble(args[3], 0);
                plugin.materials().setResonance(a, b, v);
                if (args.length >= 5 && args[4].equalsIgnoreCase("all")) {
                    plugin.materials().setResonance(b, a, v);
                }
                sender.sendMessage(plugin.msg().msg("saved"));
                return true;
            }
            sender.sendMessage(plugin.msg().msg("invalid_args"));
            return true;
        }
        if (sub.equals("ex")) {
            if (!sender.hasPermission("forge.admin")) { sender.sendMessage(plugin.msg().msg("no_permission")); return true; }
            if (args.length >= 3) {
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(plugin.msg().msg("player_not_found")); return true; }
                int level = 0;
                try { level = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                if (level < 0) level = 0;
                if (level > 4) level = 4;
                plugin.playerDao().setForgingLevel(target.getUniqueId(), level);
                sender.sendMessage("§a已将玩家 " + target.getName() + " 的锻造等级设置为 " + level);
                return true;
            }
            sender.sendMessage(plugin.msg().msg("invalid_args"));
            return true;
        }
        if (sub.equals("cx")) {
            if (!sender.hasPermission("dt.cx")) { sender.sendMessage(plugin.msg().msg("no_permission")); return true; }
            if (args.length >= 3 && args[1].equalsIgnoreCase("xz")) {
                String tag = args[2];
                java.util.List<String> repels = plugin.materials().listRepel(tag);
                java.util.List<String> resos = plugin.materials().listResonance(tag);
                String repelMsg = repels.isEmpty() ? "无" : String.join(" ", repels);
                String resoMsg = resos.isEmpty() ? "无" : String.join(" ", resos);
                sender.sendMessage("排斥：" + repelMsg + ";共鸣：" + resoMsg);
                return true;
            }
            sender.sendMessage(plugin.msg().msg("invalid_args"));
            return true;
        }
        if (sub.equals("help")) {
            sender.sendMessage("§e---- 锻造插件指令帮助 ----");
            sender.sendMessage("§a/dt at create <属性名> §7- 手持物品创建属性");
            sender.sendMessage("§a/dt at ys <源属性> <目标属性> <百分比> §7- 设置属性映射");
            sender.sendMessage("§a/dt mb create <模板名> §7- 手持物品创建模板");
            sender.sendMessage("§a/dt mb bj <模板名> §7- 编辑模板必要槽位");
            sender.sendMessage("§a/dt mb st <玩家> <模板名> §7- 授予玩家模板");
            sender.sendMessage("§a/dt cl bj §7- 编辑手持材料属性");
            sender.sendMessage("§a/dt pc <星缀A> <星缀B> <值> [all] §7- 设置排斥");
            sender.sendMessage("§a/dt gm <星缀A> <星缀B> <值> [all] §7- 设置共鸣");
            sender.sendMessage("§a/dt cx xz <星缀> §7- 查询星缀的排斥与共鸣");
            // 武器相关功能已移除
            sender.sendMessage("§a/dt ex <玩家> <等级> §7- 设置玩家锻造等级(0-4)");
            return true;
        }
        sender.sendMessage(plugin.msg().msg("unknown_subcommand"));
        return true;
    }
    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>(Arrays.asList("at", "mb", "cl", "pc", "gm", "ex", "help", "cx"));
            return filter(sub, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("at") && args.length == 2) return filter(Arrays.asList("create", "ys"), args[1]);
        if (sub.equals("mb") && args.length == 2) return filter(Arrays.asList("create", "bj", "st"), args[1]);
        if (sub.equals("cl") && args.length == 2) return filter(Collections.singletonList("bj"), args[1]);
        if (sub.equals("ex") && args.length == 2) return null; // Player list
        if (sub.equals("ex") && args.length == 3) return filter(Arrays.asList("0", "1", "2", "3", "4"), args[2]);
        return Collections.emptyList();
    }
    private List<String> filter(List<String> list, String prefix) {
        if (prefix.isEmpty()) return list;
        List<String> res = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(prefix.toLowerCase())) res.add(s);
        return res;
    }
}
