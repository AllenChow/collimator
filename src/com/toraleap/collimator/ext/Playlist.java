package com.toraleap.collimator.ext;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.util.Log;

/**
 * ϵͳ��ý��洢�����б�
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1028
 */
public final class Playlist {
	
	private final ContentResolver mResolver;
	private final String[] mItems;
	
	/**
	 * ʹ������·���ļ������鹹��һ�������б�
	 */
	public Playlist(ContentResolver resolver, String[] items) {
		mResolver = resolver;
		mItems = items;
	}
	
	/**
	 * �ø���������ý����д���һ���µĲ����б������Խ���������ļ����뵽�б��С�������ͬ�������б�����ͼɾ��ԭ���б�
	 * @param name	�²����б�����
	 * @return �ɹ���ӵ���Ŀ��
	 */
	public int createNew(String name) {
		long[] ids = toMediaId();
		if (ids.length == 0) return 0;
		removeIfExist(name);
        ContentValues[] values = new ContentValues[ids.length];
        Uri uri = createPlaylist(name);
        for (int i = 0; i < ids.length; i++) {
            values[i] = new ContentValues();
            values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(i));
            values[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, ids[i]);
        }		
        return mResolver.bulkInsert(uri, values);
	}
	
	/**
	 * ��ý����в�ѯָ���Ĳ����б�����������ɾ����
	 * @param name	�����б�����
	 */
	public void removeIfExist(String name) {
        String whereclause = MediaStore.Audio.Playlists.NAME + " == '" + name.replace("'", "''") + "'";
        Cursor cursor = mResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
        		new String[] { MediaStore.Audio.Playlists._ID }, whereclause, null, MediaStore.Audio.Playlists.NAME);
        if (cursor != null && cursor.getCount() > 0) {
        	cursor.moveToFirst();
            long id = cursor.getLong(0);
            mResolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MediaStore.Audio.Playlists._ID + " == " + id, null);
        }
        if (cursor != null) cursor.close();
	}
	
	public PlaylistPair[] getPlaylists() {
		List<PlaylistPair> list = new ArrayList<PlaylistPair>();
        String[] cols = new String[] {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };
        String whereclause = MediaStore.Audio.Playlists.NAME + " != ''";
        Cursor cursor = mResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            cols, whereclause, null,
            MediaStore.Audio.Playlists.NAME);
        if (cursor != null && cursor.getCount() > 0) {
        	cursor.moveToFirst();
            while (! cursor.isAfterLast()) {
                list.add(new PlaylistPair(cursor.getInt(0), cursor.getString(1)));
                cursor.moveToNext();
            }
        }
        if (cursor != null) cursor.close();
        return (PlaylistPair[]) list.toArray();
	}
	
	/**
	 * �Ը������ִ���һ���µĲ����б��������²����б�� URI��
	 * @param name	�²����б������
	 * @return �²����б�� URI
	 */
	private Uri createPlaylist(String name) {
		ContentValues values = new ContentValues();
		values.put(MediaStore.Audio.PlaylistsColumns.NAME, name);
		return mResolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
	}
	
	/**
	 * ��ѯý�����ݿ⣬��˳���ÿһ���ļ�ת����ý�����ݿ��е� ID ��ʾ��
	 * @return ���� ID ����
	 */
	private long[] toMediaId() {
		long[] list = new long[mItems.length];
		for (int i = 0; i < list.length; i++) list[i] = -1;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mItems.length; i++) {
			if (i > 0) sb.append(",");
			// Android 2.1 or earlier
			sb.append("'").append(mItems[i].replace("'", "''")).append("',");
			// Android 2.2
			sb.append("'/mnt").append(mItems[i].replace("'", "''")).append("'");
		}
		String where = MediaStore.Audio.AudioColumns.DATA + " IN (" + sb.toString() + ")";
		try {
			Cursor cursor = mResolver.query(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					new String[] { Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.DATA }, 
					where, null, null);
			if (cursor == null) return new long[0];
			for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				String data = cursor.getString(1);
				for (int i = 0; i < mItems.length; i++) {
					if (data.endsWith(mItems[i])) {
						list[i] = cursor.getLong(0);
						break;
					}
				}
			}
		}
		catch (SQLiteException e) {
			Log.e("SQL", e.getMessage());
		}
		return shrinkLongArray(list);
	}
	
	/**
	 * ѹ��һ�� long �����飬������Ǹ�Ԫ�ء�
	 * @param source	��������
	 * @return �����Ǹ�Ԫ�ص�����
	 */
	public long[] shrinkLongArray(long[] source) {
		int count = 0;
		for (int i = 0; i < source.length; i++)
			if (source[i] >= 0) count++;
		long[] result = new long[count];
		count = 0;
		for (int i = 0; i < source.length; i++)
			if (source[i] >= 0) result[count++] = source[i];
		return result;
	}
	
	public class PlaylistPair {
		private long mId;
		private String mName;
		
		public PlaylistPair(int id, String name) {
			mId = id;
			mName = name;
		}
		
		public long getId() { return mId; }
		public String getName() { return mName; }
	}
}
