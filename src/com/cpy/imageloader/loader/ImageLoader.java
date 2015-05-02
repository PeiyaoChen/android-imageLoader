/*
 * Written by Peiyao Chen
 * Peiyao Chen is a master student in Sen Yat-sen University
 * Eamil: wincpy@gmail.com
 */
package com.cpy.imageloader.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.cpy.imageloader.R;
import com.cpy.imageloader.http.HttpHelper;
import com.cpy.imageloader.loader.MyDiscardOldestPolicy.DiscardCallback;
import com.cpy.imageloader.loader.deque.LIFOLinkedBlockingDeque;

/**
 * This class is used for loading image from server, local file system or memory cache </br></br>
 * Default site of memory cache is: 20 pictures. Before usage, developer can call {@link #initCacheSizeByPictureCount(int)} or 
 * {@link #initCacheSizeByByte(int)}  to change the size. </br></br>
 * 
 * Default thread pool setting is: core thread number: 3, max thread number: 8, non-core thread alive time: 15s.
 * Before usage, developer can call {@link #initThreadPool(int, int, int, int, int)} to change the setting</br> </br>
 * 
 * When the waiting queue is full and a new thread is enqueued, the oldest thread will be removed and the new one will 
 * be dequeued. </br></br>
 * 
 * This class use singleton pattern. Usage: ImageLoader.getInstance().XXX(method name) </br></br>
 * 
 * Note: When using this class to load image in ListView's items, please set ListView's layout_height fill_parent,
 * otherwise performance will be not good.
 *  
 * @author Peiyao Chen
 *
 */
public class ImageLoader {

	/**
	 * Finite FIFO queue
	 */
	public static final int QUEUE_TYPE_FIFO_FINITE = 0;
	/**
	 * infinite FIFO queue
	 */
	public static final int QUEUE_TYPE_FIFO_INFINITE = 1;
	/**
	 * LIFO finite queue
	 */
	public static final int QUEUE_TYPE_LIFO_FINITE = 2;
	/**
	 * LIFO infinite queue
	 */
	public static final int QUEUE_TYPE_LIFO_INFINITE = 3;

