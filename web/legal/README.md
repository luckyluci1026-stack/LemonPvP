# Rechtstexte — Nutzungsbedingungen & Datenschutzerklärung

`terms.html` (Nutzungsbedingungen) und `privacy.html` (Datenschutzerklärung) für
lemonpvp.de, im Lemon/Dark-Design der Hauptseite (aus der hochgeladenen
`style.css` übernommen: `#facc15` auf `#07090f`, Poppins + Inter via Google
Fonts, Glassmorphism-Karten).

## ⚠️ Wichtig — vor dem Livegang

**Das sind fundierte Entwürfe, keine fertige Rechtsberatung.** Beide Dokumente
sind an den tatsächlichen Datenflüssen des LemonPvP-Codes orientiert (IP-Check
via Drittanbieter für VPN-Erkennung, Chat-Moderation, Bann-/Mute-Historie,
optionale Discord-Verknüpfung, Store-Käufe) — das macht sie deutlich genauer
als eine generische Vorlage, ersetzt aber keine anwaltliche Prüfung.

### Vor der Veröffentlichung:

1. **Alle `[PLATZHALTER]`-Felder ausfüllen** — Anschrift, E-Mail,
   Hosting-Anbieter, Zahlungsdienstleister, Aufbewahrungsfristen, Datum. Beide
   Dateien enthalten sie an denselben Stellen (`§ 1` / `Ziffer 1` = Anbieter).
2. **Von einer Anwältin/einem Anwalt prüfen lassen** — insbesondere:
   - § 6 der Nutzungsbedingungen (Widerrufsrecht bei digitalen Käufen —
     braucht einen echten Checkbox-Consent-Schritt im Checkout, nicht nur den
     Text)
   - § 13 der Nutzungsbedingungen (Gerichtsstand bei Verbrauchern in der EU)
   - Ziffer 5 und 11 der Datenschutzerklärung (Auftragsverarbeiter,
     Drittlandtransfer — abhängig davon, wo eure Server/Dienste tatsächlich
     stehen)
3. **Separates Impressum ergänzen.** In Deutschland ist ein Impressum
   (§ 5 DDG, vormals TMG) für so gut wie jede Webseite mit Kaufoption
   **gesetzlich Pflicht und rechtlich unabhängig** von diesen beiden
   Dokumenten — es wurde hier nicht miterstellt, weil dafür echte
   Geschäftsangaben (ggf. Handelsregister, USt-ID) nötig sind, die wir nicht
   erfinden. Sag Bescheid, dann bauen wir es im selben Design.
4. Falls die Hauptseite Analyse-/Marketing-Cookies einsetzt (Google Analytics
   o. ä.): vorher ein Einwilligungsbanner nach § 25 TTDSG einrichten — Ziffer 9
   der Datenschutzerklärung geht aktuell von rein technisch notwendigen
   Cookies aus.

## Deploy

Statische Dateien, kein Build:

1. `terms.html` und `privacy.html` auf lemonpvp.de hochladen, z. B. unter
   `/terms` und `/privacy`.
2. In der Haupt-Navigation verlinken (Footer bietet sich an).
3. Fertig — beide Seiten verlinken bereits gegenseitig aufeinander und zurück
   zu `lemonpvp.de` (relativer Pfad `../../` — ggf. an eure echte
   Verzeichnisstruktur anpassen).
