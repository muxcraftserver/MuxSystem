package me.muxteam.events;

import com.boydti.fawe.object.number.MutableLong;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.muxteam.basic.NMSReflection;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.shop.MuxMining;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuxTournament extends Event implements Listener {
    private final Map<Player, Duel> duels = new HashMap<>();
    private final Location loc = new Location(ms.getWarpsWorld(), 563.5D, 41D, -571.5, 0F, 0F),
            locA = new Location(ms.getWarpsWorld(), 563.5D, 41D, -522.5, 180F, 0F), locB = new Location(ms.getWarpsWorld(), 563.5D, 41D, -620.5, 360F, 0F);
    private long roundEndTime;
    private int currentState = 0; //  0 = Warmup, 1 = Ingame, 2 = Ingame First Death, 3 = No more time, 4 = Ending match
    private int round = 0, time = 5;
    public short kit = -1;
    private boolean reset = true;
    private final MuxScoreboard.ScoreboardType sb;

    public MuxTournament(final MuxEvents e) {
        super(e);
        canSpectate = false;
        item = new ItemStack(Material.STONE_SWORD);
        name = "MuxTurnier";
        pvp = true;
        canMoveItems = true;
        maxPlayers = 500;
        maxSpectatorDistance = 55D;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lTurnier");
        sb.setLook((p, board) -> {
            if (players.contains(p)) {
                int lowestpcnt = 100;
                short armorcount = 0;
                for (final ItemStack armor : p.getInventory().getArmorContents()) {
                    if (armor != null && armor.getType() != Material.AIR && ms.isArmor(armor.getType())) {
                        armorcount++;
                        int durability = 100 - (int) (((double) armor.getDurability() / (double) armor.getType().getMaxDurability()) * 100D);
                        if (durability < lowestpcnt) lowestpcnt = durability;
                    }
                }
                sb.setLine(board, "§a");
                sb.setSection(board, "§3§lRüstung     ", (armorcount == 0 ? "Keine" : armorcount < 4 ? "Unvollständig" : "Zustand: " + (lowestpcnt < 15 ? "§c" : "") + lowestpcnt + "%"), false);
                sb.setLine(board, "§b");
                final Player opponent = getOpponent(p);
                sb.setSection(board, "§4§lGegner", opponent == null ? "Kein Gegner" : "§f" + opponent.getName(), false);
            }
            sb.setLine(board, "§c");
            sb.setSection(board, "§c§lVerbleibend", players.size() + " Spieler", false);
            sb.setLine(board, "§4");
            sb.setSection(board, "§2§lFortschritt", "§fRunde " + (round == 0 ? 1 : round), false);
        });
        spectatorSB = sb;
    }

    private void win(final Player winner) {
        ms.sendTitle(winner, "§d§lMuxTurnier", 10, 80, 10);
        ms.sendSubTitle(winner, "§fDu hast das Event §agewonnen§f!", 10, 80, 10);
        ms.broadcastMessage("§d§lMuxTurnier>§7 Der Spieler §d" + winner.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
        resetPlayer(winner);
        if (startedByPlayer == false) {
            final MutableLong coins = new MutableLong();
            coins.set(giveReward(winner, 2000, MuxMining.OreTypes.DIAMOND, false));
            ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (winner.isOnline() == false) return;
                    ms.sendScoreboard(winner);
                    ms.sendSubTitle(winner, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(coins.get()) + " MuxCoins", 0, 60, 10);
                }
            }, 60L);
            ms.broadcastMessage("§d§lMuxTurnier>§7 Der Gewinn der Woche: §e" + ms.getNumberFormat(coins.get()) + " MuxCoins", null, MuxSystem.Priority.LOW);
        }
        stopEvent(false);
    }

    private void handleDeath(final Player player, final Player killer) {
        sendEventMessage("§7Der Spieler §d" + player.getName() + " §7ist §causgeschieden§7.");
        ms.showItemBar(player, "§cDu bist aus dem Event ausgeschieden.");
        ms.sendTitle(player, "§d§lMuxTurnier", 10, 80, 10);
        ms.sendSubTitle(player, "§fDein Platz: §d#" + (players.size()), 10, 80, 10);
        duels.get(killer).finished = true;
        if (noRunningDuels()) {
            if (players.size() == 1) {
                win(killer);
                return;
            }
            currentState = 4;
            reset = true;
        } else if (currentState == 1) {
            currentState = 2;
            sendEventMessage("§d§lMuxTurnier>§c §lAchtung!§7 Die Runde läuft noch §c5 Minuten.");
            roundEndTime = System.currentTimeMillis() + 30_0000L;
        }
        killer.getInventory().clear();
        killer.getInventory().setArmorContents(null);
        killer.getActivePotionEffects().forEach(potionEffect -> killer.removePotionEffect(potionEffect.getType()));
        players.forEach(player1 -> ms.sendScoreboard(player1, sb));
    }

    private Player getOpponent(final Player p) {
        final Duel duel = duels.get(p);
        if (duel == null) return null;
        return duel.playerA == p ? duel.playerB : duel.playerA;
    }

    private boolean noRunningDuels() {
        return duels.values().stream().filter(duel -> duel.finished == false).toArray().length == 0;
    }

    private boolean hasMovedSignificant(final Location pos1, final Location pos2, final double max) {
        if (max > 1 && pos1.getBlockX() == pos2.getBlockX() && pos1.getBlockZ() == pos2.getBlockZ()) return false;
        return pos1.distanceSquared(pos2) > max * max;
    }

    private void giveKitItems(final Player pl) {
        pl.setHealth(20D);
        pl.setFoodLevel(20);
        pl.getInventory().clear();
        if (kit == 0) {
            pl.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            pl.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            pl.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            pl.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            for (final ItemStack is : pl.getInventory().getArmorContents()) {
                is.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                is.addEnchantment(Enchantment.DURABILITY, 3);
            }
            final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            sword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
            sword.addEnchantment(Enchantment.DURABILITY, 3);
            sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            final ItemStack healPot = new ItemStack(Material.POTION, 64, (short) 16421), regenerationPot = new ItemStack(Material.POTION, 64, (short) 16417), speedPot = new ItemStack(Material.POTION, 64, (short) 16418), strengthPot = new ItemStack(Material.POTION, 64, (short) 16425), fireResiPot = new ItemStack(Material.POTION, 64, (short) 16451);
            pl.getInventory().setItem(0, sword);
            pl.getInventory().setItem(1, healPot.clone());
            pl.getInventory().setItem(2, healPot.clone());
            pl.getInventory().setItem(4, fireResiPot);
            pl.getInventory().setItem(5, strengthPot);
            pl.getInventory().setItem(6, regenerationPot);
            pl.getInventory().setItem(7, speedPot);
        } else if (kit == 1) {
            final ItemStack iH = new ItemStack(Material.LEATHER_HELMET), iC = new ItemStack(Material.LEATHER_CHESTPLATE), iL = new ItemStack(Material.LEATHER_LEGGINGS), iB = new ItemStack(Material.LEATHER_BOOTS),
                    iS = new ItemStack(Material.STONE_SWORD), iBow = new ItemStack(Material.BOW);
            iH.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
            iH.addEnchantment(Enchantment.OXYGEN, 3);
            iC.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            iL.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            iB.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            iB.addEnchantment(Enchantment.PROTECTION_FALL, 1);
            iS.addEnchantment(Enchantment.DAMAGE_UNDEAD, 2);
            iBow.addEnchantment(Enchantment.ARROW_FIRE, 1);
            iBow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
            iBow.addEnchantment(Enchantment.ARROW_KNOCKBACK, 1);
            pl.getInventory().setHelmet(iH);
            pl.getInventory().setChestplate(iC);
            pl.getInventory().setLeggings(iL);
            pl.getInventory().setBoots(iB);
            pl.getInventory().setItem(0, iS);
        } else {
            final ItemStack[] armor = new ItemStack[4];
            armor[3] = new ItemStack(Material.IRON_HELMET);
            armor[2] = new ItemStack(Material.IRON_CHESTPLATE);
            armor[1] = new ItemStack(Material.IRON_LEGGINGS);
            armor[0] = new ItemStack(Material.IRON_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
            }
            final ItemStack ironSword = new ItemStack(Material.DIAMOND_SWORD);
            ironSword.addEnchantment(Enchantment.DAMAGE_ALL, 2);
            ironSword.addEnchantment(Enchantment.DURABILITY, 1);
            pl.getInventory().setItem(0, ironSword);
            final ItemStack soup = new ItemStack(Material.MUSHROOM_SOUP);
            for (int i = 1; i < 8; i++) {
                pl.getInventory().setItem(i, soup.clone());
            }
            pl.getInventory().setArmorContents(armor);
        }
    }

    @Override
    public void start() {
        if (kit == -1) kit = (short) 0;

        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxTurnier §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public void stop() {
        kit = -1;
        round = 0;
        currentState = 0;
        canSpectate = false;
        reset = true;
        duels.clear();
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, loc.clone().add(0, 2D, 0));
        return true;
    }

    @Override
    public void quit(final Player p) {
        if (duels.containsKey(p)) {
            final Duel duel = duels.get(p);
            if (duel.finished == false) {
                handleDeath(p, getOpponent(p));
            }
        }
        resetPlayer(p);
        unblockPotionPackets(p);
        players.forEach(player -> {
            player.showPlayer(p);
            p.showPlayer(player);
        });
    }

    @Override
    public long getUpdateTime() {
        return 20L;
    }

    @Override
    public void update() {
        if (canjoin) return;
        else if (reset) {
            time = currentState == 0 || currentState == 4 ? 5 : 0;
            reset = false;
            return;
        }
        if (currentState == 0) {
            if (time == 5) {
                final List<Player> tempPlayers = new ArrayList<>(players);
                if (tempPlayers.isEmpty()) {
                    stopEvent(true);
                    return;
                }
                if (tempPlayers.size() == 1) {
                    win(tempPlayers.get(0));
                    return;
                }
                do {
                    final Player playerA = tempPlayers.remove(r.nextInt(tempPlayers.size())),
                            playerB = tempPlayers.isEmpty() ? null : tempPlayers.remove(r.nextInt(tempPlayers.size()));
                    final Duel duel = new Duel(playerA, playerB);
                    ms.forcePlayer(playerA, locA);
                    giveKitItems(playerA);
                    duels.put(playerA, duel);
                    blockPotionPackets(playerA, duel);
                    if (playerB == null) {
                        duel.finished = true;
                        break;
                    }
                    duels.put(playerB, duel);
                    ms.forcePlayer(playerB, locB);
                    giveKitItems(playerB);
                    blockPotionPackets(playerB, duel);
                } while (tempPlayers.isEmpty() == false);
                players.forEach(player1 -> {
                    final Duel duel = duels.get(player1);
                    players.forEach(player2 -> {
                        if (duel.playerA != player2 && duel.playerB != player2) {
                            player1.hidePlayer(player2);
                        } else {
                            player1.showPlayer(player2);
                        }
                    });
                });
                round++;
                players.forEach(player -> ms.sendScoreboard(player, sb));
                if (canSpectate == false && players.size() <= 6 && players.isEmpty() == false) {
                    canSpectate = true;
                    for (final Player o : ms.getServer().getOnlinePlayers().stream().filter(player -> players.contains(player) == false).collect(Collectors.toSet())) {
                        ms.chatClickHoverRun(o, "§d§lMuxTurnier>§7 Klicke §dhier§7, um den letzten " + players.size() + " Spielern zuzusehen.", "§d§oKlicke zum Zuschauen", "/event join");
                    }
                }
            }
            time--;
            if (time == 0) {
                players.forEach(player -> {
                    final Player opponent = getOpponent(player);
                    if (opponent == null) {
                        ms.sendNormalTitle(player, "§aDu hast in dieser Runde keinen Gegner.", 0, 24, 0);
                        player.sendMessage("§d§lMuxTurnier>§7 Du hast in dieser Runde keinen Gegner und bist somit §aautomatisch eine Runde §aweiter§7.");
                        player.playSound(player.getLocation(), Sound.NOTE_BASS_DRUM, 1F, 1F);
                        return;
                    }
                    ms.sendNormalTitle(player,  "§fDer Kampf beginnt §6jetzt§f, viel Glück!", 0, 24, 0);
                    player.playSound(player.getLocation(), Sound.WITHER_IDLE, 1F, 1F);
                });
                currentState = 1;
                roundEndTime = System.currentTimeMillis() + 60_0000L;
                players.forEach(player -> ms.sendScoreboard(player, sb));
                reset = true;
            } else if (time <= 3) {
                players.stream().filter(player -> getOpponent(player) != null).forEach(player -> {
                    ms.sendNormalTitle(player, "§fDer Kampf beginnt in §6" + time + " Sekunde" + (time > 1 ? "n" : "") + "§f...", 0, 22, 10);
                    player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1F, 0F);
                });
            }
        } else if (currentState == 1 || currentState == 2) {
            time++;
            if (roundEndTime <= System.currentTimeMillis()) {
                currentState = 3;
                sendEventMessage("§d§lMuxTurnier>§c §lAchtung!§7 Wer in 10 Sekunden am nächsten in der Mitte ist hat das Duell gewonnen.");
                players.forEach(player -> player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1, 1));
                reset = true;
            }
            players.forEach(player -> ms.sendScoreboard(player, sb));
        } else if (currentState == 3) {
            time++;
            if (time == 10) {
                final List<Duel> activeduels = new ArrayList<>();
                for (final Duel d : duels.values()) {
                    if (activeduels.contains(d)) continue;
                    if (d.finished == false) activeduels.add(d);
                }
                activeduels.forEach(duel -> {
                    if (duel.playerA.getLocation().distance(loc) < duel.playerB.getLocation().distance(loc)) {
                        duel.playerA.setHealth(20D);
                        quit(duel.playerB);
                        duel.playerA.getActivePotionEffects().forEach(potionEffect -> duel.playerA.removePotionEffect(potionEffect.getType()));
                    } else {
                        duel.playerB.setHealth(20D);
                        quit(duel.playerA);
                        duel.playerB.getActivePotionEffects().forEach(potionEffect -> duel.playerB.removePotionEffect(potionEffect.getType()));
                    }
                    duel.finished = true;
                });
                players.forEach(player -> {
                    player.setHealth(20D);
                    player.getInventory().clear();
                    player.getInventory().setArmorContents(null);
                });
                currentState = 4;
                players.forEach(player -> ms.sendScoreboard(player, sb));
                reset = true;
            } else if (time >= 5) {
                sendEventBar("§fNoch §c" + (10 - time) + " Sekunden§f.", Sound.WOOD_CLICK);
            }
        } else {
            time--;
            if (time == 0) {
                currentState = 0;
                reset = true;
                players.forEach(player -> ms.sendScoreboard(player, sb));
            }
        }
    }

    @Override
    public Location getSpectateLocation() {
        return loc;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Besiege in jeder Runde deinen Gegner",
                "§7in einem 1 gegen 1 Duell, um schließlich",
                "§7das gesamte Turnier zu gewinnen.",
                "",
                "§7Überlebende: §d" + players.size()
        };
    }

    @Override
    public String[] getAdminInformation() {
        if (kit == -1) return new String[]{};
        else return new String[]{
                "§7Kit: §d" + (kit == 0 ? "Potion" : kit == 1 ? "Schnelles" : kit == 2 ? "Soup" : "Potion")
        };
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent e) {
        final Player p = e.getEntity();
        if (players.contains(p) == false) return;
        final Duel duel = duels.get(p);
        duel.finished = true;

        e.setDroppedExp(0);
        e.getDrops().clear();
        new BukkitRunnable() {
            @Override
            public void run() {
                quit(p);
            }
        }.runTaskLater(ms, 5L);
        handleDeath(p, getOpponent(p));
    }

    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player == false || players.contains((Player) e.getEntity()) == false) return;
        if (currentState == 0 || currentState == 4 || duels.get((Player) e.getEntity()).finished) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent e) {
        final Player player = e.getPlayer();
        if (players.contains(player) == false || duels.containsKey(player) == false) return;
        final Location from = e.getFrom(), to = e.getTo();
        final Duel duel = duels.get(player);
        if (currentState == 0 && hasMovedSignificant(from, to, 0.1)) {
            player.teleport(duel.playerA == player ? locA : locB);
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR ||
                e.getMaterial() != Material.POTION || players.contains(e.getPlayer()) == false) return;
        thrower = e.getPlayer();
    }

    @EventHandler
    public void onProjectile(final ProjectileLaunchEvent e) {
        if (e.getEntity() == null || e.getEntityType() != EntityType.SPLASH_POTION || e.getEntity().getShooter() instanceof Player == false) return;
        final Player p = (Player) e.getEntity().getShooter();
        if (players.contains(p) == false) return;
        final Duel duel = duels.get(p);
        if (duel != null) potions.put(e.getEntity().getEntityId(), duel);
    }

    @EventHandler
    public void onSplash(final PotionSplashEvent e) {
        if (e.getPotion().getShooter() instanceof Player == false) return;
        final int id = e.getEntity().getEntityId();
        final Player p = (Player) e.getPotion().getShooter();
        final Duel duel = potions.remove(id);
        if (p != null && duel != null) {
            for (final LivingEntity le : e.getAffectedEntities()) {
                if (le instanceof Player && (duel.playerA != le && duel.playerB != null && duel.playerB != le))
                    e.setIntensity(le, 0D);
            }
            lastpotion = p;
        }
    }

    class Duel {
        private final Player playerA, playerB;
        private boolean finished = false;

        public Duel(final Player playerA, final Player playerB) {
            this.playerA = playerA;
            this.playerB = playerB;
        }
    }

    private Player lastpotion, thrower;
    private final HashMap<UUID, PacketBlocker> blocker = new HashMap<>();
    private final Map<Integer, Duel> potions = new HashMap<>();

    private void blockPotionPackets(final Player p, final Duel duel) {
        blocker.put(p.getUniqueId(), new PacketBlocker(p, duel));
    }

    private void unblockPotionPackets(final Player p) {
        if (blocker.containsKey(p.getUniqueId()))
            blocker.remove(p.getUniqueId()).unblock();
    }

    class PacketBlocker {
        private final Duel duel;
        private final Player player;
        public Channel channel;

        public PacketBlocker(final Player player, final Duel duel) {
            this.player = player;
            this.duel = duel;
            this.block();
        }

        public void block() {
            final CraftPlayer craftPlayer = (CraftPlayer) this.player;
            this.channel = craftPlayer.getHandle().playerConnection.networkManager.channel;
            if (channel.pipeline().get("EdILeCOpDeg") == null)
                channel.pipeline().addBefore("packet_handler", "EdILeCOpDeg", new ChannelDuplexHandler() {
                    @Override
                    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
                        if (player == null || msg == null) return;
                        if (msg instanceof Packet) {
                            if (msg instanceof PacketPlayOutSpawnEntity) {
                                final PacketPlayOutSpawnEntity spawn = (PacketPlayOutSpawnEntity) msg;
                                final int type = (int) NMSReflection.getObject(PacketPlayOutSpawnEntity.class, "j", spawn),
                                        entityid = (int) NMSReflection.getObject(PacketPlayOutSpawnEntity.class, "a", spawn);
                                if (type == 73 && potions.get(entityid) != duel) {
                                    return;
                                }
                            } else if (msg instanceof PacketPlayOutWorldEvent) {
                                final PacketPlayOutWorldEvent world = (PacketPlayOutWorldEvent) msg;
                                final int type = (int) NMSReflection.getObject(PacketPlayOutWorldEvent.class, "a", world);
                                if (type == 2002 && lastpotion != player && getOpponent(player) != lastpotion) {
                                    return;
                                }
                            } else if (msg instanceof PacketPlayOutNamedSoundEffect) {
                                final PacketPlayOutNamedSoundEffect sound = (PacketPlayOutNamedSoundEffect) msg;
                                final String s = (String) NMSReflection.getObject(PacketPlayOutNamedSoundEffect.class, "a", sound);
                                if (s != null && s.equals("random.bow") && thrower != player && getOpponent(player) != thrower) {
                                    return;
                                }
                            }
                        }
                        super.write(ctx, msg, promise);
                    }
                });
        }

        public void unblock() {
            channel.eventLoop().execute(() -> {
                try {
                    if (channel.pipeline().get("EdILeCOpDeg") != null) channel.pipeline().remove("EdILeCOpDeg");
                } catch (final Exception e) {
                    System.out.println("MuxSystem> Error at removing channel from player. Ignoring...");
                }
            });
        }
    }
}