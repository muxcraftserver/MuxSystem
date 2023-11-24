package me.muxteam.muxteam;


import java.util.*;

public class Analytics {
	private final int dayofyear;
	private int chestopens, playedonevsonegames, affiliate, basegriefed;
	private double affiliatemoney;
	private long eventcoins, gambledchips, spentgems;
	private final Set<UUID> newplayers, eventplayers, casinoplayers, onevsoneplayers, baseplayers, chestplayers, gemshop;
	private final SortedMap<UUID, Long> joinedplayers;
	private final SortedMap<String, Long> gemshopcategories;
	private final SortedMap<String, Integer> npcs, warps, commands, acbans, ackicks, playedcasinogames;

	public Analytics(final int dayofyear, final long eventcoins, final SortedMap<String, Integer> playedcasinogames, final int playedonevsonegames, final int affiliate, final double affiliatemoney, final long totalgambledchips, final long spentgems, final Set<UUID> newplayers, final Set<UUID> gemshop, final Set<UUID> eventplayers, final Set<UUID> casinoplayers, final Set<UUID> onevsoneplayers, final Set<UUID> baseplayers, final SortedMap<UUID, Long> joinedplayers, final SortedMap<String, Integer> npcs, final SortedMap<String, Integer> warps, final SortedMap<String, Integer> commands, final int chestopens, final Set<UUID> chestplayers, final SortedMap<String, Integer> ackicks, final SortedMap<String, Integer> acbans, final SortedMap<String, Long> gemshopcategories, final int basegriefed) {
		this.dayofyear = dayofyear;
		this.eventcoins = eventcoins;
		this.playedcasinogames = playedcasinogames;
		this.playedonevsonegames = playedonevsonegames;
		this.affiliate = affiliate;
		this.affiliatemoney = affiliatemoney;
		this.gambledchips = totalgambledchips;
		this.spentgems = spentgems;
		this.newplayers = newplayers;
		this.eventplayers = eventplayers;
		this.casinoplayers = casinoplayers;
		this.onevsoneplayers = onevsoneplayers;
		this.baseplayers = baseplayers;
		this.joinedplayers = joinedplayers;
		this.npcs = npcs;
		this.warps = warps;
		this.commands = commands;
		this.chestopens = chestopens;
		this.chestplayers = chestplayers;
		this.ackicks = ackicks;
		this.acbans = acbans;
		this.gemshop = gemshop;
		this.gemshopcategories = gemshopcategories;
		this.basegriefed = basegriefed;
	}

	public int getDayOfYear() {
		return dayofyear;
	}

	public Set<UUID> getNewPlayers() {
		return newplayers;
	}

	public void addNewPlayer(final UUID uuid) {
		this.newplayers.add(uuid);
	}

	public Set<UUID> getJoinedPlayers() {
		return joinedplayers.keySet();
	}

	public SortedMap<UUID, Long> getPlayers() {
		return joinedplayers;
	}

	public void addJoinedPlayer(final UUID uuid) {
		this.joinedplayers.putIfAbsent(uuid, 0L);
	}

	public void addCasinoPlayer(final UUID uuid) {
		this.casinoplayers.add(uuid);
	}

	public void addOneVsOnePlayer(final UUID uuid) {
		this.onevsoneplayers.add(uuid);
	}

	public void incrementOnevsOneGames() {
		this.playedonevsonegames++;
	}

	public Set<UUID> getOnevsOnePlayers() {
		return onevsoneplayers;
	}

	public Set<UUID> getGemShop() {
		return gemshop;
	}

	public int getPlayedOnevsOneGames() {
		return playedonevsonegames;
	}

	public void addSpentGems(final long gems) {
		this.spentgems += gems;
	}

	public void addGambledChips(final int chips) {
		this.gambledchips += chips;
	}

	public long getGemsSpent() {
		return spentgems;
	}

	public SortedMap<String, Long> getGemShopCategories() {
		return gemshopcategories;
	}

	public void addGemShopCategory(final String category, final long amount) {
		this.gemshopcategories.put(category, this.gemshopcategories.getOrDefault(category, 0L) + amount);
	}

	public void addPlayTime(final UUID uuid, long timestamp) {
		final long correcttime = checkloggedInYesterday(timestamp);
		if (correcttime != 0L) {
			timestamp = correcttime;
		}
		final Long playtime = System.currentTimeMillis() - timestamp, oldtime = joinedplayers.getOrDefault(uuid, 0L);
		this.joinedplayers.put(uuid, oldtime + playtime);
	}

