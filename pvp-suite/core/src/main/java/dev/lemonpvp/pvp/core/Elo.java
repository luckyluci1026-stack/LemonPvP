package dev.lemonpvp.pvp.core;

/**
 * Elo rating as in chess: beating a stronger player brings more points than beating a
 * weaker one. Pure function, no server code (unit-tested in EloTest).
 */
final class Elo {

    /** Rating of a player without PvP history. */
    static final int START = 1000;

    private Elo() {
    }

    /**
     * @param winner rating of the killer
     * @param loser  rating of the victim
     * @param k      K-factor: the most a single kill can change a rating
     * @return points the killer gains and the victim loses, at least 1
     */
    static int change(int winner, int loser, int k) {
        double expected = 1.0 / (1.0 + Math.pow(10, (loser - winner) / 400.0));
        return Math.max(1, (int) Math.round(k * (1.0 - expected)));
    }
}
