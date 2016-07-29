package org.landroo.digiclock;

import android.R.color;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.MotionEvent;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.landroo.ui.UI;
import org.landroo.ui.UIInterface;

public class DigiClockActivity extends Activity implements UIInterface
{
	private static final String TAG = DigiClockActivity.class.getSimpleName();

	private UI ui = null;
	private ScrollView scrollView;
	private ClockViewGroup clockView;
	private int displayWidth;
	private int displayHeight;
	
	public float pictureWidth;
	public float pictureHeight;
	public float origWidth;
	public float origHeight;	
	
	private Timer timer;
	
	private DigiClockClass clock;
	
	private Drawable backDrawable;
	
	private int numColor = 0xFFFFFFFF;
	private int backColor = 0xFFFFFFFF;
	
	private Messenger mMessenger = new Messenger(new IncomingHandler());
	private Messenger mService = null;
	private boolean mIsBound = false;
	private ServiceConnection mConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			mService = new Messenger(service);
			try
			{
				Message msg = Message.obtain(null, DigiClockService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			}
			catch (RemoteException ex)
			{
				Log.i(TAG, "" + ex);
			}
		}

		public void onServiceDisconnected(ComponentName className)
		{
			// mBoundService = null;
			mService = null;
		}
	};
	
	class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case DigiClockService.MSG_SET_INT_VALUE:
				// Log.i(TAG, "integer received: " + msg.arg1);
				break;
			case DigiClockService.MSG_SET_STRING_VALUE:
				String sState = msg.getData().getString("state");
				if (sState != null)
				{
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}	
	
	private class ClockViewGroup extends ViewGroup
	{
		private float xPos = 0;
		private float yPos = 0;

		private float dx = 0;
		private float dy = 0;
		
		private float zoomX = 1;
		private float zoomY = 1;
		
		private Paint paint = new Paint();
		
		public ClockViewGroup(Context context) 
		{
			super(context);
			
			paint.setColor(Color.WHITE);
			paint.setTextSize(32);
			
			this.setKeepScreenOn(true);
		}
		
		@Override
		protected void dispatchDraw(Canvas canvas)
		{
			super.dispatchDraw(canvas);
			
			xPos = 0;
			yPos = 0;
			if (scrollView != null)
			{
				xPos = scrollView.xPos();
				yPos = scrollView.yPos();
				
				zoomX = scrollView.getZoomX();
				zoomY = scrollView.getZoomY();
			}
			
			if(backDrawable != null)
			{
				backDrawable.draw(canvas);
			}
			
			if (clock != null && canvas != null)
			{
				for (int i = 0; i < clock.clockArray.length; i++)
				{
					DigiClockClass.Primitive prim = clock.clockArray[i]; 
					if (prim != null)
					{
						dx = (prim.posX * zoomX) + xPos;
						dy = (prim.posY * zoomY) + yPos;
						
						prim.drawable.setBounds(0, 0, (int)(prim.width * zoomX), (int)(prim.height * zoomY));
						prim.shadow.setBounds(0, 0, (int)(prim.width * zoomX), (int)(prim.height * zoomY));

						canvas.save();
						canvas.rotate(prim.rot, (prim.posX * zoomX) + xPos + prim.width * zoomX / 2, (prim.posY * zoomY) + yPos + prim.height * zoomY / 2);
						// canvas.rotate(prim.rot, prim.oriX, prim.oriY);
						canvas.translate(dx, dy);
						if(prim.visible) prim.drawable.draw(canvas);
						prim.shadow.draw(canvas);
						// canvas.translate(-xPos, -yPos);
						canvas.restore();
					}
				}
			}

			//canvas.drawText("xPos: " + xPos, 0, 32, paint);
			//canvas.drawText("yPos: " + yPos, 0, 64, paint);
			//canvas.drawText("z: " + zoomX, 0, 96, paint);			
		}
		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) 
		{
		}
		
		@Override
	    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
		{
			setMeasuredDimension(displayWidth, displayHeight);
			
			int count = this.getChildCount();
		    for (int i = 0; i < count; i++) 
		    {
		    	View child = this.getChildAt(i);
	    		measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
		    }
		}
		
		@Override
		protected void onDetachedFromWindow()
		{
		    super.onDetachedFromWindow();
		    this.setKeepScreenOn(false);
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();        
        
		clockView = new ClockViewGroup(this);
		setContentView(clockView);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		ui = new UI(this);
		
		initApp();
		
		doBindService();

		timer = new Timer();
		timer.scheduleAtFixedRate(new RotateTask(), 1000, 500);

    }
    
    private void initApp()
	{

		try
		{
			pictureWidth = displayWidth * 3;
			pictureHeight = displayHeight * 3;
			origWidth = pictureWidth;
			origHeight = pictureHeight;
			
			int half = 1;
			Bitmap texture = BitmapFactory.decodeResource(this.getResources(), R.drawable.marble2);
			clock = new DigiClockClass(displayWidth / half, displayHeight / half, texture, true, numColor, true, true, 255);
			
			texture = BitmapFactory.decodeResource(this.getResources(), R.drawable.marble2);
			backDrawable = clock.createBackground(displayWidth, displayHeight, backColor, texture);
			
			scrollView = new ScrollView(displayWidth, displayHeight, (int)pictureWidth, (int)pictureHeight, clockView, true);
		}
		catch (OutOfMemoryError e)
		{
			Log.e(TAG, "Out of memory error in new stage!");
		}
		catch (Exception ex)
		{
			Log.e(TAG, "" + ex);
		}
		
		return;
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.digi_clock, menu);
		return true;
	}    
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
		case R.id.action_settings:
			Intent SettingsIntent = new Intent(this, SettingsScreen.class);
			startActivity(SettingsIntent);
			return true;
		case R.id.action_exit:
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			sendMessageToService(1000);
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}    
    
	@Override
	public void onDestroy()
	{
		super.onDestroy();

		try
		{
			doUnbindService();
		}
		catch (Throwable t)
		{
			Log.e("MainActivity", "Failed to unbind from the service", t);
		}
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		loadPreferences();
		initApp();
	}	
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		return ui.tapEvent(event);
	}	

	@Override
	public void onDown(float x, float y)
	{
		scrollView.onDown(x, y);
	}

	@Override
	public void onUp(float x, float y)
	{
		scrollView.onUp(x, y);		
	}

	@Override
	public void onTap(float x, float y)
	{

	}

	@Override
	public void onHold(float x, float y)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMove(float x, float y)
	{
		scrollView.onMove(x, y);		
	}

	@Override
	public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2)
	{
		scrollView.onSwipe(direction, velocity, x1, y1, x2, y2);	
	}

	@Override
	public void onDoubleTap(float x, float y)
	{
		scrollView.onDoubleTap(x, y);		
	}

	@Override
	public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff)
	{
		scrollView.onZoom(mode, x, y, distance, xdiff, ydiff);		
	}

	@Override
	public void onRotate(int mode, float x, float y, float angle)
	{
	}

	@Override
	public void onFingerChange()
	{
		// TODO Auto-generated method stub
		
	}
	
	class RotateTask extends TimerTask
	{
		public void run()
		{
			Date date = new Date();
			long time = date.getTime();
			
			clock.createNums((int)displayWidth, (int)displayHeight, time);
			
			clockView.postInvalidate();
		}
	}
	
	void doBindService()
	{
		if (bindService(new Intent(this, DigiClockService.class), mConnection, Context.BIND_AUTO_CREATE))
		{
			mIsBound = true;
			startService(new Intent(this, DigiClockService.class));
		}
	}
	
	void doUnbindService()
	{
		if (mIsBound)
		{
			if (mService != null)
			{
				try
				{
					Message msg = Message.obtain(null, DigiClockService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				}
				catch (RemoteException e)
				{
					Log.i(TAG, "" + e);
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	
	private void sendMessageToService(int iMess)
	{
		if (mIsBound)
		{
			if (mService != null)
			{
				try
				{
					mService.send(Message.obtain(null, DigiClockService.MSG_SET_INT_VALUE, iMess, 0));
				}
				catch (RemoteException e)
				{
				}
			}
		}
	}
	
	public static boolean isConnected(Context context) 
	{
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }
	
	public void loadPreferences()
	{
		SharedPreferences settings = getSharedPreferences("org.landroo.digiclock.digiclock_preferences", MODE_PRIVATE);
		numColor = settings.getInt("numColor", 0x00000000);
		backColor = settings.getInt("backColor", 0x00000000);
	}		
}