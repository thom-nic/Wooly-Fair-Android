package org.thomnichols.android.wooly;

import static java.lang.String.format;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {

	static final String TAG = "WOOLY DB";
	static final String DB_NAME = "wooly.db";
	static final int DB_VERSION = 1;
	
	static final String[][] STATIC_ENTRIES = {
		new String[] { "Neil Armstrong",
				"http://en.m.wikipedia.org/wiki/Neil_armstrong",
				"http://upload.wikimedia.org/wikipedia/commons/thumb/0/0d/Neil_Armstrong_pose.jpg/220px-Neil_Armstrong_pose.jpg"
			},
		new String[] { "Apollo 11",
				"http://en.m.wikipedia.org/wiki/Apollo_11",
				"http://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Apollo_11_insignia.png/201px-Apollo_11_insignia.png"
			},
		new String[] { "The Soyuz Rocket",
				"http://en.m.wikipedia.org/wiki/Soyuz_programme",
				"http://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Soyuz_rocket_ASTP.jpg/170px-Soyuz_rocket_ASTP.jpg"
			},		
		new String[] { "Ю́рий Алексе́евич Гага́рин",
				"http://en.m.wikipedia.org/wiki/Yuri_Gagarin",
				"http://upload.wikimedia.org/wikipedia/commons/c/cc/Gagarin_in_Sweden.jpg"
			},
		new String[] { "The Vostok Program",
				"http://en.m.wikipedia.org/wiki/Vostok_program",
				"http://upload.wikimedia.org/wikipedia/commons/thumb/7/71/Vostok-1_patch.svg/95px-Vostok-1_patch.svg.png"
			},
		new String[] { "Луноход (Lunokhod Soviet Lander)",
				"http://en.m.wikipedia.org/wiki/Lunokhod_programme",
				"http://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Lunokhod2.jpg/220px-Lunokhod2.jpg"
			},
		new String[] { "James Webb",
				"http://en.m.wikipedia.org/wiki/James_E._Webb",
				"http://upload.wikimedia.org/wikipedia/commons/thumb/c/c3/James_E._Webb%2C_official_NASA_photo%2C_1966.jpg/240px-James_E._Webb%2C_official_NASA_photo%2C_1966.jpg"
			},
		new String[] { "NASA",
				"http://en.m.wikipedia.org/wiki/NASA",
				"http://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/140px-NASA_logo.svg.png"
			},
		new String[] { "The X-15 Spaceplane",
				"http://en.m.wikipedia.org/wiki/North_American_X-15",
				"http://upload.wikimedia.org/wikipedia/commons/thumb/d/d6/X-15_in_flight.jpg/300px-X-15_in_flight.jpg"
			},
		new String[] { "The Atlantis Shuttle Launch",
				"http://www.youtube.com/watch?v=C_8cfzWoYVQ",
				"http://upload.wikimedia.org/wikipedia/commons/thumb/1/16/STS122_Atlantis.jpg/200px-STS122_Atlantis.jpg"
			},
	};
	
	public Database(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table " + Target.TABLE_NAME + " ( "
				+ Target.Cols._ID + " integer primary key," 
				+ Target.Cols.TITLE + " varchar(40) not null," 
				+ Target.Cols.URL + "  varchar(100) unique not null,"
			    + Target.Cols.IMAGE + " varchar(80)," 
			    + Target.Cols.FOUND + " bit not null default 0)");
		db.execSQL( format("create index idx_target_found on %s (%s)", 
				Target.TABLE_NAME, Target.Cols.FOUND ) );
		
		ContentValues vals = new ContentValues();
		for ( String[] row : STATIC_ENTRIES ) {
			vals.clear();
			vals.put(Target.Cols.TITLE, row[0]);
			vals.put(Target.Cols.URL, row[1]);
			vals.put(Target.Cols.IMAGE, row[2]);
			db.insert(Target.TABLE_NAME, null, vals);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int from, int to) {
		Log.i(TAG, "+++++++++++ UPGRADE +++ from v" + from + " to v" + to);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		if (!db.isReadOnly()) {
			// Enable foreign key constraints
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}

    public static class DBException extends Exception {
		private static final long serialVersionUID = 234523699234581L;
		public DBException() { super(); }
		public DBException(String arg0, Throwable arg1) { super(arg0, arg1); }
		public DBException(String arg0) { super(arg0); }
		public DBException(Throwable arg0) { super(arg0); }
    }    
}
