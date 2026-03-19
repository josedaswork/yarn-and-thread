package com.yarnandthread.app.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.yarnandthread.app.database.ProjectRepository;
import com.yarnandthread.app.model.Project;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectViewModel extends ViewModel {

    private final ProjectRepository repository;
    private final LiveData<List<Project>> allProjects;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ProjectViewModel(Application app) {
        repository = new ProjectRepository(app);
        allProjects = repository.getAllProjects();
    }

    public LiveData<List<Project>> getAllProjects() { return allProjects; }

    public void insert(Project p) { repository.insert(p); }
    public void update(Project p) { repository.update(p); }
    public void delete(Project p) { repository.delete(p); }

    public void getProjectById(String id, OnProjectLoaded callback) {
        executor.execute(() -> {
            Project p = repository.getById(id);
            if (callback != null) {
                // post back to main thread handled in caller
                callback.onLoaded(p);
            }
        });
    }

    public interface OnProjectLoaded {
        void onLoaded(Project project);
    }

    public static class Factory extends ViewModelProvider.AndroidViewModelFactory {
        private final Application app;
        public Factory(@NonNull Application app) {
            super(app);
            this.app = app;
        }
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ProjectViewModel.class)) {
                //noinspection unchecked
                return (T) new ProjectViewModel(app);
            }
            return super.create(modelClass);
        }
    }
}
