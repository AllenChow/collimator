package com.toraleap.collimator.data;

import android.graphics.Bitmap;
import android.text.Html;
import android.text.Spanned;

import com.toraleap.collimator.util.FileInfo;

/**
 * ÿ�� Match �����ʾ�ɹ��ĵ���ƥ���
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1104
 */
public final class Match {
	private final int mIndex;
	private boolean[] mHilite;
	
	/**
	 * ƥ����캯����
	 * @param entry		�˳ɹ�ƥ���������Ŀ
	 */
	public Match(int index) {
		mIndex = index;
		mHilite = new boolean[Index.getName(index).length()];
	}
	
	/**
	 * ����ƥ�����ļ����ĸ�����Χ������жദ������Χ���ɶ�ε��ñ�������
	 * @param start		������Χ���
	 * @param end		������Χ�յ�
	 */
	public void setHilite(int start, int end) {
		for (int i = start; i < end; i++)
			mHilite[i] = true;
	}
	
	/**
	 * ��ȡ��ƥ�����Ӧ��������
	 * @return ����
	 */
	public int index() { return mIndex; }
	/**
	 * ��ȡ�ļ�����
	 * @return	�ļ���
	 */
	public String name() { return Index.getName(mIndex); }
	/**
	 * ��ȡ�ļ���������ĸ��ʾ����û��Ϊ����ĸ������������������ null��
	 * @return	�ļ���������ĸ��ʾ
	 */
	public String nameAlpha() { return Index.getNameAlpha(mIndex); }
	/**
	 * ��ȡ�ļ����ڵ�·����
	 * @return	·���ַ���
	 */
	public String path() { return Index.getPath(mIndex); }
	/**
	 * ��ȡ�ļ�����·��������ĸ��ʾ����û��Ϊ����ĸ������������������ null��
	 * @return	·���ַ���������ĸ��ʾ
	 */
	public String pathAlpha() { return Index.getPathAlpha(mIndex); }
	/**
	 * ��ȡ�ļ����ֽ�Ϊ��λ��ʾ���ļ����ȡ�
	 * @return	�ļ�����
	 */
	public long size() { return Index.getSize(mIndex); }
	/**
	 * ��ȡ�ļ����ȵ����ܿɶ��ַ�����ʽ��
	 * @return	�ļ����ȵ��ַ�����ʾ
	 */
	public String sizeString() { return FileInfo.sizeString(Index.getSize(mIndex)); }
	/**
	 * ��ȡ�ļ��ϴ��޸�ʱ��� 1970-01-01 �ĺ�������
	 * @return	�ϴ��޸�ʱ��ĺ�����
	 */
	public long time() { return Index.getTime(mIndex); }
	/**
	 * ��ȡ�ļ��ϴ��޸�ʱ����ı��ػ��ַ�����ʾ��
	 * @return	���ʱ��ı��ػ���ʾ
	 */
	public String timeString() { return FileInfo.timeSpanString(System.currentTimeMillis() - Index.getTime(mIndex)); }
	/***
	 * ��ȡ�ļ�����ͼ���������ͼ�ѻ��棬��ֱ�ӷ�������ͼ�����򷵻� null��������һ�����̺߳�̨��ȡ����ͼ��������ͼ�ɹ�ȡ��ʱ����Ϣ���������� RELOAD_UPDATE_THUMBNAIL ��Ϣ��֪ͨ����ͼ�Ѹ��¡�
	 * @return �ļ�����ͼλͼ
	 */
	public Bitmap thumbnail() { return Index.getThumbnail(mIndex); }
	/***
	 * ��ȡ�ļ���ժҪ��Ϣ�����ժҪ��Ϣ�ѻ��棬��ֱ�ӷ�����Ϣ�����򷵻� null��������һ�����̺߳�̨��ȡ�ļ�ժҪ�����ļ�ժҪ�ɹ�ȡ��ʱ����Ϣ���������� RELOAD_UPDATE_DIGEST ��Ϣ��֪ͨ�ļ�ժҪ�Ѹ��¡�
	 * @return �ļ�ժҪ�ַ���
	 */
	public Spanned digest() { return Index.getDigest(mIndex); }
	/***
	 * ��ȡ�����������ļ�����ʾ����
	 * @return �����ı���
	 */
	public Spanned highlightedName() {
		String source = Index.getName(mIndex);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mHilite.length; i++) {
			if (mHilite[i]) {
				sb.append("<font color='#ffff00'>" + source.charAt(i) + "</font>");
			} else {
				sb.append(source.charAt(i));
			}
		}
		return Html.fromHtml(sb.toString());
	}
}