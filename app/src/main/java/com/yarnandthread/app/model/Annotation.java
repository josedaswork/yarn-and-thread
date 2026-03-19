package com.yarnandthread.app.model;

public class Annotation {

    public static final String TYPE_HIGHLIGHT = "highlight";
    public static final String TYPE_NOTE = "note";

    public String id;
    public String type;
    public int page;

    // Normalized coordinates (0.0 - 1.0)
    public float x, y, w, h;

    public String color; // "yellow", "pink", "green", "blue"
    public String text;  // for notes only

    public Annotation() {}

    public static Annotation highlight(int page, float x, float y, float w, float h, String color) {
        Annotation a = new Annotation();
        a.id = "a_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        a.type = TYPE_HIGHLIGHT;
        a.page = page;
        a.x = x; a.y = y; a.w = w; a.h = h;
        a.color = color;
        return a;
    }

    public static Annotation note(int page, float x, float y, String text, String color) {
        Annotation a = new Annotation();
        a.id = "a_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        a.type = TYPE_NOTE;
        a.page = page;
        a.x = x; a.y = y;
        a.text = text;
        a.color = color;
        return a;
    }
}
