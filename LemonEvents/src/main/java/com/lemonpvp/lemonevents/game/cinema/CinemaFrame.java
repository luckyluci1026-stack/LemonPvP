package com.lemonpvp.lemonevents.game.cinema;

public class CinemaFrame {

    public enum FrameType {
        TITLE, CHAT, SOUND
    }

    private final FrameType type;
    private final long delayTicks;
    private final String title;
    private final String subtitle;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;
    private final String message;
    private final String sound;
    private final float volume;
    private final float pitch;

    private CinemaFrame(Builder b) {
        this.type = b.type;
        this.delayTicks = b.delayTicks;
        this.title = b.title;
        this.subtitle = b.subtitle;
        this.fadeIn = b.fadeIn;
        this.stay = b.stay;
        this.fadeOut = b.fadeOut;
        this.message = b.message;
        this.sound = b.sound;
        this.volume = b.volume;
        this.pitch = b.pitch;
    }

    public FrameType getType() { return type; }
    public long getDelayTicks() { return delayTicks; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getFadeIn() { return fadeIn; }
    public int getStay() { return stay; }
    public int getFadeOut() { return fadeOut; }
    public String getMessage() { return message; }
    public String getSound() { return sound; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }

    public static class Builder {
        FrameType type;
        long delayTicks;
        String title = "", subtitle = "", message = "", sound = "";
        int fadeIn = 10, stay = 70, fadeOut = 20;
        float volume = 1.0f, pitch = 1.0f;

        public Builder type(FrameType t) { this.type = t; return this; }
        public Builder delay(long d) { this.delayTicks = d; return this; }
        public Builder title(String t) { this.title = t; return this; }
        public Builder subtitle(String s) { this.subtitle = s; return this; }
        public Builder fadeIn(int f) { this.fadeIn = f; return this; }
        public Builder stay(int s) { this.stay = s; return this; }
        public Builder fadeOut(int f) { this.fadeOut = f; return this; }
        public Builder message(String m) { this.message = m; return this; }
        public Builder sound(String s) { this.sound = s; return this; }
        public Builder volume(float v) { this.volume = v; return this; }
        public Builder pitch(float p) { this.pitch = p; return this; }
        public CinemaFrame build() { return new CinemaFrame(this); }
    }
}
