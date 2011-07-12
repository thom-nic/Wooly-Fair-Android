package org.thomnichols.android.wooly;

import java.net.URL;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.github.droidfu.widgets.WebImageView;

public class MainActivity extends ListActivity implements OnClickListener, DialogInterface.OnClickListener {

	static final String TAG = "WOOLY TARGETS LIST";
	static final String[] SELECT_TARGET_COLUMNS = {
		Target.Cols._ID, 
		Target.Cols.TITLE,
		Target.Cols.IMAGE,
		Target.Cols.URL }; 
	static final String[] VIEW_TARGET_COLUMNS = {
		Target.Cols.TITLE,
		Target.Cols.IMAGE }; 
	static final int[] VIEW_TARGET_IDS = {
		R.id.label_title,
		R.id.img_item }; 
	
	static final int ACTION_NEW_TARGET = 1;
	
	static final int DIALOG_ERROR_ID = 1;
	static final int DIALOG_FOUND_ITEM = 2;
	static final int DIALOG_ALREADY_FOUND = 3;
	static final int DIALOG_WAIT = 4;
	static final int DIALOG_NOT_FOUND = 5;
	
	SQLiteDatabase db;
	View titleBar = null;
	Target foundItem = null; // populated after a new item is found.
	TextView footerView = null;
	SoundPool soundPool = null;
	int r2d2Sound = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		soundPool = new SoundPool(1,AudioManager.STREAM_MUSIC,0);
		r2d2Sound = soundPool.load(this, R.raw.r2d2, 1);
		
		super.setContentView(R.layout.main_list_view);
		
		this.titleBar = findViewById(R.id.title_bar); 
		findViewById(R.id.btn_capture).setOnClickListener(this);
		findViewById(R.id.btn_capture2).setOnClickListener(this);
		
		Intent intent = getIntent();
		Uri uri = intent.getData();
		if ( uri == null ) uri = Target.CONTENT_URI;
		
		this.db = Provider.getReadableDatabase(this);
		
