package org.landroo.digiclock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ImgListDialog extends Dialog
{
	private ListView listView;
	private List<String> thumbNames = new ArrayList<String>();
	
	public interface OnImgChangedListener
	{
		void imgChanged(int color);
	}

	private OnImgChangedListener mListener;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		OnImgChangedListener l = new OnImgChangedListener()
		{
			public void imgChanged(int selected)
			{
				mListener.imgChanged(selected);
				dismiss();
			}
		};

		
		//setTitle("Color");
	}

	public ImgListDialog(Context context, OnImgChangedListener onImgChangedListener)
	{
		super(context);
		setContentView(R.layout.activity_img_list);
		mListener = onImgChangedListener;
		
		listView = (ListView) findViewById(R.id.imagesListView);
		listView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{

			}
		});

		loadPhotos();

		FileLisAdapter adapter = new FileLisAdapter(context, thumbNames);
		listView.setAdapter(adapter);

	}

	private int loadPhotos()
	{
		int iRet = 0;
		try
		{
			File[] imageFiles = new File(Environment.getExternalStorageDirectory() + File.separator + "DCIM/Camera")
					.listFiles();
			iRet = imageFiles.length;

			for (int i = 0; i < imageFiles.length; i++)
				thumbNames.add(imageFiles[i].getAbsolutePath());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return iRet;
	}

	private class FileLisAdapter extends BaseAdapter
	{

		List<String> files;
		private LayoutInflater mInflater;

		public FileLisAdapter(Context context, List<String> files)
		{
			this.files = files;
			this.mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount()
		{
			return files.size();
		}

		@Override
		public Object getItem(int position)
		{
			return files.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null) convertView = mInflater.inflate(R.layout.list_item, null);

			//UrlImageView thumb = (UrlImageView) convertView.findViewById(R.id.imageThumbImageView);

			String file = files.get(position);
			//thumb.setImageDrawable(file, 255, 255);

			TextView title = (TextView) convertView.findViewById(R.id.fileTitleTextView);
			String label = file.substring(file.lastIndexOf("/") + 1);
			title.setText(label);

			return convertView;
		}
	}
}
