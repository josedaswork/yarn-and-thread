package com.yarnandthread.app.database;

import android.content.Context;
import androidx.room.*;
import com.yarnandthread.app.model.Project;

@Database(entities = {Project.class}, version = 1, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ProjectDao projectDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "yarnandthread_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
