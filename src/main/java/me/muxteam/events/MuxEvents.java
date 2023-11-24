package me.muxteam.events;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import me.muxteam.basic.MuxActions;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.basic.MuxChatInput;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.basic.MuxLocation;
import me.muxteam.basic.NMSReflection;
import me.muxteam.extras.MuxChests.ChestType;
import me.muxteam.holidays.MuxChristmas;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

public final class MuxEvents {
    private final Random r = new Random();
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private final Map<EventCategory, List<Event>> categories = new EnumMap<>(EventCategory.class);
    private final MuxHolograms.Hologram autoEventHologram;
    private final Effects effects;
    private final Location npcloc;
    private Event ev;
    public MuxActions.PlayerAction npcaction = p -> p.performCommand("event");

    // ACCESS FROM EVERY EVENT
    public MuxSystem ms;
    public final Inventory opinv, pinv;

    // EVENTS
    private final Set<Event> eventList = new HashSet<>();
    private final Event splegg, jump, arena, exit, button, fastchat, war, guess, blockparty, tntrun, globalmute,
            sg, tnt, dropper, toparena, gunwar, anvildrop, getdown, boss, templerun, guns, chamber, varo, mine;
    private final MuxDropSearch dropsearch;
    private final MuxCustom custom;
    private final MuxLotto lotto;
    private final MuxGiveAll giveall;
    private final MuxRandom random;
    private final MuxEXP exp;
    private final MuxQuiz quiz;
    private final MuxTournament tournament;

