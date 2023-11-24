package me.muxteam.pvp;

import com.sk89q.worldguard.bukkit.WGBukkit;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxSystem.Priority;
import me.muxteam.pvp.MuxPvPBots.PvPBot;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuxGames {
	private MuxSystem ms;
	private final Map<String, Integer> backexp = new HashMap<>(), backfood = new HashMap<>();
	private final Map<String, ItemStack[]> backarmor = new HashMap<>(), backinv = new HashMap<>();
	private final Map<Material, GameType> gameitems = new EnumMap<>(Material.class);
	private final Map<GameType, Game> games = new EnumMap<>(GameType.class);

	public MuxGames(final MuxSystem ms) {
		this.ms = ms;
		games.put(GameType.TRAINING, new MuxTraining());
		loadLocs();
	}
	public void close() {
		for (final Game g : games.values()) {
			g.stopGame(false, false);
		}
		this.ms = null;
	}
	public void loadLocs() {
		for (final Game g : games.values()) {
			g.loadLocs();
		}
	}
	public boolean handleGames(final Player p, final String[] args) {
		if (args.length > 0 && args[0].toLowerCase().startsWith("leave")) {
			GameType gt = inGame(p);
			if (gt == null) gt = inQueue(p);
			if (gt != null) {
				getGame(gt).leaveGame(p , true);
				return true;
			}
			ms.showItemBar(p, "§cDu bist derzeit in keinem Spiel.");
			return true;
		}
		ms.getGames().getGame(GameType.TRAINING).joinGame(p);
		return true;
	}
	public Game getGame(final GameType type) {
		return games.get(type);
	}
	private void startGame(final Player p, final GameType g) {
		games.get(g).startGame(p);
	}
	public void stopGame(final GameType g, final boolean force, final boolean extra) {
		games.get(g).stopGame(force, extra);
	}
	public void joinGame(final Player p, final GameType g) {
		if (g != GameType.TRAINING && ms.checkGeneralCooldown(p.getName(), "GAMECHEAT", 1800000L, false)) {
			p.closeInventory();
			ms.showItemBar(p,"§cDu bist temporär gesperrt. Versuche es später wieder.");
			return;
		}
		games.get(g).joinGame(p);
	}
	public void leaveActiveGame(final Player p) {
		games.values().forEach(g -> g.leaveGame(p, false));
	}
	public GameType inGame(final Player p) {
		for (final Map.Entry<GameType, Game> entry : games.entrySet()) {
			if (entry.getValue().inGame(p)) return entry.getKey();
		}
		return null;
	}
	private GameType inQueue(final Player p) {
		for (final Map.Entry<GameType, Game> entry : games.entrySet()) {
			if (entry.getValue().inQueue(p)) return entry.getKey();
		}
		return null;
	}
	private void equip(final GameType g, final Player p) {
		p.closeInventory();
		final PlayerInventory pi = p.getInventory();
		games.get(g).equipPlayer(p, pi);
	}
	public void checkTime() {
		games.values().forEach(Game::checkTime);
	}

	public void punishCheater(final Player p) {
		if (ms.checkLimit(p.getName(), "GAMECHEAT", 2, true)) {
			ms.checkGeneralCooldown(p.getName(), "GAMECHEAT", 1800000L, true);
		}
	}

	/***
	 * EVENTS
	 ***/

	public boolean quitEvent(final Player p) {
		for (final Game g : games.values()) {
			if (g.leaveGame(p, true)) return true;
		}
		return false;
	}
	public boolean damageEvent(final Player p, final EntityDamageEvent e) {
		for (final Game g : games.values()) {
			if (g.damageEvent(p, e)) return true;
		}
		return false;
	}
	public boolean combustEvent(final Player p) {
		for (final Game g : games.values()) {
			if (g.combustEvent(p)) return true;
		}
		return false;
	}
	public void moveEvent(final Player p, final Material m) {
		for (final Game g : games.values()) {
			g.moveEvent(p, m);
		}
	}
	public void projectileEvent(final Projectile en) {
		for (final Game g : games.values()) {
			g.projectileEvent(en);
		}
	}

	/***
	 * GAMES
	 ***/

	public abstract class Game {
		boolean open = false;
		boolean started = false;
		//long gamestart = 0L;
		List<String> queue = new ArrayList<>();
		ConcurrentHashMap<String, GamePlayer> players = new ConcurrentHashMap<>();
		public boolean isOpen() {
			return open;
		}
		public void setOpen(final boolean open) {
			this.open = open;
		}
		boolean isStarted() {
			return started;
		}
		void broadcastGame(final String msg) {
			for (final String s : players.keySet()) {
				final Player pl = Bukkit.getPlayer(s);
				pl.sendMessage(msg);
			}
		}
		boolean inQueue(final Player p) {
			return queue.contains(p.getName());
		}
		void loadLocs() {}
		void startGame(final Player p) {}
		void stopGame(final boolean force, final boolean extra) {}
		void joinGame(final Player p) {}
		boolean leaveGame(final Player p, final boolean cmd) { return false; }
		boolean inGame(final Player p) { return false; }
		boolean damageEvent(final Player p, final EntityDamageEvent e) { return false; }
		boolean combustEvent(final Player p) { return false; }
		void moveEvent(final Player p, final Material m) {}
		void projectileEvent(final Projectile e) {}
		void resetPlayer(final String name, final boolean back) {}
		void equipPlayer(final Player p, final PlayerInventory pi) {}
		void checkTime() {}
	}
	public class GamePlayer {
		long time;
		Team team;

		GamePlayer(final long time, final Team team) {
			this.time = time;
			this.team = team;
		}
		public void setTime(final long time) {
			this.time = time;
		}
		public long getTime() {
			return time;
		}
		public Team getTeam() {
			return team;
		}
	}
	class MuxTraining extends Game {
		private final Set<Player> gamers = new HashSet<>();
		private final Map<UUID, Integer> levelcache = new HashMap<>();
		private final ConcurrentHashMap<Player, PvPBot> bots = new ConcurrentHashMap<>();
		private Location trainspawn1, trainspawn2;

		public MuxTraining() {
			gameitems.put(Material.LEATHER_CHESTPLATE, GameType.TRAINING);
		}
		@Override
		public boolean leaveGame(final Player p, final boolean cmd) {
			if (gamers.contains(p)) {
				if (cmd) ms.showItemBar(p, "§fDu hast das Training §cverlassen§f.");
				resetPlayer(p.getName(), true);
				return true;
			}
			return false;
		}
		@Override
		public boolean inGame(final Player p) {
			return gamers.contains(p);
		}
		@Override
		public void joinGame(final Player p) {
			ms.getNewbies().handleNewbieTraining(p);
			final String name = p.getName();
			if (ms.isBeingTeleported(name)) {
				ms.showItemBar(p, "§cDu wirst gerade teleportiert.");
			} else if (ms.getGames().inGame(p) != null) {
				ms.showItemBar(p, "§cDu bist bereits in einem Spiel.");
			} else if (open == false || WGBukkit.getPlugin().getGlobalStateManager().activityHaltToggle) {
				ms.showItemBar(p, "§cTraining ist im moment nicht betretbar.");
			} else if (gamers.contains(p)) {
				ms.showItemBar(p, "§cDu bist bereits im Training.");
			} else  if (p.hasPermission("muxsystem.vip") == false && ms.isVPN(p.getAddress().getAddress().getHostAddress()) == 1) {
				ms.sendNoVPNMessage(p, "VIP");
			} else {
				gamers.add(p);
				ms.getMounts().deactivateMount(p);
				ms.getEmojis().stopEmoji(p);
				if (p.isInsideVehicle()) {
					p.leaveVehicle();
				}
				p.teleport(trainspawn1);
				ms.showItemBar(p, "§fDu wurdest zum §6Training §fteleportiert.");
				final int level = getTrainingLevel(p.getUniqueId());
				ms.sendNormalTitle(p, "§f§lLevel " + level + " (" + getTrainingDifficulty(level) + "§f§l)", 10, 30, 10);
				ms.getPerks().activatePerk(p, (byte) 1);
				trainspawn2.getChunk().load();
				final PvPBot bot = ms.getPvPBots().addPvPBot(p, trainspawn2, getTrainingLevel(p.getUniqueId()));
				ms.getPvPBots().blockPotionPackets(p);
				bots.put(p, bot);
				final Location target = new Location(trainspawn1.getWorld(), (trainspawn1.getX() + trainspawn2.getX()) / 2,
						(trainspawn1.getY() + trainspawn2.getY()) / 2, (trainspawn1.getZ() + trainspawn2.getZ()) / 2);
				bot.setTarget(target, 0.2F, true);
				equip(GameType.TRAINING, p);
				ms.getPerks().activatePerk(p, (byte) 4);
				for (final Player pl : gamers) {
					p.hidePlayer(pl);
					pl.hidePlayer(p);
				}
				p.sendMessage("§6§lTraining>§7 Du kämpfst nun gegen den §cMuxBot§7. Viel Glück!");
			}
		}
		@Override
		public void startGame(final Player p) {
			if (open) {
				ms.showItemBar(p, "§cMuxTraining ist bereits gestartet.");
				return;
			} else if (trainspawn1 == null || trainspawn2 == null) {
				ms.showItemBar(p, "§cDie Positionen wurden noch nicht gesetzt.");
				return;
			}
			ms.broadcastMessage("§6§lTraining>§7 Training ist wieder §abetretbar§7. §6§l/training", null, Priority.NORMAL);
			open = true;
		}
		@Override
		public void stopGame(final boolean force, final boolean extra) {
			if (force) {
				open = false;
				for (final Player p : ms.getServer().getOnlinePlayers()) {
					if (p.isOp() || gamers.contains(p)) {
						p.sendMessage("§6§lTraining>§7 Das Spiel wurde von einem Admin §cabgebrochen§7.");
					}
				}
			}
			for (final Player p : gamers) {
				resetPlayer(p.getName(), true);
			}
			gamers.clear();
		}
		@Override
		public void equipPlayer(final Player p, final PlayerInventory pi) {
			final String pname = p.getName();
			backinv.put(pname, pi.getContents());
			backarmor.put(pname, pi.getArmorContents());
			backexp.put(pname, p.getLevel());
			backfood.put(pname, p.getFoodLevel());
			for (final PotionEffect pe : p.getActivePotionEffects()) {
				p.removePotionEffect(pe.getType());
			}
			pi.clear();
			pi.setArmorContents(null);
			p.setExp(0F);
			p.setLevel(0);
			p.setHealth(20D);
			p.setFoodLevel(20);
			if (p.getFireTicks() > 0) {
				new BukkitRunnable() {
					@Override
					public void run() {
						if (p.isOnline())
							p.setFireTicks(0);
					}
				}.runTaskLater(ms, 1L);
			}
			p.setAllowFlight(false);
			p.setFlying(false);
			p.setGameMode(GameMode.ADVENTURE);
			ms.removeGodMode(pname);
			final ItemStack iH = new ItemStack(Material.DIAMOND_HELMET), iC = new ItemStack(Material.DIAMOND_CHESTPLATE), iL = new ItemStack(Material.DIAMOND_LEGGINGS), iB = new ItemStack(Material.DIAMOND_BOOTS),
					iS = new ItemStack(Material.DIAMOND_SWORD);
			pi.setHelmet(iH);
			pi.setChestplate(iC);
			pi.setLeggings(iL);
			pi.setBoots(iB);
			pi.addItem(iS);
			pi.addItem(new ItemStack(Material.POTION, bots.get(p).getHealPots(), (short) 16421));
			p.updateInventory();
		}
		@Override
		public void resetPlayer(final String name, final boolean back) {
			final Player p = ms.getServer().getPlayer(name);
			final PvPBot bot = bots.remove(p);
			bot.removeFromTablist(p);
			bot.despawn(p);
			bot.delete();
			ms.getPvPBots().unblockPotionPackets(p);
			final PlayerInventory pi = p.getInventory();
			if (p.isInsideVehicle()) {
				p.leaveVehicle();
			}
			p.teleport(ms.getGameSpawn());
			p.setHealth(20D);
			p.setFoodLevel(20);
			if (p.getFireTicks() > 0) {
				new BukkitRunnable() {
					@Override
					public void run() {
						if (p.isOnline())
							p.setFireTicks(0);
					}
				}.runTaskLater(ms, 1L);
			}
			if (back) {
				final ItemStack[] inv = backinv.get(name);
				final Integer level = backexp.get(name);
				p.setLevel(level == null ? 0 : level);
				pi.setArmorContents(backarmor.get(name));
				pi.setContents(inv == null ? new ItemStack[0] : inv);
				p.setFoodLevel(backfood.get(name));
				backarmor.remove(name);
				backinv.remove(name);
				backexp.remove(name);
				backfood.remove(name);
			}
			p.setGameMode(GameMode.SURVIVAL);
			ms.getPerks().deactivatePerks(name);
			p.getActivePotionEffects().forEach(pe -> p.removePotionEffect(pe.getType()));
			ms.getPerks().activatePerks(p);
			for (final Player pl : gamers) {
				pl.showPlayer(p);
				p.showPlayer(pl);
			}
			gamers.remove(p);
		}
		@Override
		public boolean combustEvent(final Player p) {
			if (gamers.contains(p)) {
				if (p.getHealth() <= 2D) {
					loseTraining(p, true);
					return true;
				}
			}
			return false;
		}
		@Override
		public boolean damageEvent(final Player p, final EntityDamageEvent e) {
			if (gamers.contains(p)) {
				if (e == null) {
					loseTraining(p, false);
				} else if (p.getHealth() - e.getFinalDamage() <= 0D) {
					e.setCancelled(true);
					loseTraining(p, true);
				}
				return true;
			}
			return false;
		}
		@Override
		public void loadLocs() {
			open = (trainspawn1 = ms.getLoc("trainspawn1")) != null && (trainspawn2 = ms.getLoc("trainspawn2")) != null;
		}
		private String getTrainingDifficulty(final int level) {
			if (level < 5) return "§aeinfach";
			else if (level < 10) return "§enormal";
			else if (level < 20) return "§6schwer";
			else if (level <= 30) return "§cextrem";
			return "§f?";
		}
		private void loseTraining(final Player p, final boolean lose) {
			resetPlayer(p.getName(), true);
			if (lose == false) {
				ms.sendTitle(p, "§6§lTraining", 10, 120, 10);
				ms.sendSubTitle(p, "§fGlückwunsch, du hast §agewonnen§f!", 10, 60, 10);
				p.playSound(p.getLocation(), Sound.FIREWORK_BLAST, 1F, 1F);
				setTrainingLevel(p.getUniqueId(), 1);
			} else {
				setTrainingLevel(p.getUniqueId(), -1);
				new BukkitRunnable() {
					@Override public void run() {
						ms.showItemBar(p, "§cSchade, du hast leider verloren.");
					}
				}.runTaskLater(ms, 20L);
			}
			ms.runLater(new BukkitRunnable() {
				@Override public void run() {
					ms.chatClickHoverRun(p, "§6§lTraining>§7 Klicke §6hier§7, um nochmal zu kämpfen.", "§6§oKlicke zum Trainieren", "/training");
				}}, 40L);
		}
		private void setTrainingLevel(final UUID uuid, final int level) {
			int current = levelcache.get(uuid);
			current += level;
			if (current < 0) current = 0;
			else if (current > 30) current = 30;
			levelcache.put(uuid, current);
			new BukkitRunnable() {
				@Override public void run() {
					ms.getDB().setTrainingLevel(uuid, level, 30);
				}
			}.runTaskAsynchronously(ms);
		}
		private int getTrainingLevel(final UUID uuid) {
			final Integer cached = levelcache.get(uuid);
			if (cached != null) {
				return cached;
			}
			final int level = ms.getDB().getTrainingLevel(uuid);
			levelcache.put(uuid, level);
			return level;
		}
	}
	public enum GameType {
		TRAINING
	}
	public enum Team {
		BLUE, RED
	}
}