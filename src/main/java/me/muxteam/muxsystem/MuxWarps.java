package me.muxteam.muxsystem;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxLocation;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.shop.MuxMining;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class MuxWarps {
	private MuxSystem ms;
	private Map<String, Warp> warps = new HashMap<>();
	private final Map<Material, String> warpitems = new EnumMap<>(Material.class);
	private final Random r = new Random();
	private final File warpsfile;
	private final FileConfiguration warpsYML;
	private Inventory warpsinv;

	public MuxWarps(final MuxSystem ms) {
		this.ms = ms;
		final Gson gson = new Gson();
		warpsfile = new File(ms.getDataFolder(), "warps.yml");
		warpsYML = YamlConfiguration.loadConfiguration(warpsfile);
		if (warpsYML.contains("warps")) {
			warps = gson.fromJson(warpsYML.getString("warps"), new TypeToken<HashMap<String, Warp>>() { }.getType());
		}
		setup();
	}

	public void close() {
		final Gson gson = new Gson();
		warpsYML.set("warps", gson.toJson(warps));
		try {
			warpsYML.save(warpsfile);
		} catch (final IOException e) {
			System.err.println("MuxSystem> Error while saving the file warps.yml!");
			e.printStackTrace();
		}
		this.ms = null;
	}

	public Location getWarpLocation(final String warp) {
		final Warp w = warps.get(warp);
		if (w == null) return null;
		return w.getLocation();
	}

	public void checkNoOP() {
		final RegionManager gameworld = WGBukkit.getRegionManager(ms.getGameSpawn().getWorld());
		for (final Player p : ms.getServer().getOnlinePlayers()) {
			final Location l = p.getLocation();
			final int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
			final ProtectedRegion mine = gameworld.getRegionExact("mine");
			if (mine != null && mine.contains(x, y, z)) {
				p.removePotionEffect(PotionEffectType.ABSORPTION);
				p.removePotionEffect(PotionEffectType.REGENERATION);
				p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			}
		}
	}

	public boolean handleCommand(final Player p, final String[] args, final String label) {
		if (ms.inBattle(p.getName(), p.getLocation())) {
			ms.showItemBar(p, ms.hnotinfight);
			return true;
		} else if (p.hasPermission("muxsystem.warps") && label.equalsIgnoreCase("warps")) {
			if (warps.isEmpty()) {
				ms.showItemBar(p, ms.getLang("warps.nowarpsyet"));
				return true;
			}
			p.sendMessage(new String[] {
					ms.header((byte) 15, "§6"),
					"  §6§lWarps§7 (" + warps.size() + ")",
					""
			});
			final IChatBaseComponent comp = ChatSerializer.a("{\"text\":\"" + "\"}");
			short i = 0;
			for (final String swarp : warps.keySet()) {
				final String warp = i++ == 0 ? " §6" + swarp : " §7§l| §6" + swarp;
				comp.addSibling(ChatSerializer.a("{\"text\":\"" + warp.replaceAll("\"", "'").replaceAll("\\\\", "/") + "\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"§6§oTeleportiere zum Warp\"},\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/warp " + swarp + "\"}}"));
			}
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(comp));
			if (ms.isTrusted(p.getName())) {
				p.sendMessage(" ");
				p.sendMessage("  §c/warp del [warp]: §7Warp löschen");
			}
			p.sendMessage(ms.footer((byte) 15, "§6"));
			return true;
		} else if (args.length > 0 && ms.isTrusted(p.getName()) && args[0].equalsIgnoreCase("del")) {
			if (args.length < 2) {
				ms.showItemBar(p, ms.usage("/warp del [warpname]"));
				return true;
			}
			final String warp = args[1].toLowerCase();
			if (warps.get(warp) == null) {
				ms.showItemBar(p, ms.getLang("warps.doesnotexist"));
				return true;
			}
			warps.remove(warp);
			ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "DELWARP", warp);
			ms.showItemBar(p, "§fWarp '§6" + warp + "§f' wurde §cgelöscht§f.");
			return true;
		}
		final long start = System.currentTimeMillis();
		final MuxUser u = ms.getMuxUser(p.getName());
		final String warp = args.length == 0 ? null : args[0].toLowerCase();
		if (ms.hasVoted(p) == false && (warp == null || (warp.equals("shop") && u.getOnlineTime() > 7200000L)
				|| (warp.equals("essen") == false && warp.startsWith("info") == false && warp.equals("tools") == false && warp.equals("base") == false && warp.equals("rang") == false))) {
			ms.sendNoVoteMessage(p);
			return true;
		} else if (warp == null) {
			final InvType it = ms.getActiveInv(p.getName());
			if (it != InvType.WARPS) p.closeInventory();
			p.openInventory(warpsinv);
			ms.setActiveInv(p.getName(), InvType.WARPS);
			return true;
		}
		final Location l = getWarpLocation(warp);
		String warpmsg = "Warp '§6" + warp + "§f'";
		switch (warp) {
			case "shop":
				warpmsg = "§6Shop";
				ms.getNewbies().handleNewbieShop(p);
				break;
			case "spawn":
				warpmsg = "§6Spawn";
				break;
			case "nether":
				warpmsg = "§6Nether";
				ms.getNewbies().handleNewbieNether(p);
				break;
			default:
				break;
		}
		ms.getAnalytics().getAnalytics().addWarpUse(warp);
		if (l == null) {
			ms.showItemBar(p, ms.getLang("warps.doesnotexist"));
			return true;
		} else if (p.hasPermission("muxsystem.notpcooldown")) {
			ms.setLastLocation(p);
			ms.teleportPlayer(p, l);
			ms.showItemBar(p, String.format(ms.getLang("teleportedto"), warpmsg));
		} else if (ms.startTPCountdown(p, l)) {
			ms.showItemBar(p, String.format(ms.getLang("teleportingto"), warpmsg));
		}
		System.out.println("took " + (System.currentTimeMillis() - start) + "ms");
		return true;
	}

	public boolean handleSetWarp(final Player p, final String[] args) {
		if (ms.isTrusted(p.getName()) == false) {
			ms.sendNoCMDMessage(p);
			return true;
		} else if (args.length != 1) {
			ms.showItemBar(p, ms.usage("/setwarp [warpname]"));
			return true;
		}
		final String warp = args[0].toLowerCase();
		if (warp.length() > 50) {
			ms.showItemBar(p, ms.getLang("warps.nametoolong"));
			return true;
		} else if (ms.notSafeGSON(warp)) {
			ms.showItemBar(p, "§cDer Warp darf keine spezielle Zeichen enthalten.");
			return true;
		}
		p.playSound(p.getLocation(), Sound.PISTON_EXTEND, 1F, 1F);
		warps.put(warp, new Warp(p.getLocation()));
		ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "SETWARP", warp);
		ms.showItemBar(p, "§fWarp '§6" + warp + "§f' wurde §agesetzt§f.");
		return true;
	}

	final Map<UUID, Location> randomtps = new HashMap<>(), randomnethertps = new HashMap<>();

	public boolean handleRTPCommand(final Player p) {
		if (ms.getAdmin().FARMWORLD.isActive() == false && p.getWorld().getEnvironment() != World.Environment.NETHER) {
			ms.showItemBar(p, "§cDie Wildnis ist temporär deaktiviert.");
			return true;
		}
		if (ms.checkLimit(p.getName(), (p.getWorld().getEnvironment() == World.Environment.NETHER ? "RANDOMNETHERTELEPORT" : "RANDOMTELEPORT"), 1, false) == false) {
			ms.getWarps().handleRandomTP(p);
		} else {
			if (ms.checkGeneralCooldown(p.getName(), "RANDOMTP", 2000L, true)) {
				ms.showItemBar(p, ms.getLang("notsofast"));
				return true;
			}
			final long activeMuxCoinsSupply = ms.getActiveMuxCoinsSupply();
			final int price = (int) (activeMuxCoinsSupply * MuxMining.OreTypes.DIAMOND.getPercentage() / 100) * 50;
			new ConfirmInventory(ms, pl -> ms.getWarps().handleRandomTP(pl), pl -> {
				Location l;
				if (p.getWorld().getEnvironment() == World.Environment.NETHER) {
					l = randomnethertps.get(p.getUniqueId());
				} else {
					l = randomtps.get(p.getUniqueId());
				}
				ms.setLastLocation(p);
				ms.teleportPlayer(p, l);
				ms.getHistory().addHistory(p.getUniqueId(), null, "TELEPORT", "RTP", ms.blockLocationToStringNoYawPitch(l));
				p.playSound(p.getLocation(), Sound.PORTAL_TRIGGER, 0.2F, 1F);
				ms.showItemBar(p, "§fDu wurdest weit weg §6teleportiert§f.");
				p.sendMessage("§6§lMuxCraft>§7 Um wieder zurückzukehren, benutze dein §f§lMenü§7.");
			}).setAbortColor((byte) 9).show(p, "§0§lRandom Teleport", "§aBestätigen (" + ms.getNumberFormat(price) + " MuxCoins)", "§7Zum heutigem Ort");
		}
		return true;
	}

	public void handleRandomTP(final Player p) {
		final MuxUser u = ms.getMuxUser(p.getName());
		final long activeMuxCoinsSupply = ms.getActiveMuxCoinsSupply();
		int price = (int) (activeMuxCoinsSupply * MuxMining.OreTypes.DIAMOND.getPercentage() / 100) * 50;
		final boolean nether = p.getWorld().getEnvironment() == World.Environment.NETHER;
		final Location gamespawn = nether ? ms.getNetherSpawn() : ms.getFarmWorld().getSpawnLocation();
		if (ms.checkLimit(p.getName(), nether ? "RANDOMNETHERTELEPORT" : "RANDOMTELEPORT", 1, true) == false) {
			price = 0;
		}
		if (u.getCoins() < price) {
			ms.showItemBar(p, ms.getLang("market.notenough"));
			ms.chatClickHoverLink(p, ms.getLang("shop.clickformuxcoins"), ms.getLang("shop.hoverformuxcoins"), "https://shop.muxcraft.eu/?ign=" + p.getName());
		} else {
			Material m = Material.WATER, centerm = Material.BEDROCK;
			Block down = null;
			int x = 0, z = 0;
			while (m == Material.WATER || m == Material.STATIONARY_WATER || m == Material.LAVA || m == Material.STATIONARY_LAVA || m == Material.BEDROCK || m == Material.GLOWSTONE || centerm == Material.LAVA || centerm == Material.STATIONARY_LAVA || centerm.isSolid()) {
				x = (r.nextBoolean() ? 1 : -1) * (r.nextInt(7500) + 8500 + 300);
				z = (r.nextBoolean() ? 1 : -1) * (r.nextInt(8500) + 4500 + 1000);
				final Block b = gamespawn.getWorld().getHighestBlockAt(x, z);
				centerm = b.getType();
				down = b.getRelative(BlockFace.DOWN);
				m = down.getType();
			}
			final Location l = new Location(gamespawn.getWorld(), x + 0.5, down.getLocation().getBlockY() + 2.5, z + 0.5);
			l.getWorld().refreshChunk(l.getChunk().getX(), l.getChunk().getZ());
			u.addCoins(-price);
			ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(-price), "RTP");
			ms.sendScoreboard(p);
			ms.setLastLocation(p, ms.getGameSpawn());
			ms.teleportPlayer(p, l);
			if (nether) randomnethertps.put(p.getUniqueId(), l);
			else randomtps.put(p.getUniqueId(), l);
			ms.getHistory().addHistory(p.getUniqueId(), null, "TELEPORT", "RTP", ms.blockLocationToStringNoYawPitch(l));
			p.playSound(p.getLocation(), Sound.PORTAL_TRIGGER, 0.2F, 1F);
			ms.showItemBar(p, "§fDu wurdest weit weg §6teleportiert§f.");
			if (u.getHomes().size() == 0) p.performCommand("sethome");
			else p.sendMessage("§6§lMuxCraft>§7 Um wieder zurückzukehren, benutze dein §f§lMenü§7.");
		}
	}

	public void handleInventory(final Player p, final ItemStack i) {
		final Material m = i.getType();
		final ItemMeta im = i.getItemMeta();
		final String warp = warpitems.get(m);
		if (warp != null) {
			p.closeInventory();
			p.performCommand("warp " + warp);
		} else if (m == Material.COMPASS && im.getLore().size() > 2) {
			p.performCommand("rtp");
			p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
		} else if (m == Material.ITEM_FRAME) {
			p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
			p.openInventory(warpsinv);
			ms.setActiveInv(p.getName(), InvType.WARPS);
		} else {
			p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
		}
	}

	private void setup() {
		warpitems.put(Material.NETHER_BRICK, "nether");
		warpitems.put(Material.BEACON, "spawn");
		warpitems.put(Material.SIGN, "infos");
		warpitems.put(Material.CHEST, "kisten");
		warpitems.put(Material.DOUBLE_PLANT, "shop");

		warpsinv = ms.getServer().createInventory(null, 45, "§0§lMuxWarps§0 | /warp");
		warpsinv.setItem(11, ms.renameItemStack(new ItemStack(Material.NETHER_BRICK), "§f§lNether", "§7Kämpfe hier oder baue deine", "§7Basis weit weg vom Spawn auf.", "", "§fKlicke§7, um dich zu teleportieren."));
		warpsinv.setItem(15, ms.renameItemStack(new ItemStack(Material.SIGN), "§f§lInfos", "§7Die Befehle und der Kontakt zum", "§7Team befinden sich auch hier.", "", "§fKlicke§7, um dich zu teleportieren."));
		warpsinv.setItem(29, ms.renameItemStack(new ItemStack(Material.CHEST), "§f§lKisten", "§7Erhalte täglich kostenlos Items", "§7oder MuxCoins von den Kisten.", "", "§fKlicke§7, um dich zu teleportieren."));
		warpsinv.setItem(22, ms.renameItemStack(new ItemStack(Material.BEACON), "§f§lSpawn", "§7Hiermit gelangst du zu dem", "§7Hauptpunkt des Servers.", "", "§fKlicke§7, um dich zu teleportieren."));
		warpsinv.setItem(33, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§f§lShop", "§7Kaufe nahezu alle Items dieses", "§7Servers hier mit MuxCoins ein.", "", "§fKlicke§7, um dich zu teleportieren."));
		warpsinv.setItem(40, ms.renameItemStack(new ItemStack(Material.COMPASS), "§f§lWildnis", "§7Hiermit gelangst du zur Wildnis", "§7an einer zufälligen Position.", "", "§fKlicke§7, um dich zu teleportieren."));
	}

	final class Warp extends MuxLocation {
		public Warp(final Location l) {
			super(l);
		}
	}
}