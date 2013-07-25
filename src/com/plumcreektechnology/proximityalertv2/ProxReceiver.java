package com.plumcreektechnology.proximityalertv2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

public class ProxReceiver extends BroadcastReceiver implements ProxConstants {

	/**
	 * when it receives a proximity transition update it creates
	 * a notification that links to a site-specific URL
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)) {
			// call our activity that creates the dialog
			//Intent intend = new Intent(context, InvisibleActivity.class); for when we were using a separate class
			Intent intend = new Intent(context, MainActivity.class);
			intend.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intend.putExtra("POI", intent.getStringExtra("POI"));
			intend.putExtra("dialog", true);
			context.startActivity(intend);

		}
		
	}

}
