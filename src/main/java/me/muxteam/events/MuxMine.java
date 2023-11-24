package me.muxteam.events;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import me.muxteam.basic.MuxActions;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.shop.MuxMining;
import net.minecraft.server.v1_8_R3.EnumColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MuxMine extends Event implements Listener {
    private final Location loc = new Location(ms.getWarpsWorld(), 300D, 25D, 200D);
    private final List<Material> mats = new ArrayList<>();
    private final Map<Material, Material> drops = new EnumMap<>(Material.class);
    private final List<Location> locs = new ArrayList<>();
    private final MuxActions.PlayerAction action;
    private final Map<Player, Integer> won = new HashMap<>();
    private final MuxScoreboard.ScoreboardType sb;
    private long lastmined = -1;

    public MuxMine(final MuxEvents e) {
        super(e);
        item = new ItemStack(Material.IRON_PICKAXE);
        name = "MuxMine";
        maxPlayers = 150;
        maxDuration = 600000;
        breakBlocks = true;
        mats.add(Material.STONE);
        mats.add(Material.COBBLESTONE);
        mats.add(Material.IRON_ORE);
        mats.add(Material.GOLD_ORE);
        mats.add(Material.DIAMOND_ORE);
        mats.add(Material.COAL_ORE);
        mats.add(Material.REDSTONE_ORE);
        mats.add(Material.EMERALD_ORE);
        mats.add(Material.GLOWING_REDSTONE_ORE);

        drops.put(Material.STONE, Material.COBBLESTONE);
        drops.put(Material.COBBLESTONE, Material.COBBLESTONE);
        drops.put(Material.IRON_ORE, Material.IRON_INGOT);
        drops.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        drops.put(Material.DIAMOND_ORE, Material.DIAMOND);
        drops.put(Material.COAL_ORE, Material.COAL);
        drops.put(Material.GLOWING_REDSTONE_ORE, Material.REDSTONE);
        drops.put(Material.EMERALD_ORE, Material.EMERALD);

        locs.add(loc.clone().add(0D, 1D, -37D));
        locs.add(loc.clone().add(-37D, 1D, 0D));
        locs.add(loc.clone().add(37D, 1D, 0D));
        locs.add(loc.clone().add(0D, 1D, 37D));
        action = p -> p.performCommand("event action");
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lMine");
        sb.setLook((p, board) -> {
            sb.setLine(board, "");
            sb.setSection(board, "§a§lTeilnehmer", players.size() + " Spieler", false);
            sb.setLine(board, "  ");
            sb.setSection(board, "§e§lGewinn", ms.getNumberFormat(won.getOrDefault(p, 0)), true);
        });
    }

    private Material getRandomMaterial() {
        return mats.get(r.nextInt(mats.size()));
    }

    private Location getRandomSpawn() {
        return locs.get(r.nextInt(locs.size()));
    }

    private void addPickaxe(final Player p) {
        final ItemStack spade = new ItemStack(Material.IRON_PICKAXE, 1, (short) 0);
        spade.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
        final ItemMeta spadem = spade.getItemMeta();
        spadem.setDisplayName("§f§lMux§b§lSpitzhacke");
        spadem.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        spadem.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        spade.setItemMeta(spadem);
        p.getInventory().setItem(0, spade);
        p.getInventory().setHeldItemSlot(0);
    }

    @Override
    public void action(final Player p, final String[] args) {
        if (wait != -1 || players.contains(p) == false) return;
        int coins = 0;
        for (final ItemStack i : p.getInventory().getContents()) {
            if (i == null || i.getType() == Material.IRON_PICKAXE || ms.isMenuItem(i)) continue;
            Material m = i.getType();
            if (m == Material.DIAMOND)
                m = Material.DIAMOND_ORE;
            else if (m == Material.IRON_INGOT)
                m = Material.IRON_ORE;
            else if (m == Material.GOLD_INGOT)
                m = Material.GOLD_ORE;
            else if (m == Material.EMERALD)
                m = Material.EMERALD_ORE;
            int sellprice;
            MuxMining.OreTypes type = null;
            for (final MuxMining.OreTypes t : MuxMining.OreTypes.values()) {
                if (t.getMaterial() == m) {
                    type = t;
                    break;
                }
            }
            if (type != null) {
                sellprice = (int) ((ms.getActiveMuxCoinsSupply() * type.getPercentage() / 100) * i.getAmount());
            } else {
                sellprice = (int) ms.getShop().getCheapestPrice(m.getId() + ":" + i.getDurability());
            }
            if (sellprice < 1)
                sellprice = 1;
            coins += sellprice;
        }
        if (coins == 0) {
            ms.showItemBar(p, "§cDu hast keine Items zum Verkaufen.");
            return;
        }
        if (ms.hasGold(p.getUniqueId()))
            coins *= 2;
        won.put(p, won.getOrDefault(p, 0) + coins);
        ms.sendScoreboard(p);
        ms.showItemBar(p, "§fDu hast die Items erfolgreich §averkauft§f.");
        p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
        p.getInventory().clear();
        ms.getMenu().addMenuItem(p, false);
        addPickaxe(p);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Baue mit der Spitzhacke so viele Erze",
                "§7wie möglich ab. Verkaufe sie dann beim",
                "§7pinken Schaf in der Mitte.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public void generate() {
        final World w = loc.getWorld();
        final EditSession session = ms.getFastBuilder(w.getName());
        for (int x = -40; x < 40; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = -40; z < 40; z++) {
                    if (loc.clone().add(x, y, z).distance(loc) <= 35 - y) {
                        final Location location = loc.clone().add(x, y, z);
                        session.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), new BaseBlock(getRandomMaterial().getId()));
                    }
                }
            }
        }
        for (int x = -40; x < 40; x++) {
            for (int y = 1; y < 10; y++) {
                for (int z = -40; z < 40; z++) {
                    if (x <= 1 && x >= -1) {
                        final Location location = loc.clone().add(x, y, z);
                        session.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), new BaseBlock(BlockID.AIR));
                    }
                    if (z <= 1 && z >= -1) {
                        final Location location = loc.clone().add(x, y, z);
                        session.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), new BaseBlock(BlockID.AIR));
                    }
                }
            }
        }
        session.flushQueue();
    }

    @Override
    public void update() {
        if (lastmined != -1 && lastmined + 30000L < System.currentTimeMillis()) {
            stopEvent(false);
        }
    }
    @Override
    public long getUpdateTime() {
        return 80L;
    }

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) players.forEach(player -> ms.sendScoreboard(player, sb));
    }

    @Override
    public void start() {
        generate();
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxMine §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, true, false);
        spawnEventNPC("§d§lEvent beitreten");
        ms.getNPCS().addSheep(EnumColor.MAGENTA, loc.clone().add(0.5D, 1D, 0.5D), BlockFace.SOUTH, "§d§lItems verkaufen", action);
    }

    @Override
    public void stop() {
        if (won.isEmpty() == false) {
            ms.broadcastMessage("§d§lMuxEvent>§7 Das MuxMine Event ist nun zuende.", null, MuxSystem.Priority.LOW);
        }
        won.clear();
        ms.getNPCS().removeNPC(action, true);
        lastmined = -1;
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, getRandomSpawn());
        if (wait == -1) ms.sendScoreboard(p, sb);
        addPickaxe(p);
        return true;
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
        final Integer coins = won.remove(p);
        if (coins == null) return;
        ms.runLater(new BukkitRunnable() {
            @Override
            public void run() {
                if (coins > 0 && startedByPlayer == false) {
                    addCoins(p, coins);
                    ms.sendTitle(p, "§d§lMuxMine", 10, 60, 10);
                    ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(coins) + " MuxCoins", 0, 60, 10);
                }
            }
        }, 60L);
    }

    @EventHandler
    public void onBreak(final BlockBreakEvent e) {
        if (players.contains(e.getPlayer()) == false) {
            return;
        }
        final Material m = e.getBlock().getType();
        final Player p = e.getPlayer();
        e.setCancelled(true);
        if (mats.contains(m) == false || e.getBlock().getData() > 0) {
            return;
        } else if (wait != -1) {
            ms.showItemBar(p, "§cWarte bis das Event startet.");
            return;
        }
        lastmined = System.currentTimeMillis();
        e.getBlock().setType(Material.AIR);
        p.getInventory().addItem(new ItemStack(drops.get(m)));
    }

    @EventHandler
    public void onPlace(final BlockPlaceEvent e) {
        if (players.contains(e.getPlayer())) {
            e.setCancelled(true);
        }
    }
}