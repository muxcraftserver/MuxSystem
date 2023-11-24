package me.muxteam.marketing;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

public final class MuxGiftCodes {
    private MuxSystem ms;
    private Map<String, GiftCode> giftcodes = new HashMap<>();
    private final Map<String, List<String>> usedips = new HashMap<>();
    private final Queue<Map.Entry<String, GiftCode>> savequeue = new LinkedBlockingQueue<>();

    public MuxGiftCodes(final MuxSystem ms) {
        this.ms = ms;
        new BukkitRunnable() {
            @Override public void run() {
                ms.getDB().loadGiftCodes(ms.getGiftCodes());
            }
        }.runTaskLater(ms, 1L);
        new BukkitRunnable() {
            @Override public void run() {
                saveTask();
            }
        }.runTaskTimerAsynchronously(ms, 1200L, 200L);
    }

    private void saveTask() {
        if (savequeue.isEmpty()) return;
        final Map<String, GiftCode> toSave = new HashMap<>();
        while (savequeue.isEmpty() == false) {
            final Map.Entry<String, GiftCode> entry = savequeue.poll();
            toSave.put(entry.getKey(), entry.getValue());
        }
        ms.getDB().saveGiftCodes(toSave);
    }

    public void close() {
        ms.getDB().saveGiftCodes(giftcodes);
        this.ms = null;
    }

    public void setGiftcodes(final Map<String, GiftCode> giftcodes) {
        this.giftcodes = giftcodes;
    }

