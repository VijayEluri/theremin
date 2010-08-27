package org.ugosan.theremin;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;

public class CalibrateView extends SurfaceView implements SurfaceHolder.Callback , SensorEventListener, OnClickListener, Runnable {
	private HelloThread mThread = null;

	float currentX = 0;
	float currentY = 0;
	float currentZ = 0;
	
	//x:0 y:0
	float mx1 = -64;
	float my1 = -166;
	float mz1 = 88;
	
	//x:0 y:320
	float mx2 = -30;
	float my2 = -1;
	float mz2 = 15;
	
	//x:240 y:320
	float mx3 = -33;
	float my3 = -12;
	float mz3 = 8;
	
	//x:240 y:0
	float mx4 = -43;
	float my4 = -28;
	float mz4 = 30;
	
	float x = 0;
	float y = 0;
	float z = 0;
	
	public CalibrateView(Context context) {
		super(context);
		
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		mThread = new HelloThread(holder, context, new Handler());
		setFocusable(true); // need to get the key events
		
		SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sm.registerListener(this, 
				sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_GAME);
		
		this.setOnClickListener(this);
	}
	public HelloThread getThread() {
		return mThread;
	}

	public void surfaceCreated(SurfaceHolder holder) {
		mThread.setRunning(true);
		mThread.start();
	}
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		mThread.setRunning(false);
		while (retry) {
			try {
				mThread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		mThread.doKeyDown(keyCode, event);
		return super.onKeyDown(keyCode, event);
	}
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		mThread.doKeyUp(keyCode, event);
		return super.onKeyUp(keyCode, event);
	}

	class FPSTimer {
		private int mFPS;
		private double mSecPerFrame;
		private double mSecTiming;
		private long mCur;
		public FPSTimer(int fps) {
			mFPS = fps;
			reset();
		}
		public void reset() {
			mSecPerFrame = 1.0 / mFPS;
			mCur = System.currentTimeMillis();
			mSecTiming = 0.0;
		}
		public boolean elapsed() {
			long next = System.currentTimeMillis();
			long passage_time = next - mCur;
			mCur = next;
			mSecTiming += (passage_time/1000.0);
			mSecTiming -= mSecPerFrame;
			if (mSecTiming > 0) {
				if (mSecTiming > mSecPerFrame) {
					reset();
					return true; // force redraw
				}
				return false;
			}
			try {
				Thread.sleep((long)(-mSecTiming * 1000.0));
			} catch (InterruptedException e) {
			}
			return true;
		}
	}

	class HelloThread extends Thread {
		private int mX = 0;
		private int mY = 0;
		private int mKeyCode = -1;
		private SurfaceHolder mSurfaceHolder;
		private boolean mRun = false;
		public HelloThread(SurfaceHolder holder, Context context, Handler handler) {
			mSurfaceHolder = holder;
		}
		public void setRunning(boolean b) {
			mRun = b;
		}
		public void doKeyDown(int keyCode, KeyEvent msg) {
			mKeyCode = keyCode;
		}
		public void doKeyUp(int keyCode, KeyEvent msg) {
			mKeyCode = -1;
		}
		public void run() {
			int fps = 0;
			long cur = System.currentTimeMillis();
			boolean isdraw = true;
			FPSTimer timer = new FPSTimer(60);
			while (mRun) {
				Canvas c = null;
				if (isdraw) {
					try {
						c = mSurfaceHolder.lockCanvas(null);
						synchronized (mSurfaceHolder) {
							doDraw(c);
						}
						fps++;
					} finally {
						if (c != null)
							mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
				isdraw = timer.elapsed();
				long now = System.currentTimeMillis();
				if (now - cur > 1000) {
					Log.d("KZK", "FPS=" + (fps * 1000 / ((double)now - cur)));
					fps = 0;
					cur = now;
				}
			}
		}

		protected void doDraw(Canvas canvas) {
			Paint paint = new Paint();
			//paint.setAntiAlias(true);
			//canvas.drawBitmap(image, 0, 0, null);

			paint.setStyle(Paint.Style.FILL);
	        paint.setColor(Color.argb(255,0,0,0));
	        
	        //canvas.drawRect(new Rect(mX+0,mY+0,mX+40,mY+40),paint);
	        canvas.drawRect(new Rect(0,0,canvas.getWidth(),canvas.getHeight()),paint);
	        
	        paint.setStyle(Paint.Style.STROKE);
	        
	        paint.setColor(Color.argb(255,0,255,100));
	        
	        canvas.drawCircle(10, 10, 3, paint);
	        canvas.drawCircle(10, 10, 10, paint);
	        
	        //primeiro ponto
	        float auxx = currentX*1/mx1;
	        float auxy = currentY*1/my1;
        		
	        //segundo ponto
	        float auxx2 = currentX*1/mx2;
	        float auxy2 = currentY*320/my2;
	        
	        //terceiro ponto
	        float auxx3 = currentX*240/mx3;
	        float auxy3 = currentY*320/my3;
	        
	        canvas.drawCircle((auxx+auxx2+auxx3)/3, (auxy+auxy2+auxy3)/3, 10, paint);
	        
	        
	        
	        canvas.drawRect(new Rect((canvas.getWidth()/2)-25,(canvas.getHeight()/2)-25,100,80), paint);
	        canvas.drawText(canvas.getWidth()+"x"+canvas.getHeight(), (canvas.getWidth()/2)-23, (canvas.getHeight()/2)-10, paint);
	        
	        canvas.drawText(currentX+"", (canvas.getWidth()/2)-25, canvas.getHeight()/2, paint);
	        canvas.drawText(currentY+"", (canvas.getWidth()/2)-25, (canvas.getHeight()/2+10), paint);
	        canvas.drawText(currentZ+"", (canvas.getWidth()/2)-25, (canvas.getHeight()/2+20), paint);
	        
	       
	       // canvas.drawText((auxx+auxx2+auxx3)/3+"x"+(auxy+auxy2+auxy3)/3, (canvas.getWidth()/2)-25, canvas.getHeight()/2+30, paint);

		}
	}


	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		Log.i("ugosan",arg0.getName()+" "+ arg1);
		
		
	}
	@Override
	public void onSensorChanged(SensorEvent arg0) {
		
		
		currentX = arg0.values[0];
		currentY = arg0.values[1];
		currentZ = arg0.values[2];
		
		
		
	}
	@Override
	public void onClick(View arg0) {
		
		
	}
	
	public boolean onTouchEvent(final MotionEvent event) {
		
		Log.i(Main.TAG, ""+event.getX());
		
		return false;
		
		
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}

