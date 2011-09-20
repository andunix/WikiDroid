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

import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

public class FlingDetector implements OnGestureListener {
	public interface OnFlingListener {
		public boolean onFlingDown(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
		public boolean onFlingLeft(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
		public boolean onFlingRight(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
		public boolean onFlingUp(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
	}
	private final OnFlingListener mListener;
	public FlingDetector(OnFlingListener listener) {
		this.mListener = listener;
	}
	@Override
	public boolean onDown(MotionEvent e) { return false; }
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (Math.abs(velocityX) > Math.abs(velocityY)) {
			if (velocityX < 0) {
				return mListener.onFlingLeft(e1, e2, velocityX, velocityY);
			} else {
				return mListener.onFlingRight(e1, e2, velocityX, velocityY);
			}
		} else {
			if (velocityY < 0) {
				return mListener.onFlingUp(e1, e2, velocityX, velocityY);
			} else {
				return mListener.onFlingDown(e1, e2, velocityX, velocityY);
			}
		}
	}
	@Override
	public void onLongPress(MotionEvent e) {}
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) { return false; }
	@Override
	public void onShowPress(MotionEvent e) {}
	@Override
	public boolean onSingleTapUp(MotionEvent e) { return false; }
}
