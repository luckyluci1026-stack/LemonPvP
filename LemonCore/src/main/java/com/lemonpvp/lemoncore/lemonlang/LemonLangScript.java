package com.lemonpvp.lemoncore.lemonlang;

import com.lemonpvp.lemoncore.lemonlang.ast.Program;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single loaded LemonLang script file: its parsed program, any errors
 * collected during load, and summary counts for diagnostics.
 */
public class LemonLangScript {

    private final String name;
    private final File file;
    private Program program;
    private final List<LemonLangError> errors = new ArrayList<>();
    private boolean loaded;

    private int itemCount;
    private int commandCount;
    private int guiCount;
    private int triggerCount;

    public LemonLangScript(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public String name() {
        return name;
    }

    public File file() {
        return file;
    }

    public Program program() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public List<LemonLangError> errors() {
        return Collections.unmodifiableList(errors);
    }

    public void addError(LemonLangError error) {
        if (error != null) {
            errors.add(error);
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean loaded() {
        return loaded;
    }

    public void markLoaded() {
        this.loaded = true;
    }

    // ---- Counts -------------------------------------------------------------

    public int itemCount() {
        return itemCount;
    }

    public int commandCount() {
        return commandCount;
    }

    public int guiCount() {
        return guiCount;
    }

    public int triggerCount() {
        return triggerCount;
    }

    public void setCounts(int itemCount, int commandCount, int guiCount, int triggerCount) {
        this.itemCount = itemCount;
        this.commandCount = commandCount;
        this.guiCount = guiCount;
        this.triggerCount = triggerCount;
    }
}