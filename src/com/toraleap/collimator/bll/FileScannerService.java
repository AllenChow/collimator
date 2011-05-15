package com.toraleap.collimator.bll;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.toraleap.collimator.dal.DBOperation;
import com.toraleap.collimator.model.BaseTag;
import com.toraleap.collimator.util.RecursiveFileObserver;

public class FileScannerService extends Service {
	private RealtimeScanner mScanner;
	private DBOperation mDBOperator;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("COLLIMATOR", "Service launched.");
		mDBOperator = new DBOperation(getApplicationContext());
//		new Thread(new Runnable(){
//			public void run() {
//				scanAll();
//			}
//		}).start();
		//launchObserver();
	}
	
	@Override
	public void onDestroy() {
		stopObserver();
		mDBOperator.close();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void scanFile(File file) {
		if (file.exists()) {
			Log.e("SCANFILE", file.getPath());
			if (mDBOperator.isFileExists(file)) {
				Log.e("UPDATEFILE", file.getPath());
				updateFile(file);
			} else {
				Log.e("INSERTFILE", file.getPath());
				insertFile(file);
			}
		} else {
			if (mDBOperator.isFileExists(file)) {
				Log.e("REMOVEFILE", file.getPath());
				removeFile(file);
			}
		}
	}
	
	/**
	 * ����ɨ��SD�����ؽ��ļ�������
	 */
	public void scanAll() {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return;
		}
		// ׼����ʼջ
		Stack<String> stack = new Stack<String>();
		stack.push(Environment.getExternalStorageDirectory().getPath());
		// ��ʼ�ļ�����
		while (!stack.isEmpty()) {
			String parent = stack.pop();
			String parentAlpha = null;
			File path = new File(parent);
			File[] files = path.listFiles();
			if (null == files) continue;
			for (File f : files)
			{
				if (f.isDirectory()) {
					if (isQualifiedDirectory(f)) stack.push(f.getPath());
				}
				else {
					if (isQualifiedFile(f)) {
						scanFile(f);
					}
				}
			}
		}
    }
	
	/**
	 * �ж�һ���ļ����Ƿ�Ӧ��ѹ�������ջ��
	 * @param file	ָ���ļ��е��ļ�����
	 * @return �Ƿ�Ӧ��ѹջ
	 */
	private boolean isQualifiedDirectory(File file) {
		if (file.getName().equals(".") || file.getName().equals("..")) return false;
//		if (!isIndexDotPrefix && file.getName().startsWith(".")) return false;
//		if (!isIndexHidden && file.isHidden()) return false;
//		if (!isIndexSystem && blackList.contains(file.getPath().toLowerCase())) return false;
//		if (new File(file.getPath(), ".nomedia").exists()) return false;
		return true;
	}
	
	/**
	 * �ж�һ���ļ��Ƿ�Ӧ�ý���������
	 * @param file	ָ���ļ����ļ�����
	 * @return �Ƿ�Ӧ������
	 */
	private boolean isQualifiedFile(File file) {
//		if (!isIndexDotPrefix && file.getName().startsWith(".")) return false;
//		if (!isIndexHidden && file.isHidden()) return false;
//		if (!isIndexAllType && FileInfo.mimeType(file.getName()).equals("*.*")) return false;
		return true;
	}
	
	public void insertFile(File file) {
		ArrayList<BaseTag> tags = TagGenerator.generate(file.getPath());
		mDBOperator.insertFile(file, tags);
	}
	
	public void updateFile(File file) {
		if (mDBOperator.isFileModified(file)) {
			ArrayList<BaseTag> tags = TagGenerator.generate(file.getPath());
			mDBOperator.updateFile(file, tags);
		}
	}
	
	public void removeFile(File file) {
		mDBOperator.removeFile(file);
	}
	
	private void launchObserver() {
		stopObserver();
		mScanner = new RealtimeScanner(Environment.getExternalStorageDirectory().getPath());
		mScanner.startWatching();
	}
	
	private void stopObserver() {
		if (mScanner != null) mScanner.stopWatching();
	}

	public class RealtimeScanner extends RecursiveFileObserver {

		public RealtimeScanner(String path) {
			super(path, CHANGES_ONLY);
		}

		@Override
		public void onEvent(int event, String path) {
			super.onEvent(event, path);
			switch (event) {
				case CREATE:
				case CLOSE_WRITE:
				case MOVED_FROM:
				case MOVED_TO:
				case DELETE:
					scanFile(new File(path));
					break;
			}
		}
		
	}
}
