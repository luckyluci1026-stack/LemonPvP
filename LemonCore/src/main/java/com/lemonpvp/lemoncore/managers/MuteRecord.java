package com.lemonpvp.lemoncore.managers;

import java.sql.Timestamp;
import java.util.UUID;

public class MuteRecord {
    public int id;
    public UUID uuid;
    public String username;
    public String reason;
    public UUID muterUuid;
    public String muterName;
    public Timestamp muteTime;
    public Timestamp expires;
    public boolean active;

    public boolean isPermanent() { return expires == null; }
    public boolean isExpired() {
        if (expires == null) return false;
        return expires.before(new Timestamp(System.currentTimeMillis()));
    }
    public long getRemainingSeconds() {
        if (expires == null) return -1;
        long diff = expires.getTime() - System.currentTimeMillis();
        return diff > 0 ? diff / 1000 : 0;
    }
}
