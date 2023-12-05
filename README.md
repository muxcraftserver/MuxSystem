# MuxSystem

**Das originale MuxCraft System, v10.**

*Alle Server Dateien findest du auf https://muxcraft.eu*

## Features

**Shop**
- GemShop: Automated Market Maker for users to switch between Coins and Gems. Purchase Items, Ranks & more with Gems.
- Shop: Visual Player Shop that works like the stock market. The cheapest offer is shown. Dynamically tracks the cheapest price for each item, updates last known prices, and keeps track of the volume of items sold. Item prices fluctuate based on supply and demand. Earn coins from other players by selling, or buy items. GUI for adding items, updating prices, removing items, and bulk editing features.
- Premium Market: User-friendly auction house for items not in the shop.
- Mining: Mine ores in the wilderness to earn coins. Inflation-adjusted rewards.
- Trading System: Trading GUI to safely trade items, coins between two players. Robust mechanisms and alerts to players to prevent fraud.

**Bases**
- Create & reset bases
- Loads & unloads base chunks as needed to optimize performance
- Invite friends to base (temporarily or permanently)
- Griefing (/grief): Bases become griefable after 72 hours of inactivity (not during whitelist)
- Automatic calculation of base value based on block prices in the MuxShop.
- Base Ranking (/basen)
- Base Spectator/Visiting - /base [name] (Gamemode 3)
- Save base as schematic (only with GOLD Rank)
- Vote to extend your base daily
- Helper NPC
- Custom Scoreboards when on own base, visiting or griefing

**Anti Lag System**
- TPS Monitoring
- Completely automated anti lag system
- Entity & Farms Management: Reduces lags from entities & farms
- Item & Drop Management: Prevent lag due to excessive item drops
- Prevent Interaction & Packet Spam
- Chunk Management & Cleanup
- Redstone Optimization
- Performance Report (/lag)
- Ping Report (/ping)


**Bot Detection System**
- Verification check via simple Book GUI if using VPNs & other suspicious behaviors
- Auto Enchanting Checks
- Mining Bot Checks
- Auto-Detect Chatbots (0% false flags) (Analyzes player chat messages to identify potential spam bots, using pattern recognition and comparison of messages across different players, perma mutes it)

**Casino**
- Realistic Games like Slot Machine, Crash, Scratch Cards, Roulette, CoinFlip, Wheel Of Fortune, Six Fields, Rock Paper Scissors, Item Flip, Guess The Number, Blackjack, Texas Holdem.
- Realistic Casino Effects & Decorations
- Bank (Switch MuxCoins to Chips and Chips to MuxCoins)
- Energy System for players (Replenish with chips)
- Special Areas for ULTRA & Gold
- Barkeepers, DJs, Security Guards, Play Music
- Custom Scoreboard for Casino

**Events**
- Automated Event System
- Chat Events, Normal Events & Big Events
- AnvilDrop, Arena, BlockParty, Boss, Button, Chamber, Custom, DropSearch, Dropper, EXP, Exit, FastChat, GetDown, GiveAll, Guess, GunWar, Guns, Jump, LastChat, Lotto, Mine, Quiz, Random, Splegg, SurvivalGames, TNT, TNTRun, TempleRun, TopArena, Tournament, Varo, War.
- MuxSearch Events for different holiday events (Easter eggs, Candies on Halloween, etc)
- Spectate players during Events
- Inflation-adjusted rewards
- Custom Scoreboard for each Event

**Extras**
- Extras GUI (/extras)
- Perks (permanent potion effects)
- Realistic Bike (your friend can also mount it)
- Animated Emoji Heads
- Mounts
- Pets
- Boosters (Fly Booster, XP Boosters, Spawner Boosters)
- Multi Tools
- Crates / Chests (Dynamically updated items based on the prices in the MuxShop)
- Extra Commands
- Expanded Enderchest
- Blood Bending (for Mobs only)

**Holiday Specials**
- Christmas Penguins, Halloween Pumpkins, Advent calendar

**Marketing**
- Affiliate System (earn money by inviting players), Cashback for YouTubers
- Email System (collect valid emails & answer to support tickets)
- Gift Codes (create, delete, use - /giftcode)
- Newbie Infos & Rewards
- Recurring Tips on how to play
- Voting System (Voting required for: Access to Base, Expand Base, Casino, Search Event, Give All, Random, Open Chests, Advent, Menu, Homes, /profile, /repair, /tpa, /tpahere, /tpaccept, Ore Mining, 1vs1, /sell, /trade, Clan Base, Warps (Shop))

**Main Player Menu**
- Dynamic based on your progress and rank
- Easy GUI
- Settings (blood effect, chat filter, global chat, profile view, support)
- Admin Settings (fly, vanish, chatprefix, "focus mode", forcefield)
- Team Base Management

**Misc (MuxSystem):**
- Full History (/history), (Coin, Gem, Casino, Command, Death, Teamactions, Teleport, Payment, Support History)
- Homes (/home), (GUI, create, view, delete homes, admin management)
- Payment System (accept paysafecards ingame)
- Warp System (/warp) (create, view, delete, visit warps. Random Teleport)
- Shared MySQL Database (Works across multiple servers, updating with netty packets)
- Right Click Player Menu (right click player)
- Teleport Management (TPA Requests)
- Async Player Data Saving, Scheduling Tasks
- Cooldown Management
- Custom Book GUIs
- Current Location in tab list
- Clickable, hoverable chat messages

