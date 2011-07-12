package org.thomnichols.android.wooly;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

public class LocalTargetSearchTask extends AsyncTask<Void,Void,Integer> {

	static final String TAG = "WOOLY DB SEARCH";
	
	static final int RESULT_OK = 1;
	static final int RESULT_NOT_FOUND = 2;
	static final int RESULT_ALREADY_FOUND = 3;
	
	/* Force the task to take at least this long (in ms) to make it 
	 * seem like we're working really hard :) */
	long anticipationFactor = 3000; 
	String searchURL = null;
	Context ctx;
	long endTime;
	Target result; // set on result if background returns a found item 
	
	static final String[] SEARCH_COLS = {
		Target.Cols._ID,
		Target.Cols.TITLE,
		Target.Cols.URL,
		Target.Cols.IMAGE,
		Target.Cols.FOUND
	};
	
	public LocalTargetSearchTask(Context ctx, String searchURL) {
		this.ctx = ctx;
		this.searchURL = searchURL;
		this.endTime = System.currentTimeMillis() + anticipationFactor;
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		SQLiteDatabase db = Provider.getReadableDatabase(ctx);
		try {
			Cursor c = db.query(Target.TABLE_NAME, SEARCH_COLS, 
					Target.Cols.URL+"=?", new String[] {searchURL}, 
					null, null, null);
			
			try {
				if ( ! c.moveToFirst() ) return RESULT_NOT_FOUND;
				
				this.result = new Target();
				result.id = c.getInt(0);
				result.title = c.getString(1);
				result.url = c.getString(2);
				result.image = c.getString(3);
				result.found = c.getInt(4) == 1;
				
				if ( result.found ) return RESULT_ALREADY_FOUND;
			}
			finally { c.close(); }
			
			ContentValues vals = new ContentValues(1);
			vals.put(Target.Cols.FOUND,1);
			db.update(Target.TABLE_NAME, vals, 
					Target.Cols._ID+"=?", new String[] {""+result.id} );
			
			long remainder = endTime - System.currentTimeMillis();
			try {
				while ( remainder > 0 ) {
					Thread.sleep( remainder );
					remainder = endTime - System.currentTimeMillis();
				}
			}
			catch ( InterruptedException ex ) {
				Log.w(TAG,"Sleep interrupted!");
			}
			
			return RESULT_OK;
		}
		catch ( SQLException ex ) {
			Log.e(TAG,"Unexpected SQL error!", ex);
			return RESULT_NOT_FOUND;
		}
	}
}
