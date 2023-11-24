package me.muxteam.basic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MuxTimings {
    private final MuxSystem ms;

    private boolean enabled = false;
    private boolean smartTimings = false;

    private final ConcurrentMap<String, Timing> timings = new ConcurrentHashMap<>();
    private final Set<Player> playersWaitingForTimings = new HashSet<>();
    private boolean reporting = false;

    public MuxTimings(final MuxSystem ms) {
        this.ms = ms;
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (p.hasPermission("muxsystem.developer") == false) {
            ms.sendNoCMDMessage(p);
            return true;
        }
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("enable")) {
                if (enabled) {
                    ms.showItemBar(p, "§fTimings sind bereits aktiviert.");
                    if (smartTimings) {
                        if (playersWaitingForTimings.contains(p)) {
                            ms.showLater(p, "§6Smarttimings ist aktiv! §fDu erhältst den Report wenn diese abgeschlossen sind.");
                            return true;
                        }
                        ms.showLater(p, "§6Smarttimings ist aktiv! §fFalls du den Report sehen willst schreibe §4!report §fin den Chat.");
                        final AtomicBoolean inInput = new AtomicBoolean(true);
                        new MuxChatInput(this.ms, (input, player) -> {
                            inInput.set(false);
                            if (input.equalsIgnoreCase("!report")) {
                                this.playersWaitingForTimings.add(player);
                                ms.showItemBar(player, "§fDu erhältst den Report wenn dieser abgeschlossen ist.");
                                return;
                            }
                            ms.showItemBar(p, "§cEingabe abgebrochen.");
                        }).show(p, "§c§lTimings>§7 Falls du den Report sehen willst schreibe §c!report §7in den Chat.");
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (inInput.get()) {
                                    p.sendMessage("§c§lTimings>§7 Eingabe beendet."); // in chat so you see it
                                    ms.getChat().removeChatInput(p.getName());
                                }
                            }
                        }.runTaskLater(this.ms, 20L * 30L);
                    }
                    return true;
                }
                final boolean smart = args.length == 1 || args[1].equalsIgnoreCase("false") == false;
                this.enabled = true;
                ms.showItemBar(p, "§fTimings aktiviert!");
                if (smart) {
                    ms.showLater(p, "§6Smarttimings ist aktiv! §fDu erhältst den Report wenn diese abgeschlossen sind.");
                    this.playersWaitingForTimings.add(p);
                    this.smartTimings = true;
                    int minutes = 0;
                    if (args.length > 1 && ms.isNumeric(args[1])) minutes = Integer.parseInt(args[1]);
                    if (args.length > 2 && ms.isNumeric(args[2])) minutes = Integer.parseInt(args[1]);
                    if (minutes < 1) minutes = 5;
                    reporting = true;
                    reset();
                    reporting = false;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            enabled = false;
                            playersWaitingForTimings.stream()
                                    .filter(wp -> {
                                        if (
                                                wp.isOnline() == false
                                                        || ms.inBattle(wp.getName(), wp.getLocation())
                                                        || ms.inEvent(wp)
                                                        || ms.inGame(wp)
                                                        || ms.in1vs1(wp)
                                                        || ms.inDuel(wp)
                                                        || ms.getInv().getInvs().containsKey(wp.getName())
                                                        || ms.inWar(wp)
                                                        || wp.getGameMode() == GameMode.SPECTATOR
                                        ) {
                                            if (wp.isOnline())
                                                wp.sendMessage("§c§lTimings> §6Smarttimings Report ist fertig! §7Benutze §c/muxtimings §7um dir die Timings anzusehen.");
                                            return false;
                                        }
                                        return true;
                                    })
                                    .forEach(wp -> {
                                        wp.setMetadata("mstimingspage", new FixedMetadataValue(ms, 0));
                                        openGeneralInventory(wp);
                                    });
                            playersWaitingForTimings.clear();
                        }
                    }.runTaskLater(ms, 20L * 60 * minutes);
                } else this.smartTimings = false;
            } else if (args[0].equalsIgnoreCase("disable")) {
                if (this.smartTimings) {
                    ms.showItemBar(p, "§fSmarttimings sind aktiv! Timings werden §aautomatisch §fbeendet.");
                    return true;
                }
                this.enabled = false;
            } else if (args[0].equalsIgnoreCase("reset")) {
                reporting = true;
                reset();
                ms.showItemBar(p, "§fTimings wurden zurückgesetzt.");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        reporting = false;
                    }
                }.runTaskLater(ms, 20);
            } else {
                final String name = args[0];
                final Timing timing = this.ofNullable(name);
                if (timing == null) {
                    ms.showItemBar(p, "§cEs wurde kein Timing mit diesem Namen gefunden.");
                    return true;
                }
                openDetailedTimingsReport(p, timing);
            }
            return true;
        }
        if (this.timings.isEmpty()) {
            ms.showItemBar(p, "§cEs wurden bis jetzt keine Timings reportet.");
            return true;
        }
        p.setMetadata("mstimingspage", new FixedMetadataValue(ms, 0));
        openGeneralInventory(p);
        return true;
    }

    private void openGeneralInventory(final Player p) {
        reporting = true;
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxTimings§0 | Report");
        final Pageifier<ItemStack> pages = new Pageifier<>(36);
        for (final Timing timing : ImmutableSet.copyOf(this.timings.values())) {
            pages.addItem(ms.renameItemStack(new ItemStack(Material.PAPER), "§f§l" + timing.getName(),
                    "§7Average: §c" + timing.getAvg() + " ms",
                    "§7Min: §c" + timing.getMin() + " ms",
                    "§7Max: §c" + timing.getMax() + " ms",
                    "§7Count: §c" + timing.getCount(),
                    "",
                    "§aKlicke§7, um den kompletten Bericht anzusehen."));
        }
        final int page = p.getMetadata("mstimingspage").get(0).asInt();
        ms.addPageButtons(inv, page, pages);
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.TIMINGS)
            ms.setActiveInv(p.getName(), MuxInventory.InvType.TIMINGS);
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.TIMINGS);
        reporting = false;
    }

    private void openDetailedTimingsReport(final Player player, final Timing timing) {
        reporting = true;
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxTimings§0 | Report");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.PAPER), "§f§l" + timing.getName(),
                "§7Average: §c" + timing.getAvg() + " ms",
                "§7Min: §c" + timing.getMin() + " ms",
                "§7Max: §c " + timing.getMax() + " ms",
                "§7Count: §c" + timing.getCount(),
                "",
                "§aKlicke§7, um dir den Bericht als hastebin anzeigen zu lassen."));
        if (player.hasMetadata("msdetailedtimingspage") == false)
            player.setMetadata("msdetailedtimingspage", new FixedMetadataValue(ms, 0));
        final int page = player.getMetadata("msdetailedtimingspage").get(0).asInt();
        final Pageifier<ItemStack> pages = new Pageifier<>(36);
        for (final Long millis : timing.getSorted()) {
            pages.addItem(this.ms.renameItemStack(new ItemStack(Material.BOOK), "§f§l" + millis + " ms"));
        }
        ms.addPageButtons(inv, page, pages);
        if (ms.getInv().getInv(player.getName()) != MuxInventory.InvType.TIMINGS)
            ms.setActiveInv(player.getName(), MuxInventory.InvType.TIMINGS);
        player.openInventory(inv);
        ms.setActiveInv(player.getName(), MuxInventory.InvType.TIMINGS);
        reporting = false;
    }

    public void handleInventory(final Player player, final ItemStack item, Inventory inv, final int slot) {
        if (item == null || item.getType() == Material.AIR) return;
        if (inv.getItem(0) != null) {
            this.handleDetailedInventory(player, item, inv, slot);
            return;
        }
        final Material m = item.getType();
        if (m == Material.SKULL_ITEM) {
            int page = player.getMetadata("mstimingspage").get(0).asInt();
            player.removeMetadata("mstimingspage", ms);
            if (slot == 8)
                page++;
            else
                page--;
            player.setMetadata("mstimingspage", new FixedMetadataValue(ms, page));
            player.playSound(player.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
            openGeneralInventory(player);
        } else if (m == Material.PAPER) {
            final Timing timing = this.ofNullable(ChatColor.stripColor(item.getItemMeta().getDisplayName()));
            if (timing == null) { // if someone resets timings while player is in inventory
                ms.showItemBar(player, "§cEs ist ein Fehler aufgetreten.");
                player.playSound(player.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                player.closeInventory();
                return;
            }
            player.playSound(player.getLocation(), Sound.CLICK, 1F, 1F);
            player.removeMetadata("msdetailedtimingspage", ms);
            openDetailedTimingsReport(player, timing);
        }
    }

    private void handleDetailedInventory(final Player player, final ItemStack item, final Inventory inv, final int slot) {
        final Material m = item.getType();
        final Timing timing = this.ofNullable(ChatColor.stripColor(inv.getItem(4).getItemMeta().getDisplayName()));
        if (timing == null) { // if someone resets timings while player is in inventory
            ms.showItemBar(player, "§cEs ist ein Fehler aufgetreten.");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            player.closeInventory();
            return;
        }
        switch (m) {
            case SKULL_ITEM:
                player.playSound(player.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                int page = player.getMetadata("msdetailedtimingspage").get(0).asInt();
                player.removeMetadata("msdetailedtimingspage", ms);
                if (slot == 8)
                    page++;
                else
                    page--;
                player.setMetadata("msdetailedtimingspage", new FixedMetadataValue(ms, page));
                player.playSound(player.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openDetailedTimingsReport(player, timing);
                break;
            case ITEM_FRAME:
                player.playSound(player.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openGeneralInventory(player);
                break;
            case PAPER:
                if (player.hasMetadata("mstimingsstacktrace")) {
                    ms.showItemBar(player, "§fStacktrace wird bereits vorbereitet.");
                    break;
                }
                player.setMetadata("mstimingsstacktrace", new FixedMetadataValue(ms, true));
                ms.showItemBar(player, "§fBereite Stacktrace vor..");

                ms.getForkJoinPool().execute(() -> {
                    try {
                        String nl = System.getProperty("line.separator");
                        final List<String> strList = new ArrayList<>();
                        strList.add("Timingbericht für " + timing.getName() + ":" + nl + nl);
                        strList.add("Avg Timings: " + timing.getAvg() + "ms" + nl);
                        strList.add("Min Timings: " + timing.getMin() + "ms" + nl);
                        strList.add("Max Timing: " + timing.getAvg() + "ms" + nl);
                        strList.add("Count: " + timing.getCount() + nl);
                        strList.add("Komplette Timings: " + ImmutableList.copyOf(timing.getSorted()).stream().map(aLong -> aLong + "ms").collect(Collectors.joining(", ")) + nl + nl);
                        strList.add("Stacktrace des Timings " + timing.getName() + ":"+ nl);
                        strList.addAll(Arrays.stream(timing.getStackTrace()).filter(stackTraceElement -> stackTraceElement.getClassName().toLowerCase().contains("timing") == false).map(stackTraceElement -> stackTraceElement + nl).collect(Collectors.toList()));
                        player.sendMessage("§a" + postToHastebin(strList.toArray(new String[0])));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.closeInventory();
                                player.removeMetadata("mstimingsstacktrace", ms);
                            }
                        }.runTask(ms);
                    } catch (IOException e) {
                        Stacktrace.print(e);
                        e.printStackTrace();
                    }
                });
                break;
            case BOOK:
                break;
            default:
                player.playSound(player.getLocation(), Sound.CLICK, 1F, 1F);
        }
    }

    public Timing of(final String name) {
        return timings.computeIfAbsent(name, Timing::new);
    }

    public Timing ofNullable(final String name) {
        return this.timings.get(name);
    }

    public Timing ofAndStart(final String name) {
        if (enabled == false || reporting) return null;
        final Timing timing = this.of(name);
        timing.start();
        return timing;
    }

    public void stop(final String name) {
        if (enabled == false || reporting) return;
        final Timing timing = this.ofNullable(name);
        if (timing == null) return;
        timing.stop();
    }

    public void reset() {
        boolean enable = this.enabled;
        this.enabled = false;
        this.timings.values().forEach(Timing::clear);
        if (enable) this.enabled = true;
    }

    public void close() {
        this.timings.clear();
    }

    private String postToHastebin(final String[] text) throws IOException {
        int postDataLength = 0;
        for (final String t : text)
            postDataLength += t.getBytes(StandardCharsets.UTF_8).length;

        final JSONObject object = new JSONObject();
        object.put("title", "MuxTimings");
        final StringBuilder sb = new StringBuilder();
        for (final String s : text) {
            sb.append(s);
        }
        object.put("content", sb.toString());

        final String requestURL = "https://springpaste-production.up.railway.app/paste"; // vantrex pasteservice 24/7 up
        final URL url = new URL(requestURL);
        final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("User-Agent", "MuxCraftSystem");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);

        try (final OutputStream wr = conn.getOutputStream()) {
            for (byte aByte : object.toString().getBytes(StandardCharsets.UTF_8)) {
                wr.write(aByte);
            }
            wr.flush();
        } catch (final IOException e) {
            Stacktrace.print(e);
            return null;
        }
        String response;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            response = reader.readLine();
        }
        JSONObject responseAsJson = new JSONObject(response);
        if (responseAsJson.has("id")) {
            final String postURL = "https://paste.vantrex.de/";
            return postURL + responseAsJson.getString("id");
        }

        return null;
    }

    public class Timing {
        private final String name;
        private final List<Long> timings = new ArrayList<>();
        private long start;
        private boolean running, cleanup;
        private StackTraceElement[] stackTrace;

        public Timing(final String name) {
            this.name = name;
        }

        public void start() {
            if (running) return;
            this.running = true;
            this.start = System.currentTimeMillis();
        }

        public void stop() {
            if (cleanup) return;
            final long millis = System.currentTimeMillis() - this.start;
            this.running = false;
            this.timings.add(millis);
            if (stackTrace == null) stackTrace = Thread.currentThread().getStackTrace();
            if (Bukkit.isPrimaryThread() && millis > 10) {
                ms.broadcastMessage("§4§lMethode " + this.name + " hat " + millis + "ms auf dem Mainthread gebraucht!", "muxsystem.developer", MuxSystem.Priority.NORMAL);
            }
        }

        public void clear() {
            cleanup = true;
            this.timings.clear();
            cleanup = false;
        }

        public String getName() {
            return name;
        }

        public StackTraceElement[] getStackTrace() {
            return stackTrace;
        }

        public long getMin() {
            return this.timings.stream().min(Comparator.comparingLong(value -> value)).orElse(-1L);
        }

        public long getMax() {
            return this.timings.stream().max(Comparator.comparingLong(value -> value)).orElse(-1L);
        }

        public long getAvg() {
            if (this.timings.isEmpty()) return 0L;
            return this.timings.stream().mapToLong(value -> value).sum() / this.timings.size();
        }

        public List<Long> getSorted() {
            return this.timings.stream().sorted(Comparator.comparingLong(value -> value)).collect(Collectors.toList());
        }

        public int getCount() {
            return this.timings.size();
        }
    }
}