package me.muxteam.extras;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.muxteam.basic.NMSReflection;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuxEmojis {
	private MuxSystem ms;
	private final String skullName = "§6§lEmoji";
	private final Random r = new Random();
	private final Map<String, Integer> emotelist = new HashMap<>();
	private final ConcurrentHashMap<UUID, Integer> activeemote = new ConcurrentHashMap<>();
	private final Map<String, ItemStack> originalitem = new HashMap<>();

	public MuxEmojis(final MuxSystem ms) {
		this.ms = ms;
	}
	public void close() {
		this.ms = null;
	}

	public int getActiveEmoji(final UUID uuid) {
		return activeemote.getOrDefault(uuid, 0);
	}
	public void stopEmoji(final Player p) {
		final int task = emotelist.getOrDefault(p.getName(), 0);
		if (task != 0) {
			removeSkull(p);
			ms.getServer().getScheduler().cancelTask(task);
			emotelist.remove(p.getName());
			activeemote.remove(p.getUniqueId());
		}
	}
	public void deactivateEmojis() {
		final Server sr = ms.getServer();
		for (final UUID uuid : activeemote.keySet()) {
			stopEmoji(sr.getPlayer(uuid));
		}
	}
	public boolean setEmoji(final Player p, final int num) {
		final int active = activeemote.getOrDefault(p.getUniqueId(), 0);
		if (active != 0) {
			stopEmoji(p);
			if (active == num) return true;
		}
		final ItemStack helm = p.getInventory().getHelmet();
		if (helm != null && helm.hasItemMeta() && helm.getItemMeta().hasDisplayName() && helm.getItemMeta().getDisplayName().equals(skullName)) {
			p.getInventory().setHelmet(null);
		}
		if (ms.getAdmin().EMOJIS.isActive() == false) {
			ms.showItemBar(p, "§cEmojis sind derzeit temporär deaktiviert.");
			p.closeInventory();
			return false;
		}
		switch (num) {
		case 1: playEffEmoteRage(p);
		break;
		case 2: playEffEmoteCry(p);
		break;
		case 3: playEffEmoteLove(p);
		break;
		case 4: playEffEmoteGoofy(p);
		break;
		case 5: playEffEmoteTooCool(p);
		break;
		case 6: playEffEmoteRIP(p);
		break;
		case 7: playEffEmoteGrin(p);
		break;
		case 8: playEffEmoteSleepy(p);
		break;
		case 9: playEffEmoteCheeky(p);
		break;
		case 10: playEffEmoteFrown(p);
		break;
		case 11: playEffEmoteSmile(p);
		break;
		case 12: playEffEmoteSpicy(p);
		break;
		case 13: playEffEmoteKiss(p);
		break;
		case 14: playEffEmotePepe(p);
		break;
		default: break;
		}
		return true;
	}
	public short getEmojiFromName(final String s) {
		switch (s) {
			case "Wütend":
				return 1;
			case "Weinend":
				return 2;
			case "Liebend":
				return 3;
			case "Goofy":
				return 4;
			case "DealWithIt":
				return 5;
			case "RIP":
				return 6;
			case "Grinsend":
				return 7;
			case "Müde":
				return 8;
			case "Frech":
				return 9;
			case "Traurig":
				return 10;
			case "Fröhlich":
				return 11;
			case "Spicy":
				return 12;
			case "Kuss":
				return 13;
			case "Pepe":
				return 14;
			default:
				break;
		}
		return 0;
	}
	public String getEmojiName(final short num) {
		switch (num) {
			case 1:
				return "Wütend";
			case 2:
				return "Weinend";
			case 3:
				return "Liebend";
			case 4:
				return "Goofy";
			case 5:
				return "DealWithIt";
			case 6:
				return "RIP";
			case 7:
				return "Grinsend";
			case 8:
				return "Müde";
			case 9:
				return "Frech";
			case 10:
				return "Traurig";
			case 11:
				return "Fröhlich";
			case 12:
				return "Spicy";
			case 13:
				return "Kuss";
			case 14:
				return "Pepe";
			default:
				break;
		}
		return "Unbekannt";
	}
	public boolean isEmojiItem(final ItemStack i) {
		return i.hasItemMeta() && i.getItemMeta().hasDisplayName() && i.getItemMeta().getDisplayName().equals(skullName);
	}
	private void giveBackOriginal(final Player p) {
		final String name = p.getName();
		if (originalitem.containsKey(name)) {
			p.getInventory().setHelmet(originalitem.get(name));
			originalitem.remove(name);
		}
	}
	private void removeSkull(final Player p) {
		for (final ItemStack item : p.getInventory().getArmorContents()) {
			if (isEmojiItem(item)) {
				p.getInventory().remove(item);
				p.getInventory().removeItem(item);
				giveBackOriginal(p);
			}
		}
	}

	//EFFECTS  (get others on skDragon-0.11.0 BETA)

	public void playEffEmoteRage(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = RAGE1(skullName), skull2 = RAGE2(skullName), skull3 = RAGE3(skullName), skull4 = RAGE4(skullName), skull5 = RAGE5(skullName), skull6 = RAGE6(skullName),
					skull7 = RAGE7(skullName);
			final int rage = new BukkitRunnable() {
				float step = 0.0F;
				int random;
				Location location;
				@Override public void run() {
					this.random = r.nextInt(201);
					this.location = p.getLocation();
					this.location.add(0.0D, 2.3D, 0.0D);
					if (this.random <= 50) {
						this.location.getWorld().spigot().playEffect(this.location, Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0.2F, 0.2F, 0.2F, 0F, 1, 32);
					}
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 5.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 10.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 15.0F) {
						p.getInventory().setHelmet(skull4);
					}
					if (this.step >= 20.0F && this.step < 200.0F && this.random <= 100) {
						p.getInventory().setHelmet(skull5);
					}
					if (this.step >= 20.0F && this.step < 200.0F && this.random >= 101 && this.random <= 150) {
						p.getInventory().setHelmet(skull6);
					}
					if (this.step >= 20.0F && this.step < 200.0F && this.random >= 151 && this.random <= 200) {
						p.getInventory().setHelmet(skull7);
					}
					if (this.step >= 200.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, rage);
			activeemote.put(p.getUniqueId(), 1);
		}
	}
	public void rotateAroundAxisY(final Vector v, final double angle) {
		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final double x = v.getX() * cos + v.getZ() * sin;
		final double z = v.getX() * -sin + v.getZ() * cos;
		v.setX(x).setZ(z);
	}
	public void playEffEmoteCry(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = CRY1(skullName), skull2 = CRY2(skullName), skull3 = CRY3(skullName), skull4 = CRY4(skullName), skull5 = CRY5(skullName), skull6 = CRY6(skullName),
					skull7 = CRY7(skullName), skull8 = CRY8(skullName), skull9 = CRY9(skullName), skull10 = CRY10(skullName), skull11 = CRY11(skullName);
			final int cry = new BukkitRunnable() {
				float step = 0.0F;
				int random;
				Location location;
				Vector v;
				@Override public void run() {
					this.random = r.nextInt(185) + 16;
					this.location = p.getEyeLocation();
					this.location.add(0.0D, -0.1D, 0.0D);
					this.v = new Vector(0.2D, 0.0D, 0.0D);
					rotateAroundAxisY(this.v, -(this.location.getYaw() + 90.0F) * 0.017453292F);
					if (this.step >= 50.0F && this.step < 200.0F && this.random <= 100) {
						this.location.getWorld().spigot().playEffect(this.location.add(v), Effect.SPLASH, 0, 0, 0.1F, 0.1F, 0.1F, 0F, 1, 32);
					}
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 10.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 20.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 30.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 35.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 40.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 45.0F) {
						p.getInventory().setHelmet(skull7);
					}
					if (this.step >= 50.0F && this.step < 200.0F && this.random <= 50) {
						p.getInventory().setHelmet(skull8);
					}
					if (this.step >= 50.0F && this.step < 200.0F && this.random >= 51 && this.random <= 100) {
						p.getInventory().setHelmet(skull9);
					}
					if (this.step >= 50.0F && this.step < 200.0F && this.random >= 101 && this.random <= 150) {
						p.getInventory().setHelmet(skull10);
					}
					if (this.step >= 50.0F && this.step < 200.0F && this.random >= 151 && this.random <= 200) {
						p.getInventory().setHelmet(skull11);
					}
					if (this.step >= 200.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, cry);
			activeemote.put(p.getUniqueId(), 2);
		}
	}
	public void playEffEmoteLove(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = LOVE1(skullName), skull2 = LOVE2(skullName), skull3 = LOVE3(skullName), skull4 = LOVE4(skullName), skull5 = LOVE5(skullName), skull6 = LOVE6(skullName),
					skull7 = LOVE7(skullName);
			final int lovey = new BukkitRunnable() {
				float step = 0.0F;
				int random;
				Location location;
				@Override public void run() {
					this.random = r.nextInt(201);
					this.location = p.getLocation();
					this.location.add(0.0D, 2.3D, 0.0D);
					if (this.random <= 20) {
						this.location.getWorld().spigot().playEffect(this.location, Effect.HEART, 0, 0, 0.2F, 0.2F, 0.2F, 0F, 1, 32);
					}
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 10.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 20.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 25.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 30.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 35.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 40.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 45.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 50.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 55.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 60.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 65.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 70.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 75.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 80.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 85.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 90.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 95.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 100.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 105.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 110.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 115.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 120.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 125.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 130.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 135.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 140.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 145.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 150.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 155.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 160.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 165.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 170.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 175.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 180.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 185.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 190.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 195.0F) {
						p.getInventory().setHelmet(skull6);
					}
					if (this.step >= 200.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, lovey);
			activeemote.put(p.getUniqueId(), 3);
		}
	}
	public void playEffEmoteGoofy(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = GOOFY1(skullName), skull2 = GOOFY2(skullName), skull3 = GOOFY3(skullName), skull4 = GOOFY4(skullName);
			final int goofy = new BukkitRunnable() {
				float step = 0.0F;
				@Override public void run() {
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 5.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 10.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 15.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 20.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 25.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 30.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 35.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 40.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 45.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 50.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 55.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 60.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 65.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 70.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 75.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 80.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 85.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 90.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 95.0F) {
						p.getInventory().setHelmet(skull2);
					}
					if (this.step >= 100.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, goofy);
			activeemote.put(p.getUniqueId(), 4);
		}
	}
	public void playEffEmoteTooCool(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = TOOCOOL1(skullName), skull2 = TOOCOOL2(skullName), skull3 = TOOCOOL3(skullName), skull4 = TOOCOOL4(skullName), skull5 = TOOCOOL5(skullName),
					skull6 = TOOCOOL6(skullName), skull7 = TOOCOOL7(skullName), skull8 = TOOCOOL8(skullName), skull9 = TOOCOOL9(skullName), skull10 = TOOCOOL10(skullName);
			final int toocool = new BukkitRunnable() {
				float step = 0.0F;
				@Override public void run() {
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 5.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 10.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 15.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 20.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 25.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 30.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 40.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 45.0F) {
						p.getInventory().setHelmet(skull9);
					} else if (this.step == 50.0F) {
						p.getInventory().setHelmet(skull10);
					}
					if (this.step >= 100.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, toocool);
			activeemote.put(p.getUniqueId(), 5);
		}
	}
	public void playEffEmoteRIP(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = RIP1(skullName), skull2 = RIP2(skullName), skull3 = RIP3(skullName), skull4 = RIP4(skullName), skull5 = RIP5(skullName), skull6 = RIP6(skullName),
					skull7 = RIP7(skullName);
			final int rip = new BukkitRunnable() {
				float step = 0.0F;
				@Override public void run() {
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 5.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 10.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 15.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 20.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 25.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 30.0F) {
						p.getInventory().setHelmet(skull7);
					}
					if (this.step >= 100.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, rip);
			activeemote.put(p.getUniqueId(), 6);
		}
	}
	public void playEffEmoteGrin(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = GRIN1(skullName), skull2 = GRIN2(skullName), skull3 = GRIN3(skullName), skull4 = GRIN4(skullName);
			final int grin = new BukkitRunnable() {
				float step = 0.0F;
				@Override public void run() {
					if (this.step <= 10.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step > 10.0F && this.step < 20.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step > 20.0F && this.step < 30.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step > 30.0F && this.step < 100.0F) {
						p.getInventory().setHelmet(skull4);
					}
					if (this.step >= 100.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, grin);
			activeemote.put(p.getUniqueId(), 7);
		}
	}
	public void playEffEmoteSleepy(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = SLEEPY1(skullName), skull2 = SLEEPY2(skullName), skull3 = SLEEPY3(skullName), skull4 = SLEEPY4(skullName), skull5 = SLEEPY5(skullName),
					skull6 = SLEEPY6(skullName), skull7 = SLEEPY7(skullName), skull8 = SLEEPY8(skullName), skull9 = SLEEPY9(skullName);
			final int sleepy = new BukkitRunnable() {
				float step = 0.0F;
				@Override public void run() {
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 8.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 16.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 20.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 30.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 35.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 45.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 55.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 80.0F) {
						p.getInventory().setHelmet(skull9);
					} else if (this.step == 100.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 130.0F) {
						p.getInventory().setHelmet(skull9);
					} else if (this.step == 150.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 180.0F) {
						p.getInventory().setHelmet(skull9);
					} else if (this.step == 200.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 230.0F) {
						p.getInventory().setHelmet(skull9);
					} else if (this.step == 250.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 280.0F) {
						p.getInventory().setHelmet(skull9);
					} else if (this.step == 300.0F) {
						p.getInventory().setHelmet(skull8);
					}
					if (this.step >= 305.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, sleepy);
			activeemote.put(p.getUniqueId(), 8);
		}
	}
	public void playEffEmoteCheeky(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = CHEEKY1(skullName), skull2 = CHEEKY2(skullName), skull3 = CHEEKY3(skullName), skull4 = CHEEKY4(skullName), skull5 = CHEEKY5(skullName),
					skull6 = CHEEKY6(skullName), skull7 = CHEEKY7(skullName), skull8 = CHEEKY8(skullName);
			final int cheeky = new BukkitRunnable() {
				float step = 0.0F;
				@Override public void run() {
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 5.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 10.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 15.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 20.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 25.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 30.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 35.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 40.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 45.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 50.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 55.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 60.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 65.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 70.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 75.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 80.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 85.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 90.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 95.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 100.0F) {
						p.getInventory().setHelmet(skull3);
					}
					if (this.step >= 101.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, cheeky);
			activeemote.put(p.getUniqueId(), 9);
		}
	}
	public void playEffEmoteFrown(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = FROWN1(skullName), skull2 = FROWN2(skullName), skull3 = FROWN3(skullName), skull4 = FROWN4(skullName), skull5 = FROWN5(skullName),
					skull6 = FROWN6(skullName), skull7 = FROWN7(skullName), skull8 = FROWN8(skullName);
			final int frown = new BukkitRunnable() {
				float step = 0.0F;
				@Override public void run() {
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 10.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 20.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 40.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 45.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 50.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 55.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 60.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 65.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 70.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 75.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 80.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 85.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 90.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 95.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 100.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 105.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 110.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 115.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 120.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 125.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 130.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 135.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 140.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 145.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 150.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 155.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 160.0F) {
						p.getInventory().setHelmet(skull8);
					} else if (this.step == 165.0F) {
						p.getInventory().setHelmet(skull7);
					} else if (this.step == 170.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 175.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 180.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 185.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 190.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 195.0F) {
						p.getInventory().setHelmet(skull7);
					}
					if (this.step >= 200.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, frown);
			activeemote.put(p.getUniqueId(), 10);
		}
	}
	public void playEffEmoteSmile(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = SMILE1(skullName), skull2 = SMILE2(skullName), skull3 = SMILE3(skullName), skull4 = SMILE(skullName);
			final int smile = new BukkitRunnable() {
				float step = 0.0F;
				@Override public void run() {
					if (this.step <= 20.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step > 20.0F && this.step < 40.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step > 40.0F && this.step < 60.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step > 60.0F && this.step < 100.0F) {
						p.getInventory().setHelmet(skull4);
					}
					if (this.step >= 100.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, smile);
			activeemote.put(p.getUniqueId(), 11);
		}
	}
	public void playEffEmoteSpicy(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = SPICY1(skullName), skull2 = SPICY2(skullName), skull3 = SPICY3(skullName), skull4 = SPICY4(skullName), skull5 = SPICY5(skullName), skull6 = SPICY6(skullName),
					skull7 = SPICY7(skullName), skull8 = SPICY8(skullName), skull9 = SPICY9(skullName);
			final int spicy = new BukkitRunnable() {
				float step = 0.0F;
				int counter = 0;
				@Override public void run() {
					if (this.step == 0.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step == 5.0F) {
						p.getInventory().setHelmet(skull2);
					} else if (this.step == 10.0F) {
						p.getInventory().setHelmet(skull3);
					} else if (this.step == 15.0F) {
						p.getInventory().setHelmet(skull4);
					} else if (this.step == 20.0F) {
						p.getInventory().setHelmet(skull5);
					} else if (this.step == 25.0F) {
						p.getInventory().setHelmet(skull6);
					} else if (this.step == 30.0F) {
						p.getInventory().setHelmet(skull7);
					}
					if (this.step >= 30.0F && this.counter == 0) {
						p.getInventory().setHelmet(skull8);
					}
					if (this.step >= 30.0F && this.counter == 2) {
						p.getInventory().setHelmet(skull9);
					}
					if (this.step >= 100.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
					if (this.step >= 30.0F) {
						this.counter += 1;
					}
					if (this.counter > 3) {
						this.counter = 0;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, spicy);
			activeemote.put(p.getUniqueId(), 12);
		}
	}
	public void playEffEmoteKiss(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = KISS1(skullName), skull2 = KISS2(skullName);
			final int kiss = new BukkitRunnable() {
				float step = 0.0F;
				int random;
				Location location;
				@Override public void run() {
					if (this.step <= 20.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step > 50.0F && this.step < 100.0F) {
						p.getInventory().setHelmet(skull2);
						this.random = r.nextInt(201);
						this.location = p.getLocation();
						this.location.add(0.0D, 1.3D, 0.0D);
						if (this.random <= 20) {
							this.location.getWorld().spigot().playEffect(this.location, Effect.HEART, 0, 0, 0.2F, 0.2F, 0.2F, 0F, 1, 32);
						}
					}
					if (this.step >= 100.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, kiss);
			activeemote.put(p.getUniqueId(), 13);
		}
	}
	public void playEffEmotePepe(final Player p) {
		final String pname = p.getName();
		if (emotelist.containsKey(pname) == false) {
			originalitem.put(pname, p.getInventory().getHelmet());
			final ItemStack skull1 = PEPE1(skullName), skull2 = PEPE2(skullName);
			final int pepe = new BukkitRunnable() {
				float step = 0.0F;
				@Override public void run() {
					if (this.step <= 20.0F) {
						p.getInventory().setHelmet(skull1);
					} else if (this.step > 90.0F && this.step < 100.0F) {
						p.getInventory().setHelmet(skull2);
					}
					if (this.step >= 100.0F) {
						this.step = 0F;
					} else {
						this.step += 1.0F;
					}
				}
			}.runTaskTimer(ms, 0L, 1L).getTaskId();
			emotelist.put(pname, pepe);
			activeemote.put(p.getUniqueId(), 14);
		}
	}

	// EMOJIS
	private ItemStack createSkull(final String name, final String url) {
		final ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
		if (url.isEmpty()) {
			return head;
		}
		final SkullMeta headMeta = (SkullMeta) head.getItemMeta();
		final GameProfile profile = new GameProfile(UUID.randomUUID(), null);
		profile.getProperties().put("textures", new Property("textures", url));
		NMSReflection.setObject(headMeta.getClass(), "profile", headMeta, profile);
		headMeta.setDisplayName(name);
		head.setItemMeta(headMeta);
		return head;
	}
	public ItemStack SMILE(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTJlOTgxNjVkZWVmNGVkNjIxOTUzOTIxYzFlZjgxN2RjNjM4YWY3MWMxOTM0YTQyODdiNjlkN2EzMWY2YjgifX19");
	}
	public ItemStack SMILE1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM1OTI3OTA3MTYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iN2Q1MzNlNjVmMmNhZTk3YWZlMzM0YzgxZWNjOTdlMmZhNWIzZTVkM2VjZjhiOTFiYzM5YTVhZGIyZTc5YSJ9fX0=");
	}
	public ItemStack SMILE2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM1OTMzMTY3OTMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kMTdlOGJhMTc0NTlkMjBmZDczNjcyYmNiOGE5ZTJhOGE0NGNmMGE1ZmYxNTQxMjJkOTZiNWRiYmQ5MTcxYSJ9fX0=");
	}
	public ItemStack SMILE3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM1OTMzOTM4ODIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jYzM1ZGZlOWI1YmVlMWUxMzlkNzI4YWQ4YWVhZjIzNTU2ZTJlMTlmYzc2MTViOTk2NWMwYTlhNmU1NjZjODkwIn19fQ==");
	}
	public ItemStack GRIN(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTA1OWQ1OWViNGU1OWMzMWVlY2Y5ZWNlMmY5Y2YzOTM0ZTQ1YzBlYzQ3NmZjODZiZmFlZjhlYTkxM2VhNzEwIn19fQ==");
	}
	public ItemStack GRIN1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MDg5NDc0MjYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80NjViNTYxMWY4YWJjMDFkOWJiOGZjZDYyZjNhNjRiNTEyNTUzNGE0Mjg3MzFmMjAyZTYxOWQ5Y2UxIn19fQ==");
	}
	public ItemStack GRIN2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MDkwNjczNzgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hNWQ0M2ViMGVjNWY2ZGUxZDQ2OWI2OTY4MDk3OGE2ZGQ3MTE3NzcyZWUwZDgyZmZkZjA4NzQ5ZTg0ZGY3ZWQifX19");
	}
	public ItemStack GRIN3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MDkxMDUyMTYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84ZDhmNWZiMzg3Y2E2NmZjMmY2NWI5MWZjYjIzMTYwNDU0OGU4NTY1ODk1YmI5NmM2NzY5ODQyMDVlNmYxOSJ9fX0=");
	}
	public ItemStack GRIN4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MDkxNzU2ODksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8wMWI5ZGVmNTU4NzZjNDFjMTdjODE1Zjg4MTE1ZjAyYzk1Zjg5NjIwZmJlZDZhNmNiMmQzOGQ0NmZlMDUifX19");
	}
	public ItemStack FROWN1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA2OTgwMzUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80ZGI0YzhkZmRjNzc5MmQyNGM1MzM0YTVhMmQxZDQ2N2I2ZTEwYWJmYWJkNjM3ZTRhODBjY2IwNWI5Y2NmYmQifX19");
	}
	public ItemStack FROWN2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA3NzQyNDIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZjVjMzk5MmVkNWYyMTNkYmM2ZTlmMzY4OTE1ZmI1MTlkYjRlMTg3NDA3NTE4ZGQyNWUwMTRiODFjNmU2ZWIifX19");
	}
	public ItemStack FROWN3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA4MTU1NzMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iYjRmMzk1OWQ3NzZhNGFhMTZjNDNkZGUxNmVlMzc3N2Y5NThiNWM2NmFjMDVjNTQyY2JlMjdmNjdkN2JlN2NlIn19fQ==");
	}
	public ItemStack FROWN4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA4NjAxNTksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZWEwNzljOTMxNDFjOGM2YmFkZTYwYjE5ODdkNDI0NzM5YjQ2MzYxZGJmNTEzYzBhNDkwM2RiYTRlNjcifX19");
	}
	public ItemStack FROWN5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA4OTg0NDUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xYzI2NzJhYWFlNThmM2QxODUyZDE5Yjg0MjJjYWY3MGIzMjU4MmY4ZGUzZmNiNWM3YzI0ZGFjYjdlYmMzIn19fQ==");
	}
	public ItemStack FROWN6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA5MzU1NTIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kNTQxOTkyZDc2MTJhMTRiYTU4OTc4ZDEyZmNiMjEyYmNmZjc3Mzk3Nzg2OGFjNDkxZmViOGYxZmE1YmMifX19");
	}
	public ItemStack FROWN7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA5Njc1NDQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85MTNlYmVkYWM0N2U0NDg2YTE4ZmM2MzQzMjRlNmEyMmIxYjljNTZjMzJjMGYxMzM5N2E3NDdjNmZjNDRjOSJ9fX0=");
	}
	public ItemStack FROWN8(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA5OTk5NzksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84NjVlMWY0NDRmYWZiNzM3YzBhYmY1YmUxYmY5N2ZjMjdhZGVkYWUzZmQ5ODIyNmE0NjhmYTcyYzFkMTc1MyJ9fX0=");
	}
	public ItemStack RAGE1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2Mzk5MzgxOTcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81MTNmN2ViOWZjZjk5MjZiZjdiOTQwNDlhZWY1ZWZkYjdiYmU3MGJjYzc0ZjNmNjYxOGUxMmRjMTgxZDYyNyJ9fX0=");
	}
	public ItemStack RAGE2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2Mzk5NzQwODMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80N2JiZjZkOWY0YzU3NTU2ZWVmODE2YzUwZWI3NWY5ZDE1OGY1Mzk1NDk1N2FhYmU2YzJlMTRmZmE2YzkwIn19fQ==");
	}
	public ItemStack RAGE3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NDAwMDkxNTUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hNzUwMTI3ZjFjM2M3MWY2YTVmNWU5OTE3YTgyNWU5MjM1ZTE5NTliMjU4ZmYyOWI2ZmY5NzcxY2I0NCJ9fX0=");
	}
	public ItemStack RAGE4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NDAwNjU3NjUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lOTViMjBmYjFmY2ZiZWYyMjIwNjJkZDQzZWVjYmNiMzg3MWM1Mjg2NjVmOGVkNjc1ZjQyZmM2ZTU4OWEwYjcifX19");
	}
	public ItemStack RAGE5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NDAxMDIzNjcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mYTE1MWNlYjY2YjM0MTI3NzVlOWQ0NDg3OTA0NmEzOThkYmRiN2RmY2IwYWY1NzFiN2EwM2U3MmQ5ZmJmMSJ9fX0=");
	}
	public ItemStack RAGE6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NDAxMzcwNDYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jOWY4ZDA0MDU3OTc4ODE3Y2I4MWUwOTVjY2M1OTc5OWZlNGI3ODBmZmRiZmI5ZjBkNjJhYTI4NjcyMTg1NiJ9fX0=");
	}
	public ItemStack RAGE7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NDAxNzY0NTgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83ZjhkYjhjZjI0MWYyNTY1YzViZDQ5NWEwNjk1YjdjYWM5MzcwYzhiZmQ3MzJkNmQ4NzRlNjJmYjEyZjNkYSJ9fX0=");
	}
	public ItemStack CHEEKY1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA2OTgwMzUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80ZGI0YzhkZmRjNzc5MmQyNGM1MzM0YTVhMmQxZDQ2N2I2ZTEwYWJmYWJkNjM3ZTRhODBjY2IwNWI5Y2NmYmQifX19");
	}
	public ItemStack CHEEKY2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA3NzQyNDIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZjVjMzk5MmVkNWYyMTNkYmM2ZTlmMzY4OTE1ZmI1MTlkYjRlMTg3NDA3NTE4ZGQyNWUwMTRiODFjNmU2ZWIifX19");
	}
	public ItemStack CHEEKY3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODEzNTEwMTcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zNWE0NmY4MzM0ZTQ5ZDI3MzM4NGViNzJiMmFjMTVlMjRhNjQwZDc2NDhlNGIyOGMzNDhlZmNlOTNkYzk3YWIifX19");
	}
	public ItemStack CHEEKY4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODE0MDUxNTgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zMDIxMTBiNGYyOTExYzBmNTU5Nzc5NmVjODEyZThmYTI2MGJhNWFiN2NmZjcyNWUxNmI3ZWVlN2M2NzdiNyJ9fX0=");
	}
	public ItemStack CHEEKY5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODE0NTc4NjksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81ZjE1YzliOGVkYzU2MjliNmNhYTQ5MTQ4YTIwYzU4OTA4NTNjMjY3NDM4NWU0Mzg3NmNhNTZkMWQ0NjVmIn19fQ==");
	}
	public ItemStack CHEEKY6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODE1NzEwMDgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80NDdkY2Y5ZGQyODNhZDZkODM5NDJiNjYwN2E3Y2U0NWJlZTljZGZlZWZiODQ5ZGEyOWQ2NjFkMDNlNzkzOCJ9fX0=");
	}
	public ItemStack CHEEKY7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODE2NDQ3NjgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kZTM1NTU1OWY0Y2Q1NjExOGI0YmM4YjQ2OTdiNjI1ZTE4NDViNjM1NzkwYzA3YmY0OTI0YzhjNzY3M2EyZTQifX19");
	}
	public ItemStack CHEEKY8(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODE3MDAwNjcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMDdlZWY5MWE0NTNhNTE1MTQ4N2M5ZDZiOWQ0YzQzNGRiN2Y4YTAyYTRjYWYxOGVmNmYzMzU4Njc3ZjYifX19");
	}
	public ItemStack SLEEPY1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2MTA2OTgwMzUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80ZGI0YzhkZmRjNzc5MmQyNGM1MzM0YTVhMmQxZDQ2N2I2ZTEwYWJmYWJkNjM3ZTRhODBjY2IwNWI5Y2NmYmQifX19");
	}
	public ItemStack SLEEPY2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NzUzNjQ2MjgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81MDVjMjM3NzMxYWU3YTY0YzA2NjhjZTg4OWRjNmQxZTIxY2ZkYmNjOWZhNTFlZmU0ODE0M2VlNDU2ZTQ1YTkifX19 ");
	}
	public ItemStack SLEEPY3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NzU0MTk3NjAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84OGFkNmIyZjFjMWMxODE4MjZkMzdmZWFhNzVkY2Q0Y2Y4NWY4NzNkNTQ1YWJhMzdlNmUzOGZmNTFlNjcwOGIifX19");
	}
	public ItemStack SLEEPY4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NzU0NjIxMzgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84ODhhODI2MzExMmM0YzkzOWQ4MmEyZGZkM2Q5YjllODk2OTI1ZDQxYmZmNzg2OWU1OWRmNmIxOTJkODZkZDcifX19");
	}
	public ItemStack SLEEPY5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NzU1MDk4MjIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82MjZkOTVhMGFjYjQyMjRhZjQ4MThkYjY3MGIzNmU1ZjIwMTkyYTg5ZWZiOTZmYTVjMmJmMGM3ZTQzZjJkN2YifX19");
	}
	public ItemStack SLEEPY6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NzU1NDcxMzUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80NjMxMTcxN2U4YzJkNTg4Mjk0MmI5Mjc5NDE3YjUxODMyY2IyYTJmN2IwNmY4MjE1YjlmZmZlNzFmMzYwZjIifX19");
	}
	public ItemStack SLEEPY7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NzU1ODI2NjUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lOGM5YzZlZmFkOWNlZThmZTdkM2I1NjM3MGY2Njc1OTMzZGI5ZmNkZjIzNmExYzQ5YjYzNDkyNTFhNGY1YTU2In19fQ==");
	}
	public ItemStack SLEEPY8(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NzU2MTM3MTUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83ZDlmMGYxOWVmMTYxYmZhYmFlYWE0OWM1Mjg4MGJlY2ZlODJmYjk2MmJiYjIxN2QxNzQzZDBmMWI1M2E5NSJ9fX0=");
	}
	public ItemStack SLEEPY9(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2NzU2ODEwNTcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hMDU4Y2U2MWQxZTlhZDZiMTE1ZGExY2JkZjU2ZDk3ZTJhMmNiMzhlMzFlYTE3OTQzMWQ5MzU2MTYxNjM1ODEifX19");
	}
	public ItemStack WINK1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODMyMDEyMTQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83Mzc0MmRlZmJjNDY3N2U0NTQ2OGI5MzI0ZDk3MDRmNjM1YTVmYzMxMWU5YjgzNjhkZDVjYzRjZDI4MmQ2YSJ9fX0=");
	}
	public ItemStack WINK2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODMyNzY0NDEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jZTI1YzUyZWVlNDdjMTJlMWFhMTY5ODljZWM5YzlkM2U0OWFmOGM0NGI3MWViYzk1M2NhMWQxZmYyZDU5NjcwIn19fQ==");
	}
	public ItemStack WINK3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODMzMTkxOTQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yN2Y2NGUyN2JjODUwYTMzZmRmZmZiNzliNTZjOWJhZWE4YTE4Zjk3NjNiN2NhZDU2YzRiNmEyZTlkMyJ9fX0=");
	}
	public ItemStack WINK4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODMzNTA0OTUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85ZDllYjYwZDkyYzFmZjJlOWNlYzY1ZGRjMjNjMmZiN2UxMjQ1MGIzNjA3MWRkZGQ2NjkwZjg4NzU0ODZkOCJ9fX0=");
	}
	public ItemStack CRY1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODM2ODAwNDIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85ZWFiZWRhNDk2ZTA2ODRmODBkODhkNGI1YTcxZDY1YWZhZjlmYWYxODRmYTRhNzdmODFlZDI0YzZkYTBmNCJ9fX0=");
	}
	public ItemStack CRY2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODM3MTY1NTEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xOTZiOGUyNzJjNTRhNDIyZDlkZjM2ZDg1Y2FmZjI2NjI0YzczM2U3YjNmNjA0MGQzZTRjOWNkNmUifX19");
	}
	public ItemStack CRY3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODM3Njg1OTYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kZWM5YWE5YjNmNDYxOTVhZTljN2ZlYTdjNjExNDg3NjRhNDFlMGQ2OGRhZTQxZTgyODY4ZDc5MmIzYyJ9fX0=");
	}
	public ItemStack CRY4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODM4MDcxMDMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iZTI5ZGFkYjYwYzkwOTZmYWI5MmZmYTc3NDllMzA0NjJlMTRhOGFmYWY2ZGU5MzhkOWMwYTRkNzg3ODEifX19");
	}
	public ItemStack CRY5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODM4NDA1NjAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jOGFiYTFmNDlmYmY4ODI5ODU5ZGRkOGY3ZTU5MTgxNTVlN2RkYzc4OTE5NzY4YjZlNmM1MzZlNTI3OGMzMSJ9fX0=");
	}
	public ItemStack CRY6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODQwMzY5NjAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xMDczYmEzZjFjYTFkMWU0ZjdlMWVjNzQyZGRjZmY4ZmIwZDk2MmJjNTY2MmQxMjc2MjJhMzcyNmUzYmI2NiJ9fX0=");
	}
	public ItemStack CRY7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODQxMTM5NzcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85NTJkY2RiMTNmNzMyMzQyZWYzN2NiZjA5MDI5NjA5ODQ5OTJmNWU2NzI4OTM3MzA1NGIwMGMyYTFmNyJ9fX0=");
	}
	public ItemStack CRY8(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODQyMzAyMTEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80YjBmMmYzZDM0OTk5NTllOTdkMjdlNjEwYmNmZDkwZGJmOGRmNWUxY2Y0Yjk4ZjI1OTI4NGYyZTM1NTcyOCJ9fX0=");
	}
	public ItemStack CRY9(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODQyNzUzNDgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lZGU0ZDQ4NWVlYzBiMDhlMzJmZjRhM2RiOGI3OWMxNTI0Y2JhOTNlNDdmODYxZDg0NjhhZGYzNjcwNDRhYiJ9fX0=");
	}
	public ItemStack CRY10(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODQzMTI5MjQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZTNlMjI3NjFjNzZiNGY4ZmFkODlkYmM4MGYzYWYyMDNlN2I4MjExMjM4MDExYmU3ZmZiODAyNjFkOWM2NCJ9fX0=");
	}
	public ItemStack CRY11(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODQzNDUwNDYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83MzJmZTEyMWE2M2VhYWJkOTljZWQ2ZDFhY2M5MTc5ODY1MmQxZWU4MDg0ZDJmOTEyN2Q4YTMxNWNhZDVjZTQifX19");
	}
	public ItemStack GOOFY1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTgzMjk0MjEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hNWYxN2I2MWMxODFlZjZlOWZjZDNlNDExYmU3YWFlMDM0NzIxMGI5NGU3MTI1OWE2ZmQxZmM1Y2RmNTgyNiJ9fX0=");
	}
	public ItemStack GOOFY2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTgzNjI2MzMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80NDNhZDdkMTZkODQxODgxYmFmMTEzZGFjZTYwYTJjZTZjNDczYTgwNTM1OTQ4ZWJjZWFkNDk5OTJjNWI5NmEifX19");
	}
	public ItemStack GOOFY3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTgzOTQwNTYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NmQ4ODYzZGM3OWMzYjRmYmU0Y2E2MTliNWYzNTZhNGViNWM5NzM0YjMzYTI1NTJhYzk5MDU1MWIxMjExIn19fQ==");
	}
	public ItemStack GOOFY4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTg0Mjc3OTMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kMWRmNTA3NTc1Yzk0YWQ2M2U0MGM0ZDRhMWRkOWM5OTYwZjk2YWU5MmFlMzZjZDFlZDE2YzZmNTQ0NjI2In19fQ==");
	}
	public ItemStack LOVE1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTg1ODkyMjEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80YmZhZjI4ZWQxNzVmYWNmYzQzMTYyMTNmZjg2OWRlNzg0NjE2OTVmZTM1OWQ2M2Q2MzRlOThkOWE4OTgxYSJ9fX0=");
	}
	public ItemStack LOVE2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTg2NTA0NzMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMjE5ZjlmZTY3MWNmZDYxYTRhZjJkYWNjNjcwMWJjYjY1OGYzZTlkMTM1NjcxZDIyNGFkOWY5NWI4MGQzMjMifX19");
	}
	public ItemStack LOVE3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTg2ODI5MTgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZDVjZGMxMmM1NzIzY2NkMTQzZTgzZGRhOWJlODJlM2ExYjRjOWU4NWRmN2YwYzdhOTcwNjIxZmFkYWY2MThkIn19fQ==");
	}
	public ItemStack LOVE4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTg3MTQ3MDgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hZDFhZDYxZTk3Njc1MGNmNWY2MjE0YTIzYjcxMjQxOWM1OWYyOTZjNWRlOGNkNzg2NGE1OGFmYTU0Y2QwNTEifX19");
	}
	public ItemStack LOVE5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTg3Nzk2NDYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85YmE1OTczOGExNGMyYjJlYjhmM2FjZmFlMzM4M2UxZWJmM2U2MzZiZTY0ODc5ODYzMzI0ODlhY2I2MjA0MmE4In19fQ==");
	}
	public ItemStack LOVE6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTg4MTAwNTIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xOWE2NGVlY2UzMmI1YjQ3YmVkNDYyZTVjM2I0YmE4ZDQ2OWQ4YWYyN2M3NGJiYmM0NTQ1Mjk1NGExYWUifX19");
	}
	public ItemStack LOVE7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjQ3MTg4NDkwODEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zZTI5MTRhOWI3M2U5MThmOTRjNTk2ZjMwMWM2MWQ3ZWU2N2ZhYzc5ZmU3OTZmZTUxOTdiNDg2NjRmMTQ2In19fQ==");
	}
	public ItemStack TOOCOOL1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjMxNTE0NTQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82NWVlNjU0MTI0NWZkNWI5ODIxN2NlYWY5NDEwYTRhYTlmYmQ1YWJjZmI2NDZkMzMyMjI3MjlhMWQ2ZDE1OWYifX19");
	}
	public ItemStack TOOCOOL2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjMxOTcyNjMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85YWFmMWRjYmMxYTg4NTM0ZjRjNmE4Mzk2MWFkMDg1NjlmNzU1OWRjYzk2N2JiNjQ4YWM0OGVlNzBmYzRjIn19fQ==");
	}
	public ItemStack TOOCOOL3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjMyMzU1NzAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lYmE3MTU4OTgxMjMyYTUxMjFlOTU1ZWEzZjIwZTM2MTFhYjg1YmQxN2IzNzI0YWMyNDgxZGJjMGMxOTA1NCJ9fX0=");
	}
	public ItemStack TOOCOOL4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjMyNjc0MzQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81OTllYjc0ZWMyYmIwYWRkZWQxOGViZDc3N2E1YTc0NzhlODYyNTZkZWI1MzZkY2NiZmM0YWNkOWJlMmEyOCJ9fX0=");
	}
	public ItemStack TOOCOOL5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjM4MDQ5ODEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kZTJkODI2ZGY0Y2YxNjljYjhiYzI4ZWY0YTFjZTQ3YjI3M2VkNTdjNzk5M2M0MWMyMDY5YzhkMzZjZTQ3ZGY2In19fQ==");
	}
	public ItemStack TOOCOOL6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU0MjI1MjAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80NzM0ZDJiNDZhZDMzYTY4OTg2OTUxNWQxN2E4MjBmMWYyMzE5YWFkNTQ1NTRiNmJhMjhlNzllMmM5Y2U1ZWIifX19");
	}
	public ItemStack TOOCOOL7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU0OTE4MjgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hMzI0YjVjOGZlNjlkZmQwNjIxODU1ODk5NjJkYTY2ZTI4YTg5OTlhNzk3YzA0MDUzYWJjMWUzZmMxYzEifX19");
	}
	public ItemStack TOOCOOL8(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU2MDY5MzMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZTc5ZTVmOWQ4MWM4YTg0ZTFmOWMyYTgxNzY4N2YzYjk1NDNiZGJiNjA5ZWY4YzZmOTA5NzRmNzNkMGMyIn19fQ==");
	}
	public ItemStack TOOCOOL9(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU2NDU3NjQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iYWRhNDJkYjEzYzY1Nzk4MmZhNzMxODZkYjdiNTk0OTYxMjE3NmVkZTY3NTM2OTVkMmI2ZDk0YWE3MjYyYjc0In19fQ==");
	}
	public ItemStack TOOCOOL10(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU2OTMyMzcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zOTIzZjViNDJkMTY3NzE1NWQwODQ0MmMwYWEzOTg1MTU5NjE1NmM1ZTA5YjM0NjFhYTM1ODY4OTg5YTRiYiJ9fX0=");
	}
	public ItemStack RIP1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjcyMDc0MTQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zMGU3ODI4NWQ1YWVlMGIyODc4N2FkODhhNWQ1OGZiMDVjY2YyMjkxOGRhYTUxNmVhZDg1YTZiZjRmZTA2OCJ9fX0=");
	}
	public ItemStack RIP2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjcyMzg1MjMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82MzYxMWI1NzI0ZTA5MTg1NGU3OTkyNmZkMTFlNDg2YmZkMGY5OTA0MjcyMWMzYjM0MTc3ZjgxODYzOWMxOWQifX19");
	}
	public ItemStack RIP3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjcyNzM2NzQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84M2UwNjIxYjQ1ZDNhMzI2ZDIzNjI5M2NkOGVhNDlhZTc0ZDUyZTU2ZmM4ZDFkMTMzZTdmYzhiY2YyYTU5ODgifX19");
	}
	public ItemStack RIP4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjczMTE1NTIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZTE2YTdhZTE4NmMzY2ZlYWMzNjRlYWMwZTgzZDM1Mjg3NDFjM2RkOWVmODI3NzA4MGUwM2RlYWJjNzE0In19fQ==");
	}
	public ItemStack RIP5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjczNTQ5MTAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMGVjM2E4MGVkMzViZDliZWI3ZDIwY2I3NWYxZWNkNWI4YWIwZDU3NmYxZGI2OTlmN2RlZjEzMTMxZmJjNSJ9fX0=");
	}
	public ItemStack RIP6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjczODYxNjUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iMDNiYWRjYzlmYjk2NmM4N2UwZGMxMzMyZDczNWIyYjU4N2MyNjAyZDM1ZmVjYjQ0YmE2ZWQ5NGNlYjQifX19");
	}
	public ItemStack RIP7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3Njc0MTgzMDAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80MzljM2RmN2E2MjhhZjhkNzUxZWNjYTE5NzY0MmNkYzFhMDdjMzBlMzI4OWIyZDMyNjFmN2E2NWNmMzk1YiJ9fX0=");
	}
	public ItemStack SPICY1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU4ODM5MTcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80YzQ2ZWU3NmZkOWU5NDdlMzc4OWZhYzM0ODIxNmFiOThjMjQyMTI2OGZhZmFmM2ViMjllYzk0OWMwYjgyMTY5In19fQ==");
	}
	public ItemStack SPICY2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjgwNzQ5MDEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81ODRmOWVlNjg1ZWM2NTRlYTU5NDFkNzg5ODM4Nzg1ZDMyMTRlMjM2MTUzZmEyZTQ4MjI4NzZiY2ZlZjg5In19fQ==");
	}
	public ItemStack SPICY3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjgxMDkxNDgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lMzRkYWQ2YzllYWIwYmFmOWY5ZDliMGY2YmU2ZTE5OTM2YjNhMWUyMGZjM2UyMTdiODg1ZWFkZWIzMTgifX19");
	}
	public ItemStack SPICY4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjgxNDA1NzAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85NjNiYTlmODhmYTVmNTM1OWQ1Y2Q5NGU5ZGY4Y2VlYmZkY2Q0MzU1ZDViZmI1ZmU2NzMyNTFlZGUwZTdmNjNkIn19fQ==");
	}
	public ItemStack SPICY5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjgxOTI2OTIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84ZTU4YWM5OTExNDU2MTEwMzc3Nzk5Y2RmZDc1YTVmNGZkNzMxYTM4YmQyOWFiMzk4ODc5NDNhYWExMzkifX19");
	}
	public ItemStack SPICY6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjgyMjI5OTcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMWY5NmJmYzkwNWM0Njg5Njk4YzA5Y2QyY2JiODE4ODI1MTQ2YzFiYzYxODQ5NDAzM2U4MDc3Y2I5YTcwIn19fQ==");
	}
	public ItemStack SPICY7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjgyNTYwMTUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xZTU2NzU1NTdkZWRlZWVkZDQ1OTlmZDBjMGIyZWFlN2Q4ZGVmYmZjNjEzZjMyMWY1MTBjNzRjNjYyYTNhNiJ9fX0=");
	}
	public ItemStack SPICY8(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjgyODcxNTcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80OWYwNDgyY2E2YmE1OTlmYmVkNDZhMWU3YmIzMzI0NTlhYzEzMjFiYjVjYzJkYzliZjJhMmJhN2Y2MWI4In19fQ==");
	}
	public ItemStack SPICY9(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjgzMzAyMDAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84ODExZWMzNjE4ZGZjMDc1ODE4YWY4YTdmZGE1MmVlNTZjMGJiMjAzZTRkMjc4MjFlMTc4NmYyYWY1NWI5ZCJ9fX0=");
	}
	public ItemStack PEPE1(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjFlYmM3YWFkNWE2NTZkNTg0MmQ0ODExNjdiNWI3Yjk4ZWFmOWQ5MjRjMmRiYjkzNDhhMzEyMDMzMzAyNjMifX19");
	}
	public ItemStack PEPE2(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjUyYTFjNWEyYzIxNDIxY2U3ZjNlNzFhMDQ4N2RmNGZmOTRhZmI1Zjg5Y2U2ZDQ0ZWVhMDdjN2RlNGM1NDFiZCJ9fX0=");
	}
	public ItemStack KISS1(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2JhYWJlNzI0ZWFlNTljNWQxM2Y0NDJjN2RjNWQyYjFjNmI3MGMyZjgzMzY0YTQ4OGNlNTk3M2FlODBiNGMzIn19fQ==");
	}
	public ItemStack KISS2(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQ1YmQxOGEyYWFmNDY5ZmFkNzJlNTJjZGU2Y2ZiMDJiZmJhYTViZmVkMmE4MTUxMjc3Zjc3OWViY2RjZWMxIn19fQ==");
	}
	// More Emojis if needed
	public ItemStack BLUSH1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjI4ODI4OTgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jYzg2NzAzY2M1ODM5ZDQxM2UzOTNmMTczZGRlNGZiNzFjZmM5NjVlMWQyNTRhZTdkN2JiMzhiZjBhMjMzZDUifX19");
	}
	public ItemStack BLUSH2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjI5MzYzMjMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NjJjM2E2MjY1NDE4OTc3YTU2NGZhOTM3NmZiNWIxYTg3ZjlmOGI4MDUyYzYzYTJkNTE4MTc2OTFlNDIyM2EifX19");
	}
	public ItemStack BLUSH3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjI5OTAxMDMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jNmFhOWUyN2M2OWI0NzM4Y2YyNTIzZTM4NzI3MWRkZDFmNGJiMDdlZDM4YWYzYWQ5MjNkYjU3OGU4ODQwODEifX19");
	}
	public ItemStack BLUSH4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjMwMjQ1MTQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84MWIxM2IyZGNiOTRkY2ZjMzBlYzdjZTc3MDVlNmUzOGE2NDU3YzQzZWI5ZjhhZTFjNDNiYTUyNDE2M2ZhNDY5In19fQ==");
	}
	public ItemStack BLUSH5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjMwNjE0ODYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZjdlNmMwNzllZmE2OWNiM2EyM2RkM2IxNDc2NDNjN2NiNWU1YzkxMjliNzRhZjBjYWI0N2IwNGYzNTVhIn19fQ==");
	}
	public ItemStack TAN1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzUzNjQ1ODEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZDIxNzVlYmU5YWUwZTFhNjU4ZDlhZjgyZGFjZmI4MzY5MDUyZDgxMjFkNGVhMzg4NjczOGExY2NhNSJ9fX0=");
	}
	public ItemStack TAN2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU0MDM5NDksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83ZWY1NzU2MjlhMjY4OWQ2M2EzYTNlOTFiZDM0MmVjM2Y3OGI0ZjM5NzY4N2MwODMzYmY2ZDY0YmYyNmQxMmU4In19fQ==");
	}
	public ItemStack TAN3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU0NTUwMzAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yODVjNzg5YjFiYWZlYjYyNzRkNWMzMzE0ZTAzMzNjY2Y2YWI5MmQ3MzEyZWYyMTRmODk3OTNjOTU5ZDI1In19fQ==");
	}
	public ItemStack TAN4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU0ODc4ODIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lMzhhZGRlM2FhNGRmMmNmOGE1MjE2NjQzZDNmNmY5MjEzM2UxNTk5ZDUwYjJlZjQxY2VmYzgxZjFlZWMxN2MifX19");
	}
	public ItemStack TAN5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU1MjA2OTksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81NmYwYzZhNmY1NTI1ZDA4NzY3NzM3NjhlMmE3ZjZiZDQzNjA4ZDhiMTVmMGU4NzgwZjY0ZDUxMmYyMCJ9fX0=");
	}
	public ItemStack TAN6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU1NTM5MTYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mMDNlYzYyMWVmMjMxNzVlZTRlZWNjNWQxZDkyMjJiOWUyN2NhODc1OGJlZGU1MDRlM2ZhM2MwYWMxNTMyYmQifX19");
	}
	public ItemStack TAN7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU1ODU4NDgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NzhhYjM3ODFkZmU5NmU1MTliMTc0ZTQ3ZWZkYmM2ODgxNzE1ZDdhZThjYTQxYTFkOGFlNjIwZjQ3NzlkMiJ9fX0=");
	}
	public ItemStack TAN8(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU2MTc3NjYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84ZGRkYTNkZjlmZGUzMDIwOGNhZDFhMzA4NjM0Yzk1Zjk1YzliNDhhNDI3ODQyYWRjOWM4Y2NmZWM2MjZiNGYifX19");
	}
	public ItemStack TAN9(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU2ODM5OTcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83MzY1YzY2NmU3ZDk4MDQzOTdlNzZkZTM1NWVlMmU2OGQ0Yzk2OWI1ZmY3YzBhYjZhZjc3YmQ3YzdlMjY2In19fQ==");
	}
	public ItemStack TAN10(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU3MTQ5MTYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xNWUxMTIyYzgzN2E4N2NlMThkNTFlNzk3OTgzZGE2YWQzODQ3OTNmZGQ5Zjc3MjUzNzYzYWU5YjZkOWExIn19fQ==");
	}
	public ItemStack TAN11(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NzU3NDU2OTAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84MDM4ODllNDRiNTU0NjVhYmZmNWNlZGM1Yjg2ZDNiZGEzNDZiYzcwYzliZjhiOTdmY2Y3OTM5NDhmMzc5YzEifX19");
	}
	public ItemStack mustache(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYzNmYyNzI0YWE2YWE0ZGU3YWM0NmMxOWYzYzg0NWZiMTQ4NDdhNTE4YzhmN2UwM2Q3OTJjODJlZmZiMSJ9fX0=");
	}
	public ItemStack sad(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQ5NjhhYzVhZjMxNDY4MjZmYTJiMGQ0ZGQxMTRmZGExOTdmOGIyOGY0NzUwNTUzZjNmODg4MzZhMjFmYWM5In19fQ==");
	}
	// Heads for other uses
	public ItemStack tongueOut(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2ZmYWNjZjE3ODc5YjE3ODkxZmM1ZWY2NjQ3MmNjMDY2YTg1YmZhMzFiNmQ3ODZjMzJhZmVlNDc5NjA2OGQifX19");
	}
	public ItemStack derp(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2JhYWJlNzI0ZWFlNTljNWQxM2Y0NDJjN2RjNWQyYjFjNmI3MGMyZjgzMzY0YTQ4OGNlNTk3M2FlODBiNGMzIn19fQ==");
	}
	public ItemStack wink(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjRlYTJkNmY5MzlmZWZlZmY1ZDEyMmU2M2RkMjZmYThhNDI3ZGY5MGIyOTI4YmMxZmE4OWE4MjUyYTdlIn19fQ==");
	}
	public ItemStack cry(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWYxYjg3NWRlNDljNTg3ZTNiNDAyM2NlMjRkNDcyZmYyNzU4M2ExZjA1NGYzN2U3M2ExMTU0YjViNTQ5OCJ9fX0=");
	}
	public ItemStack dead(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjM3MWU0ZTFjZjZhMWEzNmZkYWUyNzEzN2ZkOWI4NzQ4ZTYxNjkyOTk5MjVmOWFmMmJlMzAxZTU0Mjk4YzczIn19fQ==");
	}
	public ItemStack kiss(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQ1YmQxOGEyYWFmNDY5ZmFkNzJlNTJjZGU2Y2ZiMDJiZmJhYTViZmVkMmE4MTUxMjc3Zjc3OWViY2RjZWMxIn19fQ==");
	}
	public ItemStack embarrased(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjcyMGRmOTExYzA1MjM3NzA2NTQwOGRiNzhhMjVjNjc4Zjc5MWViOTQ0YzA2MzkzNWFlODZkYmU1MWM3MWIifX19");
	}
	public ItemStack scared(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjM2ZTI2YzQ0NjU5ZTgxNDhlZDU4YWE3OWU0ZDYwZGI1OTVmNDI2NDQyMTE2ZjgxYjU0MTVjMjQ0NmVkOCJ9fX0=");
	}
	// Bad emojis
	public ItemStack angel(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UxZGViYzczMjMxZjhlZDRiNjlkNWMzYWMxYjFmMThmMzY1NmE4OTg4ZTIzZjJlMWJkYmM0ZTg1ZjZkNDZhIn19fQ==");
	}
	public ItemStack surprised(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmMyYjliOWFlNjIyYmQ2OGFkZmY3MTgwZjgyMDZlYzQ0OTRhYmJmYTEzMGU5NGE1ODRlYzY5MmU4OTg0YWIyIn19fQ==");
	}
	public ItemStack SURPRISED1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODIwNTQ2NzksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xMTE1YjI3YmQxZDc5YjY4OGY5MTA2YTNmYzIzMzc0YTI0YzdmNzNlZjlkOTNmNTY3MjYxYTRmZWZmZDczIn19fQ==");
	}
	public ItemStack SURPRISED2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODIzOTc0OTgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iMzI4ZGIxYzMyMzU4NWFkZWJhMTkwN2NlZDMwNjA1MGUwMmFhNzc1OTE1ODhmYjE4MmZkZWFmNDIzYWQ2In19fQ==");
	}
	public ItemStack SURPRISED3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODI0NzY0ODcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZGJkZTBjNDRlOTlmOGRhNjQ4OGE5MzU4OGE4MmFhYmQ0YTFmYzYwM2VkMmFhZjUxZDU2NzFjM2Q0ZCJ9fX0=");
	}
	public ItemStack SURPRISED4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODI1MTA0MTIsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80NTg2ODUyOWZiZjRiZTYyOTM3MTI3NWIxMTM4ZGFiOTI5NTc2MDIxNzE2ZWU3MzdkYjEyNjM0YWExMjVhZjMifX19");
	}
	public ItemStack LOVESTRUCK1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjExNTcwNjMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85MDFiOTU4ZWQyYzM2ZTQ1YmFlNzJiNDJkNGVlNzE5ZDQ1MjQwYjIzMzY2OTA5MWIxY2M5ZTA3MGUzMTExOSJ9fX0=");
	}
	public ItemStack LOVESTRUCK2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjExOTMwMTUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84OTVmNjQxNWJkOTQyNGE2NjRkNjk0MzcxYTg0NjgzOGMyMGZiMzZjM2I0YTIyZjM4NWZlN2UzZGNlMjk5NiJ9fX0=");
	}
	public ItemStack LOVESTRUCK3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjEyMjUwMzksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85NmZiYjUyYTRkMGM2MmQ4ZTZjYWU4YzQ4NWU1NTFiMzdmZWM2OGU2ZGFhYjIzZDg1ZjJmZjUyZmFhNGM0In19fQ==");
	}
	public ItemStack LOVESTRUCK4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjEyNTYyMzMsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZDI2YWU0YjU3OTNkMDg3ZTYyYTJjZjNmMzQzNTk4MjlkMDI4NjlhYWU2NjI2YmZjZmY1OWRlMTQ2OWY1MSJ9fX0=");
	}
	public ItemStack RELAX1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjE2Nzg5MDQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85MjdlYmJmNWMyNTM1ZmU2YjVjZWY4YjhhN2UxZTcwNjdhMzllZDIxYmE1NDdmODNmY2U0NDcyMTg0ZDgwYzcifX19");
	}
	public ItemStack RELAX2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjE3MzI3NDYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZDJkYzRkYjc4MDI5NDkxOTUxNzMwNjk2NGQ2NWVhMDc4YjQ3YjgyM2ZkYTU2MjhiZTQ4YjZhZTYxYyJ9fX0=");
	}
	public ItemStack RELAX3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjE3NjQ2NDUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNzQ0ZTYxYjQ2YTVmNzQ5Y2NhYTJiZjgxMzI5ODFiODk4NjIzNDM1OWY5YTI5MjRmNTQ3ZWMwMTExZTQzNzUifX19");
	}
	public ItemStack RELAX4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjIxMjkxNjQsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mNjczZGQyY2VkNzhkODlkYWM4NDVkMzk1NDQzZDZkMzliY2ZlOWM3NGM3MWE2NTIwMjk0ODdiOTI4MSJ9fX0=");
	}
	public ItemStack RELAX5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjI0MTM2NjUsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83MjBmMTU4ZWE3NzFhZGYwODYyZTdjMWExOWYwMTQwOWNkZGQ2ZWRmZDY0Mzg1NjgyZWQ3YmM2NTNlZGEzIn19fQ==");
	}
	public ItemStack RELAX6(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjI0NzAyOTgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iNmQ2NTgxZWUwZWM5M2NhOWQ1ZjRhZmJmNmUyOGY1YTk1ODJhODk2Y2NjY2I5ZTdjMTdlNjQxOWU1OTdlMjcifX19");
	}
	public ItemStack RELAX7(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjI3MzYwMjgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lMDg4NzZhNDliMWFiYmFkMTQ5NzI0YmUzZWFlMzVhYTYzMDVjNTI5ZTM4NGMxMThiYTM4MWE4MWUyZGY1OWUifX19");
	}
	public ItemStack COOL(final String name) {
		return createSkull(name, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODY4ZjRjZWY5NDlmMzJlMzNlYzVhZTg0NWY5YzU2OTgzY2JlMTMzNzVhNGRlYzQ2ZTViYmZiN2RjYjYifX19");
	}
	public ItemStack COOL1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODEwNDY2MjYsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kMzYxMTA2OWMyNWFkOWIzMmE3YmRmNDg1MzQ5MzViOWY1ODc1Y2Q4NTQwMWJjMGEwMzQ2NTU2NDcxZDQ4In19fQ==");
	}
	public ItemStack COOL2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODEyMTUyNzksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zNzFhNjYxNjQ4OTZkY2Q4YjE3YmZlOWJiZjVlMWU4YzQzYWZiYmQ3YzdlNDJiNzUxNDgzYzljNDY4ZTUyIn19fQ==");
	}
	public ItemStack COOL3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjM2ODEyNDgxMzAsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80NGM4Y2QzMWJhYzY1N2EzZjI2YjUyZTI2MmM4M2ViODMzOGQ1NjY1MWZkMjJjMTYzMzU2NWYzZGJiYzQ1Nzc3In19fQ==");
	}
	public ItemStack DIZZY1(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU4ODM5MTcsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80YzQ2ZWU3NmZkOWU5NDdlMzc4OWZhYzM0ODIxNmFiOThjMjQyMTI2OGZhZmFmM2ViMjllYzk0OWMwYjgyMTY5In19fQ==");
	}
	public ItemStack DIZZY2(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU4MzcwNDksInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80MTZmM2RjZTk3N2Q4Yjc5N2UxZTQ3NmY1YWI5MzYxOWVkMmYyYjIxYTQ5YWM5Mzc0MzE0MGNmNjdhMDg4In19fQ==");
	}
	public ItemStack DIZZY3(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU5MzI0OTEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82MTgwM2E3MTMyZmJlNTllNjUwNjBkZGVjNjI2M2Q2NzRhMWRhNmQyMzhjZTBhZGExNDBlNjc5OWIyODU1OWQifX19");
	}
	public ItemStack DIZZY4(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU3NjU5Njg3MDgsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kODkyZmM1NWIyMzRiM2I1MzU2M2NkNDhmOThjMjZlOGUzZjdlNWFlMzUyM2I2ZWVkODliMjYyZWNmMWM1ZDUifX19");
	}
	public ItemStack DIZZY5(final String name) {
		return createSkull(name, "eyJ0aW1lc3RhbXAiOjE0NjU5MjI2MzUyNjEsInByb2ZpbGVJZCI6ImY1ODgyOTVlY2UyZDRhODZiMTA5YzBkMTI4MDM0YjFkIiwicHJvZmlsZU5hbWUiOiJfU2FzaGllXyIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iYTE1MWE1NWViYTczNGYxYzY1MWJiYzdlNzE3Y2M4YTdmZjU1NzVkNjgwNGYxMzdkMWNkMjE2MTMxNDk4OWYifX19");
	}
}