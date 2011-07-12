package org.thomnichols.android.wooly;

import java.net.URL;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ViewerActivity extends Activity implements OnCancelListener {

	static final String TAG = "WOOLY WEBVIEW";
	static final int DIALOG_WAIT_ID = 1;
	static final String EXTRA_URL = "org.thomnichols.wooly.EXTRA_URL";
	
	WebView webView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.web_view);
		webView = (WebView)findViewById(R.id.web_view);
		webView.setWebViewClient(this.webClient);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setSupportZoom(true);
		String url = getIntent().getStringExtra(EXTRA_URL);

		try {
			if ( url == null ) { // look for a database ID in intent URI
				long id = ContentUris.parseId(getIntent().getData());
				Cursor c = Provider.getReadableDatabase(this).query(
						Target.TABLE_NAME, 
						new String[] { Target.Cols.URL }, 
						Target.Cols._ID+"=?", new String[] {""+id}, 
						null, null, null);
				if ( ! c.moveToFirst() ) {
					Log.w(TAG,"Not found: ID " + id);
					finish();
				}
				url = c.getString(0);
				c.close();
			}
			
			new URL(url);
		}
		catch ( SQLiteException ex ) {
			Log.w(TAG,"SQL error", ex);
			finish();
		}
		catch ( Exception ex ) {
			Log.w(TAG, "Bad URL: " + url, ex);
			finish();
		}
		// if url -> youtube.com, launch intent instead.
		Uri uri = Uri.parse(url);		
		if ( uri.getAuthority() != null &&
				uri.getAuthority().indexOf("youtube.com") >=0 ) {
			startActivity( new Intent(Intent.ACTION_VIEW,uri)
				.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY) );
			finish();
		}
		else webView.loadUrl(url);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dlg = null;
		switch ( id ) {
		case DIALOG_WAIT_ID:
			dlg = new Dialog(this);
			dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dlg.setContentView(R.layout.processing_dialog_view);
			dlg.setOnCancelListener(this);
			break;
		}
		return dlg;
	}
	
	public void onCancel(DialogInterface dialog) {
		this.webView.stopLoading();
	}
	
	final WebViewClient webClient = new WebViewClient() {
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			showDialog(DIALOG_WAIT_ID);
			super.onPageStarted(view, url, favicon);
		}
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			try {
				dismissDialog(DIALOG_WAIT_ID);
			}
			catch ( IllegalArgumentException ex ) {}
		}
	};
}
