package me.muxteam.muxteam;

import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.TreeMap;

public class MuxAnalytics {
	private MuxSystem ms;
	private Analytics analytics;

	public MuxAnalytics(final MuxSystem ms) {
		this.ms = ms;
		analytics = loadAnalytics(Calendar.getInstance(TimeZone.getTimeZone("CET")).get(Calendar.DAY_OF_YEAR));
		load();
	}
	public void close() {
		saveTodayAnalytics(false);
		save();
		this.ms = null;
	}
	private void load() {
		final FileConfiguration cfg = ms.getConfig();
		pvpexpenses = cfg.getLong("pvpexpenses");
		voteexpenses = cfg.getLong("voteexpenses");
		eventexpenses = cfg.getLong("eventexpenses");
		chestexpenses = cfg.getLong("chestexpenses");
		minegenerated = cfg.getLong("minegenerated");
		affiliateexpenses = cfg.getLong("affiliateexpenses");
		purchasedgems = cfg.getLong("purchasedgems");
		gemprofits = cfg.getLong("gemprofits");
	}

	public void save() {
		final FileConfiguration cfg = ms.getConfig();
		cfg.set("pvpexpenses", pvpexpenses);
		cfg.set("voteexpenses", voteexpenses);
		cfg.set("eventexpenses", eventexpenses);
		cfg.set("chestexpenses", chestexpenses);
		cfg.set("purchasedgems", purchasedgems);
		cfg.set("minegenerated", minegenerated);
		cfg.set("affiliateexpenses", affiliateexpenses);
		cfg.set("gemprofits", gemprofits);
		ms.saveConfig();
	}
	public void saveTodayAnalytics(final boolean finishday) {
		if (finishday) {
			for (final Player pl : ms.getServer().getOnlinePlayers()) {
				if (pl.isOnline() == false) continue;
				final MuxUser u = ms.getMuxUser(pl.getName());
				if (u != null) {
					getAnalytics().addPlayTime(pl.getUniqueId(), u.getLoginTimestamp());
				}
			}
		}
		ms.getDB().saveAnalytics(analytics);
		checkDay();
	}
	public Analytics getAnalytics() {
		return analytics;
	}
	public Analytics loadAnalytics(final int dayofyear) {
		return analytics != null && dayofyear == analytics.getDayOfYear() ? analytics : ms.getDB().loadAnalytics(dayofyear);
	}
	private void checkDay() {
		final int day = analytics.getDayOfYear(), today = Calendar.getInstance(TimeZone.getTimeZone("CET")).get(Calendar.DAY_OF_YEAR);
		if (day != today) {
			analytics = new Analytics(today, 0, new TreeMap<>(), 0, 0, 0, 0, 0, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new TreeMap<>(), new TreeMap<>(),
					new TreeMap<>(), new TreeMap<>(), 0, new HashSet<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), 0);
		}
	}

	private long pvpexpenses, voteexpenses, eventexpenses, chestexpenses, purchasedgems, minegenerated, affiliateexpenses, gemprofits;

	public long getPvPExpenses() {
		return pvpexpenses;
	}

	public long getVoteExpenses() {
		return voteexpenses;
	}

	public long getEventExpenses() {
		return eventexpenses;
	}

	public long getAffiliateExpenses() {
		return affiliateexpenses;
	}

	public long getGemProfits() {
		return gemprofits;
	}

	public long getChestExpenses() {
		return chestexpenses;
	}

	public long getPurchasedGems() {
		return purchasedgems;
	}

	public long getMineGenerated() {
		return minegenerated;
	}

	public void addMineGenerated(final int amount) {
		this.minegenerated += amount;
	}

	public void addPvPExpenses(final int expenses) {
		this.pvpexpenses += expenses;
	}

	public void addVoteExpenses(final int expenses) {
		this.voteexpenses += expenses;
	}

	public void addEventExpenses(final int expenses) {
		this.eventexpenses += expenses;
	}

	public void addChestExpenses(final int expenses) {
		this.chestexpenses += expenses;
	}

	public void addAffiliateExpenses(final long expenses) {
		this.affiliateexpenses += expenses;
	}

	public void addGemProfits(final int gemprofits) {
		this.gemprofits += gemprofits;
	}

	public void addPurchasedGems(final int purchased) {
		this.purchasedgems += purchased;
	}
}