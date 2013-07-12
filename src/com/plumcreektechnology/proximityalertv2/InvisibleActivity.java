package com.plumcreektechnology.proximityalertv2;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Toast;

public class InvisibleActivity extends FragmentActivity {
	
	public void onCreate(Bundle onSavedInstanceState) {
		super.onCreate(onSavedInstanceState);
		setContentView(R.layout.invisible_layout);
		
		// assemble dialog from the receiver
		Intent intent = getIntent();
		MyDialogFragment dfrag = new MyDialogFragment();
		Bundle fragBundle = new Bundle();
		fragBundle.putString("POI", intent.getStringExtra("POI"));
		fragBundle.putString("URI", intent.getStringExtra("URI"));
		dfrag.setArguments(fragBundle);
		FragmentManager fragman = getFragmentManager();
		dfrag.show(fragman, null);
	}
	
}