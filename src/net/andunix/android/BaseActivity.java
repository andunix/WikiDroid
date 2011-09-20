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

package net.andunix.android;

import net.andunix.android.FlingDetector.OnFlingListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class BaseActivity extends Activity implements OnFlingListener {
	private FlingDetector mFlingDetector;
	private GestureDetector mGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFlingDetector = new FlingDetector(this);
		mGestureDetector = new GestureDetector(mFlingDetector);
	}

	@Override
	public boolean onFlingDown(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public boolean onFlingLeft(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public boolean onFlingRight(MotionEvent e1, MotionEvent e2,
			float velocityX, float velocityY) {
		return false;
	}

	@Override
	public boolean onFlingUp(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return mGestureDetector.onTouchEvent(me);
	}
    protected void startActivity(Class<? extends Activity> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }
}
