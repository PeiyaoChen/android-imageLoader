package com.cpy.imageloader.activity;

import java.util.ArrayList;

import com.cpy.imageloader.R;
import com.cpy.imageloader.loader.ImageLoader;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MyAdapter extends BaseAdapter{

	ArrayList<String> mUrls = new ArrayList<>();
	Context mContext;
	private boolean isLoadImage = true;
	
	public MyAdapter(ArrayList<String> urls, Context context) {
		mUrls = urls;
		mContext = context;
	}
	@Override
	public int getCount() {
		return mUrls.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return mUrls.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout view;
		if(convertView != null)
			view = (LinearLayout)convertView;
		else {
			view = (LinearLayout)LayoutInflater.from(mContext).inflate(R.layout.listview_item, null);
		}
		ImageView imageView = (ImageView)view.findViewById(R.id.imageview);
		imageView.setImageResource(R.drawable.ic_launcher);
		if(isLoadImage)
			ImageLoader.getInstance(mContext.getApplicationContext()).loadImage(mUrls.get(position), imageView);
		Log.v("cpy", "get view " + position);
		return view;
	}
	
	public void setIsLoadImage(boolean isLoadImage) {
		this.isLoadImage = isLoadImage;
	}

}