	public int getAffiliate() {
		return affiliate;
	}

	public double getAffiliateEarned() {
		return affiliatemoney;
	}

	public void addAffiliate() {
		this.affiliate++;
	}

	public void addEventCoins(final int eventcoins) {
		this.eventcoins += eventcoins;
	}

	public void addAffiliateMoney(final double money) {
		this.affiliatemoney += money;
	}

	public long checkloggedInYesterday(final long timestamp) {
		final Calendar midnight = new GregorianCalendar(); // Today 00:01
		midnight.set(Calendar.HOUR_OF_DAY, 0);
		midnight.set(Calendar.MINUTE, 1); // Leave one minute to save yesterday's playtime
		midnight.set(Calendar.SECOND, 0);
		midnight.set(Calendar.MILLISECOND, 0);
		final Date date = new Date(timestamp);
		if (date.before(midnight.getTime())) {
			return midnight.getTimeInMillis();
		}
		return 0L;
	}

	public Long getAveragePlayTime() {
		if (joinedplayers.size() == 0) return 0L;
		Long playtime = 0L;
		for (final Long time : joinedplayers.values()) {
			playtime += time;
		}
		return playtime / joinedplayers.size();
	}

	public int getReturnPlayers(final Set<UUID> newplayers) {
		short n = 0;
		for (final UUID uuid : newplayers) {
			if (joinedplayers.containsKey(uuid)) n++;
		}
		return n;
	}

	public double getPercentageReturn(final int returning) {
		if (newplayers.isEmpty()) return 0D;
		return (double) returning / (double) newplayers.size() * 100D;
	}

	public double getPercentageNew() {
		if (joinedplayers.size() == 0) return 0L;
		return (double) newplayers.size() / (double) joinedplayers.size() * 100D;
	}

	public SortedMap<String, Integer> getNPCs() {
		return npcs;
	}

	public void addNPCUse(final String npc) {
		if (npc == null) return;
		final int use = npcs.getOrDefault(npc, 0);
		npcs.put(npc, use + 1);
	}

	public SortedMap<String, Integer> getWarps() {
		return warps;
	}

	public void addWarpUse(final String warp) {
		final int use = warps.getOrDefault(warp, 0);
		warps.put(warp, use + 1);
	}

	public SortedMap<String, Integer> getCommands() {
		return commands;
	}

	public void addCMDUse(final String cmd) {
		if (cmd.contains("?")) return;
		final int use = commands.getOrDefault(cmd, 0);
		commands.put(cmd, use + 1);
	}

	public void addChestOpen() {
		chestopens++;
	}

	public int getChestOpens() {
		return chestopens;
	}

	public long getEventcoins() {
		return eventcoins;
	}

	public SortedMap<String, Integer> getPlayedCasinoGames() {
		return playedcasinogames;
	}

	public int getTotalPlayedCasinoGames() {
		return playedcasinogames.values().stream().mapToInt(value -> value).sum();
	}

	public String getMostplayedCasinoGame() {
		return playedcasinogames.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(Map.Entry::getKey).orElse("Kein Spiel");
	}

	public void addPlayedCasinoGame(final String game) {
		int curr = this.playedcasinogames.getOrDefault(game, 0);
		this.playedcasinogames.put(game, curr + 1);
	}

	public long getGambledChips() {
		return gambledchips;
	}

	public Set<UUID> getCasinoPlayers() {
		return casinoplayers;
	}

	public Set<UUID> getEventPlayers() {
		return eventplayers;
	}

	public Set<UUID> getBasePlayers() {
		return baseplayers;
	}

	public void addPlayerChestOpen(final UUID uuid) {
		this.chestplayers.add(uuid);
	}

	public Set<UUID> getChestPlayers() {
		return chestplayers;
	}

	public SortedMap<String, Integer> getACKicks() {
		return ackicks;
	}

	public void addAntiCheatKick(final String type) {
		final int kicks = ackicks.getOrDefault(type, 0);
		ackicks.put(type, kicks + 1);
	}

	public SortedMap<String, Integer> getACBans() {
		return acbans;
	}

	public void addAntiCheatBan(final String type) {
		final int bans = acbans.getOrDefault(type, 0);
		acbans.put(type, bans + 1);
	}

	public int getGriefed() {
		return basegriefed;
	}

	public void incrementGriefed() {
		this.basegriefed += 1;
	}
}