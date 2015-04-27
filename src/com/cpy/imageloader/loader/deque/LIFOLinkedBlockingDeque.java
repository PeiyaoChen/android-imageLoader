package com.cpy.imageloader.loader.deque;


/**
 * {@link LinkedBlockingDeque} using LIFO algorithm
 */
public class LIFOLinkedBlockingDeque<T> extends LinkedBlockingDeque<T> {

	private static final long serialVersionUID = -4114786347960826192L;
	private Integer mMaxCount = -1;
	
	public LIFOLinkedBlockingDeque(int maxCount) {
		mMaxCount = maxCount;
	}
	
	public LIFOLinkedBlockingDeque() {
	}

	@Override
	public boolean offer(T e) {
		if(mMaxCount != null && size() >= mMaxCount)
			return false;
		return super.offerFirst(e);
	}

	@Override
	public T remove() {
		return super.removeFirst();
	}
}