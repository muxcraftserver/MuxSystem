package me.muxteam.extras;

import me.muxteam.basic.MuxActions.RouletteAction;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxSystem.Priority;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.pvp.MuxPvP;
import me.muxteam.shared.MuxSharedPackets;
import me.muxteam.shop.MuxShop;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.Tuple;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public final class MuxChests {
    private final Random r = new Random();
    private final Location chest;
    private final Map<UUID, MuxChestUser> chests = new HashMap<>();
    private Map<ChestType, List<Tuple<Double, RarityItem>>> topchests = new EnumMap<>(ChestType.class);
    private final Set<String> newbiechestopens = new HashSet<>();
    private final MuxExtras extras;
    private MuxSystem ms;

    public MuxChests(final MuxSystem ms) {
        this.ms = ms;
        this.extras = ms.getExtras();
        this.chest = new Location(ms.getGameSpawn().getWorld(), -16D, 77D, -1D);
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshTopChests();
            }
        }.runTaskLater(ms, 100L);
    }

    public void close() {
        save();
        this.ms = null;
    }

    public void save() {
        chests.values().parallelStream().filter(Objects::nonNull).forEach(ms.getDB()::saveChests);
    }

    public void handleJoin(final Player p, boolean brandnew) {
        new BukkitRunnable() {
            @Override
            public void run() {
                final MuxChestUser chestuser = ms.getDB().loadChests(p.getUniqueId());
                setChestData(p.getUniqueId(), chestuser);
                if (brandnew && chestuser.getChests().isEmpty()) {
                    for (int i = 0; i < 4; i++) chestuser.addChest(MuxChests.ChestType.NEWBIE);
                    ms.getDB().saveChests(chestuser);
                }
            }
        }.runTaskAsynchronously(ms);
    }

    public void handleQuit(final String name, final UUID uuid) {
        newbiechestopens.remove(name);
        ms.getDB().saveChests(chests.remove(uuid));
    }

    private void saveChestUser(final MuxChestUser u) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.getDB().saveChests(u);
            }
        }.runTaskAsynchronously(ms);
    }

    public void setChestData(final UUID uuid, final MuxChestUser user) {
        final Player p = ms.getServer().getPlayer(uuid);
        if (p != null) {
            if (user == null) {
                chests.put(uuid, new MuxChestUser(uuid, new CopyOnWriteArrayList<>(), false));
                return;
            }
            chests.put(uuid, user);
        }
    }

    public MuxChestUser getChestUser(final UUID uuid) {
        final MuxChestUser user = chests.get(uuid);
        if (user == null) {
            return new MuxChestUser(uuid, new CopyOnWriteArrayList<>(), false);
        }
        return user;
    }

    public boolean containsChestUser(final UUID uuid) {
        return this.chests.containsKey(uuid);
    }

    public void pushAway() {
        for (final Player p : chest.getWorld().getPlayers()) {
            if (p.getLocation().distance(chest) < 2D) {
                if (ms.checkLimit(p.getName(), "CHESTAFK", 5, true) == false) {
                    continue;
                }
                final Vector v = ms.getGameSpawn().toVector().subtract(chest.toVector());
                p.setVelocity(v.normalize().multiply(1.2D));
            }
        }
    }

    public void refreshTopChests() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final Map<ChestType, List<Tuple<Double, RarityItem>>> topChests = new EnumMap<>(ChestType.class);
                final MuxShop shop = ms.getShop();

                final ChestType[] topTypes = {ChestType.TOP3, ChestType.TOP10, ChestType.TOP20, ChestType.TOP30, ChestType.TOP50, ChestType.TOP100, ChestType.TOP200};
                final List<String> notInShop = new ArrayList<>();
                final List<Tuple<String, Double>> fulltopitems = ms.getDB().getTopItemsByAvgPriceLast12Hours(200);
                for (final ChestType topType : topTypes) {
                    final List<Tuple<Double, RarityItem>> items = new ArrayList<>();
                    topChests.put(topType, items);
                    final int limit = Integer.parseInt(topType.name().substring(3));
                    final List<Tuple<String, Double>> topitems = fulltopitems.stream().limit(limit).collect(Collectors.toList());

                    final ChestType last = topTypes[topTypes.length - 1];

                    if (last == topType) {
                        shop.getItemIDs().forEach(itemId -> {
                            if (topitems.stream().noneMatch(tuple -> tuple.a().equals(itemId)) && shop.getCheapestPrice(itemId) == -1) {
                                topitems.add(new Tuple<>(itemId, 1D));
                                if (notInShop.contains(itemId) == false)
                                    notInShop.add(itemId);
                            }
                        });
                    }
                    final long total = Math.round(topitems.stream().mapToDouble(Tuple::b).sum());
                    double totalPercentage = 0.00D;
                    final double[] percentages = new double[topitems.size()];
                    for (final Tuple<String, Double> oTuple : topitems) {
                        double percentage;
                        totalPercentage += percentage = ((total / (double) topitems.size()) / oTuple.b());
                        percentages[topitems.indexOf(oTuple)] = percentage;
                    }
                    final double median = Arrays.stream(percentages).sum() / percentages.length;
                    final double[] lower = new double[Math.toIntExact(Arrays.stream(percentages).filter(value -> value < median).count())],
                            upper = new double[Math.toIntExact(Arrays.stream(percentages).filter(value -> value >= median).count())];
                    int l = 0, u = 0;
                    for (final double d : percentages) {
                        if (d < median) {
                            lower[l] = d;
                            l++;
                        } else {
                            upper[u] = d;
                            u++;
                        }
                    }
                    final double lowerMedian = Arrays.stream(lower).sum() / lower.length,
                            upperMedian = Arrays.stream(upper).sum() / upper.length;
                    for (final Tuple<String, Double> tuple : topitems) {
                        double percentage = ( (total / (double) topitems.size()) / tuple.b());
                        final Rarity rarity;
                        if (limit <= 3) {
                            rarity = Rarity.LEGENDARY;
                        } else if (percentage >= upperMedian) {
                            rarity = Rarity.COMMON;
                        } else if (percentage >= median) {
                            rarity = Rarity.RARE;
                        } else if (percentage >= lowerMedian) {
                            rarity = Rarity.EPIC;
                        } else {
                            rarity = Rarity.LEGENDARY;
                        }
                        percentage = (percentage / totalPercentage) * 100; // real percentage (because we need 100% in the end)
                        final String itemId = tuple.a();
                        final int id = Integer.parseInt(itemId.split(":")[0]), d = Integer.parseInt(itemId.split(":")[1]);
                        final String color = rarity == Rarity.RARE ? "§b" : rarity == Rarity.EPIC ? "§5" : rarity == Rarity.LEGENDARY ? "§a" : "§f";
                        final ItemStack i = ms.renameItemStack(new ItemStack(id, 1, (short) d), color + shop.getItemName(itemId));
                        if (i.getType() == Material.POTION && d > 10000) {
                            i.setDurability((short) (d | 0x4000));
                        }
                        items.add(new Tuple<>(percentage, new RarityItem(rarity, i)));
                    }
                }

                for (final ChestType topType : topTypes) {
                    if (topType == ChestType.TOP10 || topType == ChestType.TOP3) continue;
                    final int baseAmount = Integer.parseInt(topType.name().substring(3)) / 10;
                    for (final Tuple<Double, RarityItem> tuple : topChests.get(topType)) {
                        int amount = baseAmount;
                        for (int j = 0; j < getIndexOf(topTypes, topType); j++) {
                            final ChestType tempType = topTypes[j];
                            if (containsItemInTop(tempType, tuple.b().item.getType(), tuple.b().item.getDurability(), topChests)) {
                                amount = Integer.parseInt(tempType.name().substring(3)) / 10;
                                break;
                            }
                        }
                        if (tuple.b().item.getType() == Material.POTION)
                            amount = 16;
                        else if (notInShop.contains(tuple.b().item.getTypeId() + ":" + tuple.b().item.getDurability()))
                            amount = 3;
                        if (amount <= 0)
                            amount = 1;
                        else if (amount > 64)
                            amount = 64;
                        tuple.b().getItem().setAmount(amount);
                    }
                }
                new BukkitRunnable() { // going sync again to avoid concurrent
                    @Override
                    public void run() {
                        topchests = topChests;
                    }
                }.runTask(ms);
            }
        }.runTaskAsynchronously(ms);
    }

    private int getIndexOf(final Object[] array, final Object obj) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == obj)
                return i;
        }
        return -1;
    }

    private boolean containsItemInTop(final ChestType chestType, final Material type, final short damage, final Map<ChestType, List<Tuple<Double, RarityItem>>> topChests) {
        final List<Tuple<Double, RarityItem>> top = topChests.get(chestType);
        if (top == null) return false;
        for (final Tuple<Double, RarityItem> tuple : top) {
            if (tuple.b().getItem().getType() == type && tuple.b().getItem().getDurability() == damage)
                return true;
        }
        return false;
    }

    public ChestType getRandomChest(final String pname) {
        final RandomCollection<ChestType> list = new RandomCollection<>();
        list.add(50, ChestType.TOP200);
        list.add(25, ChestType.TOP100);
        list.add(15, ChestType.TOP50);
        list.add(6, ChestType.TOP30);
        list.add(3, ChestType.TOP20);
        list.add(1, ChestType.TOP10);
        list.add(0.02, ChestType.TOP3);
        final ChestType next = list.next();
        if (next == ChestType.TOP3) {
            ms.broadcastMessage("§6§lMuxKisten>§7 Der Spieler §6" + pname + " §7hat aus Glück eine " + next.getName() + " §7erhalten.", null, Priority.NORMAL);
        }
        return next;
    }

    public void handleInventory(final Player p, final ItemStack i, final int rawslot, final String title, final int invsize) {
        String display = i.getItemMeta().getDisplayName();
        final Material m = i.getType();
        final int pos = rawslot >= 20 ? rawslot - 14 : rawslot - 10;
        if (invsize == 54) {
            final ChestType ct = getChestType(ChatColor.stripColor(display));
            final OfflinePlayer op = ms.getPlayer(title.split(" \\| ")[1]);
            if (m == Material.ITEM_FRAME) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                ms.getOverview().openDataManagement(p, op);
                return;
            }
            if (ct == null) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final MuxChestUser cu = op.isOnline() ? getChestUser(op.getUniqueId()) : ms.getDB().loadChests(op.getUniqueId());
            if (rawslot > 35) {
                addChest(cu, ct);
                ms.getHistory().addHistory(cu.getUUID(), p.getUniqueId(), "TEAMACTION", "CRATE ADD", ct.name());
                ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketAddChest(op.getUniqueId(), ct, false, p.getUniqueId()));
            } else {
                cu.removeChest(pos - 1);
                ms.getHistory().addHistory(cu.getUUID(), p.getUniqueId(),"TEAMACTION", "CRATE REMOVE", ct.name());
                ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketRemoveChest(op.getUniqueId(), pos - 1, p.getUniqueId(), ct.name()));
            }
            if (op.isOnline()) {
                if (ms.getActiveInv(op.getName()) == InvType.MUXCHESTS && op.getName().equals(p.getName()) == false)
                    op.getPlayer().closeInventory();
                openChestAdminInv(p, op);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openChestAdminInv(p, op);
                    }
                }.runTaskLater(ms, 2L);
            }
            saveChestUser(cu);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            return;
        }
        if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("crates");
            return;
        } else if (invsize == 45) {
            final ChestType ct = getChestType(ChatColor.stripColor(display));
            if (ct == null) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            final NavigableMap<Double, RarityItem> items = getItems(p, ct).map;
            final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxKisten§0 | " + ct.getRawName());
            short slot = 0;
            double chance = 0;
            for (final Map.Entry<Double, RarityItem> item : items.entrySet()) {
                if (slot == inv.getSize()) break;
                inv.setItem(slot, ms.addLore(item.getValue().getItem().clone(), "§f§l" + (item.getKey() - chance) + "%", "§f§l" + item.getValue().getRarity().name()));
                chance += (item.getKey() - chance);
                slot++;
            }
            if (ms.getActiveInv(p.getName()) != InvType.MUXCHESTS) p.closeInventory();
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.NOINTERACT);
            return;
        } else if (i.getType() == Material.CHEST) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openAllChests(p);
            return;
        }
        if (display.contains("§8 »")) {
            display = display.split("§8 » ")[0];
        }
        final ChestType ct = getChestType(ChatColor.stripColor(display));
        if (ct == null) {
            if (display.contains("Vote")) {
                p.performCommand("vote");
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        if (i.getItemMeta().getLore().get(1).startsWith("§c") && ct.getColor().equals("§c") == false) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        openChest(p, getChestUser(p.getUniqueId()), ct, pos);
    }

    public void addChest(final UUID uuid, final ChestType ct) {
        final MuxChestUser u = getChestUser(uuid);
        addChest(u, ct);
    }

    private void addChest(final MuxChestUser u, final ChestType ct) {
        u.addChest(ct, true);
        saveChestUser(u);
    }

    public void openChest(final Player p, final ChestType ct) {
        openChest(p, getChestUser(p.getUniqueId()), ct, -1);
    }

    private void openChest(final Player p, final MuxChestUser u, final ChestType ct, final int pos) {
        if (u.getChests().size() < pos) {
            p.closeInventory();
            ms.showItemBar(p, "§cProbiere es erneut.");
            return;
        }
        if (p.getInventory().firstEmpty() == -1) {
            p.closeInventory();
            ms.showItemBar(p, "§cDein Inventar ist voll.");
            return;
        } else if (ms.getEmptyInvSlots(p.getInventory()) < 3 /*&& ct != ChestType.DAILY*/) {
            p.closeInventory();
            ms.showItemBar(p, "§cDu benötigst mind. §63 freie Slots §cin deinem Inventar.");
            return;
        } else if (ms.hasVoted(p) == false) {
            p.closeInventory();
            ms.sendNoVoteMessage(p);
            return;
        } else if (ct == ChestType.NEWBIE && newbiechestopens.add(p.getName()) == false) {
            p.closeInventory();
            ms.showItemBar(p, "§cDu öffnest bereits eine " + ct.getRawName() + "§c.");
            return;
        }
        final RandomCollection<RarityItem> items = getItems(p, ct);
        if (items.size() == 0) {
            p.closeInventory();
            ms.showItemBar(p, "§cEs wurden noch keine Items für die Kiste gesetzt.");
            return;
        } else {
            ms.getAnalytics().getAnalytics().addChestOpen(); // Don't track Daily Chest
            ms.getAnalytics().getAnalytics().addPlayerChestOpen(p.getUniqueId());
            u.removeChest(pos - 1);
            saveChestUser(u);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                new Roulette(items, itemstack -> {
                    final boolean topcrate = ct.name().startsWith("TOP") || ct == ChestType.EVENT || ct == ChestType.GIFT || ct == ChestType.ADVENT;
                    final boolean alreadyhas = giveItem(p, itemstack, topcrate);
                    final ChestType alreadyhasct = ChestType.EVENT;
                    final String reward = itemstack.getItemMeta().getDisplayName();
                    final int coins = (r.nextInt(3) + 1) * ct.getCoinBonus();
                    if (reward.startsWith("§a")) { // Legendary
                        ms.broadcastMessage("§6§lMuxKisten>§7 Der Spieler §6" + p.getName() + " §7hat aus der " + ct.getAdjName()
                                + " §7als Gewinn §a" + (topcrate ? (itemstack.getAmount() + "x ") : getAdj(reward)) + reward + " §7gezogen.", null, Priority.NORMAL);
                        ms.playEffect(p, EnumParticle.MOB_APPEARANCE, p.getLocation(), 1, 1, 1, 0, 1);
                    }
                    ms.sendTitle(p, ct.getName(), 10, 120, coins == 0 ? 10 : 0);
                    ms.sendSubTitle(p, "§fDeine Belohnung: §6§l" + (alreadyhas ? "§c§m" + ChatColor.stripColor(reward) : reward)
                            + (alreadyhas ? alreadyhasct.getColor() + " (+" + alreadyhasct.getName() + alreadyhasct.getColor() + ")" : ""), 10, coins == 0 ? 120 : 60, coins == 0 ? 10 : 0);
                    if (alreadyhas) {
                        addChest(p.getUniqueId(), alreadyhasct);
                    }
                    if (coins > 0) {
                        final MuxUser u = p.isOnline() ? ms.getMuxUser(p.getName()) : ms.getDB().loadPlayer(p.getUniqueId());
                        ms.runLater(new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (p.isOnline() == false) return;
                                ms.sendSubTitle(p, "§fDein Bonus: §e§l" + ms.getNumberFormat(coins) + " MuxCoins", 0, 60, 10);
                                ms.sendScoreboard(p);
                            }
                        }, 60L);
                        if (u != null) {
                            ms.getAnalytics().addChestExpenses(coins);
                            u.addCoins(coins);
                            ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(coins), "Crate");
                        }
                        if (p.isOnline() == false) ms.saveMuxUser(u);
                    }
                }).startTask(p, "§0§lMuxKiste§0 | " + ct.getRawName());
            }
        }.runTaskLater(ms, 2L);
    }

    private String getAdj(final String s) {
        if (s.contains("Rüstung") || s.contains("Kartoffel")) {
            return "eine ";
        } else if (s.contains("Schwert")) {
            return "ein ";
        } else if (s.matches(".*\\d+.*") || s.contains("Booster")) {
            return "";
        }
        return "einen ";
    }

    private boolean giveItem(final Player p, ItemStack i, final boolean topcrate) {
        if (p == null || p.isOnline() == false) return false;
        final Material m = i.getType();
        final String display = ChatColor.stripColor(i.getItemMeta().getDisplayName());
        if (topcrate) {
            final ItemStack gi = new ItemStack(i.getType(), i.getAmount(), i.getDurability());
            if (gi.getType() == Material.POTION && i.getDurability() > 10000) {
                final int data = gi.getData().getData() & 0xFF;
                gi.setDurability((short) (data | 0x4000));
            }
            p.getInventory().addItem(gi);
            if (gi.getType().isBlock() && ms.getShop().getCheapestPrice(gi.getTypeId() + ":" + gi.getDurability()) > 0.00D && ms.checkGeneralCooldown(p.getName(), "CRATEBASEINFO", 60000L, true) == false) {
                ms.chatClickHoverRun(p, "§a§lMuxBase>§7 Klicke §ahier§7, um den Block in deiner Base zu setzen.", "§a§oKlicke zum teleportieren", "/base");
            } else {
                if (ms.checkGeneralCooldown(p.getName(), "CRATESHOPINFO", 15000L, true)) return false;
                ms.chatClickHoverRun(p, "§e§lMuxShop>§7 Klicke §ehier§7, um das Item zum Verkauf zu stellen.", "§e§oKlicke zum Verkaufen", "/shop");
            }
            return false;
        } else if (m == Material.DOUBLE_PLANT) {
            final String c = display.split(" ")[0].replace(".", "");
            if (ms.isNumeric(c) == false) return false;
            final MuxUser u = ms.getMuxUser(p.getName());
            final int coins = Integer.parseInt(c);
            u.addCoins(coins);
            ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(coins), "Crate");
            ms.getAnalytics().addChestExpenses(coins);
            ms.sendScoreboard(p);
            return false;
        } else if (m == Material.DIAMOND_CHESTPLATE) {
            if (display.contains("Vollenchant")) {
                extras.addItem(p, "armor");
            } else {
                final PlayerInventory pi = p.getInventory();
                final Map<Enchantment, Integer> aEnch = new HashMap<>();
                aEnch.put(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                aEnch.put(Enchantment.DURABILITY, 3);
                pi.addItem(ms.addEnchants(new ItemStack(Material.DIAMOND_HELMET), aEnch));
                pi.addItem(ms.addEnchants(new ItemStack(Material.DIAMOND_CHESTPLATE), aEnch));
                pi.addItem(ms.addEnchants(new ItemStack(Material.DIAMOND_LEGGINGS), aEnch));
                pi.addItem(ms.addEnchants(new ItemStack(Material.DIAMOND_BOOTS), aEnch));
            }
            return false;
        } else if (m == Material.DIAMOND_SWORD) {
            if (display.contains("Vollenchant")) {
                extras.addItem(p, "sword");
            } else {
                p.getInventory().addItem(ms.addEnchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.DAMAGE_ALL, 5));
            }
            return false;
        } else if (m == Material.DIAMOND_PICKAXE) {
            extras.addItem(p, display.contains("Multi") ? " multipickaxe" : " pickaxe");
            return false;
        } else if (m == Material.DIAMOND_AXE) {
            extras.addItem(p, "multiaxe");
            return false;
        } else if (m == Material.DIAMOND_SPADE) {
            extras.addItem(p, "multishovel");
            return false;
        } else if (m == Material.FIREWORK_CHARGE) {
            if (display.contains("Fly Booster")) {
                extras.addItem(p, "flybooster");
                extras.addItem(p, "flybooster");
            } else if (display.contains("XP Booster")) {
                extras.addItem(p, "xpbooster");
                extras.addItem(p, "xpbooster");
            } else if (display.contains("Mega Spawner Booster")) {
                for (int n = 0; n < 3; n++) extras.addItem(p, "megaspawnerbooster");
            } else if (display.contains("Spawner Booster")) {
                for (int n = 0; n < 5; n++) extras.addItem(p, "spawnerbooster");
            } else if (display.contains("Globaler XP Booster")) {
                extras.addItem(p, "globalxpbooster");
            }
            return false;
        } else if (m == Material.POTATO_ITEM) {
            extras.addItem(p, "potato");
            return false;
        } else if (m == Material.MOB_SPAWNER) {
            extras.addItem(p, "spawner");
            if (display.contains("2")) {
                extras.addItem(p, "spawner");
            }
            return false;
        } else if (m == Material.WATER_BUCKET) {
            extras.addItem(p, "netherwater");
            return false;
        } else if (m == Material.EXP_BOTTLE && display.contains("Level")) {
            ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "exp add " + p.getName() + " 47520");
            return false;
        } else if (m == Material.SKULL_ITEM) {
            if (display.equals("Dein Kopf")) {
                i = ms.renameItemStack(ms.getHead(p.getName()), "§aMuxKopf §7» §a" + p.getName());
            } else if (display.equals("Emoji")) {
                final Set<Short> ownedemojis = extras.getExtraUser(p.getUniqueId()).getEmojis();
                if (ownedemojis.size() == extras.getEmojis()) {
                    return true;
                }
                for (short n = 1; n < extras.getEmojis() + 1; n++) {
                    if (ownedemojis.contains(n) == false) {
                        ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "emoji add " + p.getName() + " " + n);
                        break;
                    }
                }
                ms.showLater(p, "§fÖffne §6/emojis§f, um deine Belohnung zu sehen.");
                return false;
            }
        } else if (m == Material.MONSTER_EGG) {
            final Set<EntityType> ownedpets = extras.getExtraUser(p.getUniqueId()).getPets().getOwned();
            if (ownedpets.size() == extras.getPets().size()) {
                return true;
            } else for (final EntityType et : extras.getPets()) {
                if (ownedpets.contains(et) == false) {
                    ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "pets add " + p.getName() + " " + et.name());
                    break;
                }
            }
            ms.showLater(p, "§fÖffne §6/pets§f, um deine Belohnung zu sehen.");
            return false;
        } else if (m == Material.IRON_BARDING) {
            final Set<Short> ownedmounts = extras.getExtraUser(p.getUniqueId()).getMounts().getOwned();
            if (ownedmounts.size() == extras.getMounts() - 2) {
                return true;
            }
            for (short n = 1; n < extras.getMounts() + 1; n++) {
                if (ownedmounts.contains(n) == false && n != 9 && n != 17) {
                    ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "mounts add " + p.getName() + " " + n);
                    break;
                }
            }
            ms.showLater(p, "§fÖffne §6/mounts§f, um deine Belohnung zu sehen.");
            return false;
        } else if (m == Material.COMMAND) {
            final Set<String> ownedcmds = extras.getExtraUser(p.getUniqueId()).getExtraCMDs();
            if (ownedcmds.size() == extras.getExtraCommands().size()) {
                return true;
            }
            ms.showLater(p, "§fÖffne §6/extras§f, um deine Belohnung zu sehen.");
            for (final String s : extras.getShuffledExtraCommands()) {
                if (ownedcmds.contains(s) == false) {
                    ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "extracmd add " + p.getName() + " " + s);
                    return false;
                }
            }
            return false;
        } else { // EXP Bottles, Heal Potions, Diamonds
            final ItemStack iC = i.clone();
            final ItemMeta meta = iC.getItemMeta();
            meta.setDisplayName(null);
            iC.setItemMeta(meta);
            if (display.contains("x")) {
                try {
                    iC.setAmount(Integer.parseInt(display.split("x")[0]));
                } catch (final Exception ignored) {
                }
            }
            i = iC;
        }
        p.getInventory().addItem(i.clone());
        return false;
    }

    private RandomCollection<RarityItem> getItems(final Player p, final ChestType ct) {
        final RandomCollection<RarityItem> c = new RandomCollection<>();
        if (ct == ChestType.DAILY) { // Daily = Common 50% | Rare 40% | Epic 9% | Legendary 1%
            c.add(50, new RarityItem(Rarity.COMMON, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 2), "§e2.000 MuxCoins")));
            c.add(25, new RarityItem(Rarity.RARE, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 4), "§e4.000 MuxCoins")));
            c.add(15, new RarityItem(Rarity.RARE, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 8), "§e8.000 MuxCoins")));
            c.add(9, new RarityItem(Rarity.EPIC, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 12), "§e12.000 MuxCoins")));
            c.add(1, new RarityItem(Rarity.LEGENDARY, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 30), "§a30.000 MuxCoins")));
        } else if (ct == ChestType.FAKE) {
            c.add(25, new RarityItem(Rarity.COMMON, ms.renameItemStack(new ItemStack(Material.EXP_BOTTLE, 1), "§f10x EXP Flaschen")));
            c.add(25, new RarityItem(Rarity.COMMON, ms.renameItemStack(new ItemStack(Material.POTION, 1, (short) 16421), "§f12x Heal Tränke")));
            c.add(25, new RarityItem(Rarity.COMMON, ms.renameItemStack(new ItemStack(Material.DIAMOND, 4), "§f4x Diamanten")));
        } else if (ct == ChestType.EVENT || ct == ChestType.GIFT || ct == ChestType.ADVENT) {
            return getItems(p, ChestType.TOP50);
        } else if (ct == ChestType.HALLOWEEN || ct == ChestType.CHRISTMAS || ct == ChestType.EASTER || ct == ChestType.SUMMER) {
            c.add(26, new RarityItem(Rarity.EPIC, ms.getEmojis().GRIN4("§5Emoji")));
            c.add(26, new RarityItem(Rarity.EPIC, ms.renameItemStack(new ItemStack(Material.MONSTER_EGG), "§5MuxPet")));
            c.add(20, new RarityItem(Rarity.LEGENDARY, ms.renameItemStack(new ItemStack(Material.IRON_BARDING), "§aMuxMount")));
            c.add(20, new RarityItem(Rarity.LEGENDARY, ms.renameItemStack(new ItemStack(Material.COMMAND), "§aExtra Befehl")));
            c.add(8, new RarityItem(Rarity.LEGENDARY, ms.renameItemStack(ms.getFireworkCharge(Color.PURPLE), "§aGlobaler XP Booster")));
        } else if (ct == ChestType.NEWBIE) {
            final MuxExtraUser eu = extras.getExtraUser(p.getUniqueId());
            if (eu.getPets().getSize() == 0)
                c.add(25, new RarityItem(Rarity.LEGENDARY, ms.renameItemStack(new ItemStack(Material.MONSTER_EGG), "§aMuxPet")));
            if (eu.getMounts().getSize() == 0)
                c.add(25, new RarityItem(Rarity.LEGENDARY, ms.renameItemStack(new ItemStack(Material.IRON_BARDING), "§aMuxMount")));
            if (eu.getEmojis().isEmpty())
                c.add(25, new RarityItem(Rarity.EPIC, ms.getEmojis().GRIN4("§5Emoji")));
            if (eu.getExtraCMDs().isEmpty())
                c.add(25, new RarityItem(Rarity.LEGENDARY, ms.renameItemStack(new ItemStack(Material.COMMAND), "§aExtra Befehl")));
            if (c.size() == 0)
                return getItems(p, ChestType.FAKE);
        } else if (ct.name().startsWith("TOP")) {
            final List<Tuple<Double, RarityItem>> topchest = topchests.get(ct);
            if (topchest != null) {
                topchest.forEach(tuple -> {
                    final RarityItem rarityItem = tuple.b();
                    final ItemStack item = rarityItem.getItem();
                    final double cheapest = ms.getShop().getCheapestPrice(item.getTypeId() + ":" + item.getDurability());
                    rarityItem.setItem(ms.setLore(item.clone(), "",
                            cheapest == -1 ? "§7Das Item steht noch nicht zum Verkauf." : ("§7Marktpreis: §e" + (ms.getNumberFormat(cheapest) + " MuxCoins"))));
                    c.add(tuple.a(), rarityItem);
                });
            }
        }
        return c;
    }

    public boolean handleCommand(final CommandSender sender, final String[] args) {
        if (sender instanceof Player == false) {
            if (args.length < 3) return true;
            final OfflinePlayer victim = ms.getPlayer(args[1]);
            if (victim == null) return true;
            if (args[0].equals("add") == false) return true;
            final MuxChestUser u = victim.isOnline() ? getChestUser(victim.getUniqueId()) : ms.getDB().loadChests(victim.getUniqueId());
            final String str = args[args.length - 1];
            if (str.equalsIgnoreCase("RANDOM")) {
                for (int i = 0; i < 3; i++) {
                    if (u.getChests().size() == 10) break;
                    addChest(u, getRandomChest(victim.getName()));
                }
            } else {
                final ChestType ct;
                try {
                    ct = ChestType.valueOf(str);
                } catch (final Exception ex) {
                    return true;
                }
                addChest(u, ct);
            }
            if (victim.isOnline() == false) saveChestUser(u);
            return true;
        }
        final Player p = (Player) sender;
        if (args.length == 0 || ms.isTrusted(p.getName()) == false) {
            if (p.hasPermission("muxsystem.vip")) {
                openChestInv(p);
                return true;
            }
            p.performCommand("warp kisten");
            return true;
        } else if (args.length < 2 || args[0].equals("evgiveall") == false) {
            final OfflinePlayer victim = ms.getPlayer(args[0]);
            if (victim == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            openChestAdminInv(p, victim);
            return true;
        }
        final ChestType ct;
        try {
            ct = ChestType.valueOf(args[args.length - 1]);
        } catch (final Exception ex) {
            return true;
        }
        final String cn = ct.name();
        if (((cn.contains("TOP10") || cn.contains("TOP20") || cn.contains("TOP30")) && ms.checkLimit(ct.getRawName(), "CHEST", 2, true)) || (ms.checkLimit(ct.getRawName(), "CHEST", 5, true))) {
            ms.showItemBar(p, "§cDie Kiste wurde schon zu oft heute vergeben.");
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            return true;
        }
        ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "CRATE GIVEALL", ct.getRawName());
        ms.broadcastMessage("§d§lMuxGiveAll>§7 Jeder Spieler hat eine " + ct.getColor() + ct.getRawName() + " §7erhalten.", null, Priority.NORMAL);
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            if (ms.hasVoted(pl) == false) {
                ms.chatClickHoverRun(pl, "§d§lMuxGiveAll>§7 Klicke §dhier§7, um zu voten und teilzunehmen.", "§d§oKlicke zum voten", "/vote");
                continue;
            }
            final MuxChestUser u = getChestUser(pl.getUniqueId());
            u.addChest(ct, true);
            pl.playSound(pl.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
        }
        return true;
    }

    public boolean handleInteract(final Player p, final Block b) {
        if (b.getLocation().equals(chest)) {
            openChestInv(p);
            return true;
        }
        return false;
    }

    private void openAllChests(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxKisten §0| Alle Kisten");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.CHEST), "§c§lAlle Kisten", "§7Hier kannst du dir alle", "§7MuxKisten anschauen."));
        int slot = 18;
        for (final ChestType type : ChestType.values()) {
            if (type == ChestType.FAKE || type == ChestType.DAILY) continue;
            inv.setItem(slot, ms.renameItemStack(ms.getHeadFromURL(type.getTexture(), type.getName()), type.getName(), "§0" + type.name(), "§7Klicke, um die Kiste " + type.getColor() + "anzuschauen§7."));
            slot++;
        }
        if (ms.getActiveInv(p.getName()) != InvType.MUXCHESTS) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.MUXCHESTS);
    }

    private void openChestAdminInv(final Player p, final OfflinePlayer victim) {
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxKisten§0 | " + victim.getName());
        final MuxChestUser u = victim.isOnline() ? getChestUser(victim.getUniqueId()) : ms.getDB().loadChests(victim.getUniqueId());
        short pos = 11;
        for (final ChestType ct : u.getChests()) {
            inv.setItem(pos, ms.setLore(ms.getHeadFromURL(ct.getTexture(), ct.getName()), "", "§7Klicke, um die Kiste zu §centfernen§7."));
            if (pos == 15) pos = 20;
            else pos++;
            if (pos == 25) break;
        }
        if (u.getChests().size() < 10) {
            while (pos < 25) {
                inv.setItem(pos, ms.setLore(ms.getHeadFromURL("https://textures.minecraft.net/texture/badc048a7ce78f7dad72a07da27d85c0916881e5522eeed1e3daf217a38c1a",
                        "§7§lFreier Platz"), "§7Halte weitere Plätze frei, um", "§7mehr MuxKisten zu erhalten."));
                if (pos == 15) pos = 20;
                else pos++;
            }
        }
        pos = 36;
        for (final ChestType type : ChestType.values()) {
            if (type == ChestType.FAKE || type == ChestType.DAILY) continue;
            inv.setItem(pos, ms.renameItemStack(ms.getHeadFromURL(type.getTexture(), type.getName()), type.getName(), "§0" + type.name(), "§7Klicke, um die Kiste §ahinzuzufügen§7."));
            pos++;
        }
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.MUXCHESTS && it != InvType.CONFIRM && it != InvType.USERCONTROL) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.MUXCHESTS);
    }

    public void openChestInv(final Player p) {
        if (ms.getAdmin().CHESTS.isActive() == false) {
            ms.showItemBar(p, "§cMuxKisten sind derzeit deaktiviert.");
            return;
        }
        final Inventory inv = ms.getServer().createInventory(null, 36, "§0§lMuxKisten§0 | /kisten");
        final MuxChestUser u = getChestUser(p.getUniqueId());
        if (ms.isTrusted(p.getName())) {
            inv.setItem(8, ms.renameItemStack(new ItemStack(Material.CHEST), "§c§lAlle Kisten", "§7Hier kannst du dir alle", "§7MuxKisten anschauen.", "", "§cKlicke§7, um alle Kisten zu sehen."));
        }
        short pos = 11;
        for (final ChestType ct : u.getChests()) {
            inv.setItem(pos, ms.setLore(ms.getHeadFromURL(ct.getTexture(), ct.getName()), "", ct.getColor() + "Klicke§7, um die Kiste zu öffnen."));
            if (pos == 15) pos = 20;
            else pos++;
            if (pos == 25) break;
        }
        if (u.getChests().size() < 10) {
            if (ms.hasVoted(p) == false) {
                inv.setItem(pos, ms.setLore(ms.getHeadFromURL("https://textures.minecraft.net/texture/d1eb3a9c2a647c6808a88c692896f48294d3b59c9ca9e8ca6265b84840d1fe9a",
                        "§b§lVote für Kisten"), "§7Vote jetzt um kostenlose", "§7MuxKisten zu erhalten.", "", "§7Klicke, um zu §bvoten§7."));
            } else pos--;
            while (pos < 24) {
                if (pos == 15) pos = 20;
                else pos++;
                inv.setItem(pos, ms.setLore(ms.getHeadFromURL("https://textures.minecraft.net/texture/badc048a7ce78f7dad72a07da27d85c0916881e5522eeed1e3daf217a38c1a",
                        "§7§lFreier Platz"), "§7Halte weitere Plätze frei, um", "§7mehr MuxKisten zu erhalten."));
            }
        }
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.MUXCHESTS && it != InvType.CONFIRM) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.MUXCHESTS);
    }

    private ChestType getChestType(final String name) {
        for (final ChestType ct : ChestType.values()) {
            if (ct.getRawName().equals(name)) {
                return ct;
            }
        }
        return null;
    }

    public enum ChestType {
        DAILY("Tägliche Kiste", "§a", -1, 0, "6f68d509b5d1669b971dd1d4df2e47e19bcb1b33bf1a7ff1dda29bfc6f9ebf"),
        FAKE("Eisenkíste", "§f", -1, 0, "f7aadff9ddc546fdcec6ed5919cc39dfa8d0c07ff4bc613a19f2e6d7f2593"),
        TOP200("Eisenkiste", "§f", -1, 0, "f7aadff9ddc546fdcec6ed5919cc39dfa8d0c07ff4bc613a19f2e6d7f2593"),
        TOP100("Goldkiste", "§e", -1, 0, "f37cae5c51eb1558ea828f58e0dff8e6b7b0b1a183d737eecf714661761"),
        TOP50("Diamantkiste", "§b", -1, 0, "25807cc4c3b6958aea6156e84518d91a49c5f32971e6eb269a23a25a27145"),
        TOP30("Große Kiste", "§6", -1, 0, "4ced34211fed4010a8c85724a27fa5fb205d67684b3da517b6821279c6b65d3f"),
        TOP20("Seltene Kiste", "§3", -1, 0, "6e19d8e7fbe7f4e6f478897662f862af4b8fc872ff298fdefc8d5780dd559689"),
        TOP10("Magische Kiste", "§5", -1, 0, "3677e65df2999d0319fdbcba3c092f160b99b4b467983af81fcfa1eb45d39a3"),
        TOP3("Legendäre Kiste", "§c", -1, 0, "b6313ec540d292fcbb17007eef8e69ce5b6798aac3a76a381502ad0c618dc8dd"),
        EVENT("Eventkiste", "§d", -1, 50, "b22396be5f397145c4c647650198935a99ff38a8352fd6c716283a49ea6"),
        GIFT("Geschenkekiste", "§d", -1, 0, "d505b7f8d5d3b912631c5c1ee6cc5b685a8ad4bae9a9552e8fdf7e711951f4"),
        ADVENT("Adventskiste", "§c", -1, 0, "9fddddb8eee11bc312d1acfd621a446b56868aaf66a5071ca9425582b18cdd6"),
        HALLOWEEN("Halloweenkiste", "§6", -1, 5000, "3e818ca67f63c1c8cd5cc2e496cbeed5f456f38b228f49ecc852767548423a"),
        CHRISTMAS("Weihnachtskiste", "§c", -1, 5000, "14e424b1676feec3a3f8ebade9e7d6a6f71f7756a869f36f7df0fc182d436e"),
        EASTER("Osterkiste", "§e", -1, 5000, "92162dceffac9574298f97c27a7ac23867cc573a9a2f1789d3db76ea4d8ed159"),
        // Easter egg: e2d317e1a1283ab2f6474f3a7e18259e60a4791b613319efeda7ebdab89934
        SUMMER("Sommerkiste", "§e", -1, 5000, "92a86755effd2506d681bf23cd38fff93ce2861ab6ca24886d91e646374c735f"),
        NEWBIE("Willkommenskiste", "§a", -1, 0, "f3d5e43de5d4177c4baf2f44161554473a3b0be5430998b5fcd826af943afe3");

        private final String name, color, texture;
        private final int price, coinbonus;

        ChestType(final String name, final String color, final int price, final int coinbonus, final String texture) {
            this.name = name;
            this.color = color;
            this.texture = "https://textures.minecraft.net/texture/" + texture;
            this.price = price;
            this.coinbonus = coinbonus;
        }

        public String getName() {
            return color + "§l" + name;
        }

        public String getAdjName() {
            if (name.contains(" ") == false) return color + name;
            final String[] split = name.split(" ");
            return color + split[0] + "n " + split[1];
        }

        public String getRawName() {
            return name;
        }

        public String getColor() {
            return color;
        }

        public String getTexture() {
            return texture;
        }

        public int getPrice() {
            return price;
        }

        public int getCoinBonus() {
            return coinbonus;
        }
    }

    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY
    }

    class RarityItem {
        private final Rarity r;
        private ItemStack item;

        public RarityItem(final Rarity r, final ItemStack item) {
            this.item = item;
            this.r = r;
        }

        public void setItem(final ItemStack item) {
            this.item = item;
        }

        public ItemStack getItem() {
            return item;
        }

        public Rarity getRarity() {
            return r;
        }

        public ItemStack getRarityItem() {
            if (r == Rarity.COMMON) {
                return ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE), "§fNormal");
            } else if (r == Rarity.RARE) {
                return ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 3), "§bSelten");
            } else if (r == Rarity.EPIC) {
                return ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 10), "§5Episch");
            } else if (r == Rarity.LEGENDARY) {
                return ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 5), "§aLegendär");
            }
            return null;
        }
    }

    class Roulette {
        private final RandomCollection<RarityItem> items;
        private final RouletteAction raction;

        public Roulette(final RandomCollection<RarityItem> items, final RouletteAction raction) {
            this.items = items;
            this.raction = raction;
        }

        public void startTask(final Player pl, final String title) {
            final Inventory inv = ms.getServer().createInventory(null, 27, title);
            final RouletteSpeed rs = new RouletteSpeed();

            pl.openInventory(inv);
            ms.setActiveInv(pl.getName(), InvType.NOINTERACT);
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.HOPPER), "§6Deine Belohnung:"));
            setItem(inv, 9, items.next());
            setItem(inv, 10, items.next());
            setItem(inv, 11, items.next());
            setItem(inv, 12, items.next());
            setItem(inv, 13, items.next());
            setItem(inv, 14, items.next());
            setItem(inv, 15, items.next());
            setItem(inv, 16, items.next());
            setItem(inv, 17, items.next());
            pl.updateInventory();

            new BukkitRunnable() {
                int step = 0;
                final long startTime = System.currentTimeMillis();

                @Override
                public void run() {
                    final long l = System.currentTimeMillis() - startTime;
                    if (step == 0) {
                        if (l < 6000L) {
                            rs.check();
                            if (rs.should()) {
                                pl.playSound(pl.getLocation(), Sound.WOOD_CLICK, 1.0F, 1.0F);
                                scroll(inv, 17, items.next());
                                pl.updateInventory();
                            }
                        } else if (l >= 6500L) {
                            pl.playSound(pl.getLocation(), Sound.BAT_TAKEOFF, 1.0F, 1.0F);
                            for (int i = 10; i < 17; i++) {
                                if (i != 13) inv.setItem(i, new ItemStack(Material.AIR, 1));
                            }
                            step += 1;
                        }
                    } else if (step == 1) {
                        if (l >= 9000L) {
                            step += 1;
                            ms.getGames().leaveActiveGame(pl); // Leave Games to give reward
                            if (ms.inEvent(pl)) ms.getEvents().quitEvent(pl, false);
                            if (ms.inWar(pl)) ms.getClanWar().handleDeath(pl, false);
                            final MuxPvP.Arena duel = ms.getPvP().getArena(pl);
                            final MuxPvP.RankedArena ranked = ms.getPvP().get1vs1Arena(pl);
                            newbiechestopens.remove(pl.getName());
                            if (duel != null) duel.clearArena(false, true, true);
                            if (ranked != null) ranked.clearArena(false, true, true, false);
                            raction.call(inv.getItem(13));
                            pl.closeInventory();

                            pl.playSound(pl.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
                        } else {
                            glassRainbow(inv, false);
                        }
                    }
                    if (step >= 2) {
                        cancel();
                    }
                }
            }.runTaskTimer(ms, 1L, 1L);
        }

        private void setItem(final Inventory inv, final int pos, final RarityItem r) {
            inv.setItem(pos, r.getItem());
            final ItemStack ritem = r.getRarityItem();
            if (pos != 13) inv.setItem(pos - 9, ritem);
            inv.setItem(pos + 9, ritem);
        }

        private void scroll(final Inventory inv, final int pos, final RarityItem r) {
            for (int i = 0; i < 8; i++) {
                if (i == 4) continue;
                else if (i == 3) {
                    inv.setItem(3, inv.getItem(22));
                    continue;
                }
                inv.setItem(i, inv.getItem(i + 1));
            }
            for (int i = 18; i < 26; i++) {
                inv.setItem(i, inv.getItem(i + 1));
            }
            for (int i = 9; i < pos; i++) {
                inv.setItem(i, inv.getItem(i + 1));
            }
            final ItemStack ritem = r.getRarityItem();
            inv.setItem(8, ritem);
            inv.setItem(26, ritem);
            inv.setItem(pos, r.getItem());
        }

        private void glassRainbow(final Inventory inv, final boolean allrandom) {
            final RandomCollection<Integer> randoms = new RandomCollection<>();
            randoms.add(50.0D, 2);
            randoms.add(50.0D, 4);
            randoms.add(50.0D, 5);
            randoms.add(50.0D, 1);
            randoms.add(50.0D, 3);
            randoms.add(50.0D, 6);
            final int i = randoms.next();
            for (int j = 0; j < inv.getSize(); j++) {
                if (j != 10 && j != 11 && j != 12 && j != 13 && j != 14 && j != 15 && j != 16 && j != 4) {
                    inv.setItem(j, ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, allrandom ? (byte) (int) randoms.next() : (byte) i), " "));
                }
            }
        }

        class RouletteSpeed {
            private int rspeed, sb, loops, maxloops;
            private final HashMap<Integer, Integer> speeds;

            public RouletteSpeed() {
                rspeed = 1;
                sb = 0;
                loops = 0;
                maxloops = 100;
                speeds = new HashMap<>();
                speeds.put(0, 1);

                final double d1 = 1.3D, d2 = 12.3D;
                maxloops = (int) (maxloops * d1);
                for (int i = 1; i <= 40; i++) {
                    int j = (int) (i + r.nextInt(i) / 1.5D);
                    final int k = (int) (j * d1);
                    if (i == 20) {
                        j = (int) (j * 0.85D);
                    } else if (i == 30) {
                        j = (int) (j * 0.65D);
                    } else if (i == 40) {
                        j = (int) (j * 0.45D);
                    }
                    speeds.put((int) (j * d2), k);
                }
            }

            public boolean should() {
                loops += 1;
                sb += 1;
                if (speeds.get(loops) != null) {
                    rspeed = speeds.get(loops);
                }
                if (sb >= rspeed) {
                    sb = 0;
                    return true;
                }
                return false;
            }

            public void check() {
                if (loops >= maxloops) {
                    rspeed += 2;
                }
            }
        }
    }

    class RandomCollection<E> {
        private final NavigableMap<Double, E> map = new TreeMap<>();
        private final Random random;
        private double total = 0.0D;

        public RandomCollection() {
            random = new Random();
        }

        public void add(final double paramDouble, final E paramE) {
            if (paramDouble <= 0.0D) {
                return;
            }
            total += paramDouble;
            map.put(total, paramE);
        }

        public E next() {
            final double d = random.nextDouble() * total;
            return map.ceilingEntry(d).getValue();
        }

        public int size() {
            return map.size();
        }
    }
}