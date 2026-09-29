# Firefox-Sperre

Sperrt Firefox, solange kein WLAN verbunden ist. Funktioniert über einen lokalen VPN-Tunnel
(nur Firefox wird hineingeleitet, es geht nichts nach außen). Kein Root nötig.

## APK über GitHub bauen
1. Neues Repository anlegen und den gesamten Inhalt dieses Ordners hochladen (inkl. `.github`).
2. Tab "Actions" -> "Build APK" läuft automatisch (oder "Run workflow").
3. Nach ca. 3-5 Minuten den Artifact "firefox-sperre-apk" herunterladen (ZIP mit app-debug.apk).
4. APK auf dem Pixel installieren, App öffnen, "Einschalten" tippen, VPN-Abfrage bestätigen.

Tipp: Unter Einstellungen -> Netzwerk & Internet -> VPN -> Firefox-Sperre -> "Always-on VPN" aktivieren,
damit die Sperre nach einem Neustart automatisch läuft.
