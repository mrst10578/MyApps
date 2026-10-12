package ir.haftehnegar.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import android.util.Log;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/** Internet is used only for opt-in GitHub Releases updates. Main application stays offline. */
public final class UpdateManager {
    private static final String API = "https://api.github.com/repos/mrst10578/MyApps/releases/latest";
    private static final String APK_NAME = "HaftehNegar-release.apk";
    private static final long MAX_APK = 150L * 1024 * 1024;
    private static final int MAX_CHECKSUM = 2048;
    private final Activity activity;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private volatile boolean canceled;
    private volatile boolean pendingInstall;
    private ProgressDialog progress;
    private File apk;

    UpdateManager(Activity activity) { this.activity = activity; }

    public void check(boolean manual) {
        if (!busy.compareAndSet(false, true)) {
            if (manual) toast("بررسی یا دانلود آپدیت در حال انجام است");
            return;
        }
        canceled = false;
        pool.execute(() -> {
            try {
                JSONObject release = new JSONObject(new String(fetch(API, 1_048_576), StandardCharsets.UTF_8));
                String tag = release.optString("tag_name", "");
                String remote = tag.startsWith("v") ? tag.substring(1) : tag;
                String current = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
                if (compareVersion(remote, current) <= 0 || release.optBoolean("draft") || release.optBoolean("prerelease")) {
                    if (manual) toast("هفته‌نگار به‌روز است (نسخه " + current + ")");
                    return;
                }
                JSONArray assets = release.getJSONArray("assets");
                String apkUrl = null, shaUrl = null;
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.getJSONObject(i);
                    if (APK_NAME.equals(a.optString("name"))) apkUrl = a.optString("browser_download_url");
                    if ((APK_NAME + ".sha256").equals(a.optString("name"))) shaUrl = a.optString("browser_download_url");
                }
                if (apkUrl == null || shaUrl == null) throw new Exception("نسخه منتشرشده فاقد APK یا فایل کنترل صحت است");
                final String downloadUrl = apkUrl, checksumUrl = shaUrl;
                ui.post(() -> new AlertDialog.Builder(activity)
                    .setTitle("آپدیت هفته‌نگار " + tag)
                    .setMessage("نسخه فعلی: " + current + "\nنسخه جدید: " + remote + "\n\nآرشیو برنامه‌ها در صورت یکسان بودن امضای نسخه‌ها حفظ می‌شود. دانلود و نصب با تأیید تو انجام می‌شود.")
                    .setNegativeButton("بعداً", (d, w) -> {})
                    .setPositiveButton("دانلود آپدیت", (d, w) -> download(downloadUrl, checksumUrl))
                    .show());
            } catch (Exception e) {
                Log.w("HaftehUpdater", "Update check failed", e);
                if (manual) toast("بررسی آپدیت ممکن نشد؛ اتصال اینترنت را بررسی کن");
            } finally { busy.set(false); }
        });
    }

    private static int compareVersion(String a, String b) throws Exception {
        if (!a.matches("[0-9]+\\.[0-9]+\\.[0-9]+") || !b.matches("[0-9]+\\.[0-9]+\\.[0-9]+"))
            throw new Exception("Invalid release version");
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            long u = Long.parseLong(x[i]), v = Long.parseLong(y[i]);
            if (u != v) return Long.compare(u, v);
        }
        return 0;
    }

    private void download(String apkUrl, String shaUrl) {
        if (!busy.compareAndSet(false, true)) { toast("یک عملیات دیگر در حال انجام است"); return; }
        canceled = false;
        progress = new ProgressDialog(activity);
        progress.setTitle("دریافت آپدیت");
        progress.setMessage("در حال دانلود امن فایل…");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setCancelable(true);
        progress.setMax(100);
        progress.setOnCancelListener(d -> canceled = true);
        progress.show();
        pool.execute(() -> {
            File temp = new File(activity.getCacheDir(), "update.apk.tmp");
            try {
                String checksumText = new String(fetch(shaUrl, MAX_CHECKSUM), StandardCharsets.UTF_8).trim();
                String checksum = checksumText.split("\\s+")[0];
                if (!checksum.matches("[0-9a-fA-F]{64}")) throw new Exception("هش فایل منتشرشده نامعتبر است");
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                HttpURLConnection con = connect(apkUrl);
                long length = con.getContentLengthLong();
                if (length > MAX_APK) throw new Exception("فایل بیش از حد بزرگ است");
                long done = 0;
                try (InputStream in = con.getInputStream(); FileOutputStream out = new FileOutputStream(temp)) {
                    byte[] buf = new byte[32768];
                    int n, previous = -1;
                    while ((n = in.read(buf)) != -1) {
                        if (canceled) throw new Exception("دانلود لغو شد");
                        done += n;
                        if (done > MAX_APK) throw new Exception("فایل بیش از حد بزرگ است");
                        sha.update(buf, 0, n);
                        out.write(buf, 0, n);
                        if (length > 0) {
                            int pc = (int)Math.min(100, done * 100 / length);
                            if (pc != previous) { previous = pc; final int value = pc; ui.post(() -> { if (progress != null) progress.setProgress(value); }); }
                        }
                    }
                    out.flush();
                } finally { con.disconnect(); }
                if (done < 10000) throw new Exception("فایل APK ناقص است");
                if (!toHex(sha.digest()).equalsIgnoreCase(checksum)) throw new Exception("تطبیق SHA-256 فایل ناموفق بود");
                verifyApk(temp);
                File destination = new File(activity.getCacheDir(), "update.apk");
                if (destination.exists() && !destination.delete()) throw new Exception("پاک‌سازی آپدیت قبلی ممکن نیست");
                if (!temp.renameTo(destination)) throw new Exception("ذخیره APK دریافت‌شده ناموفق بود");
                apk = destination;
                ui.post(() -> {
                    dismissProgress();
                    new AlertDialog.Builder(activity).setTitle("آپدیت آماده نصب است")
                        .setMessage("فایل بررسی شد. برای نصب نسخه جدید، تأیید اندروید لازم است. برنامه‌های ذخیره‌شده در به‌روزرسانی معمولی باقی می‌مانند.")
                        .setNegativeButton("بعداً", (d, w) -> {})
                        .setPositiveButton("نصب", (d, w) -> install()).show();
                });
            } catch (Exception e) {
                Log.w("HaftehUpdater", "Update download failed", e);
                temp.delete();
                ui.post(() -> {
                    dismissProgress();
                    if (!canceled) new AlertDialog.Builder(activity).setTitle("آپدیت نصب نشد")
                        .setMessage(e.getMessage()).setPositiveButton("باشه", null).show();
                });
            } finally { busy.set(false); }
        });
    }

    private void verifyApk(File file) throws Exception {
        PackageManager pm = activity.getPackageManager();
        int flag = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo archive = pm.getPackageArchiveInfo(file.getAbsolutePath(), flag);
        if (archive == null || !activity.getPackageName().equals(archive.packageName))
            throw new Exception("شناسه APK با هفته‌نگار سازگار نیست");
        PackageInfo installed = pm.getPackageInfo(activity.getPackageName(), flag);
        if (Build.VERSION.SDK_INT >= 28) {
            if (archive.signingInfo == null || installed.signingInfo == null) throw new Exception("امضای فایل بررسی نشد");
            Signature[] incoming = archive.signingInfo.getApkContentsSigners();
            Signature[] active = installed.signingInfo.getApkContentsSigners();
            if (!sameSigners(incoming, active)) throw new Exception("امضای نسخه نصب‌شده با آپدیت متفاوت است. ابتدا از آرشیو پشتیبان JSON بگیر، سپس نسخه رسمی را جداگانه نصب کن.");
        } else {
            if (!sameSigners(archive.signatures, installed.signatures)) throw new Exception("امضای نسخه فعلی و آپدیت متفاوت است؛ از آرشیو پشتیبان تهیه کن");
        }
        long nextCode = Build.VERSION.SDK_INT >= 28 ? archive.getLongVersionCode() : archive.versionCode;
        long currentCode = Build.VERSION.SDK_INT >= 28 ? installed.getLongVersionCode() : installed.versionCode;
        if (nextCode <= currentCode) throw new Exception("شماره ساخت نسخه جدید بالاتر از نسخه نصب‌شده نیست");
    }
    private static boolean sameSigners(Signature[] a, Signature[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) return false;
        String[] aa = new String[a.length], bb = new String[b.length];
        for (int i = 0; i < a.length; i++) aa[i] = a[i].toCharsString();
        for (int i = 0; i < b.length; i++) bb[i] = b[i].toCharsString();
        Arrays.sort(aa); Arrays.sort(bb);
        return Arrays.equals(aa, bb);
    }

    private void install() {
        if (apk == null || !apk.isFile()) { toast("فایل آپدیت پیدا نشد"); return; }
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            pendingInstall = true;
            new AlertDialog.Builder(activity).setTitle("مجوز نصب نسخه جدید")
                .setMessage("برای نصب APK از داخل هفته‌نگار، باید اجازه نصب از این منبع را در تنظیمات اندروید فعال کنی. این مجوز فقط برای آپدیت استفاده می‌شود.")
                .setNegativeButton("لغو", (d, w) -> pendingInstall = false)
                .setPositiveButton("تنظیمات", (d, w) -> {
                    Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName()));
                    try { activity.startActivity(settings); } catch (Exception ex) { pendingInstall = false; toast("تنظیمات نصب در دسترس نیست"); }
                }).show();
            return;
        }
        pendingInstall = false;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(UpdateProvider.APK_URI, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try { activity.startActivity(intent); }
        catch (Exception e) { toast("نصب‌کننده اندروید در دسترس نیست"); }
    }
    void onResume() {
        if (pendingInstall && (Build.VERSION.SDK_INT < 26 || activity.getPackageManager().canRequestPackageInstalls())) install();
    }
    void close() { canceled = true; pool.shutdownNow(); dismissProgress(); }
    private void dismissProgress() { if (progress != null) { progress.dismiss(); progress = null; } }
    private void toast(String msg) { ui.post(() -> Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()); }
    private static String toHex(byte[] data) {
        char[] digits="0123456789abcdef".toCharArray(), out=new char[data.length*2];
        for(int i=0;i<data.length;i++){out[i*2]=digits[(data[i]>>4)&15];out[i*2+1]=digits[data[i]&15];}
        return new String(out);
    }
    private static byte[] fetch(String url, int maxBytes) throws Exception {
        HttpURLConnection c = connect(url);
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) != -1) { if (out.size() + n > maxBytes) throw new Exception("پاسخ آپدیت بیش از حد مجاز است"); out.write(buf, 0, n); }
            return out.toByteArray();
        } finally { c.disconnect(); }
    }
    private static HttpURLConnection connect(String url) throws Exception {
        for (int redirects = 0; redirects < 7; redirects++) {
            URL u = new URL(url);
            String host = u.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(u.getProtocol()) ||
                !(host.equals("github.com") || host.equals("api.github.com") || host.endsWith(".githubusercontent.com")))
                throw new Exception("نشانی دانلود نامعتبر است");
            HttpURLConnection c = (HttpURLConnection)u.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(12000); c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", "HaftehNegar-Android-Updater");
            c.setRequestProperty("Accept", "application/vnd.github+json");
            int status = c.getResponseCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = c.getHeaderField("Location"); c.disconnect();
                if (location == null) throw new Exception("آدرس انتقال دانلود مشخص نیست");
                url = new URL(u, location).toString();
                continue;
            }
            if (status != 200) { c.disconnect(); throw new Exception("سرور نسخه‌ها در دسترس نیست (" + status + ")"); }
            return c;
        }
        throw new Exception("تعداد انتقال‌های دانلود بیش از حد مجاز است");
    }
}
