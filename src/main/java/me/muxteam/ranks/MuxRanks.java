package me.muxteam.ranks;

import me.muxteam.base.PlayerBase;
import me.muxteam.basic.NMSReflection;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.shared.MuxSharedPackets;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuxRanks {
    private MuxSystem ms;
    private final Map<UUID, PermissionsUser> users = new HashMap<>();
    private final Map<String, PermissionAttachment> players = new HashMap<>();
    private final Map<String, PermissionsGroup> groups = new HashMap<>();
    private final Map<String, String> staff = new HashMap<>();
    private final Map<UUID, String> tmpstaff = new HashMap<>();
    private final File datafolder;
    private long whitelistActivateTime;

    public MuxRanks(final MuxSystem ms) {
        datafolder = ms.getDataFolder();
        this.ms = ms;
        this.whitelistActivateTime = ms.getHashYAML().getLong("whitelistactivatetime", -1);
        try {
            final long temp = Long.parseLong(Objects.requireNonNull(ms.getDB().getSharedData("WhitelistActivationTime")));
            if (temp > this.whitelistActivateTime && this.whitelistActivateTime != -1)
                this.whitelistActivateTime = temp;
        } catch (Exception ignored) { }
        NMSReflection.removeFinal(NMSReflection.getField(PermissionAttachmentInfo.class, "value"));
    }

    public void close() {
        removeActivePermissions();
        ms.getHashYAML().set("whitelistactivatetime", this.whitelistActivateTime);
        this.ms = null; // Stop memory leak
    }

    public boolean load() {
        if (loadGroups() == false || loadUsers() == false) {
            System.err.println("MuxSystem> The permissions failed to load.");
            return false;
        }
        return true;
    }

    public void reload() {
        groups.clear();
        loadGroups();
    }

    public String getPrimaryGroup(final UUID uuid) {
        final PermissionsUser permissionsUser = users.get(uuid);
        return permissionsUser == null ? "Default" : permissionsUser.getDisplayedGroup();
    }

    public void removeActivePermissions() {
        final Server sr = ms.getServer();
        for (final Map.Entry<String, PermissionAttachment> entry : players.entrySet()) {
            final String pname = entry.getKey();
            final Player p = sr.getPlayer(pname);
            if (p != null) {
                p.removeAttachment(entry.getValue());
                removeWorldEdit(pname, false); // WORLDEDIT
                removeWorldGuard(pname, false); // WORLDGUARD
            }
        }
    }

    public void removePermissions(final Player p, final String pname) {
        p.removeAttachment(players.get(pname));
        final PermissionsGroup group = getGroupOf(p.getUniqueId());
        final String team = group != null ? group.getTeam() : null;
        for (final Scoreboard sb : ms.getScoreboards()) {
            if (team != null) sb.getTeam(team).removeEntry(pname);
            sb.getTeam("default").removeEntry(pname);
        }
        players.remove(pname);
        removeWorldEdit(pname, false); // WORLDEDIT
        removeWorldGuard(pname, false); // WORLDGUARD
    }

    private boolean loadGroups() {
        final File gf = new File(datafolder, "groups.yml");
        final FileConfiguration config = YamlConfiguration.loadConfiguration(gf);
        if (gf.exists() == false) {
            try {
                config.createSection("groups");
                // DEFAULT GROUP
                config.set("groups.Default", "");
                config.set("groups.Default.prefix", "&7[&9Member&7] ");
                config.set("groups.Default.color", "&f");
                config.set("groups.Default.permissions", "[]");
                config.set("groups.Default.inheritance", "[]");
                config.set("groups.Default.isExtraGroup", false);
                config.save(gf);
            } catch (final Exception e) {
                System.err.println("MuxSystem> File groups.yml could not be created!");
                return false;
            }
        }
        for (final Scoreboard sb : ms.getScoreboards()) {
            if (sb.getTeam("default") == null) {
                sb.registerNewTeam("default").setCanSeeFriendlyInvisibles(false);
            }
        }
        for (final String name : config.getConfigurationSection("groups").getKeys(false)) {
            final PermissionsGroup group = new PermissionsGroup(name);
            final String temp = "groups." + name;
            group.setPrefix(config.getString(temp + ".prefix"));
            group.setColor(config.getString(temp + ".color"));
            group.addPermissions(config.getStringList(temp + ".permissions"));
            group.addPermissions(getInheritance(name));
            group.setTeam(config.getString(temp + ".team"));
            group.setTeamGroup(config.getBoolean(temp + ".isExtraGroup", false));
            groups.put(name, group);
        }
        try {
            config.save(gf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public void checkExpiringRanks() {
        for (PermissionsUser user : this.users.values()) {
            final RankExpireData expireData = user.getExpireData();
            if (expireData != null && ms.getServer().hasWhitelist() == false && expireData.getExpireTime() < System.currentTimeMillis()) {
                final String name = ms.getShared().getNameFromUUID(user.getUUID());
                final String oldRank = expireData.getOldRank();
                user.setExpireData(null);
                changePlayerGroup(user.getUUID(), name, oldRank, ms.getServer().getConsoleSender());
                if (ms.getBase().hasBase(user.getUUID())) {
                    ms.queueNotImportantSyncAction(() -> ms.getBase().getFromUUID(user.getUUID()).generateOutline());
                }
            }
        }
    }

    public void checkWhitelist() {
        final boolean whitelist = ms.getServer().hasWhitelist();
        if (whitelist) {
            if (this.whitelistActivateTime == -1) {
                this.whitelistActivateTime = System.currentTimeMillis();
                ms.getDB().setSharedData("WhitelistActivationTime", String.valueOf(System.currentTimeMillis()));
            }
        } else {
            if (this.whitelistActivateTime != -1) {
                final long timeToAdd = System.currentTimeMillis() - this.whitelistActivateTime;
                this.whitelistActivateTime = -1;
                ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketWhitelistDeactivate());
                final Set<PermissionsUser> toSave = new HashSet<>();
                users.values().forEach(permissionsUser -> {
                    if (permissionsUser.getExpireData() != null && isActivePlayer(permissionsUser.getUUID())) {
                        permissionsUser.getExpireData().setExpireTime(permissionsUser.getExpireData().getExpireTime() + timeToAdd);
                        toSave.add(permissionsUser);
                    }
                });
                ms.getForkJoinPool().execute(() -> toSave.forEach(permissionsUser -> this.ms.getDB().savePermissionsUser(permissionsUser)));
            }
        }
    }

    private boolean isActivePlayer(final UUID uuid) {
        final Player p = ms.getServer().getPlayer(uuid);
        if (p != null) return true;
        return ms.getBase().hasBase(uuid) && (ms.getShop().getSoldItems(uuid).isEmpty() == false || ms.getShop().getItemsOf(uuid).isEmpty() == false);
    }

    private boolean loadUsers() {
        ms.getDB().loadMuxRanks(this);
        final Server sr = ms.getServer();
        for (final PermissionsUser user : this.users.values()) {
            String group = user.getTeamGroup();
            if (group != null) {
                if (group.contains("Developer")) {
                    group = "Developer";
                } else if (group.contains("Builder")) {
                    group = "Builder";
                } else if (group.endsWith("Supporter+")) {
                    group = "Supporter+";
                } else if (group.endsWith("Supporter")) {
                    group = "Supporter";
                } else if (group.endsWith("GameAdmin+")) {
                    group = "GameAdmin+";
                } else if (group.endsWith("GameAdmin")) {
                    group = "GameAdmin";
                } else if (group.equalsIgnoreCase("Owner") == false && group.equalsIgnoreCase("Builder") == false) {
                    group = null;
                }
            }
            if (group != null) {
                final OfflinePlayer pl = sr.getOfflinePlayer(user.getUUID());
                if (pl != null && pl.getName() != null) staff.put(pl.getName(), group);
                else tmpstaff.put(user.getUUID(), group);
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (tmpstaff.size() > 0) System.out.println("MuxSystem> Loading Staff Usernames...");
                for (final Entry<UUID, String> entry : ms.getDB().fetchNames(new HashSet<>(tmpstaff.keySet())).entrySet()) {
                    staff.put(entry.getValue(), tmpstaff.get(entry.getKey()));
                }
            }
        }.runTaskAsynchronously(ms);
        return true;
    }

    public PermissionsUser addUser(final UUID uuid, final String rank, final String teamRank, final RankExpireData expireData) {
        final PermissionsUser user = new PermissionsUser(uuid);
        user.setTeamGroup(teamRank);
        if (expireData != null && (ms.getServer().hasWhitelist() == false && expireData.getExpireTime() < System.currentTimeMillis())) {
            user.setGroup(expireData.getOldRank());
            user.setExpireData(null);
            ms.queueNotImportantSyncAction(() -> {
                if (ms.getBase().hasBase(uuid)) {
                    final PlayerBase base = ms.getBase().getFromUUID(uuid);
                    base.generateOutline();
                }
            });
        } else {
            user.setGroup(rank);
            user.setExpireData(expireData);
        }
        if (this.users.keySet().stream().anyMatch(u -> u.equals(user.getUUID()))) return user;
        this.users.put(user.getUUID(), user);
        return user;
    }

    private List<String> getInheritance(final String group) {
        final File gf = new File(datafolder, "groups.yml");
        final FileConfiguration config = YamlConfiguration.loadConfiguration(gf);
        final List<String> permissions = new ArrayList<>();
        for (final String inheritance : config.getStringList("groups." + group + ".inheritance")) {
            permissions.addAll(config.getStringList("groups." + inheritance + ".permissions"));
            permissions.addAll(getInheritance(inheritance));
        }
        return permissions;
    }

    public PermissionsGroup loadPerms(final Player p) {
        final PermissionAttachment attachment = p.addAttachment(ms);
        final String name = p.getName();
        final UUID uuid = p.getUniqueId();
        PermissionsGroup group;
        PermissionsGroup teamGroup = null;
        if (users.containsKey(uuid)) {
            final PermissionsUser user = users.get(uuid);
            final RankExpireData expireData = user.getExpireData();
            if (expireData != null && ms.getServer().hasWhitelist() == false && expireData.getExpireTime() < System.currentTimeMillis()) {
                final String oldRank = expireData.getOldRank();
                user.setExpireData(null);
                changePlayerGroup(uuid, name, oldRank, ms.getServer().getConsoleSender());
            }
            group = groups.get(user.getGroup());
            teamGroup = user.getTeamGroup() != null ? groups.get(user.getTeamGroup()) : null;
            for (final String permission : user.getPermissions()) {
                attachment.setPermission(permission, true);
            }
        } else {
            group = groups.get("Default");
        }
        for (final String permission : group.getPermissions()) {
            attachment.setPermission(permission, true);
        }
        if (teamGroup != null) {
            for (final String permission : teamGroup.getPermissions()) {
                attachment.setPermission(permission, true);
            }
            if (teamGroup.getTeam() != null) {
                for (final Scoreboard sb : ms.getScoreboards()) {
                    sb.getTeam(teamGroup.getTeam()).addEntry(name);
                }
            }
        } else {
            if (group.getTeam() != null) {
                for (final Scoreboard sb : ms.getScoreboards()) {
                    sb.getTeam(group.getTeam()).addEntry(name);
                }
            } else {
                for (final Scoreboard sb : ms.getScoreboards()) {
                    sb.getTeam("default").addEntry(name);
                }
            }
        }
        attachment.setPermission("worldedit.*", ms.isWETrusted(name));
        attachment.setPermission("worldguard.*", ms.isWGTrusted(name));
        players.put(name, attachment);
        if (p.isOp() && ms.isWETrusted(name) == false) {
            for (PermissionAttachmentInfo effectivePermission : p.getEffectivePermissions()) {
                if ((effectivePermission.getPermission().startsWith("worldedit.")
                        || effectivePermission.getPermission().startsWith("fawe")
                        || effectivePermission.getPermission().startsWith("fast"))
                        && effectivePermission.getPermission().equals("worldedit.*") == false) {
                    NMSReflection.setObject(PermissionAttachmentInfo.class, "value", effectivePermission, false);
                }
            }
        }
        return teamGroup == null ? group : teamGroup;
    }

    public void changePlayerGroup(final UUID uuid, final String name, String group, final CommandSender sender) {
        this.changePlayerGroup(uuid, name, group, sender, false);
    }

    public void changePlayerGroup(final UUID uuid, final String name, String group, final CommandSender sender, boolean packet) {
        final OfflinePlayer ply = ms.getServer().getOfflinePlayer(uuid);

        boolean exists = false;
        for (final String gr : groups.keySet()) {
            if (group.equalsIgnoreCase(gr)) {
                exists = true;
                group = gr;
            }
        }
        if (exists == false && group.equals("teamnull") == false) {
            sender.sendMessage("§cDie Gruppe '" + group + "' existiert nicht!");
            return;
        }
        final boolean removeTeamGroup = group.equals("teamnull");
        final PermissionsUser userdata = users.get(uuid);
        final PermissionsGroup oldgroup = removeTeamGroup ? getGroupOf(uuid) : userdata != null ? getGroup(userdata.group) : null;
        if (removeTeamGroup) {
            group = userdata != null ? userdata.getGroup() : "Default";
        }
        PermissionsGroup pg = getGroup(group);
        final String oldteam = oldgroup != null ? oldgroup.getTeam() : null;
        final String oldGroup = userdata != null ? userdata.getGroup() : "Default";
        final String oldTeamGroup = userdata != null ? userdata.getTeamGroup() : null;
        final RankExpireData expireData = userdata != null ? userdata.getExpireData() : null;
        final PermissionsUser user = new PermissionsUser(uuid);
        user.setExpireData(expireData);
        user.setGroup(pg.isTeamGroup ? oldGroup : group);
        user.setTeamGroup(removeTeamGroup ? null : pg.isTeamGroup ? group : oldTeamGroup);
        if ((sender instanceof Player) || packet) {
            ms.getHistory().addHistory(uuid, packet ? null : ((Player) sender).getUniqueId(), "TEAMACTION", "RANK", (removeTeamGroup || (pg.isTeamGroup && oldTeamGroup != null) ? oldTeamGroup : oldGroup) + " -> " + group);
        }
        users.put(uuid, user);
        if (group.equals("Default") && user.getTeamGroup() == null) {
            ms.getDB().deletePermissionsUser(users.get(uuid));
            ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateRank(uuid, group));
        } else {
            String finalGroup = group;
            new BukkitRunnable() {
                @Override
                public void run() {
                    ms.getDB().savePermissionsUser(users.get(uuid));
                    ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketUpdateRank(uuid, finalGroup));
                }
            }.runTaskAsynchronously(ms);
        }
        if (group.equalsIgnoreCase("Owner")) {
            staff.put(name, group);
        } else if (group.startsWith("Builder")) {
            staff.put(name, "Builder");
        } else if (group.startsWith("Developer")) {
            staff.put(name, "Developer");
        } else if (group.endsWith("GameAdmin+")) {
            staff.put(name, "GameAdmin+");
        } else if (group.endsWith("GameAdmin")) {
            staff.put(name, "GameAdmin");
        } else if (group.endsWith("Supporter+")) {
            staff.put(name, "Supporter+");
        } else if (group.endsWith("Supporter")) {
            staff.put(name, "Supporter");
        } else if (staff.containsKey(name)) {
            ms.getChat().removePrefix(uuid);
            staff.remove(name);
        }
        if (ply.isOnline()) {
            final Player p = ply.getPlayer();
            p.removeAttachment(players.get(name));
            final PermissionAttachment attachment = p.addAttachment(ms);
            final PermissionsGroup pgroup = groups.get(user.getGroup()), tgroup = groups.get(user.getTeamGroup());
            for (final String permission : user.getPermissions()) {
                attachment.setPermission(permission, true);
            }
            if (tgroup != null) {
                for (final String permission : tgroup.getPermissions()) {
                    attachment.setPermission(permission, true);
                }
            }
            for (final String permission : pgroup.getPermissions()) {
                attachment.setPermission(permission, true);
            }
            players.put(name, attachment);
            for (final Scoreboard sb : ms.getScoreboards()) {
                if (oldteam != null) {
                    sb.getTeam(oldteam).removeEntry(name);
                }
                sb.getTeam("default").removeEntry(name);
            }
            if (oldTeamGroup != null && tgroup == null) {
                ms.removeGlobalFly(p.getName());
                p.setFlying(false);
                p.setAllowFlight(false);
            }
            if (tgroup != null) {
                if (tgroup.getPermissions().contains("muxteam") == false) {
                    final String newname = tgroup.getColor().replace("&", "§") + name;
                    if (newname.length() > 16) {
                        p.setPlayerListName(newname.substring(0, 16));
                    } else {
                        p.setPlayerListName(newname);
                    }
                    for (final Scoreboard sb : ms.getScoreboards()) {
                        if (pgroup.getTeam() != null) sb.getTeam(tgroup.getTeam()).addEntry(name);
                        else sb.getTeam("default").addEntry(name);
                    }
                } else if (tgroup.getTeam() != null) {
                    p.setPlayerListName(name);
                    for (final Scoreboard sb : ms.getScoreboards()) {
                        sb.getTeam(tgroup.getTeam()).addEntry(name);
                    }
                }
            } else if (pgroup.getTeam() != null) {
                p.setPlayerListName(name);
                for (final Scoreboard sb : ms.getScoreboards()) {
                    sb.getTeam(pgroup.getTeam()).addEntry(name);
                }
            }

            if (sender instanceof Player || packet) {
                p.sendMessage("§6§lMuxRang>§7 Dein " + (pg.isTeamGroup() ? "Teamrang" : "Spielerrang") + " ist nun " + ChatColor.translateAlternateColorCodes('&', pg.getPrefix()).replaceFirst(".$", "") + "§7.");
                if (sender.getName().equals(name) == false) p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            } else {
                p.sendMessage("  §a" + ms.getLang("perms.yourrankis") + " §6" + ChatColor.translateAlternateColorCodes('&', pgroup.getPrefix()).replaceFirst(".$", "") + "§a.");
            }
            ms.sendScoreboard(p);
            ms.sendTabListColoredNames(p, tgroup != null ? tgroup : pgroup);
        }
        if (getPrimaryGroup(uuid) == null || getGroup(getPrimaryGroup(uuid)).isTeamGroup() == false) {
            ms.getClans().leaveTeamClan(ply, name);
            ms.getHomes().removeIllegalHomes(name, uuid);
        }
        ms.getChat().removePrefix(uuid);
        if (ms.getCustomRank().getTagData(uuid) != null) { // DO NOT USE hasTagData!
            ms.getCustomRank().resetTag(uuid);
        }
        if (ms.getBase().hasBase(uuid)) {
            if ((pg.name.equalsIgnoreCase("gold") || (oldgroup != null && oldgroup.name.equalsIgnoreCase("gold"))) || (pg.name.equalsIgnoreCase("X") || (oldgroup != null && oldgroup.name.equalsIgnoreCase("X"))))
                new BukkitRunnable() { // um save zu sein noch mal extra in einem task
                    @Override
                    public void run() {
                        ms.getBase().getFromUUID(uuid).generateOutline();
                    }
                }.runTask(ms);
        }
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            if (pl.isOp()) {
                if (name.equals(pl.getName()) == false) {
                    pl.sendMessage("§c§lMuxRang>§7 Der Spieler §6" + name + " §7ist nun §6" + ChatColor.translateAlternateColorCodes('&', pg.getPrefix()).replaceFirst(".$", "") + "§7.");
                }
                if (sender.getName().equals("CONSOLE") == false && pl.getName().equals(sender.getName()) == false) {
                    if (name.equals(pl.getName())) {
                        pl.sendMessage("§c§lMuxRang>§7 Der Spieler §c" + sender.getName() + " §7hat ihn geändert.");
                    } else {
                        pl.sendMessage("§c§lMuxRang>§7 Den " + (pg.isTeamGroup() ? "Teamrang" : "Spielerrang") + " hat §c" + sender.getName() + " §7geändert.");
                    }
                }
            }
        }
        removeWorldEdit(name, ms.isWETrusted(name) == false); // WORLDEDIT
        removeWorldGuard(name, ms.isWGTrusted(name) == false); // WORLDGUARD
    }

    public boolean hasGroup(final UUID uuid) {
        return users.containsKey(uuid);
    }

    public PermissionsGroup getGroup(final String group) {
        return groups.get(group);
    }

    public PermissionsUser getUserData(final UUID uuid) {
        return users.get(uuid);
    }

    public int getStaffSize() {
        return staff.size();
    }

    public Set<String> getStaff() {
        return staff.keySet();
    }

    public String getStaffRank(final String name) {
        return staff.get(name);
    }

    public Set<String> getGroupNames() {
        return groups.keySet();
    }

    public void setWhitelistActivateTime(long whitelistActivateTime) {
        this.whitelistActivateTime = whitelistActivateTime;
    }

    public Set<UUID> getMembersOfGroup(final String group) {
        final Set<UUID> members = new HashSet<>();
        users.forEach((uuid, user) -> {
            final String teamg = user.getTeamGroup();
            if (user.getGroup().equals(group) || (teamg != null && teamg.equals(group))) {
                members.add(user.getUUID());
            }
        });
        return members;
    }

    public PermissionsGroup getGroupOf(final UUID uuid) {
        final PermissionsUser pu = getUserData(uuid);
        if (pu == null) return null;
        return groups.get(pu.getDisplayedGroup());
    }

    public final class PermissionsGroup {
        private final String name;
        private String prefix, color, teamname;
        private boolean isTeamGroup = false;
        private final List<String> permissions = new ArrayList<>();

        public PermissionsGroup(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(final String prefix) {
            this.prefix = prefix;
        }

        public String getColor() {
            return color;
        }

        public void setColor(final String color) {
            this.color = color;
        }

        public String getTeam() {
            return teamname;
        }

        public boolean isTeamGroup() {
            return isTeamGroup;
        }

        public void setTeamGroup(boolean teamGroup) {
            isTeamGroup = teamGroup;
        }

        public int getWeight() {
            try {
                return Integer.parseInt(getTeam().split("-")[0]);
            } catch (final Exception ignored) {
                return -1;
            }
        }

        public void setTeam(final String teamname) {
            this.teamname = teamname;
            if (teamname == null) return;
            for (final Scoreboard sb : ms.getScoreboards()) {
                if (sb.getTeam(teamname) == null) {
                    final Team t = sb.registerNewTeam(teamname);
                    if (permissions.contains("muxteam")) t.setPrefix("§4§lTeam§r" + color.replace("&", "§") + " ");
                }
            }
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void addPermissions(final List<String> permissions) {
            this.permissions.addAll(permissions);
        }
    }

    public static final class RankExpireData {
        private final String oldRank;
        private long expireTime;

        public RankExpireData(long expireTime, String oldRank) {
            this.expireTime = expireTime;
            this.oldRank = oldRank;
        }

        public String getOldRank() {
            return oldRank;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(long expireTime) {
            this.expireTime = expireTime;
        }

    }

    public final class PermissionsUser {
        private final UUID uuid;
        private final List<String> permissions = new ArrayList<>();
        private String group, teamgroup;
        private RankExpireData expireData;

        public PermissionsUser(final UUID uuid) {
            this.uuid = uuid;
        }

        public UUID getUUID() {
            return uuid;
        }

        public String getGroup() {
            return group;
        }

        public String getTeamGroup() {
            return teamgroup;
        }

        public String getDisplayedGroup() {
            return this.teamgroup == null ? this.group : this.teamgroup;
        }

        public void setGroup(final String group) {
            this.group = group;
        }

        public void setTeamGroup(final String teamgroup) {
            this.teamgroup = teamgroup;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void addPermissions(final List<String> permissions) {
            this.permissions.addAll(permissions);
        }

        public void setExpireData(RankExpireData expireData) {
            this.expireData = expireData;
        }

        public RankExpireData getExpireData() {
            return expireData;
        }
    }

    /*** ADD / REMOVE DYNAMICALLY ***/
    public void setPerms(final String name, final String permission, final boolean value) {
        final PermissionAttachment attachment = players.get(name);
        if (attachment == null) return;
        attachment.setPermission(permission, value);
    }

    /*** WORLDEDIT ***/
    private final Map<String, Long> worldedit = new ConcurrentHashMap<>();

    public Long addWorldEditPerms(final String name) {
        final PermissionAttachment attachment = players.get(name);
        if (attachment == null) return 0L;
        attachment.setPermission("worldedit.*", true);
        attachment.setPermission("worldedit.anyblock", false);
        return worldedit.put(name, System.currentTimeMillis() + 1800000L);
    }

    public boolean hasWorldEdit(final Player p) {
        return worldedit.containsKey(p.getName());
    }

    public void checkWorldEdit() {
        final long current = System.currentTimeMillis();
        worldedit.forEach((name, time) -> {
            if (current > time) {
                removeWorldEdit(name, true);
            }
        });
    }

    public void removeWorldEdit(final String name, final boolean removeperms) {
        if (removeperms) {
            final PermissionAttachment attachment = players.get(name);
            if (attachment == null) return;
            for (PermissionAttachmentInfo effectivePermission : attachment.getPermissible().getEffectivePermissions()) {
                if ((effectivePermission.getPermission().startsWith("worldedit.")
                        || effectivePermission.getPermission().startsWith("fawe")
                        || effectivePermission.getPermission().startsWith("fast"))
                        && effectivePermission.getPermission().equals("worldedit.*") == false) {
                    attachment.setPermission(effectivePermission.getPermission(), false);
                }
            }
          //  attachment.setPermission("worldedit.*", false);
        }
        worldedit.remove(name);
    }

    /*** WORLDGUARD ***/
    private final Map<String, Long> worldguard = new ConcurrentHashMap<>();

    public Long addWorldGuardPerms(final String name) {
        final PermissionAttachment attachment = players.get(name);
        if (attachment == null) return 0L;
        if (ms.isFullTrusted(name)) {
            attachment.setPermission("worldguard.*", true);
        }
        attachment.setPermission("worldguard.region.bypass.*", true);
        return worldguard.put(name, System.currentTimeMillis() + 1800000L);
    }

    public boolean hasWorldGuard(final Player p) {
        return worldguard.containsKey(p.getName());
    }

    public void checkWorldGuard() {
        final long current = System.currentTimeMillis();
        worldguard.forEach((name, time) -> {
            if (current > time) {
                removeWorldGuard(name, true);
            }
        });
    }

    public void removeWorldGuard(final String name, final boolean removeperms) {
        if (removeperms) {
            final PermissionAttachment attachment = players.get(name);
            if (attachment == null) return;
            final Player pl = ms.getServer().getPlayer(name);
            final Boolean buildperm = attachment.getPermissions().get("worldguard.region.bypass.*");
            if (pl != null && buildperm != null && buildperm) {
                ms.showItemBar(pl, "§eDeine Baurechte sind abgelaufen.");
                pl.setGameMode(GameMode.SURVIVAL);
            }
            attachment.setPermission("worldguard.region.bypass.*", false);
        }
        worldguard.remove(name);
    }
}