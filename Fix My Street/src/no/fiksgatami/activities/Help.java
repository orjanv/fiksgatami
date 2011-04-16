package no.fiksgatami.activities;


import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import no.fiksgatami.R;

public class Help extends Base {
	private Bundle extras = null;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		extras = getIntent().getExtras();
		setContentView(R.layout.help);
		TextView noteView = (TextView) findViewById(R.id.faq);
		Linkify.addLinks(noteView, Linkify.ALL);
	}

	// ****************************************************
	// Options menu functions
	// ****************************************************

	// TODO - add Bundles for these?
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem homeItem = menu.add(Menu.NONE, MENU_HOME, Menu.NONE, R.string.menu_home);
		MenuItem aboutItem = menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.menu_about);
		homeItem.setIcon(android.R.drawable.ic_menu_edit);
		aboutItem.setIcon(android.R.drawable.ic_menu_info_details);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent i = new Intent(Help.this, Home.class);
			if (extras != null) {
				i.putExtras(extras);
			}
			startActivity(i);
			return true;
		case 1:
			Intent j = new Intent(Help.this, About.class);
			if (extras != null) {
				j.putExtras(extras);
			}
			startActivity(j);
			return true;
		}
		return false;
	}
}
