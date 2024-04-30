package com.puddingkc;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PuddingMacro extends JavaPlugin implements TabCompleter {

    private File macroFile;
    private FileConfiguration macroConfig;

    @Override
    public void onEnable() {
        // 初始化插件
        getLogger().info("插件 PuddingMacro 已启用");
        // 加载宏配置文件
        macroFile = new File(getDataFolder(), "macros.yml");
        if (!macroFile.exists()) {
            saveResource("macros.yml", false);
        }
        macroConfig = YamlConfiguration.loadConfiguration(macroFile);
        getCommand("macro").setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        // 关闭插件
        getLogger().info("插件 PuddingMacro 已卸载");
        // 保存宏配置文件
        try {
            macroConfig.save(macroFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("macro")) {

            if (!sender.hasPermission("puddingmacro.admin")) {
                return false;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用该指令");
                return true;
            }
            Player player = (Player) sender;
            // 处理宏命令
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "指令宏使用方法:");
                sender.sendMessage(ChatColor.GRAY + " /macro create <名称> - 创建宏");
                sender.sendMessage(ChatColor.GRAY + " /macro add <名称> <指令(原版不要/，WE要一个/)> - 向指定宏添加指令");
                sender.sendMessage(ChatColor.GRAY + " /macro remove <名称> - 删除宏");
                sender.sendMessage(ChatColor.GRAY + " /macro removeline <名称> <行数(从0开始)> - 删除指定宏的指定行命令");
                sender.sendMessage(ChatColor.GRAY + " /macro run <名称> - 运行宏");
                sender.sendMessage(ChatColor.GRAY + " /macro list - 查看你创建的宏");
                sender.sendMessage(ChatColor.GRAY + " /macro info <名称> - 查看指定宏的命令");
                sender.sendMessage(ChatColor.GRAY + " /macro share <名称> <玩家> - 将指定宏分享给指定玩家");
                return true;
            }
            String subCommand = args[0];
            switch (subCommand.toLowerCase()) {
                case "create":
                    // 创建宏
                    if (args.length < 2) {
                        sender.sendMessage("使用方法: /macro create <名称>");
                        return true;
                    }
                    String macroName = args[1];
                    createMacro(player, macroName);
                    sender.sendMessage("指令宏 '" + macroName + "' 创建成功!");
                    break;
                case "add":
                    // 向宏添加命令
                    if (args.length < 3) {
                        sender.sendMessage("使用方法: /macro add <名称> <指令>");
                        return true;
                    }
                    String macroToAdd = args[1];
                    StringBuilder commandToAdd = new StringBuilder(args[2]);
                    for (int i = 3; i < args.length; i++) {
                        commandToAdd.append(" ").append(args[i]);
                    }
                    addCommandToMacro(player, macroToAdd, commandToAdd.toString());
                    break;
                case "remove":
                    // 删除宏
                    if (args.length < 2) {
                        sender.sendMessage("使用方法: /macro remove <名称>");
                        return true;
                    }
                    String macroToRemove = args[1];
                    removeMacro(player, macroToRemove);
                    break;
                case "run":
                    // 运行宏
                    if (args.length < 2) {
                        sender.sendMessage("使用方法: /macro run <名称>");
                        return true;
                    }
                    String macroToRun = args[1];
                    runMacro(player, macroToRun);
                    break;
                case "info":
                    // 查看指定宏的指令列表
                    if (args.length < 2) {
                        sender.sendMessage("使用方法: /macro info <名称>");
                        return true;
                    }
                    String macroInfo = args[1];
                    showMacroInfo(player, macroInfo);
                    break;
                case "list":
                    // 查看宏列表
                    listMacros(player);
                    break;
                case "removeline":
                    // 删除指令
                    if (args.length < 3) {
                        sender.sendMessage("使用方法: /macro removeline <名称> <行数>");
                        return true;
                    }
                    String macroToRemoveCmd = args[1];
                    int index;
                    try {
                        index = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "行数请输入一个整数");
                        return true;
                    }
                    removeCommandFromMacro(player, macroToRemoveCmd, index);
                    break;
                case "share":
                    // 分享宏
                    if (args.length < 3) {
                        sender.sendMessage("使用方法: /macro share <名称> <玩家>");
                        return true;
                    }
                    String macroToShare = args[1];
                    String playerToShareWith = args[2];
                    shareMacro(player, macroToShare, playerToShareWith);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "未知的指令");
                    break;
            }
            return true;
        }
        return false;
    }

    private void createMacro(Player player, String macroName) {
        ConfigurationSection macroSection = macroConfig.createSection(player.getUniqueId() + "." + macroName);
        macroSection.set("commands", new ArrayList<String>());
    }

    private void removeCommandFromMacro(Player player, String macroName, int index) {
        ConfigurationSection macroSection = macroConfig.getConfigurationSection(player.getUniqueId() + "." + macroName);
        if (macroSection == null) {
            player.sendMessage(ChatColor.RED + "指令宏 '" + macroName + "' 未找到");
            return;
        }
        List<String> commands = macroSection.getStringList("commands");
        if (index < 0 || index >= commands.size()) {
            player.sendMessage(ChatColor.RED + "没有找到指定的指令行数");
            return;
        }
        commands.remove(index);
        macroSection.set("commands", commands);
        player.sendMessage(ChatColor.GREEN + "指定行 " + index + " 已成功删除");
    }

    private void addCommandToMacro(Player player, String macroName, String command) {
        ConfigurationSection macroSection = macroConfig.getConfigurationSection(player.getUniqueId() + "." + macroName);
        if (macroSection == null) {
            player.sendMessage(ChatColor.RED + "指令宏 '" + macroName + "' 未找到");
            return;
        }
        List<String> commands = macroSection.getStringList("commands");
        commands.add(command);
        macroSection.set("commands", commands);
        player.sendMessage(ChatColor.GREEN + "已成功将指令添加到选定的宏");
    }

    private void removeMacro(Player player, String macroName) {
        if (macroConfig.contains(player.getUniqueId() + "." + macroName)) {
            macroConfig.set(player.getUniqueId() + "." + macroName, null);
            player.sendMessage(ChatColor.GREEN + "指令宏 '" + macroName + "' 已成功删除");
        } else {
            player.sendMessage(ChatColor.RED + "指令宏 '" + macroName + "' 未找到");
        }
    }

    private void runMacro(Player player, String macroName) {
        ConfigurationSection macroSection = macroConfig.getConfigurationSection(player.getUniqueId() + "." + macroName);
        if (macroSection == null) {
            player.sendMessage(ChatColor.RED + "指令宏 '" + macroName + "' 未找到");
            return;
        }
        List<String> commands = macroSection.getStringList("commands");
        for (String cmd : commands) {
            getServer().dispatchCommand(player, cmd);
        }
        player.sendMessage(ChatColor.GREEN + "指令宏 '" + macroName + "' 已成功运行");
    }

    private void listMacros(Player player) {
        ConfigurationSection playerSection = macroConfig.getConfigurationSection(player.getUniqueId().toString());
        if (playerSection == null) {
            player.sendMessage(ChatColor.RED + "你还没有创建任何指令宏");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "你创建的指令宏:");
        for (String macroName : playerSection.getKeys(false)) {
            player.sendMessage("- " + macroName);
        }
    }

    private void showMacroInfo(Player player, String macroName) {
        ConfigurationSection macroSection = macroConfig.getConfigurationSection(player.getUniqueId() + "." + macroName);
        if (macroSection == null) {
            player.sendMessage(ChatColor.RED + "指令宏 '" + macroName + "' 未找到");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "指令宏 '" + macroName + "' 包含的命令:");
        List<String> commands = macroSection.getStringList("commands");
        for (String cmd : commands) {
            player.sendMessage("- " + cmd);
        }
    }

    private void shareMacro(Player player, String macroName, String targetPlayerName) {
        ConfigurationSection playerSection = macroConfig.getConfigurationSection(player.getUniqueId().toString());
        if (playerSection == null || !playerSection.contains(macroName)) {
            player.sendMessage(ChatColor.RED + "指令宏 '" + macroName + "' 未找到");
            return;
        }

        Player targetPlayer = getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "指定的玩家 '" + targetPlayerName + "' 不在线");
            return;
        }

        ConfigurationSection targetPlayerSection = macroConfig.getConfigurationSection(targetPlayer.getUniqueId().toString());
        if (targetPlayerSection == null) {
            targetPlayerSection = macroConfig.createSection(targetPlayer.getUniqueId().toString());
        }

        ConfigurationSection targetMacroSection = targetPlayerSection.createSection(macroName);
        targetMacroSection.set("commands", playerSection.getStringList(macroName + ".commands"));

        try {
            macroConfig.save(macroFile);
            player.sendMessage(ChatColor.GREEN + "已成功将指令宏 '" + macroName + "' 分享给玩家 " + targetPlayerName);
            targetPlayer.sendMessage(ChatColor.GREEN + "玩家 " + player.getName() + " 将一个指令宏分享给了你");
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "分享失败 '" + macroName + "' 玩家: " + targetPlayerName);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("macro")) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                String[] subCommands = {"create", "add", "remove", "run", "list", "info", "removeline", "share"};
                for (String subCommand : subCommands) {
                    if (subCommand.startsWith(args[0].toLowerCase())) {
                        completions.add(subCommand);
                    }
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("run") || args[0].equalsIgnoreCase("share") || args[0].equalsIgnoreCase("removeline")) {
                    ConfigurationSection playerSection = macroConfig.getConfigurationSection(((Player) sender).getUniqueId().toString());
                    if (playerSection != null) {
                        for (String macroName : playerSection.getKeys(false)) {
                            if (macroName.startsWith(args[1].toLowerCase())) {
                                completions.add(macroName);
                            }
                        }
                    }
                }
            }
            return completions;
        }
        return null;
    }
}
