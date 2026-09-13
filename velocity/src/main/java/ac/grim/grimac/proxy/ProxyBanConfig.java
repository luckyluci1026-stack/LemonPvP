/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The proxy plugin's settings.
 *
 * <p>A {@code .properties} file rather than YAML on purpose: Velocity does not
 * hand plugins a YAML parser, and pulling one in to read six lines would mean
 * shading a library for no gain.</p>
 */
public record ProxyBanConfig(boolean databaseEnabled, String host, int port,
                             String database, String user, String password) {

    private static final String TEMPLATE = """
            # BuckSMPAC proxy settings.
            #
            # Where the proxy keeps its ban list.
            #
            #   false — plugins/bucksmpac/bans.txt, a plain text file. Fine for a
            #           single proxy; nothing else can read it.
            #   true  — a MySQL/MariaDB table called bucksmpac_bans, created on
            #           first start. Use this if you run more than one proxy, or
            #           want the bans to outlive the container.
            #
            # The database only stores and reloads bans. Logins are answered from
            # memory, so a slow database never delays anybody joining.
            database.enabled=false

            # Not "localhost" on a panel like Pterodactyl: each server is its own
            # container, so localhost means that container and not your database.
            # Use the address the panel shows under Databases.
            database.host=127.0.0.1
            database.port=3306
            database.name=bucksmp
            database.user=root
            database.password=
            """;

    public static ProxyBanConfig loadOrCreate(Path file, Logger logger) {
        Properties properties = new Properties();

        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, TEMPLATE, StandardCharsets.UTF_8);
                logger.info("Wrote a default {} — set database.enabled=true in it to share bans "
                        + "through MySQL/MariaDB.", file.getFileName());
            }
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            }
        } catch (IOException e) {
            logger.warn("Could not read {}; using defaults (ban list in a text file).", file, e);
        }

        return new ProxyBanConfig(
                Boolean.parseBoolean(properties.getProperty("database.enabled", "false").trim()),
                properties.getProperty("database.host", "127.0.0.1").trim(),
                parsePort(properties.getProperty("database.port", "3306")),
                properties.getProperty("database.name", "bucksmp").trim(),
                properties.getProperty("database.user", "root").trim(),
                properties.getProperty("database.password", ""));
    }

    private static int parsePort(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 3306;
        }
    }
}
