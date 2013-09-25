/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kishore.gallery.ui;

import com.kishore.gallery.ui.GestureRecognizer.Listener;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.WindowManager;

public class DownUpDetector {
    public interface DownUpListener {
        void onDown(MotionEvent e);
        void onUp(MotionEvent e);
    }
    final int MAX_NUMBER_OF_POINTERS =4;
    final int screenWidth;
    final int screenHeight;
    private boolean mStillDown;
    private boolean mSensorScroll;
    private boolean mVolumeCtl;
    private boolean mBrightness;
    private PointerCoords mTouchPoint0 = new PointerCoords();
    private PointerCoords mTouchPoint1 = new PointerCoords();
    private PointerCoords mTouchPoint2 = new PointerCoords();
    private PointerCoords mTouchPoint3 = new PointerCoords();
    private DownUpListener mListener;

    public DownUpDetector(Context incontext, DownUpListener listener) {
        mListener = listener;
        Display just4gags = ((WindowManager)incontext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point whyme = new Point();
        just4gags.getSize(whyme);
        screenHeight = whyme.y;
        screenWidth = whyme.x;
    }

    private void setState(boolean down, MotionEvent e) {
        if (down == mStillDown) return;
        mStillDown = down;
        if (down) {
            mListener.onDown(e);
        } else {
            mListener.onUp(e);
        }
    }

    public void onTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            setState(true, ev);
            break;
        case MotionEvent.ACTION_POINTER_DOWN: 
        	int itr1 = ev.getPointerCount()-1;
        	switch (itr1) {
        	case 3:
        		ev.getPointerCoords(3, mTouchPoint3);
            	Log.i("KISHORE_TEST", "receieved data for pointer:3 value: "+mTouchPoint3.x+ " y: "+mTouchPoint3.y);
            	if(itr1==3) {
        			mBrightness = true;
        		}else{
        			mBrightness = false;
        		}
        	case 2:
        		ev.getPointerCoords(2, mTouchPoint2);
            	Log.i("KISHORE_TEST", "receieved data for pointer:2 value: "+mTouchPoint2.x+ " y: "+mTouchPoint2.y);
            	if(itr1==2) {
        			mVolumeCtl = true;
        		}else{
        			mVolumeCtl = false;
        		}
        	case 1:
        		ev.getPointerCoords(1, mTouchPoint1);
        		if(itr1==1) {
//        			if(Math.abs((mTouchPoint1.x - mTouchPoint0.x)) > ((int)screenWidth*0.2)) {
        				mSensorScroll = true;
//        			}
//        			else{
//        				Log.e("KISHORE_TEST", "Distance less. pointer1: "+(mTouchPoint1.x - mTouchPoint0.x)+ " screenwidth: "+(int)screenWidth*0.4);
//        			}
        		}else{
        			mSensorScroll = false;
        		}
            	Log.i("KISHORE_TEST", "receieved data for pointer:1 value: "+mTouchPoint1.x+ " y: "+mTouchPoint1.y);
        	case 0:
        		ev.getPointerCoords(0, mTouchPoint0);
            	Log.i("KISHORE_TEST", "receieved data for pointer:0 value: "+mTouchPoint0.x+ " y: "+mTouchPoint0.y);
            	break;
        	default:
        		mListener.onDown(ev);
        		Log.e("KISHORE_TEST", "Itr value is messed up: "+ itr1);
        		break;
        	}
        	setState(true, ev);
        	break;
        	
        case MotionEvent.ACTION_POINTER_UP:
        	Log.i("KISHORE_TEST", "getaction index UP: "+ev.getActionIndex());
        	int itr = ev.getPointerCount();
        	Log.i("KISHORE_TEST", "UP action count: "+itr);
        	mSensorScroll = false;
        	mBrightness = false;
        	mVolumeCtl = false;
        	switch (itr) {
        	case 3:
        		mSensorScroll = true;
        		break;
        	case 4:
        		mVolumeCtl = true;
        		break;
        	case 5:
        		mBrightness = true;
        		break;
        	default:
    			break;
        	}
        	setState(true, ev);
        	break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            setState(false, ev);
            mSensorScroll = false;
            mBrightness = false;
            mVolumeCtl = false;
            break;
        }
    }

    public boolean isDown() {
        return mStillDown;
    }
    
    public boolean isSensorScroll() {
    	return mSensorScroll;
    }
    public boolean isVolumeCtl() {
    	return mVolumeCtl;
    }
    public boolean isBrightness() {
    	return mBrightness;
    }
}
