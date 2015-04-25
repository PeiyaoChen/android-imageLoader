package com.cpy.imageloader.loader;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import com.cpy.imageloader.loader.deque.LIFOLinkedBlockingDeque;

/**
 * custom thread pool reject policy 
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
        		old = e.getQueue().poll();	//dequeue the oldest one
            e.execute(r);					//enqueue the new one
            discardCallback.processDiscard(old, r);
        }
    }
    
    /**
	 * Callback which will be invoked when the queue is full and a thread is added.
	 * 
     * @author cpy
     */
    public interface DiscardCallback
    {
    	public void processDiscard(Runnable old, Runnable r);
    }
}
