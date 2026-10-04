const DREI_D = [
  'grass_block',
  'bedrock',
  'bricks',
  'command_block',
  'observer',
  'crafting_table',
  'chest',
  'player_head',
];

const ITEMS = [
  'book',
  'enchanted_book',
  'writable_book',
  'written_book',
  'paper',
  'map',
  'compass',
  'clock',
  'spyglass',
  'bell',
  'goat_horn',
  'oak_sign',
  'oak_door',
  'oak_sapling',
  'wheat_seeds',
  'wooden_pickaxe',
  'iron_pickaxe',
  'diamond_pickaxe',
  'iron_sword',
  'diamond_sword',
  'mace',
  'bow',
  'arrow',
  'trident',
  'golden_helmet',
  'diamond_chestplate',
  'elytra',
  'feather',
  'totem_of_undying',
  'emerald',
  'diamond',
  'gold_ingot',
  'iron_ingot',
  'amethyst_shard',
  'heart_of_the_sea',
  'nether_star',
  'ender_pearl',
  'ender_eye',
  'experience_bottle',
  'redstone',
  'redstone_torch',
  'repeater',
  'hopper',
  'lead',
  'name_tag',
  'saddle',
  'minecart',
  'firework_rocket',
  'fire_charge',
  'slime_ball',
  'lava_bucket',
  'water_bucket',
  'golden_apple',
  'apple',
  'cake',
  'cooked_beef',
  'bone',
  'spider_eye',
  'painting',
  'barrier',
];

const SYMBOLE = [...DREI_D, ...ITEMS];

const AUS_FONT_AWESOME = {
  'fa-book': 'book',
  'fa-book-open': 'enchanted_book',
  'fa-terminal': 'command_block',
  'fa-hand-fist': 'diamond_sword',
  'fa-shield-halved': 'observer',
  'fa-coins': 'emerald',
  'fa-flag-checkered': 'wooden_pickaxe',
  'fa-mobile-screen-button': 'bedrock',
  'fa-person-running': 'feather',
  'fa-crosshairs': 'iron_sword',
  'fa-cube': 'bricks',
  'fa-scale-balanced': 'totem_of_undying',
  'fa-house': 'compass',
  'fa-map': 'map',
  'fa-compass': 'compass',
  'fa-gem': 'diamond',
  'fa-hammer': 'iron_pickaxe',
  'fa-trophy': 'nether_star',
  'fa-users': 'player_head',
  'fa-circle-question': 'book',
  'fa-lightbulb': 'redstone_torch',
  'fa-gear': 'repeater',
  'fa-wheat-awn': 'wheat_seeds',
  'fa-dragon': 'elytra',
  'fa-skull': 'bone',
  'fa-heart': 'golden_apple',
  'fa-star': 'nether_star',
  'fa-fire': 'fire_charge',
  'fa-bolt': 'redstone',
  'fa-gamepad': 'ender_pearl',
};

function symbolName(wert) {
  const name = String(wert || '').trim().toLowerCase();
  if (SYMBOLE.includes(name)) {
    return name;
  }
  return AUS_FONT_AWESOME[name] || '';
}

function symbolBild(name, vorne) {
  const datei = `${vorne}assets/mc/${name}`;
  if (DREI_D.includes(name)) {
    return `<img src="${datei}.png" srcset="${datei}.png 1x, ${datei}@2x.png 2x, ${datei}@3x.png 3x" alt="" width="32" height="32">`;
  }
  return `<img src="${datei}.png" alt="" width="32" height="32">`;
}

module.exports = {
  DREI_D,
  SYMBOLE,
  symbolName,
  symbolBild,
};
