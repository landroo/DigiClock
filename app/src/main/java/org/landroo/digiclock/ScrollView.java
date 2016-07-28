package org.landroo.digiclock;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.landroo.ui.UIInterface;

import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.FloatMath;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

public class ScrollView implements UIInterface
{
	private static final String TAG = ScrollView.class.getSimpleName();
	private static final int SWIPE_INTERVAL = 10;
	
	private int displayWidth = 0; // display width
	private int displayHeight = 0; // display height
	private float pictureWidth;
	private float pictureHeight;
	private float origWidth;
	private float origHeight;
	
	private float xPos = 0;
	private float yPos = 0;
	
	private float sX = 0;
	private float sY = 0;
	private float mX = 0;
	private float mY = 0;
	
	private Timer swipeTimer = null;
	private float swipeDistX = 0;
	private float swipeDistY = 0;
	private float swipeVelocity = 0;
	private float swipeSpeed = 0;

	private float backSpeedX = 0;
	private float backSpeedY = 0;
	private float offMarginX = 0;
	private float offMarginY = 0;
	
	private float zoomSize = 0;
	private float zoomX = 1;
	private float zoomY = 1;
	
	private View view;
	
	private boolean checkoff;
		
	public ScrollView(int displayWidth, int displayHeight, int pictureWidth, int pictureHeight, View view, boolean checkoff)
	{
		this.displayWidth = displayWidth;
		this.displayHeight = displayHeight;
		this.pictureWidth = pictureWidth;
		this.pictureHeight = pictureHeight;
		
		origWidth = pictureWidth;
		origHeight = pictureHeight;
		
		offMarginX = displayWidth;
		offMarginY = displayHeight;
	
		xPos = (displayWidth - pictureWidth) / 2;
		yPos = (displayHeight - pictureHeight) / 2;
		
		swipeTimer = new Timer();
		swipeTimer.scheduleAtFixedRate(new SwipeTask(), 0, SWIPE_INTERVAL);
		
		this.view = view;
		this.checkoff = checkoff;
	}
	
	public float xPos()
	{
		return xPos;
	}
	
	public float yPos()
	{
		return yPos;
	}
	
	public void setXPos(float x)
	{
		xPos = x;
	}
	
	public void setYPos(float y)
	{
		yPos = y;
	}
	
	public void setSize(int displayWidth, int displayHeight, int pictureWidth, int pictureHeight)
	{
		this.displayWidth = displayWidth;
		this.displayHeight = displayHeight;
		this.pictureWidth = pictureWidth;
		this.pictureHeight = pictureHeight;
		
		origWidth = pictureWidth;
		origHeight = pictureHeight;
	}
	
	private void Invalidate()
	{
		view.postInvalidate();
	}
	
	@Override
	public void onDown(float x, float y)
	{
		sX = x;
		sY = y;

		swipeVelocity = 0;

		Invalidate();
		
		return;
	}

	@Override
	public void onUp(float x, float y)
	{
		if(checkoff) checkOff();
		
		Invalidate();
		
		return;
	}

	@Override
	public void onTap(float x, float y)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onHold(float x, float y)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMove(float x, float y)
	{
		mX = x;
		mY = y;

		float dx = mX - sX;
		float dy = mY - sY;

		// picture bigger than the display
		if (pictureWidth > displayWidth || pictureHeight > displayHeight)
		{
			if(xPos + dx < displayWidth - (pictureWidth + offMarginX) || xPos + dx > offMarginX) dx = 0;
			if(yPos + dy < displayHeight - (pictureHeight + offMarginY) || yPos + dy > offMarginY) dy = 0;
		}
		else
		{
			if(xPos + dx > displayWidth - pictureWidth || xPos + dx < 0) dx = 0;
			if(yPos + dy > displayHeight - pictureHeight || yPos + dy < 0) dy = 0;
		}

		xPos += dx;
		yPos += dy;

		sX = mX;
		sY = mY;

		Invalidate();

		return;
	}

