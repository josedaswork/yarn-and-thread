package com.yarnandthread.app.util;

import android.content.Context;
import android.net.Uri;
import java.io.*;

public class PdfStorage {

    private static File getPdfDir(Context context) {
        File dir = new File(context.getFilesDir(), "pdfs");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /**
     * Copies a PDF from a content URI into app-private storage.
     * @return the filename (not full path) under which it was saved, or null on error.
     */
    public static String copyPdfToStorage(Context context, Uri uri, String projectId) {
        String fileName = "pdf_" + projectId + ".pdf";
        File dest = new File(getPdfDir(context), fileName);
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dest)) {
            if (in == null) return null;
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
            return fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the File for a stored PDF filename, or null if it doesn't exist.
     */
    public static File getPdfFile(Context context, String fileName) {
        if (fileName == null) return null;
        File f = new File(getPdfDir(context), fileName);
        return f.exists() ? f : null;
    }

    /**
     * Deletes the PDF stored for a project.
     */
    public static void deletePdf(Context context, String projectId) {
        File f = new File(getPdfDir(context), "pdf_" + projectId + ".pdf");
        if (f.exists()) f.delete();
    }
}
