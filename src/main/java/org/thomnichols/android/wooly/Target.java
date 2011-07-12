package org.thomnichols.android.wooly;

import android.net.Uri;
import android.provider.BaseColumns;

public class Target {

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.thomnichols.wooly.target";
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.thomnichols.wooly.target";
	public static final Uri CONTENT_URI = Uri.parse("content://"+Provider.AUTHORITY+"/target");
	public static final String EXTRA_TARGET_ID = "org.thomnichols.android.wooly.TARGET_ID";
	public static final String TABLE_NAME = "targets";
	
	static final class Cols implements BaseColumns {
		public static final String TITLE = "title"; 
		public static final String IMAGE = "image";
		public static final String URL = "link_url"; 
		public static final String FOUND = "found";
	}
	
	Integer id;
	String image;
	String title;
	String url;
	boolean found;
}
