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

import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.xmlrpc.android.XMLRPCClient;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class SyncActivity extends Activity {

	public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	
    private static final int MAX_LOG_LINES = 32;
    private static final int MENU_CANCEL_ID = Menu.FIRST;
    
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
	private TextView mSyncLogView;
	private StringBuilder mSyncLog;
	private Printer mLogPrinter = null;
	private int mSyncLogLine = 0;
	private int mSyncLogLines = 0;
	private DatabaseController mDatabaseController;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDatabaseController = new DatabaseController(this).open();
		setContentView(R.layout.sync);
		mSyncLog = new StringBuilder();
		mSyncLogLine = 0;
		mSyncLogLines = 0;
		mSyncLogView = (TextView) findViewById(R.id.textSyncLog);
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
			try {
				File logDir = new File(Environment.getExternalStorageDirectory(), "log");
				if (!logDir.exists()) {
					logDir.mkdirs();
				}
//				File logFile = File.createTempFile("sync_", ".log", logDir);
				String fileName = dateFormat(DATE_FORMAT, new Date(), "");
				fileName = fileName.replaceAll("[ ]", "_");
				fileName = fileName.replaceAll("[:]", "");
				fileName = "sync_"+fileName+"_log.txt";
				File logFile = new File(logDir, fileName);
				log("open log: "+fileName);
				logFile.createNewFile();
				PrintWriter pw = new PrintWriter(logFile);
				mLogPrinter = new PrintWriterPrinter(pw);
			} catch (Exception ex) {
				log("### "+ex);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_CANCEL_ID, 0, R.string.menu_cancel);
        return true;
	}

	@Override
	protected void onDestroy() {
		mDatabaseController.close();
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case MENU_CANCEL_ID:
        	finish();
            return true;
    }
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();
		client = null;
		log("*** Sync Paused ***");
	}

	@Override
	protected void onResume() {
		super.onResume();
		log("*** Sync Starting ***");
		log("--- connect ---");
		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String wiki_url = prefs.getString(PrefsActivity.PREF_WIKI_URL, "<unset>");
			String wiki_user = prefs.getString(PrefsActivity.PREF_WIKI_USER, "");
			String wiki_passwd = prefs.getString(PrefsActivity.PREF_WIKI_PASSWD, "");
			log("wiki_url="+wiki_url);
			String xmlrpc_url = wiki_url;
			if (!xmlrpc_url.endsWith("/")) {
				xmlrpc_url += "/";
			}
			xmlrpc_url += "lib/exe/xmlrpc.php";
			log("xmlrpc_url="+xmlrpc_url);
			URI uri = URI.create(xmlrpc_url);
			if ((wiki_user == null) || (wiki_user.length() == 0)) {
				log("opening XML-RPC client with no credentials");
				client = new XMLRPCClient(uri);
			} else {
				log("opening XML-RPC client as "+wiki_user+"/"+wiki_passwd);
//				client = new XMLRPCClient(uri, wiki_user, wiki_passwd);
				client = new XMLRPCClient(uri);
				Boolean loginResult = (Boolean) client.call("dokuwiki.login", wiki_user, wiki_passwd);
				log("login: "+loginResult);
			}
			String rpcVersionSupported = client.call("wiki.getRPCVersionSupported").toString();
			log("wiki.getRPCVersionSupported = "+rpcVersionSupported);
		} catch (Exception e) {
			log("### "+e);
		}
		if (client == null) {
			log("### XML-RPC Client was not created. Exiting Sync.");
			return;
		}
		log("--- sync ---");
		try {
			Object[] pages = (Object[]) client.call("wiki.getAllPages");
			for (Object o : pages) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> pageMap = (HashMap<String, Object>) o;
				String pageName = (String) pageMap.get("id");
				WikiPage page = mDatabaseController.findPageByName(pageName);
				if (page == null) {
					page = new WikiPage();
					page.name = pageName;
				}
				page.remoteChangedAt = (java.util.Date) pageMap.get("lastModified");
				int action = getAction(page);
				if (action != ACTION_NONE) {
					log(page.name+": "+ACTION_NAMES[action]);
				}
				switch (action) {
				case ACTION_NONE:
					break;
				case ACTION_MERGE: // FIXME: implement merge action
					log("WARNING: merge not implemented. Killing local changes!");
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
					log("ERROR: unknown action #"+action);
				}
//				storePage(page);
			}
		} catch (Exception e) {
			log("### "+e);
		}
		log("*** Sync Finished ***");
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
		log("--- downloadPageBody("+page.name+") ---");
		try {
			page.body = (String) client.call("wiki.getPage", page.name);
			log("page content:\n"+page.body);
		} catch (Exception e) {
			log("### "+e);
		}
	}
	
	private boolean uploadPage(WikiPage page) {
		boolean result = true;
		log("--- uploadPage("+page.name+") ---");
		try {
			HashMap<String, String> attrs = new HashMap<String, String>();
//			attrs.put("sum", "sync upload");
			Integer callResult = (Integer) client.call("wiki.putPage", page.name, page.body, attrs);
			result = callResult == 0;
			log("result: "+result);
			return result;
		} catch (Exception e) {
			log("### "+e);
			return false;
		}
	}
	
	private void storePage(WikiPage page) {
		if (page.id == 0) {
			mDatabaseController.createPage(page);
		} else {
			mDatabaseController.updatePage(page);
		}
	}
	
	private String dateFormat(DateFormat dateFormat, Date date, String nullSubstitute) {
		return (date == null) ? nullSubstitute : dateFormat.format(date);
	}

	private long getTime(Date date) {
		return date == null ? 0L : date.getTime();
	}
	
	private void log(String log) {
		if (mLogPrinter != null) {
			try {
				mLogPrinter.println(log);
			} catch (Exception ex) {} // ignored
		}
		while (mSyncLogLines > MAX_LOG_LINES) {
			int cutIndex = mSyncLog.indexOf("\n");
			if ((cutIndex < 0) || (cutIndex >= mSyncLog.length()-1)) {
				break;
			}
			mSyncLog.delete(0, cutIndex+1);
			mSyncLogLines--;
		}
		mSyncLog.append("[");
		mSyncLog.append(++mSyncLogLine);
		mSyncLog.append("] ");
		mSyncLog.append(log);
		mSyncLog.append("\n");
		mSyncLogLines++;
		mSyncLogView.setText(mSyncLog);
	}
	
}
