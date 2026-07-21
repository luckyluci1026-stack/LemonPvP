# Rechtstexte — Nutzungsbedingungen, Datenschutzerklärung & Impressum

`terms.html` (Nutzungsbedingungen), `privacy.html` (Datenschutzerklärung) und
`impressum.html` (Impressum) für lemonpvp.de, im Lemon/Dark-Design der
Hauptseite (aus der hochgeladenen `style.css` übernommen: `#facc15` auf
`#07090f`, Poppins + Inter via Google Fonts, Glassmorphism-Karten). Alle drei
verlinken sich gegenseitig über die Topbar.

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
3. **Impressum ausfüllen (`impressum.html`).** In Deutschland gesetzlich
   Pflicht (§ 5 DDG) und **rechtlich unabhängig** von den anderen beiden
   Dokumenten. Die Anschrift dort muss ladungsfähig sein (kein Postfach); die
   USt-ID-Zeile nur ausfüllen, falls vorhanden — als Kleinunternehmer
   (§ 19 UStG) den Hinweis im Dokument stattdessen stehen lassen. Die
   EU-Streitschlichtungs-Hinweise (§ 5) vor Veröffentlichung gegen die
   aktuelle Rechtslage prüfen — die ODR-Pflicht wurde 2025 novelliert.
4. Falls die Hauptseite Analyse-/Marketing-Cookies einsetzt (Google Analytics
   o. ä.): vorher ein Einwilligungsbanner nach § 25 TTDSG einrichten — Ziffer 9
   der Datenschutzerklärung geht aktuell von rein technisch notwendigen
   Cookies aus.

## Deploy

Statische Dateien, kein Build:

1. `terms.html`, `privacy.html` und `impressum.html` auf lemonpvp.de
   hochladen, z. B. unter `/terms`, `/privacy` und `/impressum`.
2. In der Haupt-Navigation verlinken (Footer bietet sich an — ein Impressum
   muss von jeder Seite aus in maximal zwei Klicks erreichbar sein).
3. Fertig — alle drei Seiten verlinken bereits gegenseitig aufeinander und
   zurück zu `lemonpvp.de` (relativer Pfad `../../` — ggf. an eure echte
   Verzeichnisstruktur anpassen).
