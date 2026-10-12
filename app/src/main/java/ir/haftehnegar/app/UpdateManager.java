package ir.haftehnegar.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/** Release checks are optional; APK installation is never performed by this app. */
public final class UpdateManager {
    private static final String API = "https://api.github.com/repos/mrst10578/MyApps/releases/latest";
    private static final String RELEASES = "https://github.com/mrst10578/MyApps/releases/tag/";
    private final Activity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final AtomicBoolean busy = new AtomicBoolean(false);
    UpdateManager(Activity activity) { this.activity = activity; }

    public void check(boolean manual) {
        if (!busy.compareAndSet(false, true)) {
            if (manual) toast("بررسی نسخه جدید در حال انجام است");
            return;
        }
        executor.execute(() -> {
            try {
                JSONObject release = new JSONObject(fetch());
                String tag = release.optString("tag_name", "");
                if (!tag.matches("v[0-9]+\\.[0-9]+\\.[0-9]+")
                    || release.optBoolean("draft") || release.optBoolean("prerelease"))
                    throw new Exception("Invalid release metadata");
                PackageInfo pkg = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                String current = pkg.versionName;
                String newer = tag.substring(1);
                if (compare(newer, current) <= 0) {
                    if (manual) toast("برنامه به‌روز است: " + current);
                    return;
                }
                JSONArray assets = release.optJSONArray("assets");
                boolean found = false;
                if (assets != null) for (int i = 0; i < assets.length(); i++)
                    if ("HaftehNegar-release.apk".equals(assets.getJSONObject(i).optString("name")))
                        found = true;
                if (!found) throw new Exception("Missing official APK");
                final String validTag = tag;
                ui.post(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    new AlertDialog.Builder(activity)
                        .setTitle("نسخه جدید هفته‌نگار: " + newer)
                        .setMessage("نسخه فعلی: " + current +
                            "\n\nاگر مایل باشی، صفحه رسمی انتشار در GitHub باز می‌شود. " +
                            "دانلود و نصب نسخه جدید فقط با انتخاب و تأیید خودت صورت می‌گیرد.")
                        .setNegativeButton("بعداً", null)
                        .setPositiveButton("مشاهده نسخه جدید", (d, w) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES + validTag));
                            try { activity.startActivity(intent); }
                            catch (Exception e) { toast("بازکردن مرورگر ممکن نشد"); }
                        }).show();
                });
            } catch (Exception error) {
                Log.w("HaftehUpdate", "Update check unsuccessful", error);
                if (manual) toast("بررسی آپدیت ممکن نشد؛ اینترنت را بررسی کن");
            } finally { busy.set(false); }
        });
    }
    private static int compare(String a, String b) throws Exception {
        if (!a.matches("[0-9]+\\.[0-9]+\\.[0-9]+") ||
            !b.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) throw new Exception("Invalid version");
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            long m = Long.parseLong(x[i]), n = Long.parseLong(y[i]);
            if (m != n) return Long.compare(m, n);
        }
        return 0;
    }
    private static String fetch() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(API).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(12000);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", "HaftehNegar-Version-Check");
        try {
            if (c.getResponseCode() != 200) throw new Exception("GitHub unavailable");
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (out.size() + n > 262144) throw new Exception("Response too large");
                    out.write(buffer, 0, n);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally { c.disconnect(); }
    }
    public void onResume() { }
    public void close() { executor.shutdownNow(); }
    private void toast(String message) {
        ui.post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed())
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        });
    }
}
