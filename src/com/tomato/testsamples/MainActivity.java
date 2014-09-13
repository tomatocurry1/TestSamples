package com.tomato.testsamples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements CreateNdefMessageCallback {
	private NfcAdapter nfcAdapter;
	private LocationListener locationListener;
	private LocationManager locationManager;
	
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
				latituteField.setText(String.valueOf(location.getLatitude()));
				longitudeField.setText(String.valueOf(location.getLongitude()));
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

		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter == null) {
			Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		nfcAdapter.setNdefPushMessageCallback(this, this);
	}

	public void vibrateClicked(View v) {
		Vibrator vi = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vi.vibrate(5000);
	}
	
	public void gpsClicked(View v) {
		Log.d("gps", "requesting update");
		locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, Looper.getMainLooper());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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
