package org.zonedabone.ihatechat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.milkbowl.vault.chat.Chat;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class IHateChat extends JavaPlugin implements Listener {
	private String format = "[{name}] ";
	private String tellFormat = "&d{name} -> {target}: ";
	private Chat chat = null;
	private Map<Player, Player> conversations = new HashMap<Player, Player>();
	private Map<Player, Player> lastTells = new HashMap<Player, Player>();

	public void onEnable() {
		format = this.getConfig().getString("format", format);
		this.getConfig().set("format", format);
		tellFormat = this.getConfig().getString("tell-format", tellFormat);
		this.getConfig().set("tell-format", tellFormat);
		this.saveConfig();
		this.getServer().getPluginManager().registerEvents(this, this);
		setupChat();
	}

	public void onDisable() {
		for(Map.Entry<Player, Player> convo:conversations.entrySet()){
			convo.getKey().sendMessage(ChatColor.LIGHT_PURPLE + "Your conversation with " + convo.getValue().getName() + " has ended because of a reload.");
		}
		conversations.clear();
		lastTells.clear();
	}

	private boolean setupChat() {
		RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
		if (chatProvider != null) {
			chat = chatProvider.getProvider();
		}

		return (chat != null);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		conversations.remove(p);
		lastTells.remove(p);
		for (Player chatter : lastTells.keySet()) {
			if (lastTells.get(chatter).equals(p)) {
				lastTells.remove(chatter);
			}
		}
		for (Player chatter : conversations.keySet()) {
			if (conversations.get(chatter).equals(p)) {
				conversations.remove(chatter);
				chatter.sendMessage(ChatColor.LIGHT_PURPLE + "Your conversation with " + p.getName() + " has ended.");
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		if (conversations.get(p) == null) {
			e.setFormat(format + "%2$s");
		} else {
			e.setFormat(tellFormat + "%2$s");
			Player target = conversations.get(p);
			e.getRecipients().clear();
			e.getRecipients().add(target);
			e.getRecipients().add(p);
			e.setFormat(e.getFormat().replace("{target}", target.getName()));
			lastTells.put(target, p);
		}
		e.setFormat(e.getFormat().replace("{name}", p.getName()));
		if (chat != null) {
			e.setFormat(e.getFormat().replace("{prefix}", chat.getPlayerPrefix(p)));
			e.setFormat(e.getFormat().replace("{suffix}", chat.getPlayerSuffix(p)));
		}
		e.setFormat(ChatColor.translateAlternateColorCodes('&', e.getFormat()));

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
		}
		Player p = (Player) sender;
		if (command.getName().equals("tell")) {
			if (args.length > 1) {
				List<Player> targets = getServer().matchPlayer(args[0]);
				if (targets.size() < 1) {
					p.sendMessage(ChatColor.RED + "No user by that name found.");
				} else {
					Player targetStorage = conversations.get(p);
					for (Player target : targets) {
						conversations.put(p, target);
						p.chat(StringUtils.join(args, " ", 1, args.length));
					}
					conversations.put(p, targetStorage);
				}
			} else if (args.length > 0) {
				List<Player> targets = getServer().matchPlayer(args[0]);
				if (targets.size() > 1) {
					p.sendMessage(ChatColor.RED + "More than one person matched the provided username.");
				} else if (targets.size() < 1) {
					p.sendMessage(ChatColor.RED + "No user by that name found.");
				} else {
					conversations.put(p, targets.get(0));
					p.sendMessage(ChatColor.LIGHT_PURPLE + "Started a conversation with " + targets.get(0).getName() + ".");
				}
			} else {
				if (conversations.get(p) == null) {
					p.sendMessage(ChatColor.RED + "You aren't in a conversation.");
				} else {
					p.sendMessage(ChatColor.LIGHT_PURPLE + "Your conversation with " + conversations.get(p).getName() + " has ended.");
					conversations.remove(p);
				}
			}
		} else if (command.getName().equals("reply")) {
			Player target = lastTells.get(p);
			if (target == null) {
				p.sendMessage(ChatColor.RED + "Nobody has sent you a message recently.");
				return true;
			}
			if (args.length > 0) {
				Player targetStorage = conversations.get(p);
				conversations.put(p, target);
				p.chat(StringUtils.join(args, " "));
				conversations.put(p, targetStorage);
			} else {
				conversations.put(p, target);
				p.sendMessage(ChatColor.LIGHT_PURPLE + "Started a conversation with " + target.getName() + ".");
			}
		} else {
			return false;
		}
		return true;
	}
}
