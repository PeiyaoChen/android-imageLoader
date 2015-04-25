package com.cpy.imageloader.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.cpy.imageloader.loader.MyDiscardOldestPolicy.DiscardCallback;
import com.cpy.imageloader.loader.deque.LIFOLinkedBlockingDeque;

/**
 * 该类从网络或者本地cache或者本地文件系统中加载图片。</br>
 * 本地cache的默认大小为:20张图片。在使用前，可以调用initCacheSizeByByte(int
 * byteNum)或initCacheSizeByPictureCount(int count)进行修改。 线程池的参数默认为：核心线程数3,
 * 最大线程数8, 非核心线程闲置存活时间15秒，调度队列为：容量10的先进先出有限队列。在使用前，可调用initThreadPool进行修改。
 * 当有限调度队列满时添加新任务的话，队列会舍弃最旧的任务，然后加入新的任务。
 * 该类使用单例模式。调用方法:ImageLoader.getInstance().XXX(方法名）</br></br>
 * 需要注意的是：当使用该类动态加载ListView图片时，ListView的layout_height最好设置为fill_parent，否则会影响性能。
 * 
 * @author cpy
 * 
 */
public class ImageLoader {

	/**
	 * 先进先出有限队列
	 */
	public static final int QUEUE_TYPE_FIFO_FINITE = 0;
	/**
	 * 先进先出无限队列
	 */
	public static final int QUEUE_TYPE_FIFO_INFINITE = 1;
	/**
	 * 后进先出有限队列
	 */
	public static final int QUEUE_TYPE_LIFO_FINITE = 2;
	/**
	 * 后进先出无限队列
	 */
	public static final int QUEUE_TYPE_LIFO_INFINITE = 3;

