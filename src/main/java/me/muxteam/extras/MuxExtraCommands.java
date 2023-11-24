package me.muxteam.extras;

import me.muxteam.basic.MuxLocation;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.ChatMessage;
import net.minecraft.server.v1_8_R3.ContainerAnvil;
import net.minecraft.server.v1_8_R3.ContainerEnchantTable;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutOpenWindow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MuxExtraCommands {
    private MuxSystem ms;
    private final MuxExtras extras;
    private final Map<Player, List<MuxLocation>> enchantlocs = new HashMap<>();

    private final int maxSaveExp = levelToExp(30);

    public MuxExtraCommands(final MuxSystem ms) {
        this.ms = ms;
        this.extras = ms.getExtras();
    }

    public void close() {
        this.ms = null;
    }

    public boolean handleExtraCommands(final String scmd, final Player p, final String[] args) {
        if (scmd.equalsIgnoreCase("anvil")) {
            if (extras.hasntCommand(p.getUniqueId(), "anvil")) {
                p.performCommand("extracmd");
                return true;
            }
            p.closeInventory();
            final EntityPlayer ep = ((CraftPlayer) p).getHandle();
            final ContainerAnvil container = new ContainerAnvil(ep.inventory, ep.world, new BlockPosition(0, 0, 0), ep);
            container.checkReachable = false;
            final int c = ep.nextContainerCounter();
            ep.playerConnection.sendPacket(new PacketPlayOutOpenWindow(c, "minecraft:anvil", new ChatMessage("Repairing"), 0));
            ep.activeContainer = container;
            ep.activeContainer.windowId = c;
            ep.activeContainer.addSlotListener(ep);
            return true;
        } else if (scmd.equalsIgnoreCase("bodysee")) {
            if (extras.hasntCommand(p.getUniqueId(), "bodysee")) {
                p.performCommand("extracmd");
                return true;
            } else if (args.length != 1) {
                ms.showItemBar(p, ms.usage("/bodysee [" + ms.getLang("player") + "]"));
                return true;
            }
            final Player victim = ms.getServer().getPlayer(args[0]);
            final String pname = p.getName();
            if (victim == null || p.canSee(victim) == false) {
                ms.showItemBar(p, ms.hnotonline);
                return true;
            } else if (victim.getName().equals(pname)) {
                ms.showItemBar(p, ms.getLang("cmd.bodyseeopenyours"));
                return true;
            }
            p.closeInventory();
            final Inventory i = ms.getServer().createInventory(null, InventoryType.HOPPER, ms.getLang("cmd.bodyseeinv") + " " + victim.getName());
            final ItemStack[] armor = victim.getInventory().getArmorContents();
            i.setItem(0, armor[3]);
            i.setItem(1, armor[2]);
            i.setItem(2, ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15), "§a"));
            i.setItem(3, armor[1]);
            i.setItem(4, armor[0]);
            p.openInventory(i);
            ms.setActiveInv(pname, InvType.ARMOR);
            return true;
        } else if (scmd.equalsIgnoreCase("bottle")) {
            if (extras.hasntCommand(p.getUniqueId(), "bottle")) {
                p.performCommand("extracmd");
                return true;
            }
            final PlayerInventory pi = p.getInventory();
            final short count = ms.getMaterialCount(pi, Material.GLASS, (byte) 0);
            if (count < 1) {
                ms.showItemBar(p, "§cDein Inventar enthält nicht genug Glas.");
                return true;
            }
            pi.remove(Material.GLASS);
            pi.addItem(new ItemStack(Material.GLASS_BOTTLE, count));
            p.playSound(p.getLocation(), Sound.GLASS, 1F, 1F);
            ms.showItemBar(p, "§fDeine §6" + count + " Glasblöcke §fwurden in §6" + count + " Glasflaschen §fumgetauscht.");
            return true;
        } else if (scmd.equalsIgnoreCase("cook")) {
            if (extras.hasntCommand(p.getUniqueId(), "cook")) {
                p.performCommand("extracmd");
                return true;
            }
            final ItemStack hand = p.getItemInHand();
            final ItemStack cook = getCookedItemStack(hand);
            if (cook == null) {
                ms.showItemBar(p, ms.getLang("cmd.cooknoitem"));
                return true;
            }
            p.playSound(p.getLocation(), Sound.FIZZ, 1F, 1F);
            p.setItemInHand(cook);
            p.updateInventory();
            ms.showItemBar(p, ms.getLang("cmd.cooked"));
            return true;
        } else if (scmd.equalsIgnoreCase("fill")) {
            if (extras.hasntCommand(p.getUniqueId(), "fill")) {
                p.performCommand("extracmd");
                return true;
            }
            final ItemStack hand = p.getItemInHand();
            if (hand == null || hand.getType() != Material.GLASS_BOTTLE) {
                ms.showItemBar(p, ms.getLang("cmd.fillnoitems"));
                return true;
            }
            p.playSound(p.getLocation(), Sound.SPLASH, 1F, 2F);
            hand.setType(Material.POTION);
            ms.showItemBar(p, ms.getLang("cmd.filled"));
            return true;
        } else if (scmd.equalsIgnoreCase("goldswitch")) {
            if (extras.hasntCommand(p.getUniqueId(), "goldswitch")) {
                p.performCommand("extracmd");
                return true;
            }
            final PlayerInventory pi = p.getInventory();
            final short count = ms.getMaterialCount(pi, Material.GOLD_NUGGET, (byte) 0);
            if (count < 9) {
                ms.showItemBar(p, ms.getLang("cmd.goldswitchnoitem"));
                return true;
            }
            pi.remove(Material.GOLD_NUGGET);
            pi.addItem(new ItemStack(Material.GOLD_INGOT, count / 9));
            if (count % 9 != 0) pi.addItem(new ItemStack(Material.GOLD_NUGGET, count % 9));
            p.playSound(p.getLocation(), Sound.ANVIL_LAND, 1F, 1F);
            ms.showItemBar(p, String.format(ms.getLang("cmd.goldswitched"), count - count % 9, count / 9));
            return true;
        } else if (scmd.equalsIgnoreCase("smelt")) {
            if (extras.hasntCommand(p.getUniqueId(), "smelt")) {
                p.performCommand("extracmd");
                return true;
            }
            final ItemStack hand = p.getItemInHand(), smelt = getSmeltedItemStack(hand);
            if (smelt == null) {
                ms.showItemBar(p, ms.getLang("cmd.smeltnoitem"));
                return true;
            }
            p.playSound(p.getLocation(), Sound.FIZZ, 1F, 1F);
            p.setItemInHand(smelt);
            p.updateInventory();
            ms.showItemBar(p, ms.getLang("cmd.smelted"));
            return true;
        } else if (scmd.equalsIgnoreCase("bottlexp")) {
            if (extras.hasntCommand(p.getUniqueId(), "bottlexp")) {
                p.performCommand("extracmd");
                return true;
            }
            final ItemStack is = p.getItemInHand();
            final PlayerInventory pi = p.getInventory();
            if (is == null || is.getType() != Material.GLASS_BOTTLE) {
                ms.showItemBar(p, "§cDu musst eine Glasflasche in deiner Hand halten.");
                return true;
            } else if (pi.firstEmpty() == -1) {
                ms.showItemBar(p, "§cDein Inventar ist voll.");
                return true;
            } else if (p.getLevel() < 1) {
                ms.showItemBar(p, "§cDu benötigst mindestens ein Level, um eine EXP Flasche zu erstellen.");
                return true;
            }
            pi.removeItem(new ItemStack(Material.GLASS_BOTTLE));
            final int totalExp = getPlayerExperience(p);
            final int expToSave = Math.min(totalExp, maxSaveExp);
            final int leftoverExp = totalExp - expToSave;
            if (leftoverExp > 0)
                setTotalExperienceNew(p, leftoverExp);
            else {
                p.setExp(0);
                p.setLevel(0);
                p.setTotalExperience(0);
            }
            final ItemStack bottle = ms.renameItemStack(new ItemStack(Material.EXP_BOTTLE), "§a§lMuxEXP", "§7Gespeicherte EXP: §a" + expToSave);
            pi.addItem(bottle);
            p.updateInventory();
            ms.showItemBar(p, "§fXP Flasche mit §a" + expToSave + " EXP §ferstellt.");
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
            return true;
        } else if (scmd.equalsIgnoreCase("near")) {
            if (extras.hasntCommand(p.getUniqueId(), "near")) {
                p.performCommand("extracmd");
                return true;
            }
            if (ms.checkGeneralCooldown(p.getName(), "near", 3000L, true)) return true;
            final Location location = p.getLocation();
            final int radius = 80;
            final String output = location.getWorld().getPlayers()
                    .stream()
                    .filter(player -> player != p)
                    .filter(p::canSee)
                    .map(player -> new AbstractMap.SimpleEntry<>(player, player.getLocation().distance(location)))
                    .filter(entry -> entry.getValue() <= radius)
                    .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                    .limit(4)
                    .map(entry -> "§e" + entry.getKey().getName() + " §c(" + entry.getValue().intValue() + " Blöcke)")
                    .collect(Collectors.joining("§7, "));
            if (output.isEmpty()) {
                ms.showItemBar(p, "§cEs befinden sich keine Spieler in deiner Nähe.");
                return true;
            }
            p.sendMessage("§7Spieler in deiner Nähe: " + output);
            return true;
        } else if (scmd.equalsIgnoreCase("xray")) {
            if (extras.hasntCommand(p.getUniqueId(), "xray")) {
                p.performCommand("extracmd");
                return true;
            }
            if (ms.checkGeneralCooldown(p.getName(), "xray", 3000L, true)) return true;
            else if (ms.inBattle(p.getName(), p.getLocation())) {
                ms.showItemBar(p, ms.hnotinfight);
                return true;
            }
            final Location l = p.getLocation();
            final List<Material> mats = Arrays.asList(Material.IRON_ORE, Material.DIAMOND_ORE, Material.GOLD_ORE, Material.REDSTONE_ORE,
                    Material.COAL_ORE, Material.EMERALD_ORE);
            final int radius = 3;
            boolean found = false;
            for (double x = -radius; x < radius; x++) {
                for (double z = -radius; z < radius; z++) {
                    final Location position = new Location(l.getWorld(), l.getBlockX() + x, l.getBlockY(), l.getBlockZ() + z);
                    if (displayOres(position, mats)) found = true;
                }
            }
            if (found) ms.showItemBar(p, "§aDiese Erze befinden sich unter dir.");
            else ms.showItemBar(p, "§cEs befinden sich keine Erze unter dir.");
            return true;
        } else if (scmd.equalsIgnoreCase("etable")) {
            if (extras.hasntCommand(p.getUniqueId(), "enchant")) {
                p.performCommand("extracmd");
                return true;
            }
            p.closeInventory();
            final List<MuxLocation> locs = enchantlocs.getOrDefault(p, new ArrayList<>());
            final MuxLocation l = new MuxLocation(p.getLocation());
            if (locs.contains(l)) {
                ms.showItemBar(p, "§cBewege dich, um den Befehl wieder zu verwenden.");
                return true;
            }
            locs.add(l);
            enchantlocs.put(p, locs);
            final EntityPlayer ep = ((CraftPlayer) p).getHandle();
            final ContainerEnchantTable container = new ContainerEnchantTable(ep.inventory, ((CraftWorld) ms.getWarpsWorld()).getHandle(), new BlockPosition(0, 13, 0));
            container.checkReachable = false;
            container.enchantSlots.setItem(1, new net.minecraft.server.v1_8_R3.ItemStack(net.minecraft.server.v1_8_R3.Item.getById(351), 64, (byte) 4));
            final int c = ep.nextContainerCounter();
            ep.playerConnection.sendPacket(new PacketPlayOutOpenWindow(c, "minecraft:enchanting_table", new ChatMessage("Enchant"), 0));
            ep.activeContainer = container;
            ep.activeContainer.windowId = c;
            ep.activeContainer.addSlotListener(ep);
            return true;
        }
        return false;
    }

    public ItemStack getSmeltedItemStack(final ItemStack item) {
        switch (item.getType()) {
            case SAND:
                return new ItemStack(Material.GLASS, item.getAmount());
            case COBBLESTONE:
                return new ItemStack(Material.STONE, item.getAmount());
            case CLAY_BALL:
                return new ItemStack(Material.CLAY_BRICK, item.getAmount());
            case CLAY:
                return new ItemStack(Material.HARD_CLAY, item.getAmount());
            case NETHERRACK:
                return new ItemStack(Material.NETHER_BRICK_ITEM, item.getAmount());
            case LOG:
            case LOG_2:
                return new ItemStack(Material.COAL, item.getAmount(), (short) 1);
            case CACTUS:
                return new ItemStack(Material.INK_SACK, item.getAmount(), (short) 2);
            case IRON_ORE:
                return new ItemStack(Material.IRON_INGOT, item.getAmount());
            case GOLD_ORE:
                return new ItemStack(Material.GOLD_INGOT, item.getAmount());
            case DIAMOND_ORE:
                return new ItemStack(Material.DIAMOND, item.getAmount());
            case LAPIS_ORE:
                return new ItemStack(Material.INK_SACK, item.getAmount(), (short) 4);
            case REDSTONE_ORE:
                return new ItemStack(Material.REDSTONE, item.getAmount());
            case COAL_ORE:
                return new ItemStack(Material.COAL, item.getAmount());
            case EMERALD_ORE:
                return new ItemStack(Material.EMERALD, item.getAmount());
            case QUARTZ_ORE:
                return new ItemStack(Material.QUARTZ, item.getAmount());
            case SMOOTH_BRICK:
                return new ItemStack(Material.SMOOTH_BRICK, item.getAmount(), (short) 2);
            default:
                return null;
        }
    }

    public boolean canSmeltItem(final Material material) {
        switch (material) {
            case SAND:
            case COBBLESTONE:
            case CLAY_BALL:
            case CLAY:
            case NETHERRACK:
            case LOG:
            case LOG_2:
            case CACTUS:
            case IRON_ORE:
            case GOLD_ORE:
            case DIAMOND_ORE:
            case LAPIS_ORE:
            case REDSTONE_ORE:
            case COAL_ORE:
            case EMERALD_ORE:
            case QUARTZ_ORE:
            case SMOOTH_BRICK:
                return true;
            default:
                return false;
        }
    }

    public ItemStack getCookedItemStack(final ItemStack item) {
        switch (item.getType()) {
            case POTATO_ITEM:
                return new ItemStack(Material.BAKED_POTATO, item.getAmount());
            case PORK:
                return new ItemStack(Material.GRILLED_PORK, item.getAmount());
            case RAW_BEEF:
                return new ItemStack(Material.COOKED_BEEF, item.getAmount());
            case RAW_CHICKEN:
                return new ItemStack(Material.COOKED_CHICKEN, item.getAmount());
            case RAW_FISH:
                if (item.getDurability() > 1) {
                    return null;
                }
                return new ItemStack(Material.COOKED_FISH, item.getAmount(), item.getDurability());
            case MUTTON:
                return new ItemStack(Material.COOKED_MUTTON, item.getAmount());
            case RABBIT:
                return new ItemStack(Material.COOKED_RABBIT, item.getAmount());
            default:
                return null;
        }
    }

    public boolean isCookable(final ItemStack item) {
        switch (item.getType()) {
            case POTATO_ITEM:
            case RABBIT:
            case MUTTON:
            case RAW_CHICKEN:
            case PORK:
            case RAW_BEEF:
                return true;
            case RAW_FISH:
                return item.getDurability() <= 1;
            default:
                return false;
        }
    }

    private boolean displayOres(final Location start, final List<Material> mats) {
        double step = 0.4D;
        boolean found = false;
        final List<ArmorStand> list = new ArrayList<>();
        for (int i = 0; i < start.getBlockY(); i++) {
            final Material m = new Location(start.getWorld(), start.getBlockX(), i, start.getBlockZ()).getBlock().getType();
            if (mats.contains(m)) {
                final ArmorStand a = (ArmorStand) start.getWorld().spawnEntity(start.clone().add(1D, step, 0D), EntityType.ARMOR_STAND);
                a.setRemoveWhenFarAway(true);
                a.setGravity(false);
                a.setVisible(false);
                a.setItemInHand(new ItemStack(m));
                step += 0.4D;
                found = true;
                list.add(a);
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final ArmorStand a : list) {
                    if (a != null) a.remove();
                }
                list.clear();
            }
        }.runTaskLater(ms, 100L);
        return found;
    }


    public int levelToExp(int level) {
       if (level <= 15) {
            return level * level + 6 * level;
        } else {
            return level <= 30 ? (int)(2.5 * (double)level * (double)level - 40.5 * (double)level + 360.0) : (int)(4.5 * (double)level * (double)level - 162.5 * (double)level + 2220.0);
        }
    }

    public int deltaLevelToExp(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else {
            return level <= 30 ? 5 * level - 38 : 9 * level - 158;
        }
    }

    public int getPlayerExperience(Player player) {
        return (int)(levelToExp(player.getLevel()) + Math.floor((deltaLevelToExp(player.getLevel()) * player.getExp())));
    }
    public void setTotalExperienceNew(Player player, int xp) {
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setTotalExperience(0);
        if (xp >= 1) {
            player.giveExp(xp);
            int xpForLevel = levelToExp(player.getLevel());
            int delta = deltaLevelToExp(player.getLevel());
            player.setExp((float)(xp - xpForLevel) / Float.valueOf((float)delta));
        }
    }
}