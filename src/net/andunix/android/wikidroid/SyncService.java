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

import java.net.URI;
import java.util.Date;
import java.util.HashMap;

import org.xmlrpc.android.XMLRPCClient;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class SyncService extends IntentService {
	
	private static final String TAG = "SyncService";
    private static final String[] ACTION_NAMES = {
    	"NONE",
    	"DOWNLOAD",
    	"UPLOAD",
    	"MERGE"
    };
    private static final int ACTION_NONE = 0;
    private static final int ACTION_DOWNLOAD = 1;
    private static final int ACTION_UPLOAD = 2;
    private static final int ACTION_MERGE = 3;
	
	private XMLRPCClient client;
	private DatabaseController database;
	
	public SyncService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		//
		// open database
		database = new DatabaseController(this).open();
		//
		// open XML-RPC connection
		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String wiki_url = prefs.getString(PrefsActivity.PREF_WIKI_URL, "<unset>");
			String wiki_user = prefs.getString(PrefsActivity.PREF_WIKI_USER, "");
			String wiki_passwd = prefs.getString(PrefsActivity.PREF_WIKI_PASSWD, "");
			Log.i(TAG, "opening connection: "+wiki_url);
			String xmlrpc_url = wiki_url;
			if (!xmlrpc_url.endsWith("/")) {
				xmlrpc_url += "/";
			}
			xmlrpc_url += "lib/exe/xmlrpc.php";
			Log.d(TAG, "xmlrpc_url="+xmlrpc_url);
			URI uri = URI.create(xmlrpc_url);
			if ((wiki_user == null) || (wiki_user.length() == 0)) {
				Log.d(TAG, "opening XML-RPC client with no credentials");
				client = new XMLRPCClient(uri);
			} else {
				Log.d(TAG, "opening XML-RPC client as "+wiki_user+"/"+wiki_passwd);
//				client = new XMLRPCClient(uri, wiki_user, wiki_passwd);
				client = new XMLRPCClient(uri);
				Boolean loginResult = (Boolean) client.call("dokuwiki.login", wiki_user, wiki_passwd);
				Log.d(TAG, "login: "+loginResult);
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}
	
	@Override
	public void onDestroy() {
		database.close();
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent i) {
		if (client == null) {
			Log.e(TAG, "XML-RPC Client was not created. Exiting Sync.");
			return;
		}
		database.createLog("sync started");
		Log.i(TAG, "sync started");
	    Toast.makeText(this, "sync started", Toast.LENGTH_SHORT).show();
		try {
			Object[] pages = (Object[]) client.call("wiki.getAllPages");
			for (Object o : pages) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> pageMap = (HashMap<String, Object>) o;
				String pageName = (String) pageMap.get("id");
				WikiPage page = database.findPageByName(pageName);
				if (page == null) {
					page = new WikiPage();
					page.name = pageName;
				}
				page.remoteChangedAt = (java.util.Date) pageMap.get("lastModified");
				int action = getAction(page);
				if (action != ACTION_NONE) {
					Log.d(TAG, page.name+": "+ACTION_NAMES[action]);
				}
				switch (action) {
				case ACTION_NONE:
					break;
				case ACTION_MERGE: // FIXME: implement merge action
//					Log.d(TAG, "WARNING: merge not implemented. Killing local changes!");
					Log.d(TAG, "WARNING: merge not implemented.");
					break;
				case ACTION_DOWNLOAD:
					downloadPageBody(page);
					page.localChangedAt = page.remoteChangedAt;
					page.syncedAt = page.remoteChangedAt;
					storePage(page);
					break;
				case ACTION_UPLOAD:
					if (uploadPage(page)) {
						page.syncedAt = page.localChangedAt;
						storePage(page);
					}
					break;
				default:
					Log.d(TAG, "ERROR: unknown action #"+action);
				}
//				storePage(page);
				database.createLog("sync finished");
			    Toast.makeText(this, "sync finished", Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			database.createLog("sync failed: "+e.getMessage());
		    Toast.makeText(this, "sync failed"+e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.e(TAG, e.getMessage(), e);
		}
		Log.i(TAG, "sync finished");
	    stopSelf();
	}
	
	private int getAction(WikiPage page) {
		int result = ACTION_NONE;
		final long localChangedTime = getTime(page.localChangedAt);
		final long remoteChangedTime = getTime(page.remoteChangedAt);
		final long syncTime = getTime(page.syncedAt);
		if (localChangedTime == 0L) {
			result = ACTION_DOWNLOAD;
		} else {
			if (remoteChangedTime > syncTime) {
				if (localChangedTime > syncTime) {
					result = ACTION_MERGE;
				} else {
					result = ACTION_DOWNLOAD;
				}
			} else {
				if (localChangedTime > syncTime) {
					result = ACTION_UPLOAD;
				} else {
					result = ACTION_NONE;
				}
			}
		}
		return result;
	}
	
	private void downloadPageBody(WikiPage page) {
		Log.d(TAG, "--- downloadPageBody("+page.name+") ---");
		try {
			page.body = (String) client.call("wiki.getPage", page.name);
			Log.d(TAG, "page content:\n"+page.body);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}
	
	private boolean uploadPage(WikiPage page) {
		boolean result = true;
		Log.d(TAG, "--- uploadPage("+page.name+") ---");
		try {
			HashMap<String, String> attrs = new HashMap<String, String>();
//			attrs.put("sum", "sync upload");
			Integer callResult = (Integer) client.call("wiki.putPage", page.name, page.body, attrs);
			result = callResult == 0;
			Log.d(TAG, "result: "+result);
			return result;
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		}
	}
	
	private void storePage(WikiPage page) {
		if (page.id == 0) {
			database.createPage(page);
		} else {
			database.updatePage(page);
		}
	}
	
	private long getTime(Date date) {
		return date == null ? 0L : date.getTime();
	}
	
}
