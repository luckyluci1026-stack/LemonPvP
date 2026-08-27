# Tags – Anleitung: Neue Tags hinzufügen

Tags sind Chat-/Tablist-Suffixe, die hinter dem Spielernamen angezeigt werden
(z. B. `Steve ᴍᴏɴᴇʏ`). Sie werden im Cosmetics-Menü (Slot 14) oder per
`/tags` geöffnet. Ein neuer Tag ist in **2 Pflicht-Schritten** angelegt – ein
dritter Schritt ist nur bei sehr vielen Tags nötig.

Es ist **keine Datenbank-Migration** nötig: Besitz und ausgerüsteter Tag werden
nur über die Tag-`id` (String) gespeichert.

---

## Schritt 1 – Tag im Enum `TagType` eintragen

Datei: `src/main/java/com/lemonpvp/lemoncosmetics/model/TagType.java`

Füge eine neue Enum-Konstante hinzu. Format:

```
ID_KONSTANTE("id", "Anzeigename", "<MiniMessage-Render>", Material.ICON, preis),
```

| Feld          | Bedeutung                                                                 |
|---------------|---------------------------------------------------------------------------|
| `id`          | Eindeutiger Kleinbuchstaben-Schlüssel (in DB & Permission verwendet)      |
| `displayName` | Klartext-Name (intern/Logs)                                               |
| `render`      | MiniMessage-Darstellung **ohne** Klammern/Kursiv – das wird im Chat gezeigt |
| `icon`        | `org.bukkit.Material` für das Icon im Menü                                |
| `preis`       | `> 0` = mit Münzen kaufbar · `0` = **nur per Permission** freischaltbar   |

**Beispiel – kaufbarer Tag:**
```java
PHOENIX ("phoenix", "Phoenix", "<gradient:#ff6d00:#ffd54f>ᴘʜᴏᴇɴɪx", Material.BLAZE_POWDER, 750),
```

**Beispiel – Permission-only Tag (Preis 0):**
```java
OWNER ("owner", "Owner", "<gradient:#ff1744:#b71c1c>ᴏᴡɴᴇʀ", Material.DRAGON_EGG, 0),
```

> Tipp: Die Kapitälchen (ᴀ-ᴢ) wie in den bestehenden Tags machen den Chat
> sauberer. Du kannst aber auch normale Buchstaben oder Emojis nehmen.

Logik, die automatisch greift (nichts weiter zu tun):
- `preis > 0` → Tag erscheint als „Kaufen" und kann mit Münzen erworben werden.
- `preis == 0` → Tag ist „🔒 Nur mit Berechtigung" und erfordert die Permission.
- Die Permission `lemoncosmetics.tag.<id>` schaltet **jeden** Tag gratis frei
  (auch kaufbare) – nützlich für Ränge/Staff.

---

## Schritt 2 – Permission in `plugin.yml` registrieren

Datei: `src/main/resources/plugin.yml`

1. Neuen Permission-Knoten anlegen:
```yaml
  lemoncosmetics.tag.phoenix:
    description: Phoenix tag
    default: false
```

2. Den Knoten im Sammel-Node `lemoncosmetics.tag.*` als Kind ergänzen:
```yaml
  lemoncosmetics.tag.*:
    description: All tags
    default: false
    children:
      ...
      lemoncosmetics.tag.phoenix: true
```

Damit kannst du den Tag z. B. per LuckPerms gezielt vergeben
(`/lp user <name> permission set lemoncosmetics.tag.phoenix true`).

---

## Schritt 3 (nur bei > 21 Tags) – Menü-Plätze erweitern

Datei: `src/main/java/com/lemonpvp/lemoncosmetics/gui/TagsGUI.java`

Das Menü zeigt aktuell max. **21 Tags** (Array `TAG_SLOTS`). Hast du mehr
Tags als Plätze, werden die überzähligen nicht angezeigt. Dann entweder
`TAG_SLOTS` um weitere Slot-Indizes erweitern **oder** das Menü auf eine
größere Inventargröße / Seiten umbauen.

---

## Schnell-Checkliste

- [ ] `TagType`-Konstante hinzugefügt (`id`, Name, Render, Icon, Preis)
- [ ] `lemoncosmetics.tag.<id>` in `plugin.yml` (eigener Node **und** als Kind unter `lemoncosmetics.tag.*`)
- [ ] (Optional) `TAG_SLOTS` erweitert, falls jetzt > 21 Tags
- [ ] Plugin neu bauen & Server neu starten

Kein DB-Schema-Update nötig. Fertig. ✅