	/**
	 * singleton instance
	 */
	private static ImageLoader instance = null;
	private Handler handler = new Handler(Looper.getMainLooper());
	/**
	 * Memory cache for bitmaps
	 */
	private LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);
	/**
	 * Used to record the url of bitmaps which are being loaded or waiting in the queue,
	 * It is used to avoid duplicate requesting
	 */
	private HashSet<String> imageAdded = new HashSet<String>();
	/**
	 * record views that are waiting for a specific image
	 */
	private HashMap<String, Set<View>> urlMapViews = new HashMap<String, Set<View>>();
	/**
	 * map from view and its requesting image's url. It is used to avoid updating views with old 
	 * requesting picture
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
	 * Callback which will be invoked when the queue is full and a thread is added.
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
	 * Thread pool
	 */
	public ThreadPoolExecutor threadPool = new ThreadPoolExecutor(3, 8, 15,
			TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10),
			new MyDiscardOldestPolicy(dcb));

	/**
	 * get instance
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
	 * Constructor
	 * 
	 * @param context
	 */
	private ImageLoader(Context context) {
		mContext = context.getApplicationContext();
	}

	/**
	 * Set the memory cache size based on byte. It should be invoked before usage.
	 * 
	 * @param byteNum
	 * @return If setting succeed, return true. Otherwise, return false
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
	 * Set the memory cache size based on picture number. It should be invoked before usage.
	 * 
	 * @param count
	 * @return If setting succeed, return true. Otherwise, return false
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
	 * Set the thread pool configuration, It should be invoked before usage.
	 * 
	 * @param corePoolSize
	 *            number of core thread
	 * @param maximumPoolSize
	 *            maximum number of thread
	 * @param keepAliveTime
	 *            alive time of non-core thread
	 * @param QueueType
	 *            queue type
	 * @param queueCapacity
	 *            queue size. (if queue type is a finite queue)
	 * @return If setting succeed, return true. Otherwise, return false
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
	 * load image from server (launch a thread to do that) or local file system or memory cache.</br>
	 * After image is pulled from server, it will be saved in local file system and memory cache.
	 * @param url url of image to be loaded
	 * @param view After image is loaded, this view will be updated. If view is ImageView, it will set its image property.
	 * 			Otherwise, it will set its background property
	 */
	public void loadImage(final String url, View view) {
		doLoadImage(url, view, null, null);
	}
	
	public void loadImage(final String url, View view, Integer width, Integer height) {
		doLoadImage(url, view, width, height);
	}
	
	/**
	 * load image from server (launch a thread to do that) or local file system or memory cache.</br>
	 * After image is pulled from server, it will be saved in local file system and memory cache.
	 * @param url url of image to be loaded
	 * @param observer After image is loaded, this callback's function will invoked
	 */
	public void loadImage(final String url, GetBitmapObserver observer) {
		doLoadImage(url, observer, null, null);
	}
	
	public void loadImage(final String url, GetBitmapObserver observer, Integer width, Integer height) {
		doLoadImage(url, observer, width, height);
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
	private void doLoadImage(final String url, View view, Integer width, Integer height) {
		hasUsed = true;
		Log.v("cpy", "load image");
		if (url == null || url.equals("") || url.equals("NULL")) {
			return;
		}

		viewToUrls.put(view, url);
		if (cache.get(getCacheKey(url, width, height)) != null) {
			Log.v("cpy", "get from cache");
			if (view instanceof ImageView) {
				((ImageView) view).setImageBitmap(cache.get(getCacheKey(url, width, height)));
			} else {
				view.setBackgroundDrawable(bitmapToDrawable(cache.get(getCacheKey(url, width, height))));
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
		new Thread(new LoadFromFileOrRemote(url, width, height)).start();
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
	private void doLoadImage(final String url, GetBitmapObserver observer, Integer width, Integer height) {
		hasUsed = true;
		Log.v("cpy", "load image");
		if (url == null || url.equals("") || url.equals("NULL")) {
			return;
		}
		// Whether the image exists in cache
		if (cache.get(getCacheKey(url, width, height)) != null) {
			Log.v("cpy", "get from cache");
			if (observer != null)
				observer.onGetBitmap(cache.get(getCacheKey(url, width, height)));
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
		new Thread(new LoadFromFileOrRemote(url, width, height)).start();

	}
	
	private String getCacheKey(String url, Integer width, Integer height) {
		if(width != null && height != null && width > 0 && height > 0) {
			Log.v("getCacheKey", "key with heihgt and width");
			return url + width + "_" + height;
		}
		return url;
	}

	private Drawable bitmapToDrawable(Bitmap bitmap) {
		return new BitmapDrawable(mContext.getResources(), bitmap);
	}

	/**
	 * load bitmap from server
	 * 
	 * @param url image url
	 * @return 
	 */
	private Bitmap loadBitmapFromUrl(String url, Integer width, Integer height) {
		URL realurl = null;
		Bitmap bitmap = null;

		try {
			realurl = new URL(url);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			InputStream is = null;
			try {
				is = new HttpHelper().getInputStream(url).second;
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(is != null) {
				String path = localPathMapper.getLocalPath(url);
				storeImage(is, path);
				bitmap = LocalImageHelper.getLocalImage(path, width, height);
				if(bitmap != null) {
					loadSuccessCount++;
					Log.v("threadSucess", "load success" + loadSuccessCount);
				}
			}
			if(is != null)
				is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}
	
	private void storeImage(InputStream is, String path) {
		File file = new File(path);
		if(!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int byteRead = 0;
			while((byteRead = is.read(buffer)) != -1) {
				fos.write(buffer, 0, byteRead);
			}
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * update views
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
	 * Runnable to load image from server
	 * 
	 * @author cpy
	 * 
	 */
	class LoadRunnable implements Runnable {
		public String url;
		public Integer width;
		public Integer height;

		public LoadRunnable(String url, Integer width, Integer height) {
			this.url = url;
			this.width = width;
			this.height = height;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				loadCount++;
				Log.v("threadStart", "thread " + loadCount + ":" + url);
				// get the bitmap from the internet
				Bitmap bitmap = loadBitmapFromUrl(url, width, height);
				// imageAdded.remove(url);
				if (bitmap != null) {
					cache.put(getCacheKey(url, width, height), bitmap);
//					LocalImageHelper.storeImage(
//							localPathMapper.getLocalPath(url), bitmap);
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
	 * Whether the url is old for the view <br/>
	 * Mainly designed for listview
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

	/**
	 * default local path mapping
	 * @param url
	 * @return
	 */
	private String urlToLocalPath(String url) {
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
		private Integer height;
		private Integer width;

		public LoadFromFileOrRemote(String url, Integer height, Integer width) {
			this.url = url;
			this.height = height;
			this.width = width;
		}

		@Override
		public void run() {
			synchronized (LoadFromFileOrRemote.class) {
				Bitmap bitmap = null;
				bitmap = LocalImageHelper.getLocalImage(localPathMapper
						.getLocalPath(url), width, height);
				if (bitmap != null) {
					Log.v("cpy", "load from local");
					cache.put(getCacheKey(url, width, height), bitmap);
					handler.post(new DisplayWaitingViews(url, bitmap));
					notifyObservers(url, bitmap);
					imageAdded.remove(url);
					return;
				}
				Log.v("thread submit:", "thread " + url);
				LoadRunnable loadRunnable = new LoadRunnable(url, height, width);
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

	/**
	 * Interface definition of callback which is invoked after loading image finished
	 * @author cpy
	 *
	 */
	public interface GetBitmapObserver {
		public void onGetBitmap(Bitmap bitmap);
	}

	public interface LocalFilePathMapper {
		public String getLocalPath(String url);
	}
	
	/**
	 * set local file mapper
	 * @param mapper
	 */
	public void setLocalFilePathMapper(LocalFilePathMapper mapper) {
		this.localPathMapper = mapper;
	}
	
	/**
	 * set keystore of self signed certificates
	 * @param keystore keystore file of self signed certificates inputstream
	 * @param password password of the keystore
	 * @throws CertificateException 
	 */
	public static void setLocalHttpsTrustKeyStore(InputStream keystore, String password) throws CertificateException {
		KeyStore trusted;
		try {
			trusted = KeyStore.getInstance("BKS");
	        trusted.load(keystore, password.toCharArray());
	        HttpHelper.setHttpsLocalKeyStore(trusted);
	        keystore.close();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * set keystore of self signed certificates
	 * @param keystorePath path of the keystore file
	 * @param password password password of the keystore
	 * @throws FileNotFoundException
	 * @throws CertificateException 
	 */
	public static void setLocalHttpsTrustKeyStore(String keystorePath, String password) throws FileNotFoundException, CertificateException {
			File file = new File(keystorePath);
			FileInputStream is = new FileInputStream(file);
			setLocalHttpsTrustKeyStore(is, password);
	}
	
	/**
	 * disable host name verification on TLS handshake</br>
	 * Default setting is enable
	 */
	public static void disableHostnameVerification() {
		HttpHelper.disableHostNameVerification();
	}
	
	/**
	 * enable host name verification on TLS handshake</br>
	 * Default setting is enable
	 */
	public static void enbleHostnameVerification() {
		HttpHelper.enableHostNameVerification();
	}
	
}