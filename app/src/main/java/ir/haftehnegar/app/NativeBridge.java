package ir.haftehnegar.app;

import android.webkit.JavascriptInterface;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Expose strictly scoped native persistence and export APIs to packaged WebView only. */
public final class NativeBridge {
    private final MainActivity activity;
    private final PlannerDb db;
    private ExportJob job;

    private static final class ExportJob {
        final String id, mime, name;
        final File file;
        FileOutputStream stream;
        ExportJob(String id, String mime, String name, File file, FileOutputStream stream) {
            this.id=id;this.mime=mime;this.name=name;this.file=file;this.stream=stream;
        }
    }
    NativeBridge(MainActivity activity, PlannerDb db) { this.activity=activity;this.db=db; }

    @JavascriptInterface public String getBootstrap() {
        try {
            JSONObject result = new JSONObject();
            result.put("library", new JSONArray(db.get("library", "[]")));
            result.put("themes", new JSONArray(db.get("themes", "[]")));
            String draft = db.get("draft", "null");
            result.put("draft", "null".equals(draft) ? JSONObject.NULL : new JSONObject(draft));
            result.put("currentId", db.get("currentId", ""));
            return result.toString();
        } catch (Exception e) { return "{\"library\":[],\"themes\":[],\"draft\":null,\"currentId\":\"\"}"; }
    }

    @JavascriptInterface public boolean saveDraft(String json, String id) {
        try { new JSONObject(json); if(json.length()>1_000_000)return false;
            return db.put("draft",json) && db.put("currentId",id);
        } catch (Exception e) { return false; }
    }
    @JavascriptInterface public boolean saveLibrary(String json) {
        try { JSONArray arr=new JSONArray(json); if(arr.length()>150 || json.length()>24_000_000)return false;
            return db.put("library",json);
        } catch (Exception e) { return false; }
    }
    @JavascriptInterface public boolean saveThemes(String json) {
        try {JSONArray arr=new JSONArray(json);if(arr.length()>40||json.length()>200_000)return false;
            return db.put("themes",json);
        } catch (Exception e) { return false; }
    }

    @JavascriptInterface public synchronized String beginExport(String name, String mime) {
        if(job!=null) return "";
        if(name==null || !name.matches("[A-Za-z0-9_.-]{1,180}"))return "";
        if(!("application/pdf".equals(mime)||"image/png".equals(mime)||"application/json;charset=utf-8".equals(mime)||"text/html;charset=utf-8".equals(mime)||"application/json".equals(mime)||"text/html".equals(mime)))return "";
        try {
            File tmp=File.createTempFile("haftehnegar-", ".part", activity.getCacheDir());
            String id=UUID.randomUUID().toString();
            job=new ExportJob(id,mime,name,tmp,new FileOutputStream(tmp));return id;
        }catch(IOException e){return "";}
    }
    @JavascriptInterface public synchronized boolean appendExportChunk(String id,String chunk) {
        if(job==null||!job.id.equals(id))return false;
        try {
            if(job.file.length()>40_000_000 || chunk.length()>100_000) return false;
            job.stream.write(Base64.decode(chunk,Base64.DEFAULT));return true;
        }catch(Exception e){return false;}
    }
    @JavascriptInterface public synchronized boolean finishExport(String id) {
        if(job==null||!job.id.equals(id))return false;
        final ExportJob complete=job;job=null;
        try {complete.stream.flush();complete.stream.close();}
        catch(Exception e){complete.file.delete();return false;}
        activity.runOnUiThread(()->activity.selectExportDestination(complete.id,complete.name,complete.mime,complete.file));
        return true;
    }
    @JavascriptInterface public synchronized void abortExport(String id) {
        if(job==null||!job.id.equals(id))return;
        try{job.stream.close();}catch(Exception ignored){}
        job.file.delete();job=null;
    }
    synchronized void abortAll(){if(job!=null)abortExport(job.id);}
}
