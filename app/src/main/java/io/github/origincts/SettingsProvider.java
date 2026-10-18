package io.github.origincts;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/** Public read-only feature flags, not general preference/file access. Only the app writes its private store. */
public final class SettingsProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (!SettingsStore.URI.equals(uri)) throw new IllegalArgumentException("Unknown settings URI");
        MatrixCursor cursor = new MatrixCursor(new String[]{SettingsStore.COLUMN});
        cursor.addRow(new Object[]{SettingsStore.read(getContext())});
        return cursor;
    }
    @Override public String getType(Uri uri) { return "vnd.android.cursor.item/vnd.origincts.features"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw readOnly(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw readOnly(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw readOnly(); }
    private SecurityException readOnly() { return new SecurityException("Settings are read-only outside OriginCTS"); }
}
