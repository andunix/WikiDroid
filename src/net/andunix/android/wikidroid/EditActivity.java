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

import net.andunix.android.BaseActivity;
import android.os.Bundle;
import android.widget.EditText;

public class EditActivity extends BaseActivity {

    private EditText mBodyText;
    private Long mRowId;
    private DatabaseController mDatabaseController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		mDatabaseController = new DatabaseController(this).open();
        setContentView(R.layout.edit);
        setTitle(R.string.edit_page);

        mBodyText = (EditText) findViewById(R.id.body);

        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DatabaseController.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(DatabaseController.KEY_ROWID)
									: null;
		}

		populateFields();
    }

    private void populateFields() {
        if (mRowId != null) {
            WikiPage page = mDatabaseController.getPage(mRowId);
            mBodyText.setText(page.body);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(DatabaseController.KEY_ROWID, mRowId);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }
    
    private void saveState() {
        String body = mBodyText.getText().toString();
        if (mRowId == null) {
        	// FIXME: create page
//            long id = mDatabaseController.createNote(title, body);
//            if (id > 0) {
//                mRowId = id;
//            }
        } else {
            mDatabaseController.updatePageBody(mRowId, body);
        }
    }
    
}
