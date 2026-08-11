package de.lemonpvp.helden.storage;

import de.lemonpvp.helden.player.HeldenProfile;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Persistenz der Spielerprofile. */
public interface Storage {

    Map<UUID, HeldenProfile> loadAll();

    void saveAll(Collection<HeldenProfile> profiles);
}
