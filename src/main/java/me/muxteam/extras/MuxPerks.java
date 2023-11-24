package me.muxteam.extras;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class MuxPerks implements Listener {
	private MuxSystem ms;
	private final Set<String> nohunger = new HashSet<>(), doubleexp = new HashSet<>(), antipoison = new HashSet<>(),
	noweakness = new HashSet<>();
	private final Set<String> fastbreaklimit = new HashSet<>();

	public MuxPerks(final MuxSystem ms) {
		this.ms = ms;
		ms.getServer().getPluginManager().registerEvents(this, ms);
	}
	public void close() {
		this.ms = null;
	}
	public boolean hasNoHunger(final String pname) {
		return nohunger.contains(pname);
	}
	public boolean hasDoubleEXP(final String pname) {
		return doubleexp.contains(pname);
	}
	public boolean hasAntiPoison(final String pname) {
		return antipoison.contains(pname);
	}
	public boolean hasNoWeakness(final String pname) {
		return noweakness.contains(pname);
	}
	public void deactivatePerks(final String pname) {
		nohunger.remove(pname);
		doubleexp.remove(pname);
		noweakness.remove(pname);
		antipoison.remove(pname);
	}
	public void activatePerks(final Player p) {
		activatePerks(p, true);
	}
	public void activatePerks(final Player p, final boolean delay) {
		try {
			final MuxExtraUser eu = ms.getExtras().getExtraUser(p.getUniqueId());
			final PerkStore ps = eu.getPerks();
			if (ps != null && ps.getActive().isEmpty() == false) {
				if (delay == false) {
					final Set<Byte> active = ps.getActive();
					active.forEach(b -> activatePerk(p, b));
					return;
				}
				new BukkitRunnable() {
					final Set<Byte> active = ps.getActive();
					@Override public void run() {
						active.forEach(b -> activatePerk(p, b));
					}
				}.runTaskLater(ms, 0L);
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}
	public void activatePerk(final Player p, final byte b) {
		switch (b) {
		case 1: nohunger.add(p.getName()); p.setFoodLevel(20); break;
		case 2: p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 9999999, 1)); break;
		case 3: p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 9999999, 0)); break;
		case 4: p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 9999999, 1)); break;
		case 5: doubleexp.add(p.getName()); break;
		case 6: p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 9999999, 0)); break;
		case 7: p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 9999999, 1)); break;
		case 8: antipoison.add(p.getName()); break;
		case 9: p.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 9999999, 1)); break;
		case 10: noweakness.add(p.getName()); break;
		default: break;
		}
	}
	public void removeActivePerk(final Player p, final byte b) {
		switch (b) {
		case 1:
			nohunger.remove(p.getName());
			break;
		case 2:
			p.removePotionEffect(PotionEffectType.JUMP);
			break;
		case 3:
			p.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
			break;
		case 4:
			p.removePotionEffect(PotionEffectType.SPEED);
			break;
		case 5:
			doubleexp.remove(p.getName());
			break;
		case 6:
			p.removePotionEffect(PotionEffectType.NIGHT_VISION);
			break;
		case 7:
			p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
			break;
		case 8:
			antipoison.remove(p.getName());
			break;
		case 9:
			p.removePotionEffect(PotionEffectType.FAST_DIGGING);
			break;
		case 10:
			noweakness.remove(p.getName());
			break;
		default:
			break;
		}
	}
	public String getPerkName(final byte b) {
		switch (b) {
			case 1: return "No Hunger";
			case 2: return "High Jump";
			case 3: return "Anti Fire";
			case 4: return "Runner";
			case 5: return ms.getMultiplier(1 + ms.getEvents().getExpBoost()) + " EXP";
			case 6: return "Night Vision";
			case 7: return "Power";
			case 8: return "Anti Poison";
			case 9: return "Fast Break";
			case 10: return "No Weakness";
			default: return "Unbekannt";
		}
	}
	public byte getPerkFromMaterial(final Material m) {
		switch (m) {
			case APPLE: return 1;
			case HOPPER: return 2;
			case MAGMA_CREAM: return 3;
			case DIAMOND_BOOTS: return 4;
			case EXP_BOTTLE: return 5;
			case EYE_OF_ENDER: return 6;
			case FIREBALL: return 7;
			case INK_SACK: return 8;
			case DIAMOND_PICKAXE: return 9;
			case FERMENTED_SPIDER_EYE: return 10;
			default: return -1;
		}
	}
	@EventHandler
	public void onBreak(final BlockBreakEvent e) {
		final Player p = e.getPlayer();
		final Material m = e.getBlock().getType();
		if (m == Material.DIAMOND_ORE || m == Material.GOLD_ORE || m == Material.IRON_ORE || m == Material.EMERALD_ORE || m == Material.QUARTZ_ORE) {
			final MuxExtraUser eu = ms.getExtras().getExtraUser(p.getUniqueId());
			final boolean hasfastbreak = eu.getPerks().getActive().contains((byte) 9);
			if (hasfastbreak && ms.checkLimit(p.getName(), "FASTBREAKMINE", 850, true)) {
				fastbreaklimit.add(p.getName());
				final PerkStore es = eu.getPerks();
				es.remActive((byte) 9);
				eu.setPerks(es);
				p.playSound(p.getLocation(), Sound.ANVIL_BREAK, 1F, 1F);
				removeActivePerk(p, (byte) 9);
			}
		}
	}
	public boolean hasFastBreakBlock(final String pname) {
		return fastbreaklimit.contains(pname);
	}
	public final class PerkStore {
		private final Set<Byte> active, owned;

		public PerkStore(final Set<Byte> active, final Set<Byte> owned) {
			this.active = active;
			this.owned = owned;
		}

		public Set<Byte> getActive() {
			return active;
		}
		public void addActive(final byte active) {
			this.active.add(active);
		}
		public void remActive(final Byte active) {
			this.active.remove(active);
		}
		public Set<Byte> getOwned() {
			return owned;
		}
		public void addOwned(final byte perk) {
			owned.add(perk);
		}
	}
}
