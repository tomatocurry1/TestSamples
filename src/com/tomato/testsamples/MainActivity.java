package com.tomato.testsamples;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class MainActivity extends Activity implements CreateNdefMessageCallback {
	static final String PROPERTY_REG_ID = "registration_id";
	static final String PROPERTY_APP_VERSION = "app_version";
	static final int APP_VERSION = 0;
	static final String SENDER_ID = "472922563262";

	private NfcAdapter nfcAdapter;
	private LocationListener locationListener;
	private LocationManager locationManager;
	private Intent vibrateIntent;

	GoogleCloudMessaging gcm;
	String gcmRegistrationId;

	private TextView nfcMessage;
	private TextView latituteField;
	private TextView longitudeField;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!checkPlayServices()) {
			Toast.makeText(MainActivity.this, "Please get a valid Play Services APK / update! Sorry :(", Toast.LENGTH_LONG).show();
			finish();
		}

		gcm = GoogleCloudMessaging.getInstance(this);
		gcmRegistrationId = getRegId(this);
		if (gcmRegistrationId.isEmpty()) {
			registerInBackground();
		} else {
			Log.d("gcm", "regid is " + gcmRegistrationId);
		}

		latituteField = (TextView) findViewById(R.id.gpsLatitudeText);
		longitudeField = (TextView) findViewById(R.id.gpsLongitudeText);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				Log.d("gps", "GPS updated " + location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter == null) {
			Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		nfcAdapter.setNdefPushMessageCallback(this, this);
		
	       SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar1); 
	       //final TextView seekBarValue = (TextView)findViewById(R.id.seekbarvalue); 
	       seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
	 		  
	 		  @Override
	 		  public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
	 			
	 			  //Toast.makeText(getApplicationContext(), "Changing seekbar's progress", Toast.LENGTH_SHORT).show();
	 		  }
	 		
	 		  @Override
	 		  public void onStartTrackingTouch(SeekBar seekBar) {
	 			 // Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
	 		  }
	 		
	 		  @Override
	 		  public void onStopTrackingTouch(SeekBar seekBar) {
	 			 ((LocalBinder) VibrateService.getStatic().onBind(null)).setDistance(seekBar.getProgress());
	 			  //textView.setText("Covered: " + progress + "/" + seekBar.getMax());
	 			  //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
	 		  }
	 	   });
	}

	private boolean checkPlayServices() {
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (result != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(result)) {
				GooglePlayServicesUtil.getErrorDialog(result, this, 9000).show(); // 9000
																					// =
																					// PLAY_SERVICES_RESOLUTION_REQUEST
			} else {
				Toast.makeText(MainActivity.this, "Can't find google play services or recover, aborting! " + result, Toast.LENGTH_LONG).show();
				finish();
			}
			return false;
		}
		return true;
	}

	private String getRegId(Context context) {
		SharedPreferences prefs = getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		String regId = prefs.getString(PROPERTY_REG_ID, "");
		if (regId.isEmpty()) {
			Log.i("gcm", "registration key is empty D:");
			return "";
		}
		int regVersion = prefs.getInt(PROPERTY_APP_VERSION, -1);
		if (regVersion != APP_VERSION) {
			Log.i("gcm", "new app version :~)");
			return "";
		}
		return regId;
	}

	private void registerInBackground() {
		Log.d("gcm", "register in background");
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					gcmRegistrationId = gcm.register(SENDER_ID);
					Log.d("gcm", "regid is " + gcmRegistrationId);
					// TODO: send registration id to server
					Log.w("gcm", "Registration ID is " + gcmRegistrationId + " TODO http post");
					SharedPreferences prefs = getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString(PROPERTY_REG_ID, gcmRegistrationId);
					editor.putInt(PROPERTY_APP_VERSION, APP_VERSION);
					editor.commit();
				} catch (IOException e) {
					Log.e("gcmn", "IOException in registerInBackground", e);
					finish();
				}
				return (Void) null;
			}
		}.execute();
	}

	private Location getBestLastLocation() {
		Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location net = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		if (gps == null && net == null) {
			Log.w("gps", "Both gps and net are null! Returning null :(");
			return null;
		} else if (gps == null) {
			return net;
		} else if (net == null) {
			return gps;
		} else if (gps.getTime() > net.getTime()) {
			return gps;
		} else {
			return net;
		}
	}

	public void vibrateClicked(View v) {
		vibrateIntent = new Intent(this, VibrateService.class);
		startService(vibrateIntent);
	}

	public void gpsClicked(View v) {
		Log.d("gps", "requesting update");
		// locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
		// locationListener, Looper.getMainLooper());
		Location location = getBestLastLocation();
		latituteField.setText(location.getLatitude() + ", " + location.getAccuracy());
		longitudeField.setText(location.getLongitude() + ", " + location.getAccuracy());
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		String text = "Beam me up, Android!\n\n" + "Beam Time: " + System.currentTimeMillis();
		NdefMessage msg = new NdefMessage(new NdefRecord[] { NdefRecord.createMime("application/vnd.com.example.android.beam", text.getBytes()) });
		return msg;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!checkPlayServices()) {
			Toast.makeText(MainActivity.this, "Please get a valid Play Services APK / update! Sorry :(", Toast.LENGTH_LONG).show();
			finish();
		}
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			processIntent(getIntent());
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopService(vibrateIntent);
	}

	void processIntent(Intent intent) {
		nfcMessage = (TextView) findViewById(R.id.nfcMessage);
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		// record 0 contains the MIME type, record 1 is the AAR, if present
		nfcMessage.setText(new String(msg.getRecords()[0].getPayload()));
	}
}
