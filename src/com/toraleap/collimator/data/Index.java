package com.toraleap.collimator.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Spanned;

import com.toraleap.collimator.data.IndexData.DifferentVersionException;
import com.toraleap.collimator.data.IndexLoader.DeserializingException;
import com.toraleap.collimator.data.IndexLoader.NoSDCardException;
import com.toraleap.collimator.data.IndexLoader.SerializingException;
import com.toraleap.collimator.util.DigestUtil;
import com.toraleap.collimator.util.ThumbnailUtil;

/**
 * �����ļ���������ز�����
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1104
 */
public final class Index {

	private static final int MESSAGE_FIRST = 100;
	public static final int MESSAGE_NOSDCARD = MESSAGE_FIRST;
	public static final int MESSAGE_RELOAD_SUCCESS = MESSAGE_FIRST + 1;
	public static final int MESSAGE_RELOAD_FAILED = MESSAGE_FIRST + 2;
	public static final int MESSAGE_SERIALIZING_FAILED = MESSAGE_FIRST + 3;
	public static final int MESSAGE_DESERIALIZING_SUCCESS = MESSAGE_FIRST + 4;
	public static final int MESSAGE_DESERIALIZING_FAILED = MESSAGE_FIRST + 5;
	public static final int MESSAGE_DESERIALIZING_DIFFERENT_VERSION = MESSAGE_FIRST + 6;
	public static final int MESSAGE_UPDATE_THUMBNAIL = MESSAGE_FIRST + 10;
	public static final int MESSAGE_UPDATE_DIGEST = MESSAGE_FIRST + 11;
	public static final int STATUS_FAILED = 0;
	public static final int STATUS_OBSOLETE = 1;
	public static final int STATUS_READY = 2;
	public static final int STATUS_RELOADING = 3;
	public static final int STATUS_DESERIALIZING = 4;
	private static Handler sEventHandler;
	private static SharedPreferences sPrefs;
	
	private static IndexData data;
	private static int status;

	private static ThumbnailUtil thumbUtil;
	private static DigestUtil digestUtil;
	
	private Index() { }
	
	/**
	 * �����ļ������ĵ�ǰ״̬��
	 * @return ��STATUSΪǰ׺��״̬����
	 */
	public static int getStatus() {
		return status;
	}
	
	/**
	 * ��������Ƿ��ѹ��ڣ������õ�ǰ״̬��
	 */
	public static void checkObsolete() {
		if (status == STATUS_READY && (IndexLoader.neededReload(data) || sPrefs.getBoolean("index_is_obsolete", true))) {
    		sPrefs.edit().putBoolean("index_is_obsolete", true).commit();
    		status = STATUS_OBSOLETE;
		}
	}
	
	/**
	 * ��ȡ�ļ������е���Ŀ������
	 * @return ��Ŀ����
	 */
	public static int length() {
		if (data != null)
			return data.length();
		else
			return 0;
	}
	
	/**
	 * ����ļ������Ľ���ʱ�䡣
	 * @return	�ļ������Ľ���ʱ��
	 */
	public static long reloadTime() {
		return data.indexTime;
	}
	
	/**
	 * ���SD���ڽ�������ʱ��ʣ��ռ䡣
	 * @return	ʣ��ռ��ֽ���
	 */
	public static long availableSpace() {
		return data.availableSpace;
	}
	
	/**
	 * �ж�����ͼ��ժҪ�Ķ�ȡ������
	 */
	public static void interrupt() {
		thumbUtil.interrupt();
		digestUtil.interrupt();
	}
	
	/**
	 * ��ʼ���ļ����������Ĺ�����������ı���ѡ���Ӧ�ٴε��ô˺�����
	 * @param prefs		�������ѡ����󣬴���������������
	 * @param handler	���̵߳���Ϣ�������������̲߳����������Ϣ����������Ϣ������
	 */
	public static void init(Context context, Handler handler) {
		sEventHandler = handler;
		sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		thumbUtil = new ThumbnailUtil(context, handler);
		digestUtil = new DigestUtil(context, handler);
	}
	
