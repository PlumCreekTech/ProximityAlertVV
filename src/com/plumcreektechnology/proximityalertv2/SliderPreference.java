package com.plumcreektechnology.proximityalertv2;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SliderPreference extends Preference implements OnSeekBarChangeListener {
	
	// attribute set gets values from the place it is invoked in XML!!!
	
	private final String TAG = getClass().getName();
	private static final String ANDROIDNS="http://schemas.android.com/apk/res/android";
	private static final String PCTNS="http://plumcreektechnology.com";
	private static final int DEFAULT_VALUE = 50;
	
	private String KEY = "slider";
	private Context context;
	private SeekBar seekBar;
	private TextView status;
	
	private int maximum = 100;
	private int minimum = 0;
	private int interval = 1;
	private int currentValue;
	
	/**
	 * one of two constructors for our class
	 * @param context
	 * @param attrs
	 */
	public SliderPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPreference(context, attrs); // custom class for initializing our preference
//		setLayoutResource(R.layout.seek_bar_layout);
//		setWidgetLayoutResource(R.layout.seek_bar_layout);
//		setDefaultValue(DEFAULT_VALUE);
//		setKey(KEY);
	}
	
	/**
	 * the second of two constructors
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public SliderPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initPreference(context, attrs);
	}
	
	/**
	 * custom class for initializing our preferences
	 * @param context
	 * @param attrs
	 */
	private void initPreference(Context context, AttributeSet attrs) {
		this.context=context;
		setValuesFromXML(attrs);
		// constructs a new seekbar
		seekBar = new SeekBar(context, attrs);
		// sets its max value
		seekBar.setMax(maximum - minimum);
		// assigns a listener
		seekBar.setOnSeekBarChangeListener(this);
	}
	
	private void setValuesFromXML(AttributeSet attrs) {
		maximum = attrs.getAttributeIntValue(ANDROIDNS, "max", 100);
		minimum = attrs.getAttributeIntValue(PCTNS, "min", 0);
		
		try {
			String newInterval = attrs.getAttributeValue(PCTNS, "insterval");
			if(newInterval != null) interval = Integer.parseInt(newInterval);
		} catch (Exception e) {
			Log.e(TAG, "Invalid interval value", e);
		}
		
	}
	
	@Override
	protected View onCreateView(ViewGroup parent) {
		// skips the super
		// creates a layout from our file
		RelativeLayout layout = null;
		try {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			layout = (RelativeLayout) inflater.inflate(R.layout.slider_preference, parent, false);
		} catch(Exception e) {
			Log.e(TAG, "Error creating seek bar preference", e);
		}
		return layout;
		
		// old stuff
//		super.onCreateView(parent);
//		*** the command above replaces layout resource with R.layout.stuff...
//		return inflater.inflate(getLayoutResource(), parent);
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		// move our seekBar into the view
		try {
			ViewParent oldContainer = seekBar.getParent();
			ViewGroup newContainer = (ViewGroup) view.findViewById(R.id.slider_container);
			if (oldContainer != newContainer) {
				//remove the seekbar from the old view
				if(oldContainer != null) {
					((ViewGroup) oldContainer).removeView(seekBar);
				}
				// remove any existing seekBar in the container and add ours
				newContainer.removeAllViews();
				newContainer.addView(seekBar, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error binding view: " + e.toString());
		}
		restore(true, (Integer) DEFAULT_VALUE);
		updateView(view);	
	}
	
	/**
	 * update the slider_preference view with our current state
	 * @param view
	 */
	protected void updateView(View view) {
		try {
			RelativeLayout layout = (RelativeLayout) view;
			status = (TextView) layout.findViewById(R.id.slider_value);
			status.setText(String.valueOf(currentValue));
			status.setMinimumWidth(30);
			seekBar.setProgress(currentValue-minimum);
		} catch (Exception e) {
			Log.e(TAG, "Error updating seek bar preference", e);
		}
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBarArg, int progress, boolean fromUser) {
		int newValue = progress + minimum;
		
		if(newValue > maximum) newValue = maximum;
		else if(newValue < minimum) newValue = minimum;
		else if(interval != 1 && newValue % interval != 0) newValue = Math.round(((float) newValue) / interval) * interval;
		
		// if the change is rejected revert to the previous value
		if(!callChangeListener(newValue)) {
			seekBarArg.setProgress(currentValue - minimum);
			return;
		}
		
		currentValue = newValue;
		status.setText(String.valueOf(newValue));
		persistInt(newValue);
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBarArg) {}
	
	@Override
	public void onStopTrackingTouch(SeekBar seekBarArg) {
		notifyChanged();
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {
		return ta.getInt(index, DEFAULT_VALUE);
	}
	
	protected void onSetInitialValue(Boolean restoreValue, Object defaultValue) {
		restore(restoreValue, defaultValue);
	}
	
	private void restore(Boolean restoreValue, Object defaultValue) {
		if(restoreValue) {
			currentValue = getPersistedInt((Integer)defaultValue);
		} else {
			int temp = 0;
			try {
				temp = (Integer)defaultValue;
			} catch(Exception e) {
				Log.e(TAG, "Invalid default value: " + defaultValue.toString());
			}
			persistInt(temp);
			currentValue = temp;
		}
	}
}