	/**
	 * 单例instance
	 */
	private static ImageLoader instance = null;
	private Handler handler = new Handler(Looper.getMainLooper());
	/**
	 * 用于放图片的cache，当cache满时添加图片，会释放前面的图片资源
	 */
	private LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);
	/**
	 * imageAdded指的是正在进程池里面被加载或等待被加载的图片url,该变量为了避免重复加载
	 */
	private HashSet<String> imageAdded = new HashSet<String>();
	/**
	 * 用来记录下某对应url图片加载完后需要更新的view
	 */
	private HashMap<String, Set<View>> urlMapViews = new HashMap<String, Set<View>>();
	/**
	 * 当view的加载url更新时，避免旧的加载图片作为显示
	 */
	private Map<View, String> viewToUrls = Collections
			.synchronizedMap(new WeakHashMap<View, String>());

	/**
	 * record observer set that waiting for a specific image loading finished
	 */
	private Map<String, Set<GetBitmapObserver>> urlMapObservers = new HashMap<String, Set<GetBitmapObserver>>();

	private LocalFilePathMapper localPathMapper = new LocalFilePathMapper() {
		@Override
		public String getLocalPath(String url) {
			return urlToLocalPath(url);
		}
	};

	private String path;
	private Context mContext;
	private boolean hasInitSize = false;
	private boolean hasUsed = false;

	// just for test
	private int loadCount = 0;
	private int loadSuccessCount = 0;
	private int loadThrowCount = 0;
	private int loadFailedCount = 0;

	/**
	 * MyDiscardOldestPolicy类的一个回调函数对象，当某个线程被挤出线程队列时，在imageAdded中去除该线程的url。
	 */
	private DiscardCallback dcb = new DiscardCallback() {
		@Override
		public void processDiscard(Runnable head, Runnable r) {
			// TODO Auto-generated method stub
			// imageAdded.remove(((LoadFromFileOrInternet) head).url);
			imageAdded.remove(((LoadRunnable) head).url);
			loadThrowCount++;
			Log.v("threadThrow", loadThrowCount + " "
					+ ((LoadRunnable) head).url);
		}
	};
	/**
	 * 线程池
	 */
	public ThreadPoolExecutor threadPool = new ThreadPoolExecutor(3, 8, 15,
			TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10),
			new MyDiscardOldestPolicy(dcb));

	/**
	 * 获取实例
	 * 
	 * @return
	 */
	public static ImageLoader getInstance(Context context) {
		if (instance == null) {
			synchronized (ImageLoader.class) {
				if (instance == null) {
					instance = new ImageLoader(context);
					Log.v("init", "reconstructor");
				}
			}
		}
		return instance;
	}

	/**
	 * 构造函数
	 * 
	 * @param context
	 */
	private ImageLoader(Context context) {
		mContext = context.getApplicationContext();
	}

	/**
	 * 以Byte为单位设置Cache大小,此方法必须在使用ImageLoader前设置
	 * 
	 * @param byteNum
	 * @return 设置成功返回true, 不成功返回false
	 */
	public boolean initCacheSizeByByte(int byteNum) {
		if (hasInitSize || (cache != null && cache.size() > 0))
			return false;
		else {
			cache = new LruCache<String, Bitmap>(byteNum) {
				protected int sizeOf(String key, Bitmap value) {
					return value.getRowBytes() * value.getHeight();
				}
			};
			hasInitSize = true;
			return true;
		}
	}

	/**
	 * 以图片张数为单位设置Cache大小,此方法必须在使用ImageLoader前设置
	 * 
	 * @param count
	 * @return 设置成功返回true, 不成功返回false
	 */
	public boolean initCacheSizeByPictureCount(int count) {
		if (hasInitSize || (cache != null && cache.size() > 0))
			return false;
		else {
			cache = new LruCache<String, Bitmap>(count);
			hasInitSize = true;
			return true;
		}
	}

	/**
	 * 设置加载图片的线程池，该方法要在加载第一张照片前使用，否则设置无效。
	 * 
	 * @param corePoolSize
	 *            核心线程数
	 * @param maximumPoolSize
	 *            最大线程数
	 * @param keepAliveTime
	 *            非核心线程闲置存活时间
	 * @param QueueType
	 *            线程队列类型
	 * @param queueCapacity
	 *            线程队列容量，当线程类型为有限队列类型时才起作用
	 * @return true为设置成功，false为设置不成功。
	 */
	public boolean initThreadPool(int corePoolSize, int maximumPoolSize,
			int keepAliveTime, int QueueType, int queueCapacity) {
		if (hasUsed)
			return false;
		if (QueueType == QUEUE_TYPE_FIFO_FINITE) {
			threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
					keepAliveTime, TimeUnit.SECONDS,
					new ArrayBlockingQueue<Runnable>(queueCapacity),
					new MyDiscardOldestPolicy(dcb));
		} else if (QueueType == QUEUE_TYPE_FIFO_INFINITE) {
			threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
					keepAliveTime, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>());
		} else if (QueueType == QUEUE_TYPE_LIFO_FINITE) {
			threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
					keepAliveTime, TimeUnit.SECONDS,
					new LIFOLinkedBlockingDeque<Runnable>(queueCapacity),
					new MyDiscardOldestPolicy(dcb));
		} else if (QueueType == QUEUE_TYPE_LIFO_INFINITE) {
			threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
					keepAliveTime, TimeUnit.SECONDS,
					new LIFOLinkedBlockingDeque<Runnable>());
		}
		return true;
	}

	/**
	 * 从网络（会启动一个线程）或本地文件或内存cache中加载图片。首先会从cache中寻找，若没有，会到文件系统中找，若还是没有，则会到网络中取。
	 * 网络中取下来后，会存到本地cache和文件系统中。当cache满时，加入新图片，会释放最先加入的图片。
	 * 
	 * @param url
	 *            需要加载图片的url
	 * @param view
	 *            加载下来的图片要被设置到的view。若是ImageView,则会设置为src;若是其它类型view,
	 *            则会设置为background
	 */
	public void loadImage(final String url, View view) {
		doLoadImage(url, view);
	}

	/**
	 * load image to set background or image of the view <br/>
	 * If view is ImageView, it will set the its image property, otherwise set
	 * its background property
	 * 
	 * @param url
	 *            image url
	 * @param view
	 *            view to update
	 */
	private void doLoadImage(final String url, View view) {
		hasUsed = true;
		Log.v("cpy", "load image");
		if (url == null || url.equals("") || url.equals("NULL")) {
			return;
		}
		// 查找cache里面是否存在图片
		viewToUrls.put(view, url);
		if (cache.get(url) != null) {
			Log.v("cpy", "get from cache");
			if (view instanceof ImageView) {
				((ImageView) view).setImageBitmap(cache.get(url));
			} else {
				view.setBackgroundDrawable(bitmapToDrawable(cache.get(url)));
			}
			return;
		}
		if (urlMapViews.get(url) != null) {
			urlMapViews.get(url).add(view);
		} else {
			Set<View> newSet = Collections
					.newSetFromMap(new WeakHashMap<View, Boolean>());
			newSet.add(view);
			urlMapViews.put(url, newSet);
		}
		if (!imageAdded.add(url)) {
			return;
		}
		new Thread(new LoadFromFileOrRemote(url)).start();
	}

	/**
	 * load image and then invoke callback
	 * 
	 * @param url
	 *            image url
	 * @param observer
	 *            callback interface whose callback method will be invoked after
	 *            loading finished
	 */
	private void doLoadImage(final String url, GetBitmapObserver observer) {
		hasUsed = true;
		Log.v("cpy", "load image");
		if (url == null || url.equals("") || url.equals("NULL")) {
			return;
		}
		// 查找cache里面是否存在图片
		if (cache.get(url) != null) {
			Log.v("cpy", "get from cache");
			if (observer != null)
				observer.onGetBitmap(cache.get(url));
			return;
		}

		if (urlMapObservers.get(url) != null) {
			urlMapObservers.get(url).add(observer);
		} else {
			Set<GetBitmapObserver> newSet = Collections
					.newSetFromMap(new WeakHashMap<GetBitmapObserver, Boolean>());
			newSet.add(observer);
			urlMapObservers.put(url, newSet);
		}
		if (!imageAdded.add(url)) {
			return;
		}
		new Thread(new LoadFromFileOrRemote(url)).start();

	}

	private Drawable bitmapToDrawable(Bitmap bitmap) {
		return new BitmapDrawable(mContext.getResources(), bitmap);
	}

	/**
	 * 根据url从网络加载bitmap类型图片
	 * 
	 * @param url
	 *            图片的url
	 * @return bitmap类型图片
	 */
	public Bitmap loadBitmapFromUrl(String url) {
		URL realurl = null;
		Bitmap bitmap = null;

		try {
			realurl = new URL(url);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			HttpURLConnection connection = (HttpURLConnection) realurl
					.openConnection();
			connection.setDoInput(true);
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(30000);
			connection.setInstanceFollowRedirects(true);
			connection.connect();

			InputStream is = connection.getInputStream();
			bitmap = BitmapFactory.decodeStream(is);

			if (bitmap != null) {
				loadSuccessCount++;
				Log.v("threadSucess", "load success" + loadSuccessCount);
			}
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * 更新ui
	 * 
	 * @author cpy
	 * 
	 */
	class DisplayWaitingViews implements Runnable {
		String url;
		Bitmap bitmap;

		public DisplayWaitingViews() {
			// TODO Auto-generated constructor stub
		}

		DisplayWaitingViews(String url, Bitmap bitmap) {
			this.url = url;
			this.bitmap = bitmap;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.v("cpy", "display " + url);
			int c = 0;
			if (urlMapViews.get(url) != null) {
				Iterator iterator = urlMapViews.get(url).iterator();
				while (iterator.hasNext()) {
					View view = (View) iterator.next();
					if (view != null && (!isUrlOld(view, url))) {
						if (view instanceof ImageView) {
							((ImageView) view).setImageBitmap(bitmap);
						} else {
							view.setBackgroundDrawable(bitmapToDrawable(bitmap));
						}
					}
				}
			}
			Log.v("cpy", "dis count " + c);
			urlMapViews.remove(url);
		}
	}

	/**
	 * 网络取图片的线程
	 * 
	 * @author cpy
	 * 
	 */
	class LoadRunnable implements Runnable {
		public String url; // 需要加载图片的url

		/**
		 * @param url
		 *            该runnable需要加载图片的url
		 */
		public LoadRunnable(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				loadCount++;
				Log.v("threadStart", "thread " + loadCount + ":" + url);
				// get the bitmap from the internet
				Bitmap bitmap = loadBitmapFromUrl(url);
				// imageAdded.remove(url);
				if (bitmap != null) {
					// cache.put(url, bitmap);
					// Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
					cache.put(url, bitmap);
					LocalImageHelper.storeImage(
							localPathMapper.getLocalPath(url), bitmap);
					handler.post(new DisplayWaitingViews(url, bitmap));
				} else {
					loadFailedCount++;
					Log.v("threadFailed", "load failed" + loadFailedCount + ":"
							+ url);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				imageAdded.remove(url);
			}
		}
	}

	/**
	 * 判断某url是否不是某个view当前最新的要加载图片的url <br/>
	 * 主要为Listview设计
	 * 
	 * @param view
	 * @param url
	 * @return
	 */
	private boolean isUrlOld(View view, String url) {
		String tag = viewToUrls.get(view);
		if (tag == null || !tag.equals(url))
			return true;
		return false;
	}

	private String urlToLocalPath(String url) {
		// int index = url.lastIndexOf("\\/");
		// String fileName = url.substring(index+1);
		// index = fileName.lastIndexOf(".");
		// String postfix = "";
		// if(index != -1)
		// postfix = fileName.substring(index);
		return mContext.getDir("", Context.MODE_PRIVATE).getParent()
				+ "/ImageCache/" + url.replace("/", "");
	}

	/**
	 * update view Runnable
	 * 
	 * @author cpy
	 *
	 */
	class UpdateSingleView implements Runnable {
		private Bitmap bitmap;
		private View view;

		public UpdateSingleView(Bitmap bitmap, View view) {
			this.bitmap = bitmap;
			this.view = view;
		}

		@Override
		public void run() {
			if (view instanceof ImageView) {
				((ImageView) view).setImageBitmap(bitmap);
			} else {
				view.setBackgroundDrawable(bitmapToDrawable(bitmap));
			}
		}

	}

	/**
	 * load image from local or remote
	 * 
	 * @author cpy
	 *
	 */
	class LoadFromFileOrRemote implements Runnable {
		private String url;

		public LoadFromFileOrRemote(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			synchronized (LoadFromFileOrRemote.class) {
				Bitmap bitmap = null;
				bitmap = LocalImageHelper.getLocalImage(localPathMapper
						.getLocalPath(url));
				if (bitmap != null) {
					Log.v("cpy", "load from local");
					cache.put(url, bitmap);
					handler.post(new DisplayWaitingViews(url, bitmap));
					notifyObservers(url, bitmap);
					imageAdded.remove(url);
					return;
				}
				Log.v("thread submit:", "thread " + url);
				LoadRunnable loadRunnable = new LoadRunnable(url);
				threadPool.execute(loadRunnable);
			}
		}

	}

	private void notifyObservers(String url, Bitmap bitmap) {
		if (urlMapObservers.get(url) != null) {
			for (GetBitmapObserver observer : urlMapObservers.get(url)) {
				observer.onGetBitmap(bitmap);
			}
			urlMapObservers.remove(url);
		}
	}

	public interface GetBitmapObserver {
		public void onGetBitmap(Bitmap bitmap);
	}

	public interface LocalFilePathMapper {
		public String getLocalPath(String url);
	}
}