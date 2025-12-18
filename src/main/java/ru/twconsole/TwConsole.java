package ru.twconsole;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class TwConsole extends JavaPlugin implements CommandExecutor {

    private final Set<UUID> listeners = new HashSet<>();
    private ConsoleAppender appender;

    @Override
    public void onEnable() {
        PluginCommand cmd = getCommand("console");
        if (cmd != null) {
            cmd.setExecutor(this);
        } else {
            getLogger().severe("Error: Command 'console' not found in plugin.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupLogAppender();
        getLogger().info("TwConsole enabled!");
    }

    @Override
    public void onDisable() {
        removeLogAppender();
        listeners.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок.");
            return true;
        }

        if (!player.hasPermission("twconsole.use")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Использование: /console <on|off>");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "on" -> {
                if (listeners.contains(player.getUniqueId())) {
                    player.sendMessage(ChatColor.YELLOW + "Вы уже видите консоль.");
                } else {
                    listeners.add(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "Режим консоли ВКЛЮЧЕН.");
                }
            }
            case "off" -> {
                if (listeners.remove(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Режим консоли ВЫКЛЮЧЕН.");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Вы не были подписаны на консоль.");
                }
            }
            default -> player.sendMessage(ChatColor.YELLOW + "Использование: /console <on|off>");
        }

        return true;
    }

    private void setupLogAppender() {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        this.appender = new ConsoleAppender();
        this.appender.start();
        rootLogger.addAppender(this.appender);
    }

    private void removeLogAppender() {
        if (this.appender != null) {
            Logger rootLogger = (Logger) LogManager.getRootLogger();
            rootLogger.removeAppender(this.appender);
            this.appender.stop();
        }
    }

    private class ConsoleAppender extends AbstractAppender {

        private final Pattern ANSI_PATTERN = Pattern.compile("\\x1B\\[[0-9;]*m");

        protected ConsoleAppender() {
            super("TwConsoleAppender", null, null, false, null);
        }

        @Override
        public void append(LogEvent event) {
            if (listeners.isEmpty()) return;

            String rawMessage = event.getMessage().getFormattedMessage();
            String cleanMessage = ANSI_PATTERN.matcher(rawMessage).replaceAll("");

            ChatColor messageColor = ChatColor.GRAY;

            if (cleanMessage.contains("ERROR") || cleanMessage.contains("Exception") || cleanMessage.contains("Caused by")) {
                messageColor = ChatColor.RED;
            } else if (cleanMessage.contains("WARN")) {
                messageColor = ChatColor.GOLD;
            } else if (cleanMessage.contains("INFO")) {
                messageColor = ChatColor.WHITE;
            }

            String finalMessage = ChatColor.DARK_GRAY + "[C] " + messageColor + cleanMessage;

            for (UUID uuid : listeners) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.sendMessage(finalMessage);
                }
            }
        }
    }
}