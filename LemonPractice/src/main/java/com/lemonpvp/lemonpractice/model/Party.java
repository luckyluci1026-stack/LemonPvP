package com.lemonpvp.lemonpractice.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Party {

    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final long createdAt = System.currentTimeMillis();

    public Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public UUID getLeader()              { return leader; }
    public void setLeader(UUID leader)   { this.leader = leader; }
    public Set<UUID> getMembers()        { return members; }
    public long getCreatedAt()           { return createdAt; }

    public void addMember(UUID uuid)     { members.add(uuid); }
    public void removeMember(UUID uuid)  { members.remove(uuid); }
    public boolean isMember(UUID uuid)   { return members.contains(uuid); }
    public boolean isLeader(UUID uuid)   { return leader.equals(uuid); }
    public int size()                    { return members.size(); }
}
