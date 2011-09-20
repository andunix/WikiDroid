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

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class MainActivity extends ListActivity {
	
    private static final int SYNC_ID = Menu.FIRST;
    private static final int SETTINGS_ID = Menu.FIRST + 1;
    
	private DatabaseController mDatabaseController;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		mDatabaseController = new DatabaseController(this).open();
        setContentView(R.layout.list);
        fillData();
        registerForContextMenu(getListView());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, SYNC_ID, 0, R.string.menu_sync);
        menu.add(0, SETTINGS_ID, 0, R.string.menu_settings);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case SYNC_ID:
//        	startActivity(SyncActivity.class);
        	Intent i = new Intent(this, SyncService.class);
        	startService(i);
            return true;
        case SETTINGS_ID:
        	startActivity(PrefsActivity.class);
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    private void fillData() {
        Cursor pagesCursor = mDatabaseController.fetchAllPages();
        startManagingCursor(pagesCursor);

        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[] { DatabaseController.KEY_NAME };

        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{ android.R.id.text1 };

        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter notes = 
            new SimpleCursorAdapter(this, R.layout.list_row, pagesCursor, from, to);
        setListAdapter(notes);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, EditActivity.class);
        i.putExtra(DatabaseController.KEY_ROWID, id);
        startActivity(i);
    }
    
    protected void startActivity(Class<? extends Activity> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }
    
}
