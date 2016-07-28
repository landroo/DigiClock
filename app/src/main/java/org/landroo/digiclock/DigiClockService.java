package org.landroo.digiclock;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class DigiClockService extends Service
{
	private final static String TAG = DigiClockService.class.getSimpleName();

	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_SET_INT_VALUE = 3;
	public static final int MSG_SET_STRING_VALUE = 4;
	
	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	private static boolean isRunning = false;
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	private NotificationManager notMan;
	
	private Timer clockTimer = null;
	
	// Handler of incoming messages from clients.
	class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				//Log.i(TAG, "Client registered");
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				//Log.i(TAG, "Client unregistered");
				break;
			case MSG_SET_INT_VALUE:
				switch(msg.arg1)
				{
				case 1:
					break;
				case 2:
					break;
				case 3:
					break;
				case 1000:
					DigiClockService.this.stopSelf();
					break;
				}
				break;
			case MSG_SET_STRING_VALUE:
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	
	@Override
	public IBinder onBind(Intent arg0) 
	{
		return mMessenger.getBinder();
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		clockTimer = new Timer();
		clockTimer.scheduleAtFixedRate(new clockTask(), 0, 1000);		
		
		isRunning = true;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		//Log.i(TAG, "onStartCommand " + startId + ": " + intent);
		return START_STICKY;
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
		//notMan.cancel(R.string.service_started);
		
		Log.i(TAG, "onDestroy");
		
		isRunning = false;
	}
	
	// service running
	public static boolean isRunning()
	{
		return isRunning;
	}	
	
	// send messages to the connected clients
	private void sendMessageToUI(int id, String sTitle)
	{
		for (int i = mClients.size() - 1; i >= 0; i--)
		{
			try
			{
				// Send data as an Integer
				if (id != -1)
				{
					mClients.get(i).send(Message.obtain(null, MSG_SET_INT_VALUE, id, 0));
					//Log.i(TAG, "" + id);
				}
				
				// Send data as a String
				if (!sTitle.equals(""))
				{
					Bundle b = new Bundle();
					b.putString("state", sTitle);
					Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
					msg.setData(b);
					mClients.get(i).send(msg);
					//Log.i(TAG, sTitle);
				}
			}
			catch (RemoteException e)
			{
				Log.i(TAG, "" + e);
				mClients.remove(i);
			}
		}
	}
	
	// show notification icon
	private void showNotification()
	{
		notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		//CharSequence text = getText(R.string.service_started);
		//Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
		//PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
		//notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);
		//notMan.notify(R.string.service_started, notification);
	}
	
	// timer task by second
	class clockTask extends TimerTask
	{
		// draw the moving graphics
		public void run()
		{
			if (isRunning)
			{

			}
		}
	}
}
