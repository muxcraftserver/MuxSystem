package me.muxteam.basic;

import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.ranks.MuxRanks.PermissionsGroup;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class MuxAntiBot implements Listener {
    private final Random r = new Random();
    private final Map<Player, List<MuxLocation>> enchantlocs = new HashMap<>(), saplinglocs = new HashMap<>();
    private final Map<UUID, AutoEnchantTracker> enchanttrack = new HashMap<>();
    private final ConcurrentHashMap<Player, Integer> joincheck = new ConcurrentHashMap<>(), enchantcheck = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> lastchat = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> ipcache = new ConcurrentHashMap<>();
    private final Set<UUID> bots = new HashSet<>();
    private final Map<UUID, MineData> minedata = new HashMap<>();
    private final List<Material> orelist = Arrays.asList(Material.GOLD_ORE, Material.DIAMOND_ORE, Material.IRON_ORE, Material.EMERALD_ORE, Material.LEAVES, Material.LEAVES_2);
    private MuxSystem ms;

    public MuxAntiBot(final MuxSystem ms) {
        this.ms = ms;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final Player pl : joincheck.keySet()) {
                    if (pl.getName() == null || ms.getMuxUser(pl.getName()) == null) {
                        joincheck.remove(pl);
                        continue;
                    }
                    if (ms.getMuxUser(pl.getName()).getLoginTimestamp() + 15000L < System.currentTimeMillis()) {
                        pl.kickPlayer(ms.getLang("antibot.clickbook"));
                        continue;
                    }
                    openBookCheck(pl);
                }
                for (final Player pl : enchantcheck.keySet()) {
                    if (pl.getName() == null || ms.getMuxUser(pl.getName()) == null) {
                        enchantcheck.remove(pl);
                        continue;
                    }
                    openEnchantBookCheck(pl);
                }
            }
        }.runTaskTimer(ms, 100L, 60L);
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void close() {
        this.ms = null;
    }

    public boolean checkBot(final Player p, final int accs, final String ip) {
        if (ms.isVPN(ip) != 0 || accs > 2) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    joincheck.put(p, r.nextInt(300000) + 1);
                    for (final Player pl : ms.getServer().getOnlinePlayers()) {
                        if (p != pl) pl.hidePlayer(p);
                    }
                    ms.getMuxUser(p.getName()).setTrophies(0);
                    openBookCheck(p);
                }
            }.runTask(ms);
            return true;
        } else if (joincheck.isEmpty() == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (final Player bot : joincheck.keySet()) {
                        if (p != bot) p.hidePlayer(bot);
                    }
                }
            }.runTask(ms);
        }
        return false;
    }

    public boolean didntPassCheck(final Player pl) {
        return joincheck.containsKey(pl);
    }

    public boolean isBot(final Player pl, final int accs) {
        if (bots.contains(pl.getUniqueId())) return true;
        final PermissionsGroup group = ms.getPerms().getGroupOf(pl.getUniqueId());
        if (accs > 2 && group == null) { // Don't affect players with ranks
            bots.add(pl.getUniqueId());
            return true;
        }
        return false;
    }

    public int isVPN(final String ip) {
        if (ipcache.containsKey(ip)) {
            return ipcache.get(ip) ? 1 : 0;
        }
        int result;
        try (final InputStream in = new URL("https://www.stopforumspam.com/api?ip=" + ip).openStream();
             final InputStreamReader instr = new InputStreamReader(in, StandardCharsets.UTF_8); final BufferedReader br = new BufferedReader(instr)) {
            String str;
            final StringBuilder confidence = new StringBuilder();
            while ((str = br.readLine()) != null) {
                confidence.append(str);
            }
            final boolean vpn = confidence.toString().contains("<appears>yes</appears>");
            ipcache.put(ip, vpn);
            result = vpn ? 1 : 0;
        } catch (final Exception ex) {
            ex.printStackTrace();
            result = 2;
        }
        if (result != 1) {
            try (final InputStream in = new URL("https://blackbox.ipinfo.app/lookup/" + ip).openStream();
                 final InputStreamReader instr = new InputStreamReader(in, StandardCharsets.UTF_8); final BufferedReader br = new BufferedReader(instr)) {
                final String line = br.readLine();
                if (line == null) {
                    return 2;
                }
                final boolean vpn = line.equals("Y");
                ipcache.put(ip, vpn);
                return vpn ? 1 : 0;
            } catch (final Exception ex) {
                ex.printStackTrace();
                return 2;
            }
        }
        return result;
    }

    @EventHandler
    public void onChat(final AsyncPlayerChatEvent e) {
        if (joincheck.containsKey(e.getPlayer())) {
            e.setCancelled(true);
            openBookCheck(e.getPlayer());
        }
    }

    @EventHandler
    public void onDrop(final PlayerDropItemEvent e) {
        if (joincheck.containsKey(e.getPlayer())) {
            e.setCancelled(true);
            openBookCheck(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent e) {
        if (joincheck.containsKey(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(final PlayerQuitEvent e) {
        if (joincheck.containsKey(e.getPlayer())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    final File file = new File(ms.getFarmWorld().getWorldFolder() + "/playerdata/" + e.getPlayer().getUniqueId().toString() + ".dat");
                    if (file.delete() == false) System.err.println("MuxSystem> Could not delete player data of " + e.getPlayer().getName());
                }
            }.runTaskLater(ms, 2L);
        }
        joincheck.remove(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(final PlayerCommandPreprocessEvent e) {
        int id = joincheck.getOrDefault(e.getPlayer(), 0);
        if (id != 0) {
            if (e.getMessage().contains(String.valueOf(id)) == false) {
                e.setCancelled(true);
                openBookCheck(e.getPlayer());
                return;
            }
            final Player p = e.getPlayer();
            joincheck.remove(p);
            for (final Player pl : ms.getServer().getOnlinePlayers()) {
                if (p != pl) pl.showPlayer(p);
            }
            if (ms.isDebug() == false) ms.getNewbies().handleNewbieJoin(p);
            e.setCancelled(true);
            return;
        }
        id = enchantcheck.getOrDefault(e.getPlayer(), 0);
        if (id != 0) {
            if (e.getMessage().contains(String.valueOf(id)) == false) {
                e.setCancelled(true);
                openEnchantBookCheck(e.getPlayer());
                return;
            }
            e.setCancelled(true);
            enchantcheck.remove(e.getPlayer());
        }
    }

    private void openBookCheck(final Player p) {
        final int id = joincheck.getOrDefault(p, 0);
        if (id == 0) return;
        final ItemStack book = ms.cmdBook(ms.getLang("join.welcome"), ms.getLang("join.start"), new Object[]{"/bots " + id, ms.getLang("join.click"), ms.getLang("join.click")});
        ms.openBook(book, p);
    }

    // ANTI SPAM BOT

    public void clearLastChat() {
        lastchat.clear();
    }

    public boolean checkBotSpam(final String pname, final UUID uuid, String lowercase) {
        if (ms.getPerms().hasGroup(uuid)) return false;
        lowercase = lowercase.replaceAll("[0-9]", "");
        final List<String> chatbots = new ArrayList<>();
        for (final Map.Entry<String, String> entry : lastchat.entrySet()) {
            final Player pl = ms.getServer().getPlayer(entry.getKey());
            if (pl == null) continue;
            final MuxUser u = ms.getMuxUser(pl.getName());
            final boolean bot = (pl.getLocation().getPitch() == 0F || pl.getEnderChest().firstEmpty() == 0 && u.getKills() == 0 && ms.hasVoted(pl) == false);
            if (bot && ms.getChat().isSimilar(entry.getValue(), lowercase)) {
                chatbots.add(entry.getKey());
            }
        }
        lastchat.put(pname, lowercase);
        if (chatbots.size() > 2 && ms.getEvents().isChatEvent() == false) {
            chatbots.forEach(botname -> ms.syncCMD("punish " + botname + " Bot"));
            if (chatbots.size() == 3) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (final Player pl : ms.getServer().getOnlinePlayers()) {
                            if (pl.hasPermission("muxsystem.clearchat") == false) {
                                ms.clearChat(pl);
                            }
                        }
                        ms.broadcastMessage(ms.getLang("chat.autoclear"), null, MuxSystem.Priority.HIGH);
                    }
                }.runTaskLater(ms, 2L);
            }
            return true;
        }
        return false;
    }

    // ANTI ENCHANT BOT

    private void openEnchantBookCheck(final Player p) {
        final int id = enchantcheck.getOrDefault(p, 0);
        if (id == 0) return;
        final ItemStack book = ms.cmdBook("Enchanting", ms.getLang("enchant.clickcontinue"), new Object[]{"/bots " + id, ms.getLang("enchant.click"), ms.getLang("enchant.click")});
        ms.openBook(book, p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(final PlayerInteractEvent e) {
        final Block clicked = e.getClickedBlock();
        final boolean saplingplace = (e.getItem() != null && e.getItem().getType() == Material.SAPLING);
        if ((clicked != null && clicked.getType() == Material.SAPLING) || saplingplace) {
            if (e.isCancelled()) return;
            final Player p = e.getPlayer();
            final List<MuxLocation> locs = saplinglocs.getOrDefault(p, new ArrayList<>());
            final MuxLocation l = new MuxLocation(p.getLocation());
            if (locs.contains(l)) {
                e.setCancelled(true);
                e.setUseInteractedBlock(Result.DENY);
                ms.showItemBar(p, "§cBewege dich, um " + (saplingplace ? "den Setzling zu platzieren." : "das Knochenmehl wieder zu verwenden."));
                return;
            }
            locs.add(l);
            saplinglocs.put(p, locs);
        } else if (clicked != null && clicked.getType() == Material.ENCHANTMENT_TABLE) {
            if (e.isCancelled()) return;
            final Player p = e.getPlayer();
            if (ms.checkGeneralCooldown(p.getName(), "ENCHANT", 10000L, false)) {
                ms.showItemBar(p, ms.getLang("enchant.trylater"));
                e.setCancelled(true);
                e.setUseInteractedBlock(Result.DENY);
                return;
            }
            final List<MuxLocation> locs = enchantlocs.getOrDefault(p, new ArrayList<>());
            final MuxLocation l = new MuxLocation(p.getLocation());
            if (locs.contains(l)) {
                e.setCancelled(true);
                e.setUseInteractedBlock(Result.DENY);
                ms.showItemBar(p, ms.getLang("enchant.movetouse"));
                return;
            }
            locs.add(l);
            enchantlocs.put(p, locs);
            final AutoEnchantTracker tracker = enchanttrack.getOrDefault(p.getUniqueId(), new AutoEnchantTracker());
            if (tracker.current == 0) {
                tracker.onEnchant();
                enchanttrack.put(p.getUniqueId(), tracker);
                return;
            }
            final int result = tracker.onEnchant();
            if (result != -1 && result != 0) { // player is most likely using auto enchant (captcha)
                e.setCancelled(true);
                e.setUseInteractedBlock(Result.DENY);
                ms.checkGeneralCooldown(p.getName(), "ENCHANT", 10000L, true);
                enchantcheck.put(p, r.nextInt(300000) + 1);
                openEnchantBookCheck(p);
            }
        } else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            final AutoEnchantTracker tracker = enchanttrack.get(e.getPlayer().getUniqueId());
            if (tracker != null) tracker.addHit();
        }
    }

    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player == false) return;
        final Player p = (Player) e.getDamager();
        if (p.getOpenInventory().getType() == InventoryType.ENCHANTING) {
            p.getOpenInventory().close();
        }
    }

    // ANTI MINING BOT

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        final Material m = e.getBlock().getType();
        if (orelist.contains(m)) {
            final Player p = e.getPlayer();
            MineData mineData = this.minedata.get(p.getUniqueId());
            if (mineData == null) {
                this.minedata.put(p.getUniqueId(), mineData = new MineData());
            }
            if (mineData.flagged || mineData.check(p) == false) {
                e.setCancelled(true);
            }
        }
    }

    public boolean isMineBanned(final UUID uuid) {
        final MineData data = minedata.get(uuid);
        if (data == null) return false;
        return data.banned;
    }

    public void removeMineBan(final OfflinePlayer ply) {
        minedata.remove(ply.getUniqueId());
    }

    private final short[] nonredcolors = {1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 15},
            nonorangecolors = {2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15}, nonyellowcolors = {1, 2, 3, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private final Material[] materials = {Material.WOOL, Material.STAINED_CLAY, Material.STAINED_GLASS};

    private void openMineCaptchaInventory(final Player p) {
        final List<String> itemIDs = ms.getShop().getItemIDs().stream().filter(s -> s.endsWith(":0")).collect(Collectors.toList());
        ItemStack itemToClick;
        String invtitle;
        int size = itemIDs.size();
        final int type = r.nextInt(4), slot = r.nextInt(27);
        final boolean b = r.nextInt(10) == 2;
        if (type == 0) {
            String fullid = itemIDs.get(r.nextInt(size));
            itemIDs.remove(fullid);
            size--;
            itemToClick = new ItemStack(Integer.parseInt(fullid.replace(":0", "")), 1);
            invtitle = String.format("§0Klicke auf '%s'", ms.getShop().getItemName(fullid));
        } else if (type == 1) {
            itemToClick = b ? new ItemStack(Material.REDSTONE_BLOCK) : new ItemStack(materials[r.nextInt(materials.length)], 1, (short) 14);
            invtitle = "§0Klicke auf das rote Item";
        } else if (type == 2) {
            itemToClick = b ? new ItemStack(Material.SADDLE) : new ItemStack(materials[r.nextInt(materials.length)], 1, (short) 1);
            invtitle = "§0Klicke auf das orangene Item";
        } else {
            itemToClick = b ? new ItemStack(Material.DOUBLE_PLANT) : new ItemStack(materials[r.nextInt(materials.length)], 1, (short) 4);
            invtitle = "§0Klicke auf das gelbe Item";
        }
        final Inventory inv = ms.getServer().createInventory(null, 27, invtitle);
        inv.setItem(slot, itemToClick);
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != slot) {
                if (type == 0) inv.setItem(i, new ItemStack(Integer.parseInt(itemIDs.get(r.nextInt(size)).replace(":0", "")), 1));
                else if (type == 1) inv.setItem(i, new ItemStack(materials[r.nextInt(materials.length)], 1, nonredcolors[r.nextInt(nonredcolors.length)]));
                else if (type == 2) inv.setItem(i, new ItemStack(materials[r.nextInt(materials.length)], 1, nonorangecolors[r.nextInt(nonorangecolors.length)]));
                else inv.setItem(i, new ItemStack(materials[r.nextInt(materials.length)], 1, nonyellowcolors[r.nextInt(nonyellowcolors.length)]));
            }
        }
        p.closeInventory();
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
        final MineData mineData = this.minedata.get(p.getUniqueId());
        mineData.captchaSlot = slot;
        p.openInventory(inv);
        mineData.canCloseCaptcha = false;
        ms.setActiveInv(p.getName(), MuxInventory.InvType.MINECAPTCHA);
    }
    public void handleMineCaptchaInv(final Player p, final int slot) {
        final MineData data = this.minedata.get(p.getUniqueId());
        if (data.captchaSlot == slot) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            data.canCloseCaptcha = true;
            data.flagged = false;
            data.check = -1;
            data.current = 0;
            data.captchaTries = 0;
            p.closeInventory();
            ms.showItemBar(p, ms.getLang("mine.canmine"));
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            data.captchaTries++;
            if (data.captchaTries == 4) {
                data.banned = true;
                data.canCloseCaptcha = true;
                data.flagged = false;
                ms.getDB().addBanEntry(p.getUniqueId(), p.getName(), "MineBan", "", "", "CONSOLE");
                p.closeInventory();
                ms.showItemBar(p, ms.getLang("mine.banned"));
            } else {
                ms.showItemBar(p, ms.getLang("mine.clickcorrect"));
            }
        }
    }
    public void handleMineCaptchaClose(final Player p, final Inventory inv) {
        final me.muxteam.basic.MuxAntiBot.MineData data = minedata.get(p.getUniqueId());
        if (data.canCloseCaptcha) return;
        ms.runLater(new BukkitRunnable() {
            @Override
            public void run() {
                data.captchaTries++;
                if (data.captchaTries == 4) {
                    data.banned = true;
                    data.canCloseCaptcha = true;
                    data.flagged = false;
                    ms.getDB().addBanEntry(p.getUniqueId(), p.getName(), "MineBan", "", "", "CONSOLE");
                    p.closeInventory();
                    ms.showItemBar(p, ms.getLang("mine.banned"));
                    return;
                }
                p.openInventory(inv);
                ms.setActiveInv(p.getName(), MuxInventory.InvType.MINECAPTCHA);
            }
        }, 1L);
    }

    class MineData {
        private boolean flagged, banned, canCloseCaptcha;
        private int current, check = -1, captchaSlot;
        private short captchaTries;

        public boolean check(final Player p) {
            if (banned) {
                ms.showItemBar(p, ms.getLang("mine.arebanned"));
                return false;
            }
            if (flagged) return false;
            if (this.check == -1) {
                this.check = ThreadLocalRandom.current().nextInt(100, 300 + 1);
            }
            current++;
            if (current == check) {
                flagged = true;
                openMineCaptchaInventory(p);
                return false;
            }
            return true;
        }
    }
    class AutoEnchantTracker {
        private int current = 0, check = 5;
        private final List<AtomicInteger> hits = new ArrayList<>();

        private int calculateHitPattern() {
            final List<AtomicInteger> sameHits = new ArrayList<>(), otherHits = new ArrayList<>();
            for (final AtomicInteger integer : hits) {
                boolean b = false;
                for (final AtomicInteger other : hits) {
                    if (integer.equals(other)) continue;
                    if (integer.get() == other.get()) {
                        sameHits.add(integer);
                        b = true;
                        break;
                    }
                }
                if (b == false) otherHits.add(integer);
            }
            if (sameHits.stream().filter(atomicInteger -> atomicInteger.get() == sameHits.get(0).get()).count() == sameHits.size() && sameHits.size() > otherHits.size()) {
                return 2;
            }
            return sameHits.size() > otherHits.size() ? 1 : 0;
        }

        public int onEnchant() {
            this.hits.add(hits.size(), new AtomicInteger(0));
            if (current == check) {
                int calculated = calculateHitPattern();
                resetHits();
                resetCurrent();
                return calculated;
            }
            increaseCurrent();
            return -1;
        }

        public void addHit() {
            if (this.hits.isEmpty())
                return;
            if (this.hits.size() < current)
                this.current = this.hits.size();
            int pos = current - 1;
            this.hits.get(pos).incrementAndGet();
        }

        private void resetHits() {
            this.hits.clear();
        }

        private void resetCurrent() {
            this.current = 0;
            this.check = r.nextInt(9) + 1;
            while (check < 2) {
                check = r.nextInt(9) + 1;
            }
        }

        private void increaseCurrent() {
            this.current++;
        }
    }
}