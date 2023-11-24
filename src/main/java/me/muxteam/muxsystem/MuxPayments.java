package me.muxteam.muxsystem;

import me.muxteam.pvp.MuxPvP;
import me.muxteam.ranks.MuxRanks;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MuxPayments {
    private MuxSystem ms;

    private final Set<UUID> paymentmsg = new HashSet<>();
    private final Set<String> usedTransactionIds = new HashSet<>();

    public MuxPayments(final MuxSystem ms) {
        this.ms = ms;
    }

    public void close() {
        this.ms = null;
    }

    public void checkPayments() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePaymentSigns();
            }
        }.runTaskAsynchronously(ms);
    }

    public long getLastPurchaseTime(final String username) {
        try {
            return ms.getDB().getLastPaymentTime(ms.getPlayer(username).getUniqueId());
        } catch (final Exception e) {
            return -1;
        }
    }

    public boolean handleCommand(final CommandSender sender, final String label, final String[] args) {
        if (sender instanceof Player) {
            ms.sendNoCMDMessage((Player) sender);
            return true;
        } else if (label.equalsIgnoreCase("chargeback")) {
            handleChargebackCommand(args);
            ms.getAffiliate().handleChargebackCommand(args);
        } else {
            handlePaymentCommand(args);
            ms.getAffiliate().handlePaymentCommand(args);
        }
        return true;
    }

    public boolean handlePaySafeCommand(final Player p, final String[] args) {
        if (ms.checkGeneralCooldown(p.getName(), "CMDPSC", 3000L, true)) {
            ms.showItemBar(p, ms.getLang("notsofast"));
            return true;
        }
        final Map<String, Integer> codes = ms.getDB().getPaySafeCodes(p.getUniqueId());
        if (args.length < 2) {
            if (codes != null && codes.size() > 0) {
                p.sendMessage("§f§lDeine PaySafeCards in Bearbeitung");
                for (final Map.Entry<String, Integer> entry : codes.entrySet()) {
                    p.sendMessage("§f" + entry.getKey().replaceFirst("(\\d{4})(\\d{4})(\\d{4})(\\d{4})", "$1-$2-$3-$4") + " - §e" + entry.getValue() + " EUR");
                }
            }
            ms.showItemBar(p, ms.usage("/paysafecard [wert] [code]"));
            return true;
        } else {
            final String code = args[1].replace("-", "").replace(" ", "").toUpperCase();
            if (code.length() != 16) {
                ms.showItemBar(p, "§cDer PaySafeCard Code muss 16-stellig sein.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return true;
            } else if (code.matches("^[0-9\\-\\s]+$") == false) {
                ms.showItemBar(p, "§cDer PaySafeCard Code darf nur Zahlen enthalten.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return true;
            }
            int value;
            try {
                value = Integer.parseInt(args[0]);
            } catch (final NumberFormatException ex) {
                ms.showItemBar(p, ms.hnonumber);
                return true;
            }
            if (codes != null && codes.containsKey(code)) {
                ms.showItemBar(p, "§cDu hast bereits den Code gesendet.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return true;
            } else if (ms.checkLimit(p.getName(), "PSCADD", 15, true)) {
                ms.showItemBar(p, "§cLimit erreicht. Versuche es in paar Stunden wieder.");
                return true;
            }
            ms.getDB().addPaySafeCode(p.getUniqueId(), p.getName(), code, value);
            ms.getForkJoinPool().execute(() -> {
                try {
                    telegram(p.getName() + " - €" + value);
                    telegram(code);
                } catch (final Exception ignored) {
                }
            });
            ms.showItemBar(p, "§aDu hast den Code erfolgreich gesendet.");
            p.sendMessage("§f§lBEHALTE DEINE PAYSAFECARD! §fSollte der Code ungültig sein, wird die Zahlung abgelehnt.");
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
        }
        return true;
    }

    public void telegram(final String text) throws URISyntaxException, IOException {
        final String key = ms.getConfig().getString("telegrambot");
        final URIBuilder query = new URIBuilder("https://api.telegram.org").setPath("/bot" + key + "/sendMessage").
                addParameter("chat_id", "-1001223760459").addParameter("text", text);
        final CloseableHttpClient client = HttpClients.createDefault();
        try (final CloseableHttpResponse response = client.execute(new HttpGet(query.build()))) {
            final HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
        }
    }

    public void handlePaymentCommand(final String[] args) {
        final Server sr = ms.getServer();
        String u = args[0];
        if (u.contains("-") == false) u = u.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        final UUID uuid = UUID.fromString(u);
        final Player p = sr.getPlayer(uuid);
        if (args.length == 1 && p != null) {
            if (paymentmsg.contains(uuid)) {
                return;
            }
            paymentmsg.add(uuid);
            ms.getGames().leaveActiveGame(p);
            ms.getCasino().handleQuit(p, true);
            if (ms.inWar(p)) ms.getClanWar().handleDeath(p, false);
            final MuxPvP.RankedArena rankedArena = ms.getPvP().get1vs1Arena(p);
            final MuxPvP.Arena arena = ms.getPvP().getArena(p);
            if (rankedArena != null) rankedArena.clearArena(false, true, true, false);
            if (arena != null) arena.clearArena(false, true, true);
            if (ms.inEvent(p)) ms.getEvents().quitEvent(p, false);
            ms.getChat().disableChat(p.getName());
            p.sendMessage(new String[]{"§a▛▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▜", "  " + ms.getLang("donation"), "", "  " + ms.getLang("donatethanks1"), "  " + ms.getLang("donatethanks2"), ""});
            new BukkitRunnable() {
                @Override
                public void run() {
                    final Firework fw = (Firework) p.getWorld().spawnEntity(p.getLocation(), EntityType.FIREWORK);
                    final FireworkMeta fmeta = fw.getFireworkMeta();
                    fmeta.addEffect(FireworkEffect.builder().flicker(true).with(FireworkEffect.Type.CREEPER).trail(true).withColor(Color.LIME).withFade(Color.GREEN).build());
                    fmeta.setPower(1);
                    fw.setFireworkMeta(fmeta);
                    p.sendMessage("§a▙▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▟");
                    paymentmsg.remove(uuid);
                    ms.getChat().enableChat(p.getName());
                }
            }.runTaskLater(ms, 60L);
            return;
        }
        if (usedTransactionIds.contains(args[1])) return;
        final String packages = ms.fromArgs(args, 3).replace("1x ", ""), price = args[2], transactionID = args[1];
        ms.getHistory().addPaymentHistory(uuid, null, "PAYMENT", packages, price + " 0 " + transactionID, System.currentTimeMillis());
        usedTransactionIds.add(transactionID);
    }

    public boolean handleRankUpgradeCommand(final CommandSender sender, final String[] args) {
        if (sender instanceof Player) {
            ms.sendNoCMDMessage((Player) sender);
            return true;
        }
        final Server sr = ms.getServer();
        String u = args[0];
        double price = Double.parseDouble(args[1].replace(",", "."));
        if (u.contains("-") == false) u = u.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        final UUID uuid = UUID.fromString(u);
        final MuxRanks.PermissionsUser permUser = ms.getPerms().getUserData(uuid);
        final String group = permUser == null ? null : permUser.getGroup();
        Map.Entry<RankPrices, Double> nextRank = getNextRank(group, price);
        CompletableFuture.supplyAsync(() -> ms.getShared().getNameFromUUID(uuid))
                .thenAccept(name -> new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (nextRank.getKey() != null)
                            ms.getPerms().changePlayerGroup(uuid, name, nextRank.getKey().getGroupName(), sr.getConsoleSender());
                        final double leftOver = nextRank.getValue();
                        if (leftOver <= 0) return; // should never be < 0 tho

                        final int gems = (int) (leftOver / (5D / 1000D));
                        final Player p = sr.getPlayer(uuid);
                        final MuxUser user = ms.getMuxUser(name) == null ? ms.getDB().loadPlayer(uuid, true) : ms.getMuxUser(name);
                        if (user == null) return;
                        user.addGems(gems);
                        ms.getForkJoinPool().execute(() -> ms.getDB().savePlayer(user));
                        if (p != null) {
                            p.sendMessage("  §7Du hast §a" + ms.getNumberFormat(gems) + " MuxGems§7 erhalten.");
                        }
                    }
                }.runTask(ms));
        return true;
    }

    public void handleChargebackCommand(final String[] args) {
        final String transactionID = args[1];
        String u = args[0];
        if (u.contains("-") == false) u = u.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        final UUID uuid = UUID.fromString(u);
        ms.getDB().changePaymentStatus(uuid, transactionID, "1");
    }

    private void updatePaymentSigns() {
        final List<String> entries = ms.getDB().getLastPayments(5);
        if (entries.isEmpty()) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                final World w = ms.getGameSpawn().getWorld();
                for (int i = 0; i < 5; i++) {
                    try {
                        final Sign sign = (Sign) new Location(w, 8D - i, 80D, 27D).getBlock().getState();
                        sign.setLine(1, entries.get(i));
                        sign.update();
                    } catch (final Exception e) {
                        System.err.println("MuxSystem> Could not update payment signs.");
                    }
                }
            }
        }.runTaskLater(ms, 0L);
    }

    public RankPrices getRankFromGroup(final String group) {
        if (group == null) return RankPrices.NONE;
        try {
            return RankPrices.valueOf(group.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RankPrices.ULTRA;
        }
    }

    public strictfp Map.Entry<RankPrices, Double> getNextRank(final String group, final double money) {
        RankPrices current = getRankFromGroup(group);
        if (current == null) current = RankPrices.NONE;
        RankPrices result = null;
        double leftoverMoney = money;
        final RankPrices[] rankArray = RankPrices.values();
        for (int i = current.ordinal() + 1; i < rankArray.length; i++) {
            final RankPrices rankBefore = rankArray[i - 1];
            final RankPrices rank = rankArray[i];
            final double difference = rank.getPrice() - rankBefore.getPrice();
            double tempLeftover = leftoverMoney - difference;
            tempLeftover = ((int) (tempLeftover * 100)) / 100D;
            if (tempLeftover < 0) break;
            leftoverMoney = tempLeftover;
            result = rank;
            if (tempLeftover == 0) break;
        }
        return new AbstractMap.SimpleEntry<>(result, leftoverMoney);
    }

    enum RankPrices {
        NONE(0, null),
        VIP(9.99, "VIP"),
        ELITE(24.99, "ELITE"),
        EPIC(49.99, "EPIC"),
        ULTRA(79.99, "ULTRA");

        private final double price;
        private final String groupName;

        RankPrices(final double price, final String groupName) {
            this.price = price;
            this.groupName = groupName;
        }

        public double getPrice() {
            return price;
        }

        public String getGroupName() {
            return groupName;
        }
    }
}