package me.muxteam.muxsystem;

import com.google.common.collect.EvictingQueue;
import me.muxteam.muxsystem.MuxHomes.Home;
import org.bukkit.Location;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MuxUser {
	private final UUID uuid;
	private String name, client;
	private final long firstlogin;
	private long login, coins, online, timeban, timemute, pvpban, lastrankedmatch;
	private int trophies, kills, deaths;
	private double cents, profits;
	private boolean permban, permmute, news, homeschanged, seenmails;
	private Location lastlocation;
	private final EvictingQueue<String> mails;
	private Settings settings;
	private Map<String, Home> homes = new LinkedHashMap<>();

	private String ip;
	private long globalLogin, globalOnline, globalFirstLogin;
	private int gems;
	private UUID lastServer;
	private transient boolean globalUser;

	public MuxUser(final UUID uuid, final String name, final long login, final long firstlogin, final long online, final Location lastlocation, final long coins, final double cents, final int trophies,
				   final double profits, final int kills, final int deaths, final boolean permban, final long timeban, final boolean permmute, final long timemute, final long pvpban, final EvictingQueue<String> mails,
				   final boolean news, final Settings settings, long lastrankedmatch) {
		this.uuid = uuid;
		this.name = name;
		this.login = login;
		this.firstlogin = firstlogin;
		this.online = online;
		this.lastlocation = lastlocation;
		this.permban = permban;
		this.timeban = timeban;
		this.permmute = permmute;
		this.timemute = timemute;
		this.pvpban = pvpban;
		this.coins = coins;
		this.trophies = trophies;
		this.profits = profits;
		this.kills = kills;
		this.deaths = deaths;
		this.mails = mails;
		this.news = news;
		this.settings = settings;
		this.lastrankedmatch = lastrankedmatch;
		this.cents = cents;
	}
	public UUID getUUID() {
		return uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public long getLoginTimestamp() {
		return login;
	}
	public void setLoginTimestamp(final long login) {
		this.login = login;
	}
	public long getFirstLogin() {
		return firstlogin;
	}
	public Location getLastLocation() {
		return lastlocation;
	}
	public void setLastLocation(final Location lastlocation) {
		this.lastlocation = lastlocation;
	}
	public long getOnlineTime() {
		return online;
	}
	public void setOnlineTime(final long online) {
		this.online = online;
	}
	public boolean isTimebanned() {
		return timeban != -1 && timeban > System.currentTimeMillis();
	}
	public long getUnbanTime() {
		return timeban;
	}
	public void applyTimeban(final int seconds) {
		if (seconds == -1) this.timeban = -1;
		else this.timeban = System.currentTimeMillis() + seconds * 1000L;
	}
	public boolean isPvPbanned() {
		return pvpban != -1 && pvpban > System.currentTimeMillis();
	}
	public long getPvPUnbanTime() {
		return pvpban;
	}
	public void applyPvPBan(final int seconds) {
		if (seconds < 1) this.pvpban = seconds;
		else this.pvpban = System.currentTimeMillis() + seconds * 1000L;
	}
	public double getCents() {
		return cents;
	}
	public void setCents(final double cents) {
		this.cents = cents;
	}
	public void addCents(final double cents) {
		if (this.cents + cents >= 1.00D) {
			this.coins++;
			if (this.cents + cents == 1.00D)
				this.cents = 0.00D;
			else
				this.cents = this.cents + cents - 1D;
		} else this.cents += cents;
		if (BigDecimal.valueOf(this.cents).scale() > 2)
			this.cents = BigDecimal.valueOf(this.cents).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
	public void removeCents(double cents) {
		if (this.cents - cents <= 0.00D) {
			this.coins--;
			if (this.cents - cents == 0.00D) {
				this.cents = 1.00D;
			} else {
				this.cents = 1.00D - Math.abs(this.cents - cents);
			}
		} else this.cents -= cents;
		if (BigDecimal.valueOf(this.cents).scale() > 2)
			this.cents = BigDecimal.valueOf(this.cents).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
	public boolean isPermban() {
		return permban;
	}
	public void setPermban(final boolean permban) {
		this.permban = permban;
	}
	public boolean isTimeMuted() {
		return timemute != -1 && timemute > System.currentTimeMillis();
	}
	public long getUnmuteTime() {
		return timemute;
	}
	public void applyTimeMute(final int seconds) {
		if (seconds == -1) this.timemute = -1;
		else this.timemute = System.currentTimeMillis() + seconds * 1000L;
	}
	public boolean isPermMute() {
		return permmute;
	}
	public void setPermMute(final boolean permmute) {
		this.permmute = permmute;
	}
	public long getCoins() {
		return coins;
	}
	public void setCoins(final long coins) {
		this.coins = coins;
	}
	public void addCoins(final long coins) {
		this.coins += coins;
	}
	public int getTrophies() {
		return trophies;
	}
	public void setTrophies(final int trophies) {
		this.trophies = trophies;
	}
	public void addKill() {
		this.kills++;
	}
	public int getKills() {
		return kills;
	}
	public void addDeath() {
		this.deaths++;
	}
	public int getDeaths() {
		return deaths;
	}
	public void resetStats() {
		this.deaths = this.kills = 0;
		this.trophies = 250;
	}
	public double getProfits() {
		return profits;
	}
	public void addProfits(final double profits) {
		this.profits += profits;
	}
	public void claimProfits() {
		if (profits < 1.00D) {
			addCents(profits);
		} else {
			final int muxcoins = Math.toIntExact(Math.round(profits));
			double rest = profits - muxcoins;
			this.coins += profits;
			addCents(rest);
		}
		this.profits = 0.00D;
	}
	public Map<String, Home> getHomes() {
		return homes;
	}
	public void setHomes(final Map<String, Home> homes) {
		this.homes = homes;
	}
	public Home getHome(final String s) {
		return homes.get(s);
	}
	public void addHome(final String s, final Home home) {
		homeschanged = true;
		homes.put(s, home);
	}
	public void removeHome(final String s) {
		homeschanged = true;
		homes.remove(s);
	}
	public void clearHomes() {
		homeschanged = true;
		homes.clear();
	}
	public boolean homesChanged() {
		return homeschanged;
	}
	public EvictingQueue<String> getMails() {
		return mails;
	}
	public void addMail(final String mail) {
		mails.add(mail);
	}
	public void clearMailsIfSeen() {
		if (seenmails) {
			seenmails = false;
			mails.clear();
		}
	}
	public void seenMails() {
		this.seenmails = true;
	}
	public boolean seenNews() { return news; }
	public void setNews(final boolean news) {
		this.news = news;
	}
	public Settings getSettings() {
		return settings;
	}
	public void setSettings(final Settings settings) {
		this.settings = settings;
	}
	public void setClient(final String client) {
		this.client = client;
	}
	public String getClient() {
		return client;
	}
	public long getLastRankedMatch() {
		return lastrankedmatch;
	}
	public void updateLastRankedMatch() {
		this.lastrankedmatch = System.currentTimeMillis();
	}

	// GLOBAL USER
	public boolean isGlobalUser() {
		return globalUser;
	}
	public void setGlobalUser(final boolean globalUser) {
		this.globalUser = globalUser;
	}
	public void setGems(int gems) {
		this.gems = gems;
	}
	public int getGems() {
		return gems;
	}
	public void addGems(final int gems) {
		this.gems += gems;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(final String ip) {
		this.ip = ip;
	}
	public void setGlobalFirstLogin(final long globalFirstOnline) {
		this.globalFirstLogin = globalFirstOnline;
	}
	public void setGlobalOnline(final long globalOnline) {
		this.globalOnline = globalOnline;
	}
	public void setGlobalLoginTimestamp(final long globalLogin) {
		this.globalLogin = globalLogin;
	}
	public void setLastServer(final UUID lastServer) {
		this.lastServer = lastServer;
	}
	public UUID getLastServer() {
		return lastServer;
	}
	public long getGlobalOnline() {
		return globalOnline;
	}
	public long getGlobalFirstLogin() {
		return globalFirstLogin;
	}
	public long getGlobalLoginTimestamp() {
		return globalLogin;
	}
}