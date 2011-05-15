package com.toraleap.collimator.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * ����ͼ��ȡ�����ࡣ�ڲ���������ͼ�����û����������ڴ治��ʱ���Զ��ͷŻ����Ա�֤ϵͳ�������С��ڽ�����ͼ�ߴ�Ϊ 96x96��
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1029
 */
public class ThumbnailUtil extends SoftCache<String, Bitmap> {

	private Bitmap mDefaultThumbnail;
	private Bitmap mLoadingThumbnail;
	private Context mContext;
	private SharedPreferences mPrefs;
	private boolean isDisplayThumbnail;
	
	/**
	 * ��ʼ������ͼ����ʵ����
	 * @param context	����������(������ Activity �������������� getApplicationContext() ���)
	 */
	public ThumbnailUtil(Context context, Handler handler) {
		super(handler);
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		refresh();
	}
	
	/**
	 * ˢ������ͼ��ȡ���á�
	 */
	public void refresh() {
		isDisplayThumbnail = mPrefs.getBoolean("display_thumbnail", true);
	}

	@Override
	Bitmap request(String key) {
		return fromFile(key);
	}
	
	@Override
	int getMaxQueueLength() {
		return 10;
	}
	
	@Override
	Bitmap getDefault() {
		if (null == mLoadingThumbnail) {
			try {
				InputStream is = mContext.getAssets().open("icons/loading.png");
				mLoadingThumbnail = BitmapFactory.decodeStream(is);
				is.close();
			} catch (IOException e) {
				mLoadingThumbnail = getUndefined();
			}
		}
		return mLoadingThumbnail;
	}
	
	/**
	 * ����������·���ļ�����ȡ���Ӧ������ͼ������ͼƬ�ļ������Լ��ظ�ͼƬ��ת��Ϊ����ͼ�����������ļ������Լ��ض�Ӧ����ͼƬ���������ͳ��Լ�����Դ�ļ�������չ����Ӧ��ͼ����Ϊ����ͼ�����δ�ҵ���Ӧͼ�꣬�򷵻�Ĭ��ͼ�ꡣ
	 * @param filename	������·���ļ���
	 * @return ����ͼλͼ
	 */
	private Bitmap fromFile(String filename) {
		Bitmap thumbnail = null;
		if (isDisplayThumbnail) {
			String mimeType = FileInfo.mimeType(filename);
			if (mimeType.startsWith("image/")) {
				thumbnail = loadFromImage(filename);
			} else if (mimeType.startsWith("video/")) {
				thumbnail = loadFromVideo(filename);
			} else if (mimeType.startsWith("audio/")) {
				thumbnail = loadFromAudio(filename);
			} else if (mimeType.startsWith("text/plain")) {
				thumbnail = loadFromAudio(filename);
			} else if (mimeType.equals("application/vnd.android.package-archive")) {
				thumbnail = loadFromApk(filename);
			}
		}
		return (null == thumbnail) ? fromExt(filename) : thumbnail;
	}
	
