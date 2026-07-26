# 🎮 Xenon Client - Installation

## ✅ Die Mod ist einsatzbereit!

### 📦 JAR-Datei Location
```
build\libs\client-1.1.3.jar
```

---

## 🚀 Installation (Minecraft 1.21.11)

### Schritt 1: Fabric Loader installieren
1. Lade den **Fabric Installer** herunter: https://fabricmc.net/use/installer/
2. Starte den Installer und wähle **Minecraft 1.21.11**
3. Klicke auf "Install"

### Schritt 2: Fabric API installieren
1. Lade **Fabric API 0.141.5+1.21.11** herunter: https://modrinth.com/mod/fabric-api
2. Lege die JAR-Datei in deinen `mods` Ordner:
   - Windows: `%appdata%\.minecraft\mods`
   - Mac: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`

### Schritt 3: Xenon Client installieren
1. Kopiere `build\libs\client-1.1.3.jar` in den `mods` Ordner
2. Starte Minecraft mit dem **Fabric** Profil
3. Drücke **RSHIFT** (Rechts Shift) im Spiel, um das Menü zu öffnen

---

## Spotify verbinden

Spotify Developer-Apps erlauben nicht automatisch beliebige Konten. Nutze einen dieser Wege:

1. Füge das Spotify-Konto unter **Spotify Developer Dashboard > Settings > User Management** zu deiner App hinzu.
2. Oder erstelle eine eigene Spotify-App, registriere `http://127.0.0.1:8888/callback` als Redirect URI und trage deren Client-ID im Spotify-Tab der Mod ein.

Danach im Xenon-Menü den Spotify-Tab öffnen und **Connect with Spotify** anklicken. Für die Wiedergabesteuerung kann Spotify Premium erforderlich sein.

---

## 🎨 Features

### Arctic Glow Design
- 🧊 Glassmorphic White UI mit Cyan-Akzenten
- ✨ Subtile Glow-Effekte bei aktiven Modulen
- 🎯 Ultra-clean minimalistisches Design
- 📱 Moderne Fluent Design Ästhetik

### HUD Module
- **CPS Tracker** - Clicks per Second Anzeige
- **FPS Counter** - Framerate Display
- **Armor Display** - Rüstungsstatus
- **Hotbar Anzeige** - Verbesserte Hotbar
- **Keystrokes** - Tasteneingaben
- **Movement Display** - Bewegungsdaten
- **Potion Effects** - Trank-Effekte
- **Text HUD** - Custom Text
- **Combo Counter** - Hit-Kombos
- **Direction HUD** - Richtungsanzeige
- **Session Stats** - Sitzungsstatistik
- **Reach Display** - Reichweitenanzeige

### Navigation
- 📋 **MODULES** - Alle HUD-Module mit Toggle
- ⚙️ **SETTINGS** - Einstellungen
- 📍 **POSITIONS** - HUD-Positionierung
- 💬 **CHAT** - Chat-Einstellungen
- ⚡ **PERFORMANCE** - Performance-Optionen
- 🔧 **CONFIG** - Erweiterte Konfiguration
- ℹ️ **ABOUT** - Über die Mod

### Funktionen
- 🔑 Individuell konfigurierbare Keybinds
- 🔍 Module-Suche im Menü
- 🏷️ Filter: All, Combat, Render, HUD, Util, Movement
- 🎚️ Skalierung pro Modul
- 💾 Automatisches Speichern der Einstellungen
- 🛡️ 16×16-Schildeditor mit eigener RGB-Farbe

### Eigenes Schild zeichnen
1. Halte ein Schild in der Haupt- oder Nebenhand.
2. Öffne das Xenon-Menü mit **RSHIFT** und wähle den Schild-Tab.
3. Zeichne mit der linken Maustaste; die rechte Maustaste löscht Pixel.
4. Wähle die Farbe über die RGB-Regler und klicke **Save local design**.

Der Schildeditor funktioniert vollständig clientseitig. Auf dem Server ist keine Xenon-Mod erforderlich. Das eigene Muster ist nur auf deinem Client sichtbar; andere Spieler sehen ein normales Schild.

---

## ⌨️ Controls

| Aktion | Taste |
|--------|-------|
| Menü öffnen | **RSHIFT** (Rechts Shift) |
| Creative-Flug beschleunigen | **Ctrl + Mausrad hoch** |
| Creative-Flug verlangsamen | **Ctrl + Mausrad runter** |
| Modul an/aus | Toggle im Menü oder Keybind |
| Keybind setzen | "Key" Button im Modul-Card |
| Suche | Suchfeld oben rechts |
| Scrollen | Mausrad |

---

## 🔧 Development

### Projekt neu bauen
```powershell
.\gradlew.bat build
```

### Minecraft mit Mod testen
```powershell
.\gradlew.bat runClient
```

---

## 📝 Version Info
- **Mod Version:** 1.1.3
- **Minecraft:** 1.21.11
- **Fabric Loader:** 0.19.3
- **Fabric API:** 0.141.5+1.21.11

---

## 🎉 Fertig!
Viel Spaß mit deinem Arctic Glow Client! ❄️✨
