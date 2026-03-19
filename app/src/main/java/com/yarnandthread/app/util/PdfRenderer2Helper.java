package com.yarnandthread.app.util;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.yarnandthread.app.model.Annotation;
import com.yarnandthread.app.model.Project;
import com.yarnandthread.app.ui.ProjectReaderActivity;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages PDF rendering using Android's built-in PdfRenderer.
 * Handles multi-page display, zoom, and annotation overlays.
 */
public class PdfRenderer2Helper {

    public interface OnAnnotationAdded    { void onAdded(Annotation a); }
    public interface OnAnnotationRemoved  { void onRemoved(Annotation a); }
    public interface OnPageChanged        { void onChanged(int page); }

    private final Context context;
    private final ScrollView scrollView;
    private final LinearLayout container;
    private final Project project;
    private final OnAnnotationAdded onAdded;
    private final OnAnnotationRemoved onRemoved;
    private final OnPageChanged onPageChanged;

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pfd;
    private int pageCount = 0;
    private int currentPage = 1;
    private float zoom = 1.3f;
    private int activeTool = ProjectReaderActivity.TOOL_NONE;
    private String activeColor = "yellow";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Map from page number → AnnotationOverlayView
    private final Map<Integer, AnnotationOverlayView> overlayMap = new HashMap<>();

    // Color maps matching the HTML app
    private static final Map<String, Integer> HIGHLIGHT_COLORS = new HashMap<String, Integer>() {{
        put("yellow", Color.argb(100, 255, 208, 60));
        put("pink",   Color.argb(95,  255, 128, 128));
        put("green",  Color.argb(95,  96,  200, 120));
        put("blue",   Color.argb(95,  96,  160, 220));
    }};
    private static final Map<String, Integer> NOTE_BG_COLORS = new HashMap<String, Integer>() {{
        put("yellow", Color.parseColor("#FFF8DC"));
        put("pink",   Color.parseColor("#FFE0E0"));
        put("green",  Color.parseColor("#E0F5E0"));
        put("blue",   Color.parseColor("#E0ECFF"));
    }};

    public PdfRenderer2Helper(Context context, ScrollView scrollView, LinearLayout container,
                              Project project,
                              OnAnnotationAdded onAdded,
                              OnAnnotationRemoved onRemoved,
                              OnPageChanged onPageChanged) {
        this.context = context;
        this.scrollView = scrollView;
        this.container = container;
        this.project = project;
        this.onAdded = onAdded;
        this.onRemoved = onRemoved;
        this.onPageChanged = onPageChanged;
    }

