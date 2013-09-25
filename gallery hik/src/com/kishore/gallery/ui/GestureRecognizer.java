/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;

import com.kishore.gallery.app.Log;

// This class aggregates three gesture detectors: GestureDetector,
// ScaleGestureDetector, and DownUpDetector.
public class GestureRecognizer {
    private static final String TAG = "KISHORE_GestureRecognizer";

    public interface Listener {
        boolean onSingleTapUp(float x, float y);
        boolean onDoubleTap(float x, float y);
        boolean onScroll(float dx, float dy, float totalX, float totalY);
        boolean onFling(float velocityX, float velocityY);
        boolean onScaleBegin(float focusX, float focusY);
        boolean onScale(float focusX, float focusY, float scale);
        void onScaleEnd();
        void onDown(float x, float y);
        void onUp();
    }
    
    //dirty
    private final ContentResolver mCr;
    final int DELAY_LEVEL = SensorManager.SENSOR_DELAY_GAME;
    final int maxvol;
    final int maxbrightness;
    private double updatevol =0.0;
    private double updatebrightness =0.0;
    //end dirty
    private boolean movieMode;
    private final Activity curact;
    private final SensorManager mSenMan;
    private SensorEvtListener mSenEvtListner;
    private Sensor mSensor;
    private boolean mMute = true;
    private final AudioManager mAudioMan;
    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;
    private final DownUpDetector mDownUpDetector;
    private final Listener mListener;
    //angle values
    float mAngle0_azimuth=0;
    float mAngle1_pitch=0;
    float mAngle2_roll=0;
    float mAngle0_filtered_azimuth=0;
    float mAngle1_filtered_pitch=0;
    float mAngle2_filtered_roll=0;
    float mAngle0_filtered_azimuth_old=0;
    float mAngle1_filtered_pitch_old=0;
    float mAngle2_filtered_roll_old=0;
    float droll =0.0f,dazi =0.0f,dpitch=0.0f;
    //Sensor values
    float[] mGravity = null;
    float[] mGeomagnetic = null;
    float Rmat[] = new float[9];
    float Imat[] = new float[9];
    float orientation[] = new float[3];
    
    private float restrictAngle(float tmpAngle){
        while(tmpAngle>=180) tmpAngle-=360;
        while(tmpAngle<-180) tmpAngle+=360;
        return tmpAngle;
    }

    //x is a raw angle value from getOrientation(...)
    //y is the current filtered angle value
    private float calculateFilteredAngle(float x, float y){ 
        final float alpha = 0.1f;
        float diff = x-y;
        if(y==0.0f)
        	y=x;
        //here, we ensure that abs(diff)<=180
        diff = restrictAngle(diff);
        y += alpha*diff;
        //ensure that y stays within [-180, 180] bounds
        y = restrictAngle(y);
        return y;
    }    

