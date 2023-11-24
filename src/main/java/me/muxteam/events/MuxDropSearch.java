package me.muxteam.events;

import me.muxteam.basic.NMSReflection;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.extras.MuxChests;
import me.muxteam.holidays.MuxBirthday;
import me.muxteam.holidays.MuxChristmas;
import me.muxteam.holidays.MuxEaster;
import me.muxteam.holidays.MuxHalloween;
import me.muxteam.holidays.MuxHoliday;
import me.muxteam.holidays.MuxSummer;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.shop.MuxMining;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class MuxDropSearch extends Event implements Listener {
    private Location loc;
    private final List<Location> locs = new ArrayList<>();
    private final Map<String, SearchEventUser> evuser = new ConcurrentHashMap<>();
    private final Map<String, Short> cooldown = new HashMap<>();
    private File dropFile;
    private FileConfiguration dropYML;
    private final List<Drop> drops = new ArrayList<>(), coindrops = new ArrayList<>();
    private String firstcolor, secondcolor, thirdcolor, specialchar, announcename;
    private String droppl, dropsg, dropadj, dropadj2;
    private String announcemsg = null;
    private MuxHoliday h;
    private MuxScoreboard.ScoreboardType sb;

    public MuxDropSearch(final MuxEvents e) {
        super(e);
        this.h = ms.getHoliday();
        noJoinMSG = true;
        load();

        final int currentYear = LocalDateTime.now().getYear();

        final LocalDateTime[] ldt = {
                LocalDateTime.of(currentYear, 3, 6, 12, 0, 0),
                LocalDateTime.of(currentYear, 10, 31, 12, 0, 0),
                LocalDateTime.of(currentYear, 12, 26, 12, 0, 0),
                LocalDateTime.of(currentYear, 7, 20, 13, 0, 0)
        };

        final Calendar currCalendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        final List<LocalDateTime> dates = new ArrayList<>(Arrays.asList(ldt));
        // Add Easter Saturday
        final LocalDateTime ld = getEasterSundayDate(currentYear).withHour(12).withMinute(0).withSecond(0);
        dates.add(ld);

        LocalDateTime august = LocalDateTime.of(currentYear, Month.AUGUST, 1, 12, 0);
        while (august.getDayOfWeek() != DayOfWeek.FRIDAY)
            august = august.plusDays(1);
        dates.add(LocalDateTime.of(currentYear, 8, august.getDayOfMonth(), 12, 0));
        LocalDateTime currentDate = LocalDateTime.now();
        if (currentDate.getHour() != currCalendar.get(Calendar.HOUR_OF_DAY)) {
            currentDate = currentDate.withHour(currCalendar.get(Calendar.HOUR_OF_DAY));
        }
        final long current = currentDate.atZone(ZoneId.of("CET")).toInstant().toEpochMilli();
        for (final LocalDateTime date : dates) {
            final long millis = date.atZone(ZoneId.of("CET")).toInstant().toEpochMilli();
            final long remainingMillis = millis - current;
            if (remainingMillis <= 0) {
                if (date.getMonth() == currentDate.getMonth() && date.getDayOfMonth() == currentDate.getDayOfMonth()) {
                    startDropSearch(3);
                    break;
                }
                continue;
            }
            if (remainingMillis <= 288000000) { // 3 Tage 8 Stunden ca
                if (remainingMillis <= 36000000L) { // 10 Stunden (4 Uhr restart)
                    final int seconds = (int) (remainingMillis / 1000L);
                    startDropSearch(seconds);
                }
                if (remainingMillis <= 36000000L) {
                    announcemsg = "%first%§lHeute§f wird das %first%" + announcename + " §fstattfinden!";
                } else if (remainingMillis <= 122400000L) {
                    announcemsg = "%first%§lMorgen§f wird das %first%" + announcename + " §fstattfinden!";
                } else {
                    final Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(millis);
                    announcemsg = "§fAm %first%§l" + date.getDayOfMonth() + ". " + c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMANY) + "§f wird das %first%" + announcename + " §fstattfinden!";
                }
                break;
            }
        }

        if (announcemsg != null) announcemsg = e.ms.getFonts().centerText(announcemsg.replace("%first%", firstcolor).replace("%third%", thirdcolor));
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.isLong("dropsearchdbclear") && hashYML.getLong("dropsearchdbclear") <= System.currentTimeMillis()) {
            hashYML.set("dropsearchdbclear", null);
            ms.saveHashFile();
            ms.getDB().clearSearchEventUsers();
        }
    }

    public String getAnnouncement() {
        return announcemsg;
    }
    public String getAnnounceName() {
        return announcename;
    }

    public void switchHoliday() {
        if (h instanceof MuxBirthday) {
            h = new MuxChristmas(ms, true);
        } else if (h instanceof MuxChristmas) {
            h = new MuxEaster(ms);
        } else if (h instanceof MuxEaster) {
            h = new MuxHalloween(ms, true);
        } else if (h instanceof MuxHalloween) {
            h = new MuxSummer(ms);
        } else if (h instanceof MuxSummer) {
            h = null;
        } else {
            h = new MuxBirthday(ms);
        }
        load();
    }

    private void load() {
        if (h instanceof MuxHalloween) {
            name = "MuxHalloween";
            announcename = "Halloweenevent";
            item = new ItemStack(Material.PUMPKIN);
            firstcolor = "§4";
            secondcolor = "§5";
            thirdcolor = "§4";
            specialchar = "✟";
            droppl = "Süßigkeiten";
            dropsg = "Süßigkeit";
            dropadj = "die";
            dropadj2 = "eine";
        } else if (h instanceof MuxChristmas) {
            name = "MuxWeihnachten";
            announcename = "Weihnachtsevent";
            item = new ItemStack(Material.SNOW_BALL);
            firstcolor = "§c";
            secondcolor = "§6";
            thirdcolor = "§f";
            specialchar = "❄";
            droppl = "Kekse";
            dropsg = "Keks";
            dropadj = "den";
            dropadj2 = "ein";
        } else if (h instanceof MuxEaster) {
            name = "MuxOstern";
            announcename = "Osterevent";
            item = new ItemStack(Material.EGG);
            firstcolor = "§6";
            secondcolor = "§2";
            thirdcolor = "§6";
            specialchar = "❀";
            droppl = "Ostereier";
            dropsg = "Osterei";
            dropadj = "das";
            dropadj2 = "ein";
        } else if (h instanceof MuxSummer) {
            name = "MuxSommer";
            announcename = "Sommerevent";
            item = new ItemStack(Material.WATER_BUCKET);
            firstcolor = "§e";
            secondcolor = "§9";
            thirdcolor = "§e";
            specialchar = "❂";
            droppl = "Getränke";
            dropsg = "Getränk";
            dropadj = "das";
            dropadj2 = "ein";
        } else if (h instanceof MuxBirthday) {
            name = "MuxBirthday";
            announcename = "MuxGeburtstagsevent";
            item = new ItemStack(Material.CAKE);
            firstcolor = "§d";
            secondcolor = "§6";
            thirdcolor = "§d";
            specialchar = "❤";
            droppl = "Kuchen";
            dropsg = "Kuchen";
            dropadj = "den";
            dropadj2 = "ein";
        } else {
            name = "MuxSuche";
            announcename = "Suchevent";
            item = new ItemStack(Material.DIAMOND);
            firstcolor = "§d";
            secondcolor = "§b";
            thirdcolor = "§d";
            specialchar = "✪";
            droppl = "Diamanten";
            dropsg = "Diamant";
            dropadj = "den";
            dropadj2 = "ein";
        }
        final String sbtitle = name.replace("Mux", "§7§lMux" + firstcolor + "§l");
        sb = ms.getScoreboard().createScoreboard(sbtitle);
        sb.setLook((p, board) -> {
            sb.setLine(board, "  ");
            final SearchEventUser u = evuser.get(p.getName());
            final int score = u.getScore(), coins = u.getCoinScore();
            sb.setSection(board, secondcolor + "§l" + droppl + " ", score == 0 ? "Noch keine" : String.valueOf(score), true);
            sb.setLine(board, "   ");
            sb.setSection(board, "§e§lBonuscoins  ", coins == 0 ? "Noch keine " : (coins * 500 + " "), true);
        });
    }

    private void startDropSearch(int seconds) {
        final MuxDropSearch instance = this;
        long targetTime = System.currentTimeMillis() + (seconds * 1000L);
        new BukkitRunnable() {
            @Override public void run() {
                if (System.currentTimeMillis() >= targetTime) {
                    if (ms.getEvents().getCurrent() == null) {
                        ms.getEvents().startEvent(null, instance);
                    } else
                        startDropSearch(10);
                    cancel();
                }
            }
        }.runTaskTimer(ms, 0L, 20L);
    }
    private void death(final Player pl) {
        ms.forcePlayer(pl, getRandomSpawn());
    }

    private Location getRandomSpawn() {
        return locs.get(r.nextInt(locs.size()));
    }

    private void dropMenu(final Player p, final String cmd) {
        p.sendMessage(new String[]{
                ms.header((byte) 11, secondcolor),
                "  " + firstcolor + specialchar + " §c/" + cmd + " create [ID]: §7Drop erstellen",
                "  " + firstcolor + specialchar + " §c/" + cmd + " remove [ID]: §7Drop entfernen",
                "  " + firstcolor + specialchar + " §c/" + cmd + " tp [ID]: §7Zum Drop teleportieren",
                ms.footer((byte) 11, secondcolor)});
    }

    private LocalDateTime getEasterSundayDate(final int year) {
        final int a = year % 19,
                b = year / 100,
                c = year % 100,
                d = b / 4,
                e = b % 4,
                g = (8 * b + 13) / 25,
                i = (19 * a + b - d - g + 15) % 30,
                j = c / 4,
                k = c % 4,
                m = (a + 11 * i) / 319,
                r = (2 * e + 2 * j - k - i + m + 32) % 7,
                month = (i - m + r + 90) / 25,
                day = (i - m + r + month + 19) % 32;
        return LocalDateTime.of(year, month, day, 0, 0);
    }

    @Override
    public void stop() {
        locs.clear();
        drops.clear();
        coindrops.clear();
        ms.getHashYAML().set("dropsearchdbclear", System.currentTimeMillis() + 259200000L);
        ms.saveHashFile();
        loc.getWorld().getPlayers().forEach(player -> ms.forcePlayer(player, ms.getGameSpawn()));
        ms.getServer().unloadWorld(loc.getWorld(), true);
    }

    @Override
    public String[] getDescription() {
        final String mapword = droppl.length() < 7 ? "dieser Karte" : droppl.length() < 11 ? "dieser Map" : "der Map";
        return new String[]{
                "§7Finde auf " + mapword + " alle " + secondcolor + "50 " + droppl + "§7,",
                "§7die versteckt wurden. Es können auch",
                "§7als Bonus §eMuxCoins §7gefunden werden.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public String[] getAdminInformation() {
        return new String[]{
                "§7Ferien: " + (h == null ? "§dKeins" : "§d" + name),
                "§7Setup Befehle: §d/drop, /coins"
        };
    }

    @Override
    public long getUpdateTime() {
        return 5L;
    }

    @Override
    public void update() {
        for (final Player pl : players) {
            final Material m = pl.getLocation().getBlock().getType();
            if (m == Material.LAVA || m == Material.WATER || m == Material.STATIONARY_LAVA || m == Material.STATIONARY_WATER) {
                death(pl);
            }
        }
    }

    @Override
    public void start() {
        final World w = ms.getServer().createWorld(new WorldCreator(h != null ? h.getClass().getSimpleName() : "MuxSearch").generateStructures(false).type(org.bukkit.WorldType.FLAT));
        if (name.equals("MuxHalloween")) w.setTime(18000L);
        else w.setTime(0L);
        w.getEntities().stream().filter(entity -> entity.getType() == EntityType.DROPPED_ITEM).forEach(Entity::remove);
        w.setGameRuleValue("doDaylightCycle", "false");
        w.setGameRuleValue("doMobSpawning", "false");
        final WorldBorder wb = w.getWorldBorder();
        final String dropsFileName;
        if (h instanceof MuxChristmas) {
            wb.setSize(200D);
            wb.setCenter(0D, 0D);
            dropsFileName = "drops-xmas.yml";
            locs.add(new Location(w, -8.5D, 12D, -1.5D, 145F, 0F));
            locs.add(new Location(w, 30.5D, 15D, -60.5D, 50F, -20F));
        } else if (h instanceof MuxEaster) {
            wb.setSize(400D);
            wb.setCenter(-450D, -1125D);
            dropsFileName = "drops-easter.yml";
            locs.add(new Location(w, -342.5D, 37D, -1184.5D, 0F, 0F));
        } else if (h instanceof MuxSummer) {
            wb.setSize(250D);
            wb.setCenter(0D, 0D);
            dropsFileName = "drops-summer.yml";
            locs.add(new Location(w, 76.5D, 64D, 56D, 145F, 0F));
            locs.add(new Location(w, 6D, 35D, -96D, 0F, 0F));
        } else if (h instanceof MuxHalloween) {
            wb.setSize(500D);
            wb.setCenter(-38D, -257D);
            dropsFileName = "drops-halloween.yml";
            locs.add(new Location(w, -54.5D, 36D, -161.5D, 90F, 0F));
            locs.add(new Location(w, 23.5D, 32.1D, -405.5D, 0F, 0F));
        } else if (h instanceof MuxBirthday) {
            wb.setSize(200D);
            wb.setCenter(0D, 0D);
            dropsFileName = "drops-birthday.yml";
            locs.add(new Location(w, 50.5D, 7D, -0D, 90F, 0F));
            locs.add(new Location(w, -55D, 7D, -0D, -90F, 0F));
        } else {
            wb.setSize(200D);
            wb.setCenter(0D, 0D);
            dropsFileName = "drops.yml";
            locs.add(new Location(w, 50.5D, 7D, -0D, 90F, 0F));
            locs.add(new Location(w, -55D, 7D, -0D, -90F, 0F));
        }
        loc = locs.get(r.nextInt(locs.size()));
        dropFile = new File(ms.getDataFolder() + "/event", dropsFileName);
        dropYML = YamlConfiguration.loadConfiguration(dropFile);
        for (final String drop : dropYML.getConfigurationSection("").getKeys(false)) {
            final Drop d;
            final boolean coin = drop.startsWith("coin");
            if (coin) {
                coindrops.add(d = new Drop(Short.parseShort(drop.replace("coindrop", "")), new Location(w, dropYML.getDouble(drop + ".x"), dropYML.getDouble(drop + ".y"), dropYML.getDouble(drop + ".z"))));
            } else {
                drops.add(d = new Drop(Short.parseShort(drop.replace("drop", "")), new Location(w, dropYML.getDouble(drop + ".x"), dropYML.getDouble(drop + ".y"), dropYML.getDouble(drop + ".z"))));
            }
            d.loc.getWorld().loadChunk(d.loc.getChunk());
            final String configitem = dropYML.getString(drop + ".i");
            final ItemStack is = configitem == null ? null : new ItemStack(Integer.parseInt(configitem.split(":")[0]), 1, Short.parseShort(configitem.split(":")[1]));
            final Item i = loc.getWorld().dropItem(d.loc.getBlock().getLocation().add(0.5D, 1.0D, 0.5D), coin ? new ItemStack(Material.DOUBLE_PLANT) : is == null ? item : is);
            i.setVelocity(new Vector(0, 0, 0));
            NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "uniqueID", ((CraftItem) i).getHandle(), ms.getIdentificationId());
        }
        if (drops.size() >= 50) {
            for (final Player pl : ms.getServer().getOnlinePlayers()) {
                ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 " + getName() + " Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
                ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
                pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
            }
            spawnEventNPC("§d§lEvent beitreten");
        }
    }

    @Override
    public boolean join(final Player p) {
        if (drops.size() < 50 && p.isOp() == false) {
            ms.showItemBar(p, "§cDas Event wird noch gerade vorbereitet.");
            return false;
        } else if (ms.getMuxUser(p.getName()).getOnlineTime() < 1800000L) {
            ms.showItemBar(p, "§cNeue Accounts können nicht teilnehmen.");
            return false;
        } else if (ms.hasVoted(p) == false) {
            ms.sendNoVoteMessage(p);
            return false;
        }
        equipPlayer(p);
        ms.forcePlayer(p, getRandomSpawn());

        p.sendMessage(new String[]{
                ms.header((byte) 11, thirdcolor),
                "  " + thirdcolor + "§l" + getName(),
                "  ",
        });
        for (final String text : getDescription()) {
            if (text.equals("")) break;
            p.sendMessage("  " + text);
        }
        p.sendMessage(ms.footer((byte) 11, thirdcolor));

        SearchEventUser u = ms.getDB().loadSearchEventUser(p.getUniqueId());
        if (u == null) {
            u = new SearchEventUser(p.getUniqueId(), new ArrayList<>(), new ArrayList<>());
        }
        evuser.put(p.getName(), u);
        ms.sendScoreboard(p, sb);

        final ItemStack tp = ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§f§lMux§a§lTeleport", "§7Teleportiert dich zu einem", "§7zufälligem Spawnpunkt.");
        p.getInventory().setItem(0, tp);
        p.getInventory().setHeldItemSlot(0);
        return true;
    }

    @Override
    public void quit(final Player p) {
        final SearchEventUser user = evuser.remove(p.getName());
        if (user != null) {
            ms.getDB().saveSearchEventUser(user);
        }
        resetPlayer(p);
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        if (players.contains(e.getPlayer()) && e.getAction() != Action.PHYSICAL && (e.getMaterial() != null && e.getMaterial() == Material.SLIME_BALL)) {
            death(e.getPlayer());
        }
    }

    @EventHandler
    public void onDespawn(final ItemDespawnEvent e) {
        if (e.getEntityType() == EntityType.DROPPED_ITEM && (e.getEntity().getUniqueId().equals(ms.getIdentificationId()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreProcess(final PlayerCommandPreprocessEvent e) {
        if (ms.isTrusted(e.getPlayer().getName()) == false) return;
        final Player p = e.getPlayer();
        final String cmd = e.getMessage().toLowerCase().split(" ")[0];
        final String[] args = e.getMessage().toLowerCase().replace(cmd + " ", "").split(" ");
        final boolean coins = cmd.equalsIgnoreCase("/coins"), drop = cmd.equalsIgnoreCase("/drop");
        if (coins || drop) {
            final String dropmenu = cmd.toLowerCase().replace("/", ""), droptype = coins ? "CoinDrop" : "Drop", dropcfg = droptype.toLowerCase();
            e.setCancelled(true);
            if (p.getWorld().getName().equals(loc.getWorld().getName()) == false) {
                ms.showItemBar(p, "§cDu musst dich in der Event Welt dafür befinden.");
                return;
            } else if (args.length != 2) {
                dropMenu(p, dropmenu);
                return;
            }
            short id;
            try {
                id = Short.parseShort(args[1]);
            } catch (final NumberFormatException ex) {
                ms.showItemBar(p, "§cDie ID muss eine Nummer sein.");
                return;
            }
            Location l = null;
            if (args[0].equalsIgnoreCase("create")) {
                final ItemStack is = p.getItemInHand();
                if (dropYML.getString(dropcfg + id) != null) {
                    ms.showItemBar(p, "§cDer " + droptype + " §6" + id + " §cexistiert bereits.");
                    return;
                } else if (is == null || is.getType() == Material.AIR) {
                    ms.showItemBar(p, "§cDu musst ein Item in der Hand halten.");
                    return;
                } else if (p.getWorld().equals(loc.getWorld()) == false) {
                    ms.showItemBar(p, "§cDu musst in der Event-Welt sein.");
                    return;
                } else if (p.isOnGround() == false) {
                    ms.showItemBar(p, "§cDu musst auf den Boden sein.");
                    return;
                }
                final Location pl = p.getLocation();
                l = new Location(loc.getWorld(), pl.getBlockX(), pl.getBlockY(), pl.getBlockZ());
                if (coins) coindrops.add(new Drop(id, l));
                else drops.add(new Drop(id, l));
                dropYML.set(dropcfg + id + ".x", l.getX());
                dropYML.set(dropcfg + id + ".y", l.getY());
                dropYML.set(dropcfg + id + ".z", l.getZ());
                if (drop) dropYML.set(dropcfg + id + ".i", is.getTypeId() + ":" + is.getDurability());
                try {
                    dropYML.save(dropFile);
                } catch (final IOException ex) {
                    ex.printStackTrace();
                    return;
                }
                final Item i = loc.getWorld().dropItem(l.getBlock().getLocation().add(0.5D, 1.0D, 0.5D), is);
                i.setVelocity(new Vector(0, 0, 0));
                NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "uniqueID", ((CraftItem) i).getHandle(), ms.getIdentificationId());
                ms.showItemBar(p, "§fDer " + droptype + " §e" + id + " §fwurde erstellt.");
                return;
            } else if (args[0].equalsIgnoreCase("remove")) {
                dropYML.set(dropcfg + id, null);
                try {
                    dropYML.save(dropFile);
                } catch (final IOException ex) {
                    ex.printStackTrace();
                    return;
                }
                Drop dr = null;
                for (final Drop d : (coins ? coindrops : drops)) {
                    if (d.getId() == id) {
                        dr = d;
                        break;
                    }
                }
                if (dr == null) {
                    ms.showItemBar(p, "§cDer " + droptype + " §6" + id + " §cexistiert nicht.");
                    return;
                }
                if (coins) coindrops.remove(dr);
                else drops.remove(dr);
                ms.showItemBar(p, "§fDer " + droptype + " §e" + id + " §fwurde §cgelöscht§f.");
                return;
            } else if (args[0].equalsIgnoreCase("tp")) {
                for (final Drop d : (coins ? coindrops : drops)) {
                    if (d.getId() == id) {
                        l = d.getLoc();
                        break;
                    }
                }
                if (l != null) {
                    ms.forcePlayer(p, l);
                    ms.showItemBar(p, "§fDu wurdest zum " + droptype + " §e" + id + " §fteleportiert.");
                } else {
                    ms.showItemBar(p, "§cDer " + droptype + " §6" + id + " §cexistiert nicht.");
                }
                return;
            }
            dropMenu(p, dropmenu);
        }
    }

    @EventHandler
    public void onDrop(final PlayerDropItemEvent e) { // For non-event players
        if (e.getItemDrop().getWorld().equals(loc.getWorld()) == false) return;
        e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(final EntityDamageEvent e) {
        final org.bukkit.entity.Entity en = e.getEntity();
        if (en instanceof Player && players.contains(en)) {
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void onBreak(final BlockBreakEvent e) {
        if (players.contains(e.getPlayer()) == false) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(final PlayerPickupItemEvent e) {
        if (e.getItem().getWorld().equals(loc.getWorld()) == false) return;
        for (final Drop d : drops) {
            final Location l = d.getLoc(), l2 = e.getItem().getLocation();
            if (l2.getBlockX() == l.getX() && l2.getBlockZ() == l.getZ()) {
                e.setCancelled(true);
                final Player p = e.getPlayer();
                final String name = p.getName();
                if (players.contains(p) == false) {
                    return;
                }
                final short id = d.getId();
                final SearchEventUser u = evuser.get(name);
                if (u.getFound().contains(id)) {
                    final Short last = cooldown.get(name);
                    if ((last == null || last != id) && ms.checkGeneralCooldown(name, "SEARCHEVENT", 800L, true) == false) {
                        ms.showItemBar(p, "§7" + specialchar + secondcolor + " Du hast " + "§7" + dropadj + " " + dropsg + secondcolor + " bereits gefunden! §7" + specialchar);
                        cooldown.put(name, id);
                    }
                    return;
                }
                u.addFound(id);
                final int count = u.getScore();
                p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1.0F, 1.0F);
                ms.showItemBar(p,thirdcolor + specialchar + secondcolor + " Du hast " + thirdcolor + dropadj2 + " " + dropsg + secondcolor + " gefunden! " + thirdcolor + specialchar);
                cooldown.put(name, id);
                switch (count) {
                    case 10:
                    case 20:
                    case 30:
                    case 40:
                        break;
                    case 50:
                        ms.getChests().addChest(p.getUniqueId(), h instanceof MuxEaster ? MuxChests.ChestType.EASTER : h instanceof MuxChristmas ? MuxChests.ChestType.CHRISTMAS : h instanceof MuxHalloween ? MuxChests.ChestType.HALLOWEEN : h instanceof MuxSummer ? MuxChests.ChestType.SUMMER : MuxChests.ChestType.TOP20);
                        p.sendMessage(thirdcolor + specialchar + secondcolor + " Du hast " + thirdcolor + "alle " + droppl + secondcolor + " gefunden! " + thirdcolor + "Glückwunsch!");
                        p.sendMessage(thirdcolor + specialchar + secondcolor + " Wenn du möchtest, kannst du noch " + thirdcolor + "die Map" + secondcolor + " erkundigen!");
                        p.sendMessage(thirdcolor + specialchar + secondcolor + " Bonus: Es gibt " + thirdcolor + "15 Coin Drops " + secondcolor + "die du finden kannst!");
                        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
                        quit(p);
                        ms.getChests().openChestInv(p);
                        ms.broadcastMessage(firstcolor + specialchar + secondcolor + " Der Spieler " + firstcolor + "§l" + name + secondcolor + " hat alle " + firstcolor + "§l" + droppl + secondcolor + " gefunden!", null, MuxSystem.Priority.NORMAL);
                        return;
                    default:
                        ms.sendScoreboard(p, sb);
                        return;
                }
                ms.sendScoreboard(p, sb);
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
                p.sendMessage(thirdcolor + specialchar + secondcolor + " Du hast jetzt " + thirdcolor + count + "/50 " + droppl + " " + secondcolor + "gefunden!");
                return;
            }
        }
        for (final Drop d : coindrops) {
            final Location l = d.getLoc(), l2 = e.getItem().getLocation();
            if (l2.getBlockX() == l.getX() && l2.getBlockZ() == l.getZ()) {
                e.setCancelled(true);
                final Player p = e.getPlayer();
                final String name = p.getName();
                if (players.contains(p) == false) {
                    return;
                }
                final short id = d.getId();
                final SearchEventUser u = evuser.get(name);
                if (u.getCoinsFound().contains(id)) {
                    final Short last = cooldown.get(name);
                    if ((last == null || last != id) && ms.checkGeneralCooldown(name, "SEARCHEVENT", 800L, true) == false) {
                        ms.showItemBar(p, "§7" + specialchar + secondcolor + " Du hast §7den Coinbonus" + secondcolor + " bereits gefunden! §7" + specialchar);
                        cooldown.put(name, id);
                    }
                    return;
                }
                final int count = u.getCoinScore();
                u.addCoinFound(id);
                giveReward(p, 2, MuxMining.OreTypes.DIAMOND, false);
                p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1.0F, 1.0F);
                if (count == 15) {
                    p.sendMessage(thirdcolor + specialchar + secondcolor + " Du hast " + thirdcolor + "alle Bonuscoins" + secondcolor + " gefunden! " + thirdcolor + "Glückwunsch!");
                    p.sendMessage(thirdcolor + specialchar + secondcolor + " Wenn du möchtest, kannst du noch " + thirdcolor + "die Map" + secondcolor + " erkundigen!");
                } else {
                    ms.showItemBar(p,thirdcolor + specialchar + secondcolor + " Du hast " + thirdcolor + "ein Coinbonus" + secondcolor + " gefunden! " + thirdcolor + specialchar);
                }
                ms.sendScoreboard(p, sb);
                cooldown.put(name, id);
                return;
            }
        }
        e.getItem().remove();
        e.setCancelled(true);
    }
    class Drop {
        private final short id;
        private final Location loc;

        public Drop(final short id, final Location loc) {
            this.id = id;
            this.loc = loc;
        }

        public short getId() {
            return id;
        }

        public Location getLoc() {
            return loc;
        }
    }
}