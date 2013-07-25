package com.plumcreektechnology.proximityalertv2;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.plumcreektechnology.proximityalertv2.AffirmativeFragment.AffirmativeHost;
import com.plumcreektechnology.proximityalertv2.MyDialogFragment.ChangeList;
import com.plumcreektechnology.proximityalertv2.ProxAlertService.ProxAlertBinder;
import com.plumcreektechnology.proximityalertv2.UserFragment.POISelect;

public class MainActivity extends FragmentActivity implements ProxConstants,
		POISelect, ChangeList, AffirmativeHost, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener,
		com.google.android.gms.location.LocationListener {

	//-------------------------------CONSTANTS--------------------------------//
	
	
	/* what has changed???
		- only the string name is passed in the pending intent, other info is attained when the
		dialog request comes back through the main method
		- when the map view is up and a dialog comes up, a marker is placed at the point interest
	*/
		
	
	// TODO figure out if we want the map to be the default screen, call the fragments appropriately
	// TODO change the frequency of map update based on mode of transportation (or speed of...)
	// TODO !!! add a marker to the map when the user is interested in a point
	// TODO get directions (use https to be secure)
	// TODO disable autocentering user when they are looking at a point (only autocenter when nothing else is happening)
	private TreeMap<String, MyGeofence> tree;
	
	// receiver is registered in manifest
	private ProxAlertService service;
	private int size;
	private boolean dialogAlert; // boolean keeps track of if we are displaying a dialogAlert on this resuming
	private boolean bound; // keeps track of bound status to client
	private boolean runningInterface; // keeps track of if the activity is currently displaying a user interface
	// private ProxReceiver receiver;
	private UserFragment userFragment;
	//private Empty empty;
	private SettingsFragment settingsFragment;
	private FragmentManager fragMan;
	private ArrayList<String> treeList;
	private Vibrator vibrator;
	private final long[] VIBRATE_PATTERN = {0, 100, 100, 100, 100, 100};
	private AudioManager audioman;
	private PowerManager powerman;

	private MapFragment mapFragment;
	private GoogleMap map;
	private boolean followUser;
	private LocationClient locClient;
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private Location currentLoc; // maybe use this later for requesting database stuff...
	private LocationRequest locRequest;
	private static final long UPDATE_INTERVAL_MS = 15000;
	private static final long FASTEST_INTERVAL_MS = 5000;
	protected static final String KEY_THIS_PREFERENCE = "location_preference";
	protected static final String KEY_ITEM_LONGITUDE = "longitude";
	protected static final String KEY_ITEM_LATITUDE = "latitude";
	protected static final String KEY_ITEM_ZOOM = "zoom";	
	protected static final String KEY_ITEM_TILT = "tilt";	
	protected static final String KEY_ITEM_BEARING = "bearing";	
	private SharedPreferences prefs;
	
	//------------------------------LIFECYCLE-------------------------------//
	
	/**
	 * service connection global variable for binding to ProxAlertService
	 */
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder) {
			ProxAlertBinder binder = (ProxAlertBinder) iBinder;
			service = binder.getService();
			bound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			bound = false;			
		}
		
	};

	//------------------------LIFECYCLE ORIENTED STUFF-------------------------//	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		prefs = getSharedPreferences(KEY_THIS_PREFERENCE, Context.MODE_PRIVATE);
		
		// Create new location client
		if (servicesConnected()) {
			locClient = new LocationClient(this, this, this);
		}
		currentLoc = new Location("whatever"); // blank location for now
		followUser = true;
		
		// do all of the right things to make a location request for repeated updates
		locRequest = LocationRequest.create();
		locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locRequest.setInterval(UPDATE_INTERVAL_MS);
		locRequest.setFastestInterval(FASTEST_INTERVAL_MS);
		
		// bind activity to ProxAlertService
		Intent intent = new Intent(this, ProxAlertService.class);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);
		treeGrow();
		size = tree.size();
		
		// initialize alert effects
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		audioman = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		powerman = (PowerManager) getSystemService(Context.POWER_SERVICE);
	}

	/**
	 * initialize the options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	protected void onStart() {
		super.onStart();
		onNewIntent(getIntent());

		// fragments!
		fragMan = getFragmentManager();
		// settings
		settingsFragment = new SettingsFragment();
		fragAdder(settingsFragment);
		fragRemover(settingsFragment);
		// map
		mapFragment = MapFragment // instantiate defaults for map
				.newInstance((new GoogleMapOptions())
						.mapType(GoogleMap.MAP_TYPE_NORMAL)
						.camera((new CameraPosition.Builder().target(new LatLng(
								Double.parseDouble(prefs
										.getString(
												getLocationPreferenceKey(KEY_ITEM_LATITUDE),
												"90.0")),
								Double.parseDouble(prefs
										.getString(
												getLocationPreferenceKey(KEY_ITEM_LONGITUDE),
												"0.0")))))
								.zoom(prefs
										.getFloat(
												getLocationPreferenceKey(KEY_ITEM_ZOOM),
												10))
								.tilt(prefs
										.getFloat(
												getLocationPreferenceKey(KEY_ITEM_TILT),
												0))
								.bearing(
										prefs.getFloat(
												getLocationPreferenceKey(KEY_ITEM_BEARING),
												0)).build()));
		fragAdder(mapFragment);
		fragRemover(mapFragment);
		// user
		userFragment = new UserFragment();
		userFragment.setListAdapter(treeAdapter());
		fragAdder(userFragment);
		// runningInterface = true; // changed this to true at this point because i think it is...
		// we don't remove the user fragment before the map fragment is added...
		fragRemover(userFragment); // let's do that for now
	}
	
	protected void onNewIntent(Intent intent) {
		dialogAlert = intent.getBooleanExtra("dialog", false);
		if(dialogAlert && !runningInterface) { // if we are creating a dialog and the user interface was not being displayed (hide the user interface)
			//remove preexisting fragments
			if(userFragment.isAdded()){
				fragRemover(userFragment);
			} else if(settingsFragment.isAdded()){
				fragRemover(settingsFragment);
			} else if(mapFragment.isAdded()){
				fragRemover(mapFragment);
			}

			getActionBar().hide();
		}
		if(dialogAlert) { // if we are creating a dialog regardless
			//show dialog
			MyDialogFragment dfrag = new MyDialogFragment();
			MyGeofence tempFence = tree.get(intent.getStringExtra("POI"));
			Bundle fragBundle = new Bundle();
			fragBundle.putString("POI", tempFence.getId());
			fragBundle.putString("URI", tempFence.getUri());
			fragBundle.putInt("ICON", tempFence.getDrawable());
			if(runningInterface) {
				fragBundle.putBoolean("hideAfter", false);
			} else fragBundle.putBoolean("hideAfter", true);
			dfrag.setArguments(fragBundle);
			dfrag.show(fragMan, null);
			
			// vibrate, make sound, and light up 
			vibrator.vibrate( VIBRATE_PATTERN, -1); // currently only vibrate works
			if(audioman.getRingerMode()==AudioManager.RINGER_MODE_NORMAL) {
				audioman.loadSoundEffects();
				audioman.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_UP);
				audioman.unloadSoundEffects();
			}
			//PowerManager.WakeLock wl = powerman.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP, "dialog light-up");
			//wl.acquire(1000);
		}
	}
	
	protected void onResume() {
		super.onResume();
		if(dialogAlert) { // if a dialog is happening, then onNewIntent handled it
			dialogAlert = false;
		} else { // if we are not creating a dialog, then something besides the dialog should be visible
			if(!userFragment.isVisible() && !settingsFragment.isVisible() && !mapFragment.isVisible()){ 
				fragReplacer(userFragment);
			}
			getActionBar().show();
			runningInterface = true; // if this resumption is not in order to display a dialog, rI is true
		}
	}
	
	protected void onStop() {
		super.onStop();
		runningInterface = false; // if the application is stopped for whatever reason, rI is false
		if(locClient.isConnected() ){
			locClient.disconnect(); //Disconnect client
		}
		
		checkMapFrag();
	}
	
	/**
	 * unbind from service at the end of activity
	 */
	protected void onDestroy() {
		super.onDestroy();
		if (bound) {
			unbindService(connection);
		}
	}

	//------------------------------OTHER STUFF-------------------------------//
	
	/**
	 * automatically populates the tree with premade MyGeofences (which will
	 * then populate a list in the user fragment)
	 */
	private void treeGrow() {
		tree = new TreeMap<String, MyGeofence>();
		tree.put("Public Library", new MyGeofence("Public Library", 41.289818, -82.216895, RADIUS, EXPIRATION, "http://www.oberlinpl.lib.oh.us/", ICON));
		tree.put("Hales Gymnasium", new MyGeofence("Hales", 41.294764, -82.223917, RADIUS, EXPIRATION, "http://new.oberlin.edu/student-life/facilities/detail.dot?id=352252&buildingId=30220", ICON));
		tree.put("Oberlin College Science Center", new MyGeofence("Science Center", 41.294692, -82.221782, RADIUS, EXPIRATION, "http://www.oberlin.edu/science/", ICON));
		tree.put("Laundromat", new MyGeofence("Laundromat", 41.294426, -82.212115, RADIUS, EXPIRATION, "http://en.wiktionary.org/wiki/laundromat", ICON));
		tree.put("Tank", new MyGeofence("Tank", 41.292088, -82.213263, RADIUS, EXPIRATION, "http://new.oberlin.edu/office/housing/housing-options/co-ops/tank.dot", ICON));
		tree.put("Arboretum", new MyGeofence("Arboretum", 41.285558, -82.226127, RADIUS, EXPIRATION, "http://new.oberlin.edu/student-life/facilities/detail.dot?id=2111210&buildingId=175090", ICON));
		tree.put("Allen Memorial Art Museum", new MyGeofence("Allen Memorial Art Museum", 41.293724, -82.216766, RADIUS, EXPIRATION, "http://www.oberlin.edu/amam/", ICON));
		tree.put("Finney Chapel", new MyGeofence("Finney Chapel", 41.293829, -82.220591, RADIUS, EXPIRATION, "https://new.oberlin.edu/student-life/facilities/detail.dot?id=318922&buildingId=50779", ICON));
		tree.put("Mudd Library", new MyGeofence("Mudd Library", 41.293208, -82.222833, RADIUS, EXPIRATION, "http://www.oberlin.edu/library/main/", ICON));
		tree.put("Hall Auditorium", new MyGeofence("Hall Auditorium", 41.292845, -82.216712, RADIUS, EXPIRATION, "http://new.oberlin.edu/student-life/facilities/detail.dot?id=16613&buildingId=30174", ICON));
		tree.put("Warner Concert Hall", new MyGeofence("Warner Concert Hall", 41.290899, -82.219620, RADIUS, EXPIRATION, "http://new.oberlin.edu/conservatory/facilities/detail.dot?id=30368&buildingId=30202", ICON));
		tree.put("Eastwood Primary School", new MyGeofence("Eastwood Primary School", 41.292349, -82.209964, RADIUS, EXPIRATION, "http://ocs.schoolwires.net/Domain/8", ICON));
		tree.put("Prospect Elementary School", new MyGeofence("Prospect Elementary School", 41.290866, -82.228932, RADIUS, EXPIRATION, "http://ocs.schoolwires.net/Domain/9", ICON));
		tree.put("Langston Middle School", new MyGeofence("Langston Middle School", 41.295852, -82.216600, RADIUS, EXPIRATION, "http://ocs.schoolwires.net/Domain/10", ICON));
		tree.put("Oberlin High School", new MyGeofence("Oberlin High School", 41.299281, -82.212431, RADIUS, EXPIRATION, "http://ocs.schoolwires.net/Domain/11", ICON));
		tree.put("Kendal at Oberlin", new MyGeofence("Kendal at Oberlin", 41.301860, -82.214406, RADIUS, EXPIRATION, "http://kao.kendal.org/", ICON));
		tree.put("Westwood Cemetery", new MyGeofence("Westwood Cemetery", 41.286948, -82.232795, RADIUS, EXPIRATION, "http://www.cityofoberlin.com/Recreation/PARKS/WestwoodCemetery.page", ICON));
		tree.put("Weltzheimer/Johnson House", new MyGeofence("Weltzheimer/Johnson House", 41.288052, -82.236711, RADIUS, EXPIRATION, "http://www.oberlin.edu/amam/flwright.html", ICON));
		tree.put("Fairchild Chapel", new MyGeofence("Fairchild Chapel", 41.294630, -82.218761, RADIUS, EXPIRATION, "http://new.oberlin.edu/student-life/facilities/detail.dot?id=30318&buildingId=30196", ICON));
	}

	/**
	 * private utility for creating an ArrayAdapter that contains all the ids of
	 * MyGeofences in the tree
	 * @return
	 */
	private ArrayAdapter<String> treeAdapter() {
		treeList = new ArrayList<String>();
		for(Map.Entry<String, MyGeofence> entry: tree.entrySet()) {
			treeList.add(entry.getKey());
		}
		return new ArrayAdapter<String>(this, R.layout.checked_text, treeList);
	}
	
	/**
	 * when a menu item is selected starts its corresponding fragment
	 */
	public boolean onOptionsItemSelected(MenuItem item){
		int itemId = item.getItemId();
		switch(itemId){
		case (R.id.action_settings):
			checkMapFrag();
			fragReplacer(settingsFragment);
			break;
		case (R.id.user_frag):
			checkMapFrag();
			fragReplacer(userFragment);
			break;
		case(R.id.map_frag):
			locClient.connect(); //connect client
			fragReplacer(mapFragment);
			map = mapFragment.getMap();
			if (map != null) {
				map.setMyLocationEnabled(true);
			} else {
				Toast.makeText(this, "map is null !!! ", Toast.LENGTH_SHORT).show();
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * private utility for adding fragments
	 * @param frag
	 */
	private void fragAdder(Fragment frag) {
		FragmentTransaction trans = fragMan.beginTransaction();
		trans.add(R.id.main_root, frag);
		trans.addToBackStack(null);
		trans.commit();
	}
	
	/**
	 * private utility for removing fragments
	 * @param frag
	 */
	private void fragRemover(Fragment frag) {
		FragmentTransaction trans = fragMan.beginTransaction();
		trans.remove(frag);
		trans.addToBackStack(null);
		trans.commit();
	}
	
	/**
	 * private utility for replacing fragments
	 * @param frag
	 */
	private void fragReplacer(Fragment frag) {
		FragmentTransaction trans = fragMan.beginTransaction();
		trans.replace(R.id.main_root, frag);
		trans.addToBackStack(null);
		trans.commit();
	}
	
	/**
	 * receive changes from user interface and ask service to add and remove
	 * proximity alerts correspondingly
	 */
	@Override
	public void onPointSelect(String name, boolean flag) {
		if (bound) {
			if (flag) {
				service.addProximityAlert(tree.get(name));
			} else {
				service.removeProximityAlert(tree.get(name));
			}
		} else {
			Log.d("CONNECTION ERROR", "service is not connected!");
		}
	}

	public int getSize() {
		return size;
	}

	@Override
	public void update(String name, boolean flag) { // a slightly eccentric work-around to updating GUI
		for (int i = 0; i < size; i++) {			// depending on how the user responds to the alert
			if ( name.equals(treeList.get(i)) ) {	// if the list is in view, change it in the GUI
				if(userFragment.isVisible()) {
					ListView lview = userFragment.getListView();
					lview.setItemChecked( i, flag);
				}
				else {	// if the list fragment is hidden, change shared preferences so the next time it loads it will be updated
					SharedPreferences.Editor ed = getSharedPreferences(UserFragment.KEY_THIS_PREFERENCE, Context.MODE_PRIVATE).edit();
					ed.putBoolean(UserFragment.getItemPreferenceKey(i), flag);
					ed.commit();
				}
				break;
			}
		}
		onPointSelect(name, flag);
	}

	/**
	 * checks whether the device is connected to Google Play Services and
	 * displays an error message if not
	 * @return
	 */
	private boolean servicesConnected() {
		// check for Google Play
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		
		if(ConnectionResult.SUCCESS == result) { //it's available
			Log.d(this.toString(), "Google Play services is available.");
			return true;
		} else { //not available
			Log.d(this.toString(), "Google Play services is not available.");
			return false;
		}
	}

	/**
	 * Resolves connection error if possible; otherwise displays error dialog
	 */
	@Override
	public void onConnectionFailed(ConnectionResult connectResult) {
		/*
		 * Google Play Services can resolve some connection errors
		 * If the error has a resolution, try to send an Intent to
		 * start a Google Play services activity that can fix error
		 */
		if(connectResult.hasResolution()) {
			try{
				connectResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
			} catch(IntentSender.SendIntentException e) {
				e.printStackTrace(); // log the error
			}
		} else {
			Log.e(this.toString(), "GooglePlayServices error code: " + connectResult.getErrorCode());
		}
	}

	/**
	 * when Location Services is connected
	 * Get most recent location
	 */
	@Override
	public void onConnected(Bundle bundle) {		
		locClient.requestLocationUpdates(locRequest, this); // locRequest was assembled in onCreate()
	}

	/**
	 * when Location Services is disconnected
	 */
	@Override
	public void onDisconnected() {	
	}

	/**
	 * every time the location changes make a toast
	 * and update the address in the textview
	 */
	@Override
	public void onLocationChanged(Location location) {
		currentLoc = location;
		if(mapFragment.isVisible() && followUser) {
			map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())) );
		}
	}
	
	private void checkMapFrag() {
		if(locClient.isConnected()) {
			locClient.disconnect();
		}
		if(mapFragment.isVisible() ) {
			// save the most recent camera location and params
			CameraPosition cam = map.getCameraPosition();
			SharedPreferences.Editor ed = prefs.edit();
			ed.putString(getLocationPreferenceKey(KEY_ITEM_LATITUDE), ((Double)cam.target.latitude).toString());
			ed.putString(getLocationPreferenceKey(KEY_ITEM_LONGITUDE), ((Double)cam.target.longitude).toString());
			ed.putFloat(getLocationPreferenceKey(KEY_ITEM_ZOOM), cam.zoom);
			ed.putFloat(getLocationPreferenceKey(KEY_ITEM_TILT), cam.tilt);
			ed.putFloat(getLocationPreferenceKey(KEY_ITEM_BEARING), cam.bearing);
			ed.commit();
		}
		
	}
	
	/**
	 * generate consistent item keys for saving the lists state in SharedPreferences
	 * @param pos
	 * @return
	 */
	protected static String getLocationPreferenceKey(String KEY) {
		return PACKAGE + "_" + KEY ;
	}

	
	@Override
	public void goMap(String name) {
		MyGeofence tempFence = tree.get(name);
		// this code should be taken out later and put into a separate dialog
		if(!mapFragment.isVisible()) {
			fragReplacer(mapFragment);
		}
		followUser = false;
		map.addMarker(new MarkerOptions().position(
				new LatLng(tempFence.getLatitude(), tempFence.getLongitude()))
				.title(tempFence.getId()));
		map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(tempFence
				.getLatitude(), tempFence.getLongitude())));
	}

}
