/*
 * Copyright 2011 Andreas Huber - http://andunix.net/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.andunix.android.wikidroid;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseController {

	private static final String TAG = "DatabaseController";

	private static final String DATABASE_NAME = "wikidroid";
	private static final int DATABASE_VERSION = 1;
	
	public static final String KEY_ROWID = "_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_TEXT = "text";
	public static final String KEY_CHANGED_AT = "changed_at";
	public static final String KEY_CREATED_AT = "created_at";
	public static final String KEY_REMOTE_CHANGED_AT = "remote_changed_at";
	public static final String KEY_SYNCED_AT = "synced_at";

	private static final String PAGE_TABLE = "pages";
	public static final String[] PAGE_TABLE_COLS = { KEY_ROWID, KEY_NAME,
		KEY_TEXT, KEY_CHANGED_AT, KEY_REMOTE_CHANGED_AT,
		KEY_SYNCED_AT };
	private static final String PAGE_TABLE_CREATE = "CREATE TABLE "
			+ PAGE_TABLE + " ("+KEY_ROWID+" integer primary key autoincrement, "
			+ KEY_NAME + " text not null, " + KEY_TEXT + " text not null, "
			+ KEY_CHANGED_AT + " date, " + KEY_REMOTE_CHANGED_AT
			+ " date, " + KEY_SYNCED_AT + " date);";

	private static final String LOG_TABLE = "log";
	public static final String[] LOG_TABLE_COLS = { KEY_ROWID, KEY_TEXT, KEY_CREATED_AT };
	private static final String LOG_TABLE_CREATE = "CREATE TABLE "
			+ LOG_TABLE + " ("+KEY_ROWID+" integer primary key autoincrement, "
			+ KEY_TEXT + " text not null, "
			+ KEY_CREATED_AT + " date);";

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(PAGE_TABLE_CREATE);
			db.execSQL(LOG_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			// FIXME: do real _upgrade_
			db.execSQL("DROP TABLE IF EXISTS " + PAGE_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE);
			onCreate(db);
		}
	}

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final Context mCtx;

	public DatabaseController(Context ctx) {
		this.mCtx = ctx;
	}

	public DatabaseController open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public long createPage(WikiPage page) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, page.name);
		initialValues.put(KEY_TEXT, page.body);
		initialValues.put(
				KEY_CHANGED_AT,
				page.localChangedAt == null ? 0L : page.localChangedAt
						.getTime());
		initialValues.put(
				KEY_REMOTE_CHANGED_AT,
				page.remoteChangedAt == null ? 0L : page.remoteChangedAt
						.getTime());
		initialValues.put(KEY_SYNCED_AT, page.syncedAt == null ? 0L
				: page.syncedAt.getTime());

		return mDb.insert(PAGE_TABLE, null, initialValues);
	}

	public boolean deletePage(long rowId) {
		return mDb.delete(PAGE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor fetchAllPages() {
		return mDb.query(PAGE_TABLE, PAGE_TABLE_COLS, null, null, null, null, null);
	}

	public WikiPage findPageByName(String pageName) {
		WikiPage result = null;
		Cursor cursor = mDb.query(true, PAGE_TABLE, PAGE_TABLE_COLS, KEY_NAME
				+ "=?", new String[] { pageName }, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			if (!cursor.isAfterLast()) {
				result = new WikiPage();
				loadPage(result, cursor);
			}
			cursor.close();
		}
		return result;
	}

	public WikiPage getPage(long rowId) throws SQLException {
		WikiPage result = null;
		Cursor cursor = mDb.query(true, PAGE_TABLE, PAGE_TABLE_COLS,
				KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			if (!cursor.isAfterLast()) {
				result = new WikiPage();
				loadPage(result, cursor);
			}
			cursor.close();
		}
		return result;
	}

	private void loadPage(WikiPage page, Cursor cursor) {
		page.id = cursor.getLong(cursor.getColumnIndex(KEY_ROWID));
		page.name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
		page.body = cursor.getString(cursor.getColumnIndex(KEY_TEXT));
		page.localChangedAt = new Date(cursor.getLong(cursor
				.getColumnIndex(KEY_CHANGED_AT)));
		page.remoteChangedAt = new Date(cursor.getLong(cursor
				.getColumnIndex(KEY_REMOTE_CHANGED_AT)));
		page.syncedAt = new Date(cursor.getLong(cursor
				.getColumnIndex(KEY_SYNCED_AT)));
	}

	public boolean updatePage(WikiPage page) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, page.name);
		args.put(KEY_TEXT, page.body);
		args.put(KEY_CHANGED_AT, page.localChangedAt.getTime());
		args.put(KEY_REMOTE_CHANGED_AT, page.remoteChangedAt.getTime());
		args.put(KEY_SYNCED_AT, page.syncedAt.getTime());

		return mDb.update(PAGE_TABLE, args, KEY_ROWID + "=" + page.id, null) > 0;
	}

	public void updatePageBody(Long rowId, String body) {
		WikiPage page = getPage(rowId);
		page.body = body;
		page.localChangedAt = new Date();
		updatePage(page);
	}
	
	public long createLog(String text) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TEXT, text);
		initialValues.put(KEY_CREATED_AT, new Date().getTime());
		return mDb.insert(LOG_TABLE, null, initialValues);
	}

}
