package com.tomato.testsamples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements CreateNdefMessageCallback {
	private NfcAdapter nfcAdapter;
	private LocationListener locationListener;
	private LocationManager locationManager;
	private Location currentLocation;
	
	private TextView nfcMessage;
	private TextView latituteField;
	private TextView longitudeField;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		latituteField = (TextView) findViewById(R.id.gpsLatitudeText);
		longitudeField = (TextView) findViewById(R.id.gpsLongitudeText);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		/*
		 * Criteria criteria = new Criteria(); provider =
		 * locationManager.getBestProvider(criteria, false); Location location =
		 * locationManager.getLastKnownLocation(provider);
		 * 
		 * if (location != null) { System.out.println("Provider " + provider +
		 * " has been selected."); onLocationChanged(location); } else {
		 * latituteField.setText("Location not available");
		 * longitudeField.setText("Location not available"); }
		 */
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				Log.d("gps", "GPS updated " + location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
				Toast.makeText(MainActivity.this, "onStatusChanged: " + provider + ", " + status, Toast.LENGTH_LONG).show();
			}

			public void onProviderEnabled(String provider) {
				Toast.makeText(MainActivity.this, "onProviderEnabled: " + provider, Toast.LENGTH_LONG).show();
			}

			public void onProviderDisabled(String provider) {
				Toast.makeText(MainActivity.this, "onProviderDisabled: " + provider, Toast.LENGTH_LONG).show();
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
		Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vi.vibrate(5000);
	}
	
	public void gpsClicked(View v) {
		Log.d("gps", "requesting update");
		//locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, Looper.getMainLooper());
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
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			processIntent(getIntent());
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	void processIntent(Intent intent) {
		nfcMessage = (TextView) findViewById(R.id.nfcMessage);
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		// record 0 contains the MIME type, record 1 is the AAR, if present
		nfcMessage.setText(new String(msg.getRecords()[0].getPayload()));
	}

}
