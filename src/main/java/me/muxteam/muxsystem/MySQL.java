package me.muxteam.muxsystem;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.muxteam.base.MuxBase;
import me.muxteam.base.PlayerBase;
import me.muxteam.basic.MuxTimings;
import me.muxteam.casino.MuxCasinoUser;
import me.muxteam.events.SearchEventUser;
import me.muxteam.extras.MuxBoosters;
import me.muxteam.extras.MuxChestUser;
import me.muxteam.extras.MuxChests;
import me.muxteam.extras.MuxExtraUser;
import me.muxteam.extras.MuxMounts;
import me.muxteam.extras.MuxPerks.PerkStore;
import me.muxteam.extras.MuxPets;
import me.muxteam.marketing.MuxAffiliate;
import me.muxteam.marketing.MuxEmails;
import me.muxteam.marketing.MuxGiftCodes;
import me.muxteam.marketing.MuxNewbies;
import me.muxteam.marketing.MuxTips;
import me.muxteam.muxteam.Analytics;
import me.muxteam.muxteam.BanEntry;
import me.muxteam.ranks.MuxCustomRank;
import me.muxteam.ranks.MuxRanks;
import me.muxteam.shop.MuxGemShop;
import me.muxteam.shop.MuxShop;
import me.muxteam.shop.PremiumItem;
import me.muxteam.shop.ShopItem;
import net.minecraft.server.v1_8_R3.Tuple;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MySQL {
    private Connection conn, longconn, clanconn;
    private Connection sharedConn, sharedLongConn;

    private final MuxTimings timings;
    private final MuxSystem ms;

    public MySQL(final MuxSystem ms, final String url, final String user, final String pw, final String sharedUrl, final String sharedUser, final String sharedPw) {
        this.ms = ms;
        this.timings = ms.getTimings();
        try {
            conn = DriverManager.getConnection(url, user, pw);
            longconn = DriverManager.getConnection(url, user, pw);
            clanconn = DriverManager.getConnection(url, user, pw);
            sharedConn = DriverManager.getConnection(sharedUrl, sharedUser, sharedPw);
            sharedLongConn = DriverManager.getConnection(sharedUrl, sharedUser, sharedPw);
            try (final Statement sctbl = sharedConn.createStatement()) {
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxVote (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "firstvote TINYINT(2) DEFAULT 1," +
                        "INDEX(firstvote)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxExtras(" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "player VARCHAR(32)," +
                        "perks VARCHAR(200)," +
                        "extracmds VARCHAR(2000)," +
                        "pets VARCHAR(2000)," +
                        "mounts VARCHAR(200)," +
                        "emojis VARCHAR(200)," +
                        "enderchest TINYINT(1)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxChests (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "chests MEDIUMTEXT," +
                        "daily TINYINT(1)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxEventSearch (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "found TEXT," +
                        "coinsfound TEXT" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxGames (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "training TINYINT UNSIGNED DEFAULT 0" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxAffiliate (" +
                        "recruiter BINARY(16)," +
                        "recruited BINARY(16) UNIQUE," +
                        "status BIGINT," +
                        "timestamp BIGINT," +
                        "payments LONGTEXT," +
                        "INDEX(recruiter)," +
                        "INDEX(recruiter, timestamp)," +
                        "INDEX(timestamp)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxCashback (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "status BIGINT," +
                        "payments LONGTEXT" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxAffiliateCashout (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "player VARCHAR(32)," +
                        "cryptoaddress VARCHAR(100)," +
                        "cryptocurrency VARCHAR(100)," +
                        "currentamount DOUBLE," +
                        "estimatedamount DOUBLE," +
                        "totalcashedout DOUBLE," +
                        "cashoutamount DOUBLE" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxRanks (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "`rank` VARCHAR(100)," +
                        "teamrank VARCHAR(100)," +
                        "rankexpiredata VARCHAR(255)," +
                        "extraperms MEDIUMTEXT" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxTips (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "tips MEDIUMTEXT," +
                        "last BIGINT" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS SharedData (" +
                        "name VARCHAR(100) PRIMARY KEY," +
                        "value VARCHAR(100)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxSupport (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "player VARCHAR(32)," +
                        "supporter BINARY(16)," +
                        "time VARCHAR(10)," +
                        "conversation MEDIUMTEXT," +
                        "reaction BIGINT DEFAULT -1," +
                        "liked BOOL DEFAULT 0," +
                        "INDEX(supporter)," +
                        "INDEX(supporter, id)," +
                        "INDEX(supporter, liked)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxContact (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "name VARCHAR(20)," +
                        "email VARCHAR(100)," +
                        "subject VARCHAR(64)," +
                        "message TEXT," +
                        "time BIGINT(20)," +
                        "ip VARCHAR(25)," +
                        "done TINYINT(1) DEFAULT 0" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxServers (" +
                        "serveruuid BINARY(16) PRIMARY KEY," +
                        "name VARCHAR(100)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxPolls (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "server BINARY(16)," +
                        "question VARCHAR(200)," +
                        "time VARCHAR(10)," +
                        "options TEXT" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxGlobalUser (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "player VARCHAR(32)," +
                        "login BIGINT," +
                        "firstlogin BIGINT," +
                        "ip VARCHAR(25)," +
                        "online BIGINT," +
                        "lastserver BINARY(16)," +
                        "gems INT DEFAULT 0," +
                        "testgems INT DEFAULT 500," +
                        "INDEX(player)," +
                        "INDEX(gems)," +
                        "INDEX(ip, player)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxEmailDomains ( " +
                        "domain VARCHAR(100) PRIMARY KEY" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxEmails (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "player VARCHAR(32)," +
                        "email VARCHAR(100)," +
                        "temp_email VARCHAR(100)," +
                        "verified BOOLEAN DEFAULT FALSE," +
                        "verify_code VARCHAR(8)," +
                        "newsletter BOOLEAN DEFAULT FALSE," +
                        "last_verify_attempt BIGINT," +
                        "current_tries_timestamp BIGINT," +
                        "last_reminder BIGINT," +
                        "current_tries TINYINT," +
                        "INDEX(email)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxEmailList (" +
                        "email VARCHAR(100) PRIMARY KEY," +
                        "player VARCHAR(32)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxHistory (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "uuid BINARY(16)," +
                        "type VARCHAR(100)," +
                        "victim BINARY(16)," +
                        "entry MEDIUMTEXT," +
                        "param VARCHAR(1000)," +
                        "timestamp BIGINT," +
                        "server BINARY(16)," +
                        "INDEX(uuid)," +
                        "INDEX(uuid,type,id)," +
                        "INDEX(uuid,type,timestamp)," +
                        "INDEX(uuid,type,timestamp,server)," +
                        "INDEX(type,id)," +
                        "INDEX(timestamp)," +
                        "INDEX(type,timestamp)," +
                        "INDEX(uuid,type)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxPaySafeCards (" +
                        "uuid BINARY(16)," +
                        "player VARCHAR(32)," +
                        "code VARCHAR(50)," +
                        "value INT," +
                        "UNIQUE KEY (uuid, code)," +
                        "INDEX(uuid)" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxBaseSchematics (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "uuid BINARY(16)," +
                        "timestamp BIGINT," +
                        "server BINARY(16)," +
                        "schematic LONGBLOB" +
                        ")");
                sctbl.execute("CREATE TABLE IF NOT EXISTS MuxCustomRank (" +
                        "uuid BINARY(16) UNIQUE," +
                        "tag VARCHAR(9) CHARACTER SET utf8mb4," +
                        "symbol INT(11)," +
                        "color VARCHAR(16)," +
                        "lastchange BIGINT," +
                        "active BOOLEAN" +
                        ")");
            }
            try (final Statement ctbl = conn.createStatement()) {
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxUser (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "player VARCHAR(32)," +
                        "login BIGINT," +
                        "firstlogin BIGINT," +
                        "online BIGINT," +
                        "locationworld VARCHAR(50)," +
                        "locationx INT," +
                        "locationy INT," +
                        "locationz INT," +
                        "coins BIGINT DEFAULT 500," +
                        "trophies INT," +
                        "profits INT DEFAULT 0," +
                        "kills INT DEFAULT 0," +
                        "deaths INT DEFAULT 0," +
                        "permban TINYINT(1) DEFAULT 0," +
                        "timeban BIGINT DEFAULT -1," +
                        "permmute TINYINT(1) DEFAULT 0," +
                        "timemute BIGINT DEFAULT -1," +
                        "pvpban BIGINT DEFAULT -1," +
                        "mails MEDIUMTEXT," +
                        "news TINYINT(1) DEFAULT 0," +
                        "settings TEXT," +
                        "lastranked BIGINT DEFAULT 0," +
                        "testcoins INT DEFAULT 10000," +
                        "cents DOUBLE DEFAULT 0," +
                        "testcents DOUBLE DEFAULT 0," +
                        "INDEX(player)," +
                        "INDEX(lastranked)," +
                        "INDEX(uuid, lastranked, pvpban)," +
                        "INDEX(login)," +
                        "INDEX(coins)," +
                        "INDEX(login, coins)," +
                        "INDEX(uuid, login)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxCasinoStats (" +
                        "server VARCHAR(100) PRIMARY KEY," +
                        "stats MEDIUMTEXT" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxCasino (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "chips BIGINT DEFAULT 0," +
                        "testchips BIGINT DEFAULT 5000," +
                        "energy TINYINT UNSIGNED DEFAULT 100," +
                        "weeklywins BIGINT DEFAULT 0," +
                        "INDEX(weeklywins)," +
                        "INDEX(chips)," +
                        "INDEX(testchips)," +
                        "INDEX(uuid, chips)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxNewbie (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "timestamp BIGINT," +
                        "data MEDIUMTEXT," +
                        "INDEX(timestamp)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxLiquidity (" +
                        "server VARCHAR(100) PRIMARY KEY," +
                        "gems BIGINT," +
                        "coins BIGINT" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxHistoryBans (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "uuid BINARY(16)," +
                        "player VARCHAR(32)," +
                        "action VARCHAR(50)," +
                        "param TEXT," +
                        "time DATETIME," +
                        "reason VARCHAR(100)," +
                        "causer VARCHAR(32)," +
                        "removecauser VARCHAR(32)," +
                        "removetime DATETIME," +
                        "INDEX(uuid,time)," +
                        "INDEX(removecauser,time)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxHomes (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "homes MEDIUMTEXT" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxClans (" +
                        "clan VARCHAR(7) PRIMARY KEY," +
                        "base VARCHAR(100)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxClanMembers (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "cname VARCHAR(7)," +
                        "uuid BINARY(16)," +
                        "`rank` TINYINT(2)," +
                        "invited TINYINT(1)," +
                        "INDEX(invited,cname)," +
                        "INDEX(`rank`,cname)," +
                        "INDEX(uuid,cname)," +
                        "INDEX(cname)," +
                        "INDEX(uuid,invited,cname)," +
                        "INDEX(cname,invited,`rank`)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxFriends (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "uuid BINARY(16)," +
                        "uuid2 BINARY(16)," +
                        "INDEX(uuid)," +
                        "INDEX(uuid,uuid2)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxEnderChest (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "player VARCHAR(32)," +
                        "enderchest MEDIUMTEXT" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxAnalytics (" +
                        "dayofyear SMALLINT PRIMARY KEY," +
                        "newplayers MEDIUMTEXT," +
                        "joinedplayers MEDIUMTEXT," +
                        "npcs MEDIUMTEXT," +
                        "warps MEDIUMTEXT," +
                        "commands MEDIUMTEXT," +
                        "ackicks MEDIUMTEXT," +
                        "acbans MEDIUMTEXT," +
                        "chestopens INT," +
                        "chestplayers MEDIUMTEXT," +
                        "eventcoins BIGINT," +
                        "playedcasinogames MEDIUMTEXT," +
                        "totalgambledchips BIGINT," +
                        "eventplayers MEDIUMTEXT," +
                        "casinoplayers MEDIUMTEXT," +
                        "playedonevsonegames INT," +
                        "onevsoneplayers MEDIUMTEXT," +
                        "spentgems MEDIUMINT," +
                        "affiliate INT," +
                        "affiliatemoney DECIMAL," +
                        "baseplayers MEDIUMTEXT," +
                        "basegriefed INT," +
                        "gemshopcategories MEDIUMTEXT," +
                        "gemshop MEDIUMTEXT" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxShop (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "item VARCHAR(20)," +
                        "uuid BINARY(16)," +
                        "amount TINYINT UNSIGNED," +
                        "price DOUBLE," +
                        "INDEX(uuid)," +
                        "INDEX(item)," +
                        "INDEX(price)," +
                        "INDEX(item, price)," +
                        "INDEX(item, uuid)," +
                        "INDEX(item, amount)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxShopEarnings (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "uuid BINARY(16)," +
                        "item VARCHAR(20)," +
                        "amount INT," +
                        "earnings DOUBLE," +
                        "UNIQUE KEY(uuid, item)," +
                        "INDEX(uuid, earnings)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxShopPrices (" +
                        "time BIGINT," +
                        "item VARCHAR(20)," +
                        "price DOUBLE," +
                        "volume BIGINT," +
                        "INDEX(time, price, item)," +
                        "INDEX(item, price)," +
                        "INDEX(volume)," +
                        "INDEX(volume,time)," +
                        "PRIMARY KEY(item, time)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxBlocks (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "uuid BINARY(16)," +
                        "x INT," +
                        "y INT," +
                        "z INT," +
                        "world VARCHAR(50)," +
                        "blockid INT," +
                        "INDEX(uuid)" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxMarket (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "item MEDIUMTEXT," +
                        "uuid BINARY(16)," +
                        "price INT," +
                        "expire BIGINT" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxBase (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "size INT(11)," +
                        "trusted MEDIUMTEXT," +
                        "mid VARCHAR(100)," +
                        "spawn VARCHAR(100)," +
                        "minpos VARCHAR(100)," +
                        "maxpos VARCHAR(100)," +
                        "grief BIGINT," +
                        "griefed TINYINT(1) DEFAULT 0," +
                        "blocks LONGTEXT," +
                        "blocksLR LONGTEXT," +
                        "lastplayed BIGINT," +
                        "griefable TINYINT(1) DEFAULT 0" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxEventStats (" +
                        "event VARCHAR(100) PRIMARY KEY," +
                        "coins BIGINT" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxChestsBlacklist (" +
                        "item VARCHAR(20) PRIMARY KEY" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxBoosters (" +
                        "uuid BINARY(16) PRIMARY KEY," +
                        "boosters MEDIUMTEXT" +
                        ")");
                ctbl.execute("CREATE TABLE IF NOT EXISTS MuxGiftCodes (" +
                        "code VARCHAR(100) PRIMARY KEY," +
                        "giftcode MEDIUMTEXT" +
                        ")");
            }
            loadClanCache();
            updateDuplicatedUserNames();
        } catch (final Exception ex) {
            System.err.println("MuxSystem> Error while loading the database.");
            ex.printStackTrace();
        }
    }

    public void kill() {
        try {
            clanconn.close();
            conn.close();
            longconn.close();
            sharedConn.close();
            sharedLongConn.close();
        } catch (final Exception e) {
            System.err.println("MuxSystem> Error while closing the database.");
        }
    }

    public boolean isConnected() {
        return conn != null;
    }

    public boolean isSharedConnected() {
        return sharedConn != null;
    }

    private final Gson gson = new Gson();

    public String getMethodName() {
        final String name = Thread.currentThread().getStackTrace()[2].getMethodName();
        timings.ofAndStart(name);
        return name;
    }

    // []=== SHARED DATA ===[]
    public void setSharedData(final String name, final String value) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO SharedData SET name = ?, value = ? ON DUPLICATE KEY UPDATE value = ?")) {
            st.setString(1, name);
            st.setString(2, value);
            st.setString(3, value);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public String getSharedData(final String name) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT value FROM SharedData WHERE name = ?")) {
            sel.setString(1, name);
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.next())
                    return rs.getString("value");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public void deleteSharedData(final String name) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("DELETE FROM SharedData WHERE name = ?")) {
            st.setString(1, name);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void saveCurrentServerName() {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO MuxServers SET serveruuid = UNHEX(?), name = ? ON DUPLICATE KEY UPDATE name = ?")) {
            st.setString(1, ms.getServerId().toString().replace("-", ""));
            st.setString(2, ms.getServerName());
            st.setString(3, ms.getServerName());
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public Map<UUID, String> getServers() {
        final String method = getMethodName();
        final Map<UUID, String> servers = new HashMap<>();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(serveruuid) as serveruuid, name FROM MuxServers")) {
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    servers.put(UUID.fromString(rs.getString("serveruuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), rs.getString("name"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return servers;
    }

    // []=== STATS ===[]
    private final Map<UUID, Integer> trophycache = new ConcurrentHashMap<>();

    public int getStatsRank(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement rank = conn.prepareStatement("SELECT 1 + (SELECT COUNT(*) FROM MuxUser a WHERE a.trophies > b.trophies AND lastranked > ? AND (pvpban = -1 OR pvpban < ?)) AS statsrank FROM MuxUser b WHERE uuid = UNHEX(?) ORDER BY statsrank LIMIT 1")) {
            rank.setLong(1, System.currentTimeMillis() - 172800000);
            rank.setLong(2, System.currentTimeMillis());
            rank.setString(3, uuid.toString().replace("-", ""));
            try (final ResultSet rs = rank.executeQuery()) {
                rs.last();
                timings.stop(method);
                if (rs.getRow() != 0) {
                    return rs.getInt("statsrank");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return -1;
    }

    public int getKillsRank(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement rank = conn.prepareStatement("SELECT 1 + (SELECT COUNT(*) FROM MuxUser a WHERE a.kills > b.kills AND (pvpban = -1 OR pvpban < ?)) AS killsrank FROM MuxUser b WHERE uuid = UNHEX(?) ORDER BY killsrank LIMIT 1")) {
            rank.setLong(1, System.currentTimeMillis());
            rank.setString(2, uuid.toString().replace("-", ""));
            try (final ResultSet rs = rank.executeQuery()) {
                rs.last();
                timings.stop(method);
                if (rs.getRow() != 0) {
                    return rs.getInt("killsrank");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return -1;
    }

    public List<Object[]> getTopKills(final int top) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT HEX(uuid) AS uuid, kills FROM MuxUser WHERE (pvpban = -1 OR pvpban < ?) ORDER BY kills DESC LIMIT ?")) {
            sel.setLong(1, System.currentTimeMillis());
            sel.setInt(2, top);
            try (final ResultSet rs = sel.executeQuery()) {
                final List<Object[]> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new Object[]{UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), rs.getInt("kills")});
                }
                timings.stop(method);
                return result;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public List<Object[]> getTopTrophies(final int top) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT HEX(uuid) AS uuid, trophies FROM MuxUser WHERE (pvpban = -1 OR pvpban < ?) AND trophies > 250 AND lastranked > ? ORDER BY trophies DESC LIMIT ?")) {
            sel.setLong(1, System.currentTimeMillis());
            sel.setLong(2, System.currentTimeMillis() - 172800000);
            sel.setInt(3, top);
            try (final ResultSet rs = sel.executeQuery()) {
                final List<Object[]> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(new Object[]{UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), rs.getInt("trophies")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public void reduceTrophies(final int mintrophies, final double percentage) {
        final String method = getMethodName();
        try (final Statement stat = conn.createStatement()) {
            stat.execute("UPDATE MuxUser SET trophies = (trophies - ((trophies - " + mintrophies + ") * " + percentage + ")) WHERE trophies > " + mintrophies);
            loadClanCache();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    /*public void updateTrophyCache(final UUID uuid, final int trophies) {
        if (fastclancache.containsKey(uuid)) {
            final Integer trophy = trophycache.getOrDefault(uuid, 0) + trophies;
            final List<UUID> members = getClanMembersOfPlayer(uuid);
            if (members == null) return;
            for (final UUID p : members) {
                trophycache.put(p, trophy);
            }
        }
    }*/

    public void resetStats() {
        final String method = getMethodName();
        try (final Statement stat = conn.createStatement()) {
            stat.execute("UPDATE MuxUser SET trophies = 250, kills = 0, deaths = 0");
            for (final UUID s : fastclancache.keySet()) {
                trophycache.put(s, 0);
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== EMAIL ===[]
    public boolean loadMuxMailUser(final MuxEmails.MuxMailUser user) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("SELECT * FROM MuxEmails WHERE uuid = UNHEX(?)")) {
            st.setString(1, user.getUUID().toString().replace("-", ""));
            try (final ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    user.setNewsletter(rs.getBoolean("newsletter"));
                    user.setVerified(rs.getBoolean("verified"));
                    user.setLastVerifyTime(rs.getLong("last_verify_attempt"));
                    user.setVerifyCode(rs.getString("verify_code"));
                    user.setEmail(rs.getString("email"));
                    user.setCurrentTries(rs.getInt("current_tries"));
                    user.setTempEmail(rs.getString("temp_email"));
                    user.setLastReminder(rs.getLong("last_reminder"));
                    timings.stop(method);
                    return true;
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    public void saveMuxMailUser(final MuxEmails.MuxMailUser user) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO MuxEmails SET uuid = UNHEX(?), newsletter = ?, verified = ?, last_verify_attempt = ?, verify_code = ?, email = ?, temp_email = ?, current_tries = 1, player = ?, last_reminder = ? ON DUPLICATE KEY UPDATE newsletter = ?, verified = ?, last_verify_attempt = ?, verify_code = ?, email = ?, temp_email = ?, current_tries = ?, last_reminder = ?")) {
            st.setString(1, user.getUUID().toString().replace("-", ""));
            st.setBoolean(2, user.isNewsletter());
            st.setBoolean(3, user.isVerified());
            st.setLong(4, user.getLastVerifyTime());
            st.setString(5, user.getVerifyCode());
            st.setString(6, user.getEmail());
            st.setString(7, user.getTempEmail());
            st.setString(8, user.getName());
            st.setLong(9, user.getLastReminder());
            st.setBoolean(10, user.isNewsletter());
            st.setBoolean(11, user.isVerified());
            st.setLong(12, user.getLastVerifyTime());
            st.setString(13, user.getVerifyCode());
            st.setString(14, user.getEmail());
            st.setString(15, user.getTempEmail());
            st.setInt(16, user.getCurrentTries());
            st.setLong(17, user.getLastReminder());
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public boolean emailAlreadyUsed(final String email) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("SELECT verified FROM MuxEmails WHERE email = ? AND verified = 1")) {
            st.setString(1, email);
            try (final ResultSet rs = st.executeQuery()) {
                timings.stop(method);
                if (rs.next()) return true;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    public void addEmailToList(final String email, final String player) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO MuxEmailList SET email = ?, player = ? ON DUPLICATE KEY UPDATE email = email")) {
            st.setString(1, email);
            st.setString(2, player);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void addDefaultDomains(final List<String> domains) {
        final String method = getMethodName();
        final StringBuilder sb = new StringBuilder("INSERT IGNORE INTO MuxEmailDomains (domain) VALUES (?)");
        for (short i = 1; i < domains.size(); i++) {
            sb.append(",(?)");
        }
        try (final PreparedStatement st = sharedConn.prepareStatement(sb.toString())) {
            int count = 0;
            for (final String domain : domains) {
                st.setString(count + 1, domain);
                count++;
            }
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void addEmailDomain(final String domain) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO MuxEmailDomains SET domain = ?")) {
            st.setString(1, domain);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void removeEmailDomain(final String domain) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("DELETE FROM MuxEmailDomains WHERE domain = ?")) {
            st.setString(1, domain);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public Set<String> getValidEmailDomains() {
        final String method = getMethodName();
        final Set<String> result = new HashSet<>();
        try (final PreparedStatement st = sharedConn.prepareStatement("SELECT * FROM MuxEmailDomains")) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next())
                    result.add(rs.getString("domain"));
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return result;
    }

    // []=== BASE SCHEMATICS ===[]
    public int saveBaseSchematic(final UUID uuid, final File file) {
        final String method = getMethodName();
        try (final PreparedStatement empty = sharedLongConn.prepareStatement("SELECT server FROM MuxBaseSchematics ORDER BY timestamp LIMIT 1")) {
            try (final ResultSet rs = empty.executeQuery()) {}
        } catch (SQLException ignored) {
        }

        try (final PreparedStatement st = sharedLongConn.prepareStatement("INSERT INTO MuxBaseSchematics SET uuid = UNHEX(?), timestamp = ?, server = UNHEX(?), schematic = ?;", Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, uuid.toString().replace("-", ""));
            st.setLong(2, System.currentTimeMillis());
            st.setString(3, ms.getServerId().toString().replace("-", ""));
            st.setBlob(4, Files.newInputStream(file.toPath()));
            st.executeUpdate();
            try (final ResultSet rs = st.getGeneratedKeys()) {
                timings.stop(method);
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (final SQLException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            file.delete();
        }
        timings.stop(method);
        return -1;
    }

    public int canSaveBaseSchematic(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT timestamp, id FROM MuxBaseSchematics WHERE uuid = UNHEX(?) AND server = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            sel.setString(2, ms.getServerId().toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    if (rs.getLong("timestamp") > System.currentTimeMillis() - 86400000L) {
                        timings.stop(method);
                        return rs.getInt("id");
                    } else deleteBaseSchematic(rs.getInt("id"));
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        timings.stop(method);
        return -1;
    }

    public void deleteOldBaseSchematics() {
        final String method = getMethodName();
        try (final PreparedStatement del = sharedConn.prepareStatement("DELETE FROM MuxBaseSchematics WHERE timestamp < ? AND server = UNHEX(?)")) {
            del.setLong(1, System.currentTimeMillis() - 86400000L);
            del.setString(2, ms.getServerId().toString().replace("-", ""));
            del.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        timings.stop(method);
    }

    private void deleteBaseSchematic(final int id) {
        final String method = getMethodName();
        try (final PreparedStatement del = sharedConn.prepareStatement("DELETE FROM MuxBaseSchematics WHERE id = ?")) {
            del.setInt(1, id);
            del.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        timings.stop(method);
    }

    // []=== BANS HISTORY ===[]
    public void addBanEntry(final UUID uuid, final String name, final String action, final String param, final String reason, final String causer) {
        final String method = getMethodName();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        final String time = sdf.format(new Date());
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxHistoryBans (uuid,player,action,param,reason,time,causer) VALUES(UNHEX(?),?,?,?,?,?,?)")) {
            set.setString(1, uuid.toString().replace("-", ""));
            set.setString(2, name);
            set.setString(3, action);
            set.setString(4, param);
            set.setString(5, reason);
            set.setString(6, time);
            set.setString(7, causer);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void removeBanEntry(final int id, final String causer) {
        final String method = getMethodName();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        final String time = sdf.format(new Date());
        try (final PreparedStatement set = conn.prepareStatement("UPDATE MuxHistoryBans SET removecauser=?, removetime=? WHERE id = ?")) {
            set.setString(1, causer);
            set.setString(2, time);
            set.setInt(3, id);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public List<BanEntry> getBanEntries(final int limit) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT id, HEX(uuid) AS uuid, player, action, param, reason, time as unformattedtime, DATE_FORMAT(time, '%d-%m-%Y %H:%i') as formattedtime, causer, removecauser, DATE_FORMAT(removetime, '%d-%m-%Y %H:%i') as removetime FROM MuxHistoryBans WHERE removecauser IS NULL ORDER BY time DESC LIMIT ?")) {
            sel.setInt(1, limit);
            try (final ResultSet rs = sel.executeQuery()) {
                final List<BanEntry> ret = new ArrayList<>();
                while (rs.next()) {
                    final String time = rs.getString("formattedtime"), removetime = rs.getString("removetime");
                    ret.add(new BanEntry(rs.getInt("id"),
                            UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")),
                            rs.getString("player"), rs.getString("action"), rs.getString("param"), rs.getString("reason"),
                            time, rs.getString("causer"), rs.getString("removecauser"), removetime, rs.getString("unformattedtime")));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public List<BanEntry> searchBanEntries(final String searchTerm, final int limit) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT id, HEX(uuid) AS uuid, player, action, param, reason, time as unformattedtime, DATE_FORMAT(time, '%d-%m-%Y %H:%i') as formattedtime, causer, removecauser, DATE_FORMAT(removetime, '%d-%m-%Y %H:%i') as removetime FROM MuxHistoryBans WHERE (removecauser IS NULL) AND (player LIKE ? OR HEX(uuid) = ? OR causer LIKE ? OR param LIKE ?) ORDER BY time DESC LIMIT ?")) {
            // Check if searchTerm is a username
            final OfflinePlayer op = ms.getPlayer(searchTerm);
            final UUID uuid = (op == null ? null : op.getUniqueId());
            if (uuid != null) {
                sel.setString(2, uuid.toString().replace("-", ""));
            } else {
                sel.setString(2, "NOMATCH");
            }
            sel.setString(1, "%" + searchTerm + "%");
            sel.setString(3, "%" + searchTerm + "%");
            sel.setString(4, "%" + searchTerm + "%");
            sel.setInt(5, limit);
            try (final ResultSet rs = sel.executeQuery()) {
                final List<BanEntry> ret = new ArrayList<>();
                while (rs.next()) {
                    final String time = rs.getString("formattedtime"), removetime = rs.getString("removetime");
                    ret.add(new BanEntry(rs.getInt("id"),
                            UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")),
                            rs.getString("player"), rs.getString("action"), rs.getString("param"), rs.getString("reason"),
                            time, rs.getString("causer"), rs.getString("removecauser"), removetime, rs.getString("unformattedtime")));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public List<BanEntry> getBanEntries(final UUID uuid, final int limit) {
        final String method = getMethodName();
        final List<BanEntry> ret = new ArrayList<>();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT id, player, action, param, reason, time as unformattedtime, DATE_FORMAT(time, '%d-%m-%Y %H:%i') as formattedtime, causer, removecauser, DATE_FORMAT(removetime, '%d-%m-%Y %H:%i') as removetime FROM MuxHistoryBans WHERE uuid = UNHEX(?) ORDER BY time DESC LIMIT ?")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            sel.setInt(2, limit);
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    final String time = rs.getString("formattedtime"), removetime = rs.getString("removetime");
                    ret.add(new BanEntry(rs.getInt("id"), uuid, rs.getString("player"), rs.getString("action"), rs.getString("param"),
                            rs.getString("reason"), time, rs.getString("causer"), rs.getString("removecauser"), removetime,
                            rs.getString("unformattedtime")));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return ret;
    }

    public String getBanReason(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT reason FROM MuxHistoryBans WHERE uuid=UNHEX(?) ORDER BY time DESC LIMIT 1")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.next()) {
                    return rs.getString("reason");
                } else {
                    return "Unbekannt";
                }
            }
        } catch (final SQLException e) {
            timings.stop(method);
            return "Unbekannt";
        }
    }

    // []=== CUSTOM RANK ===[]

    public MuxCustomRank.TagData loadCustomRank(final UUID uuid) {
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT tag, symbol, color, lastchange, active FROM MuxCustomRank WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    return ms.getCustomRank().new TagData(rs.getString("tag"), rs.getInt("symbol"), ChatColor.valueOf(rs.getString("color")), rs.getLong("lastchange"), rs.getBoolean("active"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveCustomRank(final UUID uuid, final MuxCustomRank.TagData tagData) {
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO MuxCustomRank SET uuid = UNHEX(?), tag=?, symbol=?, color=?, lastchange=?, active=? ON DUPLICATE KEY UPDATE tag=?, symbol=?, color=?, lastchange=?, active=?")) {
            st.setString(1, uuid.toString().replace("-", ""));
            st.setString(2, tagData.getTag());
            st.setInt(3, tagData.getSymbolIndex());
            st.setString(4, tagData.getChatColor().name());
            st.setLong(5, tagData.getLastChange());
            st.setBoolean(6, tagData.isActive());
            st.setString(7, tagData.getTag());
            st.setInt(8, tagData.getSymbolIndex());
            st.setString(9, tagData.getChatColor().name());
            st.setLong(10, tagData.getLastChange());
            st.setBoolean(11, tagData.isActive());
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteCustomRank(final UUID uuid) {
        try (final PreparedStatement st = sharedConn.prepareStatement("DELETE FROM MuxCustomRank WHERE uuid = UNHEX(?)")) {
            st.setString(1, uuid.toString().replace("-", ""));
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    // []=== USER ===[]

    public MuxUser loadPlayer(final UUID uuid, final boolean global) {
        final String method = getMethodName();
        final String uuidStr = uuid.toString().replace("-", "");
        try (final PreparedStatement sel = conn.prepareStatement("SELECT player, login, firstlogin, online, locationworld, locationx, locationy, locationz, " + (ms.isBeta() ? "testcoins" : "coins") + " AS coins, trophies, profits, kills, deaths, permban, timeban, permmute, timemute, pvpban, mails, news, settings, lastranked, " + (ms.isBeta() ? "testcents" : "cents") + " AS cents FROM MuxUser WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuidStr);
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.last()) {
                    final EvictingQueue<String> mails = EvictingQueue.create(50);
                    final Collection<String> cmails = gson.fromJson(rs.getString("mails"), new TypeToken<LinkedList<String>>() {
                    }.getType());
                    Settings settings = gson.fromJson(rs.getString("settings"), new TypeToken<Settings>() {
                    }.getType());
                    if (settings == null) settings = new Settings();
                    if (cmails != null) mails.addAll(cmails);
                    final MuxUser u = new MuxUser(uuid, rs.getString("player"), rs.getLong("login"), rs.getLong("firstlogin"), rs.getLong("online"),
                            new Location(ms.getServer().getWorld(rs.getString("locationworld")), rs.getDouble("locationx"), rs.getDouble("locationy"), rs.getDouble("locationz")),
                            rs.getLong("coins"), rs.getDouble("cents"), rs.getInt("trophies"), rs.getDouble("profits"), rs.getInt("kills"),
                            rs.getInt("deaths"), rs.getBoolean("permban"), rs.getLong("timeban"), rs.getBoolean("permmute"), rs.getLong("timemute"),
                            rs.getLong("pvpban"), mails, rs.getBoolean("news"), settings, rs.getLong("lastranked"));
                    if (global) {
                        try (final PreparedStatement gsel = sharedConn.prepareStatement("SELECT player, login, firstlogin, ip, online, HEX(lastserver) as lastserver, " + (ms.isBeta() ? "testgems" : "gems") + " AS gems FROM MuxGlobalUser WHERE uuid = UNHEX(?)")) {
                            gsel.setString(1, uuidStr);
                            try (final ResultSet grs = gsel.executeQuery()) {
                                if (grs.last()) {
                                    u.setGlobalUser(true);
                                    u.setName(grs.getString("player"));
                                    u.setGlobalLoginTimestamp(grs.getLong("login"));
                                    u.setGlobalFirstLogin(grs.getLong("firstlogin"));
                                    u.setIp(grs.getString("ip"));
                                    u.setGlobalOnline(grs.getLong("online"));
                                    u.setGems(grs.getInt("gems"));
                                    u.setLastServer(UUID.fromString(grs.getString("lastserver").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
                                }
                            }
                        }
                    }
                    return u;
                } else return null;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public MuxUser loadPlayer(final UUID uuid) {
        return this.loadPlayer(uuid, true);
    }

    public String getPlayerName(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT player FROM MuxUser WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.next())
                    return rs.getString("player");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public Map<String, MuxHomes.Home> loadHomes(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT homes FROM MuxHomes WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.last()) {
                    return gson.fromJson(rs.getString("homes"), new TypeToken<LinkedHashMap<String, MuxHomes.Home>>() {
                    }.getType());
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
            return null;
        }
        timings.stop(method);
        return new LinkedHashMap<>();
    }

    public void saveGlobalPlayer(final MuxUser u) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxGlobalUser SET uuid=UNHEX(?), player=?, login=?, firstlogin=?, ip=?, online=?, lastserver=UNHEX(?)," + (ms.isBeta() ? "testgems" : "gems") + "=? ON DUPLICATE KEY UPDATE player=?, login=?, firstlogin=?, ip=?, online=?, lastserver=UNHEX(?), " + (ms.isBeta() ? "testgems" : "gems") + "=?")) {
            final String lastServer;
            set.setString(1, u.getUUID().toString().replace("-", ""));
            set.setString(2, u.getName());
            set.setLong(3, u.getGlobalLoginTimestamp());
            set.setLong(4, u.getGlobalFirstLogin());
            set.setString(5, u.getIp());
            set.setLong(6, u.getGlobalOnline());
            set.setString(7, lastServer = u.getLastServer().toString().replace("-", ""));
            set.setInt(8, u.getGems());
            set.setString(9, u.getName());
            set.setLong(10, u.getGlobalLoginTimestamp());
            set.setLong(11, u.getGlobalFirstLogin());
            set.setString(12, u.getIp());
            set.setLong(13, u.getGlobalOnline());
            set.setString(14, lastServer);
            set.setInt(15, u.getGems());
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void savePlayer(final MuxUser u) {
        final String method = getMethodName();
        String uuid = null;
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxUser SET uuid=UNHEX(?),player=?,login=?,firstlogin=?,online=?,locationworld=?,locationx=?,locationy=?,locationz=?," + (ms.isBeta() ? "testcoins" : "coins") + "=?," + (ms.isBeta() ? "testcents" : "cents") + "=?,trophies=?,profits=?,kills=?,deaths=?,permban=?,timeban=?,permmute=?,timemute=?,pvpban=?,mails=?,news=?,settings=?,lastranked=? " +
                "ON DUPLICATE KEY UPDATE player=?,login=?,firstlogin=?,online=?,locationworld=?,locationx=?,locationy=?,locationz=?," + (ms.isBeta() ? "testcoins" : "coins") + "=?," + (ms.isBeta() ? "testcents" : "cents") + "=?,trophies=?,profits=?,kills=?,deaths=?,permban=?,timeban=?,permmute=?,timemute=?,pvpban=?,mails=?,news=?,settings=?,lastranked=?")) {
            final String mails = gson.toJson(u.getMails()), settings = gson.toJson(u.getSettings());
            set.setString(1, uuid = u.getUUID().toString().replace("-", ""));
            set.setString(2, u.getName());
            set.setLong(3, u.getLoginTimestamp());
            set.setLong(4, u.getFirstLogin());
            set.setLong(5, u.getOnlineTime());
            final Location lastloc = u.getLastLocation();
            final String wname = ms.getServer().getWorlds().get(0).getName(), w = lastloc == null || lastloc.getWorld() == null ? wname : u.getLastLocation().getWorld().getName();
            set.setString(6, w);
            set.setInt(7, u.getLastLocation().getBlockX());
            set.setInt(8, u.getLastLocation().getBlockY());
            set.setInt(9, u.getLastLocation().getBlockZ());
            set.setLong(10, u.getCoins());
            set.setDouble(11, u.getCents());
            set.setInt(12, u.getTrophies());
            set.setDouble(13, u.getProfits());
            set.setInt(14, u.getKills());
            set.setInt(15, u.getDeaths());
            set.setBoolean(16, u.isPermban());
            set.setLong(17, u.getUnbanTime());
            set.setBoolean(18, u.isPermMute());
            set.setLong(19, u.getUnmuteTime());
            set.setLong(20, u.getPvPUnbanTime());
            set.setString(21, mails);
            set.setBoolean(22, u.seenNews());
            set.setString(23, settings);
            set.setLong(24, u.getLastRankedMatch());
            set.setString(25, u.getName());
            set.setLong(26, u.getLoginTimestamp());
            set.setLong(27, u.getFirstLogin());
            set.setLong(28, u.getOnlineTime());
            set.setString(29, w);
            set.setInt(30, u.getLastLocation().getBlockX());
            set.setInt(31, u.getLastLocation().getBlockY());
            set.setInt(32, u.getLastLocation().getBlockZ());
            set.setLong(33, u.getCoins());
            set.setDouble(34, u.getCents());
            set.setInt(35, u.getTrophies());
            set.setDouble(36, u.getProfits());
            set.setInt(37, u.getKills());
            set.setInt(38, u.getDeaths());
            set.setBoolean(39, u.isPermban());
            set.setLong(40, u.getUnbanTime());
            set.setBoolean(41, u.isPermMute());
            set.setLong(42, u.getUnmuteTime());
            set.setLong(43, u.getPvPUnbanTime());
            set.setString(44, mails);
            set.setBoolean(45, u.seenNews());
            set.setString(46, settings);
            set.setLong(47, u.getLastRankedMatch());
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        if (u.homesChanged() && uuid != null) {
            try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxHomes SET uuid=UNHEX(?),homes=? ON DUPLICATE KEY UPDATE homes=?")) {
                set.setString(1, uuid);
                final String homes = gson.toJson(u.getHomes());
                set.setString(2, homes);
                set.setString(3, homes);
                set.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        if (u.isGlobalUser()) saveGlobalPlayer(u);
        timings.stop(method);
    }

    public void updateName(final String name) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT HEX(uuid) AS uuid FROM MuxUser WHERE player=?")) {
            sel.setString(1, name);
            final Set<UUID> toUpdate = new HashSet<>();
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    final String uuid = rs.getString("uuid");
                    toUpdate.add(UUID.fromString(uuid.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
                }
                if (toUpdate.isEmpty() == false) {
                    for (final Map.Entry<UUID, String> entry : fetchNames(toUpdate).entrySet()) {
                        updateChangedName(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    private boolean updateChangedName(final UUID uuid, final String name) {
        final String method = getMethodName();
        final String suuid = uuid.toString().replace("-", "");
        try (final PreparedStatement update = conn.prepareStatement("UPDATE MuxUser SET player = ? WHERE uuid = UNHEX(?)");
             final PreparedStatement update2 = conn.prepareStatement("UPDATE MuxEnderChest SET player = ? WHERE uuid = UNHEX(?)");
             final PreparedStatement update3 = sharedConn.prepareStatement("UPDATE MuxExtras SET player = ? WHERE uuid = UNHEX(?)");
             final PreparedStatement update4 = sharedConn.prepareStatement("UPDATE MuxAffiliateCashout SET player = ? WHERE uuid = UNHEX(?)");
             final PreparedStatement update5 = sharedConn.prepareStatement("UPDATE MuxGlobalUser SET player = ? WHERE uuid = UNHEX(?)");
             final PreparedStatement update6 = sharedConn.prepareStatement("UPDATE MuxEmails SET player = ? WHERE uuid = UNHEX(?)")) {
            update.setString(1, name);
            update2.setString(1, name);
            update3.setString(1, name);
            update4.setString(1, name);
            update5.setString(1, name);
            update6.setString(1, name);
            update.setString(2, suuid);
            update2.setString(2, suuid);
            update3.setString(2, suuid);
            update4.setString(2, suuid);
            update5.setString(2, suuid);
            update6.setString(2, suuid);
            update.executeUpdate();
            update2.executeUpdate();
            update3.executeUpdate();
            update4.executeUpdate();
            update5.executeUpdate();
            update6.executeUpdate();
            timings.stop(method);
            return true;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    private String fetchNameFromMojang(final UUID uuid) {
        final String method = getMethodName();
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "")).openConnection();
            final JSONObject response = (JSONObject) new JSONParser().parse(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            timings.stop(method);
            return (String) response.get("name");
        } catch (final ParseException | IOException ignored) {
            System.err.println("Error fetching name from Mojang.");
            timings.stop(method);
            return null;
        }
    }

    public Map<UUID, String> fetchNames(final Set<UUID> uuids) {
        final String method = getMethodName();
        final Map<UUID, String> uuidmap = new HashMap<>();
        try {
            for (final UUID uuid : uuids) {
                final HttpURLConnection connection = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "")).openConnection();
                final JSONObject response = (JSONObject) new JSONParser().parse(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                final String name = (String) response.get("name");
                if (name == null) continue;
                final String cause = (String) response.get("cause"), errorMessage = (String) response.get("errorMessage");

                if (cause != null && cause.length() > 0) throw new IllegalStateException(errorMessage);
                uuidmap.put(uuid, name);
            }
            System.out.println("MuxSystem> Playernames were successfully loaded.");
        } catch (final Exception e) {
            System.err.println("MuxSystem> Playernames failed to load. \n" + e.getMessage());
        }
        timings.stop(method);
        return uuidmap;
    }

    public void updateDuplicatedUserNames() {
        final String method = getMethodName();
        final Map<UUID, String> uuids = new HashMap<>();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT HEX(uuid) AS uuid, player FROM MuxUser WHERE player IN (SELECT player FROM MuxUser GROUP BY player HAVING COUNT(*) > 1)")) {
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    final String name = rs.getString("player");
                    uuids.put(UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), name);
                }
                System.out.println("MuxSystem> Found " + uuids.size() + " duplicated names.");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        if (uuids.isEmpty() == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    int count = 0, updatedNames = 0;
                    for (final Map.Entry<UUID, String> entry : uuids.entrySet()) {
                        if (count == 150) {
                            try {
                                Thread.sleep(1000L * 60L); // 1 minute
                                count = 0;
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        count++;
                        final UUID uuid = entry.getKey();
                        final String newName = fetchNameFromMojang(uuid);
                        System.out.println(newName + " --> " + uuid);
                        if (newName != null) {
                            updateChangedName(uuid, newName);
                        }
                    }
                    System.out.println("MuxSystem> Updated names: " + updatedNames);
                }
            }.runTaskAsynchronously(ms);
        }
        timings.stop(method);
    }

    public Map.Entry<String, UUID> getPlayerUUID(final String name) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(uuid) AS uuid, player FROM MuxGlobalUser WHERE player = ?")) {
            sel.setString(1, name);
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.last()) {
                    return new AbstractMap.SimpleEntry<>(rs.getString("player"), UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new AbstractMap.SimpleEntry<>(null, null);
    }

    public List<String> getPlayersWithSameIP(final String ip, final String name) {
        final String method = getMethodName();
        if (ip == null) return new ArrayList<>();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT player FROM MuxGlobalUser WHERE ip = ? AND player != ?")) {
            sel.setString(1, ip);
            sel.setString(2, name);
            final List<String> players = new ArrayList<>();
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    players.add(rs.getString("player"));
                }
            }
            timings.stop(method);
            return players;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public List<UUID> getUUIDsWithSameIP(final String ip, final String name) {
        final String method = getMethodName();
        if (ip == null) return new ArrayList<>();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(uuid) AS uuid FROM MuxGlobalUser WHERE ip = ? AND player != ?")) {
            sel.setString(1, ip);
            sel.setString(2, name);
            final List<UUID> players = new ArrayList<>();
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    players.add(UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
                }
            }
            timings.stop(method);
            return players;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public void resetNews() {
        final String method = getMethodName();
        try (final PreparedStatement clr = conn.prepareStatement("UPDATE MuxUser SET news = 0")) {
            clr.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public long getNewsCount() {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("SELECT COUNT(news) AS c FROM MuxUser WHERE news = 1")) {
            try (final ResultSet rs = st.executeQuery()) {
                timings.stop(method);
                if (rs.next())
                    return rs.getLong("c");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return 0;
    }

    public Object[] getServerStats() {
        final String method = getMethodName();
        final List<Long> stats = new ArrayList<>();
        try (final PreparedStatement sel = longconn.prepareStatement("SELECT COUNT(*) FROM MuxUser UNION ALL SELECT SUM(" + (ms.isBeta() ? "testcoins" : "coins") + ") FROM MuxUser WHERE " + (ms.isBeta() ? "testcoins" : "coins") + " > 200 "
                + "UNION ALL SELECT SUM(" + (ms.isBeta() ? "testchips" : "chips") + ") FROM MuxCasino UNION ALL SELECT COUNT(*) FROM MuxBase");
             final PreparedStatement sel2 = sharedConn.prepareStatement("SELECT SUM(" + (ms.isBeta() ? "testgems" : "gems") + ") FROM MuxGlobalUser")) {
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    stats.add(rs.getLong(1));
                }
            }
            try (final ResultSet rs = sel2.executeQuery()) {
                while (rs.next()) {
                    stats.add(rs.getLong(1));
                }
            }
            timings.stop(method);
            return new Object[]{stats.get(0), stats.get(1), stats.get(2), stats.get(3), stats.get(4)};
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new Object[]{0, 0, 0, 0, 0};
    }

    // []=== FRIENDS ===[]
    public Set<UUID> getFriends(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT HEX(uuid2) AS uuid2 FROM MuxFriends WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                final HashSet<UUID> ret = new HashSet<>();
                while (rs.next()) {
                    ret.add(UUID.fromString(rs.getString("uuid2").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new HashSet<>();
    }

    public void addFriend(final UUID uuid, final UUID uuid2) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxFriends(uuid,uuid2) VALUES(UNHEX(?),UNHEX(?))");
             final PreparedStatement set2 = conn.prepareStatement("INSERT INTO MuxFriends(uuid,uuid2) VALUES(UNHEX(?),UNHEX(?))")) {
            final String s1 = uuid.toString().replace("-", ""), s2 = uuid2.toString().replace("-", "");
            set.setString(1, s1);
            set.setString(2, s2);
            set2.setString(1, s2);
            set2.setString(2, s1);
            set.executeUpdate();
            set2.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void delFriend(final UUID uuid, final UUID uuid2) {
        final String method = getMethodName();
        try (final PreparedStatement del = conn.prepareStatement("DELETE FROM MuxFriends WHERE uuid = UNHEX(?) AND uuid2 = UNHEX(?)");
             final PreparedStatement del2 = conn.prepareStatement("DELETE FROM MuxFriends WHERE uuid = UNHEX(?) AND uuid2 = UNHEX(?)")) {
            final String s1 = uuid.toString().replace("-", ""), s2 = uuid2.toString().replace("-", "");
            del.setString(1, s1);
            del.setString(2, s2);
            del2.setString(1, s2);
            del2.setString(2, s1);
            del.executeUpdate();
            del2.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== VOTE ===[]
    public void addVote(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxVote(uuid) VALUES(UNHEX(?))")) {
            set.setString(1, uuid.toString().replace("-", ""));
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void addSecondVote(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("UPDATE MuxVote SET firstvote = 0 WHERE uuid = UNHEX(?)")) {
            set.setString(1, uuid.toString().replace("-", ""));
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public List<UUID> getFirstVotes() {
        final String method = getMethodName();
        final List<UUID> result = new ArrayList<>();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(uuid) as uuid FROM MuxVote WHERE firstvote = 1")) {
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next())
                    result.add(UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return result;
    }

    public boolean isFirstVote(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT firstvote FROM MuxVote WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.next())
                    return rs.getBoolean("firstvote");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return true;
    }

    public List<UUID> getVotes() {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(uuid) AS uuid FROM MuxVote WHERE firstvote = 0")) {
            try (final ResultSet rs = sel.executeQuery()) {
                final List<UUID> votes = new ArrayList<>();
                while (rs.next()) {
                    votes.add(UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
                }
                timings.stop(method);
                return votes;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public void resetVotes() {
        final String method = getMethodName();
        try (final Statement stat = sharedConn.createStatement()) {
            stat.execute("TRUNCATE TABLE MuxVote");
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== SUPPORT ===[]
    public int addSupportEntry(final String name) {
        final String method = getMethodName();
        int id = -1;
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        final String time = sdf.format(new Date());
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxSupport(player,time) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            set.setString(1, name);
            set.setString(2, time);
            set.executeUpdate();
            try (final ResultSet generatedKeys = set.getGeneratedKeys()) {
                if (generatedKeys.next())
                    id = generatedKeys.getInt(1);
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return id;
    }

    public int addFullSupportEntry(final String name, final UUID supporter, final List<String> conversation) {
        final String method = getMethodName();
        int id = -1;
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        final String time = sdf.format(new Date());
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxSupport(player,time,supporter,conversation,reaction) VALUES(?,?,UNHEX(?),?,?)", Statement.RETURN_GENERATED_KEYS)) {
            set.setString(1, name);
            set.setString(2, time);
            set.setString(3, supporter.toString().replace("-", ""));
            set.setString(4, gson.toJson(conversation));
            set.setLong(5, -1L);
            set.executeUpdate();
            try (final ResultSet generatedKeys = set.getGeneratedKeys()) {
                if (generatedKeys.next())
                    id = generatedKeys.getInt(1);
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return id;
    }

    public void addSupporterEntry(final String name, final UUID supporter, final List<String> conversation, final long reaction) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("UPDATE MuxSupport SET supporter = UNHEX(?), conversation = ?, reaction = ? WHERE player = ? ORDER BY id DESC LIMIT 1")) {
            set.setString(1, supporter.toString().replace("-", ""));
            set.setString(2, gson.toJson(conversation));
            set.setLong(3, reaction);
            set.setString(4, name);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void saveSupportLike(final int id, final boolean liked, final UUID uuid, final UUID supporter) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("UPDATE MuxSupport SET liked = ? WHERE id = ?")) {
            set.setBoolean(1, liked);
            set.setInt(2, id);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        if (updateCachedSupport(uuid, supporter) == false) {
            try (final PreparedStatement update = sharedConn.prepareStatement("UPDATE MuxHistory SET param = '1' WHERE uuid = UNHEX(?) AND type = 'SUPPORT' AND victim = UNHEX(?) ORDER BY timestamp DESC LIMIT 1")) {
                update.setString(1, uuid.toString().replace("-", ""));
                update.setString(2, supporter.toString().replace("-", ""));
                update.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        timings.stop(method);
    }

    public boolean updateCachedSupport(final UUID uuid, final UUID supporter) {
        for (final MuxHistory.CachedHistory entry : ms.getHistory().getCachedHistory()) {
            if (entry.getType().equals("SUPPORT") && entry.getUUID().equals(uuid) && entry.getUUID2().equals(supporter)) {
                entry.setParam("1");
                return true;
            }
        }
        return false;
    }

    public Object[] getSupportStats() {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT (SELECT COUNT(*) FROM MuxSupport) as total, (SELECT AVG(reaction) FROM MuxSupport WHERE supporter != \"NULL\" AND reaction != -1) as avgreaction, (SELECT COUNT(supporter) FROM MuxSupport) as done, HEX(supporter) AS supporter, COUNT(*) AS amount FROM MuxSupport WHERE supporter != \"NULL\" GROUP BY supporter ORDER BY amount DESC")) {
            try (final ResultSet rs = sel.executeQuery()) {
                final Map<UUID, Integer> list = new LinkedHashMap<>();
                while (rs.next()) {
                    list.put(UUID.fromString(rs.getString("supporter").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), rs.getInt("amount"));
                }
                timings.stop(method);
                if (rs.last()) {
                    return new Object[]{rs.getInt("done"), rs.getInt("total"), list, rs.getLong("avgreaction")};
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public Integer[] getSupportLikeAndSupportCount(final UUID supporter) {
        final String method = getMethodName();
        Integer[] result = {0, 0};
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT COUNT(*) as count, (SELECT COUNT(*) FROM MuxSupport WHERE supporter = UNHEX(?) AND liked = 1) as likes FROM MuxSupport WHERE supporter = UNHEX(?)")) {
            sel.setString(1, supporter.toString().replace("-", ""));
            sel.setString(2, supporter.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    result = new Integer[]{rs.getInt("count"), rs.getInt("likes")};
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return result;
    }

    public List<Object[]> getSupportStats(final UUID supporter) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT conversation, time, liked FROM MuxSupport WHERE supporter = UNHEX(?) ORDER BY id DESC")) {
            sel.setString(1, supporter.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                final List<Object[]> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(new Object[]{rs.getString("time"), gson.fromJson(rs.getString("conversation"), new TypeToken<ArrayList<String>>() {
                    }.getType()), rs.getBoolean("liked")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public void resetSupports() {
        final String method = getMethodName();
        try (final Statement stat = sharedConn.createStatement()) {
            stat.execute("TRUNCATE TABLE MuxSupport");
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== CLANS ===[]
    private final Map<String, List<UUID>> clancache = new HashMap<>();
    private final Map<UUID, String> fastclancache = new HashMap<>();

    private void loadClanCache() {
        final String method = getMethodName();
        clancache.clear();
        fastclancache.clear();
        trophycache.clear();
        try (final PreparedStatement exists = conn.prepareStatement("SELECT mcm.cname, HEX(mcm.uuid) AS uuid, mc.clan AS clan, IF(ms.lastranked > ? AND (ms.pvpban = -1 OR ms.pvpban < ?), ms.trophies, 0) AS trophies FROM MuxClanMembers AS mcm INNER JOIN MuxClans AS mc ON mcm.cname = mc.clan LEFT JOIN MuxUser AS ms ON mcm.uuid = ms.uuid WHERE mcm.invited = 0")) {
            exists.setLong(1, System.currentTimeMillis() - 172800000L);
            exists.setLong(2, System.currentTimeMillis());
            try (final ResultSet rs = exists.executeQuery()) {
                final Map<String, Integer> tmpctrophycache = new HashMap<>();
                while (rs.next()) {
                    clancache.putIfAbsent(rs.getString("cname"), new ArrayList<>());
                    final UUID uuid = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    clancache.get(rs.getString("cname")).add(uuid);
                    fastclancache.put(uuid, rs.getString("clan"));
                    final Integer ck = tmpctrophycache.get(rs.getString("clan"));
                    tmpctrophycache.put(rs.getString("clan"), (ck != null ? ck : 0) + rs.getInt("trophies"));
                }
                for (final Map.Entry<UUID, String> entry : fastclancache.entrySet()) {
                    trophycache.put(entry.getKey(), tmpctrophycache.get(entry.getValue()));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public boolean createClan(final String clan, final Location l) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT 1 FROM MuxClans WHERE clan=?")) {
            sel.setString(1, clan);
            try (final ResultSet rs = sel.executeQuery()) {
                rs.last();
                if (rs.getRow() == 0) {
                    try (final PreparedStatement newuser = conn.prepareStatement("INSERT INTO MuxClans(clan,base) VALUES(?,?)")) {
                        newuser.setString(1, clan);
                        newuser.setString(2, ms.locationToString(l));
                        newuser.executeUpdate();
                        clancache.put(clan, new ArrayList<>());
                        timings.stop(method);
                        return true;
                    }
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    public Location getClanBase(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT mc.base FROM MuxClans AS mc INNER JOIN MuxClanMembers AS mcm ON mc.clan = mcm.cname WHERE mcm.uuid=UNHEX(?) AND mcm.invited=0")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                rs.last();
                if (rs.getRow() != 0) {
                    timings.stop(method);
                    return ms.stringToLocation(rs.getString("base"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public void updateTrophyCache(final String clan) {
        final String method = getMethodName();
        final List<UUID> members = getClanMembers(clan);
        int trophies = 0;
        final StringBuilder sql = new StringBuilder("SELECT SUM(trophies) FROM MuxUser WHERE uuid IN (UNHEX(?)");
        for (short i = 1; i < members.size(); i++) {
            sql.append(",UNHEX(?)");
        }
        sql.append(") AND (pvpban = -1 OR pvpban < ?) AND lastranked > ?");
        try (final PreparedStatement sel = conn.prepareStatement(sql.toString())) {
            for (short i = 0; i < members.size(); i++) {
                sel.setString(i + 1, members.get(i).toString().replace("-", ""));
            }
            sel.setLong(members.size() + 1, System.currentTimeMillis());
            sel.setLong(members.size() + 2, System.currentTimeMillis() - 172800000L);
            try (final ResultSet rs = sel.executeQuery()) {
                trophies = rs.last() ? rs.getInt(1) : 0;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        for (final UUID plr : members) {
            trophycache.put(plr, trophies);
        }
        timings.stop(method);
    }

    public void updateGlobalTrophyCache() {
        final String method = getMethodName();
        final Map<String, Integer> tmpctrophycache = new HashMap<>();
        try (final PreparedStatement exists = conn.prepareStatement("SELECT mcm.cname, HEX(mcm.uuid) AS uuid, mc.clan AS clan, IF(ms.lastranked > ? AND (ms.pvpban = -1 OR ms.pvpban < ?), ms.trophies, 0) AS trophies FROM MuxClanMembers AS mcm INNER JOIN MuxClans AS mc ON mcm.cname = mc.clan LEFT JOIN MuxUser AS ms ON mcm.uuid = ms.uuid WHERE mcm.invited = 0")) {
            exists.setLong(1, System.currentTimeMillis() - 172800000L);
            exists.setLong(2, System.currentTimeMillis());
            try (final ResultSet rs = exists.executeQuery()) {
                while (rs.next()) {
                    final Integer ck = tmpctrophycache.get(rs.getString("clan"));
                    tmpctrophycache.put(rs.getString("clan"), (ck != null ? ck : 0) + rs.getInt("trophies"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        } finally {
            for (final Map.Entry<UUID, String> entry : fastclancache.entrySet()) {
                final Integer trophies = tmpctrophycache.get(entry.getValue());
                if (trophies != null)
                    trophycache.put(entry.getKey(), trophies);
            }
        }
        timings.stop(method);
    }


    public boolean addPlayerToClan(final UUID plr, final String clan, final int rank, final boolean invited) {
        final String method = getMethodName();
        final List<UUID> members = clancache.get(clan);
        if (members == null) return false;
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxClanMembers(cname,uuid,`rank`,invited) VALUES(?,UNHEX(?),?,?)")) {
            set.setString(1, clan);
            set.setString(2, plr.toString().replace("-", ""));
            set.setInt(3, rank);
            set.setBoolean(4, invited);
            set.executeUpdate();
            if (invited == false) {
                members.add(plr);
                fastclancache.put(plr, clan);
                updateTrophyCache(clan);
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return true;
    }

    public void leaveClan(final UUID plr) {
        final String method = getMethodName();
        try (final PreparedStatement del = conn.prepareStatement("DELETE FROM MuxClanMembers WHERE uuid=UNHEX(?) AND invited=0")) {
            del.setString(1, plr.toString().replace("-", ""));
            del.execute();
            final String clan = getClanFromPlayer(plr);
            if (clan != null) {
                clancache.get(clan).remove(plr);
                updateTrophyCache(clan);
            }
            fastclancache.remove(plr);
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void deleteInvitation(final UUID plr, final String clan) {
        final String method = getMethodName();
        try (final PreparedStatement del = conn.prepareStatement("DELETE FROM MuxClanMembers WHERE uuid=UNHEX(?) AND invited=1 AND cname=?")) {
            del.setString(1, plr.toString().replace("-", ""));
            del.setString(2, clan);
            del.execute();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void deleteInvitations(final UUID plr) {
        final String method = getMethodName();
        try (final PreparedStatement del = conn.prepareStatement("DELETE FROM MuxClanMembers WHERE uuid=UNHEX(?) AND invited=1")) {
            del.setString(1, plr.toString().replace("-", ""));
            del.execute();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public List<String> getInvitations(final UUID plr) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT mc.clan FROM MuxClanMembers AS mcm INNER JOIN MuxClans AS mc ON mcm.cname = mc.clan WHERE mcm.uuid=UNHEX(?) AND mcm.invited=1")) {
            sel.setString(1, plr.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                final List<String> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(rs.getString("clan"));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public void setRank(final UUID plr, final short rank) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("UPDATE MuxClanMembers SET `rank`=? WHERE uuid=UNHEX(?) AND invited=0")) {
            set.setInt(1, rank);
            set.setString(2, plr.toString().replace("-", ""));
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public int getClanRank(final UUID plr) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT `rank` FROM MuxClanMembers WHERE uuid=UNHEX(?) AND invited=0")) {
            sel.setString(1, plr.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                rs.last();
                if (rs.getRow() != 0) {
                    timings.stop(method);
                    return rs.getInt("rank");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return -1;
    }

    public void setClanBase(final String clan, final Location l) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("UPDATE MuxClans SET base=? WHERE clan=?")) {
            set.setString(1, ms.locationToString(l));
            set.setString(2, clan);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public Location getClanBase(final String clan) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT base FROM MuxClans WHERE clan=?")) {
            sel.setString(1, clan);
            try (final ResultSet rs = sel.executeQuery()) {
                rs.last();
                if (rs.getRow() != 0) {
                    timings.stop(method);
                    return ms.stringToLocation(rs.getString("base"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public boolean renameClan(final String clan, final String newname) {
        try (final PreparedStatement check = conn.prepareStatement("SELECT clan FROM MuxClans WHERE clan=?")) {
            check.setString(1, newname);
            try (final ResultSet resultSet = check.executeQuery()) {
                if (resultSet.next()) {
                    return false;
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("UPDATE MuxClans SET clan=? WHERE clan=?");
             final PreparedStatement set2 = conn.prepareStatement("UPDATE MuxClanMembers SET cname=? WHERE cname=?")) {
            set.setString(1, newname);
            set.setString(2, clan);
            set.executeUpdate();
            set2.setString(1, newname);
            set2.setString(2, clan);
            set2.executeUpdate();
            final List<UUID> members = clancache.remove(clan);
            if (members != null) {
                clancache.put(newname, members);
                members.forEach(uuid -> fastclancache.put(uuid, newname));
            }
            updateTrophyCache(newname);
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return true;
    }

    public void deleteClan(final String clan) {
        final String method = getMethodName();
        try (final PreparedStatement del = conn.prepareStatement("DELETE FROM MuxClans WHERE clan=?");
             final PreparedStatement del2 = conn.prepareStatement("DELETE FROM MuxClanMembers WHERE cname=?")) {
            del.setString(1, clan);
            del.execute();
            del2.setString(1, clan);
            del2.execute();
            if (clancache.get(clan) != null) {
                clancache.get(clan).forEach(fastclancache::remove);
            }
            clancache.remove(clan);
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public boolean inOneClan(final UUID plr1, final UUID plr2) {
        for (final List<UUID> clan : clancache.values()) {
            if (clan.contains(plr1) && clan.contains(plr2)) return true;
        }
        return false;
    }

    public String getClanCase(final String clan) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT clan FROM MuxClans WHERE clan=?")) {
            sel.setString(1, clan);
            try (final ResultSet rs = sel.executeQuery()) {
                rs.last();
                if (rs.getRow() != 0) {
                    timings.stop(method);
                    return rs.getString("clan");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public List<UUID> getClanMembers(final String clan) {
        final List<UUID> members = clancache.get(clan);
        if (members == null) return new ArrayList<>();
        else return members;
    }

    public List<UUID> getClanMembersOfPlayer(final UUID plr) {
        for (final List<UUID> clan : clancache.values()) {
            if (clan.contains(plr)) return clan;
        }
        return null;
    }

    public List<UUID> getRankMembers(final String clan, final int rank) {
        final String method = getMethodName();
        try (final PreparedStatement grm = conn.prepareStatement("SELECT HEX(uuid) AS uuid FROM MuxClanMembers WHERE cname=? AND invited=0 AND `rank`=?")) {
            grm.setString(1, clan);
            grm.setInt(2, rank);
            try (final ResultSet rs = grm.executeQuery()) {
                final List<UUID> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public String getClanFromPlayer(final UUID plr) {
        return fastclancache.get(plr);
    }

    public boolean isInvited(final UUID plr, final String kurz) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT mcm.cname FROM MuxClanMembers AS mcm INNER JOIN MuxClans AS mc ON mcm.cname = mc.clan WHERE mcm.uuid=UNHEX(?) AND mc.clan=? AND mcm.invited=1")) {
            sel.setString(1, plr.toString().replace("-", ""));
            sel.setString(2, kurz);
            try (final ResultSet rs = sel.executeQuery()) {
                rs.last();
                timings.stop(method);
                return rs.getRow() != 0;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    public String getChatText(final UUID plr) {
        final String c = fastclancache.get(plr);
        if (c != null) {
            final int trophies = trophycache.get(plr);
            final String clan = getClanFromPlayer(plr);
            return (clan.equalsIgnoreCase("MuxTeam") ? "§4§l" : trophies >= 12500 ? "§6§l" : trophies >= 7500 ? "§5§l" : trophies >= 5000 ? "§a§l" : "§3") + c;
        }
        return "";
    }

    public int getClanTrophies(final String clan) {
        final String method = getMethodName();
        try (final PreparedStatement sel = clanconn.prepareStatement("SELECT SUM(ms.trophies) AS trophies FROM MuxClanMembers AS mcm INNER JOIN MuxUser AS ms ON mcm.uuid = ms.uuid WHERE mcm.cname = ? AND mcm.invited = 0 AND (ms.pvpban = -1 OR ms.pvpban < ?) AND ms.lastranked > ? GROUP BY mcm.cname ORDER BY NULL")) {
            sel.setString(1, clan);
            sel.setLong(2, System.currentTimeMillis());
            sel.setLong(3, System.currentTimeMillis() - 172800000L);
            try (final ResultSet rs = sel.executeQuery()) {
                rs.last();
                if (rs.getRow() != 0) {
                    timings.stop(method);
                    return rs.getInt("trophies");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return -1;
    }

    public List<Object[]> getClanTop10() {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT sq.*, HEX((SELECT mcmi.uuid FROM MuxClanMembers AS mcmi WHERE mcmi.cname = sq.clan AND mcmi.`rank` = 2)) AS owner FROM (SELECT mcm.cname AS clan, SUM(ms.trophies) AS trophies FROM MuxClanMembers AS mcm INNER JOIN MuxUser AS ms ON mcm.uuid = ms.uuid WHERE mcm.invited=0 AND mcm.cname != 'MuxTeam' AND ms.lastranked > ? AND (pvpban = -1 OR pvpban < ?) GROUP BY mcm.cname ORDER BY trophies DESC LIMIT 8) AS sq")) {
            sel.setLong(1, System.currentTimeMillis() - 172800000);
            sel.setLong(2, System.currentTimeMillis());
            try (final ResultSet rs = sel.executeQuery()) {
                final List<Object[]> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(new Object[]{rs.getString("clan"), UUID.fromString(rs.getString("owner").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), rs.getInt("trophies")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public int getClanRanking(final String clan) {
        final String method = getMethodName();
        try (final PreparedStatement rank = clanconn.prepareStatement("SELECT sb.clanrank FROM (SELECT sq.clan, (@num := @num +1) as clanrank FROM (SELECT mcm.cname AS clan, SUM(ms.trophies) AS trophies FROM MuxClanMembers AS mcm LEFT JOIN MuxUser AS ms ON mcm.uuid = ms.uuid JOIN (SELECT @num := 0) x WHERE mcm.invited = 0 AND mcm.cname != 'MuxTeam' GROUP BY mcm.cname ORDER BY trophies DESC) AS sq) AS sb WHERE clan = ?")) {
            rank.setString(1, clan);
            try (final ResultSet rs = rank.executeQuery()) {
                rs.last();
                if (rs.getRow() != 0) {
                    timings.stop(method);
                    return rs.getInt("clanrank");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return -1;
    }

    // []=== ENDERKISTEN ===[]
    public String loadEnderChest(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT enderchest FROM MuxEnderChest WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    timings.stop(method);
                    return rs.getString("enderchest");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
            return "error";
        }
        timings.stop(method);
        return null;
    }

    public void saveEnderChest(final UUID uuid, final String name, final String endInv) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxEnderChest SET uuid = UNHEX(?), player = ?, enderchest = ? ON DUPLICATE KEY UPDATE player = ?, enderchest = ?")) {
            set.setString(1, uuid.toString().replace("-", ""));
            set.setString(2, name);
            set.setString(3, endInv);
            set.setString(4, name);
            set.setString(5, endInv);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== BOOSTER ===[]
    public ConcurrentMap<UUID, Set<MuxBoosters.Booster>> loadBoosters() {
        final String method = getMethodName();
        final ConcurrentHashMap<UUID, Set<MuxBoosters.Booster>> boosters = new ConcurrentHashMap<>();
        try (final PreparedStatement st = conn.prepareStatement("SELECT HEX(uuid) AS uuid, boosters FROM MuxBoosters")) {
            try (final ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    final UUID uuid = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    boosters.put(uuid, gson.fromJson(rs.getString("boosters"), new TypeToken<Set<MuxBoosters.Booster>>() {
                    }.getType()));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return boosters;
    }

    public void saveBoosters(final Map<UUID, Set<MuxBoosters.Booster>> boosters) {
        final String method = getMethodName();
        if (boosters.isEmpty()) {
            try (final PreparedStatement st = conn.prepareStatement("DELETE FROM MuxBoosters")) {
                st.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        final StringBuilder sql = new StringBuilder("INSERT INTO MuxBoosters (uuid, boosters) VALUES (UNHEX(?), ?)");
        for (short i = 1; i < boosters.size(); i++) {
            sql.append(",(UNHEX(?), ?)");
        }
        sql.append(" ON DUPLICATE KEY UPDATE boosters = VALUES(boosters)");
        try (final PreparedStatement st = conn.prepareStatement(sql.toString())) {
            int count = 0;
            for (final Map.Entry<UUID, Set<MuxBoosters.Booster>> entry : boosters.entrySet()) {
                final String boosterjson = gson.toJson(entry.getValue());
                st.setString(count + 1, entry.getKey().toString().replace("-", ""));
                st.setString(count + 2, boosterjson);
                count += 2;
            }
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void removeBoosterData(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("DELETE FROM MuxBoosters WHERE uuid=UNHEX(?)")) {
            st.setString(1, uuid.toString().replace("-", ""));
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void saveBooster(final UUID uuid, final Set<MuxBoosters.Booster> boosters) {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("INSERT INTO MuxBoosters SET uuid=UNHEX(?), boosters=? ON DUPLICATE KEY UPDATE boosters=?")) {
            final String booster = gson.toJson(boosters);
            st.setString(1, uuid.toString().replace("-", ""));
            st.setString(2, booster);
            st.setString(3, booster);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== CHESTS ===[]
    public MuxChestUser loadChests(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT chests, daily FROM MuxChests WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.last()) {
                    timings.stop(method);
                    return new MuxChestUser(uuid, gson.fromJson(rs.getString("chests"), new TypeToken<CopyOnWriteArrayList<MuxChests.ChestType>>() {
                    }.getType()),
                            rs.getBoolean("daily"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new MuxChestUser(uuid, new CopyOnWriteArrayList<>(), false);
    }

    public void saveChests(final MuxChestUser u) {
        final String method = getMethodName();
        if (u == null || u.hasChanged() == false) {
            timings.stop(method);
            return;
        }
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxChests SET uuid=UNHEX(?),chests=?,daily=? ON DUPLICATE KEY UPDATE chests=?,daily=?")) {
            set.setString(1, u.getUUID().toString().replace("-", ""));
            final String chests = gson.toJson(u.getChests());
            set.setString(2, chests);
            set.setBoolean(3, u.getDaily());
            set.setString(4, chests);
            set.setBoolean(5, u.getDaily());
            set.executeUpdate();
            u.saved();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== EXTRAS ===[]
    public MuxExtraUser loadExtras(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT player, perks, extracmds, mounts, pets, emojis, enderchest FROM MuxExtras WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.last()) {
                    return new MuxExtraUser(uuid, rs.getString("player"),
                            gson.fromJson(rs.getString("perks"), new TypeToken<PerkStore>() {
                            }.getType()),
                            gson.fromJson(rs.getString("mounts"), new TypeToken<MuxMounts.MountStore>() {
                            }.getType()),
                            gson.fromJson(rs.getString("pets"), new TypeToken<MuxPets.PetStore>() {
                            }.getType()),
                            gson.fromJson(rs.getString("extracmds"), new TypeToken<HashSet<String>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("emojis"), new TypeToken<HashSet<Short>>() {
                            }.getType()),
                            (byte) rs.getShort("enderchest"));
                } else return null;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public void saveExtras(final MuxExtraUser u) {
        final String method = getMethodName();
        if (u == null || u.hasChanged() == false) {
            timings.stop(method);
            return;
        }
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxExtras SET uuid=UNHEX(?),player=?,perks=?,extracmds=?,mounts=?,pets=?,emojis=?,enderchest=? ON DUPLICATE KEY UPDATE player=?,perks=?,extracmds=?,mounts=?,pets=?,emojis=?,enderchest=?")) {
            set.setString(1, u.getUUID().toString().replace("-", ""));
            set.setString(2, u.getName());
            final String perks = gson.toJson(u.getPerks()), extracmds = gson.toJson(u.getExtraCMDs()), mounts = gson.toJson(u.getMounts()), pets = gson.toJson(u.getPets()),
                    emojis = gson.toJson(u.getEmojis());
            set.setString(3, perks);
            set.setString(4, extracmds);
            set.setString(5, mounts);
            set.setString(6, pets);
            set.setString(7, emojis);
            set.setInt(8, u.getEnderChest());
            set.setString(9, u.getName());
            set.setString(10, perks);
            set.setString(11, extracmds);
            set.setString(12, mounts);
            set.setString(13, pets);
            set.setString(14, emojis);
            set.setInt(15, u.getEnderChest());
            u.saved();
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== GIFTCODES ===[]
    public void loadGiftCodes(final MuxGiftCodes gc) {
        final String method = getMethodName();
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(MuxGiftCodes.GiftCode.class, gc.new GiftDeserializer());
        final Gson gsonbuilder = builder.create();
        final MuxGiftCodes giftcodes = ms.getGiftCodes();
        giftcodes.setGiftcodes(new HashMap<>());
        try (final PreparedStatement st = conn.prepareStatement("SELECT * FROM MuxGiftCodes")) {
            try (final ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    final MuxGiftCodes.GiftCode giftcode = gsonbuilder.fromJson(rs.getString("giftcode"), new TypeToken<MuxGiftCodes.GiftCode>() {
                    }.getType());
                    giftcodes.getGiftcodes().put(rs.getString("code"), giftcode);
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void deleteGiftCode(final String code) {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("DELETE FROM MuxGiftCodes WHERE code = ?")) {
            st.setString(1, code);
            st.execute();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void saveGiftCodes(final Map<String, MuxGiftCodes.GiftCode> giftcodes) {
        final String method = getMethodName();
        if (ms.getGiftCodes().getGiftcodes().isEmpty()) {
            try (final PreparedStatement st = conn.prepareStatement("DELETE FROM MuxGiftCodes")) {
                st.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        final StringBuilder sql = new StringBuilder("INSERT INTO MuxGiftCodes (code, giftcode) VALUES (?, ?)");
        for (short i = 1; i < giftcodes.size(); i++) {
            sql.append(",(?, ?)");
        }
        sql.append(" ON DUPLICATE KEY UPDATE giftcode = VALUES(giftcode)");
        try (final PreparedStatement st = conn.prepareStatement(sql.toString())) {
            int count = 0;
            for (final Map.Entry<String, MuxGiftCodes.GiftCode> entry : giftcodes.entrySet()) {
                final String serializedGiftCode = gson.toJson(entry.getValue());
                st.setString(count + 1, entry.getKey());
                st.setString(count + 2, serializedGiftCode);
                count += 2;
            }
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== ANALYTICS ===[]
    public Analytics loadAnalytics(final int dayofyear) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT newplayers,joinedplayers,npcs,warps,commands,chestopens,chestplayers,ackicks,acbans,eventcoins,playedcasinogames,totalgambledchips,eventplayers,casinoplayers,playedonevsonegames,onevsoneplayers,spentgems,affiliate,affiliatemoney,baseplayers,gemshop,gemshopcategories,basegriefed FROM MuxAnalytics WHERE dayofyear = ?")) {
            sel.setInt(1, dayofyear);
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.last()) {
                    timings.stop(method);
                    return new Analytics(dayofyear, rs.getLong("eventcoins"),
                            gson.fromJson(rs.getString("playedcasinogames"), new TypeToken<TreeMap<String, Integer>>() {
                            }.getType()), rs.getInt("playedonevsonegames"), rs.getInt("affiliate"), rs.getInt("affiliatemoney"), rs.getLong("totalgambledchips"),
                            rs.getLong("spentgems"), gson.fromJson(rs.getString("newplayers"), new TypeToken<Set<UUID>>() {
                    }.getType()),
                            gson.fromJson(rs.getString("gemshop"), new TypeToken<HashSet<UUID>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("eventplayers"), new TypeToken<HashSet<UUID>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("casinoplayers"), new TypeToken<HashSet<UUID>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("onevsoneplayers"), new TypeToken<HashSet<UUID>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("baseplayers"), new TypeToken<HashSet<UUID>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("joinedplayers"), new TypeToken<TreeMap<UUID, Long>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("npcs"), new TypeToken<TreeMap<String, Integer>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("warps"), new TypeToken<TreeMap<String, Integer>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("commands"), new TypeToken<TreeMap<String, Integer>>() {
                            }.getType()),
                            rs.getInt("chestopens"), gson.fromJson(rs.getString("chestplayers"), new TypeToken<HashSet<UUID>>() {
                    }.getType()),
                            gson.fromJson(rs.getString("ackicks"), new TypeToken<TreeMap<String, Integer>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("acbans"), new TypeToken<TreeMap<String, Integer>>() {
                            }.getType()),
                            gson.fromJson(rs.getString("gemshopcategories"), new TypeToken<TreeMap<String, Long>>() {
                            }.getType()),
                            rs.getInt("basegriefed"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new Analytics(dayofyear, 0, new TreeMap<>(), 0, 0, 0, 0, 0, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new TreeMap<>(), new TreeMap<>(),
                new TreeMap<>(), new TreeMap<>(), 0, new HashSet<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), 0);
    }

    public void saveAnalytics(final Analytics a) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxAnalytics SET dayofyear=?,newplayers=?,joinedplayers=?,npcs=?,warps=?,commands=?,chestopens=?,chestplayers=?,ackicks=?,acbans=?,eventcoins=?,playedcasinogames=?,totalgambledchips=?,eventplayers=?,casinoplayers=?,playedonevsonegames=?,onevsoneplayers=?,spentgems=?,affiliate=?,affiliatemoney=?,baseplayers=?,gemshop=?,gemshopcategories=?,basegriefed=? " +
                "ON DUPLICATE KEY UPDATE newplayers=?,joinedplayers=?,npcs=?,warps=?,commands=?,chestopens=?,chestplayers=?,ackicks=?,acbans=?,eventcoins=?,playedcasinogames=?,totalgambledchips=?,eventplayers=?,casinoplayers=?,playedonevsonegames=?,onevsoneplayers=?,spentgems=?,affiliate=?,affiliatemoney=?,baseplayers=?,gemshop=?,gemshopcategories=?,basegriefed=?")) {
            set.setInt(1, a.getDayOfYear());
            final String newplayers = gson.toJson(a.getNewPlayers()), joinedplayers = gson.toJson(a.getPlayers()), npcs = gson.toJson(a.getNPCs()), warps = gson.toJson(a.getWarps()),
                    commands = gson.toJson(a.getCommands()), ackicks = gson.toJson(a.getACKicks()),
                    acbans = gson.toJson(a.getACBans()), eventplayers = gson.toJson(a.getEventPlayers()), casinoplayers = gson.toJson(a.getCasinoPlayers()), onevsoneplayers = gson.toJson(a.getOnevsOnePlayers()),
                    baseplayers = gson.toJson(a.getBasePlayers()), playedcasinogames = gson.toJson(a.getPlayedCasinoGames()), chestplayers = gson.toJson(a.getChestPlayers()), gemshop = gson.toJson(a.getGemShop()), gemshopcategories = gson.toJson(a.getGemShopCategories());
            set.setString(2, newplayers);
            set.setString(3, joinedplayers);
            set.setString(4, npcs);
            set.setString(5, warps);
            set.setString(6, commands);
            set.setInt(7, a.getChestOpens());
            set.setString(8, chestplayers);
            set.setString(9, ackicks);
            set.setString(10, acbans);
            set.setLong(11, a.getEventcoins());
            set.setString(12, playedcasinogames);
            set.setLong(13, a.getGambledChips());
            set.setString(14, eventplayers);
            set.setString(15, casinoplayers);
            set.setInt(16, a.getPlayedOnevsOneGames());
            set.setString(17, onevsoneplayers);
            set.setLong(18, a.getGemsSpent());
            set.setInt(19, a.getAffiliate());
            set.setDouble(20, a.getAffiliateEarned());
            set.setString(21, baseplayers);
            set.setString(22, gemshop);
            set.setString(23, gemshopcategories);
            set.setInt(24, a.getGriefed());
            set.setString(25, newplayers);
            set.setString(26, joinedplayers);
            set.setString(27, npcs);
            set.setString(28, warps);
            set.setString(29, commands);
            set.setInt(30, a.getChestOpens());
            set.setString(31, chestplayers);
            set.setString(32, ackicks);
            set.setString(33, acbans);
            set.setLong(34, a.getEventcoins());
            set.setString(35, playedcasinogames);
            set.setLong(36, a.getGambledChips());
            set.setString(37, eventplayers);
            set.setString(38, casinoplayers);
            set.setInt(39, a.getPlayedOnevsOneGames());
            set.setString(40, onevsoneplayers);
            set.setLong(41, a.getGemsSpent());
            set.setInt(42, a.getAffiliate());
            set.setDouble(43, a.getAffiliateEarned());
            set.setString(44, baseplayers);
            set.setString(45, gemshop);
            set.setString(46, gemshopcategories);
            set.setInt(47, a.getGriefed());
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== EVENT ===[]
    public SearchEventUser loadSearchEventUser(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT found, coinsfound FROM MuxEventSearch WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.last()) {
                    return new SearchEventUser(uuid, gson.fromJson(rs.getString("found"), new TypeToken<ArrayList<Short>>() {
                    }.getType()), gson.fromJson(rs.getString("coinsfound"), new TypeToken<ArrayList<Short>>() {
                    }.getType()));
                } else return null;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
            timings.stop(method);
            return null;
        }
    }

    public void saveSearchEventUser(final SearchEventUser u) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxEventSearch SET uuid=UNHEX(?),found=?,coinsfound=? ON DUPLICATE KEY UPDATE found=?,coinsfound=?")) {
            set.setString(1, u.getUUID().toString().replace("-", ""));
            final String found = gson.toJson(u.getFound()), coinsfound = gson.toJson(u.getCoinsFound());
            set.setString(2, found);
            set.setString(3, coinsfound);
            set.setString(4, found);
            set.setString(5, coinsfound);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void clearSearchEventUsers() {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("DELETE FROM MuxEventSearch")) {
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void addEventCoins(final String event, final int coins) {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("INSERT INTO MuxEventStats SET event = ?, coins = ? ON DUPLICATE KEY UPDATE coins = coins + ?")) {
            st.setString(1, event);
            st.setLong(2, coins);
            st.setLong(3, coins);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public long getEventCoins(final String event) {
        final String method = getMethodName();
        long coins = 0;
        try (final PreparedStatement st = conn.prepareStatement("SELECT coins FROM MuxEventStats WHERE event = ?")) {
            st.setString(1, event);
            try (final ResultSet rs = st.executeQuery()) {
                if (rs.next()) coins = rs.getLong("coins");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return coins;
    }

    // []=== GAMES ===[]
    public int getTrainingLevel(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT training FROM MuxGames WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.last()) {
                    timings.stop(method);
                    return rs.getInt("training");
                }
                return 0;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return 0;
    }

    public void setTrainingLevel(final UUID uuid, final int pluslevel, final int max) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxGames SET uuid=UNHEX(?),training=1 + ? ON DUPLICATE KEY UPDATE training = LEAST(GREATEST(training + ?, 0), ?)")) {
            set.setString(1, uuid.toString().replace("-", ""));
            set.setInt(2, pluslevel);
            set.setInt(3, pluslevel);
            set.setInt(4, max);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== POLLS ===[]
    public void addPollHistory(final String question, final Map<String, Short> options) {
        final String method = getMethodName();
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        final String time = sdf.format(new Date());
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxPolls (question,time,options,server) VALUES(?,?,?,UNHEX(?))")) {
            set.setString(1, question);
            set.setString(2, time);
            set.setString(3, gson.toJson(options));
            set.setString(4, ms.getServerId().toString().replace("-", ""));
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public List<Object[]> getPollHistory(final int limit) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT id, question, time, options, HEX(server) AS server FROM MuxPolls ORDER BY id DESC LIMIT ?,500")) {
            sel.setInt(1, limit - 500);
            try (final ResultSet rs = sel.executeQuery()) {
                final LinkedList<Object[]> ret = new LinkedList<>();
                while (rs.next()) {
                    final String server = rs.getString("server");
                    ret.add(new Object[]{rs.getInt("id"), rs.getString("question"), rs.getString("time"), gson.fromJson(rs.getString("options"), new TypeToken<LinkedHashMap<String, Short>>() {
                    }.getType()), server == null || server.isEmpty() ? null : UUID.fromString(rs.getString("server").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"))});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    // []=== CONTACT ===[]
    public List<Object[]> getContactMessages(final boolean archived) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT * FROM MuxContact WHERE subject != 'MuxBewerbung' AND subject != 'Feedback' AND done = ? ORDER BY id DESC LIMIT 50")) {
            sel.setInt(1, archived ? 1 : 0);
            try (final ResultSet rs = sel.executeQuery()) {
                final LinkedList<Object[]> ret = new LinkedList<>();
                while (rs.next()) {
                    ret.add(new Object[]{rs.getString("name"), rs.getString("email"), rs.getLong("time"), rs.getString("subject"), rs.getString("message"), rs.getInt("id")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new LinkedList<>();
    }

    public String getLastContactEmail(final String name) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT email FROM MuxContact WHERE name = ? ORDER BY time DESC LIMIT 1")) {
            sel.setString(1, name);
            try (final ResultSet rs = sel.executeQuery()) {
                timings.stop(method);
                if (rs.next())
                    return rs.getString("email");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public void setStaffDone(final String name, final boolean done) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("UPDATE MuxContact SET done = ? WHERE name = ? AND subject = 'MuxBewerbung'")) {
            set.setInt(1, done ? 1 : 0);
            set.setString(2, name == null ? "" : name);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void setFeedbackDone(final int id, final boolean done) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("UPDATE MuxContact SET done = ? WHERE id = ? AND subject = 'Feedback'")) {
            set.setInt(1, done ? 1 : 0);
            set.setInt(2, id);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void setContactDone(final int id, final boolean done) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("UPDATE MuxContact SET done = ? WHERE id = ? AND subject != 'MuxBewerbung' AND subject != 'Feedback'")) {
            set.setInt(1, done ? 1 : 0);
            set.setInt(2, id);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void addFeedback(final String name, final String message) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxContact SET name = ?, subject = 'Feedback', message = ?, done = '0', time = ?, email = 'NULLSTR'")) {
            set.setString(1, name);
            set.setString(2, message);
            set.setLong(3, System.currentTimeMillis());
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public List<Object[]> getFeedback(final boolean archived) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT * FROM MuxContact WHERE subject = 'Feedback' AND done = ? ORDER BY id DESC LIMIT 150")) {
            sel.setInt(1, archived ? 1 : 0);
            try (final ResultSet rs = sel.executeQuery()) {
                final LinkedList<Object[]> ret = new LinkedList<>();
                while (rs.next()) {
                    ret.add(new Object[]{rs.getString("name"), rs.getString("email"), rs.getLong("time"), rs.getString("subject"), rs.getString("message"), rs.getInt("id")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public List<Object[]> getStaffApplications(final boolean archived) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT * FROM MuxContact WHERE subject = 'MuxBewerbung' AND done = ? ORDER BY id DESC")) {
            sel.setInt(1, archived ? 1 : 0);
            try (final ResultSet rs = sel.executeQuery()) {
                final LinkedList<Object[]> ret = new LinkedList<>();
                while (rs.next()) {
                    ret.add(new Object[]{rs.getString("name"), rs.getString("email"), rs.getLong("time"), rs.getString("subject"), rs.getString("message")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public List<Object[]> getStaffApplications(final String pname) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT * FROM MuxContact WHERE subject = 'MuxBewerbung' AND name = ? ORDER BY id DESC")) {
            sel.setString(1, pname);
            try (final ResultSet rs = sel.executeQuery()) {
                final LinkedList<Object[]> ret = new LinkedList<>();
                while (rs.next()) {
                    ret.add(new Object[]{rs.getString("name"), rs.getString("email"), rs.getLong("time"), rs.getString("subject"), rs.getString("message")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    // []=== HISTORY ===[]
    public void addPvPHistory(final UUID uuid, final UUID rival, final int trophies, final String type) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxHistory (uuid,type,victim,entry,param,timestamp) VALUES(UNHEX(?),?,UNHEX(?),?,?,?)")) {
            set.setString(1, uuid.toString().replace("-", ""));
            set.setString(2, "PVP");
            set.setString(3, rival.toString().replace("-", ""));
            set.setInt(4, trophies);
            set.setString(5, type);
            set.setLong(6, System.currentTimeMillis());
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        // Delete the "DEATH" entry if it exists within a recent time range
        try (final PreparedStatement delete = sharedConn.prepareStatement("DELETE FROM MuxHistory WHERE uuid = UNHEX(?) AND type = ? AND ABS(timestamp - ?) < ?")) {
            delete.setString(1, uuid.toString().replace("-", ""));
            delete.setString(2, "DEATH");
            delete.setLong(3, System.currentTimeMillis());
            delete.setLong(4, 200L);
            delete.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }


    public void saveHistory(final List<MuxHistory.CachedHistory> history) {
        final String method = getMethodName();
        if (history.isEmpty()) {
            timings.stop(method);
            return;
        }
        final ImmutableList<MuxHistory.CachedHistory> list = ImmutableList.copyOf(history);
        final StringBuilder sql = new StringBuilder("INSERT INTO MuxHistory (uuid,type,victim,entry,param,timestamp,server) VALUES (UNHEX(?),?,UNHEX(?),?,?,?,UNHEX(?))");
        for (short i = 1; i < list.size(); i++) {
            sql.append(",(UNHEX(?),?,UNHEX(?),?,?,?,UNHEX(?))");
        }
        final Map<UUID, Set<String>> listdel = new HashMap<>();
        try (final PreparedStatement set = sharedLongConn.prepareStatement(sql.toString())) {
            int count = 0;
            for (final MuxHistory.CachedHistory ch : list) {
                final UUID uuid = ch.getUUID(), uuid2 = ch.getUUID2();
                final String type = ch.getType(), entry = ch.getEntry(), param = ch.getParam();
                history.remove(ch);
                if (type.equals("PAYMENT") == false) {
                    final Set<String> deltypes = listdel.getOrDefault(uuid, new HashSet<>());
                    if (deltypes.isEmpty()) listdel.put(uuid, deltypes);
                    deltypes.add(type);
                }
                set.setString(count + 1, uuid.toString().replace("-", ""));
                set.setString(count + 2, type);
                set.setString(count + 3, (uuid2 == null ? null : uuid2.toString().replace("-", "")));
                set.setString(count + 4, entry);
                set.setString(count + 5, param);
                set.setLong(count + 6, ch.getTimestamp());
                set.setString(count + 7, ms.getServerId().toString().replace("-", ""));
                count += 7;
            }
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        try (final PreparedStatement del = sharedLongConn.prepareStatement("DELETE FROM MuxHistory WHERE id NOT IN (SELECT * FROM (SELECT id FROM MuxHistory WHERE uuid = UNHEX(?) AND type = ? ORDER BY id DESC LIMIT 200) AS t) AND uuid = UNHEX(?) AND type = ?")) {
            for (final Map.Entry<UUID, Set<String>> entry : listdel.entrySet()) {
                final UUID uuid = entry.getKey();
                final Set<String> types = entry.getValue();
                try {
                    for (final String type : types) {
                        del.setString(1, uuid.toString().replace("-", ""));
                        del.setString(2, type);
                        del.setString(3, uuid.toString().replace("-", ""));
                        del.setString(4, type);
                        del.executeUpdate();
                        del.clearParameters();
                    }
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public List<String> getLastPayments(final int limit) {
        final String method = getMethodName();
        final List<String> results = new ArrayList<>();
        try (final PreparedStatement st = sharedConn.prepareStatement("SELECT HEX(uuid) as uuid FROM MuxHistory WHERE type = 'PAYMENT' AND NOT entry = 'USER TRANSFER' ORDER BY timestamp DESC LIMIT ?")) {
            st.setInt(1, limit);
            try (final ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    final UUID uuid = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    results.add(ms.getShared().getNameFromUUID(uuid));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return results;
    }

    public long getLastPaymentTime(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("SELECT timestamp FROM MuxHistory WHERE uuid = UNHEX(?) AND type = 'PAYMENT' ORDER BY timestamp DESC LIMIT 1")) {
            st.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    timings.stop(method);
                    return rs.getLong("timestamp");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return -1;
    }

    public boolean deleteHistoryEntry(final int id) {
        final String method = getMethodName();
        try (final PreparedStatement del = sharedConn.prepareStatement("DELETE FROM MuxHistory WHERE id = ?")) {
            del.setInt(1, id);
            final int affectedRows = del.executeUpdate();
            timings.stop(method);
            return affectedRows > 0;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }


    public List<Object[]> getHistory(final UUID uuid, final String type, final int limit, boolean serverBound) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(victim) AS victim, entry, param, timestamp, id FROM MuxHistory WHERE uuid = UNHEX(?) AND type = ? " + (serverBound ? "AND server = UNHEX(?) " : "") + "ORDER BY timestamp DESC LIMIT ?")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            sel.setString(2, type);
            if (serverBound) {
                sel.setString(3, ms.getServerId().toString().replace("-", ""));
            }
            sel.setInt(serverBound ? 4 : 3, limit);

            try (final ResultSet rs = sel.executeQuery()) {
                final LinkedList<Object[]> ret = new LinkedList<>();
                while (rs.next()) {
                    ret.add(new Object[]{rs.getString("victim") != null ? UUID.fromString(rs.getString("victim").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")) : null, rs.getString("entry"), rs.getString("param"), rs.getLong("timestamp"), type, rs.getInt("id")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public List<Object[]> getGeneralHistory(final String type, final int limit) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(victim) AS victim, HEX(uuid) AS uuid, entry, param, timestamp, id FROM MuxHistory WHERE type = ? AND server = UNHEX(?) ORDER BY id DESC LIMIT ?")) {
            sel.setString(1, type);
            sel.setString(2, ms.getServerId().toString().replace("-", ""));
            sel.setInt(3, limit);
            try (final ResultSet rs = sel.executeQuery()) {
                final LinkedList<Object[]> ret = new LinkedList<>();
                while (rs.next()) {
                    ret.add(new Object[]{rs.getString("victim") != null ? UUID.fromString(rs.getString("victim").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")) : null, rs.getString("entry"), rs.getString("param"), rs.getLong("timestamp"),
                            UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), rs.getInt("id")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public List<Object[]> getGeneralHistory(final String type, final int limit, final String searchTerm) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(victim) AS victim, HEX(uuid) AS uuid, entry, param, timestamp, id FROM MuxHistory WHERE type = ? AND server = UNHEX(?) AND (HEX(uuid) LIKE ? OR HEX(victim) LIKE ? OR entry LIKE ? OR param LIKE ?) ORDER BY id DESC LIMIT ?")) {
            sel.setString(1, type);
            sel.setString(2, ms.getServerId().toString().replace("-", ""));

            // Check if searchTerm is a username
            final OfflinePlayer op = ms.getPlayer(searchTerm);
            final UUID uuid = (op == null ? null : op.getUniqueId());
            if (uuid != null) {
                final String suuid = uuid.toString().replace("-", "");
                sel.setString(3, suuid + "%");
                sel.setString(4, suuid + "%");
            } else {
                sel.setString(3, "NOMATCH");
                sel.setString(4, "NOMATCH");
            }
            sel.setString(5, "%" + searchTerm + "%");
            sel.setString(6, "%" + searchTerm + "%");
            sel.setInt(7, limit);
            try (final ResultSet rs = sel.executeQuery()) {
                final LinkedList<Object[]> ret = new LinkedList<>();
                while (rs.next()) {
                    ret.add(new Object[]{rs.getString("victim") != null ? UUID.fromString(rs.getString("victim").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")) : null, rs.getString("entry"), rs.getString("param"), rs.getLong("timestamp"),
                            UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), rs.getInt("id")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public List<Object[]> getPvPHistory(final UUID uuid, final String type, final int limit) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(victim) AS victim, entry, param FROM MuxHistory WHERE uuid = UNHEX(?) AND type = ? ORDER BY id DESC LIMIT ?")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            sel.setString(2, type);
            sel.setInt(3, limit);
            try (final ResultSet rs = sel.executeQuery()) {
                final LinkedList<Object[]> ret = new LinkedList<>();
                while (rs.next()) {
                    ret.add(new Object[]{UUID.fromString(rs.getString("victim").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), Integer.parseInt(rs.getString("entry")), rs.getString("param")});
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    // []=== PAYSAFECARDS ===[]
    public Map<String, Integer> getPaySafeCodes(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT value, code FROM MuxPaySafeCards WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            final ResultSet rs = sel.executeQuery();
            final Map<String, Integer> codes = new HashMap<>();
            while (rs.next()) {
                codes.put(rs.getString("code"), rs.getInt("value"));
            }
            return codes;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public void addPaySafeCode(final UUID uuid, final String pname, final String code, final int value) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxPaySafeCards (uuid,player,code,value) VALUES(UNHEX(?),?,?,?)")) {
            set.setString(1, uuid.toString().replace("-", ""));
            set.setString(2, pname);
            set.setString(3, code);
            set.setInt(4, value);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== CASINO ===[]
    public MuxCasinoUser loadCasino(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT " + (ms.isBeta() ? "testchips" : "chips") + " AS chips, energy, weeklywins FROM MuxCasino WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.last()) {
                    timings.stop(method);
                    return new MuxCasinoUser(uuid, rs.getLong("chips"), rs.getInt("energy"), rs.getLong("weeklywins"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new MuxCasinoUser(uuid, 0, 100, 0);
    }

    public void saveCasino(final MuxCasinoUser u) {
        final String method = getMethodName();
        if (u == null || u.hasChanged() == false) {
            timings.stop(method);
            return;
        }
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxCasino SET uuid=UNHEX(?)," + (ms.isBeta() ? "testchips" : "chips") + "=?,energy=?,weeklywins=? ON DUPLICATE KEY UPDATE " + (ms.isBeta() ? "testchips" : "chips") + "=?,energy=?,weeklywins=?")) {
            set.setString(1, u.getUUID().toString().replace("-", ""));
            set.setLong(2, u.getChips());
            set.setInt(3, u.getEnergy());
            set.setLong(4, u.getWeeklyWins());
            set.setLong(5, u.getChips());
            set.setInt(6, u.getEnergy());
            set.setLong(7, u.getWeeklyWins());
            set.executeUpdate();
            u.saved();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void resetCasino(final boolean week) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("UPDATE MuxCasino SET energy=100" + (week ? ",weeklywins=0" : ""))) {
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public List<MuxCasinoUser> getWeekly5() {
        final String method = getMethodName();
        final List<MuxCasinoUser> weekly = new ArrayList<>();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT HEX(uuid) AS uuid, weeklywins FROM MuxCasino ORDER BY weeklywins DESC LIMIT 5")) {
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    weekly.add(new MuxCasinoUser(UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), 0, 0, rs.getLong("weeklywins")));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return weekly;
    }

    public List<MuxCasinoUser> getTop5() {
        final String method = getMethodName();
        final List<MuxCasinoUser> top = new ArrayList<>();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT HEX(uuid) AS uuid, " + (ms.isBeta() ? "testchips" : "chips") + " AS chips FROM MuxCasino ORDER BY " + (ms.isBeta() ? "testchips" : "chips") + " DESC LIMIT 5")) {
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    top.add(new MuxCasinoUser(UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), rs.getLong("chips"), 0, 0));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return top;
    }

    public String getCasinoStats() {
        final String method = getMethodName();
        String str = "";
        try (final PreparedStatement st = conn.prepareStatement("SELECT stats FROM MuxCasinoStats WHERE server = ?")) {
            st.setString(1, ms.getServerName());
            try (final ResultSet rs = st.executeQuery()) {
                if (rs.next())
                    str = rs.getString("stats");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return str;
    }

    public void saveCasinoStats(final String str) {
        final String method = getMethodName();
        if (ms.getServerName() == null) {
            timings.stop(method);
            return;
        }
        try (final PreparedStatement st = conn.prepareStatement("INSERT INTO MuxCasinoStats SET server=?, stats=? ON DUPLICATE KEY UPDATE stats=?")) {
            st.setString(1, ms.getServerName());
            st.setString(2, str);
            st.setString(3, str);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== NEWBIE ===[]
    public MuxNewbies.NewbieData loadNewbieData(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT data FROM MuxNewbie WHERE uuid=UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    timings.stop(method);
                    return gson.fromJson(rs.getString("data"), new TypeToken<MuxNewbies.NewbieData>() {
                    }.getType());
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public void saveNewbieData(final UUID uuid, final MuxNewbies.NewbieData data) {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("INSERT INTO MuxNewbie SET uuid=UNHEX(?), data = ?, timestamp = ? ON DUPLICATE KEY UPDATE data = ?, timestamp = ?")) {
            st.setString(1, uuid.toString().replace("-", ""));
            final String dataStr = gson.toJson(data);
            st.setString(2, dataStr);
            st.setLong(3, data.getTimestamp());
            st.setString(4, dataStr);
            st.setLong(5, data.getTimestamp());
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void deleteOldNewbieData() {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("DELETE FROM MuxNewbie WHERE timestamp < ?")) {
            st.setLong(1, System.currentTimeMillis() - 172800000);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== TIPS ===[]
    public Map<MuxTips.Tip, Integer> loadUserTips(final UUID uuid, final MuxTips.TipUser tu) {
        final String method = getMethodName();
        final Map<MuxTips.Tip, Integer> usedTips = new EnumMap<>(MuxTips.Tip.class);
        try (final PreparedStatement st = sharedConn.prepareStatement("SELECT * FROM MuxTips WHERE uuid = UNHEX(?)")) {
            st.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    tu.setLastTipDisplay(rs.getLong("last"));
                    final String tipsJson = rs.getString("tips");
                    final Map<String, Integer> tips = gson.fromJson(tipsJson, new TypeToken<Map<String, Integer>>() {
                    }.getType());
                    for (final Map.Entry<String, Integer> entry : tips.entrySet()) {
                        final String tipName = entry.getKey();
                        if (Arrays.stream(MuxTips.Tip.values()).anyMatch(tip -> tip.name().equalsIgnoreCase(tipName))) {
                            usedTips.put(MuxTips.Tip.valueOf(tipName), entry.getValue());
                        }
                    }
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return usedTips;
    }

    public void saveUserTips(final UUID uuid, final Map<MuxTips.Tip, Integer> usedTips, long last) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO MuxTips SET uuid=UNHEX(?), tips=?, last=? ON DUPLICATE KEY UPDATE tips=?, last=?")) {
            st.setString(1, uuid.toString().replace("-", ""));
            final String tips = gson.toJson(usedTips);
            st.setString(2, tips);
            st.setLong(3, last);
            st.setString(4, tips);
            st.setLong(5, last);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== PERMISSIONS ===[]
    public void loadMuxRanks(final MuxRanks perms) {
        final String method = getMethodName();
        try (final PreparedStatement select = sharedConn.prepareStatement("SELECT HEX(uuid) AS uuid, `rank`, teamrank, extraperms, rankexpiredata FROM MuxRanks")) {
            try (final ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    final UUID uuid = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    final String rank = rs.getString("rank"), extraperms = rs.getString("extraperms"), teamrank = rs.getString("teamrank");
                    final MuxRanks.RankExpireData expireData = rs.getString("rankexpiredata") == null ? null : gson.fromJson(rs.getString("rankexpiredata"), new TypeToken<MuxRanks.RankExpireData>() {
                    }.getType());
                    final MuxRanks.PermissionsUser pu = perms.addUser(uuid, rank, teamrank, expireData);
                    final JSONArray array = new JSONArray(extraperms);
                    final List<String> list = new ArrayList<>();
                    for (final Object object : array) {
                        if (object instanceof String) {
                            list.add((String) object);
                        }
                    }
                    pu.addPermissions(list);
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void savePermissionsUser(final MuxRanks.PermissionsUser user) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("INSERT INTO MuxRanks SET uuid=UNHEX(?),`rank`=?,extraperms=?,teamrank=?,rankexpiredata=? ON DUPLICATE KEY UPDATE `rank`=?,extraperms=?,teamrank=?,rankexpiredata=?")) {
            set.setString(1, user.getUUID().toString().replace("-", ""));
            set.setString(2, user.getGroup());
            final JSONArray array = new JSONArray();
            final String expireData = user.getExpireData() == null ? null : gson.toJson(user.getExpireData());
            user.getPermissions().forEach(array::put);
            final String arrayAsString = array.toString().length() == 0 ? null : array.toString();
            set.setString(3, arrayAsString);
            set.setString(4, user.getTeamGroup());
            set.setString(5, expireData);
            set.setString(6, user.getGroup());
            set.setString(7, arrayAsString);
            set.setString(8, user.getTeamGroup());
            set.setString(9, expireData);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void deletePermissionsUser(final MuxRanks.PermissionsUser user) {
        final String method = getMethodName();
        if (user == null) {
            timings.stop(method);
            return;
        }
        try (final PreparedStatement delete = sharedConn.prepareStatement("DELETE FROM MuxRanks WHERE uuid = UNHEX(?)")) {
            delete.setString(1, user.getUUID().toString().replace("-", ""));
            delete.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== ACTIVE MONEY SUPPLY ===[]
    public long getTotalActiveCoins() {
        final String method = getMethodName();
        long value = 10000000;
        try (final PreparedStatement select = longconn.prepareStatement("SELECT SUM(" + (ms.isBeta() ? "testcoins" : "coins") + ") AS totalcoins FROM MuxUser WHERE login > ?")) {
            select.setLong(1, System.currentTimeMillis() - 3456000000L);
            try (final ResultSet rs = select.executeQuery()) {
                if (rs.next()) value = rs.getLong("totalcoins");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return value;
    }

    public long getTotalActiveChips() {
        final String method = getMethodName();
        long value = 10000000;
        try (final PreparedStatement select = longconn.prepareStatement("SELECT SUM(" + (ms.isBeta() ? "testchips" : "chips") + ") AS totalchips FROM MuxCasino WHERE EXISTS(SELECT 1 FROM MuxUser WHERE (login > ?) AND (MuxCasino.uuid = MuxUser.uuid))")) {
            select.setLong(1, System.currentTimeMillis() - 3456000000L);
            try (final ResultSet rs = select.executeQuery()) {
                if (rs.next()) value = rs.getLong("totalchips");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return value;
    }

    // []=== SHOP ===[]
    public List<PremiumItem> loadPremiumItems() {
        final String method = getMethodName();
        final List<PremiumItem> items = new ArrayList<>();
        try (final PreparedStatement st = conn.prepareStatement("SELECT id, HEX(uuid) AS uuid, item, price, expire FROM MuxMarket")) {
            try (final ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    final ItemStack item = ms.stringToItemStack(rs.getString("item"));
                    final int price = rs.getInt("price");
                    final long expire = rs.getLong("expire");
                    final UUID owner = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    items.add(new PremiumItem(rs.getLong("id"), owner, item, price, expire));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return items;
    }

    public boolean addPremiumItem(final UUID uuid, final PremiumItem mi) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxMarket (item, uuid, price, expire) VALUES (?,UNHEX(?),?,?)", Statement.RETURN_GENERATED_KEYS)) {
            set.setString(1, ms.itemStackToString(mi.getItem()));
            set.setString(2, uuid.toString().replace("-", ""));
            set.setInt(3, mi.getPrice());
            set.setLong(4, mi.getUpdated());
            set.executeUpdate();
            final ResultSet rs = set.getGeneratedKeys();
            long key;
            if (rs.next()) {
                key = rs.getLong(1);
                mi.setKeyId(key);
            }
            timings.stop(method);
            return true;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    public boolean removePremiumItem(final PremiumItem mi) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("DELETE FROM MuxMarket WHERE id = ?")) {
            set.setLong(1, mi.getKeyId());
            set.executeUpdate();
            timings.stop(method);
            return true;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    public void updatePremiumItem(final PremiumItem mi) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("UPDATE MuxMarket SET expire = ? WHERE id = ?")) {
            set.setLong(1, mi.getUpdated());
            set.setLong(2, mi.getKeyId());
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void deleteShopPrices(final long time) {
        final String method = getMethodName();
        try (final PreparedStatement st = longconn.prepareStatement("DELETE FROM MuxShopPrices WHERE time < ? AND item != 'GEM'")) {
            st.setLong(1, time);
            st.executeUpdate();
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
        timings.stop(method);
    }

    public void saveShopPrices(final long time, final String item, final double price, final long volume) {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("INSERT INTO MuxShopPrices SET time=?,item=?,price=?,volume=?")) {
            st.setLong(1, time);
            st.setString(2, item);
            st.setDouble(3, price);
            st.setLong(4, volume);
            st.executeUpdate();
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
        timings.stop(method);
    }

    public double getPriceAtTime(final String item, final long time) {
        final String method = getMethodName();
        double price = -1;
        try (final PreparedStatement sl = conn.prepareStatement("SELECT price, time FROM MuxShopPrices WHERE time>? AND item=? AND price>0 ORDER BY time ASC LIMIT 1")) {
            sl.setLong(1, time);
            sl.setString(2, item);
            try (final ResultSet rs = sl.executeQuery()) {
                if (rs.next()) {
                    price = rs.getDouble("price");
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
        timings.stop(method);
        return price;
    }

    public AbstractMap.SimpleEntry<Double, Long> getLastKnownPrice(final String item) {
        final String method = getMethodName();
        double price = -1;
        long t = -1;
        try (final PreparedStatement sl = conn.prepareStatement("SELECT price, time FROM MuxShopPrices WHERE item=? AND price>0 ORDER BY time DESC LIMIT 1")) {
            sl.setString(1, item);
            try (final ResultSet rs = sl.executeQuery()) {
                if (rs.next()) {
                    price = rs.getDouble("price");
                    t = rs.getLong("time");
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
        timings.stop(method);
        return new AbstractMap.SimpleEntry<>(price, t);
    }

    public Map<String, Long> get5ItemsWithMostVolume24H() {
        final String method = getMethodName();
        final Map<String, Long> m = new HashMap<>();
        try (final PreparedStatement sl = longconn.prepareStatement("SELECT item, SUM(volume) AS vol FROM MuxShopPrices WHERE time > ? AND item != 'GEM' GROUP BY item ORDER BY vol DESC LIMIT 5")) {
            sl.setLong(1, (System.currentTimeMillis() - 86400000L));
            try (final ResultSet rs = sl.executeQuery()) {
                while (rs.next()) {
                    final String item = rs.getString("item");
                    long volume = rs.getLong("vol");
                    m.put(item, m.getOrDefault(item, 0L) + volume);
                }
            }
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
        timings.stop(method);
        return m;
    }

    public long getTotalVolumeLast24Hours() {
        final String method = getMethodName();
        long volume = 0;
        try (final PreparedStatement sel = longconn.prepareStatement("SELECT SUM(volume) AS totalvolume FROM MuxShopPrices WHERE time > ?")) {
            sel.setLong(1, System.currentTimeMillis() - 86400000L);
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next())
                    volume = rs.getLong("totalvolume");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return volume;
    }

    public void soldItem(final UUID uuid, final ShopItem si) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("INSERT INTO MuxShopEarnings SET uuid = UNHEX(?), item = ?, amount = ?, earnings = ? ON DUPLICATE KEY UPDATE amount = amount + ?, earnings = earnings + ?")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            sel.setString(2, si.getFullId());
            sel.setInt(3, si.getAmount());
            sel.setDouble(4, si.getPrice());
            sel.setInt(5, si.getAmount());
            sel.setDouble(6, si.getPrice());
            sel.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public double getTotalEarnings(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT SUM(earnings) AS totalearnings FROM MuxShopEarnings WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    timings.stop(method);
                    return rs.getDouble("totalearnings");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return -1;
    }

    public List<ShopItem> getSoldItems(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT item, amount, earnings FROM MuxShopEarnings WHERE uuid = UNHEX(?) ORDER BY earnings DESC")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                final List<ShopItem> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(new ShopItem(-1, null, rs.getString("item"), rs.getDouble("earnings"), rs.getInt("amount")));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public boolean addItem(final UUID uuid, final ShopItem si) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxShop (item, uuid, amount, price) VALUES(?,UNHEX(?),?,?)", Statement.RETURN_GENERATED_KEYS)) {
            set.setString(1, si.getFullId());
            set.setString(2, uuid.toString().replace("-", ""));
            set.setInt(3, si.getAmount());
            set.setDouble(4, si.getPrice());
            set.executeUpdate();
            final ResultSet rs = set.getGeneratedKeys();
            long key;
            if (rs.next()) {
                key = rs.getLong(1);
                si.setKeyId(key);
            }
            timings.stop(method);
            return true;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    public boolean removeItem(final ShopItem si) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("DELETE FROM MuxShop WHERE id = ?")) {
            set.setLong(1, si.getKeyId());
            set.executeUpdate();
            timings.stop(method);
            return true;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    public void changePrices(final UUID uuid, final String fullid, final double price) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("UPDATE MuxShop SET price = ? WHERE item = ? AND uuid = UNHEX(?)")) {
            set.setDouble(1, price);
            set.setString(2, fullid);
            set.setString(3, uuid.toString().replace("-", ""));
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void changeItem(final ShopItem si) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("UPDATE MuxShop SET amount = ? WHERE id = ?")) {
            set.setInt(1, si.getAmount());
            set.setLong(2, si.getKeyId());
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public List<ShopItem> getShopItemsOf(final UUID uuid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT id, item, amount, price FROM MuxShop WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                final List<ShopItem> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(new ShopItem(rs.getLong("id"), null, rs.getString("item"), rs.getDouble("price"), rs.getInt("amount")));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public double getCheapestItem(final String fullid) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT price FROM MuxShop WHERE item = ? ORDER BY price ASC LIMIT 1")) {
            sel.setString(1, fullid);
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    timings.stop(method);
                    return rs.getDouble("price");
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return -1;
    }

    public List<Tuple<String, Double>> getTopItemsByAvgPriceLast12Hours(final int limit) {
        final String method = getMethodName();
        final List<Tuple<String, Double>> list = new ArrayList<>();
        try (final PreparedStatement sel = longconn.prepareStatement("SELECT item, AVG(price) as avgprice FROM MuxShopPrices WHERE item NOT IN (SELECT item FROM MuxChestsBlacklist) AND item != 'GEM' AND time > ? GROUP BY item ORDER BY avgprice DESC LIMIT ?")) {
            sel.setLong(1, System.currentTimeMillis() - 43200000L);
            sel.setInt(2, limit);
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next())
                    list.add(new Tuple<>(rs.getString("item"), rs.getDouble("avgprice")));
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return list;
    }

    public List<ShopItem> getShopItems(final String fullid, final double price) {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT id, HEX(uuid) AS uuid, amount FROM MuxShop WHERE item = ? AND price = ?")) {
            sel.setString(1, fullid);
            sel.setDouble(2, price);
            try (final ResultSet rs = sel.executeQuery()) {
                final List<ShopItem> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(new ShopItem(rs.getLong("id"), UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")),
                            fullid, price, rs.getInt("amount")));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public Map<String, Long> getShopItemsLiquidity() {
        final String method = getMethodName();
        final Map<String, Long> liquidityMap = new HashMap<>();
        try (final PreparedStatement sel = longconn.prepareStatement("SELECT item, SUM(amount) as liquidity FROM MuxShop GROUP BY item")) {
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    final String item = rs.getString("item");
                    long liquidity = rs.getLong("liquidity");
                    liquidityMap.put(item, liquidity);
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return liquidityMap;
    }

    public List<ShopItem> getBestSellers() {
        final String method = getMethodName();
        try (final PreparedStatement sel = longconn.prepareStatement("SELECT ms1.item AS i, ms1.price AS sprice FROM MuxShop AS ms1 LEFT JOIN MuxShop AS ms2 ON ms1.item = ms2.item AND ms1.price > ms2.price WHERE ms2.item IS NULL ORDER BY sprice DESC LIMIT 50")) {
            try (final ResultSet rs = sel.executeQuery()) {
                final List<ShopItem> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(new ShopItem(-1, null, rs.getString("i"), rs.getDouble("sprice"), 0));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public List<ShopItem> getTopSold() {
        final String method = getMethodName();
        try (final PreparedStatement sel = conn.prepareStatement("SELECT item, COUNT(*) AS sold, SUM(amount) AS amt FROM MuxShop GROUP BY item ORDER BY amt DESC LIMIT 5")) {
            try (final ResultSet rs = sel.executeQuery()) {
                final List<ShopItem> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(new ShopItem(-1, null, rs.getString("item"), 0, rs.getInt("amt")));
                }
                timings.stop(method);
                return ret;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return new ArrayList<>();
    }

    public Tuple<Long, Long> loadLiquidity() {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("SELECT gems, coins FROM MuxLiquidity WHERE server = ?")) {
            st.setString(1, ms.getServerName());
            try (final ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    timings.stop(method);
                    return new Tuple<>(rs.getLong("gems"), rs.getLong("coins"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return null;
    }

    public void saveLiquidity() {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("INSERT INTO MuxLiquidity SET server = ?, gems = ?, coins = ? ON DUPLICATE KEY UPDATE gems = ?, coins = ?")) {
            st.setString(1, ms.getServerName());
            final MuxGemShop gemshop = ms.getGemShop();
            st.setLong(2, gemshop.getGemLiquidity());
            st.setLong(3, gemshop.getCoinsLiquidity());
            st.setLong(4, gemshop.getGemLiquidity());
            st.setLong(5, gemshop.getCoinsLiquidity());
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== PROTECTED BLOCKS ===[]
    public void addBlock(final UUID uuid, final String world, final int x, final int y, final int z, final int id) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("INSERT INTO MuxBlocks (uuid, x, y, z, world, blockid) VALUES(UNHEX(?),?,?,?,?,?)")) {
            set.setString(1, uuid.toString().replace("-", ""));
            set.setInt(2, x);
            set.setInt(3, y);
            set.setInt(4, z);
            set.setString(5, world);
            set.setInt(6, id);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public boolean removeBlock(final UUID uuid, final String world, final int x, final int y, final int z, final int id) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("DELETE FROM MuxBlocks WHERE uuid = UNHEX(?) AND x = ? AND y = ? AND z = ? AND world = ? AND blockid = ?")) {
            set.setString(1, uuid.toString().replace("-", ""));
            set.setInt(2, x);
            set.setInt(3, y);
            set.setInt(4, z);
            set.setString(5, world);
            set.setInt(6, id);
            int r = set.executeUpdate();
            timings.stop(method);
            return r != 0;
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return false;
    }

    public void removeBlocksWithin(final String world, final int minX, final int maxX, final int minZ, final int maxZ) {
        final String method = getMethodName();
        try (final PreparedStatement set = conn.prepareStatement("DELETE FROM MuxBlocks WHERE world = ? AND x <= ? AND x >= ? AND z <= ? AND z >= ?")) {
            set.setString(1, world);
            set.setInt(2, maxX);
            set.setInt(3, minX);
            set.setInt(4, maxZ);
            set.setInt(5, minZ);
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== BASE ===[]
    public void loadBases(final MuxBase base) {
        final String method = getMethodName();
        try (final PreparedStatement select = conn.prepareStatement("SELECT HEX(uuid) as uuid, size, trusted, spawn, minpos, maxpos, mid, grief, griefed, blocks, blocksLR, lastplayed, griefable FROM MuxBase")) {
            try (final ResultSet rs = select.executeQuery()) {
                final MuxShop shop = ms.getShop();
                while (rs.next()) {
                    final UUID uuid = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    final PlayerBase playerBase = base.addBase(uuid, rs.getInt("size"), ms.stringToLocation("MuxBase:" + rs.getString("mid")),
                            ms.stringToLocation("MuxBase:" + rs.getString("minpos")), ms.stringToLocation("MuxBase:" + rs.getString("maxpos")));
                    final JSONArray array = new JSONArray(rs.getString("trusted"));
                    for (final Object object : array) {
                        if (object instanceof String) {
                            final String s = (String) object;
                            playerBase.addTrusted(UUID.fromString(s.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
                        }
                    }
                    playerBase.setGriefed(rs.getBoolean("griefed"));
                    playerBase.setGriefable(rs.getBoolean("griefable"));
                    playerBase.setGrief(rs.getLong("grief"));
                    playerBase.setSpawn(ms.stringToLocation("MuxBase:" + rs.getString("spawn")));
                    playerBase.getBlocks().putAll(gson.fromJson(rs.getString("blocks"), new TypeToken<HashMap<String, Long>>() {
                    }.getType()));
                    playerBase.getBlocksLR().putAll(gson.fromJson(rs.getString("blocksLR"), new TypeToken<HashMap<String, Long>>() {
                    }.getType()));
                    playerBase.setLastPlayed(rs.getLong("lastplayed"));
                    final AtomicDouble value = new AtomicDouble(0);
                    if (playerBase.getBlocks().isEmpty() == false) {
                        playerBase.getBlocks().forEach((s, aLong) -> {
                            final double cheapest = shop.getCheapestPrice(s, false);
                            value.addAndGet((cheapest < 0 ? 0 : cheapest) * aLong);
                        });
                        playerBase.setValue(value.get());
                    }
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void saveBase(final PlayerBase base) {
        final String method = getMethodName();
        try (final PreparedStatement st = conn.prepareStatement("INSERT INTO MuxBase SET uuid=UNHEX(?),size=?,trusted=?,mid=?,spawn=?,minpos=?,maxpos=?,grief=?,griefed=?,blocks=?,blocksLR=?,lastplayed=?,griefable=? ON DUPLICATE KEY UPDATE size=?,trusted=?,mid=?,spawn=?,minpos=?,maxpos=?,grief=?,griefed=?,blocks=?,blocksLR=?,lastplayed=?,griefable=?")) {
            st.setString(1, base.getOwner().toString().replace("-", ""));
            st.setInt(2, base.getSize());
            final JSONArray trusted = new JSONArray();
            for (final UUID uuid : base.getTrusted()) {
                trusted.put(uuid.toString().replace("-", ""));
            }
            st.setString(3, trusted.toString());
            st.setString(4, ms.locationToString(base.getLocation(), false));
            st.setString(5, ms.locationToString(base.getSpawn(), false));
            st.setString(6, ms.locationToString(base.getMinPos(), false));
            st.setString(7, ms.locationToString(base.getMaxPos(), false));
            st.setLong(8, base.getGrief());
            st.setInt(9, base.isGriefed() ? 1 : 0);
            st.setString(10, gson.toJson(base.getBlocks()));
            st.setString(11, gson.toJson(base.getBlocksLR()));
            st.setLong(12, base.getLastPlayed());
            st.setInt(13, base.isGriefable() ? 1 : 0);
            st.setInt(14, base.getSize());
            st.setString(15, trusted.toString());
            st.setString(16, ms.locationToString(base.getLocation(), false));
            st.setString(17, ms.locationToString(base.getSpawn(), false));
            st.setString(18, ms.locationToString(base.getMinPos(), false));
            st.setString(19, ms.locationToString(base.getMaxPos(), false));
            st.setLong(20, base.getGrief());
            st.setInt(21, base.isGriefed() ? 1 : 0);
            st.setString(22, gson.toJson(base.getBlocks()));
            st.setString(23, gson.toJson(base.getBlocksLR()));
            st.setLong(24, base.getLastPlayed());
            st.setInt(25, base.isGriefable() ? 1 : 0);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== PAYMENT ===[]
    public void changePaymentStatus(final UUID uuid, final long paymentTimestamp, final String newStatus) {
        final String method = getMethodName();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT param FROM MuxHistory WHERE uuid = UNHEX(?) AND timestamp = ? AND type = 'PAYMENT'")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            sel.setLong(2, paymentTimestamp);
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    final String param = rs.getString("param"), priceStr = param.split(" ")[0];
                    try (final PreparedStatement st = sharedConn.prepareStatement("UPDATE MuxHistory SET param = ? WHERE uuid = UNHEX(?) AND timestamp = ? AND type = 'PAYMENT'")) {
                        st.setString(1, priceStr + " " + newStatus);
                        st.setString(2, uuid.toString().replace("-", ""));
                        st.setLong(3, paymentTimestamp);
                        st.executeUpdate();
                    }
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public void changePaymentStatus(final UUID uuid, final String transactionID, final String newStatus) {
        final String method = getMethodName();
        try (final PreparedStatement set = sharedConn.prepareStatement("UPDATE MuxHistory SET param = REPLACE(param, ' 0 ', ?) WHERE uuid = UNHEX(?) AND param LIKE ? AND type = 'PAYMENT'")) {
            set.setString(1, " " + newStatus + " ");
            set.setString(2, uuid.toString().replace("-", ""));
            set.setString(3, "%" + transactionID + "%");
            set.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    // []=== AFFILIATE ===[]
    public boolean isRegisteredAffiliateUser(final UUID uuid) {
        final String method = getMethodName();
        boolean result = false;
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT player FROM MuxAffiliateCashout WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) result = true;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return result;
    }

    public boolean isRecruited(final UUID uuid) {
        final String method = getMethodName();
        boolean result = false;
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT recruiter FROM MuxAffiliate WHERE recruited = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) result = true;
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return result;
    }

    public void saveRecruited(final MuxAffiliate.RecruitedInformations informations) {
        this.saveRecruited(informations.getUUID(), informations.getRecruitedBy(), informations.getStatus(), ms.getAffiliate().getGson().toJson(informations.getStoreTransactions()));
    }

    public void saveRecruited(final UUID recruited, final UUID recruiter, final long status, final String payments) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO MuxAffiliate SET recruiter = UNHEX(?), recruited = UNHEX(?), status = ?, timestamp = ?, payments = ? ON DUPLICATE KEY UPDATE status = ?, payments = ?")) {
            st.setString(1, recruiter.toString().replace("-", ""));
            st.setString(2, recruited.toString().replace("-", ""));
            st.setLong(3, status);
            st.setLong(4, System.currentTimeMillis());
            st.setString(5, payments);
            st.setLong(6, status);
            st.setString(7, payments);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public int getRecruitedByRecruiter(final UUID recruiter) {
        final String method = getMethodName();
        int count = 0;
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT COUNT(*) AS recruitedcount FROM MuxAffiliate WHERE recruiter = UNHEX(?)")) {
            sel.setString(1, recruiter.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) count = rs.getInt("recruitedcount");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return count;
    }

    public int getValidRecruitedByRecruiter(final UUID recruiter) {
        final String method = getMethodName();
        int count = 0;
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT COUNT(*) AS recruitedcount FROM MuxAffiliate WHERE recruiter = UNHEX(?) AND status != '-2'")) {
            sel.setString(1, recruiter.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) count = rs.getInt("recruitedcount");
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return count;
    }

    public MuxAffiliate.RecruitedInformations loadRecruited(final UUID recruited) {
        final String method = getMethodName();
        MuxAffiliate.RecruitedInformations informations = null;
        final MuxAffiliate aff = ms.getAffiliate();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(recruiter) AS uuid, status, payments, timestamp FROM MuxAffiliate WHERE recruited = UNHEX(?)")) {
            sel.setString(1, recruited.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    final UUID recruiter = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    informations = aff.new RecruitedInformations(recruited, recruiter, rs.getLong("status"),
                            aff.getGson().fromJson(rs.getString("payments"), new TypeToken<ArrayList<MuxAffiliate.StoreTransaction>>() {
                            }.getType()), rs.getLong("timestamp"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return informations;
    }

    public MuxAffiliate.CashbackInformations loadCashback(final UUID uuid) {
        final String method = getMethodName();
        MuxAffiliate.CashbackInformations informations = null;
        final MuxAffiliate aff = ms.getAffiliate();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT status, payments FROM MuxCashback WHERE uuid = UNHEX(?)")) {
            sel.setString(1, uuid.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    informations = aff.new CashbackInformations(uuid, rs.getLong("status"),
                            aff.getGson().fromJson(rs.getString("payments"), new TypeToken<ArrayList<MuxAffiliate.StoreTransaction>>() {
                            }.getType()));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return informations;
    }

    public void saveCashback(final MuxAffiliate.CashbackInformations cashbackInformations) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO MuxCashback SET uuid = UNHEX(?), status = ?, payments = ? ON DUPLICATE KEY UPDATE status = ?, payments = ?")) {
            final String payments = ms.getAffiliate().getGson().toJson(cashbackInformations.getStoreTransactions());
            st.setString(1, cashbackInformations.getUUID().toString().replace("-", ""));
            st.setLong(2, cashbackInformations.getStatus());
            st.setString(3, payments);
            st.setLong(4, cashbackInformations.getStatus());
            st.setString(5, payments);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public MuxAffiliate.RecruiterInformations loadRecruiter(final UUID recruiter) {
        final String method = getMethodName();
        MuxAffiliate.RecruiterInformations informations = null;
        final MuxAffiliate affiliate = ms.getAffiliate();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT * FROM MuxAffiliateCashout WHERE uuid = UNHEX(?)")) {
            sel.setString(1, recruiter.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    final String currencyAsString = rs.getString("cryptocurrency");
                    MuxAffiliate.CryptoCurrency currency = null;
                    if (currencyAsString != null)
                        try {
                            currency = MuxAffiliate.CryptoCurrency.valueOf(currencyAsString);
                        } catch (final Exception ignored) {
                        }
                    informations = affiliate.new RecruiterInformations(
                            recruiter,
                            rs.getDouble("currentamount"),
                            rs.getDouble("cashoutamount"),
                            rs.getDouble("estimatedamount"),
                            rs.getDouble("totalcashedout"),
                            currency,
                            rs.getString("cryptoaddress"));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return informations;
    }

    public void saveRecruiter(final MuxAffiliate.RecruiterInformations informations, final String name) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("INSERT INTO MuxAffiliateCashout SET uuid = UNHEX(?), player = ?, cryptoaddress = ?, cryptocurrency = ?, currentamount = ?, cashoutamount = ?, estimatedamount = ?, totalcashedout = ? ON DUPLICATE KEY UPDATE " +
                "cryptoaddress = ?, cryptocurrency = ?, currentamount = ?, cashoutamount = ?, estimatedamount = ?, totalcashedout = ?" + (name != null ? ", player = ?" : ""))) {
            st.setString(1, informations.getUUID().toString().replace("-", ""));
            st.setString(2, name);
            final String currency = informations.getCurrency() == null ? null : informations.getCurrency().name();
            st.setString(3, informations.getCryptoAddress());
            st.setString(4, currency);
            st.setDouble(5, informations.getCurrentAmountInEur());
            st.setDouble(6, informations.getCurrentCashoutInEur());
            st.setDouble(7, informations.getEstimatedAmountInEur());
            st.setDouble(8, informations.getTotalCashedoutInEur());
            st.setString(9, informations.getCryptoAddress());
            st.setString(10, currency);
            st.setDouble(11, informations.getCurrentAmountInEur());
            st.setDouble(12, informations.getCurrentCashoutInEur());
            st.setDouble(13, informations.getEstimatedAmountInEur());
            st.setDouble(14, informations.getTotalCashedoutInEur());
            if (name != null) st.setString(15, name);
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public Map<MuxAffiliate.CashbackInformations, MuxAffiliate.RecruiterInformations> getAllCashbackData() {
        final String method = getMethodName();
        final Map<MuxAffiliate.CashbackInformations, MuxAffiliate.RecruiterInformations> result = new HashMap<>();
        final MuxAffiliate aff = ms.getAffiliate();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT status, payments, HEX(uuid) as uuid FROM MuxCashback")) {
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    final UUID cashbackUuid = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    final MuxAffiliate.CashbackInformations informations = aff.new CashbackInformations(cashbackUuid, rs.getLong("status"),
                            aff.getGson().fromJson(rs.getString("payments"), new TypeToken<ArrayList<MuxAffiliate.StoreTransaction>>() {
                            }.getType()));
                    result.put(informations, this.loadRecruiter(cashbackUuid));
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return result;
    }

    public Map<MuxAffiliate.RecruiterInformations, List<MuxAffiliate.RecruitedInformations>> getAllAffiliateData() {
        final String method = getMethodName();
        final Map<MuxAffiliate.RecruiterInformations, List<MuxAffiliate.RecruitedInformations>> result = new HashMap<>();
        final MuxAffiliate aff = ms.getAffiliate();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT DISTINCT HEX(recruiter) as recruiteruuid from MuxAffiliate")) {
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    final UUID recruiterId = UUID.fromString(rs.getString("recruiteruuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    final MuxAffiliate.RecruiterInformations recruiter = loadRecruiter(recruiterId);
                    final List<MuxAffiliate.RecruitedInformations> list = new ArrayList<>();
                    try (final PreparedStatement sel2 = sharedConn.prepareStatement("SELECT HEX(recruited) AS uuid, status, payments, timestamp FROM MuxAffiliate WHERE recruiter = UNHEX(?)")) {
                        sel2.setString(1, recruiterId.toString().replace("-", ""));
                        try (final ResultSet rs2 = sel2.executeQuery()) {
                            boolean isNext = false;
                            while (rs2.next()) {
                                isNext = true;
                                final UUID recruited = UUID.fromString(rs2.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                                final MuxAffiliate.RecruitedInformations informations = aff.new RecruitedInformations(recruited, recruiterId, rs2.getLong("status"),
                                        aff.getGson().fromJson(rs2.getString("payments"), new TypeToken<ArrayList<MuxAffiliate.StoreTransaction>>() {
                                        }.getType()), rs2.getLong("timestamp"));
                                list.add(informations);
                            }
                            if (isNext) result.put(recruiter, list);
                        }
                    }
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return result;
    }

    public MuxAffiliate.TopRecruiter[] getTop10Recruiter() throws Exception {
        final String method = getMethodName();
        final MuxAffiliate.TopRecruiter[] topRecruiters = new MuxAffiliate.TopRecruiter[10];
        int index = 0;
        final MuxAffiliate aff = ms.getAffiliate();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(recruiter) as uuid, COUNT(*) AS amount " +
                "FROM MuxAffiliate WHERE status != '-2' GROUP BY recruiter HAVING COUNT(*) >= 1 ORDER BY amount DESC LIMIT 100");
             final ResultSet rs = sel.executeQuery()) {
            while (rs.next()) {
                final UUID uuid = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                final MuxRanks.PermissionsUser user = ms.getPerms().getUserData(uuid);
                if (user.getTeamGroup() != null && user.getTeamGroup().equalsIgnoreCase("Creator+")) continue;
                topRecruiters[index] = aff.new TopRecruiter(uuid, rs.getInt("amount"));
                index++;
                if (index == 10) break;
            }
        } finally {
            timings.stop(method);
        }
        return topRecruiters;
    }

    public void saveRecruiterCryptoAddress(final UUID recruiter, final MuxAffiliate.CryptoCurrency currency, final String address) {
        final String method = getMethodName();
        try (final PreparedStatement st = sharedConn.prepareStatement("UPDATE MuxAffiliateCashout SET cryptoaddress = ?, cryptocurrency = ? WHERE uuid = UNHEX(?)")) {
            st.setString(1, address);
            st.setString(2, currency == null ? null : currency.name());
            st.setString(3, recruiter.toString().replace("-", ""));
            st.executeUpdate();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
    }

    public List<MuxAffiliate.RecruitedInformations> getRecruited(final UUID recruiter, final int page, final int perpage) {
        final String method = getMethodName();
        final List<MuxAffiliate.RecruitedInformations> list = new ArrayList<>();
        final MuxAffiliate aff = ms.getAffiliate();
        final int max = perpage * page, min = max - perpage;
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(recruited) AS uuid, status, payments, timestamp FROM MuxAffiliate WHERE recruiter = UNHEX(?) ORDER BY timestamp DESC LIMIT ?, ?")) {
            sel.setString(1, recruiter.toString().replace("-", ""));
            sel.setInt(2, min);
            sel.setInt(3, perpage);
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    final UUID recruited = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    final MuxAffiliate.RecruitedInformations informations = aff.new RecruitedInformations(recruited, recruiter, rs.getLong("status"),
                            aff.getGson().fromJson(rs.getString("payments"), new TypeToken<ArrayList<MuxAffiliate.StoreTransaction>>() {
                            }.getType()), rs.getLong("timestamp"));
                    list.add(informations);
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return list;
    }

    public List<MuxAffiliate.RecruitedInformations> getRecruitedList(final UUID recruiter) {
        final String method = getMethodName();
        final List<MuxAffiliate.RecruitedInformations> list = new ArrayList<>();
        final MuxAffiliate aff = ms.getAffiliate();
        try (final PreparedStatement sel = sharedConn.prepareStatement("SELECT HEX(recruited) AS uuid, status, payments, timestamp FROM MuxAffiliate WHERE recruiter = UNHEX(?)")) {
            sel.setString(1, recruiter.toString().replace("-", ""));
            try (final ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    final UUID recruited = UUID.fromString(rs.getString("uuid").replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    final MuxAffiliate.RecruitedInformations informations = aff.new RecruitedInformations(recruited, recruiter, rs.getLong("status"),
                            aff.getGson().fromJson(rs.getString("payments"), new TypeToken<ArrayList<MuxAffiliate.StoreTransaction>>() {
                            }.getType()), rs.getLong("timestamp"));
                    list.add(informations);
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        timings.stop(method);
        return list;
    }
}