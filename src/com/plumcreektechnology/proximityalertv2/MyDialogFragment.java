package com.plumcreektechnology.proximityalertv2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class MyDialogFragment extends DialogFragment implements ProxConstants {

	private static final int REMOVE = 0;
	private static final int IGNORE = 1;
	private static final int VISIT = 2;
	
	private String POI;
	private String URI;
	private int icon;
	boolean hideAfter; // do we hide activity after dismissing dialog?
	private ChangeList listChange;

	/** The system calls this only when creating the layout in a dialog. */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		// get data from arguments
		Bundle bundle = getArguments();
		POI = bundle.getString("POI");
		URI = bundle.getString("URI");
		icon = bundle.getInt("ICON", INVALID_INT_VALUE);
		Log.d("icon", "icon in dialog is "+icon);
		hideAfter = bundle.getBoolean("hideAfter");
		
		// format and build and return the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder( ((Activity)listChange), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
		builder.setMessage("you're near " + POI).setTitle("PROXIMITY UPDATE").setIcon(icon);
		builder.setPositiveButton("visit",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//buttonSelected(true);
						buttonSelected(VISIT);
					}
				});
		builder.setNeutralButton("ignore",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// buttonSelected(false);
						buttonSelected(IGNORE);
					}
				});
		builder.setNegativeButton("remove",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// buttonSelected(false);
						buttonSelected(REMOVE);
					}
				});
		return builder.create();
	}

	/**
	 * callback method for when the user selects dialog buttons if true, it
	 * opens a web page for relevant comment in all cases it dismisses the
	 * dialog at the end
	 */
	public void buttonSelected(Boolean action) { // TODO remove
		if (action) {
			Intent websurfing = new Intent(Intent.ACTION_VIEW, Uri.parse(URI));
			startActivity(websurfing);
		}
		if (hideAfter)
			((Activity) listChange).onBackPressed(); // so that screen
															// returns to its
															// previous state
		dismiss();
	}
	
	public void buttonSelected(int action) { // TODO implement 3 button functionality
		switch (action) {
		case (VISIT):
			Intent websurfing = new Intent(Intent.ACTION_VIEW, Uri.parse(URI));
			startActivity(websurfing); // maybe remove point from alerts if it is being visited...
			break;
		case (REMOVE):
			listChange.update(POI, false);
			break;
		}
		if (hideAfter)
			((Activity) listChange).onBackPressed(); // so that screen returns to its previous state
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
			listChange = (ChangeList) activity;
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
	public interface ChangeList {
		public void update(String name, boolean flag);
	}

}