	/**
	 * ����һ�����߳��첽�������������������ʱ���Զ��������л�������������Ϣ���������� RELOAD_FINISHED ��Ϣ��
	 */
	public static synchronized void reloadEntriesAsync() {
		status = STATUS_RELOADING;
		new Thread(new Runnable() {
        	public void run() {
        		interrupt();
    			data = null;
        		IndexLoader loader = new IndexLoader(sPrefs);
        		try {
	        		IndexData newData = loader.reload();
					IndexLoader.serialize(newData);
    				data = newData;
		    		sPrefs.edit().putBoolean("index_is_obsolete", false).commit();
		    		status = STATUS_READY;
		    		checkObsolete();
	        		sendHandlerMessage(MESSAGE_RELOAD_SUCCESS, 0, 0, null);
        		} catch (NoSDCardException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_NOSDCARD, 0, 0, null);
        		} catch (SerializingException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_SERIALIZING_FAILED, 0, 0, null);
        		}
            }
        }).start();
	}
	
	/**
	 * �������̣߳������л�Ψһ�������󡣵������л����ʱ������Ϣ���������� DESERIALIZED_SUCCESS �� DESERIALIZED_FAILED ��Ϣ��
	 */
	public static synchronized void deserialization() {
		status = STATUS_DESERIALIZING;
		new Thread(new Runnable() {
        	public void run() {
        		interrupt();
        		try {
        			data = IndexLoader.deserialize();
	        		status = STATUS_READY;
	        		checkObsolete();
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_SUCCESS, 0, 0, null);    			
        		} catch (NoSDCardException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_NOSDCARD, 0, 0, null);
        		} catch (FileNotFoundException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_FAILED, 0, 0, null);
				} catch (DeserializingException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_FAILED, 0, 0, null);
				} catch (IOException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_FAILED, 0, 0, null);
				} catch (ClassNotFoundException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_FAILED, 0, 0, null);
				} catch (DifferentVersionException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_DIFFERENT_VERSION, 0, 0, null);
				} 
            }
        }).start();
	}

	/**
	 * ����Ϣ����������һ����Ϣ��
	 * @param what	��Ϣ����
	 * @param arg1	��Ϣ����1 (����Ϣ���Ͷ���)
	 * @param arg2	��Ϣ����2 (����Ϣ���Ͷ���)
	 * @param obj	��Ϣ���Ӷ��� (����Ϣ���Ͷ���)
	 */
	private static void sendHandlerMessage(int what, int arg1, int arg2, Object obj) {
		if (null != sEventHandler) {
			Message msg = Message.obtain();
			msg.what = what;
			msg.arg1 = arg1;
			msg.arg2 = arg2;
			msg.obj = obj;
			sEventHandler.sendMessage(msg);
		}
	}
		
	/**
	 * ��ȡָ��������Ŀ���ļ�����
	 * @return	�ļ���
	 */
	public static String getName(int i) { return data.name[i]; }
	/**
	 * ��ȡָ��������Ŀ�ļ���������ĸ��ʾ����û��Ϊ����ĸ�����������������ؿ��ַ�����
	 * @return	�ļ���������ĸ��ʾ
	 */
	public static String getNameAlpha(int i) { return data.nameAlpha[i]; }//Unicode2Alpha.toAlpha(data.name[i]); }
	/**
	 * ��ȡָ��������Ŀ�ļ����ڵ�·����
	 * @return	·���ַ���
	 */
	public static String getPath(int i) { return data.path[i]; }
	/**
	 * ��ȡָ��������Ŀ�ļ�����·��������ĸ��ʾ����û��Ϊ����ĸ������������������ null��
	 * @return	·���ַ���������ĸ��ʾ
	 */
	public static String getPathAlpha(int i) { return data.pathAlpha[i]; }//Unicode2Alpha.toAlpha(data.path[i]); }
	/**
	 * ��ȡָ��������Ŀ�ļ����ϴ��޸�ʱ�䣬�Գ����ͱ�ʾ���� 1970��1��1�� ����ĺ���ֵ��
	 * @return	�ļ����ϴ��޸�ʱ��
	 */
	public static long getTime(int i) { return data.time[i]; }
	/**
	 * ��ȡָ��������Ŀ�ļ����ֽ�Ϊ��λ��ʾ�ĳ��ȡ�
	 * @return	�ļ�����
	 */
	public static long getSize(int i) { return data.size[i]; }
	
	/***
	 * ��ȡָ��������Ŀ�ļ�������ͼ���������ͼ�ѻ��棬��ֱ�ӷ�������ͼ�����򷵻ش��������е�ͼ�񣬲������첽�����ȡ����ͼ�����첽������ɺ�����Ϣ���������� UPDATE_THUMBNAIL ��Ϣ��֪ͨ����ͼ�Ѹ��¡�
	 * @return �ļ�����ͼλͼ
	 */
	public static Bitmap getThumbnail(int i) {
		return thumbUtil.get(data.path[i] + "/" + data.name[i]);
	}
	
	/***
	 * ��ȡָ��������Ŀ�ļ���ժҪ��Ϣ�����ժҪ��Ϣ�ѻ��棬��ֱ�ӷ�����Ϣ�����򷵻ش��������е����֣��������첽�����ȡ�ļ�ժҪ�����첽������ɺ�����Ϣ���������� RELOAD_UPDATE_DIGEST ��Ϣ��֪ͨ�ļ�ժҪ�Ѹ��¡�
	 * @return �ļ�ժҪ�ַ���
	 */
	public static Spanned getDigest(int i) {
		return digestUtil.get(data.path[i] + "/" + data.name[i]);
	}

	/**
	 * ����ɾ��ָ��������Ŀ���ļ���
	 * @param i	 �������
	 * @return �ļ��Ƿ�ɹ�ɾ��
	 */
	public static boolean delete(int i) {
		File file = new File(data.path[i], data.name[i]);
		boolean result = file.delete();
		if (result == true) {
			data.name[i] = null;
		}
		return result;
	}
}
