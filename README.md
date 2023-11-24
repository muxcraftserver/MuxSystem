# MuxSystem

**Das originale MuxCraft System, v10.**

*Alle Server Dateien findest du auf https://muxcraft.eu*

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
8Klicke auf `OK`, um das Artifact zu erstellen.

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