package com.plumcreektechnology.proximityalertv2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * this fragment either goes to a webpage, shows a point on a map, or disappears
 * @author devinfrenze
 *
 */
public class AffirmativeFragment extends DialogFragment implements ProxConstants {
	private static final int MAP = 5;
	private static final int WEB = 4;
	private static final int DISMISS = 3;
	
	private String name;
	private String webAddress;
	private int iconId;
	boolean hideAfter; // do we hide activity after dismissing dialog?
	private AffirmativeHost affirmativeHost;

	/** The system calls this only when creating the layout in a dialog. */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		// get data from arguments
		Bundle bundle = getArguments();
		name = bundle.getString("POI");
		webAddress = bundle.getString("URI");
		iconId = bundle.getInt("ICON", INVALID_INT_VALUE);
		hideAfter = bundle.getBoolean("hideAfter");
		
		
		// format and build and return the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder( ((Activity) affirmativeHost), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
		builder.setMessage("you're near " + name).setTitle("PROXIMITY UPDATE").setIcon(iconId);
		builder.setPositiveButton("web",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//buttonSelected(true);
						buttonSelected(WEB);
					}
				});
		builder.setNeutralButton("map",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// buttonSelected(false);
						buttonSelected(MAP);
					}
				});
		builder.setNegativeButton("dismiss",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// buttonSelected(false);
						buttonSelected(DISMISS);
					}
				});
		return builder.create();
	}
	
	public void buttonSelected(int action) { // TODO implement 3 button functionality
		switch (action) {
		case (WEB):
			Intent websurfing = new Intent(Intent.ACTION_VIEW, Uri.parse(webAddress));
			startActivity(websurfing); // maybe remove point from alerts if it is being visited...
			break;
		case (MAP):
			affirmativeHost.goMap(name);
			break;
		}
		if (hideAfter)
			((Activity) affirmativeHost).onBackPressed(); // so that screen returns to its previous state
		dismiss();
	}
	
	/**
	 * makes sure that an instantiating activity can not attach this fragment
	 * unless it implements POISelect and saves the activity in onSelect for
	 * future reference
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			affirmativeHost = (AffirmativeHost) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement changeList");
		}
	}
	
	/**
	 * interface this fragment requires for an activity to instantiate it
	 * must receive a string (location name) and a boolean (checked or unchecked)
	 * @author devinfrenze
	 *
	 */
	public interface AffirmativeHost {
		public void goMap(String name);
	}
}
