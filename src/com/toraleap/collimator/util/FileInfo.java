package com.toraleap.collimator.util;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.toraleap.collimator.R;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * �ṩ���ļ���Ϣ��صľ�̬���ߺ�����
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1025
 */
public class FileInfo {

	private static Context sContext;
	
	private FileInfo() { }
	
	/**
	 * ��ȡ�����ļ��������ļ���������
	 * @param filename	Դ�ļ���
	 * @return Դ�ļ��������ļ���������(����·������չ��)
	 */
	public static String mainName(String filename) {
		int start = filename.lastIndexOf("/");
		int stop = filename.lastIndexOf(".");
		if (stop < start) stop = filename.length();
		if (start >= 0) {
			return filename.substring(start + 1, stop);
		} else {
			return "";
		}
	}
	
	/**
	 * ��ȡ�����ļ�������չ������
	 * @param filename	Դ�ļ���
	 * @return Դ�ļ�������չ������(����С����)
	 */
	public static String extension(String filename) {
		int start = filename.lastIndexOf("/");
		int stop = filename.lastIndexOf(".");
		if (stop < start || stop >= filename.length() - 1) return "";
		else return filename.substring(stop + 1, filename.length());
	}
	
	/**
	 * ��ȡ�����ļ����� MIME ����
	 * @param filename	Դ�ļ���
	 * @return Դ�ļ����� MIME ����
	 */
	public static String mimeType(String filename) {
		String ext = extension(filename);
		String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
		return (mime == null) ? "*.*" : mime;
	}
	
	/**
	 * ��ȡ�ļ����ȵ����ܿɶ��ַ�����ʽ��
	 * @param size	�ļ��ֽڳ���
	 * @return	�ļ����ȵ��ַ�����ʾ
	 */
	public static String sizeString(long size) {
		if (size < 1024)
			return String.format("%d B", size);
		else if (size < 1024 * 1024)
			return String.format("%.2f KB", (double)size / 1024);
		else if (size < 1024 * 1024 * 1024)
			return String.format("%.2f MB", (double)size / (1024 * 1024));
		else if (size < 1024L * 1024 * 1024 * 1024)
			return String.format("%.2f GB", (double)size / (1024 * 1024 * 1024));
		else
			return String.format("%.2f EB", (double)size / (1024L * 1024 * 1024 * 1024));
	}
	
	/**
	 * ���ļ����ȵ��ַ�����ʽת��Ϊ�ֽ�����ʾ��
	 * @param sizeString	�ļ����ȵ��ַ�����ʾ
	 * @return	�ļ��ֽڳ���
	 * @throws ParseException	�����ַ�������֧�ֵ���ʽ������ʧ��
	 */
	public static long stringToSize(String sizeString) throws ParseException {
		Pattern pattern = Pattern.compile("(-?\\d+\\.?\\d*)([\\w]{0,2})", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(sizeString);
		if (matcher.matches()) {
			double baseSize = Double.parseDouble(matcher.group(1));
			String unit = matcher.group(2).toLowerCase();
			if (unit.equals("b") || unit.length() == 0) {
				return (long)baseSize;
			} else if (unit.equals("k") || unit.equals("kb")) {
				return (long)(baseSize * 1024);
			} else if (unit.equals("m") || unit.equals("mb")) {
				return (long)(baseSize * (1024 * 1024));
			} else if (unit.equals("g") || unit.equals("gb")) {
				return (long)(baseSize * (1024 * 1024 * 1024));
			} else if (unit.equals("e") || unit.equals("eb")) {
				return (long)(baseSize * (1024L * 1024 * 1024 * 1024));
			}
		}
		throw new ParseException(sizeString, 0);
	}
	
	/**
	 * ��ȡʱ���ȵ����ܿɶ��ַ�����ʽ����Ҫ����һ���Ǹ�������
	 * @param millisec	���뵥λ��ʱ���ȣ��Ǹ�����
	 * @return	ʱ���ȵ��ַ�����ʾ
	 */
	public static String timeString(long timeMillis) {
		if (timeMillis < 1000)
			return sContext.getString(R.string.util_fileinfo_milliseconds, timeMillis);
		else if (timeMillis < 1000 * 60)
			return sContext.getString(R.string.util_fileinfo_seconds, timeMillis / 1000);
		else if (timeMillis < 1000 * 60 * 60)
			return sContext.getString(R.string.util_fileinfo_minutes, timeMillis / (1000 * 60));
		else if (timeMillis < 1000 * 60 * 60 * 48)
			return sContext.getString(R.string.util_fileinfo_hours, timeMillis / (1000 * 60 * 60));
		else if (timeMillis < 1000L * 60 * 60 * 24 * 60)
			return sContext.getString(R.string.util_fileinfo_days, timeMillis / (1000L * 60 * 60 * 24));
		else if (timeMillis < 1000L * 60 * 60 * 24 * 30 * 12)
			return sContext.getString(R.string.util_fileinfo_months, timeMillis / (1000L * 60 * 60 * 24 * 30));
		else
			return sContext.getString(R.string.util_fileinfo_years, timeMillis / (1000L * 60 * 60 * 24 * 30 * 12));
	}
	
	/**
	 * ��ȡ���ʱ������ܿɶ��ַ�����ʽ��
	 * @param millisec	���뵥λ��ʱ����
	 * @return	���ʱ����ַ�����ʾ
	 */
	public static String timeSpanString(long timeMillis) {
		if (timeMillis > 0) {
			return sContext.getString(R.string.util_fileinfo_ago, timeString(timeMillis));
		} else {
			return sContext.getString(R.string.util_fileinfo_hence, timeString(-timeMillis));
		}
	}
	
	/**
	 * ��ʱ���ȵ��ַ�����ʽת��Ϊ��������ʾ��
	 * @param sizeString	ʱ���ȵ��ַ�����ʾ
	 * @return	������
	 * @throws ParseException	�����ַ�������֧�ֵ���ʽ������ʧ��
	 */
	public static long timespanToMillis(String timeString) throws ParseException {
		Pattern pattern = Pattern.compile("(-?\\d+\\.?\\d*)([\\w]{0,1})", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(timeString);
		if (matcher.matches()) {
			double baseMillis = Double.parseDouble(matcher.group(1));
			String unit = matcher.group(2).toLowerCase();
			if (unit.equals("d") || unit.length() == 0) {
				return (long)(baseMillis * 1000 * 3600 * 24);
			} else if (unit.equals("h")) {
				return (long)(baseMillis * 1000 * 3600);
			} else if (unit.equals("w")) {
				return (long)(baseMillis * 1000 * 3600 * 24 * 7);
			} else if (unit.equals("m")) {
				return (long)(baseMillis * 1000 * 3600 * 24 * 30);
			} else if (unit.equals("y")) {
				return (long)(baseMillis * 1000 * 3600 * 24 * 360);
			}
		}
		throw new ParseException(timeString, 0);
	}
	
	/**
	 * ��ʼ���ļ���Ϣ���ߺ��������Ĺ�����������ı���ѡ���Ӧ�ٴε��ô˺�����
	 * @param prefs			�������ѡ����󣬴���������������
	 * @param context		����������(������ Activity �������������� getApplicationContext() ���)
	 */
	public static void init(SharedPreferences prefs, Context context) {
		sContext = context;
	}
}
