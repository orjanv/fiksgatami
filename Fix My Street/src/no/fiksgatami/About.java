package no.fiksgatami;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class About extends Activity {

	private Bundle extras = null;
	String versionName = "";
	boolean loadedLibLic = false;
	boolean loadedAppLic = false;
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.about);
		extras = getIntent().getExtras();

		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(),
					0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// add links
		TextView noteView = (TextView) findViewById(R.id.faq);
		TextView noteView2 = (TextView) findViewById(R.id.faq2);
		final TextView licAppView = (TextView) findViewById(R.id.licApp);
		
		final TextView licLibView = (TextView) findViewById(R.id.licLib);

		
		((ToggleButton) findViewById(R.id.btnViewLicApp)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					if(!loadedAppLic) {
						// strings.xml dosen't like 400 lines of license, read it from raw file
						licAppView.setText(readRawTxt(R.raw.fiksgatami));
						Linkify.addLinks(licAppView, Linkify.WEB_URLS);
					}
					licAppView.setVisibility(View.VISIBLE);
				} else {
					licAppView.setVisibility(View.GONE);
				}
			}
		});
		((ToggleButton) findViewById(R.id.btnViewLicLib)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					if(!loadedLibLic) {
						// strings.xml dosen't like 400 lines of license, read it from raw file
						licLibView.setText(readRawTxt(R.raw.httpmime));
						Linkify.addLinks(licLibView, Linkify.WEB_URLS);
					}
					licLibView.setVisibility(View.VISIBLE);
				} else {
					licLibView.setVisibility(View.GONE);
				}
			}
		});
		noteView2.setText(String.format(getString(R.string.copyright), versionName));
		Linkify.addLinks(noteView, Linkify.ALL);
		Linkify.addLinks(noteView2, Linkify.ALL);
		
	}

	// ****************************************************
	// Options menu functions
	// ****************************************************

	// TODO - add Bundles for these?
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem homeItem = menu.add(0, 0, 0, "Hjem");
		MenuItem aboutItem = menu.add(0, 1, 0, "Hjelp");
		aboutItem.setIcon(android.R.drawable.ic_menu_info_details);
		homeItem.setIcon(android.R.drawable.ic_menu_edit);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent i = new Intent(About.this, Home.class);
			if (extras != null) {
				i.putExtras(extras);
			}
			startActivity(i);
			return true;
		case 1:
			Intent j = new Intent(About.this, Help.class);
			if (extras != null) {
				j.putExtras(extras);
			}
			startActivity(j);
			return true;
		}
		return false;
	}
	private String readRawTxt(int id){

		InputStream inputStream = getResources().openRawResource(id);

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		int i;
		try {
			i = inputStream.read();
			while (i != -1)
			{
				byteArrayOutputStream.write(i);
				i = inputStream.read();
			}
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return byteArrayOutputStream.toString();
	}
}