    public Map<String, GiftCode> getGiftcodes() {
        return giftcodes;
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (ms.isFullTrusted(p.getName()) == false || args.length == 0) {
            new MuxAnvil(this.ms, (input, player) -> handlePlayer(player, ms.retardCleaner(input, ms.getLang("giftcode.entercode")).toLowerCase(), true)).show(p, ms.getLang("giftcode.entercode"));
        } else if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 4) {
                ms.showItemBar(p, ms.usage("/gutschein add [code] [limit] [coins|item:data] [anzahl]"));
                return true;
            }
            short limit;
            try {
                limit = Short.parseShort(args[2]);
                if (limit < 1) throw new NumberFormatException();
            } catch (final NumberFormatException e) {
                ms.showItemBar(p, ms.hnonumber);
                return true;
            }
            final String gname = args[1].toLowerCase();
            if (gname.length() > 15) {
                ms.showItemBar(p, "§cDer Name vom Gutschein ist zu lang.");
                return true;
            }
            if (args[3].equalsIgnoreCase("coins")) {
                int amount;
                try {
                    amount = Integer.parseInt(args[4]);
                    if (amount < 0) throw new NumberFormatException();
                } catch (final NumberFormatException e) {
                    ms.showItemBar(p, ms.hnonumber);
                    return true;
                }
                giftcodes.put(gname, new CoinGift(limit, amount));
                ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "GIFTCODE", gname + " (" + amount + " COINS" + ")");
            } else {
                final ItemStack item = ms.getItemStack(args, 3);
                if (item == null) {
                    ms.showItemBar(p, "§cDas Item existiert nicht.");
                    return true;
                }
                giftcodes.put(gname, new ItemGift(limit, item.getTypeId(), item.getAmount(), item.getDurability()));
                ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "GIFTCODE", gname + " (" + item.getAmount() + " " + item.getType().toString() + ")");
            }
            savequeue.add(new AbstractMap.SimpleEntry<>(gname, giftcodes.get(gname)));
            ms.showItemBar(p, "§fDer Gutschein '§6" + gname + "§f' wurde §aerstellt§f.");
        } else if (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("remove")) {
            if (args.length != 2) {
                ms.showItemBar(p, ms.usage("/gutschein del [code]"));
                return true;
            }
            final String gname = args[1].toLowerCase();
            if (giftcodes.containsKey(gname)) {
                giftcodes.remove(gname);
                savequeue.removeIf(entry -> entry.getKey().equalsIgnoreCase(gname));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ms.getDB().deleteGiftCode(gname);
                    }
                }.runTaskAsynchronously(ms);
                usedips.remove(gname);
                ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "GIFTCODE REMOVE", gname);
                ms.showItemBar(p, "§fDer Gutschein '§6" + gname + "§f' wurde §cgelöscht§f.");
            } else {
                ms.showItemBar(p, "§cDer Gutschein '§6" + gname + "§f' §cexistiert nicht.");
            }
        } else {
            p.sendMessage(ms.header((byte) 13, "§6"));
            byte count = 1;
            for (final Entry<String, GiftCode> entry : giftcodes.entrySet()) {
                final GiftCode data = entry.getValue();
                String info = "";
                if (data instanceof ItemGift) {
                    final ItemStack it = ((ItemGift) data).getItemStack();
                    info = it.getAmount() + " " + it.getType().toString();
                } else if (data instanceof CoinGift) {
                    final CoinGift wg = (CoinGift) data;
                    info = wg.getCoins() + " MUXCOINS";
                }
                p.sendMessage("  §6" + count + "§8» §7" + entry.getKey() + " §8| §e" + data.getUsedCount() + "/" + data.getLimit() + " §8| §a(" + info + ")");
                count++;
            }
            if (count == 1) p.sendMessage("  §6§oEs gibt noch keine Gutscheine.");
            p.sendMessage(new String[]{
                    "",
                    "  §6/gutschein add [code] ",
                    "  §6[wieoft] [coins | item] [anzahl]: §7Gutschein erstellen",
                    "  §6/gutschein del [code]: §7Gutschein entfernen"
            });
            p.sendMessage(ms.footer((byte) 13, "§6"));
        }
        return true;
    }

    public void handlePlayer(final Player p, final String gname, final boolean notconsole) {
        final UUID uuid = p.getUniqueId();
        final GiftCode giftcode = giftcodes.get(gname);
        if (giftcode == null) {
            ms.showItemBar(p, "§c" + (notconsole ? ms.getLang("giftcode.thegift") : ms.getLang("giftcode.thereward")) + " " + ms.getLang("giftcode.doesntexist"));
            return;
        }
        final List<String> ips = usedips.getOrDefault(gname, new ArrayList<>());
        if (giftcode.hasUsed(uuid) || ips.contains(p.getAddress().getAddress().getHostAddress())) {
            ms.showItemBar(p, ms.getLang("giftcode.youhave") + " " + (notconsole ? ms.getLang("giftcode.alreadygift") : ms.getLang("giftcode.alreadyreward")));
            return;
        } else if (giftcode instanceof ItemGift) {
            if (p.getInventory().firstEmpty() == -1) {
                ms.showItemBar(p, ms.getLang("invfull"));
            } else {
                p.updateInventory();
                giftcode.setUsed(uuid);
                if (giftcode.getUsedCount() >= giftcode.getLimit()) {
                    giftcodes.remove(gname);
                    savequeue.removeIf(entry -> entry.getKey().equalsIgnoreCase(gname));
                    usedips.remove(gname);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            ms.getDB().deleteGiftCode(gname);
                        }
                    }.runTaskAsynchronously(ms);
                } else {
                    if (savequeue.stream().noneMatch(entry -> entry.getKey().equalsIgnoreCase(gname))) {
                        savequeue.add(new AbstractMap.SimpleEntry<>(gname, giftcode));
                    }
                }
                if (notconsole) {
                    ms.sendTitle(p, "§6§lGutschein", 10, 120, 10);
                    ms.sendSubTitle(p, ms.getLang("giftcode.success"), 10, 60, 10);
                    ms.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            ms.sendSubTitle(p, ms.getLang("giftcode.gotitems"), 0, 60, 10);
                        }
                    }, 60L);
                } else {
                    ms.showItemBar(p, ms.getLang("giftcode.gotitems"));
                }
                p.getInventory().addItem(((ItemGift) giftcode).getItemStack());
            }
        } else if (giftcode instanceof CoinGift) {
            final MuxUser u = ms.getMuxUser(p.getName());
            final CoinGift wg = (CoinGift) giftcode;
            giftcode.setUsed(uuid);
            if (giftcode.getUsedCount() >= giftcode.getLimit()) {
                giftcodes.remove(gname);
                usedips.remove(gname);
                savequeue.removeIf(entry -> entry.getKey().equalsIgnoreCase(gname));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ms.getDB().deleteGiftCode(gname);
                    }
                }.runTaskAsynchronously(ms);
            } else {
                if (savequeue.stream().noneMatch(entry -> entry.getKey().equalsIgnoreCase(gname))) {
                    savequeue.add(new AbstractMap.SimpleEntry<>(gname, giftcode));
                }
            }
            if (notconsole) {
                ms.sendTitle(p, "§6§lGutschein", 10, 120, 10);
                ms.sendSubTitle(p, ms.getLang("giftcode.success"), 10, 60, 10);
                ms.runLater(new BukkitRunnable() {
                    @Override
                    public void run() {
                        ms.sendSubTitle(p, String.format(ms.getLang("giftcode.gotcoins"), wg.getCoins()), 0, 60, 10);
                    }
                }, 60L);
            } else {
                ms.showItemBar(p, String.format(ms.getLang("giftcode.gotcoins"), wg.getCoins()));
            }
            if (wg.getCoins() > 0) {
                ms.getAnalytics().addEventExpenses(wg.getCoins());
                u.addCoins(wg.getCoins());
                ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(wg.getCoins()), "Giftcode");
                ms.sendScoreboard(p);
                ms.saveMuxUser(u);
            }
        } else {
            ms.showItemBar(p, ms.getLang("giftcode.invalid"));
            return;
        }
        if (savequeue.stream().noneMatch(entry -> entry.getKey().equalsIgnoreCase(gname))) {
            final GiftCode gift = giftcodes.get(gname);
            if (gift != null) {
                savequeue.add(new AbstractMap.SimpleEntry<>(gname, gift));
            }
        }
        ips.add(p.getAddress().getAddress().getHostAddress());
        usedips.put(gname, ips);
    }

    public class GiftCode {
        private final int limit;
        private List<UUID> players = new ArrayList<>();

        public GiftCode(final int limit) {
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }

        public int getUsedCount() {
            return players.size();
        }

        public boolean hasUsed(final UUID uuid) {
            return players.contains(uuid);
        }

        public void setUsed(final UUID uuid) {
            this.players.add(uuid);
        }
    }

    public final class ItemGift extends GiftCode {
        private final int itemid, itemamount;
        private final short itemdata;

        public ItemGift(final int limit, final int itemid, final int itemamount, final short itemdata) {
            super(limit);
            this.itemid = itemid;
            this.itemamount = itemamount;
            this.itemdata = itemdata;
        }

        public ItemStack getItemStack() {
            return new ItemStack(itemid, itemamount, itemdata);
        }
    }

    public final class CoinGift extends GiftCode {
        private final int coins;

        public CoinGift(final int limit, final int coins) {
            super(limit);
            this.coins = coins;
        }

        public int getCoins() {
            return coins;
        }
    }

    public final class GiftDeserializer implements JsonDeserializer<GiftCode> {
        @Override
        public GiftCode deserialize(final JsonElement json, final Type type, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject obj = json.getAsJsonObject();

            final GiftCode giftcode;
            if (obj.get("coins") != null) {
                giftcode = new CoinGift(obj.get("limit").getAsInt(), obj.get("coins").getAsInt());
            } else {
                giftcode = new ItemGift(obj.get("limit").getAsInt(), obj.get("itemid").getAsInt(), obj.get("itemamount").getAsInt(), obj.get("itemdata").getAsShort());
            }

            final Gson gson = new Gson();
            giftcode.players = gson.fromJson(obj.get("players"), new TypeToken<List<UUID>>() {}.getType());

            return giftcode;
        }
    }
}