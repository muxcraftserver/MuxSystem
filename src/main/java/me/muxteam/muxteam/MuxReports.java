package me.muxteam.muxteam;

import com.google.common.collect.EvictingQueue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class MuxReports {
	private Map<String, HashMap<String, String>> reports = new HashMap<>();
	private MuxSystem ms;

	public MuxReports(final MuxSystem ms) {
		this.ms = ms;
		final Gson gson = new Gson();
		final FileConfiguration hashYML = ms.getHashYAML();
		if (hashYML.contains("reports")) {
			reports = gson.fromJson(hashYML.getString("reports"), new TypeToken<HashMap<String, HashMap<String, String>>>() { }.getType());
		}
	}
	public void close() {
		final Gson gson = new Gson();
		final FileConfiguration hashYML = ms.getHashYAML();
		hashYML.set("reports", gson.toJson(reports));
		this.ms = null;
	}
	public void clearReports() {
		reports.clear();
	}
	public int getReports() {
		final int cheatreports = (int) reports.keySet().stream().filter(key -> key.contains("|Cheat") && ms.getServer().getPlayer(UUID.fromString(key.split("\\|")[0])) != null).count();
		return getChatReports() + cheatreports;
	}
	public int getChatReports() {
		return (int) reports.keySet().stream().filter(key -> key.contains("|Chat")).count();
	}
	public String getChatHistory(final UUID uuid) {
		final HashMap<String, String> r = reports.get(uuid.toString() + "|Chat");
		if (r == null) {
			return null;
		}
		AtomicReference<String> history = new AtomicReference<>("");
		r.values().stream().limit(5).forEach(text -> history.updateAndGet(v -> v + ms.getLoreFromLongText(text.substring(1, text.length() - 1), "§f")));
		return history.get();
	}
	public void sendReportSummary(final Player p) {
		final int r = p.hasPermission("muxsystem.anticheat") ? getReports() : getChatReports();
		if (p.hasPermission("muxsystem.reports") && r > 0) p.sendMessage("§6§lMuxReports>§7 Es gibt §6" + r + "§7 Spieler " + (r > 1 ? "Meldungen" : "Meldung") + ".");
	}
	public void informReporters(final UUID uuid, final String type) {
		final Map<String, String> r = reports.remove(uuid.toString() + "|" + type);
		if (r != null) {
			for (final String plname : r.keySet()) {
				final OfflinePlayer op = ms.getPlayer(plname);
				final MuxUser u = op.isOnline() ? ms.getMuxUser(op.getName()) : ms.getDB().loadPlayer(op.getUniqueId());
				ms.getChat().sendOfflineMessageOrChat(op, u, "MuxReport", "Deine Anfrage wurde bearbeitet und der Spieler wurde bestraft.", "§f", null);
			}
		}
	}
	public void handleInventory(final Player p, final ItemStack i, final String invname, final Inventory inv) {
		final Material m = i.getType();
		if (m == Material.ITEM_FRAME) {
			p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
			if (i.getItemMeta().getDisplayName().contains("Overview")) {
				final ItemMeta profile = inv.getItem(4).getItemMeta();
				final String name = ChatColor.stripColor(profile.getDisplayName());
				p.performCommand("o " + name);
				return;
			}
			if (invname.contains("Reports")) {
				p.performCommand("team");
				return;
			}
			handleCommand(p, new String[0]);
			return;
		} else if (m == Material.STAINED_CLAY) {
			final String plname = ChatColor.stripColor(i.getItemMeta().getDisplayName()).split(" ")[0];
			p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
			if (i.getDurability() == 11) p.performCommand("report " + plname + " Chat");
			else p.performCommand("report " + plname + " Cheat");
			return;
		} else if (inv.getItem(4).getType() != Material.SKULL_ITEM) {
			p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
			return;
		}
		final ItemMeta profile = inv.getItem(4).getItemMeta();
		final String name = ChatColor.stripColor(profile.getDisplayName()).toLowerCase().replace("✔", ""), type = ChatColor.stripColor(profile.getLore().get(0).split(": ")[1]);
		if (m == Material.TNT) {
			final OfflinePlayer op = ms.getPlayer(name);
			reports.remove(op.getUniqueId().toString() + "|" + type);
			p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
		} else if (m == Material.EYE_OF_ENDER) {
			p.closeInventory();
			final Player pl = ms.getServer().getPlayer(name);
			if (pl == null || p.canSee(pl) == false) {
				return;
			}
			if (ms.isVanish(p) == false) p.performCommand("vanish");
			if (p.getAllowFlight() == false) p.performCommand("fly");
			p.performCommand("tp " + name);
			ms.getAntiCheat().watchCPS(p, pl);
			return;
		} else if (m == Material.WEB) {
			final OfflinePlayer op = ms.getPlayer(name);
			if (ms.checkGeneralCooldown(name, "PVPPUNISH", 15000L, true)){ // PREVENT DOUBLE PUNISHES
				p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
				return;
			}
			p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
			ms.getBans().applyTimeMute(p, name, 21600, reports.get(op.getUniqueId().toString() + "|Chat") != null ? "Report" : "Manual");
			p.closeInventory();
			return;
		} else if (m == Material.SPRUCE_DOOR_ITEM) {
			final OfflinePlayer op = ms.getPlayer(name);
			if (ms.checkGeneralCooldown(name, "CHATPUNISH", 15000L, true)){ // PREVENT DOUBLE PUNISHES
				p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
				return;
			}
			p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
			ms.getBans().applyPvPBan(p, name, reports.get(op.getUniqueId().toString() + "|Cheat") != null ? "Report" : "Manual");
			p.closeInventory();
			return;
		} else {
			p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
			return;
		}
		handleCommand(p, new String[0]);
	}
	public boolean handleCommand(final Player p, final String[] args) {
		if (p.hasPermission("muxsystem.reports") == false) {
			if (args.length < 1) {
				ms.showItemBar(p, ms.usage("/report [spieler]"));
				return true;
			}
			final String pname = p.getName();
			final OfflinePlayer op = ms.getPlayer(args[0]);
			if (op == null) {
				ms.showItemBar(p, ms.hnotfound);
				return true;
			}
			String victimuuid = op.getUniqueId().toString(), victimn = args[0].toLowerCase();
			if (pname.equalsIgnoreCase(op.getName())) {
				ms.showItemBar(p, "§cDu kannst dich nicht selber melden.");
				return true;
			} else if (args.length < 2 || args[1].toLowerCase().matches("(cheat|chat)") == false) {
				final HashMap<String, String> chatreports = reports.get(victimuuid + "|Chat"), cheatreports = reports.get(victimuuid + "|Cheat");
				final boolean alreadychat = chatreports != null && chatreports.containsKey(pname),
						alreadycheat = cheatreports != null && cheatreports.containsKey(pname);
				final String chatcolor = alreadychat ? "§7" : "§9", cheatcolor = alreadycheat ? "§7" : "§4";
				ms.openBook(ms.cmdBook("§7       §lMux§2§lCraft", "   Klicke auf die Art\n   des Reports für:\n\n§0§l" + op.getName() + "\n",
						new Object[] {"/report " + victimn + " Chat", chatcolor + "§lChat" + chatcolor + ", wenn er sich " + chatcolor + "anstößig verhält.\n\n", alreadychat ? "§7§oDu hast ihn bereits hierfür gemeldet." : "§9§oKlicke zum reporten"},
						new Object[] {"/report " + victimn + " Cheat", cheatcolor + "§lCheat" + cheatcolor + ", wenn er " + cheatcolor + "unfair kämpft.", alreadycheat ? "§7§oDu hast ihn bereits hierfür gemeldet." : "§4§oKlicke zum reporten"}), p);
				return true;
			}
			final boolean chat = args[1].toLowerCase().matches("(chat)");
			final String type = (chat ? "Chat" : "Cheat");
			HashMap<String, String> r = reports.get(victimuuid + "|" + type);
			if (r == null) r = new HashMap<>();
			if (r.containsKey(pname)) {
				ms.showItemBar(p, "§cDu hast den Spieler bereits dafür gemeldet.");
			} else if (ms.checkGeneralCooldown(pname, "REPORT", 60000L, true)) {
				ms.showItemBar(p, "§cBitte warte bevor du einen weiteren Report erstellst.");
			} else {
				p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
				ms.showItemBar(p, "§fDu hast den Spieler §6" + op.getName() + " §ferfolgreich gemeldet.");
				final EvictingQueue<String> lastchats = ms.getChat().getLastChats(op.getUniqueId());
				r.put(pname, chat ? lastchats.toString() : "");
				if (r.size() == 1) reports.put(victimuuid + "|" + type, r);
			}
			return true;
		} else if (args.length == 0) {
			final InvType it = ms.getActiveInv(p.getName());
			final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxTeam§0 | Reports");
			inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
			inv.setItem(4, ms.renameItemStack(new ItemStack(Material.EMPTY_MAP), "§f§lReports", "§7Prüfe nach, ob die Beschwerden", "§7der Spieler gerechtfertigt sind."));
			if (p.hasPermission("muxsystem.anticheat"))
				inv.setItem(8, ms.addLore(ms.renameItemStack(new ItemStack(Material.ENCHANTED_BOOK), "§f§lAntiCheat Stats", "§7Heutige Kicks: §f" + ms.getAntiCheat().getKicks(), ""), ms.getAntiCheat().getKickString()));
			short pos = 18;
			final LinkedList<Map.Entry<String, HashMap<String, String>>> entries = new LinkedList<>(reports.entrySet());
			entries.sort(Collections.reverseOrder(Comparator.comparingInt(o -> o.getValue().size())));
			for (final Entry<String, HashMap<String, String>> keys : entries) {
				final UUID uuid = UUID.fromString(keys.getKey().split("\\|")[0]);
				final OfflinePlayer op = ms.getServer().getOfflinePlayer(uuid);
				final String type = keys.getKey().split("\\|")[1], plname = op.getName();
				final boolean chat = type.equals("Chat");
				if (chat || p.hasPermission("muxsystem.anticheat") && plname != null && ms.getServer().getPlayer(plname) != null) {
					final MuxUser u = ms.getMuxUser(plname);
					if (chat == false && u != null && u.isPvPbanned()) continue;
					final int size = keys.getValue().size();
					inv.setItem(pos, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, chat ? (byte) 11 : (byte) 14), "§f§l" + plname + " §7» §f" + size, "§7Art des Reports: " + (chat ? "§9" : "§c") + type, "", "§fKlicke§7, um die Meldung", "§7zu bearbeiten."));
					pos++;
				}
				if (pos == 45) break;
			}
			if (it != InvType.TEAM && it != InvType.REPORTS) p.closeInventory();
			p.openInventory(inv);
			ms.setActiveInv(p.getName(), InvType.REPORTS);
		} else {
			final String type = args.length == 1 ? "Cheat" : args[1];
			if (p.hasPermission("muxsystem.anticheat") == false && type.equals("Chat") == false) {
				ms.showItemBar(p, "§cDu kannst nicht andere Spieler reporten.");
				return true;
			}
			final Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(args[0]);
			if (entry.getValue() == null) {
				ms.showItemBar(p, ms.hnotfound);
				return true;
			}
			final String plname = entry.getKey();
			final Player pl = ms.getServer().getPlayerExact(plname);
			final boolean online = pl != null && p.canSee(pl);
			final boolean verified = type.equals("Cheat") && online && ms.getAntiCheat().isVerified(pl);
			final InvType it = ms.getActiveInv(p.getName());
			final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxReport§0 | " + plname);
			inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + (it == InvType.USERCONTROL ? " (Overview)" : "")));
			inv.setItem(4, ms.renameItemStack(ms.getHead(plname), "§f§l" + plname + (verified ? "§a✔" : ""), "§7Art des Reports: " + (type.equals("Chat") ? "§9" : "§c") + type));
			if (type.equals("Cheat")) {
				final ItemStack cheathistory = ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lWerte");
				final String client = online ? ms.getMuxUser(plname).getClient() : null;
				if (client != null) ms.addLore(cheathistory, "§7Minecraft Client: §f" + client + (ms.getAntiCheat().hasForge(plname) ? " (Forge)" : ""), "");
				ms.addLore(cheathistory, ms.getAntiCheat().getViolations(plname));
				if (cheathistory.getItemMeta().getLore() == null || (client != null && cheathistory.getItemMeta().getLore().size() == 2)) {
					ms.addLore(cheathistory, "§aEs sind noch keine Werte für", "§adiesen Spieler vorhanden.");
				}
				final UUID uuid = entry.getValue().getUniqueId();
				final HashMap<String, String> r = reports.get(uuid.toString() + "|Cheat");
				final boolean hasreports = r != null, fulltrusted = ms.isFullTrusted(p.getName());
				inv.setItem(fulltrusted ? (online == false || hasreports ? 22 : 24) : (hasreports ? 20 : 22), cheathistory);
				if (online) {
					if (fulltrusted) inv.setItem(hasreports ? 19 : 20, ms.renameItemStack(new ItemStack(Material.SPRUCE_DOOR_ITEM), "§6§lVom PvP ausschließen", "§7Nur bei hohen Werten und sehr verdächtigem", "§7Verhalten, ist diese Maßnahme berechtigt.", "", "§7Klicke, um ihn §6vom PvP auzuschließen§7."));
					inv.setItem(8, ms.renameItemStack(new ItemStack(Material.EYE_OF_ENDER), "§f§lKampf beobachten", "§7Prüfe, ob dieser Spieler während", "§7des Kampfes sich legitim verhält.", "", "§7Klicke, um diesen zu §fbeobachten§7."));
					if (hasreports) inv.setItem(fulltrusted ? 25 : 24, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lReports löschen", "§7Hiermit entfernst du alle Reports,", "§7die dem Spieler gegeben wurden.", "", "§7Klicke, um die §cReports zu löschen§7."));
				}
			} else if (type.equals("Chat")) {
				final UUID uuid = entry.getValue().getUniqueId();
				final HashMap<String, String> r = reports.get(uuid.toString() + "|Chat");
				final ItemStack chathistory = ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lChatverlauf");
				final boolean hasreports = r != null;
				if (hasreports) r.values().stream().limit(5).filter(text -> text.length() > 2).forEach(text -> ms.addLore(chathistory, ms.getLoreFromLongText(text.substring(1, text.length() - 1), "§f")));
				if (chathistory.getItemMeta().getLore() != null) {
					ms.addLore(chathistory, "");
				}
				final String s = ms.getChat().getLastChats(uuid).toString();
				if (s.length() > 2) ms.addLore(chathistory, ms.getLoreFromLongText(s.substring(1, s.length() - 1), "§f"));
				else if (chathistory.getItemMeta().getLore() == null) ms.addLore(chathistory, "§aDieser Spieler hat nichts geschrieben.");
				inv.setItem(hasreports ? 19 : 20, ms.renameItemStack(new ItemStack(Material.WEB), "§e§lChat verlangsamen", "§7Diese Maßnahme hilft sofort bei", "§7ständiges Umgehen des Filters.", "", "§7Klicke, um ihn zu §everlangsamen§7."));
				inv.setItem(hasreports ? 22 : 24, chathistory);
				if (hasreports) inv.setItem(25, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lReports löschen", "§7Hiermit entfernst du alle Reports,", "§7die dem Spieler gegeben wurden.", "", "§7Klicke, um die §cReports zu löschen§7."));
			}
			if (it != InvType.REPORTS && it != InvType.USERCONTROL) p.closeInventory();
			p.openInventory(inv);
			ms.setActiveInv(p.getName(), InvType.REPORTS);
		}
		return true;
	}
}