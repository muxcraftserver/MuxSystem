package me.muxteam.extras;

import me.muxteam.extras.MuxPerks.PerkStore;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.ranks.MuxRanks;
import me.muxteam.shared.MuxSharedPackets;
import me.muxteam.shop.MuxGemShop;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MuxExtras implements Listener {
    private MuxSystem ms;
    private final Map<UUID, MuxExtraUser> extras = new HashMap<>();
    private ItemStack fH, fC, fL, fB, fS, fBow, fP, fA, fK, fMP, fMA, fMS;
    private final Set<Player> bloodbending = new HashSet<>();
    private final Map<Player, Map<Entity, Location>> bloodtargeted = new ConcurrentHashMap<>();

    public MuxExtras(final MuxSystem ms) {
        this.ms = ms;
        ms.getServer().getPluginManager().registerEvents(this, ms);
        new BukkitRunnable() {
            @Override
            public void run() {
                processBloodBending();
            }
        }.runTaskTimer(ms, 20L, 1L);
        setup();
    }

    public void close() {
        this.ms = null;
    }

    private void setup() {
        fH = new ItemStack(Material.DIAMOND_HELMET);
        fC = new ItemStack(Material.DIAMOND_CHESTPLATE);
        fL = new ItemStack(Material.DIAMOND_LEGGINGS);
        fB = new ItemStack(Material.DIAMOND_BOOTS);
        fS = new ItemStack(Material.DIAMOND_SWORD);
        fBow = new ItemStack(Material.BOW);
        fP = new ItemStack(Material.DIAMOND_PICKAXE);
        fA = new ItemStack(Material.DIAMOND_AXE);
        fK = new ItemStack(Material.POTATO_ITEM);
        fMP = new ItemStack(Material.DIAMOND_PICKAXE);
        fMA = new ItemStack(Material.DIAMOND_AXE);
        fMS = new ItemStack(Material.DIAMOND_SPADE);
        final Map<Enchantment, Integer> aEnch = new HashMap<>();
        aEnch.put(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        aEnch.put(Enchantment.PROTECTION_FIRE, 4);
        aEnch.put(Enchantment.PROTECTION_EXPLOSIONS, 4);
        aEnch.put(Enchantment.PROTECTION_PROJECTILE, 4);
        aEnch.put(Enchantment.DURABILITY, 3);
        fH.addEnchantments(aEnch);
        fH.addEnchantment(Enchantment.OXYGEN, 3);
        fH.addEnchantment(Enchantment.WATER_WORKER, 1);
        fC.addEnchantments(aEnch);
        fL.addEnchantments(aEnch);
        fB.addEnchantments(aEnch);
        fB.addEnchantment(Enchantment.PROTECTION_FALL, 4);
        fB.addEnchantment(Enchantment.DEPTH_STRIDER, 3);
        fS.addEnchantment(Enchantment.DAMAGE_ALL, 5);
        fS.addEnchantment(Enchantment.DAMAGE_UNDEAD, 5);
        fS.addEnchantment(Enchantment.DAMAGE_ARTHROPODS, 5);
        fS.addEnchantment(Enchantment.FIRE_ASPECT, 2);
        fS.addEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
        fS.addEnchantment(Enchantment.DURABILITY, 3);
        fBow.addEnchantment(Enchantment.ARROW_DAMAGE, 5);
        fBow.addEnchantment(Enchantment.ARROW_KNOCKBACK, 2);
        fBow.addEnchantment(Enchantment.ARROW_FIRE, 1);
        fBow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        fBow.addEnchantment(Enchantment.DURABILITY, 3);
        fP.addUnsafeEnchantment(Enchantment.DIG_SPEED, 6);
        fP.addUnsafeEnchantment(Enchantment.DURABILITY, 4);
        fP.addEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 3);
        fA.addEnchantment(Enchantment.DIG_SPEED, 5);
        fA.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
        fA.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
        fA.addEnchantment(Enchantment.DURABILITY, 3);
        fK.addUnsafeEnchantment(Enchantment.KNOCKBACK, 10);
        fMP.addEnchantment(Enchantment.DIG_SPEED, 5);
        fMA.addEnchantment(Enchantment.DIG_SPEED, 5);
        fMS.addEnchantment(Enchantment.DIG_SPEED, 5);
        final ItemMeta im = fMP.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        fMP.setItemMeta(im);
        final ItemMeta im2 = fMA.getItemMeta();
        im2.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        fMA.setItemMeta(im2);
        final ItemMeta im3 = fMS.getItemMeta();
        im3.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        fMS.setItemMeta(im3);
    }

    public void setExtraData(final UUID uuid, final MuxExtraUser user) {
        final Player p = ms.getServer().getPlayer(uuid);
        if (p != null) {
            if (user == null) {
                extras.put(uuid, new MuxExtraUser(uuid, p.getName(), ms.getPerks().new PerkStore(new HashSet<>(), new HashSet<>()),
                        ms.getMounts().new MountStore(new HashSet<>(), (short) -1), ms.getPets().new PetStore(new HashSet<>(), null), new HashSet<>(), new HashSet<>(), (byte) 0));
                return;
            }
            extras.put(uuid, user);
            ms.getPerks().activatePerks(p);
        }
    }

    public void saveExtraUser(final MuxExtraUser u) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.getDB().saveExtras(u);
            }
        }.runTaskAsynchronously(ms);
    }

    public void handleJoin(final Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                final MuxExtraUser extrauser = ms.getDB().loadExtras(p.getUniqueId());
                if (extrauser != null) {
                    extrauser.setName(p.getName());
                }
                setExtraData(p.getUniqueId(), extrauser);
            }
        }.runTaskAsynchronously(ms);
    }

    public void handleQuit(final Player p) {
        bloodbending.remove(p);
        ms.getDB().saveExtras(extras.remove(p.getUniqueId()));
    }

    public MuxExtraUser getExtraUser(final UUID uuid) {
        final MuxExtraUser user = extras.get(uuid);
        if (user == null) {
            return new MuxExtraUser(uuid, "N/A", ms.getPerks().new PerkStore(new HashSet<>(), new HashSet<>()),
                    ms.getMounts().new MountStore(new HashSet<>(), (short) -1), ms.getPets().new PetStore(new HashSet<>(), null), new HashSet<>(), new HashSet<>(), (byte) 0);
        }
        return user;
    }

    public boolean hasntCommand(final UUID uuid, final String name) {
        final Set<String> list = getExtraUser(uuid).getExtraCMDs();
        return list.contains(name) == false;
    }

    public boolean hasPerks(final UUID uuid) {
        final Set<Byte> owned = getExtraUser(uuid).getPerks().getOwned();
        return owned != null && owned.isEmpty() == false;
    }

    public PerkStore getPerks(final UUID uuid) {
        return getExtraUser(uuid).getPerks();
    }

    public boolean hasExtraEnderChest(final UUID uuid) {
        return getExtraUser(uuid).getEnderChest() == (byte) 1;
    }

    public void setExtraEnderChest(final UUID uuid, final MuxExtraUser user, final boolean b) {
        final Player pl = ms.getServer().getPlayer(uuid);
        if (pl != null) {
            final InventoryView openinv = pl.getOpenInventory();
            if (openinv != null && (openinv.getTitle().contains("Erweiterte Enderkiste") || openinv.getType() == InventoryType.ENDER_CHEST)) {
                pl.closeInventory();
            }
        }
        if (b) user.setEnderChest((byte) 1);
        else user.setEnderChest((byte) 0);
    }

    public void handleInventory(final Player p, final ItemStack i, final String iname) {
        handleInventory(p, i, iname, -1);
    }

    public void handleInventory(final Player p, final ItemStack i, final String iname, final int rawslot) {
        final UUID uuid = p.getUniqueId();
        final Material m = i.getType();
        if (m == Material.INK_SACK && i.getDurability() == 8) {
            if (iname.endsWith("Perks")) {
                p.closeInventory();
                p.sendMessage(ms.header((byte) 12, "§6"));
                ms.chatClickHoverLink(p, "  " + i.getItemMeta().getDisplayName().replace(" kaufen", ""), ms.getLang("extras.perkshover"), "https://shop.muxcraft.eu/?ign=" + p.getName());
                p.sendMessage(" ");
                ms.chatClickHoverLink(p, "  " + ms.getLang("extras.clickforperks"), ms.getLang("extras.perkshover"), "https://shop.muxcraft.eu/?ign=" + p.getName());
                p.sendMessage(ms.footer((byte) 12, "§6"));
                p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
            } else if (iname.endsWith("Mounts")) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                if (i.getItemMeta().hasLore() && i.getItemMeta().getLore().get(1).contains("Liga")) {
                    ms.getPvP().openLeagueInv(p);
                    return;
                }
                ms.getGemShop().openInventory(p, MuxGemShop.GemShopCategory.MOUNTS);
            } else if (iname.endsWith("Pets")) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                ms.getGemShop().openInventory(p, MuxGemShop.GemShopCategory.PETS);
            } else if (iname.endsWith("Befehle")) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                ms.getGemShop().openInventory(p, MuxGemShop.GemShopCategory.EXTRA_COMMANDS);
            } else if (iname.endsWith("Emojis")) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                ms.getGemShop().openInventory(p, MuxGemShop.GemShopCategory.EMOJIS);
            } else {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            }
            return;
        } else if (rawslot == 4) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        } else if (m == Material.ITEM_FRAME) {
            final String display = i.getItemMeta().getDisplayName();
            if (display.contains("Vorteile")) {
                p.playSound(p.getLocation(), Sound.CLICK, 0.4F, 1F);
                p.performCommand("rank");
                return;
            } else if (iname.contains("/extras")) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("menu");
                return;
            } else if (iname.startsWith("§0§lMuxExtras") && display.contains("(")) {
                final String plname = ChatColor.stripColor(iname).split("\\| ")[1];
                final OfflinePlayer op = ms.getPlayer(plname);
                if (op == null) return;
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                ms.getOverview().openDataManagement(p, op);
                return;
            } else if (iname.startsWith("§0§lMuxExtras") == false) {
                final String plname = ChatColor.stripColor(iname).split("\\| ")[1];
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("extras " + plname);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openExtras(p, false);
            return;
        } else if (p.hasMetadata("extradelay")) {
            return;
        } else if (m == Material.ENDER_CHEST) {
            if (ms.isFullTrusted(p.getName()) == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final OfflinePlayer op = ms.getPlayer(ChatColor.stripColor(iname).split("\\| ")[1]);
            if (op == null) return;
            final MuxExtraUser user = op.isOnline() ? getExtraUser(op.getUniqueId()) : ms.getDB().loadExtras(op.getUniqueId());
            if (user == null) return;
            final boolean remove = user.getEnderChest() == (byte) 1;
            setExtraEnderChest(uuid, user, remove == false);
            saveUserAndRehandleCommand(p, user, "extraender", op.getName(), remove, "ENDERCHEST");
            ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateExtraEnderChest(user.getUUID(), remove == false, p.getUniqueId()));
            return;
        } else if (iname.startsWith("§0§lMuxPets")) {
            if (ms.isFullTrusted(p.getName()) == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final EntityType type = EntityType.fromId(i.getDurability());
            final OfflinePlayer op = ms.getPlayer(ChatColor.stripColor(iname).split("\\| ")[1]);
            final MuxExtraUser user = op.isOnline() ? getExtraUser(op.getUniqueId()) : ms.getDB().loadExtras(op.getUniqueId());
            if (user == null) return;
            final Set<EntityType> owned = user.getPets().getOwned();
            final boolean remove = i.getItemMeta().getLore().stream().anyMatch(s -> s.contains("§c"));
            if (remove) {
                if (user.getPets().getActive() == type) {
                    if (op.isOnline())
                        ms.getPets().setPet(op.getPlayer(), null, false);
                    user.getPets().setActive(null);
                }
                owned.remove(type);
            } else {
                owned.add(type);
            }
            saveUserAndRehandleCommand(p, user, "pets", op.getName(), remove, ms.getPets().getPetName(type));
            ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateExtraPet(user.getUUID(), remove == false, type, p.getUniqueId()));
            return;
        } else if (iname.startsWith("§0§lMuxMounts")) {
            if (ms.isFullTrusted(p.getName()) == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final short num = ms.getMounts().getMountFromMaterial(i.getType());
            if (num == -1) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final OfflinePlayer op = ms.getPlayer(ChatColor.stripColor(iname).split("\\| ")[1]);
            final MuxExtraUser user = op.isOnline() ? getExtraUser(op.getUniqueId()) : ms.getDB().loadExtras(op.getUniqueId());
            if (user == null) return;
            final Set<Short> owned = user.getMounts().getOwned();
            final boolean remove = i.getItemMeta().getLore().stream().anyMatch(s -> s.contains("§c"));
            if (remove) {
                if (user.getMounts().getActive() == num) {
                    user.getMounts().setActive((short) -1);
                    if (op.isOnline()) {
                        ms.getMounts().removeMount(op.getUniqueId());
                    }
                }
                owned.remove(num);
            } else {
                owned.add(num);
            }
            saveUserAndRehandleCommand(p, user, "mounts", op.getName(), remove, ms.getMounts().getMountName(num));
            ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateExtraMount(user.getUUID(), remove == false, num, p.getUniqueId()));
            return;
        } else if (iname.startsWith("§0§lMuxBefehle")) {
            if (ms.isFullTrusted(p.getName()) == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            if (m == Material.AIR || m == null) return;
            final String extracmd = ChatColor.stripColor(i.getItemMeta().getDisplayName().replace("/", ""));
            if (getExtraCommands().contains(extracmd) == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final OfflinePlayer op = ms.getPlayer(ChatColor.stripColor(iname).split("\\| ")[1]);
            final MuxExtraUser user = op.isOnline() ? getExtraUser(op.getUniqueId()) : ms.getDB().loadExtras(op.getUniqueId());
            if (user == null) return;
            final Set<String> owned = user.getExtraCMDs();
            final boolean remove = i.getItemMeta().getLore().stream().anyMatch(s -> s.contains("§c"));
            if (remove) {
                owned.remove(extracmd);
            } else {
                owned.add(extracmd);
            }
            saveUserAndRehandleCommand(p, user, "extracmd", op.getName(), remove, extracmd);
            ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateExtraCommand(user.getUUID(), remove == false, extracmd, p.getUniqueId()));
            return;
        } else if (iname.startsWith("§0§lMuxPerks")) {
            if (ms.isFullTrusted(p.getName()) == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final byte perk = ms.getPerks().getPerkFromMaterial(m);
            if (perk == -1) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final OfflinePlayer op = ms.getPlayer(ChatColor.stripColor(iname).split("\\| ")[1]);
            final MuxExtraUser user = op.isOnline() ? getExtraUser(op.getUniqueId()) : ms.getDB().loadExtras(op.getUniqueId());
            if (user == null) return;
            final PerkStore ps = user.getPerks();
            final boolean remove = i.getItemMeta().getLore().stream().anyMatch(s -> s.contains("§c"));
            if (remove) {
                if (ps.getActive().contains(perk)) {
                    if (op.isOnline())
                        ms.getPerks().removeActivePerk(op.getPlayer(), perk);
                    ps.getActive().remove(perk);
                }
                ps.getOwned().remove(perk);
            } else {
                ps.getOwned().add(perk);
            }
            saveUserAndRehandleCommand(p, user, "perks", op.getName(), remove, ms.getPerks().getPerkName(perk));
            ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateExtraPerk(user.getUUID(), remove == false, perk, p.getUniqueId()));
            return;
        } else if (iname.startsWith("§0§lMuxEmojis")) {
            if (ms.isFullTrusted(p.getName()) == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            if (i.getItemMeta() == null) {
                return;
            }
            final short num = ms.getEmojis().getEmojiFromName(ChatColor.stripColor(i.getItemMeta().getDisplayName()));
            if (num == -1) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final OfflinePlayer op = ms.getPlayer(ChatColor.stripColor(iname).split("\\| ")[1]);
            final MuxExtraUser user = op.isOnline() ? getExtraUser(op.getUniqueId()) : ms.getDB().loadExtras(op.getUniqueId());
            if (user == null) return;
            final Set<Short> owned = user.getEmojis();
            final boolean remove = i.getItemMeta().getLore().stream().anyMatch(s -> s.contains("§c"));
            if (remove) {
                if (op.isOnline() && ms.getEmojis().getActiveEmoji(op.getUniqueId()) == num) {
                    ms.getEmojis().setEmoji(op.getPlayer(), -1);
                }
                owned.remove(num);
            } else {
                owned.add(num);
            }
            saveUserAndRehandleCommand(p, user, "emojis", op.getName(), remove, ms.getEmojis().getEmojiName(num));
            ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateExtraEmoji(user.getUUID(), remove == false, num, p.getUniqueId()));
            return;
        } else if (iname.endsWith("Perks")) {
            final byte perk = ms.getPerks().getPerkFromMaterial(m);
            if (perk == -1) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final MuxExtraUser extrauser = getExtraUser(uuid);
            final PerkStore es = extrauser.getPerks();
            if (es != null) {
                if (es.getActive().contains(perk)) {
                    es.remActive(perk);
                    extrauser.setPerks(es);
                    ms.getPerks().removeActivePerk(p, perk);
                } else {
                    if (perk == (byte) 9 && ms.getPerks().hasFastBreakBlock(p.getName())) {
                        ms.showItemBar(p, "§cDu hast für heute bereits zu viele Erze gefarmt.");
                        return;
                    } else if (perk == (byte) 2 && ms.inCasino(p)) {
                        ms.showItemBar(p, "§cDu kannst das Perk hier nicht aktivieren.");
                        return;
                    }
                    es.addActive(perk);
                    ms.getPerks().activatePerk(p, perk);
                    extrauser.setPerks(es);
                }
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            handleInventory(p, new ItemStack(Material.BLAZE_POWDER), "");
            return;
        } else if (iname.endsWith("Befehle")) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        } else if (iname.endsWith("Pets")) {
            if ((p.getWorld().equals(ms.getWarpsWorld()) && ms.getShopRegion() != null && ms.getShopRegion().contains(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ()))) {
                ms.showItemBar(p, "§cPets sind hier deaktiviert.");
                p.closeInventory();
                return;
            }
            final EntityType type = EntityType.fromId(i.getDurability());
            if (ms.getPets().setPet(p, type, true)) {
                final MuxPets.PetStore petStore = getExtraUser(p.getUniqueId()).getPets();
                petStore.setActive(ms.getPets().getPetType(p.getUniqueId()));
                getExtraUser(p.getUniqueId()).setPets(petStore);
            } else {
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            handleInventory(p, new ItemStack(Material.BONE), "");
            return;
        } else if (iname.endsWith("Mounts")) {
            final short num = ms.getMounts().getMountFromMaterial(i.getType());
            if (ms.inBattle(p.getName(), p.getLocation())) {
                ms.showItemBar(p, "§cMounts sind im Kampf deaktiviert.");
                p.closeInventory();
                return;
            } else if (ms.inCasino(p) || (p.getWorld().equals(ms.getWarpsWorld()) && ms.getShopRegion() != null && ms.getShopRegion().contains(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ()))) {
                ms.showItemBar(p, "§cMounts sind hier deaktiviert.");
                p.closeInventory();
                return;
            }
            final MuxMounts.MountStore mountStore = getExtraUser(p.getUniqueId()).getMounts();
            ms.getMounts().setMount(p, num, true);
            mountStore.setActive(mountStore.getActive() == num ? -1 : num);
            getExtraUser(p.getUniqueId()).setMounts(mountStore);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            handleInventory(p, new ItemStack(Material.IRON_BARDING), "");
            return;
        } else if (iname.endsWith("Emojis")) {
            final short num = ms.getEmojis().getEmojiFromName(ChatColor.stripColor(i.getItemMeta().getDisplayName().split(" ")[0]));
            if (ms.inBattle(p.getName(), p.getLocation())) {
                ms.showItemBar(p, "§cEmojis sind im Kampf deaktiviert.");
                p.closeInventory();
                return;
            }
            if (ms.getEmojis().setEmoji(p, num) == false) {
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            handleInventory(p, new ItemStack(Material.SKULL_ITEM), "");
            return;
        }
        final String plname = iname.contains("|") == false || iname.contains("/extras") ? null : ChatColor.stripColor(iname).split("\\| ")[1];
        if (plname != null) {
            final String cmd = m == Material.BLAZE_POWDER ? "perks" : m == Material.COMMAND ? "extracmd" : m == Material.SKULL_ITEM ? "emojis" : m == Material.BONE ? "pets" : m == Material.IRON_BARDING ? "mounts" : null;
            if (cmd != null) p.performCommand(cmd + " " + plname);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        }
        final Inventory iextra;
        final ItemStack inksack = new ItemStack(Material.INK_SACK, 1, (byte) 8);
        if (m == Material.BLAZE_POWDER) {
            iextra = ms.getServer().createInventory(null, 45, "§0§lMuxExtras§0 | Perks");
            final PerkStore ps = getExtraUser(uuid).getPerks();
            final Set<Byte> owned = ps != null ? ps.getOwned() : new HashSet<>();
            Set<Byte> active = new HashSet<>();
            if (ps != null) active = ps.getActive();
            iextra.setItem(4, ms.renameItemStack(new ItemStack(Material.BLAZE_POWDER), ms.getLang("extras.perks"), ms.getLang("extras.perkslore1"), ms.getLang("extras.perkslore2"), ms.getLang("extras.perkslore3")));
            setPerkItem(iextra, active, owned, inksack, 19, "No Hunger", (byte) 1, new ItemStack(Material.APPLE), ms.getLang("extras.nohunger1"));
            setPerkItem(iextra, active, owned, inksack, 20, "High Jump", (byte) 2, new ItemStack(Material.HOPPER), ms.getLang("extras.highjump1"));
            setPerkItem(iextra, active, owned, inksack, 21, "Anti Fire", (byte) 3, new ItemStack(Material.MAGMA_CREAM), ms.getLang("extras.antifire1"));
            setPerkItem(iextra, active, owned, inksack, 22, "Runner", (byte) 4, new ItemStack(Material.DIAMOND_BOOTS), ms.getLang("extras.runner1"));
            setPerkItem(iextra, active, owned, inksack, 23, ms.getMultiplier(1 + ms.getEvents().getExpBoost()) + " EXP", (byte) 5, new ItemStack(Material.EXP_BOTTLE), ms.getLang("extras.doubleexp1"));
            setPerkItem(iextra, active, owned, inksack, 24, "Night Vision", (byte) 6, new ItemStack(Material.EYE_OF_ENDER), ms.getLang("extras.nightvision1"));
            setPerkItem(iextra, active, owned, inksack, 25, "Power", (byte) 7, new ItemStack(Material.FIREBALL), ms.getLang("extras.power1"));
            setPerkItem(iextra, active, owned, inksack, 28, "Anti Poison", (byte) 8, new ItemStack(Material.INK_SACK, 1, (byte) 2), ms.getLang("extras.antipoison1"));
            setPerkItem(iextra, active, owned, inksack, 29, "Fast Break", (byte) 9, new ItemStack(Material.DIAMOND_PICKAXE), "§7Doppelt so schnell abbauen.");
            setPerkItem(iextra, active, owned, inksack, 30, "No Weakness", (byte) 10, new ItemStack(Material.FERMENTED_SPIDER_EYE), "§7Keine Schwäche mehr.");
        } else if (m == Material.COMMAND) {
            iextra = ms.getServer().createInventory(null, 45, "§0§lMuxExtras§0 | Befehle");
            iextra.setItem(4, ms.renameItemStack(new ItemStack(Material.COMMAND), "§6§lBefehle", "§7Die zusätzlichen Befehle helfen", "§7dir sehr und bieten Funktionen", "§7an, die sonst keiner hat."));
            final Set<String> list = getExtraUser(uuid).getExtraCMDs();
            setCMDItem(iextra, list, inksack, 19, "anvil", new ItemStack(Material.ANVIL), "§7Amboss überall öffnen.");
            setCMDItem(iextra, list, inksack, 20, "enchant", new ItemStack(Material.ENCHANTMENT_TABLE), "§7Zaubertisch überall öffnen.");
            setCMDItem(iextra, list, inksack, 21, "cook", new ItemStack(Material.COOKED_BEEF), "§7Essen sofort kochen.", "§7(Sofortiger Ofen)");
            setCMDItem(iextra, list, inksack, 22, "bodysee", new ItemStack(Material.CHAINMAIL_CHESTPLATE), "§7Rüstung betrachten.");
            setCMDItem(iextra, list, inksack, 23, "goldswitch", new ItemStack(Material.GOLD_INGOT), "§7Alle 9 Goldnuggets in", "§7Goldbarren umtauschen.");
            setCMDItem(iextra, list, inksack, 24, "smelt", new ItemStack(Material.FURNACE), "§7Items sofort schmelzen.", "§7(Sofortiger Ofen)");
            setCMDItem(iextra, list, inksack, 25, "fill", new ItemStack(Material.POTION), "§7Glasflaschen sofort füllen.");
            setCMDItem(iextra, list, inksack, 28, "bottle", new ItemStack(Material.GLASS_BOTTLE), "§7Alle Glasblöcke in", "§7Glasflaschen umtauschen.");
            setCMDItem(iextra, list, inksack, 29, "xray", new ItemStack(Material.REDSTONE_ORE), "§7Alle Erze unter dir", "§7sofort sehen.");
            setCMDItem(iextra, list, inksack, 30, "near", new ItemStack(Material.COMPASS), "§7Alle Spieler in deiner", "§7Nähe anzeigen.");
            setCMDItem(iextra, list, inksack, 31, "bottlexp", new ItemStack(Material.EXP_BOTTLE), "§7Konvertiere deine Level", "§7zu einer Flasche.");
        } else if (m == Material.SKULL_ITEM) {
            iextra = ms.getServer().createInventory(null, 45, "§0§lMuxExtras§0 | Emojis");
            iextra.setItem(4, ms.setLore(ms.getEmojis().GRIN4("§6§lEmojis"), "§7Drücke all deine Gefühle mit Emojis", "§7aus. Diese sind deshalb besonders", "§7weil sie die ganze Zeit animiert sind."));
            final int active = ms.getEmojis().getActiveEmoji(uuid);
            final Set<Short> owned = getExtraUser(uuid).getEmojis();
            final MuxEmojis emojis = ms.getEmojis();
            setEmojiItem(iextra, active, owned, inksack, 19, "Wütend", emojis.RAGE7(""), (short) 1);
            setEmojiItem(iextra, active, owned, inksack, 20, "Weinend", emojis.cry(""), (short) 2);
            setEmojiItem(iextra, active, owned, inksack, 21, "Liebend", emojis.LOVE7(""), (short) 3);
            setEmojiItem(iextra, active, owned, inksack, 22, "Goofy", emojis.GOOFY4(""), (short) 4);
            setEmojiItem(iextra, active, owned, inksack, 23, "DealWithIt", emojis.TOOCOOL9(""), (short) 5);
            setEmojiItem(iextra, active, owned, inksack, 24, "RIP", emojis.RIP7(""), (short) 6);
            setEmojiItem(iextra, active, owned, inksack, 25, "Grinsend", emojis.GRIN4(""), (short) 7);
            setEmojiItem(iextra, active, owned, inksack, 28, "Müde", emojis.SLEEPY9(""), (short) 8);
            setEmojiItem(iextra, active, owned, inksack, 29, "Frech", emojis.CHEEKY8(""), (short) 9);
            setEmojiItem(iextra, active, owned, inksack, 30, "Traurig", emojis.FROWN8(""), (short) 10);
            setEmojiItem(iextra, active, owned, inksack, 31, "Fröhlich", emojis.SMILE3(""), (short) 11);
            setEmojiItem(iextra, active, owned, inksack, 32, "Spicy", emojis.SPICY9(""), (short) 12);
            setEmojiItem(iextra, active, owned, inksack, 33, "Kuss", emojis.KISS2(""), (short) 13);
            setEmojiItem(iextra, active, owned, inksack, 34, "Pepe", emojis.PEPE1(""), (short) 14);
        } else if (m == Material.BONE) {
            iextra = ms.getServer().createInventory(null, 45, "§0§lMuxExtras§0 | Pets");
            iextra.setItem(4, ms.renameItemStack(new ItemStack(Material.BONE), "§6§lPets", "§7Dein persönlicher Begleiter folgt", "§7dir, wo immer du auch hingehst,", "§7und gehorcht dir."));
            final EntityType active = getExtraUser(uuid).getPets().getActive();
            final Set<EntityType> owned = getExtraUser(uuid).getPets().getOwned();
            setPetItem(iextra, active, owned, inksack, 19, "Schwein", EntityType.PIG);
            setPetItem(iextra, active, owned, inksack, 20, "Schaf", EntityType.SHEEP);
            setPetItem(iextra, active, owned, inksack, 21, "Kuh", EntityType.COW);
            setPetItem(iextra, active, owned, inksack, 22, "Huhn", EntityType.CHICKEN);
            setPetItem(iextra, active, owned, inksack, 23, "Hund", EntityType.WOLF);
            setPetItem(iextra, active, owned, inksack, 24, "Pilzkuh", EntityType.MUSHROOM_COW);
            setPetItem(iextra, active, owned, inksack, 25, "Katze", EntityType.OCELOT);
            setPetItem(iextra, active, owned, inksack, 28, "Elf", EntityType.VILLAGER);
            setPetItem(iextra, active, owned, inksack, 29, "Hase", EntityType.RABBIT);
            setPetItem(iextra, active, owned, inksack, 30, "Pigman", EntityType.PIG_ZOMBIE);
            setPetItem(iextra, active, owned, inksack, 31, "Mini Me", EntityType.ZOMBIE);
            setPetItem(iextra, active, owned, inksack, 32, "Fledermaus", EntityType.BAT);
            setPetItem(iextra, active, owned, inksack, 33, "Kuchen", EntityType.SILVERFISH);
            setPetItem(iextra, active, owned, inksack, 34, "Wither", EntityType.WITHER);
        } else if (m == Material.IRON_BARDING) {
            iextra = ms.getServer().createInventory(null, 45, "§0§lMuxExtras§0 | Mounts");
            iextra.setItem(4, ms.renameItemStack(new ItemStack(Material.IRON_BARDING), "§6§lMounts", "§7Strenge deine Beine nicht mehr an,", "§7indem du von einem Reittier", "§7schnell transportiert wirst."));
            final MuxMounts.MountStore mountStore = getExtraUser(uuid).getMounts();
            final int active = mountStore.getActive();
            final Set<Short> owned = getExtraUser(uuid).getMounts().getOwned();
            setMountItem(iextra, active, owned, inksack, p, 19, "Pferd des Schreckens", Material.BONE, (short) 1);
            setMountItem(iextra, active, owned, inksack, p, 20, "Eisgletscher Pferd", Material.SNOW_BALL, (short) 2);
            setMountItem(iextra, active, owned, inksack, p, 21, "Packesel", Material.HAY_BLOCK, (short) 3);
            setMountItem(iextra, active, owned, inksack, p, 22, "Giftgrüner Schleim", Material.SLIME_BALL, (short) 4);
            setMountItem(iextra, active, owned, inksack, p, 23, "Minecart", Material.MINECART, (short) 5);
            setMountItem(iextra, active, owned, inksack, p, 24, "Kletternde Spinne", Material.WEB, (short) 6);
            setMountItem(iextra, active, owned, inksack, p, 25, "Untotes Ritterpferd", Material.ROTTEN_FLESH, (short) 7);
            setMountItem(iextra, active, owned, inksack, p, 28, "Fliegendes Rentier", Material.GOLDEN_CARROT, (short) 8);
            setMountItem(iextra, active, owned, inksack, p, 29, "Röpsendes Schwein", Material.PORK, (short) 10);
            setMountItem(iextra, active, owned, inksack, p, 30, "Feuerschleim", Material.BLAZE_POWDER, (short) 11);
            setMountItem(iextra, active, owned, inksack, p, 31, "Liebendes Schaf", Material.RED_ROSE, (short) 12);
            setMountItem(iextra, active, owned, inksack, p, 32, "Titan", Material.IRON_INGOT, (short) 13);
            setMountItem(iextra, active, owned, inksack, p, 33, "Chaotischer Guardian", Material.PRISMARINE_SHARD, (short) 15);
            setMountItem(iextra, active, owned, inksack, p, 34, "Milkende Pilzkuh", Material.HUGE_MUSHROOM_2, (short) 16);
            setMountItem(iextra, active, owned, inksack, p, 39, "Bike", Material.COAL, (short) 9);
            setMountItem(iextra, active, owned, inksack, p, 40, "Fliegende Schlange", Material.MAGMA_CREAM, (short) 14);
            setMountItem(iextra, active, owned, inksack, p, 41, "Liga Mount", Material.IRON_BARDING, (short) 17);
        } else {
            return;
        }
        if (ms.in1vs1(p) == false && ms.inDuel(p) == false && ms.inWar(p) == false)
            iextra.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + (iname.equals("Vorteile") ? " (Vorteile)" : "")));
        p.openInventory(iextra);
        ms.setActiveInv(p.getName(), InvType.EXTRAS);
        if (iname.isEmpty() == false && iname.equals("Vorteile") == false)
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
    }

    public void openExtras(final Player p, final boolean close) {
        final MuxExtraUser u = extras.get(p.getUniqueId());
        if (u == null) {
            ms.showItemBar(p, "§cDeine Daten konnten nicht geladen werden.");
            return;
        }
        final Inventory extrainv = ms.getServer().createInventory(null, 45, "§0§lMuxExtras§0 | /extras");
        extrainv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        extrainv.setItem(4, ms.renameItemStack(new ItemStack(Material.GOLD_BARDING), "§6§lMuxExtras", "§7Diese lassen sich jederzeit und", "§7egal wo du bist, nutzen."));
        extrainv.setItem(19, ms.renameItemStack(new ItemStack(Material.IRON_BARDING), "§6§lMounts", "§7Strenge deine Beine nicht mehr", "§7an, indem du von einem Reittier", "§7schnell transportiert wirst.", "",
                "§7Sammlung: §6" + (u.getMounts().getOwned().size() + (p.hasPermission("muxsystem.ultra") ? 1 : 0) + (ms.getMuxUser(p.getName()).getTrophies() > 999 ? 1 : 0)) + "§7/§6" + getMounts(), "", "§6Klicke§7, um die Kategorie zu öffnen."));
        extrainv.setItem(22, ms.renameItemStack(new ItemStack(Material.BLAZE_POWDER), "§6§lPerks", "§7Die 'Perks' geben dir permanente", "§7Effekte, sodass du beispielsweise", "§7höher springst.", "", "§7Sammlung: §6" + u.getPerks().getOwned().size() + "§7/§6" + getPerks(), "", "§6Klicke§7, um die Kategorie zu öffnen."));
        extrainv.setItem(25, ms.renameItemStack(new ItemStack(Material.BONE), "§6§lPets", "§7Dein persönlicher Begleiter folgt", "§7dir, wo immer du auch hingehst,", "§7und gehorcht dir.", "", "§7Sammlung: §6" + u.getPets().getOwned().size() + "§7/§6" + (getPets().size() + 1), "", "§6Klicke§7, um die Kategorie zu öffnen."));
        extrainv.setItem(38, ms.renameItemStack(new ItemStack(Material.COMMAND), "§6§lBefehle", "§7Die zusätzlichen Befehle helfen", "§7dir sehr und bieten Funktionen", "§7an, die sonst keiner hat.", "", "§7Sammlung: §6" + u.getExtraCMDs().size() + "§7/§6" + getExtraCommands().size(), "", "§6Klicke§7, um die Kategorie zu öffnen."));
        extrainv.setItem(42, ms.setLore(ms.getEmojis().GRIN4("§6§lEmojis"), "§7Drücke all deine Gefühle mit Emojis", "§7aus. Diese sind deshalb besonders", "§7weil sie die ganze Zeit animiert sind.", "", "§7Sammlung: §6" + u.getEmojis().size() + "§7/§6" + getEmojis(), "", "§6Klicke§7, um die Kategorie zu öffnen."));
        if (close) p.closeInventory();
        p.openInventory(extrainv);
        ms.setActiveInv(p.getName(), InvType.EXTRAS);
    }

    private boolean handleConsoleCommand(final CommandSender sender, final String label, final String[] args) {
        if (args.length < 3) return true;
        final OfflinePlayer p = ms.getPlayer(args[1]);
        if (p == null) return true;
        final UUID uuid = p.getUniqueId();
        final MuxExtraUser extrauser = p.isOnline() ? getExtraUser(uuid) : ms.getDB().loadExtras(uuid);
        if (extrauser == null) {
            sender.sendMessage("§cDer Spieler besitzt keine Extras!");
            return true;
        }
        if (args[0].equalsIgnoreCase("add")) {
            if (label.equalsIgnoreCase("extracmd")) {
                final String cmd = args[2].toLowerCase();
                if (getExtraCommands().contains(cmd) == false) {
                    sender.sendMessage("§cBitte gebe einen gültigen Extra Befehl an!");
                    return true;
                }
                final Set<String> list = extrauser.getExtraCMDs();
                if (list.contains(cmd)) {
                    sender.sendMessage("§cDer Spieler besitzt den Extra Befehl bereits!");
                    return true;
                }
                list.add(cmd);
                extrauser.setExtraCMDs(list);
                if (p.isOnline()) {
                    final Player pl = ms.getServer().getPlayer(p.getName());
                    pl.sendMessage("  §aDir wurde das Extra §6" + cmd + " §ahinzugefügt.");
                }
                saveExtraUser(extrauser);
                sender.sendMessage("§6§lMuxExtras>§a Extra Befehl §6" + cmd + " §awurde hinzugefügt.");
                return true;
            } else if (label.equalsIgnoreCase("pet") || label.equalsIgnoreCase("pets")) {
                EntityType et;
                try {
                    et = EntityType.valueOf(args[2].toUpperCase());
                } catch (final IllegalArgumentException e) {
                    sender.sendMessage("§cBitte gebe eine gültige Entitytyp ein!");
                    return true;
                }
                if (getPetPrice(et) == -1) {
                    sender.sendMessage("§cBitte gebe einen gültige Entitytyp ein!");
                    return true;
                }
                final Set<EntityType> owned = extrauser.getPets().getOwned();
                if (owned.contains(et)) {
                    sender.sendMessage("§cDer Spieler besitzt den Pet bereits!");
                    return true;
                }
                owned.add(et);
                extrauser.setPets(extrauser.getPets());
                final String petname = ms.getPets().getPetName(et);
                if (p.isOnline()) {
                    final Player pl = ms.getServer().getPlayer(p.getName());
                    pl.sendMessage("  §aDir wurde das Pet §6" + petname + " §ahinzugefügt.");
                }
                saveExtraUser(extrauser);
                sender.sendMessage("§6§lMuxExtras>§a Pet §6" + petname + "§a wurde hinzugefügt.");
                return true;
            } else if (label.equalsIgnoreCase("mount") || label.equalsIgnoreCase("mounts")) {
                short num;
                try {
                    num = Short.parseShort(args[2]);
                } catch (final NumberFormatException e) {
                    sender.sendMessage("§cBitte gebe einen gültige Mountnummer ein!");
                    return true;
                }
                if (getMountPrice(num) == -1) {
                    sender.sendMessage("§cBitte gebe einen gültige Mountnummer ein!");
                    return true;
                } else if (getMountPrice(num) == -2) {
                    sender.sendMessage("§cDieses Mount ist nur mit §c§lULTRA §cverfügbar.");
                    return true;
                } else if (getMountPrice(num) == -3) {
                    sender.sendMessage("§cDieses Mount ist nur mit §e§lGOLD §cverfügbar.");
                    return true;
                } else if (getMountPrice(num) == -4) {
                    sender.sendMessage("§cDieses Mount ist nur ab der §f§lSilber Liga§c verfügbar.");
                    return true;
                }
                final MuxMounts.MountStore mountStore = extrauser.getMounts();
                if (mountStore.getOwned().contains(num)) {
                    sender.sendMessage("§cDer Spieler besitzt den Mount bereits!");
                    return true;
                }
                mountStore.addOwned(num);
                extrauser.setMounts(mountStore);
                final String mountname = ms.getMounts().getMountName(num);
                if (p.isOnline()) {
                    final Player pl = ms.getServer().getPlayer(p.getName());
                    pl.sendMessage("  §aDir wurde das Mount §6" + mountname + " §ahinzugefügt.");
                }
                saveExtraUser(extrauser);
                sender.sendMessage("§6§lMuxExtras>§a Mount §6" + mountname + "§a wurde hinzugefügt.");
                return true;
            } else if (label.equalsIgnoreCase("emoji") || label.equalsIgnoreCase("emojis")) {
                short num;
                try {
                    num = Short.parseShort(args[2]);
                } catch (final NumberFormatException e) {
                    sender.sendMessage("§cBitte gebe einen gültige Emojinummer ein!");
                    return true;
                }
                if (num < 1 || num > getEmojis()) {
                    sender.sendMessage("§cBitte gebe einen gültige Emojinummer ein!");
                    return true;
                }
                final Set<Short> owned = extrauser.getEmojis();
                if (owned.contains(num)) {
                    sender.sendMessage("§cDer Spieler besitzt den Emoji bereits!");
                    return true;
                }
                owned.add(num);
                extrauser.setEmojis(owned);
                final String emojiname = ms.getEmojis().getEmojiName(num);
                if (p.isOnline()) {
                    final Player pl = ms.getServer().getPlayer(p.getName());
                    pl.sendMessage("  §aDir wurde das Emoji §6" + emojiname + " §ahinzugefügt.");
                }
                saveExtraUser(extrauser);
                sender.sendMessage("§6§lMuxExtras>§a Emoji §6" + emojiname + "§a wurde hinzugefügt.");
                return true;
            } else if (label.equalsIgnoreCase("extraender")) {
                if (extrauser.getEnderChest() == (byte) 1) {
                    sender.sendMessage("§cDer Spieler besitzt die erweiterte Enderkiste bereits!");
                    return true;
                }
                setExtraEnderChest(uuid, extrauser, true);
                if (p.isOnline()) {
                    final Player pl = ms.getServer().getPlayer(p.getName());
                    pl.sendMessage("  §aDu hast nun eine erweiterte Enderkiste!");
                }
                saveExtraUser(extrauser);
                sender.sendMessage("§6§lMuxExtras>§a Extra Enderkiste wurde §6" + p.getName() + " §ahinzugefügt.");
                ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateExtraEnderChest(extrauser.getUUID(), true, null));
                return true;
            }
            final byte nummer = getPerkNumber(sender, args[2]);
            if (nummer == 0) return true;
            PerkStore ps = extrauser.getPerks();
            Set<Byte> owned, active;
            if (ps == null) {
                owned = new HashSet<>();
                active = new HashSet<>();
            } else {
                owned = ps.getOwned();
                active = ps.getActive();
                if (owned.contains(nummer)) {
                    sender.sendMessage("§cDer Spieler besitzt den Perk bereits!");
                    return true;
                }
            }
            owned.add(nummer);
            active.add(nummer);
            ps = ms.getPerks().new PerkStore(active, owned);
            extrauser.setPerks(ps);
            if (p.isOnline()) {
                final Player pl = ms.getServer().getPlayer(p.getName());
                final boolean console = sender.getName().equals("CONSOLE");
                ms.getPerks().activatePerk(pl, nummer);
                pl.sendMessage((console ? " " : "§6§lMuxExtras>") + "§a " + String.format(ms.getLang("donate.perks1"), "§l" + ms.getPerks().getPerkName(nummer)));
                pl.sendMessage((console ? " " : "§6§lMuxExtras>") + "§a " + ms.getLang("donate.perks2"));
            }
            saveExtraUser(extrauser);
            sender.sendMessage("§6MuxExtras> §aPerk Nummer §6" + nummer + " §awurde hinzugefügt!");
            return true;
        } else if (args[0].equalsIgnoreCase("rem") || args[0].equalsIgnoreCase("remove")) {
            if (label.equalsIgnoreCase("extracmd")) {
                final String cmd = args[2].toLowerCase();
                if (getExtraCommands().contains(cmd) == false) {
                    sender.sendMessage("§cBitte gebe einen gültigen Extra Befehl an!");
                    return true;
                }
                final Set<String> list = extrauser.getExtraCMDs();
                if (list.contains(cmd) == false) {
                    sender.sendMessage("§cDer Spieler besitzt den Extra Befehl nicht!");
                    return true;
                }
                list.remove(cmd);
                extrauser.setExtraCMDs(list);
                saveExtraUser(extrauser);
                sender.sendMessage("§6§lMuxExtras>§a Extra Befehl §6" + cmd + " §awurde von §6" + p.getName() + " §aentfernt.");
                return true;
            } else if (label.equalsIgnoreCase("extraender")) {
                if (extrauser.getEnderChest() == (byte) 0) {
                    sender.sendMessage("§cDer Spieler besitzt die erweiterte Enderkiste nicht!");
                    return true;
                }
                ms.getHistory().addHistory(extrauser.getUUID(), p.getUniqueId(), "TEAMACTION", "EXTRAENDER REMOVE", "ENDERCHEST");
                ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateExtraEnderChest(extrauser.getUUID(), false, p.getUniqueId()));
                setExtraEnderChest(uuid, extrauser, false);
                saveExtraUser(extrauser);
                sender.sendMessage("§6§lMuxExtras>§a Extra Enderkiste wurde von §6" + p.getName() + " §aentfernt.");
                return true;
            } else if (label.equalsIgnoreCase("mount") || label.equalsIgnoreCase("mounts")) {
                short num;
                try {
                    num = Short.parseShort(args[2]);
                } catch (final NumberFormatException e) {
                    sender.sendMessage("§cBitte gebe einen gültige Mountnummer ein!");
                    return true;
                }
                if (getMountPrice(num) == -1) {
                    sender.sendMessage("§cBitte gebe einen gültige Mountnummer ein!");
                    return true;
                } else if (getMountPrice(num) == -2) {
                    sender.sendMessage("§cDieses Mount ist nur mit §c§lULTRA §cverfügbar.");
                    return true;
                } else if (getMountPrice(num) == -3) {
                    sender.sendMessage("§cDieses Mount ist nur mit §e§lGOLD §cverfügbar.");
                    return true;
                } else if (getMountPrice(num) == -4) {
                    sender.sendMessage("§cDieses Mount ist nur ab der §f§lSilber Liga§c verfügbar.");
                    return true;
                }
                final MuxMounts.MountStore mountStore = extrauser.getMounts();
                final Set<Short> owned = mountStore.getOwned();
                if (owned.remove(num) == false) {
                    sender.sendMessage("§cDer Spieler besitzt den Mount nicht!");
                    return true;
                }
                if (mountStore.getActive() == num && p.isOnline()) {
                    ms.getMounts().deactivateMount(p.getPlayer());
                }
                extrauser.setMounts(ms.getMounts().new MountStore(owned, (short) 0));
                final String mountname = ms.getMounts().getMountName(num);
                saveExtraUser(extrauser);
                sender.sendMessage("§6§lMuxExtras>§a Mount §6" + mountname + "§a wurde von §6" + p.getName() + " §aentfernt.");
                return true;
            }
            final byte nummer = getPerkNumber(sender, args[2]);
            if (nummer == 0) return true;
            final String pname = p.getName();
            final PerkStore ps = extrauser.getPerks();
            if (ps == null) {
                sender.sendMessage("§cDer Spieler besitzt keine Perks!");
                return true;
            }
            final Set<Byte> owned = ps.getOwned();
            if (owned.remove(nummer) == false) {
                sender.sendMessage("§cDer Spieler besitzt den Perk nicht!");
                return true;
            }
            final Set<Byte> active = ps.getActive();
            if (active.remove(nummer) && p.isOnline()) {
                ms.getPerks().removeActivePerk(ms.getServer().getPlayer(pname), nummer);
            }
            extrauser.setPerks(ms.getPerks().new PerkStore(active, owned));
            saveExtraUser(extrauser);
            sender.sendMessage("§6§lMuxExtras>§a Perk Nummer §6" + nummer + " §awurde von §6" + pname + " §aentfernt.");
            return true;
        }
        sender.sendMessage(ms.usage("/" + label + " [add | remove] [spieler] [" + label + "]"));
        return true;
    }

    public boolean handleExtras(final CommandSender sender, final String label, final String[] args) {
        if (sender instanceof Player == false) {
            new BukkitRunnable() {
                @Override public void run() {
                    handleConsoleCommand(sender, label, args);
                }
            }.runTaskLater(ms, 5L);
            return true;
        }
        final Player p = (Player) sender;
        if (args.length == 0 || ms.isTrusted(sender.getName()) == false) {
            if (ms.getActiveInv(p.getName()) != InvType.WARPS) p.closeInventory();
            switch (label) {
                case "pet":
                case "pets":
                    handleInventory(p, new ItemStack(Material.BONE), "");
                    break;
                case "perk":
                case "perks":
                    handleInventory(p, new ItemStack(Material.BLAZE_POWDER), "");
                    break;
                case "mount":
                case "mounts":
                    handleInventory(p, new ItemStack(Material.IRON_BARDING), "");
                    break;
                case "emoji":
                case "emojis":
                    handleInventory(p, new ItemStack(Material.SKULL_ITEM), "");
                    break;
                case "extracmd":
                case "extrabefehle":
                    handleInventory(p, new ItemStack(Material.COMMAND), "");
                    break;
                default:
                    openExtras(p, true);
                    break;
            }
        } else {
            final OfflinePlayer op = ms.getPlayer(args[0]);
            if (op == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            final MuxExtraUser extrauser = op.isOnline() ? getExtraUser(op.getUniqueId()) : ms.getDB().loadExtras(op.getUniqueId());
            if (extrauser == null) {
                ms.showItemBar(p, "§cDer Spieler besitzt keine Extras.");
                return true;
            }
            Inventory inv;
            final boolean edit = ms.isFullTrusted(p.getName());
            if (label.equalsIgnoreCase("pet") || label.equalsIgnoreCase("pets")) {
                inv = ms.getServer().createInventory(null, 45, "§0§lMuxPets§0 |§0 " + op.getName());
                inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BONE), "§6§lPets", "§7Dein persönlicher Begleiter folgt", "§7dir, wo immer du auch hingehst,", "§7und gehorcht dir."));
                final Set<EntityType> owned = extrauser.getPets().getOwned();
                setAdminPetItem(inv, owned, 19, "Schwein", EntityType.PIG, edit);
                setAdminPetItem(inv, owned, 20, "Schaf", EntityType.SHEEP, edit);
                setAdminPetItem(inv, owned, 21, "Kuh", EntityType.COW, edit);
                setAdminPetItem(inv, owned, 22, "Huhn", EntityType.CHICKEN, edit);
                setAdminPetItem(inv, owned, 23, "Hund", EntityType.WOLF, edit);
                setAdminPetItem(inv, owned, 24, "Pilzkuh", EntityType.MUSHROOM_COW, edit);
                setAdminPetItem(inv, owned, 25, "Katze", EntityType.OCELOT, edit);
                setAdminPetItem(inv, owned, 28, "Elf", EntityType.VILLAGER, edit);
                setAdminPetItem(inv, owned, 29, "Hase", EntityType.RABBIT, edit);
                setAdminPetItem(inv, owned, 30, "Pigman", EntityType.PIG_ZOMBIE, edit);
                setAdminPetItem(inv, owned, 31, "Mini Me", EntityType.ZOMBIE, edit);
                setAdminPetItem(inv, owned, 32, "Fledermaus", EntityType.BAT, edit);
                setAdminPetItem(inv, owned, 33, "Kuchen", EntityType.SILVERFISH, edit);
                setAdminPetItem(inv, owned, 34, "Wither", EntityType.WITHER, edit);
            } else if (label.equalsIgnoreCase("mount") || label.equalsIgnoreCase("mounts")) {
                inv = ms.getServer().createInventory(null, 45, "§0§lMuxMounts§0 |§0 " + op.getName());
                final Set<Short> owned = extrauser.getMounts().getOwned();
                inv.setItem(4, ms.renameItemStack(new ItemStack(Material.IRON_BARDING), "§6§lMounts", "§7Strenge deine Beine nicht mehr an,", "§7indem du von einem Reittier", "§7schnell transportiert wirst."));
                setAdminMountItem(inv, owned, 19, "Pferd des Schreckens", Material.BONE, (short) 1, edit);
                setAdminMountItem(inv, owned, 20, "Eisgletscher Pferd", Material.SNOW_BALL, (short) 2, edit);
                setAdminMountItem(inv, owned, 21, "Packesel", Material.HAY_BLOCK, (short) 3, edit);
                setAdminMountItem(inv, owned, 22, "Giftgrüner Schleim", Material.SLIME_BALL, (short) 4, edit);
                setAdminMountItem(inv, owned, 23, "Minecart", Material.MINECART, (short) 5, edit);
                setAdminMountItem(inv, owned, 24, "Kletternde Spinne", Material.WEB, (short) 6, edit);
                setAdminMountItem(inv, owned, 25, "Untotes Ritterpferd", Material.ROTTEN_FLESH, (short) 7, edit);
                setAdminMountItem(inv, owned, 28, "Fliegendes Rentier", Material.GOLDEN_CARROT, (short) 8, edit);
                setAdminMountItem(inv, owned, 29, "Röpsendes Schwein", Material.PORK, (short) 10, edit);
                setAdminMountItem(inv, owned, 30, "Feuerschleim", Material.BLAZE_POWDER, (short) 11, edit);
                setAdminMountItem(inv, owned, 31, "Liebendes Schaf", Material.RED_ROSE, (short) 12, edit);
                setAdminMountItem(inv, owned, 32, "Titan", Material.IRON_INGOT, (short) 13, edit);
                setAdminMountItem(inv, owned, 33, "Chaotischer Guardian", Material.PRISMARINE_SHARD, (short) 15, edit);
                setAdminMountItem(inv, owned, 34, "Milkende Pilzkuh", Material.HUGE_MUSHROOM_2, (short) 16, edit);
            } else if (label.equalsIgnoreCase("perk") || label.equalsIgnoreCase("perks")) {
                final PerkStore ps = extrauser.getPerks();
                final Set<Byte> owned = ps != null ? ps.getOwned() : new HashSet<>();
                inv = ms.getServer().createInventory(null, 45, "§0§lMuxPerks§0 |§0 " + op.getName());
                inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BLAZE_POWDER), ms.getLang("extras.perks"), ms.getLang("extras.perkslore1"), ms.getLang("extras.perkslore2"), ms.getLang("extras.perkslore3")));
                setAdminPerkItem(inv, owned, 19, "No Hunger", (byte) 1, new ItemStack(Material.APPLE), ms.getLang("extras.nohunger1"), edit);
                setAdminPerkItem(inv, owned, 20, "High Jump", (byte) 2, new ItemStack(Material.HOPPER), ms.getLang("extras.highjump1"), edit);
                setAdminPerkItem(inv, owned, 21, "Anti Fire", (byte) 3, new ItemStack(Material.MAGMA_CREAM), ms.getLang("extras.antifire1"), edit);
                setAdminPerkItem(inv, owned, 22, "Runner", (byte) 4, new ItemStack(Material.DIAMOND_BOOTS), ms.getLang("extras.runner1"), edit);
                setAdminPerkItem(inv, owned, 23, ms.getMultiplier(1 + ms.getEvents().getExpBoost()) + " EXP", (byte) 5, new ItemStack(Material.EXP_BOTTLE), ms.getLang("extras.doubleexp1"), edit);
                setAdminPerkItem(inv, owned, 24, "Night Vision", (byte) 6, new ItemStack(Material.EYE_OF_ENDER), ms.getLang("extras.nightvision1"), edit);
                setAdminPerkItem(inv, owned, 25, "Power", (byte) 7, new ItemStack(Material.FIREBALL), ms.getLang("extras.power1"), edit);
                setAdminPerkItem(inv, owned, 28, "Anti Poison", (byte) 8, new ItemStack(Material.INK_SACK, 1, (byte) 2), ms.getLang("extras.antipoison1"), edit);
                setAdminPerkItem(inv, owned, 29, "Fast Break", (byte) 9, new ItemStack(Material.DIAMOND_PICKAXE), "§7Doppelt so schnell abbauen.", edit);
                setAdminPerkItem(inv, owned, 30, "No Weakness", (byte) 10, new ItemStack(Material.FERMENTED_SPIDER_EYE), "§7Keine Schwäche mehr.", edit);
            } else if (label.equalsIgnoreCase("emoji") || label.equalsIgnoreCase("emojis")) {
                inv = ms.getServer().createInventory(null, 45, "§0§lMuxEmojis§0 |§0 " + op.getName());
                final Set<Short> owned = extrauser.getEmojis();
                inv.setItem(4, ms.setLore(ms.getEmojis().GRIN4("§6§lEmojis"), "§7Drücke all deine Gefühle mit Emojis", "§7aus. Diese sind deshalb besonders", "§7weil sie die ganze Zeit animiert sind."));
                final MuxEmojis emojis = ms.getEmojis();
                setAdminEmojiItem(inv, owned, 19, "Wütend", emojis.RAGE7(""), (short) 1, edit);
                setAdminEmojiItem(inv, owned, 20, "Weinend", emojis.cry(""), (short) 2, edit);
                setAdminEmojiItem(inv, owned, 21, "Liebend", emojis.LOVE7(""), (short) 3, edit);
                setAdminEmojiItem(inv, owned, 22, "Goofy", emojis.GOOFY4(""), (short) 4, edit);
                setAdminEmojiItem(inv, owned, 23, "DealWithIt", emojis.TOOCOOL9(""), (short) 5, edit);
                setAdminEmojiItem(inv, owned, 24, "RIP", emojis.RIP7(""), (short) 6, edit);
                setAdminEmojiItem(inv, owned, 25, "Grinsend", emojis.GRIN4(""), (short) 7, edit);
                setAdminEmojiItem(inv, owned, 28, "Müde", emojis.SLEEPY9(""), (short) 8, edit);
                setAdminEmojiItem(inv, owned, 29, "Frech", emojis.CHEEKY8(""), (short) 9, edit);
                setAdminEmojiItem(inv, owned, 30, "Traurig", emojis.FROWN8(""), (short) 10, edit);
                setAdminEmojiItem(inv, owned, 31, "Fröhlich", emojis.SMILE3(""), (short) 11, edit);
                setAdminEmojiItem(inv, owned, 32, "Spicy", emojis.SPICY9(""), (short) 12, edit);
                setAdminEmojiItem(inv, owned, 33, "Kuss", emojis.KISS2(""), (short) 13, edit);
                setAdminEmojiItem(inv, owned, 34, "Pepe", emojis.PEPE1(""), (short) 14, edit);
            } else if (label.equalsIgnoreCase("extracmd")) {
                inv = ms.getServer().createInventory(null, 45, "§0§lMuxBefehle§0 |§0 " + op.getName());
                final Set<String> owned = extrauser.getExtraCMDs();
                inv.setItem(4, ms.renameItemStack(new ItemStack(Material.COMMAND), "§6§lBefehle", "§7Die zusätzlichen Befehle helfen", "§7dir sehr und bieten Funktionen", "§7an, die sonst keiner hat."));
                setAdminCMDItem(inv, owned, 19, "anvil", new ItemStack(Material.ANVIL), edit, "§7Amboss überall öffnen.");
                setAdminCMDItem(inv, owned, 20, "enchant", new ItemStack(Material.ENCHANTMENT_TABLE), edit, "§7Zaubertisch überall öffnen.");
                setAdminCMDItem(inv, owned, 21, "cook", new ItemStack(Material.COOKED_BEEF), edit, "§7Essen sofort kochen.", "§7(Sofortiger Ofen)");
                setAdminCMDItem(inv, owned, 22, "bodysee", new ItemStack(Material.CHAINMAIL_CHESTPLATE), edit, "§7Rüstung betrachten.");
                setAdminCMDItem(inv, owned, 23, "goldswitch", new ItemStack(Material.GOLD_INGOT), edit, "§7Alle 9 Goldnuggets in", "§7Goldbarren umtauschen.");
                setAdminCMDItem(inv, owned, 24, "smelt", new ItemStack(Material.FURNACE), edit, "§7Items sofort schmelzen.", "§7(Sofortiger Ofen)");
                setAdminCMDItem(inv, owned, 25, "fill", new ItemStack(Material.POTION), edit, "§7Glasflaschen sofort füllen.");
                setAdminCMDItem(inv, owned, 28, "bottle", new ItemStack(Material.GLASS_BOTTLE), edit, "§7Alle Glasblöcke in", "§7Glasflaschen umtauschen.");
                setAdminCMDItem(inv, owned, 29, "xray", new ItemStack(Material.REDSTONE_ORE), edit, "§7Alle Erze unter dir", "§7sofort sehen.");
                setAdminCMDItem(inv, owned, 30, "near", new ItemStack(Material.COMPASS), edit, "§7Alle Spieler in deiner", "§7Nähe anzeigen.");
                setAdminCMDItem(inv, owned, 31, "bottlexp", new ItemStack(Material.EXP_BOTTLE), edit, "§7Konvertiere deine Level", "§7zu einer Flasche.");
            } else {
                inv = ms.getServer().createInventory(null, 45, "§0§lMuxExtras§0 |§0 " + op.getName());
                final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(op.getUniqueId());
                final boolean ultra = group != null && group.getPermissions().contains("muxsystem.ultra");
                final MuxUser user = op.isOnline() ? ms.getMuxUser(op.getName()) : ms.getDB().loadPlayer(op.getUniqueId());
                inv.setItem(4, ms.renameItemStack(new ItemStack(Material.GOLD_BARDING), "§6§lMuxExtras", "§7Diese lassen sich jederzeit und", "§7egal wo du bist, nutzen."));
                inv.setItem(19, ms.renameItemStack(new ItemStack(Material.IRON_BARDING), "§6§lMounts", "§7Strenge deine Beine nicht mehr", "§7an, indem du von einem Reittier", "§7schnell transportiert wirst.", "",
                        "§7Sammlung: §6" + (extrauser.getMounts().getOwned().size() + (ultra ? 1 : 0) + (user != null && user.getTrophies() > 999 ? 1 : 0)) + "§7/§6" + getMounts(), "", "§6Klicke§7, um die Kategorie zu öffnen."));
                inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BLAZE_POWDER), "§6§lPerks", "§7Die 'Perks' geben dir permanente", "§7Effekte, sodass du beispielsweise", "§7höher springst.", "", "§7Sammlung: §6" + extrauser.getPerks().getOwned().size() + "§7/§6" + getPerks(), "", "§6Klicke§7, um die Kategorie zu öffnen."));
                inv.setItem(25, ms.renameItemStack(new ItemStack(Material.BONE), "§6§lPets", "§7Dein persönlicher Begleiter folgt", "§7dir, wo immer du auch hingehst,", "§7und gehorcht dir.", "", "§7Sammlung: §6" + extrauser.getPets().getOwned().size() + "§7/§6" + (getPets().size() + 1), "", "§6Klicke§7, um die Kategorie zu öffnen."));
                inv.setItem(37, ms.renameItemStack(new ItemStack(Material.COMMAND), "§6§lBefehle", "§7Die zusätzlichen Befehle helfen", "§7dir sehr und bieten Funktionen", "§7an, die sonst keiner hat.", "", "§7Sammlung: §6" + extrauser.getExtraCMDs().size() + "§7/§6" + getExtraCommands().size(), "", "§6Klicke§7, um die Kategorie zu öffnen."));
                inv.setItem(40, ms.renameItemStack(new ItemStack(Material.ENDER_CHEST), "§6§lDoppelte Enderkiste", "", extrauser.getEnderChest() == (byte) 1 ? "§7Klicke, um die Enderkiste §czu entfernen§7." : "§7Klicke, um die Enderkiste §ahinzuzufügen§7."));
                inv.setItem(43, ms.setLore(ms.getEmojis().GRIN4("§6§lEmojis"), "§7Drücke all deine Gefühle mit Emojis", "§7aus. Diese sind deshalb besonders", "§7weil sie die ganze Zeit animiert sind.", "", "§7Sammlung: §6" + extrauser.getEmojis().size() + "§7/§6" + getEmojis(), "", "§6Klicke§7, um die Kategorie zu öffnen."));
            }
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + (inv.getTitle().startsWith("§0§lMuxExtras") ? " (Overview)" : "")));
            final InvType it = ms.getActiveInv(p.getName());
            if (it != InvType.EXTRAS && it != InvType.USERCONTROL) p.closeInventory();
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.EXTRAS);
        }
        return true;
    }

    public void addItem(final Player p, final String type) {
        this.addItem(p, type, 1);
    }

    public void addItem(final Player p, final String type, int amount) {
        if (amount < 1)
            amount = 1;
        else if (amount > 64)
            amount = 64;
        final PlayerInventory pi = p.getInventory();
        ItemStack is;
        String name;
        if (type.equalsIgnoreCase("sword")) {
            is = ms.renameItemStack(fS.clone(), "§7[§a§lMUX§7] §b§lFull-Enchant Schwert");
            name = "das Full Schwert";
        } else if (type.equals("bloodstick")) {
            is = ms.renameItemStack(new ItemStack(Material.STICK), "§c§lBlutbendiger Stock", "§7Haltbarkeit: §c100/100", "", "§7Sneake und schaue ein Mob an.");
            name = "der Blutbendiger Stock";
        } else if (type.equals("bow")) {
            is = ms.renameItemStack(fBow.clone(), "§7[§a§lMUX§7] §b§lFull-Enchant Bogen");
            name = "der Full Bogen";
        } else if (type.equals("superpickaxe")) {
            is = ms.renameItemStack(fP.clone(), "§7[§a§lMUX§7] §b§lSuper Spitzhacke");
            name = "die Super Spitzhacke";
        } else if (type.equals("potato")) {
            is = ms.renameItemStack(fK.clone(), "§7[§a§lMUX§7] §6§lRückstoß 10 Kartoffel");
            name = "die Kartoffel";
        } else if (type.equalsIgnoreCase("spawner")) {
            is = ms.renameItemStack(new ItemStack(Material.MOB_SPAWNER), "§a§lMuxSpawner");
            name = "der MuxSpawner";
        } else if (type.equalsIgnoreCase("axe")) {
            is = ms.renameItemStack(fA.clone(), "§7[§a§lMUX§7] §4§lLegendäre Axt");
            name = "die Legendäre Axt";
        } else if (type.equalsIgnoreCase("multipickaxe")) {
            is = ms.renameItemStack(fMP.clone(), "§7[§a§lMUX§7] §b§lMultiblock Spitzhacke", "§7Zerstöre alle Blöcke in deinem", "§7Umkreis mit einem Linksklick.", "", "§7Haltbarkeit: 5000 / 5000");
            name = "die Multiblock Spitzhacke";
        } else if (type.equalsIgnoreCase("multishovel")) {
            is = ms.renameItemStack(fMS.clone(), "§7[§a§lMUX§7] §b§lMultiblock Schaufel", "§7Zerstöre alle Blöcke in deinem", "§7Umkreis mit einem Linksklick.", "", "§7Haltbarkeit: 5000 / 5000");
            name = "die Multiblock Schaufel";
        } else if (type.equalsIgnoreCase("multiaxe")) {
            is = ms.renameItemStack(fMA.clone(), "§7[§a§lMUX§7] §b§lMultiblock Axt", "§7Zerstöre alle Blöcke in deinem", "§7Umkreis mit einem Linksklick.", "", "§7Haltbarkeit: 5000 / 5000");
            name = "die Multiblock Axt";
        } else if (type.equalsIgnoreCase("flybooster")) {
            is = ms.renameItemStack(ms.getFireworkCharge(Color.RED), "§c§lFly Booster", "§7Dauer: §a30 Minuten", "§0" + 1800, "§7Rechtsklick auf den Booster", "§7um diesen zu aktivieren.");
            name = "der Booster";
        } else if (type.equalsIgnoreCase("globalxpbooster")) {
            is = ms.renameItemStack(ms.getFireworkCharge(Color.PURPLE), "§d§lGlobaler XP Booster", "§7Dauer: §a15 Minuten", "§0" + 900, "§7Rechtsklick auf den Booster", "§7um diesen zu aktivieren.");
            name = "der Booster";
        } else if (type.equalsIgnoreCase("xpbooster")) {
            is = ms.renameItemStack(ms.getFireworkCharge(Color.LIME), "§a§lXP Booster", "§7Dauer: §a1 Stunde", "§0" + 3600, "§7Rechtsklick auf den Booster", "§7um diesen zu aktivieren.");
            name = "der Booster";
        } else if (type.equalsIgnoreCase("megaspawnerbooster")) {
            is = ms.renameItemStack(ms.getFireworkCharge(Color.ORANGE), "§6§lMega Spawner Booster", "§7Dauer: §a15 Tage", "§0" + 1296000, "§7Rechtsklick auf ein Spawner", "§7um den Booster zu aktivieren.");
            name = "der Booster";
        } else if (type.equalsIgnoreCase("spawnerbooster")) {
            is = ms.renameItemStack(ms.getFireworkCharge(Color.YELLOW), "§6§lSpawner Booster", "§7Dauer: §a2 Tage", "§0" + 172800, "§7Rechtsklick auf ein Spawner", "§7um den Booster zu aktivieren.");
            name = "der Booster";
        } else if (type.equalsIgnoreCase("netherwater")) {
            is = ms.renameItemStack(new ItemStack(Material.WATER_BUCKET), "§7[§a§lMUX§7] §9§lNetherwasser", "§7Im Nether platzierbar.");
            name = "das Nether Wasser";
        } else if (type.equalsIgnoreCase("armor")) {
            short count = ms.getEmptyInvSlots(p.getInventory());
            final CopyOnWriteArrayList<ItemStack> armor = new CopyOnWriteArrayList<>(Arrays.asList(
                    ms.renameItemStack(fH.clone(), "§7[§a§lMUX§7] §b§lFull-Enchant Rüstung"),
                    ms.renameItemStack(fC.clone(), "§7[§a§lMUX§7] §b§lFull-Enchant Rüstung"),
                    ms.renameItemStack(fL.clone(), "§7[§a§lMUX§7] §b§lFull-Enchant Rüstung"),
                    ms.renameItemStack(fB.clone(), "§7[§a§lMUX§7] §b§lFull-Enchant Rüstung")));
            for (final ItemStack i : armor) {
                if (count == 0) break;
                pi.addItem(i);
                armor.remove(i);
                count--;
            }
            if (armor.isEmpty() == false) {
                final World w = p.getWorld();
                final Location l = p.getLocation();
                for (final ItemStack i : armor) {
                    w.dropItemNaturally(l, i);
                }
                p.sendMessage("§aDein Inventar war voll und einige Rüstungsteile wurden auf den Boden gedroppt.");
            }
            return;
        } else {
            return;
        }
        is.setAmount(amount);
        if (pi.firstEmpty() == -1) {
            p.getWorld().dropItemNaturally(p.getLocation(), is);
            p.sendMessage("§aDein Inventar war voll und " + name + " wurde auf den Boden gedroppt.");
        } else {
            pi.addItem(is);
            p.updateInventory();
        }
    }

    public boolean isMultiTool(final ItemStack i) {
        return (i.getType() == Material.DIAMOND_PICKAXE || i.getType() == Material.DIAMOND_AXE || i.getType() == Material.DIAMOND_SPADE) && i.hasItemMeta() && i.getItemMeta().hasDisplayName() && i.getItemMeta().getDisplayName().contains("§b§lMultiblock");
    }

    private final List<String> extraCommands = Arrays.asList("anvil", "enchant", "cook", "bodysee", "goldswitch", "smelt", "fill", "bottle", "xray", "near", "bottlexp");

    public List<String> getExtraCommands() {
        return extraCommands;
    }

    public List<String> getShuffledExtraCommands() {
        final List<String> list = new ArrayList<>(extraCommands);
        Collections.shuffle(list);
        return list;
    }

    public int getMounts() {
        return 17;
    }

    public int getEmojis() {
        return 14;
    }

    public int getPerks() {
        return 10;
    }

    public List<EntityType> getPets() {
        final List<EntityType> list = Arrays.asList(EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.OCELOT, EntityType.CHICKEN,
                EntityType.MUSHROOM_COW, EntityType.WOLF, EntityType.VILLAGER, EntityType.RABBIT, EntityType.PIG_ZOMBIE, EntityType.ZOMBIE,
                EntityType.BAT, EntityType.SILVERFISH);
        Collections.shuffle(list);
        return list;
    }

    private void saveUserAndRehandleCommand(final Player player, final MuxExtraUser user, final String command, final String name, final boolean remove, final String extra) {
        user.setChanged(true);
        saveExtraUser(user);
        player.setMetadata("extradelay", new FixedMetadataValue(ms, true));
        ms.getHistory().addHistory(user.getUUID(), player.getUniqueId(), "TEAMACTION", command.toUpperCase() + (remove ? " REMOVE" : " ADD"), extra);
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1F, 1F);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("extradelay", ms);
                handleExtras(player, command, new String[]{name});
            }
        }.runTaskLater(ms, 2L);
    }

    private int getPetPrice(final EntityType pet) {
        switch (pet) {
            case PIG:
            case SHEEP:
            case VILLAGER:
            case RABBIT:
                return 1500;
            case COW:
                return 1000;
            case OCELOT:
                return 2200;
            case CHICKEN:
            case WOLF:
            case BAT:
            case SILVERFISH:
                return 2500;
            case MUSHROOM_COW:
            case PIG_ZOMBIE:
            case ZOMBIE:
                return 2000;
            case WITHER:
                return 5000;
            default:
                return -1;
        }
    }

    private int getMountPrice(final short n) {
        switch (n) {
            case 1: // SKELETON HORSE
                return 1500;
            case 2: // WHITE HORSE
                return 1000;
            case 3: // MULE
                return 500;
            case 4: // SLIME
                return 2000;
            case 5: // MINECART
                return 1500;
            case 6: // SPIDER
                return 2200;
            case 7: // UNDEAD HORSE
                return 1500;
            case 8: // REINDEER
                return 2500;
            case 9:
                return -2;
            case 10: // PIG
                return 2000;
            case 11: // MAGMA CUBE
                return 2000;
            case 12: // SHEEP
                return 5000;
            case 13: // IRON GOLEM
                return 2000;
            case 14: // FLYING SNAKE
                return -3;
            case 15: // GUARDIAN
                return 2000;
            case 16: // MUSHROOM COW
                return 1500;
            case 17: // LEAGUE MOUNT
                return -4;
            default:
                return -1;
        }
    }

    private byte getPerkNumber(final CommandSender sender, final String s) {
        byte nummer;
        try {
            nummer = Byte.parseByte(s);
            if (nummer < 1 || nummer > 10) {
                throw new NumberFormatException();
            }
        } catch (final NumberFormatException ex) {
            nummer = 0;
            sender.sendMessage("§cGebe einen gültige Perknummer an!");
        }
        return nummer;
    }

    private void setPerkItem(final Inventory iextra, final Set<Byte> active, final Set<Byte> owned, final ItemStack inksack, final int pos, final String name, final byte perk, final ItemStack i, final String lore) {
        final boolean own = owned.contains(perk), enabled = active.contains(perk);
        iextra.setItem(pos, ms.addLore(ms.renameItemStack(own ? enabled ? ms.addGlow(i) : i : inksack,
                        "§6§l" + name + (enabled ? "§6 §a§l" + ms.getLang("extras.activated") : ""), lore),
                own ? new String[]{"", ms.getLang("extras.perklore"), enabled ? ms.getLang("extras.perkdeactivate") : ms.getLang("extras.perkactivate")} : new String[]{"", "§7Klicke, um diesen Perk", "§7jetzt §6freizuschalten§7."}));
    }

    private void setAdminPerkItem(final Inventory iextra, final Set<Byte> owned, final int pos, final String name, final byte perk, final ItemStack i, final String lore, final boolean edit) {
        final boolean own = owned.contains(perk);
        final ItemStack item = ms.renameItemStack(own ? ms.addGlow(i) : i, "§6§l" + name, lore);
        iextra.setItem(pos, edit == false ? item : ms.addLore(item, "",
                own ? "§7Klicke, um dieses Perk §czu entfernen§7." : "§7Klicke§7, um dieses Perk §ahinzuzufügen§7."));
    }

    private void setCMDItem(final Inventory iextra, final Set<String> list, final ItemStack inksack, final int pos, final String name, final ItemStack i, final String... lore) {
        final boolean own = list.contains(name);
        iextra.setItem(pos, ms.addLore(ms.renameItemStack(own ? i : inksack, "§6§l/" + name, lore), own ? new String[0] : new String[]{"", "§7Klicke, um diesen Befehl", "§7jetzt §bfreizuschalten§7."}));
    }

    private void setAdminCMDItem(final Inventory iextra, final Set<String> owned, final int pos, final String name, final ItemStack i, final boolean edit, final String... lore) {
        final boolean own = owned.contains(name);
        final ItemStack item = ms.renameItemStack(own ? ms.addGlow(i) : i, "§6§l/" + name, lore);
        iextra.setItem(pos, edit == false ? item : ms.addLore(item,
                "", own ? "§7Klicke, um diesen Befehl §czu entfernen§7." : "§7Klicke§7, um diesen Befehl §ahinzuzufügen§7." ));
    }

    private void setPetItem(final Inventory iextra, final EntityType active, final Set<EntityType> owned, final ItemStack inksack, final int pos, final String name, final EntityType type) {
        final boolean haspet = owned.contains(type);
        final int price = getPetPrice(type);
        iextra.setItem(pos, ms.renameItemStack(haspet ? active == type ? ms.addGlow(new SpawnEgg(type).toItemStack(1)) : new SpawnEgg(type).toItemStack(1) : inksack,
                "§6§l" + name + (active == type ? "§6 §a§l" + ms.getLang("extras.activated") : ""),
                price == -1 ? new String[]{"", ms.getLang("extras.notavailable")} :
                        active == type ? new String[]{"", ms.getLang("extras.petlore"), ms.getLang("extras.petdeactivate")} :
                                haspet ? new String[]{"", ms.getLang("extras.petlore"), ms.getLang("extras.petactivate")} :
                                        new String[]{"", "§7Klicke, um dieses Haustier", "§7jetzt §bfreizuschalten§7."}));
    }

    private void setAdminPetItem(final Inventory inv, final Set<EntityType> owned, final int pos, final String name, final EntityType type, final boolean edit) {
        final boolean haspet = owned.contains(type);
        final ItemStack item = ms.renameItemStack(haspet ? ms.addGlow(new SpawnEgg(type).toItemStack(1)) : new SpawnEgg(type).toItemStack(1), "§6§l" + name);
        inv.setItem(pos, edit == false ? item : ms.addLore(item, "", haspet ? "§7Klicke§7, um dieses Pet §czu entfernen§7." : "§7Klicke, um dieses Pet §ahinzuzufügen§7."));
    }

    private void setMountItem(final Inventory iextra, final int active, final Set<Short> owned, final ItemStack inksack, final Player p, final int pos, final String name, final Material m, final short num) {
        final boolean hasm = owned.contains(num) || (num == 9 && p.hasPermission("muxsystem.ultra")) || (num == 14 && ms.hasGold(p.getUniqueId())) || (num == 17 && ms.getMuxUser(p.getName()).getTrophies() > 749);
        final int price = getMountPrice(num);
        iextra.setItem(pos, ms.renameItemStack(hasm ? active == num ? ms.addGlow(new ItemStack(m)) : new ItemStack(m) : inksack,
                "§6§l" + name + (active == num ? "§6 §a§l" + ms.getLang("extras.activated") : ""), price == -1 ? new String[]{"", "§eNicht verfügbar."} :
                        active == num ? new String[]{"", "§7Klicke, um diesen Mount", "§7zu §cdeaktivieren§7."} :
                                hasm ? new String[]{"", "§7Klicke, um diesen Mount", "§7zu §aaktivieren§7."} :
                                        price == -2 ? new String[]{"", "§eExklusiv mit §c§lULTRA"} :
                                                price == -3 ? new String[]{"", "§eExklusiv mit §e§lGOLD"} :
                                                price == -4 ? new String[]{"", "§eExklusiv ab der §fSilber Liga"} :
                                                        new String[]{"", "§7Klicke, um diesen Mount", "§7jetzt §bfreizuschalten§7."}));
    }

    private void setAdminMountItem(final Inventory inv, final Set<Short> owned, final int pos, final String name, final Material m, final short num, final boolean edit) {
        final boolean hasm = owned.contains(num) || num == 9;
        final ItemStack item = ms.renameItemStack(hasm ? ms.addGlow(new ItemStack(m)) : new ItemStack(m), "§6§l" + name);
        inv.setItem(pos, edit == false ? item : ms.addLore(item, "", hasm ? "§7Klicke§7, um dieses Mount §czu entfernen§7." : "§7Klicke, um dieses Mount §ahinzuzufügen§7."));
    }

    private void setEmojiItem(final Inventory iextra, final int active, final Set<Short> owned, final ItemStack inksack, final int pos, final String name, final ItemStack item, final short num) {
        final boolean hase = owned.contains(num);
        iextra.setItem(pos, ms.renameItemStack(hase ? item : inksack,
                "§6§l" + name + (active == num ? "§6 §a§l" + ms.getLang("extras.activated") : ""), num > getEmojis() ? new String[]{"", "§eNicht verfügbar"} :
                        active == num ? new String[]{"", "§7Klicke, um diesen Emoji", "§7zu §cdeaktivieren§7."} :
                                hase ? new String[]{"", "§7Klicke, um diesen Emoji", "§7zu §aaktivieren§7."} :
                                        new String[]{"", "§7Klicke, um diesen Emoji", "§7jetzt §bfreizuschalten§7."}));
    }

    private void setAdminEmojiItem(final Inventory iextra, final Set<Short> owned, final int pos, final String name, final ItemStack item, final short num, final boolean edit) {
        final boolean hase = owned.contains(num);
        final ItemStack it =  ms.renameItemStack(hase ? ms.addGlow(item) : item, "§6§l" + name);
                iextra.setItem(pos, edit == false ? it : ms.addLore(it, "", hase ? "§7Klicke§7, um dieses Emoji §czu entfernen§7." : "§7Klicke§7, um dieses Emoji §ahinzuzufügen§7."));
    }

    private boolean handleBloodBending(final Player player) {
        if (bloodtargeted.containsKey(player) == false) {
            final Entity entity = getTargetedEntity(player, 12D);
            if (entity == null) return false;
            if ((entity instanceof LivingEntity) == false || entity instanceof Player || ms.canBuildAtLoc(player, entity.getLocation()) == false)
                return false;
            bloodtargeted.put(player, new HashMap<>());
            ((LivingEntity) entity).damage(0D);
            bloodtargeted.get(player).put(entity, entity.getLocation().clone());
            return true;
        }
        bloodtargeted.remove(player);
        return false;
    }

    private void processBloodBending() {
        bloodtargeted.forEach((player, entityLocationMap) -> {
            if (player.isSneaking() == false || player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR || player.getItemInHand().getItemMeta().hasDisplayName() == false || player.getItemInHand().getItemMeta().getDisplayName().contains("Blutbendiger Stock") == false) {
                bloodtargeted.remove(player);
                bloodbending.remove(player);
                return;
            }
            entityLocationMap.forEach((entity, location) -> {
                final Location newlocation2 = entity.getLocation(), location2 = getTargetedLocation(player, (int) location.distance(player.getLocation()));
                final double distance2 = location2.distance(newlocation2), dx2 = location2.getX() - newlocation2.getX(), dy2 = location2.getY() - newlocation2.getY(), dz2 = location2.getZ() - newlocation2.getZ();
                final Vector vector2 = new Vector(dx2, dy2, dz2);
                if (distance2 > 0.5) {
                    entity.setVelocity(vector2.normalize().multiply(0.5));
                } else {
                    entity.setVelocity(new Vector(0, 0, 0));
                }
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 2, 60, false, false));
                entity.setFallDistance(0F);
                if (entity instanceof Creature == false) return;
                ((Creature) entity).setTarget(null);
            });
        });
    }

    private Entity getTargetedEntity(final Player player, final double range) {
        double longestr = range + 1.0D;
        Entity target = null;
        final Location origin = player.getEyeLocation();
        final Vector direction = player.getEyeLocation().getDirection().normalize();
        for (final Entity entity : origin.getWorld().getEntities()) {
            if (entity.getLocation().distance(origin) < longestr && getDistanceFromLine(direction, origin, entity.getLocation()) < 2.0D && entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId()) {
                if (entity.getLocation().distance(origin.clone().add(direction)) < entity.getLocation().distance(origin.clone().add(direction.clone().multiply(-1)))) {
                    target = entity;
                    longestr = entity.getLocation().distance(origin);
                }
            }
        }
        return target;
    }

    private Location getTargetedLocation(final Player player, final int range) {
        return getTargetedLocation(player, range, 0);
    }

    private Location getTargetedLocation(final Player player, final double originselectrange, final Integer... nonOpaque2) {
        final Location origin = player.getEyeLocation();
        final Vector direction = origin.getDirection();
        HashSet<Byte> trans = new HashSet<>();
        trans.add((byte) 0);
        if (nonOpaque2 == null) {
            trans = null;
        } else {
            for (final Integer i : nonOpaque2) {
                trans.add(i.byteValue());
            }
        }
        final Block block = player.getTargetBlock(trans, (int) originselectrange + 1);
        final double distance = block.getLocation().distance(origin) - 1.5;
        return origin.add(direction.multiply(distance));
    }

    private double getDistanceFromLine(final Vector line, final Location pointonline, final Location point) {
        final Vector aP = new Vector();
        final double aX = pointonline.getX(), aY = pointonline.getY(), aZ = pointonline.getZ(), pX = point.getX(), pY = point.getY(), pZ = point.getZ();
        aP.setX(pX - aX);
        aP.setY(pY - aY);
        aP.setZ(pZ - aZ);
        return aP.crossProduct(line).length() / line.length();
    }

    @EventHandler
    public void onToggleSneak(final PlayerToggleSneakEvent event) {
        final Player player = event.getPlayer();
        final ItemStack itemInHand = player.getItemInHand();
        if (itemInHand != null && itemInHand.getType() == Material.STICK && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName() && itemInHand.getItemMeta().getDisplayName().contains("Blutbendiger Stock") && itemInHand.getItemMeta().hasLore()) {
            if (event.isSneaking() == false && bloodbending.contains(player)) {
                bloodbending.remove(player);
                bloodtargeted.remove(player);
            } else if (event.isSneaking() && bloodbending.contains(player) == false) {
                int currentCount = Integer.parseInt(ChatColor.stripColor(itemInHand.getItemMeta().getLore().get(0)).replaceAll("[:a-zA-Z ]*", "").split("/")[0]);
                if (handleBloodBending(player)) {
                    if (--currentCount == -1) {
                        player.setItemInHand(null);
                        player.playSound(player.getLocation(), Sound.ITEM_BREAK, 1F, 1F);
                        return;
                    }
                    final List<String> lore = itemInHand.getItemMeta().getLore();
                    lore.remove(0);
                    lore.add(0, "§7Haltbarkeit: §c" + currentCount + "/100");
                    final ItemMeta meta = itemInHand.getItemMeta();
                    meta.setLore(lore);
                    itemInHand.setItemMeta(meta);
                    bloodbending.add(player);
                }
            }
        }
    }
}