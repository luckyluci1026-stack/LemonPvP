package de.lemonpvp.helden.item;

import de.lemonpvp.helden.util.Compat;
import de.lemonpvp.helden.util.Keys;
import de.lemonpvp.helden.util.Text;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ein Artefakt aus items.yml.
 *
 * <p>Die {@code custom-model-data} steuert das Java-Resourcepack, der
 * {@code bedrock-identifier} das Geyser-Mapping. Beide Werte muessen zu den
 * Dateien unter /resourcepack passen.</p>
 */
public final class CustomItem {

    private final String id;
    private final Material material;
    private final int customModelData;
    private final String bedrockIdentifier;
    private final String display;
    private final List<String> lore;
    private final boolean unbreakable;
    private final boolean glow;
    private final boolean hideFlags;
    private final Map<String, Integer> enchantments;

    private CustomItem(Builder builder) {
        this.id = builder.id;
        this.material = builder.material;
        this.customModelData = builder.customModelData;
        this.bedrockIdentifier = builder.bedrockIdentifier;
        this.display = builder.display;
        this.lore = builder.lore;
        this.unbreakable = builder.unbreakable;
        this.glow = builder.glow;
        this.hideFlags = builder.hideFlags;
        this.enchantments = builder.enchantments;
    }

    public static Builder builder(String id, Material material) {
        return new Builder(id, material);
    }

    public ItemStack build() {
        return build(1);
    }

    public ItemStack build(int amount) {
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(Text.color(display));
        if (!lore.isEmpty()) {
            meta.setLore(Text.color(lore));
        }
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        if (unbreakable) {
            meta.setUnbreakable(true);
        }

        boolean enchanted = false;
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = Compat.enchantment(entry.getKey());
            if (enchantment != null) {
                meta.addEnchant(enchantment, entry.getValue(), true);
                enchanted = true;
            }
        }
        if (glow && !enchanted) {
            // Leuchten ohne Effekt: harmlose Verzauberung plus versteckter Tooltip.
            Enchantment filler = Compat.enchantment("unbreaking");
            if (filler != null) {
                meta.addEnchant(filler, 1, true);
            }
        }
        if (hideFlags || glow) {
            meta.addItemFlags(ItemFlag.values());
        }

        meta.getPersistentDataContainer().set(Keys.itemId(), PersistentDataType.STRING, id);
        stack.setItemMeta(meta);
        return stack;
    }

    public String id() {
        return id;
    }

    public Material material() {
        return material;
    }

    public int customModelData() {
        return customModelData;
    }

    public String bedrockIdentifier() {
        return bedrockIdentifier;
    }

    public String display() {
        return display;
    }

    public String coloredDisplay() {
        return Text.color(display);
    }

    public static final class Builder {

        private final String id;
        private final Material material;
        private int customModelData;
        private String bedrockIdentifier = "";
        private String display;
        private List<String> lore = List.of();
        private boolean unbreakable;
        private boolean glow;
        private boolean hideFlags;
        private Map<String, Integer> enchantments = new LinkedHashMap<>();

        private Builder(String id, Material material) {
            this.id = id;
            this.material = material;
            this.display = id;
        }

        public Builder customModelData(int customModelData) {
            this.customModelData = customModelData;
            return this;
        }

        public Builder bedrockIdentifier(String bedrockIdentifier) {
            this.bedrockIdentifier = bedrockIdentifier == null ? "" : bedrockIdentifier;
            return this;
        }

        public Builder display(String display) {
            this.display = display == null ? id : display;
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = lore == null ? List.of() : List.copyOf(lore);
            return this;
        }

        public Builder unbreakable(boolean unbreakable) {
            this.unbreakable = unbreakable;
            return this;
        }

        public Builder glow(boolean glow) {
            this.glow = glow;
            return this;
        }

        public Builder hideFlags(boolean hideFlags) {
            this.hideFlags = hideFlags;
            return this;
        }

        public Builder enchantments(Map<String, Integer> enchantments) {
            this.enchantments = enchantments == null ? new LinkedHashMap<>() : new LinkedHashMap<>(enchantments);
            return this;
        }

        public CustomItem build() {
            return new CustomItem(this);
        }
    }
}
