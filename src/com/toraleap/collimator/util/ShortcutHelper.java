package com.toraleap.collimator.util;

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;

/**
 * �������������ݷ�ʽ����ع���
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1104
 */
public final class ShortcutHelper {
	
	private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
	private static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";
	
	private Context mContext;
	private String mName;
	private Intent mIntent;
	private ShortcutIconResource mIconResource;
	
	/**
	 * �������� Intent ��װΪһ����ݷ�ʽ�����ࡣ
	 * @param context	�����Ķ���
	 * @param intent	Ŀ���ݷ�ʽ Intent ����
	 */
	public ShortcutHelper(Context context, Intent intent) {
		mContext = context;
		mIntent = intent;
	}
	
	/**
	 * ���ÿ�ݷ�ʽ��ʾ�����ơ�
	 * @param name	��ʾ�����ַ���
	 * @return ��ǰ��װ����
	 */
	public ShortcutHelper setName(String name) {
		mName = name;
		return this;
	}
	
	/**
	 * ���ÿ�ݷ�ʽ��ʾ��ͼ����Դ��
	 * @param iconResource	Ҫ��ʾ��ͼ��
	 * @return	��ǰ��װ����
	 */
	public ShortcutHelper setIconResource(ShortcutIconResource iconResource) {
		mIconResource = iconResource;
		return this;
	}
	
	/**
	 * ���ɿ�ݷ�ʽ�������ϡ�
	 * @param duplicate		�Ƿ�����ж����ݷ�ʽ�ĸ���
	 */
	public void install(boolean duplicate) {
		Intent installIntent = new Intent(ACTION_INSTALL_SHORTCUT);    
	    installIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mName);    
	    installIntent.putExtra(EXTRA_SHORTCUT_DUPLICATE, duplicate);
	    installIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, mIntent);    
	    installIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, mIconResource);
	    mContext.sendBroadcast(installIntent);  	
	}
}
