package com.toraleap.collimator.data;

import java.util.regex.Pattern;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;

import com.toraleap.collimator.util.FileInfo;

/**
 * ÿ�� Matcher �����ʾ����ƥ���ж�������Matcher �಻�ɱ��ⲿʵ��������ʹ�ñ���ľ�̬��������ƥ�䡣
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1023
 */
public final class Matcher {
	
	private static final int MESSAGE_FIRST = 0;
	public static final int MATCHER_START = MESSAGE_FIRST + 1;
	public static final int MATCHER_ENTRY = MESSAGE_FIRST + 2;
	public static final int MATCHER_FINISHED = MESSAGE_FIRST + 3;
	public static final int MATCHER_NODATA = MESSAGE_FIRST + 4;
	public static final int MATCHER_SYNTAX_ERROR = MESSAGE_FIRST + 5;
	private static final int MATCHER_TYPE_NAME = 1;
	private static final int MATCHER_TYPE_FOLDER = 2;
	private static final int MATCHER_TYPE_SIZELT = 3;
	private static final int MATCHER_TYPE_SIZEGT = 4;
	private static final int MATCHER_TYPE_DATEDURING = 5;
	private static final int MATCHER_TYPE_MIMETYPE = 6;
	
	private static Thread sThread;
	private static Handler sHandler;
	private static boolean isRegex = false;
	private static boolean isFuzzy = true;
	
	private Pattern mPattern;
	private long mSeparator;
	private int mType = MATCHER_TYPE_NAME;
	private boolean isReverse = false;
	private int mStart;
	private int mEnd;
	
	public Matcher(String regex) throws Exception {
		if (regex.startsWith("!")) {
			isReverse = true;
			regex = regex.substring(1, regex.length());
		}
		if (regex.startsWith("/") || regex.startsWith("\\")) {
			mType = MATCHER_TYPE_FOLDER;
			mPattern = toRegex(regex.substring(1, regex.length()));
		} else if (regex.endsWith("/") || regex.endsWith("\\")) {
			mType = MATCHER_TYPE_FOLDER;
			mPattern = toRegex(regex.substring(0, regex.length() - 1));
		} else if (regex.startsWith("<")) {
			mType = MATCHER_TYPE_SIZELT;
			mSeparator = FileInfo.stringToSize(regex.substring(1, regex.length()));
		} else if (regex.startsWith(">")) {
			mType = MATCHER_TYPE_SIZEGT;
			mSeparator = FileInfo.stringToSize(regex.substring(1, regex.length()));
		} else if (regex.startsWith(":")) {
			mType = MATCHER_TYPE_DATEDURING;
			mSeparator = FileInfo.timespanToMillis(regex.substring(1, regex.length()));
		} else if (regex.startsWith("mimetype:")) {
			mType = MATCHER_TYPE_MIMETYPE;
			mPattern = toRegex(regex.substring(9, regex.length()));
		} else if (regex.startsWith("mt:")) {
			mType = MATCHER_TYPE_MIMETYPE;
			mPattern = toRegex(regex.substring(3, regex.length()));
		} else { 
			mType = MATCHER_TYPE_NAME;
			mPattern = toRegex(regex);
		}
	}

	/**
	 * ����ƥ���������ͶԲ���������Ŀ����ƥ�䡣
	 * @param i		���ڲ��Ե�������Ŀ
	 * @return �Ƿ�ɹ�ƥ��
	 */
	private boolean match(int i) {
		java.util.regex.Matcher matcher;
		switch (mType) {
		case MATCHER_TYPE_NAME:
			matcher = mPattern.matcher(Index.getName(i));
			if (matcher.find()) {
				mStart = matcher.start();
				mEnd = matcher.end();
				return !isReverse;
			} else if (null != Index.getNameAlpha(i)) {
				matcher = mPattern.matcher(Index.getNameAlpha(i));
				if (matcher.find()) {
					mStart = matcher.start();
					mEnd = matcher.end();
					return !isReverse;
				}
			}
			break;
		case MATCHER_TYPE_FOLDER:
			matcher = mPattern.matcher(Index.getPath(i));
			if (matcher.find()) {
				return !isReverse;
			} else if (null != Index.getPath(i)) {
				matcher = mPattern.matcher(Index.getPathAlpha(i));
				if (matcher.find()) {
					return !isReverse;
				}
			}
			break;
		case MATCHER_TYPE_SIZELT:
			if (Index.getSize(i) < mSeparator) return !isReverse;
			break;
		case MATCHER_TYPE_SIZEGT:
			if (Index.getSize(i) > mSeparator) return !isReverse;
			break;
		case MATCHER_TYPE_DATEDURING:
			if (Index.getTime(i) > System.currentTimeMillis() - mSeparator) return !isReverse;
			break;
		case MATCHER_TYPE_MIMETYPE:
			matcher = mPattern.matcher(FileInfo.mimeType(Index.getName(i)));
			if (matcher.find()) {
				return !isReverse;
			}
			break;
		}
		return isReverse;
	}
	
