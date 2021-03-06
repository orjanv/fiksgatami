//*************************************************************
//
//*************************************************************

package no.fiksgatami.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import no.fiksgatami.R;

public class Success extends Base {


	//private static final String LOG_TAG = "Success";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.success);
	}

	// ****************************************************
	// Options menu functions
	// ****************************************************

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem helpItem = menu.add(Menu.NONE, MENU_HOME, Menu.NONE, R.string.menu_home);
		helpItem.setIcon(android.R.drawable.ic_menu_edit);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent i = new Intent(Success.this, Home.class);
			startActivity(i);
			finish(); // finish so we don't go back here
			return true;
		}
		return false;
	}

	// disable the Back key in case things get submitted twice
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent(Success.this, Home.class);
			startActivity(i);
			finish(); // finish so we don't go back here
			return true;
		}
		return false;
	}

}