    public void processSensorData(){
        if (mGravity != null && mGeomagnetic != null) { 
        	
            boolean success = SensorManager.getRotationMatrix(Rmat, Imat, mGravity, mGeomagnetic);
            if (success) {              
                SensorManager.getOrientation(Rmat, orientation);
                mAngle0_azimuth = (float)Math.toDegrees((double)orientation[0]); // orientation contains: azimut, pitch and roll
                mAngle1_pitch = (float)Math.toDegrees((double)orientation[1]); //pitch
                mAngle2_roll = -(float)Math.toDegrees((double)orientation[2]); //roll  
//                Log.i(TAG, "Raw angle values: azi="+mAngle0_azimuth+" ,pitch="+mAngle1_pitch+" ,roll="+mAngle2_roll);
                if(mAngle0_filtered_azimuth == 0.0f) {
                	mAngle0_filtered_azimuth=mAngle0_azimuth;
                    mAngle1_filtered_pitch = mAngle1_pitch;
                    mAngle2_filtered_roll = mAngle2_roll;
                	mAngle0_filtered_azimuth_old=mAngle0_filtered_azimuth;
                    mAngle1_filtered_pitch_old = mAngle1_filtered_pitch;
                    mAngle2_filtered_roll_old = mAngle2_filtered_roll;
                }else {
                	mAngle0_filtered_azimuth = calculateFilteredAngle(mAngle0_azimuth, mAngle0_filtered_azimuth);
                	mAngle1_filtered_pitch = calculateFilteredAngle(mAngle1_pitch, mAngle1_filtered_pitch);
                	mAngle2_filtered_roll = calculateFilteredAngle(mAngle2_roll, mAngle2_filtered_roll);
                }
//                Log.i(TAG, "Filtered angle values: azi="+mAngle0_filtered_azimuth+" ,pitch="+mAngle1_filtered_pitch+" ,roll="+mAngle2_filtered_roll);
                droll += mAngle2_filtered_roll-mAngle2_filtered_roll_old;
                dazi += mAngle0_filtered_azimuth-mAngle0_filtered_azimuth_old;
                dpitch += mAngle1_filtered_pitch-mAngle1_filtered_pitch_old;
                mAngle0_filtered_azimuth_old=mAngle0_filtered_azimuth;
                mAngle1_filtered_pitch_old = mAngle1_filtered_pitch;
                mAngle2_filtered_roll_old = mAngle2_filtered_roll;
            }           
            mGravity=null; //oblige full new refresh
            mGeomagnetic=null;
            if(droll > 3.0f && droll < 70.0f){
            	mListener.onScroll(droll*1.5f, 0.0f, 0.0f, 0.0f);
            	Log.i(TAG, "Scrolling ---------> Angle value is: "+droll);
            }
            else if(droll <-3.0f && droll >-70.0f){
            	mListener.onScroll(-1.0f*droll*1.5f, 0.0f, 0.0f, 0.0f);
            	Log.i(TAG, "Scrolling ---------> Angle value is: "+droll);
            }
//            else if (mAngle2_filtered_roll < -60.0f) {
//            	mListener.onFling(-1.0f*mAngle2_filtered_roll*mAngle2_filtered_roll*7.5f, 0.0f);
//            	Log.i(TAG, "Flinging ---------> Angle value is: "+mAngle2_filtered_roll);
//            }
//            else if (mAngle2_filtered_roll > 60.0f) {
//            	mListener.onFling(mAngle2_filtered_roll*mAngle2_filtered_roll*7.5f, 0.0f);
//            	Log.i(TAG, "Flinging ---------> Angle value is: "+mAngle2_filtered_roll);
//            }
            else {
            	Log.e(TAG, "SOMETHING VERY VERY WRONG GOING ON HERE "+mAngle2_filtered_roll);
            }
        }
    }
    
