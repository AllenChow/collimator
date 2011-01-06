package com.toraleap.collimator.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.toraleap.collimator.R;

/**
 * ����һ������ʽ������������Χ������ʽ����Ϣ��֧�����л���JSON�ַ������Լ���JSON�ַ����ָ���
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1015
 */
public final class Expression {
	private Context mContext;
	private int mRange;
	private int mSortMode = Sorter.SORT_DATE;
	private boolean mSortReverse = true;
	private String mKey = "";
	private String mName;
	
	/**
	 * ����һ���յ�Ĭ�ϼ���ʽ��
	 * @param context	Ӧ�ó���������
	 */
	public Expression(Context context) { 
		mContext = context;
	}
	
	/**
	 * ��һ��JSON�����ַ����������ʽ��
	 * @param context	Ӧ�ó���������
	 * @param json		JSON�����ַ���
	 */
	public Expression(Context context, String json) {
		this(context);
		try {
			JSONObject obj = new JSONObject(json);
			mName = obj.optString("name");
			mRange = obj.optInt("range");
			mSortMode = obj.optInt("sortMode");
			mSortReverse = obj.optBoolean("sortReverse");
			mKey = obj.optString("key");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ��ȡ�˼���ʽ�ıȽ�����
	 * @return �Ƚ���
	 */
	public Comparator<Match> getSorter() {
		return Sorter.getSorter(mSortMode, mSortReverse);
	}
	
	/**
	 * ��ȡ�˼���ʽ������ʽ(����λ��Sorter����)��
	 * @return ����ʽ����
	 */
	public int getSortMode() {
		return mSortMode;
	}
	
	/**
	 * ��ȡ�˼���ʽ��������
	 * @return �Ƿ���
	 */
	public boolean getSortReverse() {
		return mSortReverse;
	}
	
	/**
	 * ��ȡ�˼���ʽ��������Χ(λ��array.xml��)��
	 * @return ������Χ����
	 */
	public int getRange() {
		return mRange;
	}
	
	/**
	 * ��ȡ�˼���ʽ�û�����Ĺؼ��֡�
	 * @return �ؼ����ַ���
	 */
	public String getKey() {
		return mKey;
	}
	
	/**
	 * ��ȡ�˼���ʽ�����ơ�
	 * @return �����ַ���
	 */
	public String getName() {
		if (mName == null) return mKey;
		return mName;
	}
	
	/**
	 * ���ô˼���ʽ������ʽ��
	 * @param sortMode ����ʽ(����λ��Sorter����)
	 * @param reverse	�Ƿ���
	 */
	public void setSort(int sortMode, boolean reverse) {
		mSortMode = sortMode;
		mSortReverse = reverse;
	}
	
	/**
	 * ���ô˼���ʽ������ʽ��
	 * @param sortMode ����ʽ(����λ��Sorter����)
	 */
	public void setSort(int sortMode) {
		mSortMode = sortMode;
	}
	
	/**
	 * ���ô˼���ʽ������ʽ��
	 * @param reverse	�Ƿ���
	 */
	public void setSort(boolean reverse) {
		mSortReverse = reverse;
	}
	
	/**
	 * ���ô˼���ʽ��������Χ(λ��array.xml��)��
	 * @param range		������Χ����
	 */
	public void setRange(int range) {
		mRange = range;
	}
	
	/**
	 * ���ô˼���ʽ�û�����Ĺؼ��֡�
	 * @param key	�ؼ����ַ���
	 */
	public void setKey(String key) {
		mKey = key;
	}
	
	/**
	 * ���ô˼���ʽ�����ơ�
	 * @return �����ַ���
	 */
	public void setName(String name) {
		mName = name;
	}
	
	/**
	 * ʹ�ô˼���ʽ�����첽ƥ�䡣�ڼ�������κ���Ϣ�������͵� Matcher �����Ϣ�����������Ҫ�ػ���Ϣ���ȵ��� Matcher.init ����ע����Ϣ���������������һ��ƥ������ڽ����У���ȡ����ǰ��ƥ����̣�Ȼ�������µ�ƥ�䡣
	 */
	public void matchAsync() {
		Matcher.matchAsync(matchers());
	}
	
	/**
	 * ����������ƥ����ʽ������ͼת��Ϊ Matcher �������顣������ʽ��Ч�������ؽ����õ��� Matcher ���飬���򷵻� null��
	 * @return �����õ��� Matcher ���� �� null
	 */
	public Matcher[] matchers() {
		String[] keys = mKey.split(" ");
		List<Matcher> matchers = new ArrayList<Matcher>(keys.length + 1);
		try {
			if (mRange > 0) {
				matchers.add(new Matcher(mContext.getResources().getStringArray(R.array.dialog_filter_range_entriesvalue)[mRange]));
			}
			for (int i = 0; i < keys.length; i++) {
				matchers.add(new Matcher(keys[i]));
			}
			return matchers.toArray(new Matcher[matchers.size()]);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * ������ʽת��ΪJSON�����ַ�����ʾ��
	 * @return JSON�����ַ���
	 */
	public String toJSON() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("name", mName);
			obj.put("range", mRange);
			obj.put("sortMode", mSortMode);
			obj.put("sortReverse", mSortReverse);
			obj.put("key", mKey);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj.toString();
	}
}
