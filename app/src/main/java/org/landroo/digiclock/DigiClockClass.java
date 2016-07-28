package org.landroo.digiclock;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class DigiClockClass 
{
	private static final String TAG = DigiClockClass.class.getSimpleName();

	private int numWidth = 0;
	private int numHeight = 0;
	private int numSize = 0;
	
	private Bitmap[] bigMasks = new Bitmap[8];
	private BitmapDrawable[] bigParts = new BitmapDrawable[8];
	private BitmapDrawable[] bigShadow = new BitmapDrawable[8];
	
	private Bitmap[] smallMasks = new Bitmap[8];
	private BitmapDrawable[] smallParts = new BitmapDrawable[8];
	private BitmapDrawable[] smallShadow = new BitmapDrawable[8];
	
	private int[][][] partpos = new int[10][7][3];// number part positions
	
	private boolean is_textured = true;
	private boolean is_embossed = true;
	private boolean show_secound = true;
	
	public Primitive[] clockArray = new Primitive[44];// clock parts array
	
	// part class
	public class Primitive
	{
		public int type;
		public float posX, posY;
		public float rot;
		public float width, height;
		public BitmapDrawable drawable;
		public BitmapDrawable shadow;
		public boolean visible = true;
	
		// constructor
		public Primitive(int type, float posX, float posY, Bitmap bitmap, float rot)
		{
			this.type = type;
			this.posX = posX;
			this.posY = posY;
			this.rot = rot;
			
			this.drawable = new BitmapDrawable(bitmap);
			this.width = bitmap.getWidth();
			this.height = bitmap.getHeight();
			this.drawable.setBounds(0, 0, (int)width, (int)height);
			
			this.shadow = new BitmapDrawable(bitmap);
			this.shadow.setBounds(0, 0, (int)width, (int)height);
		}
	}
	
	//public List<Primitive> clockList = new ArrayList<Primitive>();// list of parts of time
	
	// constructor
	public DigiClockClass(int width, int height, Bitmap texture, boolean sec, int color, boolean istexture, boolean isemboss, int alpha)
	{
		show_secound = sec;
		is_textured = istexture;
		is_embossed = isemboss;
		
		if(sec) numWidth = width / 6;
		else numWidth = width / 5;
		numHeight = height;
		createParts(numWidth, height / 2, texture, color, alpha);
		numSize = numWidth / 4;
		initPartPos(width, height, numSize);
	}
	
	//resize bitmap
	private Bitmap strechImage(Bitmap image, float width, float height)
	{
		Bitmap bitmap = null;
		int origWidth = image.getWidth();
		int origHeight = image.getHeight();

		if(origWidth > width && origHeight > height)
		{
			bitmap = Bitmap.createBitmap((int)width, (int)height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			Paint paint = new Paint();
			canvas.drawBitmap(image, 0, 0, paint);
		}
		else
		{
			try
			{
				float newheight = height;
				float newwidth = width;
				if(height == 0) newheight = (float) width / (float) origWidth * (float) origHeight;
				if(width == 0) newwidth = (float) height / (float) origHeight * (float) origWidth;
				float scaleWidth = newwidth / origWidth;
				float scaleHeight = newheight / origHeight;
				Matrix matrix = new Matrix();
				matrix.postScale(scaleWidth, scaleHeight);
				bitmap = Bitmap.createBitmap(image, 0, 0, origWidth, origHeight, matrix, false);
			}
			catch (OutOfMemoryError e)
			{
				Log.e(TAG, "Out of memory error in new page!");
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Load image error!");
			}		
		}
		
		return bitmap;
	}
	
	// emboss effect
	private Bitmap processingBitmap_Emboss(Bitmap src)
	{
		int width = src.getWidth();
		int height = src.getHeight();

		Paint paintEmboss = new Paint();

		Bitmap dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(dest);

		Bitmap alpha = src.extractAlpha();
		
		float ambientValue = 0.5f;
		float specularValue = 0.5f;
		float blurRadiusValue = 5.0f;

		float[] direction = new float[] { 1, 1, 1 };
		EmbossMaskFilter embossMaskFilter = new EmbossMaskFilter(direction, ambientValue, specularValue, blurRadiusValue);

		paintEmboss.setMaskFilter(embossMaskFilter);
		canvas.drawBitmap(alpha, 0, 0, paintEmboss);

		return dest;
	}
	
	// cut a mask from an image
	private Bitmap maskImage(Bitmap image, Bitmap mask, int color)
	{
		int width = image.getWidth();
		int height = image.getHeight();

		Bitmap resImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(resImage);

		Paint imagePaint = new Paint();
		imagePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		imagePaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));

		canvas.drawBitmap(mask, 0, 0, null);
		canvas.drawBitmap(image, 0, 0, imagePaint);

		return resImage;
	}

	// texture image
	private Bitmap applyTexture(Bitmap image, Bitmap texture, int color)
	{
		Bitmap maskBitmap = image;
		// create mask
		//int width = image.getWidth();
		//int height = image.getHeight();
		//Bitmap maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		//Canvas maskCanvas = new Canvas(maskBitmap);
		//Paint maskPaint = new Paint();
		//maskPaint.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN));
		//maskCanvas.drawBitmap(image, 0, 0, maskPaint);
		
		// apply texture on mask
		Bitmap maskedBitmap = null;
		if(is_textured)
		{
			maskedBitmap = maskImage(texture, maskBitmap, color);
		}
		
		// emboss filter image
		if(is_embossed)
		{
			Bitmap embossBitmap = processingBitmap_Emboss(image);
			
			Canvas maskedCanvas = null;
			if(maskedBitmap != null)
				maskedCanvas = new Canvas(maskedBitmap);
			else
				maskedCanvas = new Canvas(maskBitmap);
			Paint maskedPaint = new Paint();
			maskedPaint.setAlpha(128);
			// apply emboss on masked image
			maskedCanvas.drawBitmap(embossBitmap, 0,  0, maskedPaint);
		}
		
		if(maskedBitmap != null)
			return maskedBitmap;
		else
			return maskBitmap;
	}
	
	// left top part
	private Bitmap drawRect11(float width, float height, int color, int alpha)
	{
		float w = width / 4;
		float h = height;

		Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAlpha(alpha);
		
		Path path = new Path();
		path.moveTo(0, 0);
		path.lineTo(w, w);
		path.lineTo(w, h - w / 2);
		path.lineTo(w / 2, h);
		path.lineTo(0, h - w / 2);
		path.lineTo(0, 0);
		
		canvas.drawPath(path, paint);
		
		return bitmap;
	}
	
	// left bottom part 
	private Bitmap drawRect12(float width, float height, int color, int alpha)
	{
		float w = width / 4;
		float h = height;

		Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAlpha(alpha);
		
		Path path = new Path();
		path.moveTo(w / 2, 0);
		path.lineTo(w, w / 2);
		path.lineTo(w, h - w);
		path.lineTo(0, h);
		path.lineTo(0, w / 2);
		path.lineTo(w / 2, 0);
		
		canvas.drawPath(path, paint);
		
		return bitmap;
	}
	
	// right top part
	private Bitmap drawRect13(float width, float height, int color, int alpha)
	{
		float w = width / 4;
		float h = height;

		Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAlpha(alpha);
		
		Path path = new Path();
		path.moveTo(w, 0);
		path.lineTo(w, h - w / 2);
		path.lineTo(w / 2, h);
		path.lineTo(0, h - w / 2);
		path.lineTo(0, w);
		path.lineTo(w, 0);
		
		canvas.drawPath(path, paint);
		
		return bitmap;
	}
	
	// right bottom part
	private Bitmap drawRect14(float width, float height, int color, int alpha)
	{
		float w = width / 4;
		float h = height;

		Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAlpha(alpha);
		
		Path path = new Path();
		path.moveTo(w / 2, 0);
		path.lineTo(w, w / 2);
		path.lineTo(w, h);
		path.lineTo(0, h - w);
		path.lineTo(0, w / 2);
		path.lineTo(w / 2, 0);
		
		canvas.drawPath(path, paint);
		
		return bitmap;
	}
	// up part
	private Bitmap drawRect21(float width, float height, int color, int alpha)
	{
		float w = width;
		float h = width / 4;

		Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAlpha(alpha);
		
		Path path = new Path();
		path.moveTo(0, 0);
		path.lineTo(h, h);
		path.lineTo(h, w - h);
		path.lineTo(w, 0);
		path.lineTo(0, 0);
		
		canvas.drawPath(path, paint);
		
		return bitmap;
	}
	
	// bottom part
	private Bitmap drawRect22(float width, float height, int color, int alpha)
	{
		float w = width;
		float h = width / 4;

		Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAlpha(alpha);
		
		Path path = new Path();
		path.moveTo(h, 0);
		path.lineTo(w - h, 0);
		path.lineTo(w, h);
		path.lineTo(0, h);
		path.lineTo(h, 0);
		
		canvas.drawPath(path, paint);
		
		return bitmap;
	}	
	
	// middle part 
	private Bitmap drawRect3(float width, float height, int color, int alpha)
	{
		float w = width;
		float h = width / 4;

		Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAlpha(alpha);
		
		Path path = new Path();
		path.moveTo(h, 0);
		path.lineTo(w - h, 0);
		path.lineTo(w - h / 2, h / 2);
		path.lineTo(w - h, h);
		path.lineTo(h, h);
		path.lineTo(h / 2, h / 2);
		path.lineTo(0, h);
		
		canvas.drawPath(path, paint);
		
		return bitmap;
	}
	
	// separator part 
	private Bitmap drawRect4(float width, float height, int color, int alpha)
	{
		float w = width / 4;
		float h = width / 4;

		Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAlpha(alpha);
		
		Rect rect = new Rect(0, 0, (int) w, (int)h);
		canvas.drawRect(rect, paint);
		
		return bitmap;
	}
	
	// create primitives
	private void createParts(float width, float height, Bitmap texture, int color, int alpha)
	{
		Bitmap pattern, bitmap;
		
		// left top big part
		bigMasks[0] = drawRect11(width, height, color, alpha);
		pattern = strechImage(texture, bigMasks[0].getWidth(), bigMasks[0].getHeight());
		bitmap = applyTexture(bigMasks[0], pattern, color);
		bigParts[0] = new BitmapDrawable(bitmap);
		bigParts[0].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// top big part
		bigMasks[1] = drawRect21(width, height, color, alpha);
		pattern = strechImage(texture, bigMasks[1].getWidth(), bigMasks[1].getHeight());
		bitmap = applyTexture(bigMasks[1], pattern, color);
		bigParts[1] = new BitmapDrawable(bitmap);
		bigParts[1].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// left bottom big part
		bigMasks[2] = drawRect12(width, height, color, alpha);
		pattern = strechImage(texture, bigMasks[2].getWidth(), bigMasks[2].getHeight());
		bitmap = applyTexture(bigMasks[2], pattern, color);
		bigParts[2] = new BitmapDrawable(bitmap);
		bigParts[2].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// middle big part
		bigMasks[3] = drawRect3(width, height, color, alpha);
		pattern = strechImage(texture, bigMasks[3].getWidth(), bigMasks[3].getHeight());
		bitmap = applyTexture(bigMasks[3], pattern, color);
		bigParts[3] = new BitmapDrawable(bitmap);
		bigParts[3].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// right top big part
		bigMasks[4] = drawRect13(width, height, color, alpha);
		pattern = strechImage(texture, bigMasks[4].getWidth(), bigMasks[4].getHeight());
		bitmap = applyTexture(bigMasks[4], pattern, color);
		bigParts[4] = new BitmapDrawable(bitmap);
		bigParts[4].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// right bottom big part
		bigMasks[5] = drawRect14(width, height, color, alpha);
		pattern = strechImage(texture, bigMasks[5].getWidth(), bigMasks[5].getHeight());
		bitmap = applyTexture(bigMasks[5], pattern, color);
		bigParts[5] = new BitmapDrawable(bitmap);
		bigParts[5].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// bottom big part
		bigMasks[6] = drawRect22(width, height, color, alpha);
		pattern = strechImage(texture, bigMasks[6].getWidth(), bigMasks[6].getHeight());
		bitmap = applyTexture(bigMasks[6], pattern, color);
		bigParts[6] = new BitmapDrawable(bitmap);
		bigParts[6].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// separator
		bigMasks[7] = drawRect4(width, height, color, alpha);
		pattern = strechImage(texture, bigMasks[7].getWidth(), bigMasks[7].getHeight());
		bitmap = applyTexture(bigMasks[7], pattern, color);
		bigParts[7] = new BitmapDrawable(bitmap);
		bigParts[7].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());

		// left top small part
		smallMasks[0] = drawRect11(width / 2, height / 2, color, alpha);
		pattern = strechImage(texture, smallMasks[0].getWidth(), smallMasks[0].getHeight());
		bitmap = applyTexture(smallMasks[0], pattern, color);
		smallParts[0] = new BitmapDrawable(bitmap);
		smallParts[0].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// top small part
		smallMasks[1] = drawRect21(width / 2, height / 2, color, alpha);
		pattern = strechImage(texture, smallMasks[1].getWidth(), smallMasks[1].getHeight());
		bitmap = applyTexture(smallMasks[1], pattern, color);
		smallParts[1] = new BitmapDrawable(bitmap);
		smallParts[1].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// left bottom small part
		smallMasks[2] = drawRect12(width / 2, height / 2, color, alpha);
		pattern = strechImage(texture, smallMasks[2].getWidth(), smallMasks[2].getHeight());
		bitmap = applyTexture(smallMasks[2], pattern, color);
		smallParts[2] = new BitmapDrawable(bitmap);
		smallParts[2].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// middle small part
		smallMasks[3] = drawRect3(width / 2, height / 2, color, alpha);
		pattern = strechImage(texture, smallMasks[3].getWidth(), smallMasks[3].getHeight());
		bitmap = applyTexture(smallMasks[3], pattern, color);
		smallParts[3] = new BitmapDrawable(bitmap);
		smallParts[3].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// right top small part
		smallMasks[4] = drawRect13(width / 2, height / 2, color, alpha);
		pattern = strechImage(texture, smallMasks[4].getWidth(), smallMasks[4].getHeight());
		bitmap = applyTexture(smallMasks[4], pattern, color);
		smallParts[4] = new BitmapDrawable(bitmap);
		smallParts[4].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// right bottom small part
		smallMasks[5] = drawRect14(width / 2, height / 2, color, alpha);
		pattern = strechImage(texture, smallMasks[5].getWidth(), smallMasks[5].getHeight());
		bitmap = applyTexture(smallMasks[5], pattern, color);
		smallParts[5] = new BitmapDrawable(bitmap);
		smallParts[5].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// bottom small part
		smallMasks[6] = drawRect22(width / 2, height / 2, color, alpha);
		pattern = strechImage(texture, smallMasks[6].getWidth(), smallMasks[6].getHeight());
		bitmap = applyTexture(smallMasks[6], pattern, color);
		smallParts[6] = new BitmapDrawable(bitmap);
		smallParts[6].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		// separator small
		smallMasks[7] = drawRect4(width / 2, height / 2, color, alpha);
		pattern = strechImage(texture, smallMasks[7].getWidth(), smallMasks[7].getHeight());
		bitmap = applyTexture(smallMasks[7], pattern, color);
		smallParts[7] = new BitmapDrawable(bitmap);
		smallParts[7].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		
		// big shadows
		int shadowColor = Color.GRAY;
		alpha = 24;
		bitmap = drawRect11(width, height, shadowColor, alpha);
		bigShadow[0] = new BitmapDrawable(bitmap);
		bigShadow[0].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect21(width, height, shadowColor, alpha);
		bigShadow[1] = new BitmapDrawable(bitmap);
		bigShadow[1].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect12(width, height, shadowColor, alpha);
		bigShadow[2] = new BitmapDrawable(bitmap);
		bigShadow[2].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect3(width, height, shadowColor, alpha);
		bigShadow[3] = new BitmapDrawable(bitmap);
		bigShadow[3].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect13(width, height, shadowColor, alpha);
		bigShadow[4] = new BitmapDrawable(bitmap);
		bigShadow[4].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect14(width, height, shadowColor, alpha);
		bigShadow[5] = new BitmapDrawable(bitmap);
		bigShadow[5].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect22(width, height, shadowColor, alpha);
		bigShadow[6] = new BitmapDrawable(bitmap);
		bigShadow[6].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect4(width, height, shadowColor, alpha);
		bigShadow[7] = new BitmapDrawable(bitmap);
		bigShadow[7].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		
		// small shadows
		bitmap = drawRect11(width / 2, height / 2, shadowColor, alpha);
		smallShadow[0] = new BitmapDrawable(bitmap);
		smallShadow[0].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect21(width / 2, height / 2, shadowColor, alpha);
		smallShadow[1] = new BitmapDrawable(bitmap);
		smallShadow[1].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect12(width / 2, height / 2, shadowColor, alpha);
		smallShadow[2] = new BitmapDrawable(bitmap);
		smallShadow[2].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect3(width / 2, height / 2, shadowColor, alpha);
		smallShadow[3] = new BitmapDrawable(bitmap);
		smallShadow[3].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect13(width / 2, height / 2, shadowColor, alpha);
		smallShadow[4] = new BitmapDrawable(bitmap);
		smallShadow[4].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect14(width / 2, height / 2, shadowColor, alpha);
		smallShadow[5] = new BitmapDrawable(bitmap);
		smallShadow[5].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect22(width / 2, height / 2, shadowColor, alpha);
		smallShadow[6] = new BitmapDrawable(bitmap);
		smallShadow[6].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		bitmap = drawRect4(width / 2, height / 2, shadowColor, alpha);
		smallShadow[7] = new BitmapDrawable(bitmap);
		smallShadow[7].setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());

		return;
	}
	
	public void createNums(int px, int py, long time)
	{
		//clockList.clear();
		
		SimpleDateFormat dt = new SimpleDateFormat("HH:mm.ss"); 
		Date date = new Date(time);
		String hms = dt.format(date);
		
		int no, x = 0, cnt = 0;
		String s;
		boolean big = true;
		for(int n = 0; n < hms.length(); n++)
		{
			s = "" + hms.charAt(n);
			if(s.equals(":"))
			{
				clockArray[cnt].posX = x + px + numWidth / 8 - numSize / 4;
				clockArray[cnt].posY = py + numHeight / 4;
				
				clockArray[cnt].drawable = bigParts[7];
				clockArray[cnt].width = bigMasks[7].getWidth();
				clockArray[cnt].height = bigMasks[7].getHeight();

				clockArray[cnt].shadow = bigShadow[7];
				clockArray[cnt].width = bigMasks[7].getWidth();
				clockArray[cnt].height = bigMasks[7].getHeight();
				
				clockArray[cnt].visible = !clockArray[cnt].visible;
				cnt++;
				
				clockArray[cnt].posX = x + px + numWidth / 8 - numSize / 4;
				clockArray[cnt].posY = py + numHeight / 4 * 3 - numSize;
				
				clockArray[cnt].drawable = bigParts[7];
				clockArray[cnt].width = bigMasks[7].getWidth();
				clockArray[cnt].height = bigMasks[7].getHeight();

				clockArray[cnt].shadow = bigShadow[7];
				clockArray[cnt].width = bigMasks[7].getWidth();
				clockArray[cnt].height = bigMasks[7].getHeight();
				
				clockArray[cnt].visible = !clockArray[cnt].visible;
				cnt++;
				
				x += numWidth / 2;
			}
			else if(s.equals("."))
			{
				big = false;
			}
			else
			{
				no = Integer.parseInt(s);
				for(int i = 0; i < 7; i++)
				{
					if(partpos[no][i][0] != -1)
					{
						if(big)
						{
							clockArray[cnt].posX = x + px + partpos[no][i][0];
							clockArray[cnt].posY = py + partpos[no][i][1];
							
							clockArray[cnt].drawable = bigParts[partpos[no][i][2]];
							clockArray[cnt].width = bigMasks[partpos[no][i][2]].getWidth();
							clockArray[cnt].height = bigMasks[partpos[no][i][2]].getHeight();
							
							if(no == 0 && x == 0)
								clockArray[cnt].visible = false;
							else
								clockArray[cnt].visible = true;
						}
						else
						{
							clockArray[cnt].posX = x + px + partpos[no][i][0] / 2;
							clockArray[cnt].posY = py + partpos[no][i][1] / 2 + numHeight / 2;
							clockArray[cnt].drawable = smallParts[partpos[no][i][2]];
							clockArray[cnt].width = smallMasks[partpos[no][i][2]].getWidth();
							clockArray[cnt].height = smallMasks[partpos[no][i][2]].getHeight();
							clockArray[cnt].visible = show_secound;
						}
					}
					else
					{
						clockArray[cnt].visible = false;
					}
					
					if(big)
					{
						clockArray[cnt].shadow = bigShadow[partpos[8][i][2]];
						clockArray[cnt].width = bigMasks[partpos[8][i][2]].getWidth();
						clockArray[cnt].height = bigMasks[partpos[8][i][2]].getHeight();
					}
					else
					{
						clockArray[cnt].shadow = smallShadow[partpos[8][i][2]];
						clockArray[cnt].width = smallMasks[partpos[8][i][2]].getWidth();
						clockArray[cnt].height = smallMasks[partpos[8][i][2]].getHeight();
					}
					cnt++;
				}
				if(big)	x += numWidth + numWidth / 10;
				else x += numWidth / 2 + numWidth / 20;
			}			
		}
		
	}
	
	private void initPartPos(int width, int height, int size)
	{
		init0(width, height, size);
		init1(width, height, size);
		init2(width, height, size);
		init3(width, height, size);
		init4(width, height, size);
		init5(width, height, size);
		init6(width, height, size);
		init7(width, height, size);
		init8(width, height, size);
		init9(width, height, size);
		
		for (int i = 0; i < clockArray.length; i++)
		{
			Primitive prim = new Primitive(1, partpos[8][0][0], partpos[8][0][1], bigMasks[0], 0);
			clockArray[i] = prim;
		}
	}
	
	private void init0(int width, int height, int size)
	{
		// left top
		partpos[0][0][0] = 0;
		partpos[0][0][1] = 0;
		partpos[0][0][2] = 0;
		// left bottom
		partpos[0][1][0] = 0;
		partpos[0][1][1] = height / 2;
		partpos[0][1][2] = 2;
		// top
		partpos[0][2][0] = 0;
		partpos[0][2][1] = 0;
		partpos[0][2][2] = 1;
		// middle
		partpos[0][3][0] = -1;
		partpos[0][3][1] = -1;
		partpos[0][3][2] = -1;
		// bottom
		partpos[0][4][0] = 0;
		partpos[0][4][1] = height - size;
		partpos[0][4][2] = 6;
		// right top
		partpos[0][5][0] = numWidth - size;
		partpos[0][5][1] = 0;
		partpos[0][5][2] = 4;
		// right bottom
		partpos[0][6][0] = numWidth - size;
		partpos[0][6][1] = height / 2;
		partpos[0][6][2] = 5;
	}
	
	private void init1(int width, int height, int size)
	{
		// left top
		partpos[1][0][0] = -1;
		partpos[1][0][1] = -1;
		partpos[1][0][2] = -1;
		// left bottom
		partpos[1][1][0] = -1;
		partpos[1][1][1] = -1;
		partpos[1][1][2] = -1;
		// top
		partpos[1][2][0] = -1;
		partpos[1][2][1] = -1;
		partpos[1][2][2] = 11;
		// middle
		partpos[1][3][0] = -1;
		partpos[1][3][1] = -1;
		partpos[1][3][2] = -1;
		// bottom
		partpos[1][4][0] = -1;
		partpos[1][4][1] = -1;
		partpos[1][4][2] = -1;
		// right top
		partpos[1][5][0] = numWidth - size;
		partpos[1][5][1] = 0;
		partpos[1][5][2] = 4;
		// right bottom
		partpos[1][6][0] = numWidth - size;
		partpos[1][6][1] = height / 2;
		partpos[1][6][2] = 5;
	}

	private void init2(int width, int height, int size)
	{
		// left top
		partpos[2][0][0] = -1;
		partpos[2][0][1] = -1;
		partpos[2][0][2] = -1;
		// left bottom
		partpos[2][1][0] = 0;
		partpos[2][1][1] = height / 2;
		partpos[2][1][2] = 2;
		// top
		partpos[2][2][0] = 0;
		partpos[2][2][1] = 0;
		partpos[2][2][2] = 1;
		// middle
		partpos[2][3][0] = 0;
		partpos[2][3][1] = height / 2 - size / 2;
		partpos[2][3][2] = 3;
		// bottom
		partpos[2][4][0] = 0;
		partpos[2][4][1] = height - size;
		partpos[2][4][2] = 6;
		// right top
		partpos[2][5][0] = numWidth - size;
		partpos[2][5][1] = 0;
		partpos[2][5][2] = 4;
		// right bottom
		partpos[2][6][0] = -1;
		partpos[2][6][1] = -1;
		partpos[2][6][2] = -1;
	}

	private void init3(int width, int height, int size)
	{
		// left top
		partpos[3][0][0] = -1;
		partpos[3][0][1] = -1;
		partpos[3][0][2] = -1;
		// left bottom
		partpos[3][1][0] = -1;
		partpos[3][1][1] = -1;
		partpos[3][1][2] = -1;
		// top
		partpos[3][2][0] = 0;
		partpos[3][2][1] = 0;
		partpos[3][2][2] = 1;
		// middle
		partpos[3][3][0] = 0;
		partpos[3][3][1] = height / 2 - size / 2;
		partpos[3][3][2] = 3;
		// bottom
		partpos[3][4][0] = 0;
		partpos[3][4][1] = height - size;
		partpos[3][4][2] = 6;
		// right top
		partpos[3][5][0] = numWidth - size;
		partpos[3][5][1] = 0;
		partpos[3][5][2] = 4;
		// right bottom
		partpos[3][6][0] = numWidth - size;
		partpos[3][6][1] = height / 2;
		partpos[3][6][2] = 5;
	}

	private void init4(int width, int height, int size)
	{
		// left top
		partpos[4][0][0] = 0;
		partpos[4][0][1] = 0;
		partpos[4][0][2] = 0;
		// left bottom
		partpos[4][1][0] = -1;
		partpos[4][1][1] = -1;
		partpos[4][1][2] = -1;
		// top
		partpos[4][2][0] = -1;
		partpos[4][2][1] = -1;
		partpos[4][2][2] = 11;
		// middle
		partpos[4][3][0] = 0;
		partpos[4][3][1] = height / 2 - size / 2;
		partpos[4][3][2] = 3;
		// bottom
		partpos[4][4][0] = -1;
		partpos[4][4][1] = -1;
		partpos[4][4][2] = -1;
		// right top
		partpos[4][5][0] = numWidth - size;
		partpos[4][5][1] = 0;
		partpos[4][5][2] = 4;
		// right bottom
		partpos[4][6][0] = numWidth - size;
		partpos[4][6][1] = height / 2;
		partpos[4][6][2] = 5;
	}

	private void init5(int width, int height, int size)
	{
		// left top
		partpos[5][0][0] = 0;
		partpos[5][0][1] = 0;
		partpos[5][0][2] = 0;
		// left bottom
		partpos[5][1][0] = -1;
		partpos[5][1][1] = -1;
		partpos[5][1][2] = -1;
		// top
		partpos[5][2][0] = 0;
		partpos[5][2][1] = 0;
		partpos[5][2][2] = 1;
		// middle
		partpos[5][3][0] = 0;
		partpos[5][3][1] = height / 2 - size / 2;
		partpos[5][3][2] = 3;
		// bottom
		partpos[5][4][0] = 0;
		partpos[5][4][1] = height - size;
		partpos[5][4][2] = 6;
		// right top
		partpos[5][5][0] = -1;
		partpos[5][5][1] = -1;
		partpos[5][5][2] = -1;
		// right bottom
		partpos[5][6][0] = numWidth - size;
		partpos[5][6][1] = height / 2;
		partpos[5][6][2] = 5;
	}
	
	private void init6(int width, int height, int size)
	{
		// left top
		partpos[6][0][0] = 0;
		partpos[6][0][1] = 0;
		partpos[6][0][2] = 0;
		// left bottom
		partpos[6][1][0] = 0;
		partpos[6][1][1] = height / 2;
		partpos[6][1][2] = 2;
		// top
		partpos[6][2][0] = 0;
		partpos[6][2][1] = 0;
		partpos[6][2][2] = 1;
		// middle
		partpos[6][3][0] = 0;
		partpos[6][3][1] = height / 2 - size / 2;
		partpos[6][3][2] = 3;
		// bottom
		partpos[6][4][0] = 0;
		partpos[6][4][1] = height - size;
		partpos[6][4][2] = 6;
		// right top
		partpos[6][5][0] = -1;
		partpos[6][5][1] = -1;
		partpos[6][5][2] = -1;
		// right bottom
		partpos[6][6][0] = numWidth - size;
		partpos[6][6][1] = height / 2;
		partpos[6][6][2] = 5;
	}
	
	private void init7(int width, int height, int size)
	{
		// left top
		partpos[7][0][0] = -1;
		partpos[7][0][1] = -1;
		partpos[7][0][2] = -1;
		// left bottom
		partpos[7][1][0] = -1;
		partpos[7][1][1] = -1;
		partpos[7][1][2] = -1;
		// top
		partpos[7][2][0] = 0;
		partpos[7][2][1] = 0;
		partpos[7][2][2] = 1;
		// middle
		partpos[7][3][0] = -1;
		partpos[7][3][1] = -1;
		partpos[7][3][2] = -1;
		// bottom
		partpos[7][4][0] = -1;
		partpos[7][4][1] = -1;
		partpos[7][4][2] = -1;
		// right top
		partpos[7][5][0] = numWidth - size;
		partpos[7][5][1] = 0;
		partpos[7][5][2] = 4;
		// right bottom
		partpos[7][6][0] = numWidth - size;
		partpos[7][6][1] = height / 2;
		partpos[7][6][2] = 5;
	}

	private void init8(int width, int height, int size)
	{
		// left top
		partpos[8][0][0] = 0;
		partpos[8][0][1] = 0;
		partpos[8][0][2] = 0;
		// left bottom
		partpos[8][1][0] = 0;
		partpos[8][1][1] = height / 2;
		partpos[8][1][2] = 2;
		// top
		partpos[8][2][0] = 0;
		partpos[8][2][1] = 0;
		partpos[8][2][2] = 1;
		// middle
		partpos[8][3][0] = 0;
		partpos[8][3][1] = height / 2 - size / 2;
		partpos[8][3][2] = 3;
		// bottom
		partpos[8][4][0] = 0;
		partpos[8][4][1] = height - size;
		partpos[8][4][2] = 6;
		// right top
		partpos[8][5][0] = numWidth - size;
		partpos[8][5][1] = 0;
		partpos[8][5][2] = 4;
		// right bottom
		partpos[8][6][0] = numWidth - size;
		partpos[8][6][1] = height / 2;
		partpos[8][6][2] = 5;
	}
	
	private void init9(int width, int height, int size)
	{
		// left top
		partpos[9][0][0] = 0;
		partpos[9][0][1] = 0;
		partpos[9][0][2] = 0;
		// left bottom
		partpos[9][1][0] = -1;
		partpos[9][1][1] = -1;
		partpos[9][1][2] = -1;
		// top
		partpos[9][2][0] = 0;
		partpos[9][2][1] = 0;
		partpos[9][2][2] = 1;
		// middle
		partpos[9][3][0] = 0;
		partpos[9][3][1] = height / 2 - size / 2;
		partpos[9][3][2] = 3;
		// bottom
		partpos[9][4][0] = 0;
		partpos[9][4][1] = height - size;
		partpos[9][4][2] = 6;
		// right top
		partpos[9][5][0] = numWidth - size;
		partpos[9][5][1] = 0;
		partpos[9][5][2] = 4;
		// right bottom
		partpos[9][6][0] = numWidth - size;
		partpos[9][6][1] = height / 2;
		partpos[9][6][2] = 5;
	}

	public Drawable createBackground(int width, int height, int color, Bitmap texture)
	{
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setColor(color);
		
		Rect rect = new Rect(0, 0, width, height);
		canvas.drawRect(rect, paint);
		
		Bitmap pattern = strechImage(texture, width, height);
		bitmap = applyTexture(bitmap, pattern, color);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, width, height);
		
		return drawable;
	}

}