**MuxTeam**
- Server Analytics
- Admin Menu (/admin) (set different server settings, see analytics & more)
- Punish System (Chat slowdowns, PvP Bans, Auto Perma Mutes for Bots)
- ChatFilter (/chatfilter) (GUI, advanced word blacklist system)
- Player Overview (/o), (GUI, manage players: resetting stats, managing homes, teleportation, and health management, banning, kicking, and controlling permissions)
- Polls (run public or private polls with multiple options & see results)
- Report players (/report), Chatreports & Cheatreports, GUI for admins to see reports & act on them
- Support System (/support) (Create, manage, close support session. Transfer support to other supporter. Notify/alert system, rotate supports to different supporters so nobody can "snipe" them, save conversation history & response times. Shows FAQ to player before he can ask for support. Ban players from support (24h))
- Team Overview (/team) (See a full list of team commands and features)
- User Transfer (transfer all user data from one player to another)
- Vanish System (hide from players/admins, depending on your rank)

**PvP**
- 1vs1 (/1vs1), (Arena System for 1vs1 fights, Random Kits, unranked after 3 days of inactivity, Custom scoreboard)
- ClanWars (/cw) (Create fights between clans, dynamic arena system, set entry costs, healing limits, bodyfix/op apples on/off, Custom scoreboards)
- Training (/training) (Dynamically creates PvP Bots in the same arena, only shown to player. Custom Bot movement, Pathfinding, visual & sound effects, attributes & behaviors)
- PvP Stats, Trophy System (/stats), Leagues
- Monthly seasons with winners

**Ranks**
- Custom Permission system
- Various User Ranks with Benefits & GUIs
- Temporary ranks (GOLD)
- VIP, ELITE, EPIC (Diamond in Menu), ULTRA (/ultra), GOLD (/gold), Creator (/creator)

**Social**
- Friendship System (with online alerts)
- Clan System: Create, join, manage, and leave clans; clan-specific GUIs for different member ranks; invite members; clan-exclusive chat; clan base; clan rankings and trophies.
- Chat System: Advanced Chat Spam Protection: Utilizes intelligent spam detection and character repetition analysis to prevent chat spam. Adaptive Word Blacklisting, IP and Domain Filtering. Staff-only chat.

**Utilities**
- Fake Snow
- Scoreboard Manager
- NPCs
- Player NPCs
- Packet Manager
- Offline Player Editing
- Language
- Play Music
- Holograms
- Giant Items
- Fireworks
- Old Enchanting, Blood Effects
- Chairs
- Bossbar
- Anvil Input
- Duplication Glitch Fixes
- Confirm Inventory
- Page Inventory Creator

and much more...



## Bibliotheken

Dieses Projekt verwendet eine Reihe von Bibliotheken, die im `libs` Ordner des Projekts zu finden sind. 

## Bauen des Plugins

Das Projekt wird mit dem Artifact-System von IntelliJ gebaut. Hier sind die Schritte, die du befolgen musst:

1. Öffne dein Projekt in IntelliJ IDEA.
2. Navigiere zu `File > Project Structure`.
3. Wähle im linken Menü `Artifacts`.
4. Klicke auf das Plus-Symbol oben links und wähle `JAR > Empty`.
5. Ziehe nun zuerst  `xyz compile output` in den `Output Layout` Bereich. 
6. Jetzt machst du rechtsklick auf Client-1.0.2-RELEASE.jar und wählst `Extract to Output Root`.
7. Wiederhole Schritt 6 mit Server-1.0.2-RELEASE.jar, Shared-1.0.2-RELEASE.jar, mail-1.5.0.jar, json-20210307.jar, httpclient-4.5.13.jar und httpcore-4.4.13.jar.
8. Klicke auf `OK`, um das Artifact zu erstellen.

**Die Artifakte sollten am Ende ungefähr so aussehen:**

![Bild](https://i.imgur.com/xofS3v9.png)

Zum Bauen des Plugins klicke auf `Build > Build Artifacts` und wähle `Build`. Das Plugin wird nun im `out` Ordner des Projekts erstellt.

## Verwendung des Projekts

Nach dem Bauen des Projekts musst du die erstellte JAR-File in den `plugins` Ordner deines Servers hochladen. Zusätzlich musst du den `MuxSystem` Ordner, der Standarddateien enthält, ebenfalls in den `plugins` Ordner deines Servers hochladen.

Bitte beachte, dass du eventuell die Konfigurationsdateien im `MuxSystem` Ordner anpassen musst, um das Plugin korrekt auf deinem Server zu konfigurieren.

## Einrichtung der MySQL-Verbindung

Um eine MySQL-Verbindung für dieses Projekt einzurichten, musst du die `config.yml` Datei im `src/main/java` Verzeichnis bearbeiten. Hier sind die Schritte, die du befolgen musst:

**Die Shared Datenbank wird verwendet, wenn man mehrere Server hat. Bei nur einem Server können hier die gleichen Daten wie bei der normalen Datenbank eingetragen werden.**

1. Öffne die `config.yml` Datei in deinem bevorzugten Texteditor.
2. Suche nach dem Abschnitt `database` unter `#############>>MYSQL<<#############`.
3. Ändere die Werte `username`, `password` und `url` unter `database` und `shared` auf deine MySQL-Datenbankdetails. Zum Beispiel:
    ```yaml
    database:
        username: deinUsername
        password: deinPasswort
        url: jdbc:mysql://deineServerAdresse:3306/deineDatenbank?autoReconnect=true&maxReconnects=3
    shared:
        username: deinUsername
        password: deinPasswort
        url: jdbc:mysql://deineServerAdresse:3306/deineDatenbank?autoReconnect=true&maxReconnects=3
    ```
4. Speichere die Änderungen und schließe die Datei.

Jetzt ist dein Projekt so konfiguriert, dass es eine Verbindung zu deiner MySQL-Datenbank herstellt, wenn es ausgeführt wird.
