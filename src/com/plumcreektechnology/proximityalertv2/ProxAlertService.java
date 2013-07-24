package com.plumcreektechnology.proximityalertv2;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;

public class ProxAlertService extends Service implements ProxConstants{

	private final ProxAlertBinder binder = new ProxAlertBinder();
	private LocationManager locationManager;
	
	/**
	 * returns an IBinder so that an activity can bind to this service
	 */
	public class ProxAlertBinder extends Binder {
		public ProxAlertService getService() {
			return ProxAlertService.this;
		}
	}

	/**
	 * creates a location manager to run in the background and returns a
	 * ProxAlertBinder as an IBinder
	 */
	@Override
	public IBinder onBind(Intent intent) {
		//initialize locationManager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return binder;
	}
	
	/**
	 * make a proximity alert from a MyGeofence
	 * @param latitude
	 * @param longitude
	 */
	public void addProximityAlert(MyGeofence geofence) {
		locationManager.addProximityAlert(geofence.getLatitude(),
				geofence.getLongitude(), geofence.getRadius(),
				geofence.getExpiration(), makeIntent(geofence));
	}
	
	/**
	 * remove a proximity alert using a MyGeofence
	 * @param geofence
	 */
	public void removeProximityAlert(MyGeofence geofence) {
		locationManager.removeProximityAlert(makeIntent(geofence));
	}
	
	/**
	 * construct identical pending intents for adding and removing
	 * proximity alerts based on MyGeofence objects
	 * @param geofence
	 * @return
	 */
	private PendingIntent makeIntent(MyGeofence geofence) {
		Intent intent = new Intent(PROX_ALERT_INTENT);
		intent.putExtra("POI", geofence.getId());
		intent.putExtra("URI", geofence.getUri());
		intent.putExtra("ICON", geofence.getDrawable());
		// hashcode is necessary so that the pending intents don't overwrite
		// each other
		return PendingIntent.getBroadcast(this, geofence.getId().hashCode(),
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

}
