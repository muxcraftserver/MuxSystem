package me.muxteam.holidays;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxSystem.Priority;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.shop.MuxMining;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public final class MuxAdvent {
	private MuxSystem ms;
	private final Random r = new Random();
	private final Location calendartree;
	private List<UUID> aplayers = new ArrayList<>();
	private List<String> aplayersip = new ArrayList<>();

	public MuxAdvent(final MuxSystem ms) {
		this.ms = ms;
		final Gson gson = new Gson();
		final FileConfiguration hashYML = ms.getHashYAML();
		calendartree = new Location(ms.getGameSpawn().getWorld(), -23D, 72D, 0D);
		if (hashYML.contains("advent")) {
			aplayers = gson.fromJson(hashYML.getString("advent"), new TypeToken<ArrayList<UUID>>() { }.getType());
			if (hashYML.contains("adventip")) {
				aplayersip = gson.fromJson(hashYML.getString("adventip"), new TypeToken<ArrayList<String>>() { }.getType());
			}
		}
	}
	public void close() {
		final Gson gson = new Gson();
		final FileConfiguration hashYML = ms.getHashYAML();
		hashYML.set("advent", gson.toJson(aplayers));
		hashYML.set("adventip", gson.toJson(aplayersip));
		this.ms = null;
	}
	public void nextDay(final short day) {
		if (Calendar.getInstance(TimeZone.getTimeZone("CET")).get(Calendar.MONTH) == Calendar.DECEMBER && day < 25) {
			aplayers.clear();
			aplayersip.clear();
			ms.broadcastMessage("§6§lAdvent>§7 Ein neues Adventstürchen steht zur Verfügung!", null, Priority.HIGH);
		}
	}
	public void pushAway() {
		final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("CET"));
		if (cal.get(Calendar.MONTH) != Calendar.DECEMBER || cal.get(Calendar.DAY_OF_MONTH) > 25) return;
		for (final Player p : ms.getServer().getOnlinePlayers()) {
			if (p.getWorld().equals(calendartree.getWorld()) && p.getLocation().distance(calendartree) < 2D) {
				final Vector v = ms.getGameSpawn().toVector().subtract(calendartree.toVector());
				p.setVelocity(v.normalize().multiply(1.2D));
			}
		}
	}
	public void handleSign(final Player p, final Sign sign, final String[] lines) {
		final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("CET"));
		if (cal.get(Calendar.MONTH) != Calendar.DECEMBER) return;
		final Location loc = sign.getLocation();
		if (loc.getWorld().getName().equals(ms.getGameSpawn().getWorld().getName()) == false || loc.distance(ms.getGameSpawn()) > 70D) return;
		int day;
		try {
			day = Integer.parseInt(ChatColor.stripColor(lines[2]));
		} catch (final NumberFormatException ex) {
			return;
		}
		if (day > 0 && day < 28) {
			final int realday = cal.get(Calendar.DAY_OF_MONTH);
			final EntityPlayer cp = ((CraftPlayer) p).getHandle();
			if (realday == day || realday == 25) {
				final UUID uuid = p.getUniqueId();
				final MuxUser u = ms.getMuxUser(p.getName());
				if (aplayers.contains(uuid) || aplayersip.contains(p.getAddress().getAddress().getHostAddress())) {
					lines[0] = "§4Du hast";
					lines[1] = "§4das Türchen";
					lines[2] = "§4bereits";
					lines[3] = "§4geöffnet!";
				} else if (realday > 1 && ms.hasVoted(p) == false) {
					lines[0] = "§4Du musst";
					lines[1] = "§4dafür";
					lines[2] = "§4voten!";
					lines[3] = "§5§l/vote";
				} else if (realday > 23 && u.getOnlineTime() < 1800000L) {
					lines[0] = "§4Zweitaccounts";
					lines[1] = "§4sind";
					lines[2] = "§4nicht";
					lines[3] = "§4erlaubt!";
				} else if (p.getInventory().firstEmpty() == -1) {
					lines[0] = "§4Dein";
					lines[1] = "§4Inventar";
					lines[2] = "§4ist";
					lines[3] = "§4voll!";
				} else {
					new BukkitRunnable() {
						@Override public void run() {
							giveReward(p, uuid, u, lines, day, loc);
							final net.minecraft.server.v1_8_R3.World world = ((CraftWorld) sign.getWorld()).getHandle();
							final BlockPosition bs = new BlockPosition(sign.getX(), sign.getY(), sign.getZ());
							cp.playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutUpdateSign(world, bs, ms.getComponentFromSignLines(lines)));
						}
					}.runTaskAsynchronously(ms);
				}
			} else if (realday > 25) {
				lines[0] = "§4Weihnachten";
				lines[1] = "§4ist vorbei!";
				lines[2] = "";
				lines[3] = "§4Frohes Fest!";
			} else if (realday > day) {
				lines[0] = "§4Oops!";
				lines[1] = "§4Der Tag ist";
				lines[2] = "§4bereits";
				lines[3] = "§4vorbei!";
			} else {
				lines[1] = "§4Nicht";
				lines[2] = "§4schummeln!";
			}
			final net.minecraft.server.v1_8_R3.World world = ((CraftWorld) sign.getWorld()).getHandle();
			final BlockPosition bs = new BlockPosition(sign.getX(), sign.getY(), sign.getZ());
			cp.playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutUpdateSign(world, bs, ms.getComponentFromSignLines(lines)));
			final int fday = day;
			new BukkitRunnable() {
				@Override public void run() {
					lines[0] = "";
					lines[1] = "§1§l[Türchen]";
					lines[2] = "§l" + fday;
					lines[3] = "";
					cp.playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutUpdateSign(world, bs, ms.getComponentFromSignLines(lines)));
				}
			}.runTaskLater(ms, 60L);
		}
	}
	private void giveReward(final Player p, final UUID uuid, final MuxUser u, final String[] lines, final int day, final Location loc) {
		if (ms.isVPN(p.getAddress().getAddress().getHostAddress()) > 0) {
			lines[0] = "§4VPNs";
			lines[1] = "§4sind";
			lines[2] = "§4nicht";
			lines[3] = "§4erlaubt!";
			return;
		} else if (aplayers.contains(uuid) || aplayersip.contains(p.getAddress().getAddress().getHostAddress())) {
			lines[0] = "§4Du hast";
			lines[1] = "§4das Türchen";
			lines[2] = "§4bereits";
			lines[3] = "§4geöffnet!";
			return;
		}
		aplayers.add(uuid);
		aplayersip.add(p.getAddress().getAddress().getHostAddress());
		final LocalDateTime current = LocalDateTime.now(ZoneId.of("CET"));
		if (current.getDayOfWeek().getValue() == DayOfWeek.SUNDAY.getValue() && day < 25) {
			ms.getChests().handleCommand(ms.getServer().getConsoleSender(), new String[] { "add", p.getName(), "ADVENT"});
			ms.showItemBar(p, "§fDu hast eine §6Adventskiste §ferhalten.");
		}
		if (day == 1) {
			final ItemStack hot = ms.renameItemStack(new ItemStack(Material.REDSTONE_TORCH_ON), "§4§lTAG 1§r §f❄ §a§lHeiße Fackel");
			hot.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 3);
			hot.addUnsafeEnchantment(Enchantment.KNOCKBACK, 5);
			hot.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 5);
			p.getInventory().addItem(hot);
			final ItemStack dore = new ItemStack(Material.DIAMOND_ORE, 8);
			p.getInventory().addItem(dore);
		} else if (day == 2) {
			final ItemStack bedrock = ms.renameItemStack(new ItemStack(Material.BEDROCK, 16), "§4§lTAG 2§r §f❄ §a§lWeihnachtsbedrock");
			p.getInventory().addItem(bedrock);
		} else if (day == 3) {
			final ItemStack rod = ms.renameItemStack(new ItemStack(Material.FISHING_ROD), "§4§lTAG 3§r §f❄ §a§lMuxAngel");
			rod.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
			rod.addUnsafeEnchantment(Enchantment.LUCK, 5);
			rod.addUnsafeEnchantment(Enchantment.LURE, 3);
			final ItemStack creeper = ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 1, (byte) 4), "§4§lTAG 3§r §f❄ §a§lMuxCreeper");
			p.getInventory().addItem(rod, creeper);
			final ItemStack heal = ms.renameItemStack(new ItemStack(Material.POTION, 64, (short) (37 | 0x4000)), "§4§lTAG 3§r §f❄ §a§lHeilungstrank", "", "§7Potion Pack I");
			p.getInventory().addItem(heal, heal, heal);
		} else if (day == 4) {
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.GOLDEN_APPLE, 4, (byte) 1), "§4§lTAG 4§r §f❄ §a§lOP Äpfel"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.POTION, 64, (short) (36 | 0x4000)), "§4§lTAG 4§r §f❄ §a§lGifttrank", "", "§7Potion Pack I"));
		} else if (day == 5) {
			final ItemStack snowball = ms.renameItemStack(new ItemStack(Material.SNOW_BALL, 16), "§4§lTAG 5§r §f❄ §a§lWeihnachtsball");
			p.getInventory().addItem(snowball, snowball, snowball, snowball);
		} else if (day == 6) {
			final ItemStack boots = ms.leatherize(ms.renameItemStack(new ItemStack(Material.LEATHER_BOOTS), "§4§lOUTFIT§r §f❄ §a§lWarme Weihnachtspantoffeln"), Color.WHITE);
			boots.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 10);
			boots.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
			p.getInventory().addItem(boots);
		} else if (day == 7) {
			final ItemStack pick = ms.renameItemStack(new ItemStack(Material.DIAMOND_PICKAXE), "§4§lTAG 7§r §8❄ §a§lWeihnachtsspitzhacke");
			pick.addEnchantment(Enchantment.DIG_SPEED, 6);
			pick.addEnchantment(Enchantment.DURABILITY, 2);
			pick.addEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 1);
			p.getInventory().addItem(pick);
		} else if (day == 8) {
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.POTION, 64, (short) (67 | 0x4000)), "§4§lTAG 8§r §8❄ §a§lResistenztrank", "", "§7Potion Pack II"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.POTION, 64, (short) (33 | 0x4000)), "§4§lTAG 8§r §8❄ §a§lRegenerationstrank", "", "§7Potion Pack II"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.POTION, 64, (short) (41 | 0x4000)), "§4§lTAG 8§r §8❄ §a§lStärketrank", "", "§7Potion Pack II"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.POTION, 64, (short) (34 | 0x4000)), "§4§lTAG 8§r §8❄ §a§lSchnelligkeitstrank", "", "§7Potion Pack II"));
		} else if (day == 9) {
			final ItemStack cloak = ms.leatherize(ms.renameItemStack(new ItemStack(Material.LEATHER_CHESTPLATE), "§4§lOUTFIT§r §8❄ §a§lWarmer Weihnachtsmantel"), Color.WHITE);
			cloak.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 10);
			cloak.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
			p.getInventory().addItem(cloak);
		} else if (day == 10) {
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.CHEST, 32), "§4§lTAG 10§r §8❄ §a§lKiste", "§7Base Pack I"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.ANVIL, 8), "§4§lTAG 10§r §8❄ §a§lAmboss", "§7Base Pack I"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.ENCHANTMENT_TABLE, 1), "§4§lTAG 10§r §8❄ §a§lZaubertisch", "§7Base Pack I"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.WORKBENCH, 2), "§4§lTAG 10§r §8❄ §a§lWerkbank", "§7Base Pack I"));
		} else if (day == 11) {
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.ENDER_CHEST, 2), "§4§lTAG 11§r §8❄ §a§lEnderkiste", "§7Base Pack II"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.LOG, 64), "§4§lTAG 11§r §8❄ §a§lHolz", "§7Base Pack II"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.LAVA_BUCKET, 8), "§4§lTAG 11§r §8❄ §a§lLavaeimer", "§7Base Pack II"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.WATER_BUCKET, 8), "§4§lTAG 11§r §8❄ §a§lWassereimer", "§7Base Pack II"));
		} else if (day == 12) {
			p.getInventory().addItem(new ItemStack(Material.EMERALD_ORE, 8));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.PISTON_BASE, 16), "§4§lTAG 12§r §8❄ §a§lKolben", "§7Redstone Pack"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.DISPENSER, 16), "§4§lTAG 12§r §8❄ §a§lSpender", "§7Redstone Pack"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.HOPPER, 16), "§4§lTAG 12§r §8❄ §a§lTrichter", "§7Redstone Pack"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.FURNACE, 16), "§4§lTAG 12§r §8❄ §a§lOfen", "§7Redstone Pack"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.REDSTONE, 64), "§4§lTAG 12§r §8❄ §a§lRedstone", "§7Redstone Pack"));
		} else if (day == 13) {
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.MONSTER_EGG, 4, (byte) 66),"§4§lTAG 13§r §8❄ §a§lHexe Spawneier"));
			final List<String> songs = Arrays.asList("All I Want for Christmas Is You", "Last Christmas", "White Christmas", "Let It Snow!", "Jingle Bells", "Rockin Around The Christmas Tree", "Feliz Navidad", "Rudolph the Red Nosed Reindeer",
					"It's the Most Wonderful Time of the Year", "In der Weihnachtsbäckerei", "O Tannenbaum");
			final List<Material> records = Arrays.asList(Material.RECORD_3, Material.RECORD_4, Material.RECORD_5, Material.RECORD_6, Material.RECORD_7, Material.RECORD_8, Material.RECORD_9, Material.RECORD_10, Material.RECORD_12, Material.GOLD_RECORD, Material.GREEN_RECORD);
			final int random = r.nextInt(songs.size());
			final String song = songs.get(random);
			final Material disc = records.get(random);
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(disc), "§4§lTAG 13§r §8❄ §a§lWeihnachtsmusik", "§7Song: §a" + song));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.JUKEBOX), "§4§lTAG 13§r §8❄ §a§lMuxStereo"));
		} else if (day == 14) {
			final ItemStack shovel = ms.renameItemStack(new ItemStack(Material.DIAMOND_SPADE), "§4§lTAG 14§r §8❄ §a§lWeihnachtsschaufel");
			shovel.addUnsafeEnchantment(Enchantment.DIG_SPEED, 6);
			shovel.addUnsafeEnchantment(Enchantment.DURABILITY, 2);
			p.getInventory().addItem(shovel);
		} else if (day == 15) {
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.DIAMOND_HELMET, 8), "§4§lTAG 15§r §8❄ §a§lRüstung", "§7Enchant Pack I"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE, 8), "§4§lTAG 15§r §8❄ §a§lRüstung", "§7Enchant Pack I"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.DIAMOND_LEGGINGS, 8), "§4§lTAG 15§r §8❄ §a§lRüstung", "§7Enchant Pack I"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.DIAMOND_BOOTS, 8), "§4§lTAG 15§r §8❄ §a§lRüstung", "§7Enchant Pack I"));
			p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
			p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
		} else if (day == 16) {
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.DIAMOND_SWORD, 8), "§4§lTAG 16§r §8❄ §a§lSchwert", "§7Enchant Pack II"));
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.BOW, 8), "§4§lTAG 16§r §8❄ §a§lBogen", "§7Enchant Pack II"));
			p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
			p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
		} else if (day == 17) {
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.DIAMOND_BLOCK, 4), "§4§lTAG 17§r §8❄ §a§lDiamantgesteine"));
			final long activeMuxCoinsSupply = ms.getActiveMuxCoinsSupply();
			final int coins = (int) (activeMuxCoinsSupply * MuxMining.OreTypes.DIAMOND.getPercentage() / 100) * 20;
			p.sendMessage("§6§lAdvent>§7 Du hast zusätzlich §e§l" + ms.getNumberFormat(coins) + " MuxCoins§7 erhalten!");
			u.addCoins(coins);
			ms.getAnalytics().addEventExpenses(coins);
			ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(coins), "Advent");
			ms.sendScoreboard(p);
			ms.itemRain(loc, Material.DOUBLE_PLANT);
		} else if (day == 18) {
			final ItemStack pants = ms.leatherize(ms.renameItemStack(new ItemStack(Material.LEATHER_LEGGINGS), "§4§lOUTFIT§r §8❄ §a§lWarme Weihnachtshose"), Color.WHITE);
			pants.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 10);
			pants.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
			p.getInventory().addItem(pants);
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.IRON_BLOCK, 16), "§4§lTAG 18§r §8❄ §a§lGeschmuggeltes Elfeneisen"));
		} else if (day == 19) {
			final ItemStack axe = ms.renameItemStack(new ItemStack(Material.DIAMOND_AXE), "§4§lTAG 19§r §8❄ §a§lWeihnachtsaxt");
			axe.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 6);
			axe.addUnsafeEnchantment(Enchantment.DIG_SPEED, 4);
			axe.addUnsafeEnchantment(Enchantment.DURABILITY, 2);
			p.getInventory().addItem(axe);
			p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.BREAD, 1), "§4§lTAG 19§r §8❄ §a§lSüßes Weihnachtsgebäck"));
		} else if (day == 20) {
			final ItemStack santa = ms.getHeadFromURL("https://textures.minecraft.net/texture/14e424b1676feec3a3f8ebade9e7d6a6f71f7756a869f36f7df0fc182d436e", "§4§lTAG 20§r §8❄ §a§lWeihnachtsmannkopf"),
					snowman = ms.getHeadFromURL("https://textures.minecraft.net/texture/98e334e4bee04264759a766bc1955cfaf3f56201428fafec8d4bf1bb36ae6", "§4§lTAG 20§r §8❄ §a§lSchneemannkopf"),
					snowglobe = ms.getHeadFromURL("https://textures.minecraft.net/texture/186156d7f2132669c367ab89523c2e1b9866e40b2b891393744657f1c355", "§4§lTAG 20§r §8❄ §a§lSchneekugel"),
					elf = ms.getHeadFromURL("https://textures.minecraft.net/texture/55afd7cac365398b15b0349d492fcfed0b7cd1956e4e34d3bd513df8112b", "§4§lTAG 20§r §8❄ §a§lElf");
			p.getInventory().addItem(santa, snowman, snowglobe, elf);
		} else if (day == 21) {
			final ItemStack helm = ms.leatherize(ms.renameItemStack(new ItemStack(Material.LEATHER_HELMET), "§4§lOUTFIT§r §8❄ §a§lWarme Weihnachtsmütze"), Color.WHITE);
			helm.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 10);
			helm.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
			p.getInventory().addItem(helm);
			final ItemStack hoe = ms.renameItemStack(new ItemStack(Material.DIAMOND_HOE), "§4§lTAG 21§r §8❄ §a§lOP-HOE");
			hoe.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 10);
			hoe.addUnsafeEnchantment(Enchantment.DIG_SPEED, 5);
			hoe.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
			hoe.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
			p.getInventory().addItem(hoe);
		} else if (day == 22) {
			final ItemStack cookie = ms.renameItemStack(new ItemStack(Material.COOKIE), "§4§lTAG 22§r §8❄ §a§lHeißer Weihnachtskeks §a❤"),
					bow = ms.renameItemStack(new ItemStack(Material.BOW), "§4§lTAG 22§r §8❄ §a§lWeihnachtsbogen", "", "§7Noch §b2 Tage §7zu Weihnachten!");
			cookie.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
			cookie.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 4);
			bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 5);
			p.getInventory().addItem(cookie, bow);
		} else if (day == 23) {
			final ItemStack sword = ms.renameItemStack(new ItemStack(Material.DIAMOND_SWORD), "§4§lTAG 23§r §8❄ §a§lWeihnachtsschwert", "", "§bMorgen §7ist Weihnachten!");
			sword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
			sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
			sword.addEnchantment(Enchantment.DURABILITY, 3);
			p.getInventory().addItem(sword);
			ms.getChests().handleCommand(ms.getServer().getConsoleSender(), new String[] { "add", p.getName(), "GIFT"});
			ms.getChests().handleCommand(ms.getServer().getConsoleSender(), new String[] { "add", p.getName(), "GIFT"});
			ms.getChests().handleCommand(ms.getServer().getConsoleSender(), new String[] { "add", p.getName(), "GIFT"});
			ms.showItemBar(p, "§fDu hast drei §6Geschenkekisten §ferhalten.");
		} else if (day == 24 || day == 25) {
			final ItemStack helm = ms.renameItemStack(new ItemStack(Material.DIAMOND_HELMET), "§4§lFrohe Weihnachten!", "", "§7Alles Gute vom MuxTeam!"),
					chest = ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE), "§4§lFrohe Weihnachten!", "", "§7Alles Gute vom MuxTeam!"),
					pants = ms.renameItemStack(new ItemStack(Material.DIAMOND_LEGGINGS), "§4§lFrohe Weihnachten!", "", "§7Alles Gute vom MuxTeam!"),
					boots = ms.renameItemStack(new ItemStack(Material.DIAMOND_BOOTS), "§4§lFrohe Weihnachten!", "", "§7Alles Gute vom MuxTeam!");
			helm.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 5);
			helm.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
			chest.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 5);
			chest.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
			pants.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 5);
			pants.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
			boots.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 5);
			boots.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
			p.getInventory().addItem(helm, chest, pants, boots);
			ms.getChests().handleCommand(ms.getServer().getConsoleSender(), new String[] { "add", p.getName(), "CHRISTMAS"});
			p.sendMessage("§6§lMuxAdvent>§7 Du hast eine krafthaltende Schutz V und eine Weihnachtskiste erhalten!");
		}
		p.updateInventory();
		lines[0] = "§5§lDu hast die";
		lines[1] = "§5§lBelohnung";
		lines[2] = "§5§lvom Türchen";
		lines[3] = "§5§lerhalten!";
	}
}