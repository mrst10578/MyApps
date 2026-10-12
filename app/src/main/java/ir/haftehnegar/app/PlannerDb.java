package ir.haftehnegar.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Durable offline key-value document store in private app SQLite. */
final class PlannerDb extends SQLiteOpenHelper {
    PlannerDb(Context context) { super(context, "haftehnegar.sqlite", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS documents (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future schema changes must preserve existing plans.
    }

    synchronized String get(String key, String fallback) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT value FROM documents WHERE key=?", new String[] {key})) {
            return c.moveToFirst() ? c.getString(0) : fallback;
        } catch (Exception e) { return fallback; }
    }

    synchronized boolean put(String key, String value) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues data = new ContentValues();
            data.put("key", key);
            data.put("value", value);
            if (db.insertWithOnConflict("documents", null, data, SQLiteDatabase.CONFLICT_REPLACE) == -1) return false;
            db.setTransactionSuccessful();
        } catch (Exception e) {
            android.util.Log.e("HaftehNegar", "SQLite write failed", e);
            return false;
        } finally {
            if (db.inTransaction()) db.endTransaction();
        }
        return value.equals(get(key, ""));
    }
}
