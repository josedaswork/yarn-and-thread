package com.yarnandthread.app.ui;

import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.yarnandthread.app.R;
import com.yarnandthread.app.adapter.CounterAdapter;
import com.yarnandthread.app.databinding.ActivityProjectReaderBinding;
import com.yarnandthread.app.model.Annotation;
import com.yarnandthread.app.model.Counter;
import com.yarnandthread.app.model.Project;
import com.yarnandthread.app.util.PdfStorage;
import com.yarnandthread.app.util.PdfRenderer2Helper;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectReaderActivity extends AppCompatActivity {

    public static final String EXTRA_PROJECT_ID = "project_id";

    // Tools
    public static final int TOOL_NONE = 0;
    public static final int TOOL_HIGHLIGHT = 1;
    public static final int TOOL_NOTE = 2;
    public static final int TOOL_ERASER = 3;

    private ActivityProjectReaderBinding binding;
    private ProjectViewModel viewModel;
    private CounterAdapter counterAdapter;
    private PdfRenderer2Helper pdfHelper;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private Project currentProject;
    private int activeTool = TOOL_NONE;
    private String activeColor = "yellow";

    private final ActivityResultLauncher<String[]> pdfReloadLauncher =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null && currentProject != null) {
                executor.execute(() -> {
                    String fileName = PdfStorage.copyPdfToStorage(this, uri, currentProject.id);
                    currentProject.pdfFileName = fileName;
                    viewModel.update(currentProject);
                    runOnUiThread(this::loadPdf);
                });
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        if (projectId == null) { finish(); return; }

        viewModel = new ViewModelProvider(this,
                new ProjectViewModel.Factory(getApplication()))
                .get(ProjectViewModel.class);

        // Load project from DB (background)
        viewModel.getProjectById(projectId, project -> {
            currentProject = project;
            runOnUiThread(this::initUi);
        });
    }

    private void initUi() {
        if (currentProject == null) { finish(); return; }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentProject.name);
        }

        setupCounters();
        setupToolbar();
        setupPdfView();
        loadPdf();
    }

    // ========== COUNTERS ==========

    private void setupCounters() {
        counterAdapter = new CounterAdapter(
                this::onCounterIncrement,
                this::onCounterDecrement,
                this::onCounterRename,
                this::onCounterSetMax,
                this::onCounterLink,
                this::onCounterRemove
        );
        binding.rvCounters.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvCounters.setAdapter(counterAdapter);
        counterAdapter.submitList(currentProject.counters);

        binding.btnAddCounter.setOnClickListener(v -> showAddCounterDialog());
    }

    private void showAddCounterDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_counter, null);
        EditText etName = view.findViewById(R.id.etCounterName);
        EditText etMax  = view.findViewById(R.id.etCounterMax);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_counter)
                .setView(view)
                .setPositiveButton(R.string.add, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) name = "Counter";
                    int max = 0;
                    try { max = Integer.parseInt(etMax.getText().toString().trim()); } catch (Exception ignored) {}
                    Counter c = new Counter(name);
                    c.max = max;
                    currentProject.counters.add(c);
                    saveProject();
                    refreshCounters();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onCounterIncrement(int idx) {
        incrementCounter(idx, 0);
        saveProject();
        refreshCounters();
    }

    private void incrementCounter(int idx, int depth) {
        if (depth > 20 || idx < 0 || idx >= currentProject.counters.size()) return;
        Counter c = currentProject.counters.get(idx);
        c.value++;
        if (c.hasMax() && c.value >= c.max) {
            c.value = 0;
            if (c.hasLink()) incrementCounter(c.linkedTo, depth + 1);
        }
    }

    private void onCounterDecrement(int idx) {
        Counter c = currentProject.counters.get(idx);
        c.value = Math.max(0, c.value - 1);
        saveProject();
        refreshCounters();
    }

    private void onCounterRename(int idx) {
        EditText et = new EditText(this);
        et.setText(currentProject.counters.get(idx).name);
        et.selectAll();
        new AlertDialog.Builder(this)
                .setTitle(R.string.rename_counter)
                .setView(et)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (!TextUtils.isEmpty(name)) {
                        currentProject.counters.get(idx).name = name.length() > 20 ? name.substring(0, 20) : name;
                        saveProject();
                        refreshCounters();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onCounterSetMax(int idx) {
        EditText et = new EditText(this);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        Counter c = currentProject.counters.get(idx);
        if (c.hasMax()) et.setText(String.valueOf(c.max));
        et.setHint(R.string.blank_to_remove);
        new AlertDialog.Builder(this)
                .setTitle(R.string.set_maximum)
                .setView(et)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String s = et.getText().toString().trim();
                    int val = 0;
                    try { val = Integer.parseInt(s); } catch (Exception ignored) {}
                    c.max = (val > 0) ? val : 0;
                    if (!c.hasMax()) c.linkedTo = -1;
                    saveProject();
                    refreshCounters();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onCounterLink(int idx) {
        Counter c = currentProject.counters.get(idx);
        if (!c.hasMax()) {
            Toast.makeText(this, R.string.set_max_first, Toast.LENGTH_SHORT).show();
            return;
        }
        List<Counter> others = currentProject.counters;
        String[] names = others.stream()
                .filter(co -> co != c)
                .map(co -> co.name)
                .toArray(String[]::new);
        int[] indices = new int[names.length];
        int j = 0;
        for (int i = 0; i < others.size(); i++) {
            if (i != idx) indices[j++] = i;
        }
        if (names.length == 0) {
            Toast.makeText(this, R.string.add_more_counters, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.link_counter)
                .setItems(names, (d, which) -> {
                    c.linkedTo = indices[which];
                    saveProject();
                    refreshCounters();
                })
                .setNeutralButton(R.string.remove_link, (d, w) -> {
                    c.linkedTo = -1;
                    saveProject();
                    refreshCounters();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onCounterRemove(int idx) {
        // Fix linked indices
        for (Counter co : currentProject.counters) {
            if (co.linkedTo == idx) co.linkedTo = -1;
            else if (co.linkedTo > idx) co.linkedTo--;
        }
        currentProject.counters.remove(idx);
        saveProject();
        refreshCounters();
    }

    private void refreshCounters() {
        counterAdapter.submitList(null);
        counterAdapter.submitList(currentProject.counters);
    }

    // ========== PDF TOOLBAR ==========

    private void setupToolbar() {
        binding.btnHighlight.setOnClickListener(v -> setTool(TOOL_HIGHLIGHT));
        binding.btnNote.setOnClickListener(v -> setTool(TOOL_NOTE));
        binding.btnEraser.setOnClickListener(v -> setTool(TOOL_ERASER));
        binding.btnPrevPage.setOnClickListener(v -> { if (pdfHelper != null) { pdfHelper.prevPage(); updatePageInfo(); } });
        binding.btnNextPage.setOnClickListener(v -> { if (pdfHelper != null) { pdfHelper.nextPage(); updatePageInfo(); } });
        binding.btnZoomIn.setOnClickListener(v -> { if (pdfHelper != null) { pdfHelper.zoomIn(); } });
        binding.btnZoomOut.setOnClickListener(v -> { if (pdfHelper != null) { pdfHelper.zoomOut(); } });

        // Color buttons
        binding.colorYellow.setOnClickListener(v -> selectColor("yellow"));
        binding.colorPink.setOnClickListener(v -> selectColor("pink"));
        binding.colorGreen.setOnClickListener(v -> selectColor("green"));
        binding.colorBlue.setOnClickListener(v -> selectColor("blue"));
        selectColor("yellow");
    }

    private void setTool(int tool) {
        activeTool = (activeTool == tool) ? TOOL_NONE : tool;
        binding.btnHighlight.setSelected(activeTool == TOOL_HIGHLIGHT);
        binding.btnNote.setSelected(activeTool == TOOL_NOTE);
        binding.btnEraser.setSelected(activeTool == TOOL_ERASER);
        if (pdfHelper != null) pdfHelper.setActiveTool(activeTool, activeColor);

        int vis = (activeTool != TOOL_NONE) ? View.VISIBLE : View.GONE;
        binding.tvModeIndicator.setVisibility(vis);
        String label = activeTool == TOOL_HIGHLIGHT ? "HIGHLIGHT MODE"
                : activeTool == TOOL_NOTE ? "NOTE MODE"
                : activeTool == TOOL_ERASER ? "ERASER MODE" : "";
        binding.tvModeIndicator.setText(label);
    }

    private void selectColor(String color) {
        activeColor = color;
        binding.colorYellow.setSelected(color.equals("yellow"));
        binding.colorPink.setSelected(color.equals("pink"));
        binding.colorGreen.setSelected(color.equals("green"));
        binding.colorBlue.setSelected(color.equals("blue"));
        if (pdfHelper != null) pdfHelper.setActiveColor(color);
    }

    // ========== PDF ==========

    private void setupPdfView() {
        pdfHelper = new PdfRenderer2Helper(
                this,
                binding.pdfScrollView,
                binding.pdfContainer,
                currentProject,
                annotation -> {
                    currentProject.annotations.add(annotation);
                    saveProject();
                },
                annotation -> {
                    currentProject.annotations.remove(annotation);
                    saveProject();
                },
                page -> {
                    currentProject.currentPage = page;
                    saveProject();
                    updatePageInfo();
                }
        );
    }

    private void loadPdf() {
        if (currentProject.pdfFileName == null) {
            showNoPdfState();
            return;
        }
        File pdfFile = PdfStorage.getPdfFile(this, currentProject.pdfFileName);
        if (pdfFile == null || !pdfFile.exists()) {
            showNoPdfState();
            return;
        }
        binding.noPdfState.setVisibility(View.GONE);
        pdfHelper.loadPdf(pdfFile, currentProject.currentPage, currentProject.zoom);
        updatePageInfo();
    }

    private void showNoPdfState() {
        binding.noPdfState.setVisibility(View.VISIBLE);
        binding.btnLoadPdf.setOnClickListener(v ->
                pdfReloadLauncher.launch(new String[]{"application/pdf"}));
    }

    private void updatePageInfo() {
        if (pdfHelper == null) return;
        binding.tvPageInfo.setText(pdfHelper.getCurrentPage() + " / " + pdfHelper.getPageCount());
    }

    private void saveProject() {
        if (currentProject != null) viewModel.update(currentProject);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentProject != null && pdfHelper != null) {
            currentProject.currentPage = pdfHelper.getCurrentPage();
            currentProject.zoom = pdfHelper.getCurrentZoom();
            saveProject();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfHelper != null) pdfHelper.close();
        executor.shutdown();
    }
}