	/**
	 * ��ȡ��ƥ��������ʼλ�á�
	 * @return	��ʼλ������
	 */
	public int start() { return mStart; }
	/**
	 * ��ȡ��ƥ�����Ľ���λ�á�
	 * @return ����λ������
	 */
	public int end() { return mEnd; }
	/**
	 * ��ȡ��ƥ���������͡�
	 * @return ƥ��������
	 */
	public int type() { return mType; }
	/**
	 * ��ȡ��ƥ�����Ƿ񾭹�ȡ�����㡣
	 * @return �Ƿ�ȡ��
	 */
	public boolean isReverse() { return isReverse; }
	
	/**
	 * ʹ�ø����ı��ʽ���ļ������н����첽ƥ�䣬�ڼ�������κ���Ϣ�������͵���Ϣ���������������һ��ƥ������ڽ����У���ȡ����ǰ��ƥ����̣�Ȼ�������µ�ƥ�䡣
	 * @param expression	ƥ����ʽ
	 */
	public static void matchAsync(final Matcher[] matchers) {
		stopAsyncMatch();
		sThread = new Thread(new Runnable() {
        	public void run() {
        		MatchThread(matchers);
            }
        });
		sThread.start();
	}
	
	/**
	 * ����첽ƥ�����ڽ��У�������ֹ�źŲ��ȴ�����ֹ��
	 */
	public static void stopAsyncMatch() {
		if (null != sThread && sThread.isAlive()) {
			sThread.interrupt();
			try {
				sThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * �� matchAsync ���õ�ƥ���첽�̡߳�ƥ������в������κ���Ϣ�������͵�ע�����Ϣ��������
	 * @param expression	ƥ����ʽ
	 */
	private static void MatchThread(Matcher[] matchers) {
		if (null == matchers) {
			sHandler.sendEmptyMessage(MATCHER_SYNTAX_ERROR);
		} else {
			sHandler.sendEmptyMessage(MATCHER_START);
			LABEL_NEXTENTRY:
			for (int i = 0; i < Index.length(); i++) {
				if (sThread.isInterrupted()) return;
				if (Index.getName(i) == null) continue LABEL_NEXTENTRY;
				for (Matcher m : matchers) {
					if (!m.match(i)) continue LABEL_NEXTENTRY;
				}
				sendMatchEntry(i, matchers);
			}
			sHandler.sendEmptyMessage(MATCHER_FINISHED);
		}
	}
	
	/**
	 * ���ַ�����ʽ�ĳ���ͨ������������﷭��Ϊ������ʽ����������ѡ�����һЩ��Ҫ�Ĵ���
	 * @param key	����ı��ʽ
	 * @return	ת�����������ʽ
	 */
	private static Pattern toRegex(String key) {
		Pattern pattern;
		String patternKey;
		if (isRegex || key.startsWith("re:")) {
			if (key.startsWith("re:")) {
				patternKey = key.substring(3, key.length());
			} else {
				patternKey = key;
			}				
			if (!isFuzzy && !patternKey.startsWith("^")) {
				pattern = Pattern.compile("^" + patternKey, Pattern.CASE_INSENSITIVE);
			} else {
				pattern = Pattern.compile(patternKey, Pattern.CASE_INSENSITIVE);
			}
		} else {
			patternKey = key
				.replace("\\", "\\u005C")
				.replace(".", "\\u002E")
				.replace("$", "\\u0024")
				.replace("^", "\\u005E")
				.replace("{", "\\u007B")
				.replace("[", "\\u005B")
				.replace("(", "\\u0028")
				//.replace("|", "\\u007C")
				.replace(")", "\\u0029")
				.replace("+", "\\u002B")
				.replace("*", "[\\s\\S]*")
				.replace("?", "[\\s\\S]");
			if (isFuzzy) {
				pattern = Pattern.compile(patternKey, Pattern.CASE_INSENSITIVE);
			} else {
				pattern = Pattern.compile("^" + patternKey, Pattern.CASE_INSENSITIVE);
			}
		}
		return pattern;
	}
	
	/**
	 * ��һ���ɹ���ƥ�����װ���͸���Ϣ��������
	 * @param entry		ƥ��ɹ��� Entry ʵ��
	 * @param matchers	���д˴�ƥ��� Matcher ����
	 */
	private static void sendMatchEntry(int index, Matcher[] matchers) {
		Match match = new Match(index);
		for (Matcher matcher : matchers) {
			if (matcher.type() == Matcher.MATCHER_TYPE_NAME && matcher.isReverse == false) match.setHilite(matcher.start(), matcher.end());
		}
		Message msg = Message.obtain();
		msg.what = MATCHER_ENTRY;
		msg.obj = match;
		sHandler.sendMessage(msg);
	}
	
	/**
	 * ��ʼ��ƥ�����������Ĺ�����������ı���ѡ���Ӧ�ٴε��ô˺�����
	 * @param prefs		�������ѡ����󣬴���������������
	 * @param handler	���̵߳���Ϣ��������ƥ���̲߳����������Ϣ����������Ϣ������
	 */
	public static void init(SharedPreferences prefs, Handler handler) {
		isRegex = prefs.getBoolean("matching_regex", false);
		isFuzzy = prefs.getBoolean("matching_fuzzy", true);
		sHandler = handler;
	}
	
}
