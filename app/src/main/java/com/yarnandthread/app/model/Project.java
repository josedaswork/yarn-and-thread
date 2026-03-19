package com.yarnandthread.app.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.yarnandthread.app.database.Converters;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "projects")
@TypeConverters(Converters.class)
public class Project {

    @PrimaryKey
    public String id;
    public String name;
    public long created;
    public int currentPage;
    public float zoom;
    public String pdfFileName; // stored filename in app's files dir

    public List<Annotation> annotations = new ArrayList<>();
    public List<Counter> counters = new ArrayList<>();

    public Project() {}

    public Project(String id, String name) {
        this.id = id;
        this.name = name;
        this.created = System.currentTimeMillis();
        this.currentPage = 1;
        this.zoom = 1.3f;
    }
}
