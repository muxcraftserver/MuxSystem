package me.muxteam.base;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MuxGriefing {
    private final MuxSystem ms;
    private final MuxBase muxBase;

    private final Inventory griefInventory;
    private int baseToRaid = 1;

    public MuxGriefing(final MuxSystem ms, final MuxBase muxBase) {
        this.ms = ms;
        this.muxBase = muxBase;
        griefInventory = ms.getServer().createInventory(null, 45, "§0§lMuxBasen§0 | /griefen");
        griefInventory.setItem(1, ms.addGlow(ms.renameItemStack(new ItemStack(Material.EXPLOSIVE_MINECART),
                "§3§lGriefbare Basen",
                "§7Die Liste enthält alle Basen, die aufgrund",
                "§7von Inaktivität temporär verfügbar sind.")));
        griefInventory.setItem(4, ms.renameItemStack(new ItemStack(Material.SPECKLED_MELON),
                "§3§lTop Basen",
                "§7Sortiert nach dem Gesamtwert befinden",
                "§7sich hier die derzeit teuersten Basen.",
                "",
                "§7Klicke, um die §3Top Basen zu sehen§7."));
        griefInventory.setItem(7, ms.renameItemStack(new ItemStack(Material.COMPASS),
                "§3§lBase suchen",
                "§7Besuche mit dieser Funktion die",
                "§7Base eines beliebigen Spielers.",
                "",
                "§7Klicke, um eine §3Base zu suchen§7."));
        startAnnounceTask();
        initGriefId();
    }

    public void updateGriefInventory() {
        int slot = 18;
        final ItemStack air = new ItemStack(Material.AIR);
        for (int i = 18; i < griefInventory.getSize(); i++)
            griefInventory.setItem(i, air);
        for (final PlayerBase base : ImmutableMap.copyOf(muxBase.getPlayerBases()).values().stream()
                .filter(PlayerBase::isGriefable)
                .sorted(Comparator.comparingInt(PlayerBase::getGriefId))
                .collect(Collectors.toList())) {
            if (slot == griefInventory.getSize())
                break;
            final long timeLeft = base.getGrief() - System.currentTimeMillis();
            final String timeColor = timeLeft < 300000 ? "§4" : (timeLeft < 600000 ? "§c" : (timeLeft < 3600000 ? "§6" : "§e")),
                    timeString = "noch " + ms.getTime((int) (timeLeft / 1000));
            final ItemStack skull = ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 1, (short) 4),
                    "§3§lInaktive Base §3#§l" + base.getGriefId(), base.getGrief() == -1 ?
                            new String[]{
                                    "§7Zugang: " + "§anoch unbenutzt", "",
                                    "§7Klicke, um diese Base als", "§7erster zu §3griefen§7."
                            } : new String[]{
                            "§7Zugang: " + timeColor + timeString, "",
                            "§7Klicke, um die Base noch", "§7rechtzeitig zu §3griefen§7."
                    });
            griefInventory.setItem(slot, skull);
            slot++;
        }
    }

    public Inventory getGriefInventory() {
        return griefInventory;
    }

    private void startAnnounceTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final long toRaid = ImmutableList.copyOf(muxBase.getPlayerBases().values()).parallelStream().filter(PlayerBase::isGriefable).count();
                if (toRaid >= 1) {
                    final String toRaidStr = toRaid == 1 ? "ist §ceine Base§7" : "sind §c" + toRaid + " Basen§7";
                    for (final Player p : ms.getServer().getOnlinePlayers()) {
                        ms.chatClickHoverRun(p, "§3§lMuxBase>§7 Es " + toRaidStr + " zum §cGriefen §7verfügbar.", "§3§oKlicke zum Öffnen", "/griefen");
                        ms.chatClickHoverRun(p, "§3§lMuxBase>§7 Klicke §chier§7, um diese zu sehen.", "§3§oKlicke zum Öffnen", "/griefen");
                    }
                }
            }
        }.runTaskTimerAsynchronously(ms, 20L * 60L * 30L, 20L * 60L * 30L);
    }

    protected void handleRaids() {
        ms.getTimings().ofAndStart("baseFullHandleRaids");
        final boolean whitelist = ms.getServer().hasWhitelist();
        final Set<String> defaultBlocks = muxBase.getBlockRegistry().DEFAULT_BLOCKS;
        for (final PlayerBase base : muxBase.getPlayerBases().values()) {
            final OfflinePlayer player = ms.getServer().getOfflinePlayer(base.getOwner());
            if (whitelist == false && base.isGriefable() == false && base.getGrief() == -1 && base.isGriefed() == false && player.isOnline() == false) {
                if (base.getLastPlayed() <= System.currentTimeMillis() - 259200000L) { /* <-- 3 Tage */
                    //    base.getLastPlayed() <= System.currentTimeMillis() - 120000 /* <-- 2 Minuten */
                            /* Da der Spieler die gleiche Base wie beim letztem Mal hat, hat er nichts verändert
                            & es gibt wahrscheinlich nichts zu holen also skippen wir */
                    if (base.getBlocksLR().equals(base.getBlocks())) continue;
                    /* Base sollte nicht bebaut sein, falls doch sind aber nur die DEFAULT BLÖCKE VORHANDEN */
                    if (defaultBlocks.containsAll(base.getBlocks().keySet())) continue;
                    // base.setGrief(System.currentTimeMillis() + 3600000);
                    base.setGriefable(true);
                    ms.getAnalytics().getAnalytics().incrementGriefed();
                    base.setGriefId(baseToRaid);
                    baseToRaid++;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            updateGriefInventory();
                            ms.getForkJoinPool().execute(() -> ms.getDB().removeBlocksWithin(muxBase.getWorld().getName(), base.getMinPos().getBlockX(), base.getMaxPos().getBlockX(), base.getMinPos().getBlockZ(), base.getMaxPos().getBlockZ()));
                            base.setGriefVillager();
                            ms.getDB().saveBase(base);
                        }
                    }.runTask(ms);
                }
            } else if (base.isGriefable() && base.getGrief() != -1 && base.getGrief() < System.currentTimeMillis()) {
                base.setGriefed(true);
                base.setGrief(-1);
                base.setGriefable(false);
                ms.getChat().sendOfflineMessage(player, "MuxBase", "§7Aufgrund von Inaktivität wurde §7dir eine §aneue Chance§7 §7geboten.", true);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        final Map<Player, PlayerBase> temp = ImmutableMap.copyOf(muxBase.getCurrentLocations());
                        temp.entrySet().stream().filter(entry -> entry.getValue() == base)
                                .filter(entry -> base.isTrusted(entry.getKey()) == false).forEach(entry -> {
                                    ms.forcePlayer(entry.getKey(), ms.getGameSpawn());
                                    ms.setLastLocation(entry.getKey(), ms.getGameSpawn());
                                    ms.sendNormalTitle(entry.getKey(), "§cDie Griefzeit ist abgelaufen.", 20, 20, 20);
                                });
                        temp.entrySet().stream().filter(entry -> entry.getValue() == base)
                                .map(Map.Entry::getKey).parallel().forEach(player -> {
                                    ms.sendNormalTitle(player, "§cDie Griefzeit ist abgelaufen.", 20, 20, 20);
                                    ms.sendSubTitle(player, "§fDie Base wird nun zurückgesetzt..", 20, 20, 20);
                                });
                        base.reset(() -> {
                            base.loadChunks(); // TODO maybe should be instant here
                            ms.getForkJoinPool().execute(() -> {
                                base.checkValue();
                                ms.getDB().saveBase(base);
                                base.getBlocksLR().clear();
                                base.getBlocksLR().putAll(base.getBlocks());
                            });
                        });
                        updateGriefInventory(); // remove base instant update
                    }
                }.runTask(ms);
            }
        }
        ms.getTimings().stop("baseFullHandleRaids");
    }

    protected void initGriefId() {
        muxBase.getPlayerBases().values().forEach(base -> {
            if (base.isGriefable()) {
                base.setGriefId(baseToRaid);
                baseToRaid++;
            }
        });
    }

    public PlayerBase getBaseByGriefId(int value) {
        for (final PlayerBase base : muxBase.getPlayerBases().values()) {
            if (base.getGriefId() == value) return base;
        }
        return null;
    }
}