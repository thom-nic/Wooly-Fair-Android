package org.thomnichols.android.wooly;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class Provider extends ContentProvider {

	static final String TAG = "WOOLY PROVIDER";

	static final String AUTHORITY = "org.thomnichols.android.wooly";

	private static final int TARGETS_URI = 1;
	private static final int TARGET_ID_URI = 2;

	private static final UriMatcher sUriMatcher;

	private static Map<String, String> targetProjectionMap;

	private Database dbHelper = null;

	@Override
	public boolean onCreate() {
		dbHelper = new Database(getContext());
		return true;
	}
	
	public static SQLiteDatabase getReadableDatabase(Context ctx) {
		return ((Provider)ctx.getContentResolver()
				.acquireContentProviderClient(AUTHORITY)
				.getLocalContentProvider())
				.dbHelper.getReadableDatabase();
	}
	
	public static SQLiteDatabase getWritableDatabase(Context ctx) {
		return ((Provider)ctx.getContentResolver()
				.acquireContentProviderClient(AUTHORITY)
				.getLocalContentProvider())
				.dbHelper.getWritableDatabase();
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		// Log.d(TAG, "Managed query: " + uri);
		String groupBy = null;
		String orderBy = null;
		String limit = null;
		switch (sUriMatcher.match(uri)) {
		case TARGETS_URI:
			qb.setTables(Target.TABLE_NAME);
			qb.setProjectionMap(targetProjectionMap);
			break;
		case TARGET_ID_URI:
			qb.setTables(Target.TABLE_NAME);
			qb.setProjectionMap(targetProjectionMap);
			qb.appendWhere(Target.Cols._ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Get the database and run the query
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy,
				null, orderBy, limit);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		String table = null;
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		// TODO normalize/ validate/ add default values
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (sUriMatcher.match(uri)) {
		case TARGETS_URI:
			table = Target.TABLE_NAME;
			break;
		}

		long rowId = db.insert(table, null, values);
		if (rowId > 0) {
			uri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		String table;
		switch (sUriMatcher.match(uri)) {
		case TARGETS_URI:
			table = Target.TABLE_NAME;
			break;
		case TARGET_ID_URI:
			table = Target.TABLE_NAME;
			String targetID = uri.getLastPathSegment();
			// TODO ignore existing where clause for now...
			where = Target.Cols._ID + "=?";
			whereArgs = new String[] { targetID };
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		count = db.delete(table, where, whereArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		String tableName = null;
		switch (sUriMatcher.match(uri)) {
		case TARGETS_URI: 
			// TODO ID must be in where clause if not in URI!
		case TARGET_ID_URI:
			tableName = Target.TABLE_NAME;
			String targetID = uri.getLastPathSegment();
			// TODO ignore existing where clause for now...
			where = Target.Cols._ID + "=?";
			whereArgs = new String[] { targetID };
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		int count = db.update(tableName, values, where, whereArgs);
		if ( count > 0 ) getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case TARGETS_URI:
			return Target.CONTENT_TYPE;
		case TARGET_ID_URI:
			return Target.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "target", TARGETS_URI);
		sUriMatcher.addURI(AUTHORITY, "target/#", TARGET_ID_URI);

		targetProjectionMap = new HashMap<String, String>();
		targetProjectionMap.put(Target.Cols._ID, Target.Cols._ID);
		targetProjectionMap.put(Target.Cols.TITLE, Target.Cols.TITLE);
		targetProjectionMap.put(Target.Cols.URL, Target.Cols.URL);
	}
}