    public void loadPdf(File file, int startPage, float zoom) {
        this.zoom = zoom;
        this.currentPage = startPage;
        executor.execute(() -> {
            try {
                if (pdfRenderer != null) { pdfRenderer.close(); }
                if (pfd != null) { pfd.close(); }
                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                pdfRenderer = new PdfRenderer(pfd);
                pageCount = pdfRenderer.getPageCount();
                ((android.app.Activity) context).runOnUiThread(this::renderAllPages);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void renderAllPages() {
        container.removeAllViews();
        overlayMap.clear();
        for (int pn = 1; pn <= pageCount; pn++) {
            addPageView(pn);
        }
        // Scroll to saved page
        final int targetPage = currentPage;
        scrollView.post(() -> {
            View pageView = container.findViewWithTag("page_" + targetPage);
            if (pageView != null) {
                scrollView.scrollTo(0, pageView.getTop());
            }
        });
        setupScrollListener();
    }

    private void addPageView(int pageNumber) {
        // Frame to hold canvas + overlay
        FrameLayout frame = new FrameLayout(context);
        frame.setTag("page_" + pageNumber);

        ImageView pageImage = new ImageView(context);
        pageImage.setTag("img_" + pageNumber);
        pageImage.setScaleType(ImageView.ScaleType.FIT_XY);

        AnnotationOverlayView overlay = new AnnotationOverlayView(context, pageNumber);
        overlayMap.put(pageNumber, overlay);

        frame.addView(pageImage);
        frame.addView(overlay);

        // Render PDF page in background
        final int pn = pageNumber;
        executor.execute(() -> {
            Bitmap bmp = renderPage(pn);
            ((android.app.Activity) context).runOnUiThread(() -> {
                if (bmp != null) {
                    pageImage.setImageBitmap(bmp);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            bmp.getWidth(), bmp.getHeight());
                    lp.setMargins(16, 16, 16, 16);
                    frame.setLayoutParams(lp);
                    overlay.setLayoutParams(new FrameLayout.LayoutParams(bmp.getWidth(), bmp.getHeight()));
                    overlay.setPageSize(bmp.getWidth(), bmp.getHeight());
                    overlay.invalidate();
                }
            });
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 16, 16, 16);
        container.addView(frame, lp);
    }

    private Bitmap renderPage(int pageNumber) {
        if (pdfRenderer == null || pageNumber < 1 || pageNumber > pageCount) return null;
        PdfRenderer.Page page = pdfRenderer.openPage(pageNumber - 1);
        int width  = (int)(page.getWidth()  * zoom);
        int height = (int)(page.getHeight() * zoom);
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.WHITE);
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
        return bmp;
    }

    private void setupScrollListener() {
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollY = scrollView.getScrollY();
            for (int pn = 1; pn <= pageCount; pn++) {
                View v = container.findViewWithTag("page_" + pn);
                if (v != null && v.getTop() <= scrollY + scrollView.getHeight() / 2
                        && v.getBottom() >= scrollY) {
                    if (currentPage != pn) {
                        currentPage = pn;
                        if (onPageChanged != null) onPageChanged.onChanged(pn);
                    }
                    break;
                }
            }
        });
    }

    // ========== NAVIGATION ==========

    public void prevPage() {
        if (currentPage > 1) scrollToPage(currentPage - 1);
    }

    public void nextPage() {
        if (currentPage < pageCount) scrollToPage(currentPage + 1);
    }

    private void scrollToPage(int page) {
        currentPage = page;
        View v = container.findViewWithTag("page_" + page);
        if (v != null) scrollView.smoothScrollTo(0, v.getTop());
    }

    // ========== ZOOM ==========

    public void zoomIn() {
        zoom = Math.min(3.0f, zoom + 0.2f);
        rerenderAll();
    }

    public void zoomOut() {
        zoom = Math.max(0.5f, zoom - 0.2f);
        rerenderAll();
    }

    private void rerenderAll() {
        container.removeAllViews();
        overlayMap.clear();
        for (int pn = 1; pn <= pageCount; pn++) addPageView(pn);
        final int target = currentPage;
        scrollView.post(() -> {
            View v = container.findViewWithTag("page_" + target);
            if (v != null) scrollView.scrollTo(0, v.getTop());
        });
    }

    // ========== TOOLS ==========

    public void setActiveTool(int tool, String color) {
        this.activeTool = tool;
        this.activeColor = color;
        for (AnnotationOverlayView ov : overlayMap.values()) {
            ov.setTool(tool, color);
        }
    }

    public void setActiveColor(String color) {
        this.activeColor = color;
        for (AnnotationOverlayView ov : overlayMap.values()) {
            ov.setColor(color);
        }
    }

    // ========== GETTERS ==========

    public int getCurrentPage() { return currentPage; }
    public int getPageCount()   { return pageCount; }
    public float getCurrentZoom() { return zoom; }

    public void close() {
        executor.shutdown();
        try { if (pdfRenderer != null) pdfRenderer.close(); } catch (Exception ignored) {}
        try { if (pfd != null) pfd.close(); } catch (Exception ignored) {}
    }

    // ========== ANNOTATION OVERLAY VIEW ==========

    public class AnnotationOverlayView extends View {

        private final int pageNumber;
        private int pageW, pageH;
        private int tool = ProjectReaderActivity.TOOL_NONE;
        private String color = "yellow";

        // Drawing state
        private boolean isDrawing = false;
        private float startX, startY, endX, endY;

        private final Paint highlightPaint = new Paint();
        private final Paint previewPaint = new Paint();
        private final Paint noteBgPaint = new Paint();
        private final Paint noteTextPaint = new Paint();
        private final Paint eraserHoverPaint = new Paint();

        public AnnotationOverlayView(Context ctx, int pageNumber) {
            super(ctx);
            this.pageNumber = pageNumber;
            setClickable(true);
            setFocusable(true);

            previewPaint.setStyle(Paint.Style.STROKE);
            previewPaint.setColor(Color.argb(160, 80, 60, 0));
            previewPaint.setStrokeWidth(3);
            previewPaint.setPathEffect(new DashPathEffect(new float[]{12, 6}, 0));

            noteBgPaint.setStyle(Paint.Style.FILL);
            noteTextPaint.setColor(Color.parseColor("#2a1a08"));
            noteTextPaint.setTextSize(28);
            noteTextPaint.setAntiAlias(true);

            eraserHoverPaint.setColor(Color.argb(60, 220, 60, 60));
            eraserHoverPaint.setStyle(Paint.Style.FILL);
        }

        public void setPageSize(int w, int h) { pageW = w; pageH = h; }
        public void setTool(int t, String c) { tool = t; color = c; invalidate(); }
        public void setColor(String c) { color = c; }

        @Override
        protected void onDraw(Canvas canvas) {
            if (pageW == 0 || pageH == 0) return;

            // Draw saved annotations for this page
            for (Annotation ann : project.annotations) {
                if (ann.page != pageNumber) continue;
                if (Annotation.TYPE_HIGHLIGHT.equals(ann.type)) {
                    drawHighlight(canvas, ann);
                } else if (Annotation.TYPE_NOTE.equals(ann.type)) {
                    drawNote(canvas, ann);
                }
            }

            // Draw in-progress highlight preview
            if (isDrawing && tool == ProjectReaderActivity.TOOL_HIGHLIGHT) {
                float l = Math.min(startX, endX), r = Math.max(startX, endX);
                float t = Math.min(startY, endY), b = Math.max(startY, endY);
                canvas.drawRect(l, t, r, b, previewPaint);
            }
        }

        private void drawHighlight(Canvas canvas, Annotation ann) {
            highlightPaint.setStyle(Paint.Style.FILL);
            highlightPaint.setColor(getHighlightColor(ann.color));
            float l = ann.x * pageW, t = ann.y * pageH;
            float r = l + ann.w * pageW, b = t + ann.h * pageH;
            canvas.drawRect(l, t, r, b, highlightPaint);
        }

        private void drawNote(Canvas canvas, Annotation ann) {
            float x = ann.x * pageW, y = ann.y * pageH;
            float padding = 12, textW = 280;

            // Wrap text lines
            String[] words = ann.text.split(" ");
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                if (noteTextPaint.measureText(line + " " + w) > textW && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(w);
                } else {
                    if (line.length() > 0) line.append(" ");
                    line.append(w);
                }
            }
            if (line.length() > 0) lines.add(line.toString());

            float boxH = padding * 2 + lines.size() * 36;
            float boxW = textW + padding * 2;

            noteBgPaint.setColor(getNoteBgColor(ann.color));
            canvas.drawRect(x, y, x + boxW, y + boxH, noteBgPaint);

            // Border
            Paint borderPaint = new Paint();
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setColor(Color.parseColor("#c89b3c"));
            borderPaint.setStrokeWidth(3);
            canvas.drawRect(x, y, x + boxW, y + boxH, borderPaint);

            // Text
            for (int i = 0; i < lines.size(); i++) {
                canvas.drawText(lines.get(i), x + padding, y + padding + 30 + i * 36, noteTextPaint);
            }
        }

        private int getHighlightColor(String c) {
            Integer col = HIGHLIGHT_COLORS.get(c);
            return col != null ? col : HIGHLIGHT_COLORS.get("yellow");
        }

        private int getNoteBgColor(String c) {
            Integer col = NOTE_BG_COLORS.get(c);
            return col != null ? col : NOTE_BG_COLORS.get("yellow");
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (pageW == 0) return false;
            float x = event.getX(), y = event.getY();

            switch (tool) {
                case ProjectReaderActivity.TOOL_HIGHLIGHT:
                    return handleHighlightTouch(event, x, y);
                case ProjectReaderActivity.TOOL_NOTE:
                    return handleNoteTouch(event, x, y);
                case ProjectReaderActivity.TOOL_ERASER:
                    return handleEraserTouch(event, x, y);
                default:
                    return false;
            }
        }

        private boolean handleHighlightTouch(MotionEvent e, float x, float y) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDrawing = true; startX = x; startY = y; endX = x; endY = y; break;
                case MotionEvent.ACTION_MOVE:
                    endX = x; endY = y; invalidate(); break;
                case MotionEvent.ACTION_UP:
                    isDrawing = false;
                    float rx = Math.min(startX, endX) / pageW;
                    float ry = Math.min(startY, endY) / pageH;
                    float rw = Math.abs(endX - startX) / pageW;
                    float rh = Math.abs(endY - startY) / pageH;
                    if (rw > 0.01f && rh > 0.005f) {
                        Annotation ann = Annotation.highlight(pageNumber, rx, ry, rw, rh, color);
                        if (onAdded != null) onAdded.onAdded(ann);
                    }
                    invalidate(); break;
            }
            return true;
        }

        private boolean handleNoteTouch(MotionEvent e, float x, float y) {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                showNoteDialog(x / pageW, y / pageH);
            }
            return true;
        }

        private boolean handleEraserTouch(MotionEvent e, float x, float y) {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                // Find tapped annotation
                for (Annotation ann : new ArrayList<>(project.annotations)) {
                    if (ann.page != pageNumber) continue;
                    float ax = ann.x * pageW, ay = ann.y * pageH;
                    if (Annotation.TYPE_HIGHLIGHT.equals(ann.type)) {
                        float ar = ax + ann.w * pageW, ab = ay + ann.h * pageH;
                        if (x >= ax && x <= ar && y >= ay && y <= ab) {
                            if (onRemoved != null) onRemoved.onRemoved(ann);
                            invalidate();
                            return true;
                        }
                    } else if (Annotation.TYPE_NOTE.equals(ann.type)) {
                        if (Math.abs(x - ax) < 150 && Math.abs(y - ay) < 80) {
                            if (onRemoved != null) onRemoved.onRemoved(ann);
                            invalidate();
                            return true;
                        }
                    }
                }
            }
            return true;
        }

        private void showNoteDialog(float normX, float normY) {
            EditText et = new EditText(context);
            et.setHint("Your annotation here...");
            et.setMaxLines(4);
            new AlertDialog.Builder(context)
                    .setTitle("Add Note")
                    .setView(et)
                    .setPositiveButton("Pin Note", (d, w) -> {
                        String text = et.getText().toString().trim();
                        if (!text.isEmpty()) {
                            Annotation ann = Annotation.note(pageNumber, normX, normY, text, color);
                            if (onAdded != null) onAdded.onAdded(ann);
                            invalidate();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}
