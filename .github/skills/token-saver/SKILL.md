---
name: token-saver
description: Sparregeln für den Agenten zur Minimierung von Token-Kosten.
---

# Token-Spar-Regeln

1. **Datei-Zugriffe minimieren:**
   - Lies niemals ganze Quelldateien, wenn nur eine bestimmte Methode bearbeitet werden soll.
   - Lies bei großen Vanilla-/Fabric-Klassen nur die Methodensignaturen (Interfaces).

2. **Antwort-Format (Output-Tokens sparen):**
   - Gib KEINE Begrüßungen, Höflichkeitsfloskeln oder Einleitungen aus ("Hier ist der Code...").
   - Gib bei Code-Änderungen NUR Unified Diffs oder die geänderten Code-Blöcke aus, NIEMALS die komplette Datei.
   - Halte Kommentare im Code auf dem absoluten Minimum.