    private class SensorEvtListener implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD: {
	                mGeomagnetic = event.values.clone();
                    processSensorData();
                    break;
                }
                case Sensor.TYPE_ACCELEROMETER: {
	                mGravity = event.values.clone();
                    processSensorData();
                }
            }
        }
    }

    public GestureRecognizer(Context context, Listener listener, boolean inmovieMode) {
        mListener = listener;
        movieMode = inmovieMode;
        mSenMan = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSenEvtListner = null;
        mSensor = mSenMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mSensor == null) {
            Log.w(TAG, "no sensor available");
        }
        mAudioMan = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        maxvol = mAudioMan.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mCr = context.getContentResolver();
        maxbrightness = 1;// from android docs
        curact = (Activity)context;
        mGestureDetector = new GestureDetector(context, new MyGestureListener(),
                null, false);//true /* ignoreMultitouch */);
        mScaleDetector = new ScaleGestureDetector(context, new MyScaleListener());
        mDownUpDetector = new DownUpDetector(context,new MyDownUpListener());
    }

    public void onTouchEvent(MotionEvent event) {
        mDownUpDetector.onTouchEvent(event);
        Log.i(TAG, "Pointer count is : "+event.getPointerCount());
    	if(event.getPointerCount() >4){
    		mAudioMan.setStreamMute(AudioManager.STREAM_MUSIC, mMute);
    		mMute = (!mMute);
    	}
    	if(true == mDownUpDetector.isSensorScroll() && null == mSenEvtListner) {
        	mSenEvtListner = new SensorEvtListener();
        	mSenMan.registerListener(mSenEvtListner, mSensor, DELAY_LEVEL);
        	mSenMan.registerListener(mSenEvtListner, mSenMan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), DELAY_LEVEL);
        }
    	else if (false == mDownUpDetector.isSensorScroll()) {
        	mSenMan.unregisterListener(mSenEvtListner);
        	mSenEvtListner = null;
        }
        mGestureDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
    }

    public boolean isDown() {
        return mDownUpDetector.isDown();
    }
    
    public void cancelScale() {
        long now = SystemClock.uptimeMillis();
        MotionEvent cancelEvent = MotionEvent.obtain(
                now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        mScaleDetector.onTouchEvent(cancelEvent);
        cancelEvent.recycle();
    }

    private class MyGestureListener
                extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
        	if(e.getPointerCount() <2)
        		return mListener.onSingleTapUp(e.getX(), e.getY());
        	else
        		return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return mListener.onDoubleTap(e.getX(), e.getY());
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        	if(mDownUpDetector.isVolumeCtl()) {
        		try {
        			updatevol+=dy/10;
        			if (updatevol > 1.0 || updatevol < -1.0){
        				int currentvol = mAudioMan.getStreamVolume(AudioManager.STREAM_MUSIC);
        				currentvol+=(int)updatevol;
        				updatevol=0.0;
        				if (currentvol>maxvol)currentvol=maxvol;
        				if (currentvol<0)currentvol=0;
            			mAudioMan.setStreamVolume(AudioManager.STREAM_MUSIC, currentvol, AudioManager.FLAG_SHOW_UI);
            			Log.i(TAG, "setting audio value: "+currentvol+" value of dy is: "+dy);
        			}
        		}
        		catch (Exception e){
        			Log.e(TAG, "Some major error: "+e.toString());
        		}
        		finally {
        		}
        		return true;
        	}
        	else if (mDownUpDetector.isBrightness()) {
        		try {
        			float currentbrightness;
        			updatebrightness+=dy/30;
        			if (updatebrightness > 0.1 || updatebrightness < -0.1 && curact != null){
        				WindowManager.LayoutParams lp = curact.getWindow().getAttributes();
        				currentbrightness = lp.screenBrightness;
        				currentbrightness+=updatebrightness;
        				updatebrightness=0.0;
        				if (currentbrightness>maxbrightness)currentbrightness=maxbrightness;
        				if (currentbrightness < 0.1f)currentbrightness=0.1f;
        				lp.screenBrightness = currentbrightness;
        				curact.getWindow().setAttributes(lp);
        				Log.i(TAG, "setting screen brightness value: "+currentbrightness+" value of update is: "+updatebrightness);
        			}
	        	}
	    		catch (Exception e){
	    			Log.e(TAG, "Some major error: "+e.toString());
	    		}
	    		finally {
	    		}
	    		return true;
        	}
        	else if (mDownUpDetector.isBrightness()) {
        		if(true == mDownUpDetector.isSensorScroll() && null == mSenEvtListner) {
                	mSenEvtListner = new SensorEvtListener();
                	mSenMan.registerListener(mSenEvtListner, mSensor, DELAY_LEVEL);
                	mSenMan.registerListener(mSenEvtListner, mSenMan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), DELAY_LEVEL);
                }
            	else if (false == mDownUpDetector.isSensorScroll()) {
                	mSenMan.unregisterListener(mSenEvtListner);
                	mSenEvtListner = null;
                }
        		return true;
        	}
        	else {
        		return mListener.onScroll(dx, dy, e2.getX() - e1.getX(), e2.getY() - e1.getY());
        	}
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            return mListener.onFling(velocityX, velocityY);
        }
    }

    private class MyScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return mListener.onScaleBegin(
                    detector.getFocusX(), detector.getFocusY());
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return mListener.onScale(detector.getFocusX(),
                    detector.getFocusY(), detector.getScaleFactor());
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mListener.onScaleEnd();
        }
    }

    private class MyDownUpListener implements DownUpDetector.DownUpListener {
        @Override
        public void onDown(MotionEvent e) {
            mListener.onDown(e.getX(), e.getY());
        }

        @Override
        public void onUp(MotionEvent e) {
            mListener.onUp();
        }
    }
}
