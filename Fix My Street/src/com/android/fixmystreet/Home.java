// **************************************************************************
// Home.java
// **************************************************************************
package com.android.fixmystreet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Home extends Activity {
	// ****************************************************
	// Local variables
	// ****************************************************
	private static final String LOG_TAG = "Home";
	public static final String PREFS_NAME = "FMS_Settings";
	private Button btnReport;
	private Button btnDetails;
	private Button btnPicture;
	// Info that's been passed from other activities
	private Boolean haveDetails = false;
	private Boolean havePicture = false;
	private String name = null;
	private String email = null;
	private String subject = null;
	// Location info
	LocationManager locationmanager = null;
	LocationListener listener;
	Location location;
	private Double latitude;
	private Double longitude;
	private String latString = "";
	private String longString = "";
	long firstGPSFixTime = 0;
	long latestGPSFixTime = 0;
	long previousGPSFixTime = 0;
	private Boolean locationDetermined = false;
	int locAccuracy;
	long locationTimeStored = 0;
	// hacky way of checking the results
	private static int globalStatus = 13;
	private static final int SUCCESS = 0;
	private static final int LOCATION_NOT_FOUND = 1;
	private static final int UPLOAD_ERROR = 2;
	private static final int UPLOAD_ERROR_SERVER = 3;
	private static final int PHOTO_NOT_FOUND = 5;
	private static final int UPON_UPDATE = 6;
	private static final int COUNTRY_ERROR = 7;
	private String serverResponse;
	SharedPreferences settings;
	String versionName = null;
	// Thread handling
	ProgressDialog myProgressDialog = null;
	private ProgressDialog pd;
	final Handler mHandler = new Handler();
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			pd.dismiss();
			updateResultsInUi();
		}
	};
	private Bundle extras;
	private TextView textProgress;
	private String exception_string = "";

	// Called when the activity is first created
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.home);
		// Log.d(LOG_TAG, "onCreate, havePicture = " + havePicture);
		settings = getSharedPreferences(PREFS_NAME, 0);
		testProviders();

		btnDetails = (Button) findViewById(R.id.details_button);
		btnPicture = (Button) findViewById(R.id.camera_button);
		btnReport = (Button) findViewById(R.id.report_button);
		btnReport.setVisibility(View.GONE);
		textProgress = (TextView) findViewById(R.id.progress_text);
		textProgress.setVisibility(View.GONE);

		if (icicle != null) {
			havePicture = icicle.getBoolean("photo");
			Log.d(LOG_TAG, "icicle not null, havePicture = " + havePicture);
		} else {
			Log.d(LOG_TAG, "icicle null");
		}
		extras = getIntent().getExtras();
		checkBundle();
		setListeners();

		// Show update message - but not to new users
		int vc = 0;
		try {
			vc = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			versionName = getPackageManager().getPackageInfo(getPackageName(),
					0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO - add this code next time!
		boolean hasSeenUpdateVersion = settings.getBoolean(
				"hasSeenUpdateVersion" + vc, false);
		boolean hasSeenOldVersion = settings.getBoolean("hasSeenUpdateVersion"
				+ (vc - 1), false);
		if (!hasSeenUpdateVersion && hasSeenOldVersion) {
			showDialog(UPON_UPDATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("hasSeenUpdateVersion" + vc, true);
			editor.commit();
		}

		// TODO: Telephonymanager will probably not work on i.e tablets w/o gsm, check this
		// Check country: show warning if not in Great Britain
		TelephonyManager mTelephonyMgr = (TelephonyManager) this
		.getSystemService(Context.TELEPHONY_SERVICE);
		String country = mTelephonyMgr.getNetworkCountryIso();
		//Log.d(LOG_TAG, "country = " + country);
		if (!(country.matches("no"))) {
			showDialog(COUNTRY_ERROR);
		}
	}

	@Override
	protected void onPause() {
		// Log.d(LOG_TAG, "onPause, havePicture = " + havePicture);
		super.onPause();
		removeListeners();
	}

	@Override
	protected void onStop() {
		// Log.d(LOG_TAG, "onStop, havePicture = " + havePicture);
		super.onStop();
		removeListeners();
	}

	@Override
	public void onRestart() {
		// Log.d(LOG_TAG, "onRestart, havePicture = " + havePicture);
		testProviders();
		checkBundle();
		super.onRestart();
	}

	// ****************************************************
	// checkBundle - check the extras that have been passed
	// is the user able to upload things yet, or not?
	// ****************************************************
	private void checkBundle() {
		// Log.d(LOG_TAG, "checkBundle");
		// Get the status icons...
		Resources res = getResources();
		Drawable checked = res.getDrawable(R.drawable.done);
		if (extras != null) {
			// Log.d(LOG_TAG, "Checking extras");
			// Details extras
			name = extras.getString("name");
			email = extras.getString("email");
			subject = extras.getString("subject");
			if (!havePicture) {
				havePicture = extras.getBoolean("photo");
			}
			// Do we have the details?
			if ((name != null) && (email != null) && (subject != null)) {
				haveDetails = true;
				// Log.d(LOG_TAG, "Have all details");
				checked.setBounds(0, 0, checked.getIntrinsicWidth(), checked
						.getIntrinsicHeight());
				// envelope.setBounds(0, 0, envelope.getIntrinsicWidth(),
				// envelope
				// .getIntrinsicHeight());
				btnDetails.setText("Detaljer lagt til: '" + subject + "'");
				btnDetails.setCompoundDrawables(null, null, checked, null);
			} else {
				// Log.d(LOG_TAG, "Don't have details");
			}
		} else {
			extras = new Bundle();
			// Log.d(LOG_TAG, "no Bundle at all");
		}
		// Log.d(LOG_TAG, "havePicture = " + havePicture);

		// Do we have the photo?
		if (havePicture) {

			checked.setBounds(0, 0, checked.getIntrinsicWidth(), checked
					.getIntrinsicHeight());
			// camera.setBounds(0, 0, camera.getIntrinsicWidth(), camera
			// .getIntrinsicHeight());
			btnPicture.setCompoundDrawables(null, null, checked, null);
			btnPicture.setText("Bilde tatt");
		}
		if (havePicture && haveDetails) {
			textProgress.setVisibility(View.VISIBLE);
		}
	}

	// ****************************************************
	// setListeners - set the button listeners
	// ****************************************************

	private void setListeners() {
		btnDetails.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Home.this, Details.class);
				extras.putString("name", name);
				extras.putString("email", email);
				extras.putString("subject", subject);
				extras.putBoolean("photo", havePicture);

				i.putExtras(extras);
				startActivity(i);
				finish();
			}
		});
		btnPicture.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				File photo = new File(
						Environment.getExternalStorageDirectory(),
				"FMS_photo.jpg");
				if (photo.exists()) {
					photo.delete();
				}
				Intent imageCaptureIntent = new Intent(
						MediaStore.ACTION_IMAGE_CAPTURE);
				imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri
						.fromFile(photo));
				startActivityForResult(imageCaptureIntent, 1);
			}
		});
		btnReport.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				locationDetermined = true;
				uploadToFMS();
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Log.d(LOG_TAG, "onActivityResult");
		// Log.d(LOG_TAG, "Activity.RESULT_OK code = " + Activity.RESULT_OK);
		// Log.d(LOG_TAG, "resultCode = " + resultCode + "requestCode = "
		// + requestCode);
		if (resultCode == Activity.RESULT_OK && requestCode == 1) {
			havePicture = true;
			extras.putBoolean("photo", true);
			Resources res = getResources();
			Drawable checked = res.getDrawable(R.drawable.done);
			checked.setBounds(0, 0, checked.getIntrinsicWidth(), checked
					.getIntrinsicHeight());
			btnPicture.setCompoundDrawables(null, null, checked, null);
			btnPicture.setText("Bilde tatt");
		}
		Log.d(LOG_TAG, "havePicture = " + havePicture.toString());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(LOG_TAG, "onSaveInstanceState, havePicture " + havePicture);
		// Log.d(LOG_TAG, "onSaveInstanceState");
		if (havePicture != null) {
			// Log.d(LOG_TAG, "mRowId = " + mRowId);
			outState.putBoolean("photo", havePicture);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.
		havePicture = savedInstanceState.getBoolean("photo");
		Log.d(LOG_TAG, "onRestoreInstanceState, havePicture " + havePicture);
	}

	// **********************************************************************
	// uploadToFMS: uploads details, handled via a background thread
	// Also checks the age and accuracy of the GPS data first
	// **********************************************************************
	private void uploadToFMS() {
		// Log.d(LOG_TAG, "uploadToFMS");
		pd = ProgressDialog
		.show(
				this,
				"Laster opp, vennligst vent...",
				"Laster opp. Dette kan ta opptil ett minutt, avhengig av tilkobling din. Vær tålmodig!",
				true, false);
		Thread t = new Thread() {
			public void run() {
				doUploadinBackground();
				mHandler.post(mUpdateResults);
			}
		};
		t.start();
	}

	private void updateResultsInUi() {
		if (globalStatus == UPLOAD_ERROR) {
			showDialog(UPLOAD_ERROR);
		} else if (globalStatus == UPLOAD_ERROR_SERVER) {
			showDialog(UPLOAD_ERROR_SERVER);
		} else if (globalStatus == LOCATION_NOT_FOUND) {
			showDialog(LOCATION_NOT_FOUND);
		} else if (globalStatus == PHOTO_NOT_FOUND) {
			showDialog(PHOTO_NOT_FOUND);
		} else {
			// Success! - Proceed to the success activity!
			Intent i = new Intent(Home.this, Success.class);
			i.putExtra("latString", latString);
			i.putExtra("lonString", longString);
			startActivity(i);
			finish(); // this Home-activity, we don't want to come back here
		}
	}

	// **********************************************************************
	// onCreateDialog: Dialog warnings
	// **********************************************************************
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case COUNTRY_ERROR:
			return new AlertDialog.Builder(Home.this)
			.setTitle("Sted eller nettverksfeil")
			.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
				}
			})
			.setMessage(
			"Beklager, FiksGataMi fungerer bare i Norge. Du kan ikke rapportere fra der du befinner deg. (Du kan også se denne feilmeldingen om du ikke er tilkoblet internett)")
			.create();
		case UPLOAD_ERROR:
			return new AlertDialog.Builder(Home.this)
			.setTitle("Feil ved opplasting")
			.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
				}
			})
			.setMessage(
					"Beklager, det skjedde en feil under opplasting - kanskje du er uten nettverkstilgang? Vennligst prøv igjen senere. Exception: " + exception_string + " " + serverResponse)
					.create();
		case UPLOAD_ERROR_SERVER:
			return new AlertDialog.Builder(Home.this)
			.setTitle("Feil ved opplasting")
			.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
				}
			})
			.setMessage(
					"Beklager, det skjedde en feil under opplasting. Vennligst prøv igjen senere. Melding fra server: "
					+ serverResponse).create();
		case LOCATION_NOT_FOUND:
			return new AlertDialog.Builder(Home.this)
			.setTitle("Feil med sted")
			.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
				}
			})
			.setMessage(
			"Kunne ikke lokalisere sted! Kan du se himmelen? Vennligst prøv igjen senere.")
			.create();
		case PHOTO_NOT_FOUND:
			return new AlertDialog.Builder(Home.this).setTitle("Ingen bilde")
			.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
				}
			}).setMessage("Bilde ikke funnet!").create();
		case UPON_UPDATE:
			if (versionName == null) {
				versionName = "";
			}
			return new AlertDialog.Builder(Home.this).setTitle("Hva er nytt?")
			.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
				}
			}).setMessage(
					"Nytt i versjon" + versionName
					+ ": bedre GPS posisjonering.").create();
		}
		return null;
	}

	// **********************************************************************
	// doUploadinBackground: POST request to FixMyStreet
	// **********************************************************************
	private boolean doUploadinBackground() {
		// Log.d(LOG_TAG, "doUploadinBackground");
		String responseString = null;

		HttpParams params = new  BasicHttpParams();
		int timeoutConnection = 100000;
		HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);

		HttpClient httpClient = new DefaultHttpClient(params);
		try {
			HttpPost httpPost = new HttpPost("http://fiksgatami-dev.nuug.no/import");

			File f = new File(Environment.getExternalStorageDirectory(),
			"FMS_photo.jpg");
			
			MultipartEntity reqEntity = new MultipartEntity();
			FileBody fb = new FileBody(f);
			Charset utf8 =  Charset.forName("UTF-8");
			reqEntity.addPart("photo", new FileBody(f, "image/jpeg"));
			reqEntity.addPart("service", new StringBody("FiksGataMi4Android",utf8));
			reqEntity.addPart("subject", new StringBody(subject,utf8));
			reqEntity.addPart("name", new StringBody(name,utf8));
			reqEntity.addPart("email", new StringBody(email,utf8));             
			reqEntity.addPart("lat", new StringBody(latString,utf8));             
			reqEntity.addPart("lon", new StringBody(longString,utf8));   

			httpPost.setEntity(reqEntity);

//			Log.i(LOG_TAG,"executing request " + httpPost.getRequestLine());
			HttpResponse response = httpClient.execute(httpPost);

			HttpEntity resEntity = response.getEntity();
			responseString = EntityUtils.toString(resEntity);
			Log.i(LOG_TAG, "Response was " + responseString);

			if (resEntity != null) {
				System.out.println("Response content length: " + resEntity.getContentLength());
			}
			//EntityUtils.consume(resEntity);
		} catch (Exception ex) {
			Log.v(LOG_TAG, "Exception", ex);
			exception_string = ex.getMessage();
			globalStatus = UPLOAD_ERROR;
			serverResponse = "";
			return false;
		} finally {
			try { httpClient.getConnectionManager().shutdown(); } catch (Exception ignore) {}
		}
		// use startswith to workaround bug where CATEGORIES-info
		// is display on every call to import.cgi
		if (responseString.startsWith("SUCCESS")) {
			// launch the Success page
			globalStatus = SUCCESS;
			return true;
		} else {
			// print the response string?
			serverResponse = responseString;
			globalStatus = UPLOAD_ERROR;
			return false;
		}

	}

	private boolean checkLoc(Location location) {
		// get accuracy
		// Log.d(LOG_TAG, "checkLocation");
		float tempAccuracy = location.getAccuracy();
		locAccuracy = (int) tempAccuracy;
		// get time - store the GPS time the first time
		// it is reported, then check it against future reported times
		latestGPSFixTime = location.getTime();
		if (firstGPSFixTime == 0) {
			firstGPSFixTime = latestGPSFixTime;
		}
		if (previousGPSFixTime == 0) {
			previousGPSFixTime = latestGPSFixTime;
		}
		long timeDiffSecs = (latestGPSFixTime - previousGPSFixTime) / 1000;

		// Log.d(LOG_TAG, "~~~~~~~ checkLocation, accuracy = " + locAccuracy
		// + ", firstGPSFixTime = " + firstGPSFixTime + ", gpsTime = "
		// + latestGPSFixTime + ", timeDiffSecs = " + timeDiffSecsInt);

		// Check our location - no good if the GPS accuracy is more than 24m
		if ((locAccuracy > 24) || (timeDiffSecs == 0)) {
			if (timeDiffSecs == 0) {
				// nor do we want to report if the GPS time hasn't changed at
				// all - it is probably out of date
				textProgress
				.setText("Venter på GPS: telefonen sier siste tilkobling er for gammel. Pass på at du kan se himmelen.");
			} else {
				textProgress
				.setText("Venter på GPS: telefonen sier at siste målte nøyaktighet var "
						+ locAccuracy
						+ "m. (Vi trenger en nøyaktighet på 24m.) Pass på at du kan se himmelen.");
			}
			//		} else if (locAccuracy == 0) {
			//			// or if no accuracy data is available
			//			textProgress
			//			.setText("Venter på GPS... Pass på at du kan se himmelen.");
		} else {
			// but if all the requirements have been met, proceed
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			latString = latitude.toString();
			longString = longitude.toString();
			if (haveDetails && havePicture) {
				btnReport.setVisibility(View.VISIBLE);
				btnReport.setText("GPS signal funnet! Rapporter til FiksGataMi");
				textProgress.setVisibility(View.GONE);
			} else {
				textProgress.setText("GPS signal funnet!");
			}
			previousGPSFixTime = latestGPSFixTime;
			return true;
		}
		previousGPSFixTime = latestGPSFixTime;
		// textProgress.setText("~~~~~~~ checkLocation, accuracy = "
		// + locAccuracy + ", locationTimeStored = " + locationTimeStored
		// + ", gpsTime = " + gpsTime);
		return false;
	}

	public boolean testProviders() {
		// Log.e(LOG_TAG, "testProviders");
		// Register for location listener
		String location_context = Context.LOCATION_SERVICE;
		locationmanager = (LocationManager) getSystemService(location_context);
		// Criteria criteria = new Criteria();
		// criteria.setAccuracy(Criteria.ACCURACY_FINE);
		// criteria.setAltitudeRequired(false);
		// criteria.setBearingRequired(false);
		// criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
		// criteria.setSpeedRequired(false);
		// String provider = locationmanager.getBestProvider(criteria, true);
		if (!locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			buildAlertMessageNoGps();
			return false;
		}
		listener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// keep checking the location + updating text - until we have
				// what we need
				if (!locationDetermined) {
					checkLoc(location);
				}
			}

			public void onProviderDisabled(String provider) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}
		};
		locationmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, listener);
		return true;
	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder
		.setMessage(
		"GPS er avslått. Vil du slå den på nå?")
		.setCancelable(false).setPositiveButton("Ja",
				new DialogInterface.OnClickListener() {
			public void onClick(
					@SuppressWarnings("unused") final DialogInterface dialog,
					@SuppressWarnings("unused") final int id) {
				Intent j = new Intent();
				j
				.setAction("android.settings.LOCATION_SOURCE_SETTINGS");
				startActivity(j);
			}
		}).setNegativeButton("Nei",
				new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog,
					@SuppressWarnings("unused") final int id) {
				dialog.cancel();
			}
		});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	public void removeListeners() {
		// Log.e(LOG_TAG, "removeListeners");
		if ((locationmanager != null) && (listener != null)) {
			locationmanager.removeUpdates(listener);
		}
		locationmanager = null;
		// Log.d(LOG_TAG, "Removed " + listener.toString());
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (  Integer.valueOf(android.os.Build.VERSION.SDK) < 7 //Instead use android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
	            && keyCode == KeyEvent.KEYCODE_BACK
	            && event.getRepeatCount() == 0) {
	        // Take care of calling this method on earlier versions of
	        // the platform where it doesn't exist.
	        onBackPressed();
	    }

	    return super.onKeyDown(keyCode, event);
	}

	//@Override
	public void onBackPressed() {
		// TODO: This dosen't work - we are still sendt back to the last activity
	    // This will be called either automatically for you on 2.0
	    // or later, or by the code above on earlier versions of the
	    // platform.
		finish(); // Close application on back-press
	    return;
	}
	
	// ****************************************************
	// Options menu functions
	// ****************************************************

	// TODO - add Bundles for these?
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem helpItem = menu.add(0, 0, 0, "Hjelp");
		MenuItem aboutItem = menu.add(0, 1, 0, "Om");
		aboutItem.setIcon(android.R.drawable.ic_menu_info_details);
		helpItem.setIcon(android.R.drawable.ic_menu_help);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent i = new Intent(Home.this, Help.class);
			if (extras != null) {
				i.putExtras(extras);
			}
			startActivity(i);
			return true;
		case 1:
			Intent j = new Intent(Home.this, About.class);
			if (extras != null) {
				j.putExtras(extras);
			}
			startActivity(j);
			return true;
		}
		return false;
	}

	// read the photo file into a byte array...
	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		// You cannot create an array using a long type.
		// It needs to be an int type.
		// Before converting to an int type, check
		// to ensure that file is not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}
}
