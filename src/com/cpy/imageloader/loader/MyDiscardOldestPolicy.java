package com.cpy.imageloader.loader;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import com.cpy.imageloader.loader.deque.LIFOLinkedBlockingDeque;

/**
 * �Զ����һ���̳߳ؾܾ������࣬��ԭ���Ļ����ϼ�����һ���ص�����������
 * �ڻص������п��Ի�ȡ�����ܵ�runnable�Լ��ȴ� ������ǰ���runnable��
 * Ȼ������Զ���Ĳ�����
 * @author cpy
 *
 */
class MyDiscardOldestPolicy implements RejectedExecutionHandler
{
	private DiscardCallback discardCallback;
	public MyDiscardOldestPolicy(DiscardCallback discardCallback) {
		this.discardCallback = discardCallback;
	}
	
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        if (!e.isShutdown()) {
        	Runnable old;
        	if(e.getQueue() instanceof LIFOLinkedBlockingDeque)
	            old = ((LIFOLinkedBlockingDeque<Runnable>)e.getQueue()).pollLast();
        	else 
        		old = e.getQueue().poll();
            //e.getQueue().add(r);
            e.execute(r);
            discardCallback.processDiscard(old, r);
        }
    }
    
    public interface DiscardCallback
    {
    	public void processDiscard(Runnable old, Runnable r);
    }
}
