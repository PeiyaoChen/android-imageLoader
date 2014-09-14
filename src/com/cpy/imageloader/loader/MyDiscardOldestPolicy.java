package com.cpy.imageloader.loader;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import com.cpy.imageloader.loader.deque.LIFOLinkedBlockingDeque;

/**
 * 自定义的一个线程池拒绝策略类，在原来的基础上加入了一个回调函数变量，
 * 在回调函数中可以获取到被拒的runnable以及等待 队列最前面的runnable，
 * 然后进行自定义的操作。
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