	@Override
	public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2)
	{
		swipeDistX = x2 - x1;
		swipeDistY = y2 - y1;
		swipeSpeed = 1;
		swipeVelocity = velocity;

		Invalidate();

		return;
	}

	@Override
	public void onDoubleTap(float x, float y)
	{
		swipeVelocity = 0;
		
		backSpeedX = 0;
		backSpeedY = 0;
	
		pictureWidth = origWidth;
		pictureHeight = origHeight;

		xPos = (displayWidth - pictureWidth) / 2;
		yPos = (displayHeight - pictureHeight) / 2;

		// set original bounds
		zoomX = 1;
		zoomY = 1;

		Invalidate();
		
		return;
	}

	@Override
	public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff)
	{
		float dist = distance * 10;
		switch (mode)
		{
		case 1:
			zoomSize = dist;
			break;
		case 2:
			float diff = dist - zoomSize;
			float sizeNew = FloatMath.sqrt(pictureWidth * pictureWidth + pictureHeight * pictureHeight);
			float sizeDiff = 100 / (sizeNew / (sizeNew + diff));
			float newSizeX = pictureWidth * sizeDiff / 100;
			float newSizeY = pictureHeight * sizeDiff / 100;

			// zoom between min and max value
			if (newSizeX > origWidth / 4 && newSizeX < origWidth * 10)
			{
				zoomX = pictureWidth / origWidth;
				zoomY = pictureHeight / origHeight;

				float diffX = newSizeX - pictureWidth;
				float diffY = newSizeY - pictureHeight;
				float xPer = 100 / (pictureWidth / (Math.abs(xPos) + mX)) / 100;
				float yPer = 100 / (pictureHeight / (Math.abs(yPos) + mY)) / 100;
				
				xPos -= diffX * xPer;
				yPos -= diffY * yPer;

				pictureWidth = newSizeX;
				pictureHeight = newSizeY;
				
				zoomSize = dist;

				//Log.i(TAG, "Math.abs(diffX * xPer - lastMidX) " + Math.abs(diffX * xPer - lastMidX));

				if (pictureWidth > displayWidth || pictureHeight > displayHeight)
				{
					if (xPos > 0) xPos = 0;
					if (yPos > 0) yPos = 0;

					if (xPos + pictureWidth < displayWidth) xPos = displayWidth - pictureWidth;
					if (yPos + pictureHeight < displayHeight) yPos = displayHeight - pictureHeight;
				}
				else
				{
					if (xPos <= 0) xPos = 0;
					if (yPos <= 0) yPos = 0;

					if (xPos + pictureWidth > displayWidth) xPos = displayWidth - pictureWidth;
					if (yPos + pictureHeight > displayHeight) yPos = displayHeight - pictureHeight;
				}
				
				// Log.i(TAG, "" + xPos + " " + yPos);
			}
			break;
		case 3:
			zoomSize = 0;
			break;
		}

		Invalidate();

		return;
	}

	@Override
	public void onRotate(int mode, float x, float y, float angle)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFingerChange()
	{
		// TODO Auto-generated method stub
		
	}
	
	class SwipeTask extends TimerTask
	{
		public void run()
		{
			boolean redraw = false;
			if (swipeVelocity > 0)
			{
				float dist = FloatMath.sqrt(swipeDistY * swipeDistY + swipeDistX * swipeDistX);
				float x = xPos - (float) ((swipeDistX / dist) * (swipeVelocity / 10));
				float y = yPos - (float) ((swipeDistY / dist) * (swipeVelocity / 10));

				if ((pictureWidth > displayWidth) && (x < displayWidth - pictureWidth || x > 0)
						|| ((pictureWidth <= displayWidth) && (x > displayWidth - pictureWidth || x < 0)))
				{
					swipeDistX *= -1;
					swipeSpeed += .1;
				}

				if ((pictureHeight > displayHeight) && (y < displayHeight - pictureHeight || y > 0)
						|| ((pictureHeight <= displayHeight) && (y > displayHeight - pictureHeight || y < 0)))
				{
					swipeDistY *= -1;
					swipeSpeed += .1;
				}

				xPos -= (float) ((swipeDistX / dist) * (swipeVelocity / 10));
				yPos -= (float) ((swipeDistY / dist) * (swipeVelocity / 10));

				swipeVelocity -= swipeSpeed;
				swipeSpeed += .0001;

				redraw = true;
				
				if(swipeVelocity <= 0 && checkoff) checkOff();
			}
			
			if(backSpeedX != 0)
			{
				if((backSpeedX < 0 && xPos <= 0.1f) || (backSpeedX > 0 && xPos + 0.1f >= displayWidth - pictureWidth)) backSpeedX = 0;
				else if(backSpeedX < 0) xPos -= xPos / 20;
				else xPos += (displayWidth - (pictureWidth + xPos)) / 20;

				redraw = true;
			}
			
			if(backSpeedY != 0)
			{
				if((backSpeedY < 0 && yPos <= 0.1f) || (backSpeedY > 0 && yPos + 0.1f >= displayHeight - pictureHeight)) backSpeedY = 0;
				else if(backSpeedY < 0) yPos -= yPos / 20;
				else yPos += (displayHeight - (pictureHeight + yPos)) / 20;
				
				redraw = true;
			}
			
			if(redraw) Invalidate();
			
			return;
		}
	}
	
	private void checkOff()
	{
		if(pictureWidth >= displayWidth)
		{
			if(xPos > 0 && xPos <= offMarginX) backSpeedX = -1;
			else if(xPos < pictureWidth - offMarginX && xPos <= pictureWidth) backSpeedX = 1;
		}
		if(pictureHeight >= displayHeight)
		{
			if(yPos > 0 && yPos <= offMarginY) backSpeedY = -1;
			else if(yPos < pictureHeight - offMarginY && yPos <= pictureHeight) backSpeedY = 1;
		}
	}
	
	public float getZoomX()
	{
		return zoomX;
	}

	public float getZoomY()
	{
		return zoomY;
	}

}
