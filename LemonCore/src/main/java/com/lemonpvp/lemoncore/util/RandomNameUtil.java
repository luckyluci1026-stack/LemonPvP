package com.lemonpvp.lemoncore.util;

import com.lemonpvp.lemoncore.LemonCore;

import java.util.List;
import java.util.Random;

public class RandomNameUtil {

    private final LemonCore plugin;
    private final Random random = new Random();

    public RandomNameUtil(LemonCore plugin) {
        this.plugin = plugin;
    }

    public String generateName() {
        List<String> firstNames = plugin.getConfig().getStringList("names.first-names");
        List<String> lastNames = plugin.getConfig().getStringList("names.last-names");
        if (firstNames.isEmpty()) firstNames = List.of("Alex", "Sam", "Jordan");
        if (lastNames.isEmpty()) lastNames = List.of("Smith", "Jones", "Taylor");
        String first = firstNames.get(random.nextInt(firstNames.size()));
        String last = lastNames.get(random.nextInt(lastNames.size()));
        return first + last + (random.nextInt(900) + 100);
    }
}
