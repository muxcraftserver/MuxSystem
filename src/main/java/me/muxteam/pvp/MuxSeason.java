package me.muxteam.pvp;

import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.TimeZone;

public class MuxSeason {
    private final MuxSystem ms;
    private final MuxHolograms holograms;
    private long seasonEnd, monthEnd;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

    public MuxSeason(final MuxSystem ms) {
        this.ms = ms;
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        this.holograms = ms.getHolograms();
        // MONTH END
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.isLong("monthend")) {
            this.monthEnd = hashYML.getLong("monthend");
            newMonth();
        } else {
            LocalDateTime time = LocalDateTime.of(LocalDate.now(ZoneId.of("CET")).with(TemporalAdjusters.lastDayOfMonth()), LocalTime.MAX);
            long monthEndMillis = time.toInstant(ZoneId.of("CET").getRules().getOffset(time)).toEpochMilli();
            hashYML.set("monthend", monthEndMillis);
            this.monthEnd = monthEndMillis;
        }
        // Season & Month Holograms
        refreshCountdowns();
    }

    public void loadConfig(final FileConfiguration config) {
        seasonEnd = config.get("seasonend") != null ? config.getLong("seasonend") : -1;
    }

    public void refreshHolograms() {
        refreshCountdowns();
        if (getSeasonEnd() != -1 && getSeasonEnd() <= System.currentTimeMillis()) {
            onSeasonEnd();
        }
    }

    public void openSeasonStartInventory(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Season");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.ARMOR_STAND), "§f§lSeason"));
        if (p.hasMetadata("muxseasonend") == false) {
            final LocalDate endDate = LocalDate.now().plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
            p.setMetadata("muxseasonend", new FixedMetadataValue(this.ms, endDate));
        }
        final LocalDate endDate = (LocalDate) p.getMetadata("muxseasonend").get(0).value();
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        if (daysBetween < 0) daysBetween = 0;
        daysBetween *= 86400000;
        final String timeStr = ms.timeToString(daysBetween, false);
        final long millis = System.currentTimeMillis() + (ChronoUnit.DAYS.between(LocalDate.now(), endDate) * 86400000);

        inv.setItem(20, ms.renameItemStack(new ItemStack(Material.WATCH), "§f§l" + sdf.format(new Date(millis)),
                "§7Die Season würde voraussichtlich", "§7in §f" + timeStr + "§7 enden.", "", "§aLinksklick§7, für ein Monat mehr.", "§cRechtsklick§7, für ein Monat weniger."));
        inv.setItem(24, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (short) 5), "§a§lSeason starten"));
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.ADMIN )
            p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.ADMIN);
    }

    public void handleSeasonInventory(final Player p, final Material m, boolean rightclick) {
        switch (m) {
            case ITEM_FRAME:
                p.openInventory(ms.getAdmin().getInfrastructureSettings());
                ms.setActiveInv(p.getName(), MuxInventory.InvType.ADMIN);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                break;
            case WATCH:
                LocalDate endDate = (LocalDate) p.getMetadata("muxseasonend").get(0).value();
                final LocalDate current = LocalDate.now(), newDate;
                if (rightclick) {
                    newDate = endDate.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                    if (newDate.getMonthValue() < current.getMonthValue()) {
                        ms.showItemBar(p, "§cEine Season kann nicht in der Vergangenheit enden.");
                        p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                        return;
                    }
                } else {
                    newDate = endDate.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                    if (newDate.getMonthValue() > current.getMonthValue() + 3) {
                        ms.showItemBar(p, "§cEine Season kann maximal 3 Monate dauern.");
                        p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                        return;
                    }
                }
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.setMetadata("muxseasonend", new FixedMetadataValue(this.ms, newDate));
                openSeasonStartInventory(p);
                break;
            case STAINED_CLAY:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                boolean started = startSeason(p);
                p.openInventory(ms.getAdmin().getInfrastructureSettings());
                ms.setActiveInv(p.getName(), MuxInventory.InvType.ADMIN);
                if (started == false) return;
                ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > Season start", null);
                break;
            case ARMOR_STAND:
            default:
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                break;
        }
    }

    public void resetSeasonStart(final Player p) {
        p.removeMetadata("muxseasonend", this.ms);
    }

    public boolean startSeason(final Player p) {
        if (seasonEnd > System.currentTimeMillis()) {
            p.playSound(p.getLocation(),  Sound.NOTE_STICKS, 1F, 1F);
            return false;
        }
        final LocalDate endDate = (LocalDate) p.getMetadata("muxseasonend").get(0).value();
        long between = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        between *= 86400000;
        seasonEnd = System.currentTimeMillis() + between;
        final FileConfiguration config = ms.getConfig();
        config.set("seasonend", seasonEnd);
        createSeasonHolo();
        ms.saveConfig();
        return true;
    }

    public void stopSeason(final Player p, final boolean accepted) {
        if (accepted == false) {
            new ConfirmInventory(ms, p1 -> {
                stopSeason(p, true);
                p.openInventory(ms.getAdmin().getInfrastructureSettings());
                ms.setActiveInv(p.getName(), MuxInventory.InvType.ADMIN);
            }, p1 -> {
                p.openInventory(ms.getAdmin().getInfrastructureSettings());
                ms.setActiveInv(p.getName(), MuxInventory.InvType.ADMIN);
            }).show(p, "§0§lSeason beenden", "§a§lBestätigen", "§cAbbrechen");
            return;
        }
        this.onSeasonEnd();
        ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > Season end", null);
    }

    public void newMonth() {
        final LocalDateTime time = LocalDateTime.of(LocalDate.now(ZoneId.of("CET")).with(TemporalAdjusters.lastDayOfMonth()), LocalTime.MAX);
        final long monthEndMillis = time.toInstant(ZoneId.of("CET").getRules().getOffset(time)).toEpochMilli();
        // if (System.currentTimeMillis() > this.monthEnd) { // When testing, set monthEnd to current millis + 2min in MuxSeason()
        if (monthEndMillis > monthEnd) {
            ms.getHashYAML().set("monthend", monthEnd = monthEndMillis);
            ms.getBase().checkTop();
            ms.getPvP().checkTop();
        }
    }

    private void refreshCountdowns() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
                if (daysBetween < 0) daysBetween = 0;
                String timeStr = (daysBetween > 1 ? "in §b" + daysBetween + " Tagen" : daysBetween == 0 ? "ist §bHeute" : "ist §bMorgen");
                holograms.removeHologram("monthendbase");
                holograms.removeHologram("monthendpvp");
                holograms.addHologram("monthendbase", new Location(ms.getGameSpawn().getWorld(), 2.5, 77, 11.5), "§b§lMonatsende§r§f " + timeStr);
                holograms.addHologram("monthendpvp", new Location(ms.getGameSpawn().getWorld(), 2.5, 77, -12.5), "§c§lMonatsende§r§f " + timeStr.replace("§b", "§c"));
                createSeasonHolo();
            }
        }.runTask(ms);
    }
    private void createSeasonHolo() {
        if (getSeasonEnd() < System.currentTimeMillis()) return;
        if (holograms.getPluginHolos().containsKey("seasonend") == false) {
            holograms.addHologram("seasonend", new Location(ms.getGameSpawn().getWorld(), -18.5, 75, -37.5), "§b§lSeason Ende");
        }
        if (holograms.getPluginHolos().containsKey("seasonendcountdown")) {
            holograms.removeHologram("seasonendcountdown");
        }
        final Date actualDate = new Date(getSeasonEnd());
        final LocalDate date = Instant.ofEpochMilli(actualDate.toInstant().toEpochMilli()).atZone(ZoneId.systemDefault()).toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (daysBetween < 0) daysBetween = 0;
        final String timeStr = "§f" + (daysBetween > 2 ? "In §b" + daysBetween + " Tagen" : daysBetween == 0 ? "Heute" : "Morgen");
        holograms.addHologram("seasonendcountdown", holograms.getPluginHolos().get("seasonend").getLocation().clone().add(0, -0.4, 0), timeStr);
        holograms.addHologram("seasonenddesc", holograms.getPluginHolos().get("seasonendcountdown").getLocation().clone().add(0, -0.4, 0), "Base, Inventar & Enderkiste wird zurückgesetzt");
        holograms.addHologram("seasonenddesc2", holograms.getPluginHolos().get("seasonenddesc").getLocation().clone().add(0, -0.4, 0), "Ränge, Extras, Gems & MuxCoins - bleiben");
    }
    private void onSeasonEnd() {
        final FileConfiguration config = ms.getConfig();
        this.seasonEnd = -1;
        config.set("seasonend", null);
        ms.saveConfig();
        holograms.removeHologram("seasonend");
        holograms.removeHologram("seasonendcountdown");
    }
    public long getSeasonEnd() {
        return seasonEnd;
    }

    public long getMonthEnd() {
        return monthEnd;
    }
}