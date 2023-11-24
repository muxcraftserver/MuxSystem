package me.muxteam.ranks;

import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.extras.MuxMounts;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class MuxGold {
    private MuxSystem ms;
    private final HashSet<Player> rain = new HashSet<>();
    private final Set<UUID> savecooldown = new HashSet<>();
    private final Set<UUID> effectdeactivated = new HashSet<>();
    private final Map<String, String> leftClickMacros = new HashMap<>(), rightClickMacros = new HashMap<>();
    private final Inventory pweatherinv;

    public MuxGold(final MuxSystem ms) {
        this.ms = ms;
        pweatherinv = ms.getServer().createInventory(null, 45, ms.getLang("cmd.pweatherinv"));
        pweatherinv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        pweatherinv.setItem(4, ms.renameItemStack(new ItemStack(Material.RED_ROSE, 1, (byte) 1), ms.getLang("cmd.pweather"), ms.getLang("cmd.pweatherlore4"), ms.getLang("cmd.pweatherlore5")));
        pweatherinv.setItem(20, ms.renameItemStack(new ItemStack(Material.MAGMA_CREAM), ms.getLang("cmd.pweathersun"), "", ms.getLang("cmd.pweatherlore"), ms.getLang("cmd.pweatherlore1")));
        pweatherinv.setItem(22, ms.renameItemStack(new ItemStack(Material.WATER_BUCKET), ms.getLang("cmd.pweatherrain"), "", ms.getLang("cmd.pweatherlore"), ms.getLang("cmd.pweatherlore2")));
        pweatherinv.setItem(24, ms.renameItemStack(new ItemStack(Material.SNOW_BALL), ms.getLang("cmd.pweathersnow"), "", ms.getLang("cmd.pweatherlore"), ms.getLang("cmd.pweatherlore3")));
    }

    public void close() {
        this.ms = null;
    }

    public boolean hasSchematicCooldown(final UUID uuid) {
        if (savecooldown.contains(uuid)) {
            return true;
        } else if (ms.getDB().canSaveBaseSchematic(uuid) != -1) {
            savecooldown.add(uuid);
            return true;
        }
        return false;
    }

    public boolean handleCommand(final Player p) {
        if (ms.hasGold(p.getUniqueId()) == false) {
            ms.sendNoRankMessage(p, "GOLD");
            return true;
        }
        final UUID uuid = p.getUniqueId();
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxGold§0 | /gold");
        final boolean cooldown = hasSchematicCooldown(uuid);
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.GOLD_BLOCK), "§e§lMuxGold",
                "§7Hier sind alle Features gelistet, die für", "§7diesen exklusiven Rang bestimmt sind."));
        inv.setItem(19, ms.renameItemStack(new ItemStack(Material.SAPLING), "§e§lBase exportieren",
                "§7Hiermit kannst du täglich eine Schematic", "§7von deiner Base direkt herunterladen.", "",
                cooldown ? "§7Klicke, um deine §aBase herunterzuladen§7." : "§7Klicke, um deine §eBase zu exportieren§7."));
        inv.setItem(21, ms.renameItemStack(new ItemStack(Material.RED_ROSE, 1, (byte) 1), ms.getLang("cmd.pweather"), ms.getLang("cmd.pweatherlore4"), ms.getLang("cmd.pweatherlore5"), "", "§eKlicke§7, um das Wetter einzustellen."));
        final String chatprefix = ms.getChat().getPrefix(p.getUniqueId());
        final ItemStack chatcolor = ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 6), "§e§lChatfarbe" + (chatprefix != null ? ":§r " + chatprefix.replace("§", "&") : ""), "§7Diese wird vor jeder Nachricht", "§7im Chat automatisch verwendet.", "", "§7Klicke, um diese zu " + (chatprefix == null ? "§esetzen" : "§centfernen") + "§7.");
        inv.setItem(23, chatprefix != null ? ms.addGlow(chatcolor) : chatcolor);
        final boolean mounted = ms.getMounts().getMountType(uuid) == (short) 14;
        final ItemStack snake = ms.addLore(ms.renameItemStack(new ItemStack(Material.MAGMA_CREAM),
                        "§e§lFliegende Schlange" + (mounted ? "§a §laktiviert" : ""), "§7Mit diesen exklusiven Mount kannst", "§7du quer durch die Gegend fliegen.", ""),
                mounted ? new String[]{"§7Klicke, um den Mount zu §cdeaktivieren§7."} :
                        new String[]{"§7Klicke, um den Mount zu §aaktivieren§7."});
        inv.setItem(25, mounted ? ms.addGlow(snake) : snake);
        inv.setItem(29, ms.renameItemStack(new ItemStack(Material.GOLD_PICKAXE), "§e§lWerkzeuge reparieren",
                "§7Nun werden neben Kampfitems auch", "§7alle Werkzeuge wiederhergestellt.", "", "§7Klicke§7, um die Items zu §ereparieren§7."));
        inv.setItem(31, ms.renameItemStack(new ItemStack(Material.COMMAND), "§e§lMacro setzen",
                "§7Setze ein Befehl auf ein Item, um", "§7ihn direkt ausführen zu können.", "", "§7Setze macros mit §e/macro"));
        final boolean noeffect = effectdeactivated.contains(uuid);
        final ItemStack particles = ms.renameItemStack(new ItemStack(Material.BLAZE_POWDER), "§e§lPartikel" + (noeffect ? " §c§ldeaktiviert" : ""),
                "§7Nur diejenigen mit Gold haben", "§7diese unverkennbare Aura.", "", "§7Klicke, um sie zu " + (noeffect ? "§a" : "§cde") + "aktivieren§7.");
        inv.setItem(33, noeffect ? ms.addGlow(particles) : particles);
        inv.setItem(37, ms.renameItemStack(new ItemStack(Material.BOOK_AND_QUILL), "§e§lFarbige MSGs",
                "§7Du kannst nun alle Chatfarben", "§7in privaten MSGs verwenden.", "", "§aDas Feature ist freigeschaltet."));
        inv.setItem(39, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§e§lDoppelte MuxCoins",
                "§7Ab sofort erhältst du zwei mal", "§7so viele MuxCoins beim Minen.", "", "§aDas Feature ist freigeschaltet."));
        inv.setItem(41, ms.renameItemStack(new ItemStack(Material.BARRIER), "§e§lKeine Werbung",
                "§7Die regelmäßigen Broadcasts im", "§7Chat erscheinen nicht mehr.", "", "§aDas Feature ist freigeschaltet."));
        inv.setItem(43, ms.renameItemStack(new ItemStack(Material.SIGN), "§e§lFreiheit im Chat",
                "§7Mit weniger Cooldowns und CAPS", "§7kannst du dich nun ausdrücken.", "", "§aDas Feature ist freigeschaltet."));
        inv.setItem(47, ms.renameItemStack(new ItemStack(Material.GOLD_PLATE), "§e§lGoldener Rand",
                "§7Die Grenze deiner Base besteht", "§7aus edlem und glänzendem Gold.", "", "§aDas Feature ist freigeschaltet."));
        final boolean hasgold = ms.hasRankNoPerms(uuid, "Gold");
        if (hasgold) {
            final MuxRanks.PermissionsUser pu = ms.getPerms().getUserData(uuid);
            if (pu.getExpireData() != null && pu.getExpireData().getExpireTime() > System.currentTimeMillis()) {
                final String time = (ms.getTime((int) ((pu.getExpireData().getExpireTime() - System.currentTimeMillis()) / 1000)));
                final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
                format.setTimeZone(TimeZone.getTimeZone("CET"));
                inv.setItem(51, ms.renameItemStack(new ItemStack(Material.WATCH), "§e§l" + time, "§7Dein Gold Rang ist noch bis" , "§7zu dem " + format.format(new Date(pu.getExpireData().getExpireTime())) + " §7gültig. Du", "§7kannst ihn hier verlängern.", "", "§eKlicke§7, für 30 weitere Tage."));
            }
        }
        inv.setItem(hasgold ? 49 : 51, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§e§lCasino Bereich",
                "§7Ein exklusiver Bereich für alle", "§7im Gold befindet sich im Casino.", "",  "§aDas Feature ist freigeschaltet."));
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.GOLD && it != MuxInventory.InvType.PWEATHER && it != MuxInventory.InvType.X && it != MuxInventory.InvType.MENU && it != MuxInventory.InvType.CONFIRM) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.GOLD);
        return true;
    }

    public void handleInventory(final Player p, final ItemStack i) {
        final Material m = i.getType();
        if (m == Material.INK_SACK && i.getDurability() == 6) {
            if (ms.getChat().getPrefix(p.getUniqueId()) != null) {
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                ms.getChat().setChatColor(p, null);
                handleCommand(p);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            new MuxAnvil(ms, (input, pl) -> {
                input = ms.retardCleaner(input, "Chatfarbe: ");
                pl.closeInventory();
                if (ms.getChat().setChatColor(pl, input) == false) {
                    pl.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                } else {
                    pl.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                }
                handleCommand(p);
            }).show(p, "Chatfarbe: ");
        } else if (m == Material.GOLD_PICKAXE) {
            p.performCommand("repair");
        } else if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (p.hasPermission("muxsystem.x")) {
                p.performCommand("x");
                return;
            }
            ms.getMenu().getBenefits().openPremiumInv(p);
        } else if (m == Material.MAGMA_CREAM) {
            if (ms.inBattle(p.getName(), p.getLocation())) {
                ms.showItemBar(p, "§cMounts sind im Kampf deaktiviert.");
                p.closeInventory();
                return;
            } else if (ms.inCasino(p)) {
                ms.showItemBar(p, "§cMounts sind hier deaktiviert.");
                p.closeInventory();
                return;
            }
            final MuxMounts.MountStore mountStore = ms.getExtras().getExtraUser(p.getUniqueId()).getMounts();
            ms.getMounts().setMount(p, (short) 14, true);
            mountStore.setActive(mountStore.getActive() == 14 ? -1 : (short) 14);
            ms.getExtras().getExtraUser(p.getUniqueId()).setMounts(mountStore);
            handleCommand(p);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
        } else if (m == Material.RED_ROSE) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("pweather");
        } else if (m == Material.SAPLING) {
            p.performCommand("base schematic");
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
        } else if (m == Material.BLAZE_POWDER) {
            if (effectdeactivated.remove(p.getUniqueId()) == false) {
                effectdeactivated.add(p.getUniqueId());
            }
            handleCommand(p);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
        } else if (m == Material.WATCH) {
            final MuxUser u = ms.getMuxUser(p.getName());
            new ConfirmInventory(ms, p1 -> {
                final MuxRanks.PermissionsUser pu = ms.getPerms().getUserData(p.getUniqueId());
                if (pu.getExpireData() == null || pu.getGroup().equalsIgnoreCase("Gold") == false) {
                    handleCommand(p);
                    return;
                }
                if (u.getGems() < 4000) {
                    p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    ms.showItemBar(p, "§cDu benötigst 4.000 MuxGems zum verlängern.");
                    return;
                }
                u.setGems(u.getGems() - 4000);
                pu.getExpireData().setExpireTime(pu.getExpireData().getExpireTime() + 2592000000L);
                ms.saveMuxUser(u);
                ms.sendScoreboard(p1);
                ms.getDB().savePermissionsUser(pu);
                final Firework firework = p1.getWorld().spawn(p1.getLocation(), Firework.class);
                final FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(Color.YELLOW)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .trail(true)
                        .build());
                meta.setPower(0);
                firework.setFireworkMeta(meta);
                p1.playSound(p1.getLocation(), Sound.LEVEL_UP, 1F, 1F);
                p1.closeInventory();
                ms.showItemBar(p1, "§fDu hast den §e§lGold Rang§f erfolgreich §averlängert§f.");
            }, p1 -> handleCommand(p))
                    .show(p, "§0§lMuxGold§0 | Verlängern (30 Tage)", "§aVerlängern (4.000 Gems)", "§cAbbrechen");
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        }
    }

    public boolean handleMacroCommand(final Player p, final String[] args) {
        if (ms.hasGold(p.getUniqueId()) == false) {
            ms.sendNoRankMessage(p, "GOLD");
            return true;
        }
        final ItemStack item = p.getInventory().getItemInHand();
        if (item == null || item.getType() == Material.AIR) {
            ms.showItemBar(p, "§cDu musst ein Item in der Hand halten.");
            return true;
        }
        final String itemKey = item.getType().name() + ':' + item.getDurability() + ':' + p.getUniqueId().toString();
        if (leftClickMacros.remove(itemKey) != null || rightClickMacros.remove(itemKey) != null) {
            final String iname = ms.getShop().getItemName(item);
            ms.showItemBar(p, "§fMacros auf '" + (iname == null ? item.getType().name() : iname) + "' §cdeaktiviert§f.");
            return true;
        } else if (args.length == 0) {
            ms.showItemBar(p, ms.usage("/macro [befehl]"));
            return true;
        }
        final String s = ms.fromArgs(args, 0).replace("/", "");
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMacro setzen");
        inv.setItem(4, ms.renameItemStack(new ItemStack(item.getType(), 1, item.getDurability()), "§a§l/" + s));
        ms.createButton(inv, 19, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 8), "§fLinksklick"));
        ms.createButton(inv, 23, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 1), "§6Rechtsklick"));
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.MACRO) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.MACRO);
        return true;
    }

    public void handleMacroInv(final Player p, final ItemStack i, final Inventory inv) {
        final ItemStack hand = p.getInventory().getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) {
            ms.showItemBar(p, "§cDu musst ein Item in der Hand halten.");
            return;
        }
        final ItemStack commandItem = inv.getItem(4);
        if (commandItem == null || commandItem.hasItemMeta() == false || commandItem.getItemMeta().hasDisplayName() == false) {
            return;
        }
        final String command = ChatColor.stripColor(commandItem.getItemMeta().getDisplayName()).replace("/", "");
        if (i.getType() == Material.STAINED_CLAY) {
            final byte data = i.getData().getData();
            final String iname = ms.getShop().getItemName(hand);
            if (data == 8) {
                final String itemKey = hand.getType().name() + ':' + hand.getDurability() + ':' + p.getUniqueId().toString();
                leftClickMacros.put(itemKey, command);
                ms.showItemBar(p, "§fLinksklick auf '" + (iname == null ? hand.getType().name() : iname) + "' §fführt aus: §6/" + command);
                p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
            } else if (data == 1) {
                if (hand.getType().isBlock() && (command.toLowerCase().contains("smelt") || command.toLowerCase().contains("stack"))) {
                    ms.showItemBar(p, "§cDer Befehl ist für Blöcke in Macros deaktiviert.");
                } else {
                    final String itemKey = hand.getType().name() + ':' + hand.getDurability() + ':' + p.getUniqueId().toString();
                    rightClickMacros.put(itemKey, command);
                    ms.showItemBar(p, "§fRechtsklick auf '" + (iname == null ? hand.getType().name() : iname) + "' §fführt aus: §6/" + command);
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
                }
            }
            p.closeInventory();
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        }
    }

    public void handleMacro(final Player p, final ItemStack item, final boolean rightclick) {
        if (item == null) return;
        final Material m = item.getType();
        if (m == Material.AIR) return;
        final String itemKey = m.name() + ':' + item.getDurability() + ':' + p.getUniqueId().toString();
        if (rightclick) {
            final String rightClickCommand = rightClickMacros.get(itemKey);
            if (rightClickCommand != null) {
                ms.runCommand(p, ms.getServer(), "/" + rightClickCommand);
            }
        } else {
            final String leftClickCommand = leftClickMacros.get(itemKey);
            if (leftClickCommand != null) {
                ms.runCommand(p, ms.getServer(), "/" + leftClickCommand);
            }
        }
    }

    public boolean handleWeatherCommand(final Player p) {
        if (ms.hasGold(p.getUniqueId()) == false) {
            ms.sendNoRankMessage(p, "GOLD");
            return true;
        }
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.PWEATHER && it != MuxInventory.InvType.GOLD) p.closeInventory();
        p.openInventory(pweatherinv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.PWEATHER);
        return true;
    }

    public void checkRain(final Player p) {
        if (rain.contains(p)) {
            p.setPlayerWeather(WeatherType.DOWNFALL);
        }
    }

    public void handlePWeatherInv(final Player p, final Material m) {
        switch (m) {
            case MAGMA_CREAM:
                rain.remove(p);
                ms.getSnow().deactivateSnow(p);
                p.setPlayerWeather(WeatherType.CLEAR);
                ms.showItemBar(p, "§fDein Wetter wurde auf §f'§eSonne§f' §fgesetzt.");
                break;
            case WATER_BUCKET:
                ms.getSnow().deactivateSnow(p);
                rain.add(p);
                p.setPlayerWeather(WeatherType.DOWNFALL);
                ms.showItemBar(p, "§fDein Wetter wurde auf §f'§9Regen§f' §fgesetzt.");
                break;
            case SNOW_BALL:
                rain.remove(p);
                ms.getSnow().activateSnow(p);
                p.setPlayerWeather(WeatherType.DOWNFALL);
                ms.showItemBar(p, "§fDein Wetter wurde auf §f'§fSchnee§f' §fgesetzt.");
                break;
            case RED_ROSE:
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            case ITEM_FRAME:
                p.performCommand("gold");
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            default:
                break;
        }
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
    }

    final Map<Player, Location> lastp = new HashMap<>();

    public void sendParticles() {
        stepX++;
        if (stepX > maxStepX) {
            stepX = 0;
        }
        if (reverse) {
            stepY++;
            if (stepY > maxStepY)
                reverse = false;
        } else {
            stepY--;
            if (stepY < -maxStepY)
                reverse = true;
        }
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            if (ms.hasGold(p.getUniqueId()) == false) continue;
            if (effectdeactivated.contains(p.getUniqueId()) == false && (ms.inEvent(p) == false && ms.isVanish(p) == false)) {
                final Location l = lastp.get(p), ploc = p.getLocation();
                if (l != null && l.getWorld().equals(ploc.getWorld()) && l.distance(ploc) <= 0.2 && p.isOnGround() && ms.inBattle(p.getName(), p.getLocation()) == false) {
                    drawQuadhelixParticles(ploc.clone().add(0, 1, 0));
                }
                lastp.put(p, ploc);
            }
        }
    }

    private int stepX = 0, stepY = 0;
    private boolean reverse = false;

    private final int orbs = 4, maxStepX = 80, maxStepY = 60;

    private void drawQuadhelixParticles(final Location location) {
        for (int i = 0; i < orbs; i++) {
            double dx = -(Math.cos((stepX / (double) maxStepX) * (Math.PI * 2) + (((Math.PI * 2) / orbs) * i))) * ((maxStepY - Math.abs(stepY)) / (double) maxStepY);
            double dy = (stepY / (double) maxStepY) * 1.5;
            double dz = -(Math.sin((stepX / (double) maxStepX) * (Math.PI * 2) + (((Math.PI * 2) / orbs) * i))) * ((maxStepY - Math.abs(stepY)) / (double) maxStepY);
            Location target = location.clone().add(dx, dy, dz);
            float r = (float) 255 / 255, g = (float) 215 / 255, b = (float) 1 / 255;
            location.getWorld().spigot().playEffect(target, Effect.COLOURED_DUST, 0, 1, r, g, b, 1, 0, 30);
        }
    }
}