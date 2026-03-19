package com.yarnandthread.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.yarnandthread.app.model.Project;
import java.util.List;

@Dao
public interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY created DESC")
    LiveData<List<Project>> getAllProjects();

    @Query("SELECT * FROM projects WHERE id = :id")
    Project getProjectById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Project project);

    @Update
    void update(Project project);

    @Delete
    void delete(Project project);

    @Query("DELETE FROM projects WHERE id = :id")
    void deleteById(String id);
}
