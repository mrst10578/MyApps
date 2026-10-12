package ir.haftehnegar.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/** Offline Android shell with real SQLite and Android Storage Access Framework. */
public final class MainActivity extends Activity {
    private static final int EXPORT_REQUEST = 1601;
    private static final int IMPORT_REQUEST = 1602;
    private WebView web;
    private PlannerDb db;
    private NativeBridge bridge;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingExportId;
    private File pendingExportFile;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(17, 27, 54));
        getWindow().setNavigationBarColor(Color.rgb(245, 247, 252));
        if (Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        db = new PlannerDb(this);
        bridge = new NativeBridge(this, db);
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(245, 247, 252));
        setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); // fallback only; SQLite is authoritative
        settings.setAllowFileAccess(true); // needed for packaged file:///android_asset/index.html
        settings.setAllowContentAccess(true); // used by JSON backup imports
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(false);
        settings.setDefaultTextEncodingName("UTF-8");
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        web.addJavascriptInterface(bridge, "HaftehNative");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return !url.startsWith("file:///android_asset/");
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                if ("https".equals(scheme) || "http".equals(scheme)) {
                    return new WebResourceResponse("text/plain", "UTF-8", new java.io.ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                try { startActivityForResult(intent, IMPORT_REQUEST); return true; }
                catch (Exception e) { fileCallback.onReceiveValue(null); fileCallback = null; return false; }
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }

    /** Runs on UI thread, launched from @JavascriptInterface. */
    void selectExportDestination(String id, String fileName, String mimeType, File temporaryFile) {
        if (pendingExportId != null) {
            notifyExport(id, false, "یک خروجی دیگر در حال ذخیره است");
            return;
        }
        pendingExportId = id;
        pendingExportFile = temporaryFile;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        try { startActivityForResult(intent, EXPORT_REQUEST); }
        catch (Exception e) { finishExport(false, "پنجره ذخیره اندروید در دسترس نیست"); }
    }

    private void notifyExport(String id, boolean success, String reason) {
        if (web == null || id == null) return;
        final String script = "window.haftehNativeExportResult(" + org.json.JSONObject.quote(id) + "," + success + "," + org.json.JSONObject.quote(reason) + ")";
        web.evaluateJavascript(script, null);
    }

    private void finishExport(boolean success, String message) {
        String id = pendingExportId;
        if (pendingExportFile != null) pendingExportFile.delete();
        pendingExportId = null;
        pendingExportFile = null;
        notifyExport(id, success, message);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_REQUEST) {
            if (fileCallback != null) {
                Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null
                    ? new Uri[] {data.getData()} : null;
                fileCallback.onReceiveValue(result);
                fileCallback = null;
            }
            return;
        }
        if (requestCode != EXPORT_REQUEST) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            finishExport(false, "ذخیره خروجی لغو شد");
            return;
        }
        try (FileInputStream in = new FileInputStream(pendingExportFile);
             OutputStream out = getContentResolver().openOutputStream(data.getData(), "w")) {
            if (out == null) throw new Exception("مقصد خروجی قابل نوشتن نیست");
            byte[] buffer = new byte[65536];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            out.flush();
            finishExport(true, "ذخیره شد");
        } catch (Exception e) {
            finishExport(false, "خطا در ثبت فایل: " + e.getMessage());
        }
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (bridge != null) bridge.abortAll();
        if (web != null) { web.removeJavascriptInterface("HaftehNative"); web.destroy(); }
        db.close();
        super.onDestroy();
    }
}
