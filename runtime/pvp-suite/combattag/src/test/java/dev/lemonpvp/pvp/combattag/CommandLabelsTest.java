package dev.lemonpvp.pvp.combattag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CommandLabelsTest {

    @Test
    void extractsTheCommandName() {
        assertEquals("spawn", CommandLabels.of("/spawn"));
        assertEquals("home", CommandLabels.of("/Essentials:Home base"));
        assertEquals("tpa", CommandLabels.of("/tpa   Alice"));
        assertEquals("", CommandLabels.of("/"));
    }
}
