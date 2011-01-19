package com.toraleap.collimator.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;

import com.toraleap.collimator.data.IndexData.DifferentVersionException;
import com.toraleap.collimator.util.FileInfo;
import com.toraleap.collimator.util.Unicode2Alpha;

/**
 * �����ļ��������ؽ������л��������л��ķ�����
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1025
 */
final class IndexLoader {
	
	// ��ѡ�����
	private boolean isIndexHidden = false;
	private boolean isIndexSystem = false;
	private boolean isIndexDotPrefix = false;
	private boolean isIndexFirstLetter = true;
	private boolean isIndexAllType = false;

	private final HashSet<String> blackList = new HashSet<String>();
	
	public IndexLoader(SharedPreferences prefs) {
		isIndexHidden = prefs.getBoolean("index_hidden", false);
		isIndexSystem = prefs.getBoolean("index_system", false);
		isIndexDotPrefix = prefs.getBoolean("index_dotprefix", false);
		isIndexFirstLetter = prefs.getBoolean("index_firstletter", true);
		isIndexAllType = prefs.getBoolean("index_alltype", false);
		getBlacklist();
	}
	
	/**
	 * ���ݹ��캯����ȡ�����ã�ɨ��SD�����ؽ��ļ�������
	 * @return ��������������
	 * @throws NoSDCardException δ��⵽���ص�SD��
	 */
	public IndexData reload() throws NoSDCardException {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new NoSDCardException();
		}
		// ׼����ʼջ
		Stack<String> stack = new Stack<String>();
		stack.push(Environment.getExternalStorageDirectory().getPath());
		// ׼�������б�
		IndexData data = new IndexData();
		ArrayList<String> lName = new ArrayList<String>();
		ArrayList<String> lPath = new ArrayList<String>();
		ArrayList<String> lNameAlpha = new ArrayList<String>();
		ArrayList<String> lPathAlpha = new ArrayList<String>();
		ArrayList<Long> lSize = new ArrayList<Long>();
		ArrayList<Long> lTime = new ArrayList<Long>();
		// ��ʼ�ļ�����
		while (!stack.isEmpty()) {
			String parent = stack.pop();
			String parentAlpha = null;
			if (isIndexFirstLetter) {
				parentAlpha = Unicode2Alpha.toAlpha(parent);
			}
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
						lName.add(f.getName());
						lPath.add(parent);
						lTime.add(f.lastModified());
						lSize.add(f.length());
						if (isIndexFirstLetter) {
							lNameAlpha.add(Unicode2Alpha.toAlpha(f.getName()));
							lPathAlpha.add(parentAlpha);
						} else {
							lNameAlpha.add("");
							lPathAlpha.add("");
						}
					}
				}
			}
		}
		int length = lName.size();
		data.indexTime = System.currentTimeMillis();
		data.availableSpace = getAvailableSpace();
		data.name = lName.toArray(new String[length]);
		data.path = lPath.toArray(new String[length]);
		data.nameAlpha = lNameAlpha.toArray(new String[length]);
		data.pathAlpha = lPathAlpha.toArray(new String[length]);
		data.size = new long[length];
		for (int i = 0; i < length; i++) data.size[i] = lSize.get(i).longValue();
		data.time = new long[length];
		for (int i = 0; i < length; i++) data.time[i] = lTime.get(i).longValue();
		return data;
    }
	
	/**
	 * ���л���������
	 * @param data	Ҫ���л��Ķ���
	 * @throws SerializingException ���л�ʧ��
	 */
	public static void serialize(IndexData data) throws SerializingException {
		File f;
		DataOutputStream out = null;
		try {
			f = new File(Environment.getExternalStorageDirectory().getPath() + "/.collimator");
			if (!f.exists()) f.mkdirs();
			f = new File(Environment.getExternalStorageDirectory().getPath() + "/.collimator/index.dat");
			if (f.exists()) f.delete();
			out = new DataOutputStream(new FileOutputStream(f));
			data.write(out);
		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializingException();
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	/**
	 * �����л���������
	 * @return ���ɹ������ط����л����������󣻷��򷵻�null��
	 * @throws NoSDCardException δ��⵽���ص�SD��
	 * @throws DeserializingException �����ļ���ʽ����ȷ
	 * @throws DifferentVersionException �����ļ��汾�쳣
	 * @throws ClassNotFoundException �����л��쳣
	 * @throws IOException �����ļ���ȡ�쳣
	 */
	public static IndexData deserialize() throws NoSDCardException, DeserializingException, IOException, ClassNotFoundException, DifferentVersionException {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new NoSDCardException();
		}
		File f = new File(Environment.getExternalStorageDirectory().getPath() + "/.collimator/index.dat");
		if (!f.exists()) {
			throw new FileNotFoundException();
		}
		DataInputStream in = null;
		FileInputStream file = null;
		try {
			file = new FileInputStream(f.getPath());
			in = new DataInputStream(file);
			IndexData data = new IndexData();
			data.read(in);
			return data;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (file != null)
				try {
					file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	/**
	 * ׼��ϵͳ�������б�
	 */
	private void getBlacklist() {
		String root = Environment.getExternalStorageDirectory().getPath();
		blackList.add(root + "/lost.dir");
		blackList.add(root + "/android");
		blackList.add(root + "/brut.googlemaps");		
		blackList.add(root + "/navione");		
		blackList.add(root + "/picstore");		
	}
	
	/**
	 * �ж�һ���ļ����Ƿ�Ӧ��ѹ�������ջ��
	 * @param file	ָ���ļ��е��ļ�����
	 * @return �Ƿ�Ӧ��ѹջ
	 */
	private boolean isQualifiedDirectory(File file) {
		if (file.getName().equals(".") || file.getName().equals("..")) return false;
		if (!isIndexDotPrefix && file.getName().startsWith(".")) return false;
		if (!isIndexHidden && file.isHidden()) return false;
		if (!isIndexSystem && blackList.contains(file.getPath().toLowerCase())) return false;
		if (new File(file.getPath(), ".nomedia").exists()) return false;
		return true;
	}
	
	/**
	 * �ж�һ���ļ��Ƿ�Ӧ�ý���������
	 * @param file	ָ���ļ����ļ�����
	 * @return �Ƿ�Ӧ������
	 */
	private boolean isQualifiedFile(File file) {
		if (!isIndexDotPrefix && file.getName().startsWith(".")) return false;
		if (!isIndexHidden && file.isHidden()) return false;
		if (!isIndexAllType && FileInfo.mimeType(file.getName()).equals("*.*")) return false;
		return true;
	}
	
	/**
	 * ��ȡ�ⲿ�洢�ϵĿ��ÿռ��С��
	 * @return ���ÿռ���ֽ���
	 */
	private static long getAvailableSpace() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			long indexSize = 0;
			File indexFile = new File(Environment.getExternalStorageDirectory().getPath() + "/.collimator/index.dat");
			if (indexFile.exists()) indexSize = indexFile.length();
			StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
			return stat.getBlockSize() * stat.getAvailableBlocks() + indexSize;
		}
		return 0;
	}
	
	/**
	 * ����ʣ��ռ��ж������Ƿ��ѹ��ڡ�
	 * @param data	�ļ��������ݽṹ
	 * @return �Ƿ��ѹ���
	 */
	public static boolean neededReload(IndexData data) {
    	if (Math.abs(getAvailableSpace() - data.availableSpace) >= 1024L * 50)  return true;
    	return false;
	}
	
	@SuppressWarnings("serial")
	public static class NoSDCardException extends Exception {
		public NoSDCardException() {
			super("No SDCard was found.");
		}
		public NoSDCardException(String detailMessage) {
			super(detailMessage);
		}
	}
	
	@SuppressWarnings("serial")
	public static class DeserializingException extends Exception {
		public DeserializingException() {
			super("Deserializing failed.");
		}
		public DeserializingException(String detailMessage) {
			super(detailMessage);
		}
	}
	
	@SuppressWarnings("serial")
	public static class SerializingException extends Exception {
		public SerializingException() {
			super("Serializing failed.");
		}
		public SerializingException(String detailMessage) {
			super(detailMessage);
		}
	}
}
