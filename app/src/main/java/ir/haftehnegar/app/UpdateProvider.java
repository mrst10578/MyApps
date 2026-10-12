package ir.haftehnegar.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Read-only one-file provider; the installer receives a temporary URI grant. */
public final class UpdateProvider extends ContentProvider {
    public static final String AUTHORITY = "ir.haftehnegar.app.updates";
    public static final Uri APK_URI = Uri.parse("content://" + AUTHORITY + "/update.apk");

    private File checkedFile(Uri uri) throws FileNotFoundException {
        if (!APK_URI.equals(uri)) throw new FileNotFoundException("Invalid update URI");
        File f = new File(getContext().getCacheDir(), "update.apk");
        if (!f.isFile() || f.length() == 0) throw new FileNotFoundException("No APK ready");
        return f;
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return APK_URI.equals(uri) ? "application/vnd.android.package-archive" : null; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sortOrder) {
        try {
            File f = checkedFile(uri);
            MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
            c.addRow(new Object[]{"HaftehNegar-update.apk", f.length()});
            return c;
        } catch (FileNotFoundException e) { return null; }
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(checkedFile(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
