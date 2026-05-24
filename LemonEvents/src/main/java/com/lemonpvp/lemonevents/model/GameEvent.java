package com.lemonpvp.lemonevents.model;

public class GameEvent {

    private final int id;
    private final String name;
    private final EventType type;
    private final int prize3;
    private final int prize2;
    private final int prize1;
    private EventStatus status;

    public GameEvent(int id, String name, EventType type, int prize3, int prize2, int prize1, EventStatus status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.prize3 = prize3;
        this.prize2 = prize2;
        this.prize1 = prize1;
        this.status = status;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public EventType getType() { return type; }
    public int getPrize3() { return prize3; }
    public int getPrize2() { return prize2; }
    public int getPrize1() { return prize1; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
}
