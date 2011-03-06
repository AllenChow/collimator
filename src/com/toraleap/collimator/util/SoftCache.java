package com.toraleap.collimator.util;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * ����һ�������û�������������Ӧ��д request ������ȡ�ؼ��ֶ�Ӧ�Ľ����
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1104
 *
 * @param <K>	�ؼ��ֵ�����
 * @param <V>	���������
 */
public abstract class SoftCache<K, V> {

	private static final int MESSAGE_FIRST = 200;
	public static final int MESSAGE_CACHE_GOT = MESSAGE_FIRST + 1;
	public static final int MESSAGE_QUEUE_FINISHED = MESSAGE_FIRST + 2;
	
    private final ConcurrentHashMap<K, SoftReference<V>> cache = new ConcurrentHashMap<K, SoftReference<V>>();
    private final LinkedBlockingQueue<K> queue = new LinkedBlockingQueue<K>();
    private final SoftReference<V> loadingHolder = new SoftReference<V>(null);
    private final Handler callback;
    private final Thread thread = new Thread() {
		public void run() {
			K key;
			while (true) {
				try {
					key = queue.take();
			    	requestAndCache(key);
			    	if (!isInterrupted()) {
				    	sendHandlerMessage(MESSAGE_CACHE_GOT, 0, 0, key);
				    	if (queue.size() == 0) sendHandlerMessage(MESSAGE_QUEUE_FINISHED, 0, 0, null);
			    	} else {
			    		clearQueue();
			    	}
				} catch (InterruptedException e) {
					e.printStackTrace();
					clearQueue();
				}
			}
		}
	};

	/**
	 * ����һ���µ������û�������
	 * @param callback	�첽���󷵻�ʱ�Ļص� Handler
	 */
    public SoftCache(Handler callback) {
    	this.callback = callback;
    	thread.setDaemon(true);
    	thread.start();
    }
    
    /**
     * �򻺴�����һ���ؼ��ֶ�Ӧ�Ľ������������ڻ����л��Ѳ����ã����� Ĭ��ֵ �����������������У�����ӻ����в��ҽ�������ء�ÿ������������ɺ󶼻���ע��� Handler ���� MESSAGE_CACHE_GOT ��Ϣ���������������ɺ󽫻���ע��� Handler ���� MESSAGE_QUEUE_FINISHED ��Ϣ�����ص�Ĭ��ֵ��ͨ����д getDefault �����ı䡣
     * @param key	����Ĺؼ���
     * @return �ؼ��ֶ�Ӧ�Ľ����Ĭ��ֵ
     */
    public V get(K key) {
    	SoftReference<V> ref = cache.get(key);
    	// ���ڻ����У�����һ���·���
    	if (ref == null) {
    		offerRequest(key);
    		return getDefault();
    	}
    	// ���ڶ�ȡ������
    	if (ref == loadingHolder) {
    		return getDefault();
    	}
    	V value = ref.get();
    	// �������棬�����Ѿ�������
    	if (value == null) {
    		offerRequest(key);
    		return getDefault();
    	}
    	// Ŀ������ڻ�����
    	return value;
    }
    
    /**
     * �����ж϶�����δ��ɵ����󡣵�ǰִ���е�������ɺ󲻻��� Handler ���� MESSAGE_CACHE_GOT ��Ϣ��ͬʱҲ������� MESSAGE_QUEUE_FINISHED ��Ϣ��
     */
    public void interrupt() {
    	thread.interrupt();
    }
    
    /**
     * �ӻ����л�ȡһ���ؼ��ֶ�Ӧ�Ľ�������ؼ����ڻ����в����ڻ��ѱ����գ����� null���˺���Ӧ�����߳��е��á�
     * @param key	Ҫ����Ĺؼ���
     * @return	�ؼ��ֶ�Ӧ�Ľ������ null
     */
    V getCache(K key) {
    	SoftReference<V> ref = cache.get(key);
    	if (ref == null) return null;
    	return ref.get();
    }

    /**
     * �������ļ�ֵ�Ի����������˺���Ӧ�����߳��е��á�
     * @param key	�ؼ���
     * @param value		�ؼ��ֶ�Ӧ�Ľ��
     * @return ����Ľ������
     */
    V putCache(K key, V value) {
    	if (value == null) return null;
    	cache.put(key, new SoftReference<V>(value));
    	return value;
    }
    
    /**
     * ����ָ���Ĺؼ��֣���������������������ؼ������ڻ����У�ֱ�ӷ��ؽ�����˺���Ӧ�����߳��е��á�
     * @param key	Ҫ����Ĺؼ���
     * @return ������
     */
    V requestAndCache(K key) {
    	V value = getCache(key);
    	// ��������ڻ��Ѿ�������
    	if (value == null) {
        	return putCache(key, request(key));
    	}
    	// Ŀ������ڻ�����
    	return value;    	
    }
    
    /**
     * �������ؼ��ּ���������С�
     * @param key	Ҫ����Ĺؼ���
     */
    private void offerRequest(K key) {
    	cache.put(key, loadingHolder);
    	queue.offer(key);
    	if (getMaxQueueLength() > 0 && queue.size() > getMaxQueueLength()) {
    		cache.remove(queue.remove());
    	}
    }
    
    /**
     * ��ԭ�����ж��������״̬��Ȼ�����������С�
     */
    private void clearQueue() {
    	while (true) {
    		K key = queue.poll();
    		if (key == null) break;
    		cache.remove(key);
    	}
    }
    
	/**
	 * ����Ϣ����������һ����Ϣ��
	 * @param what	��Ϣ����
	 * @param arg1	��Ϣ����1 (����Ϣ���Ͷ���)
	 * @param arg2	��Ϣ����2 (����Ϣ���Ͷ���)
	 * @param obj	��Ϣ���Ӷ��� (����Ϣ���Ͷ���)
	 */
	private void sendHandlerMessage(int what, int arg1, int arg2, Object obj) {
		if (null != callback) {
			Message msg = Message.obtain();
			msg.what = what;
			msg.arg1 = arg1;
			msg.arg2 = arg2;
			msg.obj = obj;
			callback.sendMessage(msg);
		}
	}
	
	/**
	 * ��д�˺����Է����Զ����Ĭ��ֵ��������Ĭ�ϵ� null��
	 * @return ��� null ��Ĭ��ֵ
	 */
	V getDefault() {
		return null;
	}
	
	/**
	 * ��д�˺����Ծ���������е���󳤶ȡ���������г������ȣ�λ�ڶ��׵Ļ����ȱ�������Ĭ�϶��г���Ϊ����(-1)��
	 * @return
	 */
	int getMaxQueueLength() {
		return -1;
	}
		
    /**
     * ���߳����壬���������д�˺�������ɻ�ȡ�����ؼ��ֵ�ֵ�����ء�
     * @param key	����Ĺؼ���
     * @return �ؼ��ֶ�Ӧ��ֵ
     */
    abstract V request(K key);
}