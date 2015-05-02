package com.cpy.imageloader.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.cpy.imageloader.R;

public class SelectActivity extends Activity implements OnClickListener{
	
	Button loadgetview;
	Button loadnotfling;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_activity);
		loadgetview = (Button)findViewById(R.id.load_getview);
		loadnotfling = (Button)findViewById(R.id.load_not_fling);
		loadgetview.setOnClickListener(this);
		loadnotfling.setOnClickListener(this);
	}

	@Override
	public void onClick(View arg0) {
		if(arg0 == loadgetview) {
			Intent intent = new Intent(SelectActivity.this, TestActivity.class);
			intent.putExtra("isOnlyLoadNotFling", false);
			startActivity(intent);
		}
		else if(arg0 == loadnotfling) {
			Intent intent = new Intent(SelectActivity.this, TestActivity.class);
			intent.putExtra("isOnlyLoadNotFling", true);
			startActivity(intent);
		}
	}

	
}
