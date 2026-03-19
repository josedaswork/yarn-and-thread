package com.yarnandthread.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yarnandthread.app.R;
import com.yarnandthread.app.adapter.ProjectAdapter;
import com.yarnandthread.app.model.Project;
import com.yarnandthread.app.util.PdfStorage;
import com.yarnandthread.app.databinding.ActivityMainBinding;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ProjectViewModel viewModel;
    private ProjectAdapter adapter;

    // Pending data while waiting for PDF picker
    private String pendingProjectId;
    private String pendingProjectName;

    private final ActivityResultLauncher<String[]> pdfPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null && pendingProjectId != null) {
                savePdfAndCreateProject(uri, pendingProjectId, pendingProjectName);
            } else if (pendingProjectId != null) {
                // No PDF selected — still create project without PDF
                createProject(pendingProjectId, pendingProjectName, null);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        viewModel = new ViewModelProvider(this,
                new ProjectViewModel.Factory(getApplication()))
                .get(ProjectViewModel.class);

        adapter = new ProjectAdapter(
                project -> openProject(project),
                project -> confirmDelete(project)
        );

        binding.recyclerProjects.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerProjects.setAdapter(adapter);

        viewModel.getAllProjects().observe(this, projects -> {
            adapter.submitList(projects);
            binding.emptyState.setVisibility(projects.isEmpty() ? View.VISIBLE : View.GONE);
        });

        binding.fabNewProject.setOnClickListener(v -> showNewProjectDialog());
        binding.btnNewProjectEmpty.setOnClickListener(v -> showNewProjectDialog());
    }

    private void showNewProjectDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_project, null);
        EditText etName = dialogView.findViewById(R.id.etProjectName);

        new AlertDialog.Builder(this)
                .setTitle(R.string.new_project)
                .setView(dialogView)
                .setPositiveButton(R.string.select_pdf, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, R.string.enter_name, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pendingProjectId = "p_" + UUID.randomUUID().toString();
                    pendingProjectName = name;
                    pdfPickerLauncher.launch(new String[]{"application/pdf"});
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void savePdfAndCreateProject(Uri uri, String projectId, String projectName) {
        new Thread(() -> {
            String fileName = PdfStorage.copyPdfToStorage(this, uri, projectId);
            runOnUiThread(() -> createProject(projectId, projectName, fileName));
        }).start();
    }

    private void createProject(String id, String name, String pdfFileName) {
        Project project = new Project(id, name);
        project.pdfFileName = pdfFileName;
        viewModel.insert(project);

        Intent intent = new Intent(this, ProjectReaderActivity.class);
        intent.putExtra(ProjectReaderActivity.EXTRA_PROJECT_ID, id);
        startActivity(intent);
    }

    private void openProject(Project project) {
        Intent intent = new Intent(this, ProjectReaderActivity.class);
        intent.putExtra(ProjectReaderActivity.EXTRA_PROJECT_ID, project.id);
        startActivity(intent);
    }

    private void confirmDelete(Project project) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_project)
                .setMessage(getString(R.string.delete_confirm, project.name))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    PdfStorage.deletePdf(this, project.id);
                    viewModel.delete(project);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
