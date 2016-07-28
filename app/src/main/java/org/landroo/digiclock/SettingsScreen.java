package org.landroo.digiclock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.landroo.digiclock.ColorPickerDialog.OnColorChangedListener;
import org.landroo.digiclock.ImgListDialog.OnImgChangedListener;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class SettingsScreen extends Activity
{
	private static final String TAG = SettingsScreen.class.getSimpleName();
	
	private ListView listView;
	private List<String> settnigTitles = new ArrayList<String>();
	private List<String> settnigDesc = new ArrayList<String>();
	private SettingsListAdapter settingsAdapter;
	
	private int numColor;
	private int backColor;
	private boolean showSec;
	
	private int displayWidth;
	private int displayHeight;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();        
		
		loadPreferences();
		
		listView = (ListView) findViewById(R.id.settingsListView);
		listView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				switch(arg2)
				{
				case 0:
					new ColorPickerDialog((Context)SettingsScreen.this, new OnColorChangedListener()
					{
						@Override
						public void colorChanged(int color)
						{
							numColor = color;
							settingsAdapter.notifyDataSetChanged();
						}
					}, numColor, displayHeight, displayHeight).show();
					break;
				case 1:
					new ImgListDialog((Context)SettingsScreen.this, new OnImgChangedListener()
					{
						@Override
						public void imgChanged(int selected)
						{
//							settingsAdapter.notifyDataSetChanged();
						}
					}).show();
					break;					
				case 2:
					new ColorPickerDialog((Context)SettingsScreen.this, new OnColorChangedListener()
					{
						@Override
						public void colorChanged(int color)
						{
							backColor = color;
							settingsAdapter.notifyDataSetChanged();
						}
					}, backColor, displayHeight, displayHeight).show();
					break;
				case 4:
					showSec = !showSec;
					settingsAdapter.notifyDataSetChanged();
					break;
				}
			}
		});
		
		TypedArray titles = getResources().obtainTypedArray(R.array.setting_titles);
		for(int i = 0; i < titles.length(); i++)
			settnigTitles.add(titles.getString(i));
		
		TypedArray desc = getResources().obtainTypedArray(R.array.setting_descriptions);
		for(int i = 0; i < desc.length(); i++)
			settnigDesc.add(desc.getString(i));
		
		settingsAdapter = new SettingsListAdapter(this, (ArrayList<String>) settnigTitles);
		listView.setAdapter(settingsAdapter);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		savePreferences();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		loadPreferences();
	}
	
	private class SettingsListAdapter extends ArrayAdapter<String>
	{
		private ArrayList<String> values;
		private TextView textView;
		private View rowView;
		private LayoutInflater inflater;
		private ImageView imageView;

		public SettingsListAdapter(Context context, ArrayList<String> values)
		{
			super(context, R.layout.setting_row, values);
			this.values = values;
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			try
			{
				rowView = convertView;
				if (rowView == null) rowView = inflater.inflate(R.layout.setting_row, parent, false);

				String sText = values.get(position);
				textView = (TextView) rowView.findViewById(R.id.label);
				textView.setText(sText);
				
				sText = values.get(position);
				textView = (TextView) rowView.findViewById(R.id.label);
				textView.setText(sText);
				
				imageView = (ImageView) rowView.findViewById(R.id.icon);
				Bitmap bitmap;
				switch(position)
				{
				case 0:
					bitmap = createColor(displayHeight / 8, displayHeight / 8, numColor);
					imageView.setImageBitmap(bitmap);
					break;
				case 1:
					//imageView.setImageResource(R.drawable.ent_lengend);
					break;
				case 2:
					bitmap = createColor(displayHeight / 8, displayHeight / 8, backColor);
					imageView.setImageBitmap(bitmap);
					break;
				case 3:
					//imageView.setImageResource(R.drawable.info_legend);
					break;
				case 4:
					if(showSec)
						imageView.setImageResource(R.drawable.checked);
					else
						imageView.setImageResource(R.drawable.frame);
					break;
				case 5:
					//imageView.setImageResource(R.drawable.man_legend);
					break;
				case 6:
					//imageView.setImageResource(R.drawable.new_nagy);
					break;
				case 7:
					//imageView.setImageResource(R.drawable.hosp);
					break;
				case 8:
					//imageView.setImageResource(R.drawable.up);
					break;
				}
			}
			catch (OutOfMemoryError e)
			{
				Log.e(TAG, "Out of memory error in SettingsListAdapter!");
				System.gc();
			}

			return rowView;
		}

	}
	
	public void loadPreferences()
	{
		SharedPreferences settings = getSharedPreferences("org.landroo.digiclock.bitrack_preferences", MODE_PRIVATE);
		numColor = settings.getInt("numColor", 0x00000000);
		backColor = settings.getInt("backColor", 0x00000000);
	}	

	public void savePreferences()
	{
		SharedPreferences settings = getSharedPreferences("org.landroo.digiclock.bitrack_preferences", MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("numColor", numColor);
		editor.putInt("backColor", backColor);
		editor.commit();
	}
	
	public Bitmap createColor(int width, int height, int color)
	{
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setColor(color);
		
		Rect rect = new Rect(0, 0, width, height);
		canvas.drawRect(rect, paint);
		
		return bitmap;
	}
}
