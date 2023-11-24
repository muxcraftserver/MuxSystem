package me.muxteam.muxteam;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxActions;
import me.muxteam.basic.MuxFonts;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.joptsimple.internal.Strings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MuxChatFilter implements Listener {
    private MuxSystem ms;
    private final Set<String> playerNames = new HashSet<>();
    private final LinkedHashMap<BlacklistType, LinkedHashSet<String>> blacklist = new LinkedHashMap<>();
    private final Map<String, String> blacklistadd = new HashMap<>();
    private final LinkedHashMap<BlacklistType, LinkedHashSet<UnconfirmedBlacklistWord>> notConfirmedAddBlacklistWords = new LinkedHashMap<>(),
            notConfirmedRemoveBlacklistWords = new LinkedHashMap<>();
    private final Pattern accentfilter = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private FileConfiguration filters;
    private File filtersFile;

    public MuxChatFilter(final MuxSystem ms) {
        this.ms = ms;
        load();
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void close() {
        save();
        this.ms = null;
    }

    public void save() {
        final Gson gson = new Gson();
        final FileConfiguration hashYML = ms.getHashYAML();
        if (blacklist.isEmpty() == false) {
            for (final Map.Entry<BlacklistType, LinkedHashSet<String>> entry : blacklist.entrySet()) {
                filters.set(entry.getKey().configkey, new ArrayList<>(entry.getValue()));
            }
            try {
                filters.save(filtersFile);
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }
        for (final Map.Entry<BlacklistType, LinkedHashSet<UnconfirmedBlacklistWord>> entry : notConfirmedAddBlacklistWords.entrySet()) {
            hashYML.set(entry.getKey().configkey + "unconfirmedadd", gson.toJson(new ArrayList<>(entry.getValue())));
        }
        for (final Map.Entry<BlacklistType, LinkedHashSet<UnconfirmedBlacklistWord>> entry : notConfirmedRemoveBlacklistWords.entrySet()) {
            hashYML.set(entry.getKey().configkey + "unconfirmedremove", gson.toJson(new ArrayList<>(entry.getValue())));
        }
    }

    public void load() {
        final Gson gson = new Gson();
        final FileConfiguration hashYML = ms.getHashYAML();
        filtersFile = new File(ms.getDataFolder(), "chatfilters.yml");
        if (filtersFile.exists() == false) {
            ms.saveResource("chatfilters.yml", false);
        }
        filters = YamlConfiguration.loadConfiguration(filtersFile);

        for (final BlacklistType type : BlacklistType.values()) {
            final LinkedHashSet<String> set = new LinkedHashSet<>(filters.getStringList(type.configkey));
            blacklist.put(type, set);
            List<UnconfirmedBlacklistWord> removeList = new ArrayList<>();
            if (hashYML.get(type.configkey + "unconfirmedremove") != null && hashYML.isString(type.configkey + "unconfirmedremove")) {
                removeList = gson.fromJson(hashYML.getString(type.configkey + "unconfirmedremove"), new TypeToken<List<UnconfirmedBlacklistWord>>() {
                }.getType());
            }
            List<UnconfirmedBlacklistWord> addList = new ArrayList<>();
            if (hashYML.get(type.configkey + "unconfirmedadd") != null && hashYML.isString(type.configkey + "unconfirmedadd")) {
                addList = gson.fromJson(hashYML.getString(type.configkey + "unconfirmedadd"), new TypeToken<List<UnconfirmedBlacklistWord>>() {
                }.getType());
            }
            notConfirmedRemoveBlacklistWords.put(type, new LinkedHashSet<>(removeList));
            notConfirmedAddBlacklistWords.put(type, new LinkedHashSet<>(addList));
        }
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            this.playerNames.add((accentfilter.matcher(java.text.Normalizer.normalize(p.getName().toLowerCase(), java.text.Normalizer.Form.NFD)).replaceAll("").replaceAll("[^\\p{L}\\p{Z}]", "")));
        }
    }

    public void handleJoin(final String name) {
        this.playerNames.add(accentfilter.matcher(java.text.Normalizer.normalize(name.toLowerCase(), java.text.Normalizer.Form.NFD)).replaceAll("").replaceAll("[^\\p{L}\\p{Z}]", ""));
    }

    public Map<String, BlacklistType> checkBlacklists(final String s, final String pname) {
        final Map<String, BlacklistType> map = new HashMap<>();
        final Set<String> whitelistfull = new HashSet<>(blacklist.get(BlacklistType.WHITELIST));
        whitelistfull.addAll(playerNames);
        for (final BlacklistType type : BlacklistType.values()) {
            if (type == BlacklistType.WHITELIST) continue;
            if (type == BlacklistType.NAME) {
                final String word  = checkBlacklist(pname, blacklist.get(type), whitelistfull);
                if (word != null) {
                    map.put(word, type);
                    continue;
                }
            }
            final String word = checkBlacklist(s, blacklist.get(type), whitelistfull);
            if (word != null && (whitelistfull.contains(word) == false)) {
                map.put(word, type);
            }
        }
        return map;
    }

    private String checkBlacklist(final String s, final Set<String> blacklist, final Set<String> whitelist) {
        final String text = accentfilter.matcher(java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)).replaceAll("").replaceAll("[^\\p{L}\\p{Z}]", ""),
                nospace = text.replace(" ", ""),
                leetspeak = s.replace("0", "o").replace("1", "i").replace("3", "e").replace("4", "a").replace("5", "s").replace("\\$", "s").replace("€", "e").replace("!", "i"),
                vu = text.replace("v", "u");
        for (final String word : blacklist) {
            final boolean onlyfullword = word.startsWith("*"), exactequals = word.startsWith("=");
            final String lower = word.toLowerCase().replace("*", "").replace("=", "");
            if (exactequals) {
                if (s.equals(lower)) return word;
                else continue;
            }

            if (text.contains(lower) || s.contains(lower) || leetspeak.contains(lower) || vu.contains(lower)
                    || (onlyfullword == false && (leetspeak.replace(" ", "").contains(lower) || nospace.contains(lower)))) {
                for (final String except : whitelist) {
                    if (except.contains(lower) == false) continue;
                    if ((text.contains(except) && text.matches(".*\\b(" + lower + ")\\b.*") == false)) {
                        return null;
                    } else if (ms.isNumeric(except) && s.contains(except)) return null;
                }
                return word;
            }
        }
        return null;
    }

    public boolean handleFilterCommand(final Player p, final String[] args) {
        if (p.hasPermission("muxsystem.chatfilter") == false) {
            ms.sendNoCMDMessage(p);
            return true;
        } else if (args.length == 0) {
            openFilterInventory(p, null);
            return true;
        } else if (args.length > 1 && args[1].equals("open")) {
            handleFilterCommand(p, new String[0]);
            return false;
        }
        final String lowercase = Strings.join(args, " ").toLowerCase();
        if (ms.notSafeGSON(lowercase)) {
            ms.showItemBar(p, "§cDas Wort darf keine spezielle Zeichen enthalten.");
            return true;
        }
        for (final BlacklistType type : BlacklistType.values()) {
            final LinkedHashSet<String> list = blacklist.get(type);
            if (list.contains(lowercase)) {
                if (notConfirmedRemoveBlacklistWords.get(type).stream().anyMatch(unconfirmedBlacklistWord -> unconfirmedBlacklistWord.word.equalsIgnoreCase(lowercase))) {
                    ms.showItemBar(p, "§eDas Wort wird erst nach Bestätigung entfernt.");
                    return true;
                } else if (notConfirmedAddBlacklistWords.get(type).stream().anyMatch(unconfirmedBlacklistWord -> unconfirmedBlacklistWord.word.equalsIgnoreCase(lowercase))) {
                    final MuxActions.PlayerAction confirm = player -> {
                        notConfirmedAddBlacklistWords.get(type).removeIf(unconfirmedBlacklistWord -> unconfirmedBlacklistWord.word.equalsIgnoreCase(lowercase));
                        blacklist.get(type).remove(lowercase);
                        ms.showItemBar(p, "§fDas Wort '§6" + lowercase + "§f' wurde vom Filter §centfernt§f.");
                        player.closeInventory();
                    }, cancel = player -> player.performCommand("chatfilter");
                    new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lWort entfernen", "§aBestätigen", "§cAbbrechen", new ItemStack(Material.PAPER), "§f§l" + lowercase);
                    return true;
                }
                final MuxActions.PlayerAction confirm = player -> {
                    ms.showItemBar(p, "§fDas Wort '§6" + lowercase + "§f' wurde vom Filter §centfernt§f.");
                    notConfirmedRemoveBlacklistWords.get(type).add(new UnconfirmedBlacklistWord(lowercase, type, p.getUniqueId()));
                    player.closeInventory();
                }, cancel = player -> player.performCommand("chatfilter");
                new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lWort entfernen (" + type.name + ")", "§aBestätigen", "§cAbbrechen", new ItemStack(Material.PAPER), "§f§l" + lowercase);
                return true;
            }
        }
        openFilterInventory(p, lowercase);
        blacklistadd.put(p.getName(), lowercase);
        return true;
    }

    public void openFilterConfirmInventory(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxTeam§0 | Chatfilter");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.HOPPER), "§f§lChatfilter", "§7Alle Wörter, die noch nicht überprüft", "§7wurden, werden komplett aufgelistet."));
        int s = 18;
        final Server sr = ms.getServer();
        for (final UnconfirmedBlacklistWord word : notConfirmedAddBlacklistWords.values().stream().flatMap(Set::stream).collect(Collectors.toList())) {
            final String color = word.type.name.equals("Whitelist") ? "§f" : "§e";
            inv.setItem(s, ms.renameItemStack(new ItemStack(Material.PAPER), "§f§l" + word.word + "§f §a§lhinzufügen", "§7zur Kategorie '" + color + word.type.name + "§7'", "", "§7Vorschlag von " + sr.getOfflinePlayer(word.addedBy).getName(),
                    "", "§aLinksklick§7, um zu bestätigen§7.", "§8Rechtsklick§7, um abzulehnen."));
            s++;
            if (s == 45) break;
        }
        for (final UnconfirmedBlacklistWord word : notConfirmedRemoveBlacklistWords.values().stream().flatMap(Set::stream).collect(Collectors.toList())) {
            if (s == 45) break;
            final String color = word.type.name.equals("Whitelist") ? "§f" : "§e";
            inv.setItem(s, ms.renameItemStack(new ItemStack(Material.PAPER), "§f§l" + word.word + "§f §c§lentfernen", "§7aus Kategorie '" + color + word.type.name + "§7'", "", "§7Vorschlag von " + sr.getOfflinePlayer(word.addedBy).getName(),
                    "", "§cLinksklick§7, um zu bestätigen§7.", "§8Rechtsklick§7, um abzulehnen."));
            s++;
        }
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.FILTER && it != MuxInventory.InvType.TEAM && it != MuxInventory.InvType.CONFIRM) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.FILTER);
    }

    public void openFilterInventory(final Player p, final String word) {
        final boolean add = word != null;
        boolean player = false;
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxFilter§0 | " + (add ? "Wort hinzufügen" : "/filter"));
        if (add) inv.setItem(4, ms.renameItemStack(new ItemStack(Material.PAPER), "§f§l" + word));
        final OfflinePlayer op = word != null ? ms.getPlayer(word) : null;
        if ((add && op != null && op.hasPlayedBefore()) || add == false) {
            inv.setItem(add ? 22 : 13, ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 1, (short) 3), "§e§lSpielernamen",
                    "§7Wenn der Nametag echte Daten oder", "§7Werbung für andere Server enthält", "",
                    add ? "§7Klicke, um zum Filter §ahinzuzufügen§7." : "§7Klicke, um die ganze Liste zu §esehen§7."));
            player = true;
        }
        inv.setItem(add ? (player ? 18 : 19) : 10, ms.renameItemStack(new ItemStack(Material.ARROW), "§e§lBeleidigungen",
                "§7Vulgäre Begriffe, die diesen Server", "§7nicht mehr kinderfreundlich machen", "",
                add ? "§7Klicke, um zum Filter §ahinzuzufügen§7." : "§7Klicke, um die ganze Liste zu §esehen§7."));
        inv.setItem(add ? (player ? 20 : 21) : 12, ms.renameItemStack(new ItemStack(Material.NAME_TAG), "§e§lDoxing",
                "§7Volle Namen, IPs, Handynummern, echte", "§7Adressen, §nkeine§7 Städte oder Vornamen", "",
                add ? "§7Klicke, um zum Filter §ahinzuzufügen§7." : "§7Klicke, um die ganze Liste zu §esehen§7."));
        inv.setItem(add ? (player ? 24 : 23) : 14, ms.renameItemStack(new ItemStack(Material.SIGN), "§e§lWerbung",
                "§7Servernamen oder -sprüche, andere", "§7Videospiele oder Handel mit Echtgeld", "",
                add ? "§7Klicke, um zum Filter §ahinzuzufügen§7." : "§7Klicke, um die ganze Liste zu §esehen§7."));
        inv.setItem(add ? (player ? 26 : 25) : 16, ms.renameItemStack(new ItemStack(Material.QUARTZ), "§f§lWhitelist",
                "§7Wenn normale Wörter aus Versehen", "§7auch vom Chatfilter geblockt sind", "",
                add ? "§7Klicke, um zur Whitelist §ahinzuzufügen§7." : "§7Klicke, um die ganze Liste zu §fsehen§7."));
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.FILTER) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.FILTER);
    }

    public int getNotConfirmed() {
        int count = 0;
        for (final LinkedHashSet<UnconfirmedBlacklistWord> set : notConfirmedAddBlacklistWords.values()) {
            count += set.size();
        }
        for (final LinkedHashSet<UnconfirmedBlacklistWord> set : notConfirmedRemoveBlacklistWords.values()) {
            count += set.size();
        }
        return count;
    }

    public void handleInventory(final Player p, final ItemStack i, boolean rightclick, int size) {
        if (size == 27) {
            final Material material = i.getType();
            final BlacklistType type = material == Material.ARROW ? BlacklistType.BADWORD : (material == Material.NAME_TAG ? BlacklistType.DOX
                    : (material == Material.SIGN ? BlacklistType.ADFILTER : material == Material.SKULL_ITEM ? BlacklistType.NAME : material == Material.QUARTZ ? BlacklistType.WHITELIST : null));
            final String preword = blacklistadd.get(p.getName());
            if (preword != null) {
                if (type == null) return;
                final String word = preword.toLowerCase().replaceAll("\\s", "");
                final LinkedHashSet<String> list = blacklist.get(type);
                notConfirmedAddBlacklistWords.get(type).add(new UnconfirmedBlacklistWord(word, type, p.getUniqueId()));
                list.add(word);
                ms.showItemBar(p, "§fDas Wort '§6" + word + "§f' wurde zum Filter §ahinzugefügt§f.");
                p.closeInventory();
            } else if (type != null) {
                openFilterList(p, type);
            }
            return;
        }
        final Material m = i.getType();
        if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            ms.getTeam().handleCommand(p);
            return;
        } else if (m == Material.PAPER) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            final ItemMeta meta = i.getItemMeta();
            final String word = ChatColor.stripColor(meta.getDisplayName().split(" ")[0]);
            final boolean add = meta.getDisplayName().contains("§a");
            if (add) {
                final Optional<Map.Entry<BlacklistType, LinkedHashSet<UnconfirmedBlacklistWord>>> optSet = notConfirmedAddBlacklistWords.entrySet().stream().filter(entry -> entry.getValue().stream().anyMatch(w -> w.word.equals(word))).findFirst();
                final UnconfirmedBlacklistWord blacklistedWord = optSet.flatMap(blacklistTypeLinkedHashSetEntry -> blacklistTypeLinkedHashSetEntry.getValue().stream().filter(w -> w.word.equalsIgnoreCase(word)).findFirst()).orElse(null);
                if (blacklistedWord == null) {
                    p.closeInventory();
                    return;
                }
                final LinkedHashSet<UnconfirmedBlacklistWord> set = optSet.get().getValue();
                if (rightclick) {
                    set.remove(blacklistedWord);
                    ms.showItemBar(p, "§fDieser Vorschlag wurde §7abgelehnt§f.");
                    blacklist.get(blacklistedWord.type).remove(blacklistedWord.word);
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION",  "CHATFILTER DENIED", blacklistedWord.word + " ADD");
                    openFilterConfirmInventory(p);
                } else {
                    set.remove(blacklistedWord);
                    openFilterConfirmInventory(p);
                    ms.showItemBar(p, "§fDieser Vorschlag wurde §abestätigt§f.");
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "CHATFILTER CONFIRMED", blacklistedWord.word + " ADD");
                }
            } else {
                final Optional<Map.Entry<BlacklistType, LinkedHashSet<UnconfirmedBlacklistWord>>> optSet = notConfirmedRemoveBlacklistWords.entrySet().stream().filter(entry -> entry.getValue().stream().anyMatch(w -> w.word.equals(word))).findFirst();
                final UnconfirmedBlacklistWord blacklistedWord = optSet.flatMap(blacklistTypeLinkedHashSetEntry -> blacklistTypeLinkedHashSetEntry.getValue().stream().filter(w -> w.word.equalsIgnoreCase(word)).findFirst()).orElse(null);
                if (blacklistedWord == null) {
                    p.closeInventory();
                    return;
                }
                final LinkedHashSet<UnconfirmedBlacklistWord> set = optSet.get().getValue();
                if (rightclick) {
                    set.remove(blacklistedWord);
                    ms.showItemBar(p, "§fDieser Vorschlag wurde §7abgelehnt§f.");
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION",  "CHATFILTER DENIED", blacklistedWord.word + " DELETE");
                    openFilterConfirmInventory(p);
                } else {
                    set.remove(blacklistedWord);
                    blacklist.get(blacklistedWord.type).remove(blacklistedWord.word);
                    openFilterConfirmInventory(p);
                    ms.showItemBar(p, "§fDieser Vorschlag wurde §cbestätigt§f.");
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "CHATFILTER CONFIRMED", blacklistedWord.word + " DELETE");
                }
            }
            return;
        }
        p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
    }

    public void openFilterList(final Player p, final BlacklistType type) {
        final MuxFonts fonts = ms.getFonts();
        StringBuilder book = new StringBuilder(fonts.centerBook("§7§lMux§9§lFilter") + "\n\n§0" + fonts.centerBook("Die Liste der Wörter") + "\n§0" + fonts.centerBook("die vom Chat") + "\n" + fonts.centerBook(type == BlacklistType.WHITELIST ? "nicht" : ("für " + type.name)) + "§0\n§0" + fonts.centerBook("gefiltert werden") + "\n\n§8" + fonts.centerBook("-*-") + "\n\n");
        byte count = 0;
        final List<String> pages = new ArrayList<>();
        final LinkedHashSet<String> filter = blacklist.get(type);
        for (final String s : filter) {
            count++;
            book.append("§0- §1").append(s).append("\n");
            if ((pages.isEmpty() && count % 6 == 0) || count % 12 == 0) {
                pages.add(book.toString());
                book = new StringBuilder();
                count = 0;
            }
        }
        pages.add(book.toString());
        ms.openBook(ms.book("§aMuxFilter", pages.toArray(new String[0])), p);
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent e) {
        blacklistadd.remove(e.getPlayer().getName());
    }

    public class UnconfirmedBlacklistWord {
        private final String word;
        private final BlacklistType type;
        private final UUID addedBy;

        public UnconfirmedBlacklistWord(final String word, final BlacklistType type, final UUID addedBy) {
            this.word = word;
            this.type = type;
            this.addedBy = addedBy;
        }
    }

    public enum BlacklistType {
        BADWORD("blacklist", "Beleidigungen"),
        DOX("dox", "Doxing"),
        ADFILTER("adblacklist", "Werbung"),
        NAME("namefilter", "Spielernamen"),
        WHITELIST("whitelist", "Whitelist");

        private final String configkey, name;

        BlacklistType(final String configkey, final String name) {
            this.configkey = configkey;
            this.name = name;
        }
    }
}