    public MuxEvents(final MuxSystem ms) {
        this.ms = ms;
        this.sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.of("CET")));
        npcloc = new Location(ms.getGameSpawn().getWorld(), -6.5D, 76D, -8.5D);
        lotto = new MuxLotto(this);
        exit = new MuxExit(this);
        button = new MuxButton(this);
        giveall = new MuxGiveAll(this);
        random = new MuxRandom(this);
        exp = new MuxEXP(this);
        quiz = new MuxQuiz(this);
        fastchat = new MuxFastChat(this);
        globalmute = new MuxLastChat(this);
        guess = new MuxGuess(this);
        splegg = new MuxSplegg(this);
        blockparty = new MuxBlockParty(this);
        dropper = new MuxDropper(this);
        tntrun = new MuxTNTRun(this);
        chamber = new MuxChamber(this);
        guns = new MuxGuns(this);
        tnt = new MuxTNT(this);
        mine = new MuxMine(this);
        jump = new MuxJump(this);
        getdown = new MuxGetDown(this);
        toparena = new MuxTopArena(this);
        war = new MuxWar(this);
        boss = new MuxBoss(this);
        arena = new MuxArena(this);
        gunwar = new MuxGunWar(this);
        anvildrop = new MuxAnvilDrop(this);
        templerun = new MuxTempleRun(this);
        custom = new MuxCustom(this);
        sg = new MuxSurvivalGames(this);
        varo = new MuxVaro(this);
        tournament = new MuxTournament(this);
        dropsearch = new MuxDropSearch(this);

        opinv = ms.getServer().createInventory(null, 54, "§0§lMuxEvent§0 | /event");
        pinv = ms.getServer().createInventory(null, 27, "§0§lMuxEvent§0 | /event");

        effects = new Effects();

        for (final EventCategory category : EventCategory.values()) {
            categories.put(category, new ArrayList<>());
        }
        for (final Field declaredField : MuxEvents.class.getDeclaredFields()) {
            try {
                final Object o = declaredField.get(this);
                if (o instanceof Event) eventList.add((Event) o);
            } catch (final IllegalAccessException ignored) {
            }
        }
        if (ms.getHashYAML().isString("nextevents")) {
            final Gson gson = new Gson();
            final Map<EventCategory, List<String>> mappedCategories = gson.fromJson(ms.getHashYAML().getString("nextevents"),
                    new TypeToken<HashMap<EventCategory, List<String>>>() {
                    }.getType());
            mappedCategories.forEach((eventCategory, classes) -> classes.forEach(aClass -> {
                for (final Field declaredField : MuxEvents.class.getDeclaredFields()) {
                    try {
                        if (declaredField.get(MuxEvents.this) == null || declaredField.get(MuxEvents.this).getClass() == null)
                            continue;
                        if (declaredField.get(MuxEvents.this).getClass().getName().equals(aClass)) {
                            categories.get(eventCategory).add((Event) declaredField.get(MuxEvents.this));
                            break;
                        }
                    } catch (final IllegalAccessException ignored) {
                    }
                }
            }));
        }
        autoEventHologram = ms.getHolograms().addHologram("AutoEventHologram", npcloc.clone(),
                LocalDateTime.now(ZoneId.of("CET")).getDayOfWeek() == DayOfWeek.SATURDAY ? "§6Um 19 Uhr" : "§6Um 16 Uhr");
        fillEventCategories();
    }

    public void close() {
        stopEvent(null,true);
        final Gson gson = new Gson();
        final Map<EventCategory, List<String>> mappedCategories = new EnumMap<>(EventCategory.class);
        categories.forEach((eventCategory, events) -> {
            final List<String> list;
            mappedCategories.put(eventCategory, list = new ArrayList<>());
            events.forEach(event -> list.add(event.getClass().getName()));
        });
        ms.getHashYAML().set("nextevents", gson.toJson(mappedCategories));
        this.ms = null;
    }

    public Location getNPCLoc() {
        return npcloc;
    }

    private void fillEventCategories() {
        for (final EventCategory category : EventCategory.values()) {
            if (categories.get(category).isEmpty()) {
                if (category == EventCategory.CHAT) {
                    categories.get(EventCategory.CHAT).add(fastchat);
                    categories.get(EventCategory.CHAT).add(fastchat);
                    categories.get(EventCategory.CHAT).add(fastchat);
                    categories.get(EventCategory.CHAT).add(globalmute);
                    categories.get(EventCategory.CHAT).add(quiz);
                    categories.get(EventCategory.CHAT).add(quiz);
                    categories.get(EventCategory.CHAT).add(lotto);
                    categories.get(EventCategory.CHAT).add(guess);
                    categories.get(EventCategory.CHAT).add(guess);
                } else if (category == EventCategory.BIG) {
                    categories.get(EventCategory.BIG).add(sg);
                    categories.get(EventCategory.BIG).add(tournament);
                    categories.get(EventCategory.BIG).add(varo);
                } else if (category == EventCategory.NORMAL) {
                    categories.get(EventCategory.NORMAL).add(splegg);
                    categories.get(EventCategory.NORMAL).add(jump);
                    categories.get(EventCategory.NORMAL).add(arena);
                    categories.get(EventCategory.NORMAL).add(toparena);
                    categories.get(EventCategory.NORMAL).add(war);
                    categories.get(EventCategory.NORMAL).add(blockparty);
                    categories.get(EventCategory.NORMAL).add(tntrun);
                    categories.get(EventCategory.NORMAL).add(tnt);
                    categories.get(EventCategory.NORMAL).add(dropper);
                    categories.get(EventCategory.NORMAL).add(toparena);
                    categories.get(EventCategory.NORMAL).add(gunwar);
                    categories.get(EventCategory.NORMAL).add(anvildrop);
                    categories.get(EventCategory.NORMAL).add(getdown);
                    categories.get(EventCategory.NORMAL).add(boss);
                    categories.get(EventCategory.NORMAL).add(templerun);
                    categories.get(EventCategory.NORMAL).add(guns);
                    categories.get(EventCategory.NORMAL).add(chamber);
                }
            }
        }
    }

    public Event getCurrent() {
        return ev;
    }

    public void clearCurrent() {
        ev = null;
    }

    public Effects getEffects() {
        return effects;
    }


    public boolean isKickable(final Event ev) {
        return ev.canSpectate || ev == custom;
    }

    public void updateHologram() {
        if (ms.getAdmin().AUTOEVENT.isActive() == false) {
            if (autoEventHologram.isDead() == false)
                autoEventHologram.hide();
            return;
        }
        final ZoneId zone = ZoneId.of("CET");
        final LocalDateTime date = LocalDateTime.now(zone);
        final String holidayevent = dropsearch.getAnnouncement();
        ms.getNPCS().changeName(npcaction,
                ev == dropsearch || (holidayevent != null && holidayevent.toLowerCase().contains("heute")) ? "§d§l" + dropsearch.getAnnounceName() :
                        ((date.getDayOfWeek() == DayOfWeek.SATURDAY && date.getHour() <= 19) || (date.getDayOfWeek() == DayOfWeek.FRIDAY && date.getHour() > 19 && getCurrent() == null) ? "§d§l" + "Großes Event" : "§d§lEvent"));
        if (ev != null && !(ev instanceof MuxLotto || ev instanceof MuxFastChat || ev instanceof MuxLastChat || ev instanceof MuxGuess || ev instanceof MuxQuiz)) {
            if (autoEventHologram.isDead() == false)
                autoEventHologram.hide();
            return;
        }
        if (autoEventHologram.isDead())
            autoEventHologram.spawn();
        if (autoEventHologram.getStand() != null && autoEventHologram.getStand().isCustomNameVisible() == false)
            autoEventHologram.show();

        if (ev != null) {
            if (autoEventHologram.isDead() == false || autoEventHologram.getStand().isCustomNameVisible())
                autoEventHologram.hide();
        } else if ((dropsearch.getAnnouncement() != null && dropsearch.getAnnouncement().toLowerCase().contains("heute") && date.getHour() < 12)) {
            autoEventHologram.setMessage("§6Um 12 Uhr");
        } else if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getHour() < 20 && date.getHour() >= 15) {
            if (date.getHour() == 17)
                autoEventHologram.setMessage("§6Um 19 Uhr");
            else
                hologramCountdown(zone);
        } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            if (date.getHour() < 18) {
                autoEventHologram.setMessage("§6Um 19 Uhr");
            } else if (date.getHour() < 19) {
                hologramCountdown(zone);
            } else {
                autoEventHologram.setMessage("§6Morgen 16 Uhr");
            }
        } else {
            autoEventHologram.setMessage(date.getHour() > 19 ? "§6Morgen " + (date.getDayOfWeek() == DayOfWeek.FRIDAY ? "19" : "16") + " Uhr" : "§6Um 16 Uhr");
        }
    }

    private void hologramCountdown(final ZoneId zone) {
        final LocalDateTime current = LocalDateTime.now(zone), to = LocalDateTime.now(zone).withHour(current.getHour() + 1).withMinute(0).withSecond(0);
        autoEventHologram.setMessage("§6In " + ms.clockTimeToString(to.get(ChronoField.SECOND_OF_DAY) - current.get(ChronoField.SECOND_OF_DAY)));
    }

    public void checkAutoEvent() {
        if (ev != null && (ev instanceof MuxSurvivalGames || ev instanceof MuxTournament || ev instanceof MuxVaro || ev instanceof MuxExit))
            return;
        final ZoneId zone = ZoneId.of("CET");
        final LocalDateTime date = LocalDateTime.now(zone);
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY && date.getHour() == 19 && date.getMinute() == 0) {
            // Varo, Tournament or SurvivalGames
            startAutoEvent(categories.get(EventCategory.BIG).remove(r.nextInt(categories.get(EventCategory.BIG).size())));
            return;
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY && date.getHour() == 18 && date.getMinute() == 0 && date.getDayOfMonth() <= 7) {
            // Exit Event
            startAutoEvent(exit);
            return;
        }
        if (date.getHour() >= 14 && date.getMinute() == 30 && ev == null) {
            // Chat Event
            startAutoEvent(categories.get(EventCategory.CHAT).remove(r.nextInt(categories.get(EventCategory.CHAT).size())));
            return;
        }
        if (date.getMinute() == 0 && date.getHour() >= 16 && date.getHour() <= 20 && date.getHour() != 18) {
            if (date.getHour() > 17 && date.getDayOfWeek() == DayOfWeek.SATURDAY) return;
            // Normal Events
            startAutoEvent(categories.get(EventCategory.NORMAL).remove(r.nextInt(categories.get(EventCategory.NORMAL).size())));
        }
    }

    private long lastStart = 0;

    private void startAutoEvent(final Event event) {
        if (Bukkit.isPrimaryThread() == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    startAutoEvent(event);
                }
            }.runTask(ms);
            return;
        }
        if (lastStart > System.currentTimeMillis() - 60000L) return;
        if (ev != null) {
            lastStart = System.currentTimeMillis() - 29000L;
            new BukkitRunnable() {
                @Override
                public void run() {
                    startAutoEvent(event);
                    lastStart = System.currentTimeMillis() - 30000L;
                }
            }.runTaskLater(ms, 600L);
            return;
        }
        lastStart = System.currentTimeMillis();
        startEvent(null, event);
        new BukkitRunnable() { // Needs to be delayed
            @Override
            public void run() {
                fillEventCategories();
            }
        }.runTaskLater(ms, 1L);
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (args.length == 0) {
            final Inventory inv = p.isOp() ? opinv : pinv;
            if (p.isOp()) {
                setAdminItem(18, inv, giveall, true);
                setAdminItem(19, inv, random, true);
                setAdminItem(20, inv, exp, true);
                setAdminItem(21, inv, lotto, true);
                setAdminItem(22, inv, quiz, true);
                setAdminItem(23, inv, guess, false);
                setAdminItem(24, inv, globalmute, false);
                setAdminItem(25, inv, fastchat, false);
                setAdminItem(26, inv, exit, false);

                setAdminItem(27, inv, tnt, false);
                setAdminItem(28, inv, splegg, false);
                setAdminItem(29, inv, arena, false);
                setAdminItem(30, inv, blockparty, false);
                setAdminItem(31, inv, tntrun, false);
                setAdminItem(32, inv, anvildrop, false);
                setAdminItem(33, inv, war, false);
                setAdminItem(34, inv, gunwar, false);
                setAdminItem(35, inv, guns, false);
                setAdminItem(36, inv, chamber, false);
                setAdminItem(37, inv, toparena, false);
                setAdminItem(38, inv, getdown, false);
                setAdminItem(39, inv, jump, false);
                setAdminItem(40, inv, dropper, false);
                setAdminItem(41, inv, templerun, false);
                setAdminItem(42, inv, mine, false);
                setAdminItem(43, inv, boss, false);

                setAdminItem(44, inv, dropsearch, true);
                setAdminItem(45, inv, sg, false);
                setAdminItem(46, inv, varo, false);
                setAdminItem(47, inv, tournament, true);
                setAdminItem(48, inv, button, false);
                setAdminItem(49, inv, custom, true);
                inv.setItem(0, ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lStatistiken", "§7Hier siehst du unter anderem die", "§7gesamten Ausgaben pro Event.", "", "§fKlicke§7, um die Statistiken zu sehen."));
            }
            final int pos = p.isOp() ? 4 : 13;
            if (ev != null) {
                final ItemStack evitem = ms.renameItemStack(ev.getItem().clone(), "§d§l" + ev.getName(), ev.getDescription());
                if (ev.canjoin) {
                    if (ev.players.contains(p))
                        inv.setItem(pos, ms.addLore(evitem, "", "§cDu nimmst bereits am Event teil."));
                    else inv.setItem(pos, ms.addLore(evitem, "", ev.getClickToJoin()));
                } else {
                    if (ev.canSpectate)
                        inv.setItem(pos, ms.addLore(evitem, "", "§dKlicke§7, um beim Event zuzuschauen."));
                    else inv.setItem(pos, evitem);
                }
            } else {
                inv.setItem(pos, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 2), "§d§lKein Event", "§7Derzeit findet kein Event statt."));
                if (p.isOp() == false) {
                    String nextevent;
                    String nextchatevent;
                    final ZoneId zone = ZoneId.of("CET");
                    final LocalDateTime date = LocalDateTime.now(zone);
                    final int year = date.getYear();
                    if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getHour() < 20 && date.getHour() >= 15) {
                        if (date.getHour() == 17) {
                            nextevent = "Um 19 Uhr";
                        } else {
                            final LocalDateTime d = LocalDateTime.of(year, date.getMonthValue(), date.getDayOfMonth(), date.getHour() + 1, 0, 0, 0);
                            nextevent = "um " + sdf.format(new Date(d.atZone(zone).toInstant().toEpochMilli())) + " Uhr";
                        }
                    } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
                        if (date.getHour() == 17)
                            nextevent = "um 19 Uhr";
                        else if (date.getHour() >= 15 && date.getHour() < 19) {
                            final LocalDateTime d = LocalDateTime.of(year, date.getMonthValue(), date.getDayOfMonth(), date.getHour() + 1, 0, 0, 0);
                            nextevent = "um " + sdf.format(new Date(d.atZone(zone).toInstant().toEpochMilli())) + " Uhr";
                        } else if (date.getHour() < 15) {
                            nextevent = "um 16 Uhr";
                        } else {
                            nextevent = "Morgen 16 Uhr";
                        }
                    } else {
                        nextevent = date.getHour() > 19 ? "Morgen " + "16" + " Uhr" : "um 16 Uhr";
                    }
                    if (date.getHour() == 23 && date.getMinute() > 30) {
                        nextchatevent = "Morgen 14:30 Uhr";
                    } else if (date.getHour() >= 14) {
                        final LocalDateTime d = LocalDateTime.of(year, date.getMonthValue(), date.getDayOfMonth(), date.getMinute() > 30 ? date.getHour() + 1 : date.getHour(), 30, 0, 0);
                        nextchatevent = "um " + sdf.format(new Date(d.atZone(zone).toInstant().toEpochMilli())) + " Uhr";
                    } else {
                        nextchatevent = "um 14:30 Uhr";
                    }


                    if (ms.getAdmin().AUTOEVENT.isActive() == false) {
                        ms.openBook(ms.book("Events",
                                "§5§lEvents\n\n§0Sie finden regelmäßig statt. Jeden Samstag um 19 Uhr gibt es ein großes Event mit einem hohen Preisgeld.\n\n" +
                                        "§4Zurzeit finden keine Events statt. Schaue nochmal später vorbei!"), p);
                        return true;
                    }
                    ms.openBook(ms.book("Events",
                            "§5§lEvents\n\n§0Sie finden regelmäßig statt. Jeden Samstag um 19 Uhr gibt es ein großes Event mit einem hohen Preisgeld.\n\n" +
                                    (date.getDayOfWeek() == DayOfWeek.SATURDAY && (date.getHour() == 17 || date.getHour() == 18) ? "Großes Event" : "Nächstes Event") + ": \n§5" + nextevent +
                                    "\n\n§0Kleines Event:\n§5" + nextchatevent), p);
                    return true;
                }
            }
            final InvType it = ms.getActiveInv(p.getName());
            if (it != InvType.EVENT && it != InvType.EARN) p.closeInventory();
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.EVENT);
        } else if (args[0].equalsIgnoreCase("join")) {
            joinEvent(p);
        } else if (args[0].equalsIgnoreCase("leave")) {
            if (ev != null && p.getGameMode() == GameMode.SPECTATOR && ev.getSpectators().containsKey(p.getUniqueId())) {
                final MuxUser u = ms.getMuxUser(p.getName());
                final Location lastloc = u.getLastLocation(),
                        l = lastloc != null && lastloc.getWorld() != null ? lastloc : ms.getGameSpawn();
                ms.forcePlayer(p, l);
                return true;
            }
            quitEvent(p, true);
        } else if (args[0].equalsIgnoreCase("action")) {
            if (ev != null) ev.action(p, args);
        } else if (args[0].equalsIgnoreCase("start") && p.isOp()) {
            if (ev == null) {
                ms.showItemBar(p, "§cDerzeit läuft kein Event.");
                return true;
            } else if (ev.seconds <= 0 || ev.canjoin == false) {
                ms.showItemBar(p, "§cDas Event hat bereits begonnen.");
                return true;
            } else if (ev.players.size() < 2) {
                ms.showItemBar(p, "§cEs müssen mindestens 2 Spieler da sein.");
                return true;
            }
            ev.seconds = 2;
            ms.showItemBar(p, "§fDas Event wurde §agestartet§f.");
        } else {
            if (p.isOp()) ms.showItemBar(p, ms.usage("/event [join | leave | start]"));
            else ms.showItemBar(p, ms.usage("/event [join | leave]"));
        }
        return true;
    }

    private void openStatsInv(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxEvent§0 | Statistiken");
        setInfoItem(18, inv, giveall);
        setInfoItem(19, inv, random);
        setInfoItem(20, inv, exp);
        setInfoItem(21, inv, lotto);
        setInfoItem(22, inv, quiz);
        setInfoItem(23, inv, guess);
        setInfoItem(24, inv, globalmute);
        setInfoItem(25, inv, fastchat);
        setInfoItem(26, inv, exit);

        setInfoItem(27, inv, tnt);
        setInfoItem(28, inv, splegg);
        setInfoItem(29, inv, arena);
        setInfoItem(30, inv, blockparty);
        setInfoItem(31, inv, tntrun);
        setInfoItem(32, inv, anvildrop);
        setInfoItem(33, inv, war);
        setInfoItem(34, inv, gunwar);
        setInfoItem(35, inv, guns);
        setInfoItem(36, inv, chamber);
        setInfoItem(37, inv, toparena);
        setInfoItem(38, inv, getdown);
        setInfoItem(39, inv, jump);
        setInfoItem(40, inv, dropper);
        setInfoItem(41, inv, templerun);
        setInfoItem(42, inv, mine);
        setInfoItem(43, inv, boss);

        setInfoItem(44, inv, dropsearch);
        setInfoItem(45, inv, sg);
        setInfoItem(46, inv, varo);
        setInfoItem(47, inv, tournament);
        setInfoItem(48, inv, button);
        setInfoItem(49, inv, custom);
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.addGlow(ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lStatistiken", "§7Hier siehst du unter anderem die", "§7gesamten Ausgaben pro Event.")));
        if (ms.getActiveInv(p.getName()) != InvType.EVENT) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.EVENT);
    }

    public void handleInventory(final Player p, final ItemStack item, final Inventory inv, final int rawslot, final boolean rightclick) {
        final Material m = item == null ? null : item.getType();
        if (rawslot > 7 && (inv == null || inv.getSize() == 54 && inv.getTitle().contains("GiveAll") == false)) {
            final String name = item == null ? "" : ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (rightclick) {
                if (m == dropsearch.getItem().getType() && dropsearch.getName().equals(name)) {
                    if (ev == dropsearch) {
                        ms.showItemBar(p, "§cDies geht nur, wenn das Event nicht aktiv ist.");
                        p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 2F);
                        return;
                    }
                    dropsearch.switchHoliday();
                    p.performCommand("event");
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                } else if (m == tournament.getItem().getType()) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    final Inventory ginv = ms.getServer().createInventory(null, 36, "§0§lMuxEvent§0 | Turnier");
                    ginv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
                    ginv.setItem(4, ms.renameItemStack(tournament.getItem(), "§d§l" + tournament.getName(), tournament.getDescription()));
                    ginv.setItem(19, ms.renameItemStack(new ItemStack(ev == tournament ? Material.BARRIER : Material.POTION),
                            "§6§lPotion Kit", "", "§7Klicke, um dieses Kit zu §6äuszuwählen§7."));
                    ginv.setItem(22, ms.renameItemStack(new ItemStack(ev == tournament ? Material.BARRIER : Material.WOOD_SWORD),
                            "§6§lSchnelles Kit", "", "§7Klicke, um dieses Kit zu §6äuszuwählen§7."));
                    ginv.setItem(25, ms.renameItemStack(new ItemStack(ev == tournament ? Material.BARRIER : Material.MUSHROOM_SOUP),
                            "§6§lSoup Kit", "", "§7Klicke, um dieses Kit zu §6äuszuwählen§7."));
                    p.openInventory(ginv);
                    ms.setActiveInv(p.getName(), InvType.EVENT);
                } else if (m == lotto.getItem().getType()) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    final Inventory ginv = ms.getServer().createInventory(null, 36, "§0§lMuxEvent§0 | Lotto");
                    final int coins = lotto.getCoins();
                    ginv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
                    ginv.setItem(4, ms.renameItemStack(lotto.getItem(), "§d§l" + lotto.getName(), lotto.getDescription()));
                    ginv.setItem(22, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT),
                            "§6§l" + (coins == 0 ? "Keine" : ms.getNumberFormat(coins)) + " MuxCoins", "", "§7Klicke, um den Ticketpreis zu §6ändern§7."));
                    p.openInventory(ginv);
                    ms.setActiveInv(p.getName(), InvType.EVENT);
                } else if (m == quiz.getItem().getType()) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    final Inventory ginv = ms.getServer().createInventory(null, 36, "§0§lMuxEvent§0 | Quiz");
                    ginv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
                    ginv.setItem(4, ms.renameItemStack(quiz.getItem(), "§d§l" + quiz.getName(), quiz.getDescription()));
                    ginv.setItem(21, ms.renameItemStack(new ItemStack(quiz.running ? Material.BARRIER : Material.PAPER),
                            "§6§l" + (quiz.getQuestion() == null ? "Keine" : quiz.getQuestion()), "", "§7Klicke, um die Frage zu §6ändern§7."));
                    ginv.setItem(23, ms.renameItemStack(new ItemStack(quiz.running ? Material.BARRIER : Material.BOOK),
                            "§6§l" + (quiz.getAnswer() == null ? "Keine" : quiz.getAnswer()), "", "§7Klicke, um die Antwort zu §6ändern§7."));
                    p.openInventory(ginv);
                    ms.setActiveInv(p.getName(), InvType.EVENT);
                } else if (m == custom.getItem().getType()) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    final Inventory ginv = ms.getServer().createInventory(null, 36, "§0§lMuxEvent§0 | Eigenes Event");
                    ginv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
                    ginv.setItem(4, ms.renameItemStack(custom.getItem(), "§d§l" + custom.getName(), custom.getDescription()));
                    if (ev == custom)
                        ginv.setItem(8, ms.renameItemStack(new ItemStack(Material.BANNER, 1, (byte) (ev.canjoin ? 10 : 11)),
                                ev.canjoin ? "§a§lBetretbar" : "§e§lNicht betretebar", "",
                                ev.canjoin ? "§7Klicke, um Betreten zu §epausieren§7." : "§7Klicke, um Betreten zu §aerlauben§7."));
                    ginv.setItem(19, ms.renameItemStack(new ItemStack(Material.BOOK_AND_QUILL),
                            "§6§lEventname setzen §7| §6" + custom.getName(), "", "§7Klicke, um den Eventnamen zu §6ändern§7."));
                    ginv.setItem(22, ms.renameItemStack(new ItemStack(Material.BEACON),
                            "§6§lEventspawn setzen", "", "§7Klicke, um den Eventspawn zu §6setzen§7."));
                    ginv.setItem(25, ms.renameItemStack(new ItemStack(Material.ENDER_CHEST),
                            "§6§lInventar mitnehmen:§7 " + (custom.keepInventory ? "§a§laktiviert" : "§c§ldeaktiviert"),
                            ev == custom ? new String[]{"", "§cDu kannst die Option jetzt nicht ändern."} : new String[]{"",
                                    "§7Klicke, um die Option zu " + (custom.keepInventory ? "§cdeaktivieren§7." : "§aaktivieren§7.")}));
                    p.openInventory(ginv);
                    ms.setActiveInv(p.getName(), InvType.EVENT);
                } else if (m == giveall.getItem().getType()) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    p.openInventory(giveall.getInventory());
                    ms.setActiveInv(p.getName(), InvType.EVENT);
                } else if (m == random.getItem().getType()) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    final Inventory ginv = ms.getServer().createInventory(null, 36, "§0§lMuxEvent§0 | Zufall");
                    final short coins = random.getCoins();
                    ginv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
                    ginv.setItem(4, ms.renameItemStack(random.getItem(), "§d§l" + random.getName(), random.getDescription()));
                    if (ev == random)
                        ginv.setItem(8, ms.renameItemStack(new ItemStack(Material.BANNER, 1, (byte) (random.paused ? 11 : 10)),
                                random.paused ? "§e§lPausiert" : "§a§lLäuft", "",
                                random.paused ? "§7Klicke, um das Event §afortzusetzen§7." : "§7Klicke, um das Event zu §epausieren§7."));
                    if (ms.isFullTrusted(p.getName())) {
                        ginv.setItem(22, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT),
                                "§6§l" + (coins == 0 ? "Keine" : ms.getNumberFormat(coins)) + " MuxCoins", "", "§7Klicke, um den Gewinnpreis zu §6ändern§7."));
                    } else {
                        ginv.setItem(22, ms.renameItemStack(new ItemStack(Material.YELLOW_FLOWER),
                                "§6§lInfo", "§7Wenn das Event läuft, kannst", "§7du es oben rechts pausieren."));
                    }
                    p.openInventory(ginv);
                    ms.setActiveInv(p.getName(), InvType.EVENT);
                } else if (m == exp.getItem().getType()) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    final Inventory ginv = ms.getServer().createInventory(null, 36, "§0§lMuxEvent§0 | EXP");
                    ginv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
                    ginv.setItem(4, ms.renameItemStack(exp.getItem(), "§d§l" + exp.getName(), exp.getDescription()));
                    ginv.setItem(22, ms.renameItemStack(new ItemStack(Material.MAGMA_CREAM),
                            "§6§l" + ms.getMultiplier(exp.expboost) + " EXP", "", "§7Linksklick, um die Stärke zu §6erhöhen§7.", "§7Rechtsklick, um die Stärke zu §6reduzieren§7."));
                    p.openInventory(ginv);
                    ms.setActiveInv(p.getName(), InvType.EVENT);
                }
                return;
            }
            boolean clickedEvent = false;
            for (final Event e : eventList) {
                if (m == e.getItem().getType() && e.getName().equals(name)) {
                    if (ev == e) {
                        stopEvent(p, e.getClass().isAnnotationPresent(NonJoinableEvent.class));
                    } else {
                        startEvent(p, e);
                    }
                    clickedEvent = true;
                    break;
                }
            }
            if (clickedEvent == false)
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            else {
                p.performCommand("event");
                p.updateInventory();
            }
            return;
        } else if (inv.getSize() == 36) {
            if (m == Material.DOUBLE_PLANT) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                new MuxAnvil(ms, (input, player) -> {
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                    if (inv.getTitle().contains("Lotto")) {
                        lotto.setCoins(player, ms.retardCleaner(input, "MuxCoins: 0"));
                        handleInventory(p, lotto.getItem(), null, 45, true);
                        return;
                    }
                    random.setCoins(player, ms.retardCleaner(input, "MuxCoins: 0"));
                    handleInventory(p, random.getItem(), null, 45, true);
                }).show(p, "MuxCoins: 0", new ItemStack(Material.DOUBLE_PLANT));
            } else if (m == Material.PAPER) {
                if (quiz.running) {
                    handleInventory(p, quiz.item, null, 43, true);
                    ms.showItemBar(p, "§cQuiz läuft bereits.");
                    return;
                }
                new MuxChatInput(ms, (input, player1) -> {
                    if (quiz.running) {
                        handleInventory(p, quiz.item, null, 43, true);
                        ms.showItemBar(p, "§cQuiz läuft bereits.");
                        return;
                    }
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                    quiz.setQuestion(input.replace("Frage: ", ""));
                    handleInventory(p, quiz.item, null, 43, true);
                }).show(p, "Frage: ");
            } else if (m == Material.BOOK) {
                if (quiz.running) {
                    handleInventory(p, quiz.item, null, 43, true);
                    ms.showItemBar(p, "§cQuiz läuft bereits.");
                    return;
                }
                new MuxChatInput(ms, (input, player1) -> {
                    if (quiz.running) {
                        handleInventory(p, quiz.item, null, 43, true);
                        ms.showItemBar(p, "§cQuiz läuft bereits.");
                        return;
                    }
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                    quiz.setAnswer(input.replace("Antwort: ", ""));
                    handleInventory(p, quiz.item, null, 43, true);
                }).show(p, "Antwort: ");
            } else if (m == Material.BANNER) {
                if (ev == custom) {
                    ev.canjoin = item.getData().getData() == 11;
                } else if (ev == random) {
                    random.setPaused(item.getData().getData() != 11);
                }
                p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                handleInventory(p, ev.getItem(), null, 45, true);
            } else if (m == Material.MAGMA_CREAM) {
                exp.setEXPBoost(((byte) (exp.expboost + (rightclick ? -1 : 1))), false);
                p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                handleInventory(p, exp.getItem(), null, 45, true);
            } else if (m == Material.BOOK_AND_QUILL) {
                if (item.getItemMeta().getLore().get(1).contains("Eventnamen") == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                new MuxAnvil(ms, (input, player) -> {
                    custom.name = ms.retardCleaner(input, "Eventname: ");
                    handleInventory(p, custom.getItem(), null, 45, true);
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                }).show(p, "Eventname: ", new ItemStack(Material.BOOK_AND_QUILL));
            } else if (m == Material.BEACON) {
                custom.setLoc(p.getLocation());
                p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                handleInventory(p, custom.getItem(), null, 45, true);
            } else if (m == Material.ENDER_CHEST && ev != custom) {
                custom.toggleKeepInventory();
                p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                handleInventory(p, custom.getItem(), null, 45, true);
            } else if (m == Material.SKULL_ITEM) {
                final String crate = ChatColor.stripColor(item.getItemMeta().getLore().get(0));
                p.performCommand("crate evgiveall " + crate);
                p.openInventory(giveall.getInventory());
                ms.setActiveInv(p.getName(), InvType.EVENT);
            } else if (m == Material.ITEM_FRAME) {
                if (item.getItemMeta().getDisplayName().contains("GiveAll")) {
                    handleInventory(p, giveall.getItem(), null, 45, true);
                    return;
                }
                p.performCommand("event");
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            } else if (m == Material.POTION || m == Material.WOOD_SWORD || m == Material.MUSHROOM_SOUP) {
                if (ev == tournament) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                tournament.kit = (short) (m == Material.POTION ? 0 : m == Material.WOOD_SWORD ? 1 : 2);
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                p.performCommand("event");
            } else if (m != Material.AIR) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            }
            return;
        } else if (inv.getSize() == 54 && inv.getTitle().contains("GiveAll")) {
            if (m == Material.RABBIT_FOOT) {
                if (giveall.giveItem()) {
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                    handleInventory(p, null, inv, 45, true);
                    return;
                }
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            } else if (m == Material.CHEST) {
                if (ms.isTrusted(p.getName()) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                final Inventory ginv = ms.getServer().createInventory(null, 36, "§0§lMuxEvent§0 | Kisten");
                ginv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), "§7Zurück (GiveAll)"));
                ginv.setItem(4, ms.renameItemStack(new ItemStack(Material.CHEST), "§d§l" + giveall.getName(), giveall.getDescription()));
                short pos = 18;
                for (final ChestType c : ChestType.values()) {
                    if (c == ChestType.HALLOWEEN || c == ChestType.CHRISTMAS || c == ChestType.EASTER || c == ChestType.SUMMER || c == ChestType.ADVENT || c == ChestType.NEWBIE || c == ChestType.FAKE || c == ChestType.DAILY)
                        continue;
                    else if (c == ChestType.GIFT && (ms.getHoliday() instanceof MuxChristmas == false || ms.isTrusted(p.getName()) == false))
                        continue;
                    else if (c.getPrice() > 0 || c == ChestType.TOP3) continue;
                    ginv.setItem(pos, ms.renameItemStack(ms.getHeadFromURL(c.getTexture(), c.getName()), c.getName(), "§0" + c.name(), "§7Klicke, um die Kiste zu " + c.getColor() + "verteilen§7."));
                    pos++;
                }
                p.openInventory(ginv);
                ms.setActiveInv(p.getName(), InvType.EVENT);
            } else if (m == Material.ITEM_FRAME) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("event");
            } else if (m == giveall.getItem().getType()) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            } else if (m == null) {
                final List<ItemStack> items = giveall.getItems();
                final Inventory ginv = giveall.getInventory();
                for (int i = 18; i < items.size() + 18; i++) {
                    ginv.setItem(i, items.get(i - 18));
                }
                ginv.setItem(18 + items.size(), null);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        final List<ItemStack> items = new ArrayList<>();
                        for (int i = 18; i < inv.getSize(); i++) {
                            items.add(inv.getItem(i));
                        }
                        giveall.saveItems(items);
                    }
                }.runTaskLater(ms, 1L);
            }
            return;
        } else if (m == Material.BOOK) {
            if (inv.getTitle().contains("Statistiken") == false) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                openStatsInv(p);
            } else {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            }
            return;
        } else if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("event");
            return;
        } else if (m == Material.STAINED_CLAY) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        joinEvent(p);
        if (ev == null || ev.leaveInventoryOpen == false) p.closeInventory();
    }

    public void quitEvent(final Player p, final boolean msg) {
        if (ev == null) {
            if (msg) ms.showItemBar(p, "§cEs läuft derzeit kein Event.");
            return;
        } else if (ev.players.remove(p) == false) {
            if (msg) ms.showItemBar(p, "§cDu nimmst derzeit an keinem Event teil.");
            return;
        }
        ev.quit(p);
        if (msg) ms.showItemBar(p, "§cDu hast das Event verlassen.");
        if (ev.scoreboard && ev.getSB() != null) {
            ev.players.forEach(player -> {
                if (player != null && player.isOnline()) ms.sendScoreboard(player, ev.getSB());
            });
        }
    }

    public Event inEvent(final Player p) {
        if (ev != null && ev.players.contains(p) && ev.chatOnly == false) return ev;
        else return null;
    }

    public boolean isRunning() {
        return ev != null;
    }

    public boolean isChatEvent() {
        return ev != null && ev.chatOnly;
    }

    public short getExpBoost() {
        return exp.expboost;
    }

    public void setExpBoost(final byte expboost, final boolean byplayer) {
        exp.setEXPBoost(expboost, byplayer);
    }

    public String getHolidayAnnouncement() {
        return dropsearch.getAnnouncement();
    }

    private void joinEvent(final Player p) {
        if (ev == null) {
            ms.showItemBar(p, "§cEs läuft derzeit kein Event.");
            return;
        } else if (ev.players.contains(p)) {
            ms.showItemBar(p, "§cDu nimmst bereits am Event teil.");
            return;
        } else if (ev.chatOnly == false) {
            if (ms.inBattle(p.getName(), p.getLocation())) {
                ms.showItemBar(p, ms.hnotinfight);
                return;
            } else if (ms.inWar(p)) {
                ms.showItemBar(p, "§cDu bist derzeit in einem ClanWar.");
                return;
            } else if (ms.in1vs1(p)) {
                ms.showItemBar(p, "§cDu bist derzeit in einem 1vs1.");
                return;
            } else if (ms.inDuel(p)) {
                ms.showItemBar(p, "§cDu bist derzeit in einem Duell.");
                return;
            } else if (ms.inGame(p)) {
                ms.showItemBar(p, "§cDu spielst gerade ein Minigame.");
                return;
            } else if (ms.isBeingTeleported(p.getName())) {
                ms.showItemBar(p, "§cDu wirst gerade teleportiert.");
                return;
            } else if (ev.isPvP() && ms.getMuxUser(p.getName()).isPvPbanned() && ev.canjoin) {
                ms.showItemBar(p, "§cDu kannst mit einer PvP Sperre nicht beitreten.");
                return;
            } else if (ms.inCasino(p)) {
                ms.getCasino().handleQuit(p, true);
                joinEvent(p);
                return;
            }
            if (p.isInsideVehicle()) {
                p.leaveVehicle();
            }
        }
        if (ev.canjoin == false) {
            if (ev.canSpectate) {
                if (p.getGameMode() == GameMode.SPECTATOR) {
                    ms.showItemBar(p, "§cDu bist schon im Zuschauermodus.");
                    return;
                }
                spectate(p);
                return;
            }
            ms.showItemBar(p, "§cDu kannst dem Event nicht mehr beitreten.");
            return;
        } else if (ev.getMaxPlayers() != -1 && ev.players.size() >= ev.getMaxPlayers()) {
            ms.showItemBar(p, "§cDas Event ist voll.");
            return;
        }
        final Location current = p.getLocation();
        if (ev.join(p) == false) {
            return;
        }
        if (ev.chatOnly == false && ev.noJoinMSG == false) {
            p.sendMessage(new String[]{
                    "",
                    ms.header((byte) 11, "§d"),
                    "  §d§l" + ev.getName(),
                    "",
            });
            for (final String text : ev.getDescription()) {
                if (text.equals("")) break;
                p.sendMessage("  " + text);
            }
            if (ev.isStartedByPlayer()) {
                p.sendMessage("");
                p.sendMessage("  §8§oBei diesem Event gibt es keine MuxCoins.");
            }
            p.sendMessage(ms.footer((byte) 11, "§d"));
        }
        if (ev.chatOnly == false) ms.setLastLocation(p, current);
        ms.showItemBar(p, "§fDu nimmst nun am §dEvent §fteil.");
        ev.players.add(p);
        ms.getAnalytics().getAnalytics().getEventPlayers().add(p.getUniqueId());
        if (ev.scoreboard && ev.getSB() != null) ev.players.forEach(player -> ms.sendScoreboard(player, ev.getSB()));
    }

    private void stopEvent(final Player p, final boolean rapid) {
        if (ev == null) return;
        if (p != null && ev.isStartedByPlayer() == false) {
            ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "EVENT", ev.getName() + " STOP");
        }
        ev.stopEvent(rapid, true);
    }

    public boolean startEvent(final Player p, final Event event) {
        if (ev != null) {
            if (p != null) ms.showItemBar(p, "§cEs läuft derzeit noch das §7'" + ev.getName() + "'§c Event.");
            return false;
        } else if (event == null) {
            if (p != null) ms.showItemBar(p, "§cDas Event existiert nicht.");
            return false;
        } else if (event == custom && custom.getName().contains(" ")) {
            if (p != null) ms.showItemBar(p, "§cSetze zuerst ein Namen für das Event.");
            return false;
        }
        ev = event;
        ev.startEvent();
        ev.startedByPlayer = p != null;
        if (ev instanceof Listener) {
            ms.getServer().getPluginManager().registerEvents((Listener) ev, ms);
        }

        if (p != null)
            ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "EVENT", ev.getName());
        if (p != null) ms.showItemBar(p, "§fDas Event wurde §agestartet§f.");
        return true;
    }

    public void checkSpectators() {
        if (ev != null) ev.checkSpectators();
    }

    public void spectate(final Player p) {
        Location l = ev.getSpectateLocation();
        if (l == null) {
            return;
        }
        l = l.clone().add(0D, 1D, 0D);
        ms.getMounts().deactivateMount(p);
        ms.getPets().deactivatePet(p);
        ms.setLastLocation(p);
        ms.forcePlayer(p, l);
        p.playSound(l, Sound.ENDERMAN_TELEPORT, 0.2F, 1F);
        p.setGameMode(GameMode.SPECTATOR);
        ev.getSpectators().put(p.getUniqueId(), l);
        ms.showItemBar(p, "§fDu wurdest zum §dEvent §fteleportiert.");
    }

    private void setAdminItem(final int pos, final Inventory inv, final Event e, final boolean advanced) {
        final List<String> admininfo = new ArrayList<>(Arrays.asList(e.getAdminInformation()));
        admininfo.add(0, "");
        ItemStack item = ms.renameItemStack(e.getItem().clone(), "§d§l" + e.getName(), e.getDescription());
        if (admininfo.size() > 1) item = ms.addLore(item, admininfo);
        if (advanced) {
            inv.setItem(pos, ms.addLore(item, (ev != null && ev.isStopping()) ? new String[]{"", "§cDas Event wird gerade gestoppt..."} : ev == e ?
                    new String[]{"", "§7Linksklick, um dieses Event zu §cbeenden§7.", "§7Rechtsklick, um das Event zu §6verändern§7."} :
                    new String[]{"", "§7Linksklick, um dieses Event zu §astarten§7.", "§7Rechtsklick, um das Event §6einzurichten§7."}));
        } else {
            inv.setItem(pos, ms.addLore(item, "", (ev != null && ev.isStopping()) ? "§cDas Event wird gerade gestoppt..." : ev == e ? "§7Klicke, um dieses Event zu §cbeenden§7." : "§7Klicke, um dieses Event zu §astarten§7."));
        }
    }

    private void setInfoItem(final int pos, final Inventory inv, final Event e) {
        final ItemStack item = ms.renameItemStack(e.getItem().clone(), "§d§l" + e.getName());
        inv.setItem(pos, ms.setLore(item, "§7Ausgaben: §e" + ms.getLFormat(ms.getDB().getEventCoins(e.getName())) + " MuxCoins"));
    }

    enum Team {
        BLUE, RED
    }

    enum EventCategory {
        CHAT, NORMAL, BIG
    }

    class Effects {
        private int step = 0;
        private boolean color = true;
        private final List<ArmorStand> entitys = new ArrayList<>();
        private final Map<MuxLocation, Object[]> blocks = new HashMap<>();
        private final Location loc = new Location(ms.getGameSpawn().getWorld(), 19.5D, 77D, -0.5D);
        private BukkitTask tagtimer;

        private void spawnAt(final Location loc) {
            final ArmorStand a = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            final net.minecraft.server.v1_8_R3.Entity nms = ((CraftEntity) a).getHandle();
            NMSReflection.setObject(Entity.class, "uniqueID", nms, ms.getIdentificationId());
            NMSReflection.setObject(Entity.class, "invulnerable", nms, true);
            a.setGravity(false);
            a.setVisible(false);
            a.setHelmet(new ItemStack(Material.IRON_BLOCK));
            entitys.add(a);
        }

        private void onUpdate() {
            if (step != 200) {
                if (step < entitys.size()) {
                    final ArmorStand a = entitys.get(step);
                    if (color) {
                        a.setHelmet(new ItemStack(Material.IRON_BLOCK));
                    } else {
                        a.setHelmet(new ItemStack(Material.WOOL, 1, (short) 10));
                    }
                    step++;
                } else {
                    step = 200;
                    color ^= true;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            step = 0;
                        }
                    }.runTaskLater(ms, 200L);
                }
            }
            ms.playEffect(EnumParticle.PORTAL, loc.clone().add(0, randomDouble(0.1, 2), 0), 0F, 0F, 0F, 1F, 20);
            ms.playEffect(EnumParticle.ENCHANTMENT_TABLE, loc.clone(), 2.2F, 0.1F, 2.2F, 1F, 20); // eventually speed 0.0001F
            ms.playEffect(EnumParticle.SPELL_WITCH, loc.clone(), 2.2F, 0.1F, 2.2F, 0.0001F, 10);
        }

        private double randomDouble(final double min, final double max) {
            return Math.random() < 0.5D ? (1.0D - Math.random()) * (max - min) + min : Math.random() * (max - min) + min;
        }

        public void disable() {
            for (final ArmorStand a : entitys) {
                a.remove();
            }
            for (final Entry<MuxLocation, Object[]> entry : blocks.entrySet()) {
                final Object[] obj = entry.getValue();
                final Block b = entry.getKey().getLocation().getBlock();
                b.setType((Material) obj[0]);
                b.setData((byte) obj[1]);
                final BlockState bs = b.getState(), bs2 = (BlockState) obj[2];
                if (bs2 instanceof Sign) {
                    final Sign targetSign = (Sign) bs, sourceSign = (Sign) bs2;
                    final String[] lines = sourceSign.getLines();
                    for (int i = 0; i < lines.length; i++) {
                        targetSign.setLine(i, lines[i]);
                    }
                    targetSign.update();
                } else if (bs2 instanceof Skull) {
                    final Skull targetSkull = (Skull) bs, sourceSkull = (Skull) bs2;
                    targetSkull.setOwner(sourceSkull.getOwner());
                    targetSkull.setRotation(sourceSkull.getRotation());
                    targetSkull.setSkullType(sourceSkull.getSkullType());
                    targetSkull.update();
                }
            }
            blocks.clear();
            entitys.clear();
            if (tagtimer != null) tagtimer.cancel();
        }

        public void enable() {
            final Location l = loc.clone().add(-1.5, 5, 5.5);
            // E
            spawnAt(l.clone().add(0, 0, 0));
            spawnAt(l.clone().add(0, 0.6, 0));
            spawnAt(l.clone().add(0, 1.2, 0));
            spawnAt(l.clone().add(0, 1.8, 0));
            spawnAt(l.clone().add(0, 2.4, 0));
            spawnAt(l.clone().add(0, 0, -0.6));
            spawnAt(l.clone().add(0, 0, -1.2));
            spawnAt(l.clone().add(0, 1.2, -0.6));
            spawnAt(l.clone().add(0, 2.4, -0.6));
            spawnAt(l.clone().add(0, 2.4, -1.2));

            // V
            spawnAt(l.clone().add(0, 0.6, -2.4));
            spawnAt(l.clone().add(0, 1.2, -2.4));
            spawnAt(l.clone().add(0, 1.8, -2.4));
            spawnAt(l.clone().add(0, 2.4, -2.4));
            spawnAt(l.clone().add(0, 0, -3));
            spawnAt(l.clone().add(0, 0.6, -3.6));
            spawnAt(l.clone().add(0, 1.2, -3.6));
            spawnAt(l.clone().add(0, 1.8, -3.6));
            spawnAt(l.clone().add(0, 2.4, -3.6));

            // E
            spawnAt(l.clone().add(0, 0, -0.6 - 4.2));
            spawnAt(l.clone().add(0, 0.6, -0.6 - 4.2));
            spawnAt(l.clone().add(0, 1.2, -0.6 - 4.2));
            spawnAt(l.clone().add(0, 1.8, -0.6 - 4.2));
            spawnAt(l.clone().add(0, 2.4, -0.6 - 4.2));
            spawnAt(l.clone().add(0, 0, -0.6 - 0.6 - 4.2));
            spawnAt(l.clone().add(0, 0, -1.2 - 0.6 - 4.2));
            spawnAt(l.clone().add(0, 1.2, -0.6 - 0.6 - 4.2));
            spawnAt(l.clone().add(0, 2.4, -0.6 - 4.2 - 0.6));
            spawnAt(l.clone().add(0, 2.4, -1.2 - 4.2 - 0.6));

            // N
            spawnAt(l.clone().add(0, 0, -0.6 - 0.6 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 0.6, -0.6 - 0.6 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 1.2, -0.6 - 0.6 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 1.8, -0.6 - 0.6 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 2.4, -0.6 - 0.6 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 1.8, -0.6 - 0.6 - 2.4 - 4.2));
            spawnAt(l.clone().add(0, 1.2, -0.6 - 0.6 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 0, -0.6 - 0.6 - 0.6 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 0.6, -0.6 - 0.6 - 0.6 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 1.2, -0.6 - 0.6 - 0.6 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 1.8, -0.6 - 0.6 - 0.6 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 2.4, -0.6 - 0.6 - 0.6 - 1.2 - 1.8 - 4.2));

            // T
            spawnAt(l.clone().add(0, 2.4, -0.6 - 0.6 - 0.6 - 1.2 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 0, -0.6 - 0.6 - 0.6 - 1.8 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 0.6, -0.6 - 0.6 - 0.6 - 1.8 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 1.2, -0.6 - 0.6 - 0.6 - 1.8 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 1.8, -0.6 - 0.6 - 0.6 - 1.8 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 2.4, -0.6 - 0.6 - 0.6 - 1.8 - 1.2 - 1.8 - 4.2));
            spawnAt(l.clone().add(0, 2.4, -0.6 - 0.6 - 0.6 - 1.2 - 1.2 - 1.2 - 1.8 - 4.2));

            tagtimer = new BukkitRunnable() {
                @Override
                public void run() {
                    onUpdate();
                }
            }.runTaskTimer(ms, 0L, 1L);
        }
    }
}