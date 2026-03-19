package com.yarnandthread.app.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yarnandthread.app.model.Annotation;
import com.yarnandthread.app.model.Counter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromAnnotationList(List<Annotation> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Annotation> toAnnotationList(String json) {
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Annotation>>(){}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String fromCounterList(List<Counter> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Counter> toCounterList(String json) {
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Counter>>(){}.getType();
        return gson.fromJson(json, type);
    }
}