	/**
	 * �Ӹ���ͼƬ�ļ��м���ͼƬ����ת��Ϊ����ͼ��
	 * @param path	������·���ļ���
	 * @return	 ����ͼλͼ
	 */
	private Bitmap loadFromImage(String path) {
		Bitmap thumbnail = null;
		try{
			Options options = new Options();
			// ��ȡ�߽���ȷ��ͼ���С
			options.inSampleSize = 1;
	        options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
	        if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) return fromExt(path);
	        // ���ò����ʲ�������ȡͼ��
	        setSampleSize(options);
	        options.inDither = true;
	        options.inJustDecodeBounds = false;
	        //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			Bitmap bitmap = BitmapFactory.decodeFile(path, options);
			if (null != bitmap) {
				thumbnail = scaleBitmap(bitmap);
				bitmap.recycle();
				bitmap = null;
			}
			return thumbnail;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * �Ӹ�����Ƶ�ļ������ļ����м��ط���ͼƬ����ת��Ϊ����ͼ��
	 * @param path	������·���ļ���
	 * @return	 ����ͼλͼ
	 */
	private Bitmap loadFromAudio(String path) {
		String folder = path.substring(0, path.lastIndexOf("/"));
		if (new File(folder + "/AlbumArt.jpg").exists()) {
			return requestAndCache(folder + "/AlbumArt.jpg");
		} else if (new File(folder + "/cover.jpg").exists()) {
			return requestAndCache(folder + "/cover.jpg");
		} else if (new File(folder + "/folder.jpg").exists()) {
			return requestAndCache(folder + "/folder.jpg");
		}
		return null;
	}
	
	/**
	 * �Ӹ�����Ƶ�ļ��ж�ȡ֡����ת��Ϊ����ͼ��
	 * @param path	������·���ļ���
	 * @return	 ����ͼλͼ
	 */
	private Bitmap loadFromVideo(String path) {
		Bitmap thumbnail = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setMode(MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
            retriever.setDataSource(path);
            Bitmap bitmap = retriever.captureFrame();
            thumbnail = scaleBitmap(bitmap);
			bitmap.recycle();
			bitmap = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodError err) {
        	err.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
		return thumbnail;
    }
	
	/**
	 * �Ӹ���Ӧ�ó�����ļ��м�������ͼ��
	 * @param path	������·���ļ���
	 * @return	 ����ͼλͼ
	 */
	private Bitmap loadFromApk(String path) {
		Bitmap thumbnail = null;
		PackageManager pm = mContext.getPackageManager();      
        PackageInfo info = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);      
        if(info != null){
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = path;
            appInfo.publicSourceDir = path;
            Drawable drawable = pm.getApplicationIcon(appInfo);
            thumbnail = scaleBitmap(((BitmapDrawable)drawable).getBitmap());
        }
		return thumbnail;
	}

	/**
	 * ������չ��������Դ�ļ�������չ����Ӧ��ͼ����Ϊ����ͼ�����δ�ҵ���Ӧͼ�꣬�򷵻�Ĭ��ͼ�ꡣ
	 * @param filename	�ļ���
	 * @return	 ����ͼλͼ
	 */
	private Bitmap fromExt(String filename) {
		String ext = FileInfo.extension(filename);
		Bitmap thumbnail = getCache(ext);
		if (thumbnail != null) return thumbnail;
		try {
			InputStream is = mContext.getAssets().open("icons/ext/" + ext + ".png");
			thumbnail = scaleBitmap(BitmapFactory.decodeStream(is));
			is.close();
		} catch (IOException e) {
			try {
				String mime = FileInfo.mimeType(filename).replace('/', '_');
				InputStream is = mContext.getAssets().open("icons/mime/" + mime + ".png");
				thumbnail = scaleBitmap(BitmapFactory.decodeStream(is));
				is.close();
			} catch (IOException ex) {
				try {
					String mimetype = FileInfo.mimeType(filename).split("/")[0];
					InputStream is = mContext.getAssets().open("icons/mime/" + mimetype + ".png");
					thumbnail = scaleBitmap(BitmapFactory.decodeStream(is));
					is.close();
				} catch (IOException exc) {
					exc.printStackTrace();
					thumbnail = getUndefined();
				}
			}
		}
		return putCache(ext, thumbnail);
	}
	
	/**
	 * ��ȡĬ�ϵ�ͼ����Ϊ����ͼ��
	 * @return	����ͼλͼ
	 */
	public Bitmap getUndefined() {
		if (null == mDefaultThumbnail) {
			try {
				InputStream is = mContext.getAssets().open("icons/undefined.png");
				mDefaultThumbnail = scaleBitmap(BitmapFactory.decodeStream(is));
				is.close();
			} catch (IOException e) {
				mDefaultThumbnail = null;
			}
		}
		return mDefaultThumbnail;
	}

	private static void setSampleSize(Options options) {
		int maxSize = options.outWidth > options.outHeight ? options.outWidth : options.outHeight;
		int minSample = (maxSize / 96) >> 1 ;
		int sample = 1;
		while (sample <= minSample) sample <<= 1;
		options.inSampleSize = sample;
	}
	
	private static Bitmap scaleBitmap(Bitmap source) {
		int maxSize = source.getWidth() > source.getHeight() ? source.getWidth() : source.getHeight();
        return Bitmap.createScaledBitmap(source, source.getWidth() * 96 / maxSize, source.getHeight() * 96 / maxSize, true);
	}
}