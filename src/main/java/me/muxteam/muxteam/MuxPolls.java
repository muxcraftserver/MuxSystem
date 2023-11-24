package me.muxteam.muxteam;

import me.muxteam.basic.Pageifier;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxSystem.Priority;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public final class MuxPolls {
	private boolean pollstart;
	private long time;
	private String question;
	private final Set<String> uplayers = new HashSet<>();
	private final LinkedHashMap<String, Short> pollchoices = new LinkedHashMap<>();
	private MuxSystem ms;

	public MuxPolls(final MuxSystem ms) {
		this.ms = ms;
	}

	public void close() {
		this.ms = null;
	}

	public boolean handleCommand(final Player p, final String[] args) {
		if (args.length == 0) {
			if (ms.isTrusted(p.getName()) == false) {
				ms.sendNoCMDMessage(p);
				return true;
			}
			p.sendMessage(ms.header((byte) 14, "§6"));
			if (pollstart) {
				printPoll(p, false);
				p.sendMessage(" ");
				for (final Map.Entry<String, Short> entry : pollchoices.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(e1,e2) -> e1, LinkedHashMap::new)).entrySet()) {
					ms.chatClickHoverRun(p, "  §8» §f" + entry.getKey() + " §7(§6" + entry.getValue() + "§7)", ms.getLang("poll.votehover"), "/poll vote " + entry.getKey());
				}
				if (uplayers.contains(p.getName()) == false) {
					p.sendMessage(" ");
					p.sendMessage("  " + ms.getLang("poll.clickoptionchoice"));
				}
			} else if (question == null) {
				p.sendMessage("  §6§o" + ms.getLang("poll.nopollrunning"));
			} else {
				printPoll(p, false);
				p.sendMessage(" ");
				if (pollchoices.isEmpty() == false) {
					for (final String option : pollchoices.keySet()) {
						p.sendMessage("  §8» §f" + option);
					}
				}
			}
			p.sendMessage("");
			if (question == null) p.sendMessage("  §c/poll [" + ms.getLang("poll.question") + "]: §7" + ms.getLang("poll.desc1"));
			if (pollstart == false && pollchoices.size() < 6) p.sendMessage("  §c/poll add [option]: §7" + ms.getLang("poll.desc2"));
			if (pollstart == false && question != null && pollchoices.size() > 1) p.sendMessage("  §c/poll start: §7" + ms.getLang("poll.desc3"));
			if (pollstart) p.sendMessage("  §c/poll stop [-p]: §7" + ms.getLang("poll.desc4"));
			else if (question != null) p.sendMessage("  §c/poll reset: §7Umfrage zurücksetzen");
			if (pollstart == false && question == null) p.sendMessage("  §c/poll history: §7" + ms.getLang("poll.desc5"));
			p.sendMessage(ms.footer((byte) 14, "§6"));
			return true;
		} else if (args[0].equalsIgnoreCase("vote")) {
			if (args.length == 1) {
				ms.showItemBar(p, ms.getLang("poll.error"));
				return true;
			} else if (pollstart == false) {
				ms.showItemBar(p, "§c" + ms.getLang("poll.nopollrunning"));
				return true;
			} else if (uplayers.contains(p.getName())) {
				ms.showItemBar(p, ms.getLang("poll.alreadyvoted"));
				return true;
			}
			final String option = ms.fromArgs(args, 1);
			Short s = pollchoices.get(option);
			if (s == null) {
				ms.showItemBar(p, ms.getLang("poll.optiondoesntexist"));
				return true;
			}
			uplayers.add(p.getName());
			pollchoices.put(option, ++s);
			ms.showItemBar(p, ms.getLang("poll.thanksforvoting"));
			return true;
		} else if (p.isOp() && args[0].equalsIgnoreCase("history")) {
			openInventory(p, 0);
			return true;
		} else if (p.isOp() == false || ms.isTrusted(p.getName()) == false) {
			ms.sendNoCMDMessage(p);
			return true;
		} else if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("reset")) {
			if (pollstart == false) {
				if (question == null) ms.showItemBar(p, "§c" + ms.getLang("poll.nopollrunning"));
				else ms.showItemBar(p, ms.getLang("poll.polldeleted"));
				resetPoll(false);
				return true;
			}
			if (args.length > 1 && args[1].equalsIgnoreCase("-p")) {
				ms.broadcastMessage(ms.header((byte) 12, "§6"), null, Priority.HIGH);
				printPoll(null, true);
				ms.broadcastMessage(" ", null, Priority.HIGH);
				ms.broadcastMessage("  §7" + ms.getLang("poll.results"), null, Priority.HIGH);
				for (final Map.Entry<String, Short> entry : pollchoices.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(e1,e2) -> e1, LinkedHashMap::new)).entrySet()) {
					ms.broadcastMessage("  §8» §f" + entry.getKey() + " §7(§6" + entry.getValue() + "§7)", null, Priority.HIGH);
				}
				ms.broadcastMessage(ms.footer((byte) 12, "§6"), null, Priority.HIGH);
			}
			resetPoll(true);
			return true;
		} else if (pollstart) {
			ms.showItemBar(p, ms.getLang("poll.alreadyrunning"));
			return true;
		} else if (args[0].equalsIgnoreCase("add")) {
			if (args.length == 1) {
				ms.showItemBar(p, ms.usage("/poll add [option]"));
				return true;
			}
			final String option = ms.fromArgs(args, 1);
			if (pollchoices.size() > 5) {
				ms.showItemBar(p, "§cDu kannst nicht mehr als 6 Optionen hinzufügen.");
				return true;
			} else if (question == null) {
				ms.showItemBar(p, "§cStelle zuerst die Frage ein.");
				return true;
			} else if (pollchoices.containsKey(option)) {
				ms.showItemBar(p, ms.getLang("poll.alreadyaddedoption"));
				return true;
			}
			pollchoices.put(option, (short) 0);
			handleCommand(p, new String[0]);
			return true;
		} else if (args[0].equalsIgnoreCase("start")) {
			if (question == null || pollchoices.size() < 2) {
				ms.showItemBar(p, ms.getLang("poll.notsetup"));
				return true;
			}
			time = System.currentTimeMillis();
			ms.getChat().muteEverything();
			ms.broadcastMessage(ms.header((byte) 12, "§6"), null, Priority.NORMAL);
			printPoll(null, true);
			ms.broadcastMessage(" ", null, Priority.NORMAL);
			ms.getServer().getOnlinePlayers().forEach(pl -> {
				for (final String option : pollchoices.keySet()) {
					ms.chatClickHoverRun(pl, "  §8» §f" + option, ms.getLang("poll.votehover"), "/poll vote " + option);
				}
			});
			ms.broadcastMessage(" ", null, Priority.NORMAL);
			ms.broadcastMessage("  " + ms.getLang("poll.clickoptionchoice"), null, Priority.NORMAL);
			ms.broadcastMessage(ms.footer((byte) 12, "§6"), null, Priority.NORMAL);
			pollstart = true;
			ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "POLL", question);
			return true;
		}
		question = ms.fromArgs(args, 0);
		if (question.contains("?") == false) question += "?";
		handleCommand(p, new String[0]);
		return true;
	}

	public void handleInventory(final Player p, final ItemStack i, final int slot, final Inventory inv) {
		final int page = Integer.parseInt(inv.getItem(4).getItemMeta().getDisplayName().split(":")[1]);
		if (i.getType() == Material.SKULL_ITEM && (slot == 7 || slot == 8)) {
			if (slot == 7) {
				p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
				openInventory(p, page - 1);
			} else {
				p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
				openInventory(p, page + 1);
			}
		} else if (i.getType() == Material.ITEM_FRAME) {
			p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
			p.performCommand("a");
		} else {
			p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
		}
	}

	private void openInventory(final Player p, final int page) {
		final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxPolls§0 | /poll");
		new BukkitRunnable() {
			@Override public void run() {
				final List<Object[]> polls = ms.getDB().getPollHistory(500);
				if (polls == null || polls.isEmpty()) {
					inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BARRIER), "§cEs wurden noch keine Umfragen gestellt."));
					return;
				}
				final Pageifier<ItemStack> pages = new Pageifier<>(27);
				for (final Object[] poll : polls) {
					final List<String> lore = new ArrayList<>(getPollQuestion((String) poll[1]));
					if (poll[4] != null) lore.add(0, "  §7Server: §f" + ms.getServers().get((UUID) poll[4]));
					@SuppressWarnings("unchecked") final LinkedHashMap<String, Short> options = (LinkedHashMap<String, Short>) poll[3];
					for (final Map.Entry<String, Short> entry : options.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(e1,e2) -> e1, LinkedHashMap::new)).entrySet()) {
						lore.add("  §8» §f" + entry.getKey() + " §7(§6" + entry.getValue() + "§7)");
					}
					pages.addItem(ms.addLore(ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 4), "§f§lUmfrage §f#§l" + poll[0] + " §7(" + poll[2] + ")"), lore));
				}
				ms.addPageButtons(inv, page, pages);
				inv.setItem(4, ms.renameItemStack(new ItemStack(Material.PUMPKIN_PIE), "§f§lMuxPolls §0Page:" + page, "§7Alle Umfragen, die zuletzt gestellt", "§7wurden, sind komplett aufgelistet."));
			}
		}.runTaskAsynchronously(ms);
		final InvType it = ms.getActiveInv(p.getName());
		inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
		if (it != InvType.POLLS && it != InvType.ADMIN) p.closeInventory();
		p.openInventory(inv);
		ms.setActiveInv(p.getName(), InvType.POLLS);
	}

	private void printPoll(final CommandSender sender, final boolean broadcast) {
		if (broadcast) {
			getPollQuestion(question).forEach(part -> ms.broadcastMessage(part, null, Priority.HIGH));
		} else {
			getPollQuestion(question).forEach(sender::sendMessage);
		}
	}

	private List<String> getPollQuestion(final String question) {
		final List<String> fullquestion = new ArrayList<>();
		fullquestion.add("  §7" + ms.getLang("poll.question2") + ": §6§o" + (question.length() > 28 ? question.substring(0, question.lastIndexOf(" ", 28)) : question));
		if (question.length() > 28) {
			fullquestion.add("  §6§o" + question.substring(question.lastIndexOf(" ", 28)));
		}
		return fullquestion;
	}

	private void resetPoll(final boolean save) {
		pollstart = false;
		uplayers.clear();
		time = 0L;
		if (save) {
			ms.getChat().unmuteEverything();
			ms.getDB().addPollHistory(question, pollchoices);
		}
		pollchoices.clear();
		question = null;
	}

	public boolean isPollRunning() {
		return pollstart;
	}

	public void checkTime() {
		if (pollstart && System.currentTimeMillis() > time + 60000L) resetPoll(true);
	}
}