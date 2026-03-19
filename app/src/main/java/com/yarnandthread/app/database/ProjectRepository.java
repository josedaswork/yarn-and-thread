package com.yarnandthread.app.database;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.yarnandthread.app.model.Project;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectRepository {

    private final ProjectDao dao;
    private final LiveData<List<Project>> allProjects;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ProjectRepository(Application app) {
        AppDatabase db = AppDatabase.getInstance(app);
        dao = db.projectDao();
        allProjects = dao.getAllProjects();
    }

    public LiveData<List<Project>> getAllProjects() {
        return allProjects;
    }

    public void insert(Project project) {
        executor.execute(() -> dao.insert(project));
    }

    public void update(Project project) {
        executor.execute(() -> dao.update(project));
    }

    public void delete(Project project) {
        executor.execute(() -> dao.delete(project));
    }

    public void deleteById(String id) {
        executor.execute(() -> dao.deleteById(id));
    }

    public Project getById(String id) {
        // Call only from background thread
        return dao.getProjectById(id);
    }
}