		final Cursor c = db.query(
				Target.TABLE_NAME, SELECT_TARGET_COLUMNS, 
				Target.Cols.FOUND+"=1", null, 
				null, null, Target.Cols._ID);
		startManagingCursor(c);
		c.setNotificationUri(getContentResolver(), uri);
		Log.d(TAG,"Count: " + c.getCount() );
		
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, 
				R.layout.main_list_item, c, 
				VIEW_TARGET_COLUMNS, VIEW_TARGET_IDS);
		
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if ( columnIndex == 2 ) { // image:
					WebImageView wiv = (WebImageView)view;
					wiv.setImageUrl(cursor.getString(columnIndex));
					wiv.loadImage();
					return true;
				}
				return false;
			}
		});
		this.refreshFoundItemCount(db);
		adapter.registerDataSetObserver(new DataSetObserver() {
			@Override public void onChanged() {
				super.onChanged();
				toggleTitleBar();
			}
		});
		setListAdapter(adapter);
		toggleTitleBar();
	}
	
	@Override
	protected void onDestroy() {
		this.soundPool.release();
		this.soundPool = null;
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {
		this.soundPool.autoPause();
		super.onPause();
	}
	
	void toggleTitleBar() {
		if ( getListAdapter() != null && getListAdapter().getCount() > 0 ) {
			Log.d(TAG, "Titlebar visible");
			titleBar.setVisibility(View.VISIBLE);
		}
		else {
			Log.d(TAG, "Titlebar hidden!");
			titleBar.setVisibility(View.GONE);
		}		
	}
	
	protected void refreshFoundItemCount(SQLiteDatabase db) {
		Cursor c = db.query(Target.TABLE_NAME, new String[] {"count(*)"}, 
				Target.Cols.FOUND+"=0", null,
				null, null, null);
		try {
			if ( ! c.moveToFirst() ) {
				Log.w(TAG,"No results for unfound count!!!");
				return;
			}
			int unfoundCount = c.getInt(0);
			if ( unfoundCount == 0 ) {
				// TODO show 'you've found them all!' blah blah blah
				return;
			}
			if ( this.footerView == null ) {
				this.footerView = (TextView)getLayoutInflater()
					.inflate(R.layout.main_list_footer, getListView(), false);
				footerView.setClickable(false);
				getListView().addFooterView(footerView);				
			}
			footerView.setText(getString(R.string.msg_main_footer, unfoundCount));
		}
		finally { c.close(); }
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if ( id < 0 ) return;
		startActivity( new Intent(Intent.ACTION_VIEW)
				.setData(ContentUris.withAppendedId(Target.CONTENT_URI,id)) );
	}
	
	public void onClick(View v) {
		switch ( v.getId() ) {
		case R.id.btn_capture:
		case R.id.btn_capture2:
	        startActivityForResult(
	        		new Intent("com.google.zxing.client.android.SCAN") 
	        			.setPackage(this.getPackageName())
	        			.putExtra("SCAN_MODE", "QR_CODE_MODE"),
	        		ACTION_NEW_TARGET);			
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if ( requestCode == ACTION_NEW_TARGET ) {
	        if (resultCode == RESULT_OK) {
				showDialog(DIALOG_WAIT);
	            final String contents = intent.getStringExtra("SCAN_RESULT");
	            String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
	            
	            if ( ! "QR_CODE".equals(format) || contents == null ) {
	            	showDialog(DIALOG_ERROR_ID);
	            	return;
	            }
	            Log.d(TAG,"Scan result: " + format + ", Contents: " + contents);
	            
	            try {
	            	new URL(contents);
	            }
	            catch ( Exception ex ) {
	            	Log.w(TAG,"Couldn't parse resulting URL: " + contents, ex);
	            	showDialog(DIALOG_ERROR_ID);
	            	return;
	            }
	            // Handle successful scan:
	            new LocalTargetSearchTask(this, contents) {
	            	protected void onPostExecute(Integer result) {
	            		try { dismissDialog(DIALOG_WAIT);
	            		} catch ( IllegalArgumentException ex ) {}
	            		
	            		switch(result) {
	            		case RESULT_OK:
		    	            // TODO vibrate when you've found a new rock
		    	            soundPool.play(r2d2Sound, 0.5f, 0.5f, 1, 0, 1f);
	            			Log.d(TAG,"Found new item: " + contents);
	            			refreshFoundItemCount(MainActivity.this.db);
	            			MainActivity.this.foundItem = this.result;
	            			((SimpleCursorAdapter)MainActivity.this.getListAdapter())
	            				.getCursor().requery();
	            			showDialog(DIALOG_FOUND_ITEM);
	            			break;
	            		case RESULT_NOT_FOUND:
	            			Log.d(TAG,"Couldn't find URL: " + contents);
	            			showDialog(DIALOG_NOT_FOUND);
	            			break;
	            		case RESULT_ALREADY_FOUND:
		    	            // if we've already found it, say "look somewhere else."
	            			Log.d(TAG,"Already found URL: " + contents);
	            			MainActivity.this.foundItem = this.result;
	            			showDialog(DIALOG_ALREADY_FOUND);
	            			break;
	            		}
	            	}
	            }.execute();	            
	        } 
	        else if (resultCode == RESULT_CANCELED) {
	            Log.d(TAG,"Scan Cancelled");
//	            dismissDialog(DIALOG_WAIT);
	        }
		}
 	}	
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dlg = null;
		switch (id) {
		case DIALOG_WAIT:
			dlg = new Dialog(this);
			dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dlg.setContentView(R.layout.processing_dialog_view);
			break;
		case DIALOG_FOUND_ITEM:
			dlg = new AlertDialog.Builder(this) 
			// TODO custom view
			.setIcon(R.drawable.icon)
			.setCancelable(true)
			.setMessage( getString(
					R.string.msg_youve_found, this.foundItem.title))
			.setPositiveButton("View", this)
			.setNegativeButton("Keep looking", this)
			.create();
			break;
		case DIALOG_ALREADY_FOUND:
			dlg = new AlertDialog.Builder(this) 
			.setIcon(R.drawable.icon)
			.setMessage( getString(
					R.string.msg_already_found, this.foundItem.title))
			.setPositiveButton("View", this)
			.setNegativeButton("Keep looking", this)
			.setCancelable(true)
			.create();
			break;
		case DIALOG_ERROR_ID:
		case DIALOG_NOT_FOUND:
			dlg = new AlertDialog.Builder(this)
				.setIcon(R.drawable.icon)
				.setMessage(R.string.msg_found_unknown)
				.setNegativeButton("Ok, I get it.", this)
				.setCancelable(true)
				.create();
			break;
		}	
		return dlg;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		switch (id) {
		case DIALOG_FOUND_ITEM:
			((AlertDialog)dialog).setMessage( getString(
					R.string.msg_youve_found, this.foundItem.title));
			break;
		case DIALOG_ALREADY_FOUND:
			((AlertDialog)dialog).setMessage( getString(
					R.string.msg_already_found, this.foundItem.title));
			break;
		}
	}

	// for the "Found new thing" dialog
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case Dialog.BUTTON_POSITIVE:
			// launch viewer
			startActivity( new Intent(this,ViewerActivity.class)
				.putExtra(ViewerActivity.EXTRA_URL, foundItem.url) );			
			break;
		case Dialog.BUTTON_NEGATIVE:
			break;
		}
		dialog.dismiss();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
		case R.id.menu_map:
			startActivity(new Intent(this,ViewerActivity.class)
				.putExtra( ViewerActivity.EXTRA_URL, 
						"file:///android_asset/fair_map.html") );
			break;
		case R.id.menu_schedule:
			break;
		case R.id.menu_about:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
