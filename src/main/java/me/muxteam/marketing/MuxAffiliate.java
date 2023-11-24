package me.muxteam.marketing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxChatInput;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.ranks.MuxRanks;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MuxAffiliate implements Listener {
    private boolean isCacheCleanup = false;
    private final int AFFILIATE_PERCENTAGE = 30;
    private final long CONFIRM_TIME = 14 * 24 * 60 * 60 * 1000L; // 14 days
    private final Gson gsonbuilder;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy 'um' HH:mm");
    private final Map<UUID, Boolean> recruitedCache = new HashMap<>();
    private final Map<Player, Map<Integer, Inventory>> inventoryCache = new HashMap<>();
    private final ItemStack solanaSkull, litecoinSkull, dogecoinSkull, moneroSkull, btcCashSkull;
    private final String CREATOR_RANK_NAME = "Creator";

    private final MuxSystem ms;

    public MuxAffiliate(final MuxSystem ms) {
        this.ms = ms;
        ms.getServer().getPluginManager().registerEvents(this, ms);
        this.solanaSkull = ms.renameItemStack(ms.getHeadFromURL("https://textures.minecraft.net/texture/dcd7d14c6db841e5864511d16ba7670b3d2038142466981feb05afc6e5edc6cb", "Solana"), "§3§lSolana", "§7Gebe im Anschluss deine öffentliche", "§7Adresse der Währung im Chat ein.", "", "§7Klicke, um die Währung zu §3benutzen§7.");
        this.litecoinSkull = ms.renameItemStack(ms.getHeadFromURL("https://textures.minecraft.net/texture/6adcf96106613a33d3d2a464adb1b1a5c5e0cb11dce72926b599943e363df", "Litecoin"), "§f§lLitecoin", "§7Gebe im Anschluss deine öffentliche", "§7Adresse der Währung im Chat ein.", "", "§7Klicke, um die Währung zu §fbenutzen§7.");
        this.dogecoinSkull = ms.renameItemStack(ms.getHeadFromURL("https://textures.minecraft.net/texture/e8e6f7795df2eb8d7f4ff5d0aed529be81134d97bb51263e38cb694d6704da4", "Dogecoin"), "§e§lDogecoin", "§7Gebe im Anschluss deine öffentliche", "§7Adresse der Währung im Chat ein.", "", "§7Klicke, um die Währung zu §ebenutzen§7.");
        this.moneroSkull = ms.renameItemStack(ms.getHeadFromURL("https://textures.minecraft.net/texture/1b5fc06f72598e84fc75dd649b50838c5393c56af98ecaee468ce132f9483f", "Monero"), "§6§lMonero", "§7Gebe im Anschluss deine öffentliche", "§7Adresse der Währung im Chat ein.", "", "§7Klicke, um die Währung zu §6benutzen§7.");
        this.btcCashSkull = ms.renameItemStack(ms.getHeadFromURL("https://textures.minecraft.net/texture/df53eb806840070585ccaedef5b23d02164c6d2b8e6438144eaff81bb5c6f6c3", "BitcoinCash"), "§a§lBitcoin Cash", "§7Gebe im Anschluss deine öffentliche", "§7Adresse der Währung im Chat ein.", "", "§7Klicke, um die Währung zu §abenutzen§7.");

        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        this.gsonbuilder = new GsonBuilder()
                .registerTypeAdapter(StoreTransaction.class, new StoreTransactionSerializer())
                .registerTypeAdapter(StoreTransaction.class, new StoreTransactionDeserializer())
                .create();

        new BukkitRunnable() {
            @Override
            public void run() {
                isCacheCleanup = true; // bool to lower risk of concurrent exception
                recruitedCache.clear();
                isCacheCleanup = false;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        refreshTransactions();
                    }
                }.runTaskAsynchronously(ms);
            }
        }.runTaskTimer(ms, 72000L, 36000L);
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshTransactions();
            }
        }.runTaskLater(ms, 5L);
    }

    public void close() {}

    private void refreshTransactions() {
        for (final Map.Entry<RecruiterInformations, List<RecruitedInformations>> entry : ms.getDB().getAllAffiliateData().entrySet()) {
            boolean saveRecruiter = false, saveRecruited = false;
            final RecruiterInformations recruiter = entry.getKey();
            for (final RecruitedInformations recruited : entry.getValue()) {
                for (final StoreTransaction transaction : recruited.getStoreTransactions()) {
                    if (transaction.status == -2 && transaction.timestamp <= System.currentTimeMillis() - CONFIRM_TIME) {
                        transaction.status = 0;
                        recruited.updateTotalAmount();
                        recruiter.currentAmountInEur += transaction.amount;
                        recruiter.estimatedAmountInEur -= transaction.amount;
                        saveRecruiter = true;
                        saveRecruited = true;
                    }
                }
                if (saveRecruited)
                    ms.getDB().saveRecruited(recruited);
            }
            if (saveRecruiter) ms.getDB().saveRecruiter(recruiter, null);
        }
        for (Map.Entry<CashbackInformations, RecruiterInformations> entry : ms.getDB().getAllCashbackData().entrySet()) {
            boolean save = false;
            final CashbackInformations cashbackInformations = entry.getKey();
            final RecruiterInformations recruiter = entry.getValue();
            for (final StoreTransaction transaction : cashbackInformations.getStoreTransactions()) {
                if (transaction.status == -2 && transaction.timestamp <= System.currentTimeMillis() - CONFIRM_TIME) {
                    transaction.status = 0;
                    recruiter.currentAmountInEur += transaction.amount;
                    recruiter.estimatedAmountInEur -= transaction.amount;
                    save = true;
                }
            }
            if (save) {
                ms.getDB().saveRecruiter(recruiter, null);
                ms.getDB().saveCashback(cashbackInformations);
            }
        }
    }

    public boolean handleCommand(final Player player, final String[] args) {
        return handleCommand(player, args, true);
    }

    public boolean handleCommand(final Player player, final String[] args, final boolean cooldown) {
        if (isEnabled() == false && player.isOp() == false) {
            ms.showItemBar(player, "§cWerben ist temporär deaktiviert.");
            return true;
        }
        if (player.hasMetadata("affiliate") && args.length == 1 && args[0].equals(player.getMetadata("affiliate").get(0).asString())) {
            player.removeMetadata("affiliate", ms);
            new BukkitRunnable() {
                @Override
                public void run() {
                    ms.getDB().saveRecruiter(new RecruiterInformations(player.getUniqueId(), 0, 0, 0, 0, null, null), player.getName());
                    ms.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            handleCommand(player, new String[0]);
                        }
                    }, 0);
                }
            }.runTaskAsynchronously(ms);
            return true;
        }
        this.inventoryCache.remove(player);
        if (player.isOp() && args.length > 0 && (ms.getPlayer(args[0]) != null)) {
            player.removeMetadata("werbencurrentpage", ms);
            player.removeMetadata("totalrecruitedamount", ms);
            openInventory(player, ms.getPlayer(args[0]).getUniqueId(), 1);
            return true;
        }
        if (isRegisteredAffiliateUser(player.getUniqueId()) == false) {
            final String code = ms.getRandomWord();
            final ItemStack book = ms.cmdBook("Werben",
                    "Lade neue Spieler auf MuxCraft ein und §overdiene Echtgeld§0 jedes Mal, wenn sie im Onlineshop etwas kaufen.",
                    new Object[]{
                            "/werben " + code,
                            "Jetzt Teilnehmen",
                            "Klicke zum Teilnehmen"});
            player.setMetadata("affiliate", new FixedMetadataValue(ms, code));
            ms.openBook(book, player);
            return true;
        }
        if (cooldown && ms.checkGeneralCooldown(player.getName(), "affiliateinv", 3000L, true)) return true;
        player.removeMetadata("werbencurrentpage", ms);
        player.removeMetadata("totalrecruitedamount", ms);
        openInventory(player, player.getUniqueId(), 1);
        return true;
    }

    // /payment <uuid> <price> <transaction id>
    public void handlePaymentCommand(final String[] args) {
        String u = args[0];
        if (args.length == 1) return;
        if (u.contains("-") == false) u = u.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        final UUID uuid = UUID.fromString(u);

        final double amountInEur = Double.parseDouble(args[2]) / 100 * AFFILIATE_PERCENTAGE;
        final String transactionId = args[1];
        final StoreTransaction transaction = new StoreTransaction(transactionId, amountInEur, (byte) -2, System.currentTimeMillis());

        final RecruitedInformations information = ms.getDB().loadRecruited(uuid);

        final MuxRanks.PermissionsGroup permissionsGroup = ms.getPerms().getGroupOf(uuid);
        if (permissionsGroup != null && permissionsGroup.getName().equalsIgnoreCase(CREATOR_RANK_NAME)) {
            CashbackInformations cashbackInformations = ms.getDB().loadCashback(uuid);
            if (cashbackInformations == null)
                cashbackInformations = new CashbackInformations(uuid, 0, new ArrayList<>());
            if (cashbackInformations.getStoreTransactions().stream().noneMatch(transaction2 -> transaction2.getTransactionId().equals(transactionId))) {
                final RecruiterInformations recruiterInformations = ms.getDB().loadRecruiter(uuid);
                if (recruiterInformations != null) { // Creator needs to be signed up to affiliate
                    if (cashbackInformations.status < 10)
                        cashbackInformations.status = System.currentTimeMillis();
                    recruiterInformations.estimatedAmountInEur += amountInEur;
                    cashbackInformations.addStoreTransaction(transaction);
                    ms.getDB().saveRecruiter(recruiterInformations, null);
                    ms.getDB().saveCashback(cashbackInformations);
                }
            }
        }

        if (information == null) return;
        if (information.getStoreTransactions().stream().anyMatch(transaction2 -> transaction2.getTransactionId().equals(transactionId)))
            return;
        if (information.status == -1) // chargebacked
            return;
        if (information.status == -2) // same IP
            return;
        if (information.status < 10)
            information.status = System.currentTimeMillis();

        information.addStoreTransaction(transaction);
        ms.getDB().saveRecruited(information);
        final UUID recruiter = information.getRecruitedBy();
        final RecruiterInformations recruiterInformations = ms.getDB().loadRecruiter(recruiter);
        recruiterInformations.estimatedAmountInEur += amountInEur;
        ms.getDB().saveRecruiter(recruiterInformations, null);
        ms.getAnalytics().getAnalytics().addAffiliateMoney(amountInEur);
    }

    // /chargeback <uuid/name> <transaction id>
    public void handleChargebackCommand(final String[] args) {
        String u = args[0];
        if (u.contains("-") == false) u = u.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        final UUID uuid = UUID.fromString(u);
        final String transactionId = args[1];
        // chargeback c32ec938-0323-43c9-8617-425f367ce073 abcd
        final CashbackInformations cashbackInformations = ms.getDB().loadCashback(uuid);
        if (cashbackInformations != null) {
            final RecruiterInformations recruiterInformations = ms.getDB().loadRecruiter(uuid);
            if (recruiterInformations != null) {
                cashbackInformations.getStoreTransactions()
                        .stream()
                        .filter(transaction -> transaction.transactionId.equals(transactionId))
                        .findFirst()
                        .ifPresent(transaction -> {
                            if (transaction.status == -1)
                                return;
                            if (transaction.status == 0) {
                                recruiterInformations.currentAmountInEur -= transaction.amount;
                            }
                            if (transaction.status == -2)
                                recruiterInformations.estimatedAmountInEur -= transaction.amount;
                            transaction.status = -1;
                        });
                ms.getDB().saveRecruiter(recruiterInformations, null);
            }
            ms.getDB().saveCashback(cashbackInformations);
        }
        final RecruitedInformations information = ms.getDB().loadRecruited(uuid);
        if (information == null) return;
        information.status = -1;
        final RecruiterInformations recruiterInformations = ms.getDB().loadRecruiter(information.getRecruitedBy());
        information.getStoreTransactions()
                .stream()
                .filter(transaction -> transaction.transactionId.equals(transactionId))
                .findFirst()
                .ifPresent(transaction -> {
                    if (transaction.status == -1)
                        return;
                    if (transaction.status == 0) {
                        recruiterInformations.currentAmountInEur -= transaction.amount;
                    }
                    if (transaction.status == -2)
                        recruiterInformations.estimatedAmountInEur -= transaction.amount;
                    transaction.status = -1;
                });
        ms.getDB().saveRecruiter(recruiterInformations, null);
        ms.getDB().saveRecruited(information);
    }

    public boolean handleChatInput(final Player player, final String message) {
        if (canAddRecruiter(player) == false) return false;
        final String[] parts = message.split(" ");
        if (parts.length > 1) return false;
        final OfflinePlayer offlineTarget = ms.getPlayer(parts[0]);

        if (offlineTarget == null) return false;
        if (isRegisteredAffiliateUser(offlineTarget.getUniqueId()) && offlineTarget.getUniqueId().equals(player.getUniqueId()) == false) {
            this.addRecruited(player, offlineTarget.getUniqueId());
            return true;
        }
        return false;
    }

    public void addRecruited(final Player player, final UUID recruiter) {
        final Player recruiterAsPlayer = ms.getServer().getPlayer(recruiter);
        if (recruiterAsPlayer != null) {
            recruiterAsPlayer.sendMessage("§a§lMuxWerben>§7 Du hast §a" + player.getName() + " §7mit Erfolg geworben!");
            if (ms.checkGeneralCooldown(recruiterAsPlayer.getName(), "AFFILIATEALERT", 30000L, true) == false) {
                ms.chatClickHoverRun(recruiterAsPlayer, "§a§lMuxWerben> §7Klicke §ahier§7, um deine Liste zu sehen.", "§a§oKlicke um deine Liste zu sehen", "/affiliate");
            }
        }
        final MuxUser u = ms.getMuxUser(player.getName());
        final long coins = getAffiliateReward();
        u.addCoins(coins);
        ms.getHistory().addHistory(player.getUniqueId(), null, "COINS", String.valueOf(coins), "Affiliate");
        player.sendMessage("§6§lMuxCraft>§7 Dir wurden §e" + ms.getNumberFormat(coins) + " MuxCoins §7gutgeschrieben.");
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.sendScoreboard(player);
                ms.getAnalytics().addAffiliateExpenses(coins);
                recruitedCache.put(player.getUniqueId(), true);
            }
        }.runTask(ms);
        final boolean sameIp = ms.getDB().getUUIDsWithSameIP(ms.getMuxUser(player.getName()).getIp(), player.getName()).contains(recruiter);
        ms.getDB().saveRecruited(player.getUniqueId(), recruiter, (byte) (sameIp ? -2 : 0), gsonbuilder.toJson(new ArrayList<StoreTransaction>()));
        ms.getAnalytics().getAnalytics().addAffiliate();
    }

    private boolean canAddRecruiter(final Player player) {
        final MuxUser user = ms.getMuxUser(player.getName());
        if (System.currentTimeMillis() - user.getGlobalFirstLogin() > 1200000L /* 20 minutes */) {
            return false;
        }
        return isRecruited(player.getUniqueId()) == false;
    }

    private boolean isRegisteredAffiliateUser(final UUID uuid) {
        return ms.getDB().isRegisteredAffiliateUser(uuid);
    }

    public Gson getGson() {
        return gsonbuilder;
    }

    private boolean isRecruited(final UUID uuid) {
        if (this.isCacheCleanup)
            return ms.getDB().isRecruited(uuid);
        final Boolean cached = recruitedCache.get(uuid);
        if (cached != null) return cached;
        this.recruitedCache.put(uuid, ms.getDB().isRecruited(uuid));
        return this.isRecruited(uuid);
    }

    public void openCashoutInventory(final Player player) {
        final Inventory inventory = ms.getServer().createInventory(null, 45, "§0§lMuxWerben§0 | Auszahlung");
        inventory.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        new BukkitRunnable() {
            @Override
            public void run() {
                final RecruiterInformations recruiterInformations = ms.getDB().loadRecruiter(player.getUniqueId());
                inventory.setItem(4, ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK), "§a§lAuszahlung", "§7Vorläufige Einnahmen: §f" + ms.getDecimalFormat(recruiterInformations.getEstimatedAmountInEur()).replace(".", ",") + " EUR",
                        "", "§7Nachdem die Spenden überprüft wurden,", "§7werden deine Einnahmen zu Profite.", "",
                        "§7Endgültige Profite: §a" + ms.getDecimalFormat(recruiterInformations.getCurrentAmountInEur()).replace(".", ",") + "§7/10 EUR"));
                if (recruiterInformations.getCurrency() != null && recruiterInformations.getCryptoAddress() != null) {
                    final String color = recruiterInformations.getCurrency().getName().split("§l")[0];
                    inventory.setItem(19, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 13), "§2§lZuletzt benutzt",
                            "§7Währung: " + recruiterInformations.getCurrency().getName().replace("§l", ""),
                            "§7Adresse: " + color + recruiterInformations.getCryptoAddress(), "", "§7Klicke, um diese Adresse §2erneut zu benutzen§7."));
                    inventory.setItem(21, solanaSkull);
                    inventory.setItem(22, litecoinSkull);
                    inventory.setItem(23, dogecoinSkull);
                    inventory.setItem(24, moneroSkull);
                    inventory.setItem(25, btcCashSkull);
                } else {
                    inventory.setItem(20, solanaSkull);
                    inventory.setItem(21, litecoinSkull);
                    inventory.setItem(22, dogecoinSkull);
                    inventory.setItem(23, moneroSkull);
                    inventory.setItem(24, btcCashSkull);
                }
            }
        }.runTaskAsynchronously(ms);

        if (ms.getActiveInv(player.getName()) != MuxInventory.InvType.AFFILIATE) player.closeInventory();
        player.openInventory(inventory);
        ms.setActiveInv(player.getName(), MuxInventory.InvType.AFFILIATE);
    }

    public void handleCashoutInventory(final Player player, final int slot, final Inventory inventory) {
        if (slot == 0) {
            player.playSound(player.getLocation(), Sound.CLICK, 1F, 1F);
            handleCommand(player, new String[0], false);
            return;
        } else if (slot == 4) {
            player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        if (inventory.getItem(19) != null && inventory.getItem(19).getType() == Material.STAINED_CLAY) {
            if (slot == 19) {
                processCashout(player.getUniqueId(), null, null);
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1F, 10F);
                player.sendMessage("§a§lAuszahlung erfolgreich beauftragt.");
            } else if (slot == 21) {
                startChatInput(player, CryptoCurrency.SOLANA);
            } else if (slot == 22) {
                startChatInput(player, CryptoCurrency.LITECOIN);
            } else if (slot == 23) {
                startChatInput(player, CryptoCurrency.DOGECOIN);
            } else if (slot == 24) {
                startChatInput(player, CryptoCurrency.MONERO);
            } else if (slot == 25) {
                startChatInput(player, CryptoCurrency.BITCOIN_CASH);
            }
        }
        if (slot == 20) {
            startChatInput(player, CryptoCurrency.SOLANA);
        } else if (slot == 21) {
            startChatInput(player, CryptoCurrency.LITECOIN);
        } else if (slot == 22) {
            startChatInput(player, CryptoCurrency.DOGECOIN);
        } else if (slot == 23) {
            startChatInput(player, CryptoCurrency.MONERO);
        } else if (slot == 24) {
            startChatInput(player, CryptoCurrency.BITCOIN_CASH);
        }
    }

    private long getAffiliateReward() {
        final long supply = ms.getActiveMuxCoinsSupply();
        final double percentage = 0.00001D;
        long result = (long) Math.ceil((supply * percentage));
        int n = (int) Math.ceil(Math.log10(result + 1D));
        n = Math.max(0, n - 1);
        result = (long) Math.ceil(result / Math.pow(10, n)) * (long) Math.pow(10, n);
        return Math.max(result, 2000L);
    }

    private void startChatInput(final Player player, final CryptoCurrency currency) {
        player.playSound(player.getLocation(), Sound.CLICK, 1F, 1F);
        player.closeInventory();
        new MuxChatInput(ms, (input, p) -> {
            if (checkCryptoAddressInput(input) == false) {
                openCashoutInventory(player);
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.showItemBar(p, "§cUngültige Addresse. Versuche es erneut.");
                return;
            }
            final String color = currency.getName().split("§l")[0];
            new ConfirmInventory(ms, p1 -> {
                p1.closeInventory();
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1F, 10F);
                processCashout(p1.getUniqueId(), currency, input);
                p1.sendMessage("§a§lAuszahlung erfolgreich beauftragt.");
            }, p1 -> openCashoutInventory(player))
                    .show(p, "§0§lMuxWerben§0 | Auszahlung", "§a§lBestätigen", "§c§lAbbrechen",
                            currency == CryptoCurrency.SOLANA ? solanaSkull.clone() : currency == CryptoCurrency.LITECOIN ? litecoinSkull.clone() : currency == CryptoCurrency.DOGECOIN ? dogecoinSkull.clone() : currency == CryptoCurrency.MONERO ? moneroSkull.clone() : btcCashSkull.clone(), currency.getName(), "§7Adresse: " + color + input);
        }).show(player, "§f§lGebe deine " + ChatColor.stripColor(currency.getName()) + " Adresse im Chat ein:");
    }

    private void processCashout(final UUID uuid, final CryptoCurrency currency, final String address) {
        final RecruiterInformations informations = ms.getDB().loadRecruiter(uuid);
        if (currency != null)
            informations.currency = currency;
        if (address != null)
            informations.cryptoAddress = address;
        informations.currentCashoutInEur = informations.currentAmountInEur;
        informations.totalCashedoutInEur += informations.currentCashoutInEur;
        informations.currentAmountInEur = 0;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final RecruitedInformations recruitedInformations : ms.getDB().getRecruitedList(uuid)) {
                    boolean save = false;
                    for (final StoreTransaction storeTransaction : recruitedInformations.getStoreTransactions()) {
                        if (storeTransaction.status == 0) {
                            storeTransaction.status = 1;
                            save = true;
                        }
                    }
                    if (save)
                        ms.getDB().saveRecruited(recruitedInformations);
                }
                final MuxRanks.PermissionsGroup permissionsGroup = ms.getPerms().getGroupOf(uuid);
                if (permissionsGroup != null && permissionsGroup.getName().equalsIgnoreCase(CREATOR_RANK_NAME)) {
                    final CashbackInformations cashbackInformations = ms.getDB().loadCashback(uuid);
                    boolean save = false;
                    for (final StoreTransaction storeTransaction : cashbackInformations.getStoreTransactions()) {
                        if (storeTransaction.status == 0) {
                            storeTransaction.status = 1;
                            save = true;
                        }
                    }
                    if (save)
                        ms.getDB().saveCashback(cashbackInformations);
                }
                final String pname = ms.getShared().getNameFromUUID(uuid);
                if (pname != null)
                    ms.getDB().saveRecruiter(informations, pname);
            }
        }.runTaskAsynchronously(ms);

    }

    private boolean checkCryptoAddressInput(final String str) {
        if (str.split(" ").length > 1) return false;
        for (final char c : str.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
                return false;
            }
        }
        return str.length() >= 24;
    }

    public void openInventory(final Player player, final UUID recruiter, final int page) {
        final Map<Integer, Inventory> invcache = this.inventoryCache.get(player);
        final AtomicInteger totalAmount = new AtomicInteger(-1);
        if (player.hasMetadata("totalrecruitedamount"))
            totalAmount.set(player.getMetadata("totalrecruitedamount").get(0).asInt());
        final Inventory inventory;
        final Inventory cached = invcache == null ? null : invcache.get(page);
        if (invcache != null && cached != null) {
            inventory = cached;
        } else {
            inventory = ms.getServer().createInventory(null, 45, "§0§lMuxWerben§0 | " + (player.getUniqueId().equals(recruiter) ? "/werben" : ms.getShared().getNameFromUUID(recruiter)));
            inventory.setItem(4, ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lMuxWerben",
                    "§7Lade neue Spieler auf MuxCraft ein!", "§7Brandneue Spieler müssen nur deinen", "§7Namen im Chat tippen, damit diese dann",
                    "§e" + ms.getNumberFormat(getAffiliateReward()) + " MuxCoins §7kostenlos bekommen.",
                    "",
                    "§7Danach bekommst du jedes Mal, wenn", "§7sie was spenden, §f" + AFFILIATE_PERCENTAGE + "% der Einnahmen§7."));
            final AtomicReference<RecruiterInformations> informations = new AtomicReference<>(null);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (informations.get() == null) {
                        informations.set(ms.getDB().loadRecruiter(recruiter));
                    }
                    if (totalAmount.get() == -1)
                        totalAmount.set(ms.getDB().getRecruitedByRecruiter(recruiter));
                    final List<RecruitedInformations> list = ms.getDB().getRecruited(recruiter, page, 27);
                    int slot = 18;
                    boolean needsSave = false;
                    final Server sr = ms.getServer();
                    final MuxRanks.PermissionsGroup permissionsGroup = ms.getPerms().getGroupOf(recruiter);
                    CashbackInformations cashbackInformations = ms.getDB().loadCashback(recruiter);

                    final boolean isCreator = (permissionsGroup != null && permissionsGroup.getName().equalsIgnoreCase(CREATOR_RANK_NAME));
                    if (cashbackInformations != null || isCreator) {
                        if (cashbackInformations == null)
                            cashbackInformations = new CashbackInformations(recruiter, 0, new ArrayList<>());
                        double cashbackAmount = 0;
                        boolean internalNeedsSave = false;
                        for (final StoreTransaction transaction : cashbackInformations.getStoreTransactions()) {
                            if (transaction.status == -2 && transaction.timestamp < System.currentTimeMillis() - CONFIRM_TIME) {
                                transaction.status = 0;
                                informations.get().currentAmountInEur += transaction.amount;
                                informations.get().estimatedAmountInEur -= transaction.amount;
                                needsSave = true;
                                internalNeedsSave = true;
                            }
                            if (transaction.status != -1 && transaction.status != 1)
                                cashbackAmount += transaction.amount;
                        }
                        if (internalNeedsSave) ms.getDB().saveCashback(cashbackInformations);
                        if (cashbackAmount > 0 || isCreator) {
                            final String progress;
                            if (cashbackInformations.status > 10) {
                                final long count = cashbackInformations.getStoreTransactions().stream().filter(tr -> tr.status != -1).count();
                                if (count == 0)
                                    progress = "§fnoch keine Spenden";
                                else if (count == 1)
                                    progress = "§abereits gespendet";
                                else
                                    progress = "§abereits " + count + "x gespendet";
                            } else
                                progress = "§fnoch keine Spenden";
                            final String creatorName = ms.getShared().getNameFromUUID(recruiter);
                            inventory.setItem(slot, ms.renameItemStack(ms.getHead(creatorName), "§d§lCreator Cashback",
                                    "§7Wenn für dich gespendet wird,", "§7bekommst du §f" + AFFILIATE_PERCENTAGE + "% der Einnahmen§7.", "", "§7Fortschritt: " + progress));
                            slot++;
                        }
                    }
                    for (final RecruitedInformations information : list) {
                        final OfflinePlayer op = sr.getOfflinePlayer(information.getUUID());
                        final String recruitedName = ms.getShared().getNameFromUUID(information.getUUID());
                        boolean internalNeedsSave = false;
                        for (final StoreTransaction transaction : information.getStoreTransactions()) {
                            if (transaction.status == -2 && transaction.timestamp < System.currentTimeMillis() - CONFIRM_TIME) {
                                transaction.status = 0;
                                informations.get().currentAmountInEur += transaction.amount;
                                informations.get().estimatedAmountInEur -= transaction.amount;
                                needsSave = true;
                                internalNeedsSave = true;
                            }
                        }
                        if (internalNeedsSave) ms.getDB().saveRecruited(information);
                        final String progress;
                        if (information.status == -1) {
                            progress = "§cSpende abgebrochen";
                        } else if (information.status == -2) {
                            progress = "§cungültiger Account";
                        } else if (information.status > 10) {
                            final int count = information.getStoreTransactions().size();
                            if (count == 0)
                                progress = "§fnoch nicht gespendet";
                            else if (count == 1)
                                progress = "§abereits gespendet";
                            else
                                progress = "§abereits " + count + "x gespendet";
                        } else
                            progress = "§fnoch nicht gespendet";
                        inventory.setItem(slot, ms.renameItemStack(ms.getHead(recruitedName), "§f§l" + recruitedName, "§7Geworben §fam " + sdf.format(new Date(information.getTimestamp())), "", "§7Status: " + ms.getPlayerStatus(player, op, null, true), "§7Fortschritt: " + progress));
                        slot++;
                    }
                    if (needsSave)
                        ms.getDB().saveRecruiter(informations.get(), null);
                    ItemStack cashoutItem;
                    RecruiterInformations recruiterInfo = informations.get();
                    if (recruiterInfo.getCurrentCashoutInEur() > 0) {
                        cashoutItem = ms.renameItemStack(new ItemStack(Material.GOLD_BLOCK), "§e§lAuszahlung",
                                "§7Vorläufige Einnahmen: §f" + ms.getDecimalFormat((recruiterInfo.getCurrentAmountInEur() + recruiterInfo.getEstimatedAmountInEur())).replace(".", ",") + " EUR",
                                "", "§7Nachdem die Spenden überprüft wurden,", "§7werden deine Einnahmen zu Profite.", "",
                                "§7Auszuzahlende Profite: §a§n" + ms.getDecimalFormat(recruiterInfo.getCurrentCashoutInEur()).replace(".", ",") + "§7/10 EUR", "", "§eDer Prozess der Auszahlung wird", "§eeinige Tage in Anspruch nehmen..");
                    } else {
                        if (recruiterInfo.getCurrentAmountInEur() < 10) {
                            cashoutItem = ms.renameItemStack(new ItemStack(Material.IRON_BLOCK), "§f§lAuszahlung", "§7Vorläufige Einnahmen: §f" + (ms.getDecimalFormat(recruiterInfo.getEstimatedAmountInEur())).replace(".", ",") + " EUR",
                                    "", "§7Nachdem die Spenden überprüft wurden,", "§7werden deine Einnahmen zu Profite.", "",
                                    "§7Endgültige Profite: §e" + ms.getDecimalFormat(recruiterInfo.getCurrentAmountInEur()).replace(".", ",") + "§7/10 EUR", "",
                                    "§7Sobald die 10 Euro überschritten wurden,", "§7kannst du dir das Geld auszahlen lassen.");
                        } else {
                            cashoutItem = ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK), "§a§lAuszahlung", "§7Vorläufige Einnahmen: §f" + ms.getDecimalFormat(recruiterInfo.getEstimatedAmountInEur()).replace(".", ",") + " EUR",
                                    "", "§7Nachdem die Spenden überprüft wurden,", "§7werden deine Einnahmen zu Profite.", "",
                                    "§7Endgültige Profite: §a" + ms.getDecimalFormat(recruiterInfo.getCurrentAmountInEur()).replace(".", ",") + "§7/10 EUR", "",
                                    "§7Klicke, um dir über eine Währung deiner", "§7Wahl dein Gewinn §aauszahlen zu lassen§7.");
                        }
                    }
                    inventory.setItem(0, cashoutItem);
                    final int currentMaxAmount = 27 * page;
                    if (currentMaxAmount > 27)
                        inventory.setItem(7, ms.getHeadFromURL("https://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9", ms.getLang("market.previouspage")));
                    if (currentMaxAmount < totalAmount.get())
                        inventory.setItem(8, ms.getHeadFromURL("https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf", ms.getLang("market.nextpage")));

                }
            }.runTaskAsynchronously(ms);
            // get amount of pages
            this.inventoryCache.putIfAbsent(player, new HashMap<>());
            this.inventoryCache.get(player).put(page, inventory);
        }
        if (ms.getActiveInv(player.getName()) != MuxInventory.InvType.AFFILIATE) player.closeInventory();
        player.openInventory(inventory);
        player.setMetadata("werbencurrentpage", new FixedMetadataValue(ms, page));
        player.setMetadata("werbencurrentrecruiter", new FixedMetadataValue(ms, recruiter.toString()));
        ms.setActiveInv(player.getName(), MuxInventory.InvType.AFFILIATE);
    }

    public void handleInventory(final Player p, final ItemStack item, final int slot, final Inventory inv) {
        if (item == null) return;
        final String title = inv.getTitle();
        final Material m = item.getType();
        if (title.contains("Auszahlung")) {
            handleCashoutInventory(p, slot, inv);
            return;
        }
        if (m == Material.BOOK) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        } else if (m == Material.SKULL_ITEM) {
            if (slot > 9) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            int currentPage = p.getMetadata("werbencurrentpage").get(0).asInt();
            if (slot == 7) {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openInventory(p, UUID.fromString(p.getMetadata("werbencurrentrecruiter").get(0).asString()), --currentPage);
            } else {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openInventory(p, UUID.fromString(p.getMetadata("werbencurrentrecruiter").get(0).asString()), ++currentPage);
            }
        } else if (title.contains("/") == false) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        } else if (m == Material.IRON_BLOCK) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            ms.showItemBar(p, "§eDu musst mindestens 10 EUR eingenommen haben.");
        } else if (m == Material.GOLD_BLOCK) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            ms.showItemBar(p, "§eDu hast bereits eine Auszahlung beantragt.");
        } else if (m == Material.EMERALD_BLOCK) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openCashoutInventory(p);
        }
    }

    public boolean isEnabled() {
        return ms.getAdmin().AFFILIATE.isActive();
    }


    public class AffiliateUser {
        private final UUID uuid;

        public AffiliateUser(final UUID uuid) {
            this.uuid = uuid;
        }

        public UUID getUUID() {
            return uuid;
        }
    }

    public class CashbackInformations extends AffiliateUser {
        private final List<StoreTransaction> storeTransactions;
        private long status; // 0 nothing, timestamp = first payment date

        public CashbackInformations(final UUID uuid, final long status, final List<StoreTransaction> storeTransactions) {
            super(uuid);
            this.storeTransactions = storeTransactions;
            this.status = status;
        }

        public List<StoreTransaction> getStoreTransactions() {
            return storeTransactions;
        }

        public long getStatus() {
            return status;
        }

        public void addStoreTransaction(final StoreTransaction transaction) {
            this.storeTransactions.add(transaction);
        }
    }

    public class RecruitedInformations extends AffiliateUser {
        private final List<StoreTransaction> storeTransactions;
        private final UUID recruitedBy;
        private long status; // 0 nothing, -1 = chargebacked, timestamp = first payment date, -2 = same ip
        private double totalAmount;
        private final long timestamp;

        public RecruitedInformations(final UUID uuid, final UUID recruitedBy, final long status, final List<StoreTransaction> storeTransactions, final long timestamp) {
            super(uuid);
            this.recruitedBy = recruitedBy;
            this.storeTransactions = storeTransactions;
            this.status = status;
            this.timestamp = timestamp;
            this.updateTotalAmount();
        }

        public double getTotalAmount() {
            return this.totalAmount;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public List<StoreTransaction> getStoreTransactions() {
            return this.storeTransactions;
        }

        public void addStoreTransaction(final StoreTransaction transaction) {
            this.storeTransactions.add(transaction);
            this.updateTotalAmount();
        }

        public UUID getRecruitedBy() {
            return this.recruitedBy;
        }

        public long getStatus() {
            return this.status;
        }

        public void updateTotalAmount() {
            this.totalAmount = storeTransactions == null ? 0 : storeTransactions
                    .stream()
                    .filter(storeTransaction -> storeTransaction.status == 0)
                    .mapToDouble(storeTransaction -> storeTransaction.amount)
                    .sum();
        }
    }

    public class StoreTransaction {
        private final String transactionId;
        private final long timestamp;
        private final double amount;
        private byte status; // -2 -> 14 day wait time, 0 -> normal, 1 -> paid out, -1 -> chargebacked

        public StoreTransaction(final String transactionId, final double amount, final byte status, final long timestamp) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getTransactionId() {
            return transactionId;
        }
    }

    public class RecruiterInformations extends AffiliateUser {
        private CryptoCurrency currency;
        private String cryptoAddress;
        private double currentAmountInEur, estimatedAmountInEur, currentCashoutInEur, totalCashedoutInEur;

        public RecruiterInformations(final UUID uuid, final double currentAmountInEur, final double currentCashoutInEur, final double estimatedAmountInEur,
                                     final double totalCashedoutInEur, final CryptoCurrency currency, final String cryptoAddress) {
            super(uuid);
            this.currentAmountInEur = currentAmountInEur;
            this.totalCashedoutInEur = totalCashedoutInEur;
            this.currentCashoutInEur = currentCashoutInEur;
            this.estimatedAmountInEur = estimatedAmountInEur;
            this.currency = currency;
            this.cryptoAddress = cryptoAddress;
        }

        public CryptoCurrency getCurrency() {
            return currency;
        }

        public String getCryptoAddress() {
            return cryptoAddress;
        }

        public double getCurrentAmountInEur() {
            return currentAmountInEur;
        }

        public double getCurrentCashoutInEur() {
            return currentCashoutInEur;
        }

        public double getEstimatedAmountInEur() {
            return estimatedAmountInEur;
        }

        public double getTotalCashedoutInEur() {
            return totalCashedoutInEur;
        }
    }

    public class TopRecruiter extends AffiliateUser {
        private final int recruited;

        public TopRecruiter(final UUID uuid, final int recruited) {
            super(uuid);
            this.recruited = recruited;
        }
    }

    public enum CryptoCurrency {
        SOLANA("§3§lSolana"),
        LITECOIN("§f§lLitecoin"),
        DOGECOIN("§e§lDogecoin"),
        MONERO("§6§lMonero"),
        BITCOIN_CASH("§a§lBitcoin Cash");

        private final String name;

        CryptoCurrency(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public class StoreTransactionSerializer implements JsonSerializer<StoreTransaction> {
        @Override
        public JsonElement serialize(final StoreTransaction transaction, final Type type, final JsonSerializationContext jsonSerializationContext) {
            final JsonObject object = new JsonObject();
            object.addProperty("id", transaction.transactionId);
            object.addProperty("amount", transaction.amount);
            object.addProperty("status", transaction.status);
            object.addProperty("timestamp", transaction.timestamp);
            return object;
        }
    }

    public class StoreTransactionDeserializer implements JsonDeserializer<StoreTransaction> {
        @Override
        public StoreTransaction deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            final JsonObject object = jsonElement.getAsJsonObject();
            return new StoreTransaction(object.get("id").getAsString(), object.get("amount").getAsDouble(), object.get("status").getAsByte(), object.get("timestamp").getAsLong());
        }
    }
}