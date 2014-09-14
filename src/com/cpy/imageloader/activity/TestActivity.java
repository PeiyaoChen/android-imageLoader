package com.cpy.imageloader.activity;

import java.util.ArrayList;

import com.cpy.imageloader.R;
import com.cpy.imageloader.loader.ImageLoader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class TestActivity extends Activity{

	MyAdapter adapter;
	ArrayList<String> urls;
	public static Context mContext;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.testactivity);
		mContext = this;
		String[] urlArray = new String[]{
				"http://img03.taobaocdn.com/tps/i3/T1lh1IXClcXXajoXZd-205-130.jpg",
		          "http://img02.taobaocdn.com/tps/i2/T1oN5LXvBdXXajoXZd-205-130.jpg",
		          "http://img03.taobaocdn.com/tps/i3/T1FbyMXrJcXXbByyEK-755-260.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/871886077/T24g65Xg8aXXXXXXXX_!!871886077.jpg_240x240.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/723989220/T20AupXg4cXXXXXXXX_!!723989220.jpg_240x240.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/479218086/T25T2ZXaJbXXXXXXXX_!!479218086.jpg_240x240.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/228784630/T2OBD4XjFaXXXXXXXX_!!228784630.jpg_240x240.jpg",
		          "http://img01.taobaocdn.com/imgextra/i1/240252102/T2.9H4Xh8aXXXXXXXX_!!240252102.jpg_240x240.jpg",
		          "http://img02.taobaocdn.com/tps/i2/T1gXWBXvJhXXaDZLo7-240-160.jpg_240x240.jpg",
		          "http://img04.taobaocdn.com/imgextra/i4/667336301/T2EMrwXaXbXXXXXXXX_!!667336301.jpg_240x240.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/458599810/T2Xn23XfRXXXXXXXXX_!!458599810.jpg_240x240.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i1/T1..SKXwheXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i1/T1lLCEXsxgXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i2/T1DOmEXvldXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i3/T1mkmtXwdjXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i3/T1ctGIXElbXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i1/T1bYeIXt4aXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i3/T14ad6XE0jXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i4/T1S0xTXt4hXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i4/T1nbThXipvXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i1/T1AoRAXppcXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i2/T1ddyhXr8XXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i3/T1RNOGXvJbXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i3/T13yGuXvXdXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i2/T1lXR2Xy4fXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1GmOeXtdhXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i3/T1mLaHXzxbXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i2/T1c_qwXu8hXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i2/T1UImBXthbXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i3/T1jrqAXDJhXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i2/T1sTmzXARdXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i1/T1n_mqXDJeXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1WkSFXCxfXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i1/T1Mp5EXA8XXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i2/T1w1WBXrXfXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i2/T19PGrXulXXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i4/T19d5HXsNbXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i3/T1X19cXxpgXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i1/T1LyGjXENeXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i4/T1OgGHXrBbXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i3/T1OQh4XDBbXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i3/T17HOzXCdcXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1_Z8LXq4hXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1kqGEXDhXXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i1/T1XyeFXuteXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i1/T1fKSIXrBeXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1eyqLXvReXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i1/T1TiA6XdxjXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i2/T1eSufXqVeXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i1/T1775HXsxbXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i3/T1quSHXuJbXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i1/T1wQCJXEtfXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i2/T1A4KHXBtaXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i3/T17KmMXvJbXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i2/T1TYyGXthdXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i2/T1VeqEXENeXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i4/T1u5VSXpxfXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i2/T1yemGXttcXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i2/T1Yk5HXzRXXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i2/T1rNuEXB4eXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i2/T18FagXudbXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i2/T1haOFXxdeXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i1/T1fE5pXwhgXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1HZWFXzdeXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i4/T1ax9zXy4hXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1SKx_XCVhXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i3/T1C55FXt8cXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i1/T1bTOjXzRgXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1fwhqXx4jXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i3/T1j05GXzFbXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i2/T1ZR1MXz0aXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i4/T19kuyXDpcXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i4/T1esymXqtbXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1kpGMXvJaXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i3/T1KGKqXx0dXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i3/T1ZgyFXCVcXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i4/T1T_KIXCXaXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i3/T1Vw5rXAJeXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i2/T1T4FVXypdXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i3/T1PQ9kXpxkXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i3/T1IcqBXwxhXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i3/T1DWiHXCdeXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T192CvXDJbXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i4/T1H8CKXDpcXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i4/T1qHiaXu4aXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i2/T1nBKcXy4fXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i2/T1Q0eFXzteXXb1upjX.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i3/T1rLWLXxFeXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i4/T1NY1dXq0hXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i1/T1LL1HXDdaXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i4/T1RQaHXEFXXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i3/T1O7CHXthaXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i3/T1ETqnXttfXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i1/T1qGuEXsXgXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i3/T1f9SeXyJbXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/114141735/T2k6f3XjJXXXXXXXXX_!!114141735.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/imgextra/i1/114141735/T2j9v3XiVXXXXXXXXX_!!114141735.jpg_90x90.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/357287592/T2uX6NXg4aXXXXXXXX_!!357287592.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/357287592/T25TDWXXtXXXXXXXXX_!!357287592.jpg_90x90.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/352797853/T275_1XgNaXXXXXXXX_!!352797853.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/352797853/T2fVTWXolaXXXXXXXX_!!352797853.jpg_90x90.jpg",
		          "http://img01.taobaocdn.com/imgextra/i1/872411436/T2yE64XfJaXXXXXXXX_!!872411436.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/872411436/T2Q.TtXXlbXXXXXXXX_!!872411436.jpg_90x90.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/103399436/T2J52PXnxaXXXXXXXX_!!103399436.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/103399436/T2yBY1XctXXXXXXXXX_!!103399436.jpg_90x90.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/306505731/T29YroXbpbXXXXXXXX_!!306505731.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/306505731/T2RKntXlJaXXXXXXXX_!!306505731.jpg_90x90.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/113462038/T2NTfyXc0bXXXXXXXX_!!113462038.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/imgextra/i4/113462038/T2UDPNXk0aXXXXXXXX_!!113462038.jpg_90x90.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/458599810/T2Xn23XfRXXXXXXXXX_!!458599810.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/458599810/T2x.T3Xe8XXXXXXXXX_!!458599810.jpg_90x90.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/885461966/T2xrP3XkJaXXXXXXXX_!!885461966.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/885461966/T2U9itXhdcXXXXXXXX_!!885461966.jpg_90x90.jpg",
		          "http://img03.taobaocdn.com/imgextra/i3/653206261/T2Kt_fXeFbXXXXXXXX_!!653206261.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/imgextra/i1/653206261/T2RmbGXfdXXXXXXXXX_!!653206261.jpg_90x90.jpg",
		          "http://img02.taobaocdn.com/imgextra/i2/397247991/T2hpP4Xj0XXXXXXXXX_!!397247991.jpg_300x300.jpg",
		          "http://img04.taobaocdn.com/imgextra/i4/397247991/T2GztAXaxOXXXXXXXX_!!397247991.jpg_90x90.jpg",
		          "http://img04.taobaocdn.com/bao/uploaded/i4/T17AStXDRhXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T19rmnXDFhXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1BgSLXEdaXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i1/T1nvqAXq0gXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i1/T1rGqsXExhXXb1upjX.jpg_300x300.jpg",
		          "http://img02.taobaocdn.com/bao/uploaded/i3/T1OGiwXy8cXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i4/T1BiOFXshhXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i2/T1nZaAXypdXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i3/T17juoXDpeXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/tps/i3/T1D6WMXzRXXXbByyEK-755-260.jpg",
		          "http://img02.taobaocdn.com/tps/i2/T1HPyMXDpXXXbByyEK-755-260.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i1/T1HAyXXEFeXXb1upjX.jpg_300x300.jpg",
		          "http://img03.taobaocdn.com/bao/uploaded/i2/T1mUaxXtxhXXb1upjX.jpg_300x300.jpg",
		          "http://img01.taobaocdn.com/bao/uploaded/i4/T1GD1fXrJeXXb1upjX.jpg_300x300.jpg"
		};
		urls = new ArrayList<String>();
		for(String url : urlArray) {
			urls.add(url);
		}
		if(ImageLoader.getInstance(this).initCacheSizeByByte(1024 * 1024 * 20)) {
//		if(ImageLoader.getInstance(this).initCacheSizeByPictureCount(10)) {
			Log.v("init", "true");
		}
		else {
			Log.v("init", "false");
		}
		ImageLoader.getInstance(this).initThreadPool(1, 3, 7, ImageLoader.QUEUE_TYPE_LIFO_FINITE, 10);
		adapter = new MyAdapter(urls, this);
		ListView lView = ((ListView)findViewById(R.id.listview));
		lView.setAdapter(adapter);
		lView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				startActivity(new Intent(TestActivity.this, ATestActivity.class));
			}
		});
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}
}
