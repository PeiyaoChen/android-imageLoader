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
 * �����������߱���cache���߱����ļ�ϵͳ�м���ͼƬ��</br>
 * ����cache��Ĭ�ϴ�СΪ:20��ͼƬ����ʹ��ǰ�����Ե���initCacheSizeByByte(int
 * byteNum)��initCacheSizeByPictureCount(int count)�����޸ġ� �̳߳صĲ���Ĭ��Ϊ�������߳���3,
 * ����߳���8, �Ǻ����߳����ô��ʱ��15�룬���ȶ���Ϊ������10���Ƚ��ȳ����޶��С���ʹ��ǰ���ɵ���initThreadPool�����޸ġ�
 * �����޵��ȶ�����ʱ���������Ļ������л�������ɵ�����Ȼ������µ�����
 * ����ʹ�õ���ģʽ�����÷���:ImageLoader.getInstance().XXX(��������</br></br>
 * ��Ҫע����ǣ���ʹ�ø��ද̬����ListViewͼƬʱ��ListView��layout_height�������Ϊfill_parent�������Ӱ�����ܡ�
 * 
 * @author cpy
 * 
 */
public class ImageLoader {

	/**
	 * �Ƚ��ȳ����޶���
	 */
	public static final int QUEUE_TYPE_FIFO_FINITE = 0;
	/**
	 * �Ƚ��ȳ����޶���
	 */
	public static final int QUEUE_TYPE_FIFO_INFINITE = 1;
	/**
	 * ����ȳ����޶���
	 */
	public static final int QUEUE_TYPE_LIFO_FINITE = 2;
	/**
	 * ����ȳ����޶���
	 */
	public static final int QUEUE_TYPE_LIFO_INFINITE = 3;

	/**
	 * ����instance
	 */
	private static ImageLoader instance = null;
	private Handler handler = new Handler(Looper.getMainLooper());
	/**
	 * ���ڷ�ͼƬ��cache����cache��ʱ���ͼƬ�����ͷ�ǰ���ͼƬ��Դ
	 */
	private LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);
	/**
	 * imageAddedָ�������ڽ��̳����汻���ػ�ȴ������ص�ͼƬurl,�ñ���Ϊ�˱����ظ�����
	 */
	private HashSet<String> imageAdded = new HashSet<String>();
	/**
	 * ������¼��ĳ��ӦurlͼƬ���������Ҫ���µ�view
	 */
	private HashMap<String, Set<View>> urlMapViews = new HashMap<String, Set<View>>();
	/**
	 * ��view�ļ���url����ʱ������ɵļ���ͼƬ��Ϊ��ʾ
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
	 * MyDiscardOldestPolicy���һ���ص��������󣬵�ĳ���̱߳������̶߳���ʱ����imageAdded��ȥ�����̵߳�url��
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
	 * �̳߳�
	 */
	public ThreadPoolExecutor threadPool = new ThreadPoolExecutor(3, 8, 15,
			TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10),
			new MyDiscardOldestPolicy(dcb));

	/**
	 * ��ȡʵ��
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
	 * ���캯��
	 * 
	 * @param context
	 */
	private ImageLoader(Context context) {
		mContext = context.getApplicationContext();
	}

	/**
	 * ��ByteΪ��λ����Cache��С,�˷���������ʹ��ImageLoaderǰ����
	 * 
	 * @param byteNum
	 * @return ���óɹ�����true, ���ɹ�����false
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
	 * ��ͼƬ����Ϊ��λ����Cache��С,�˷���������ʹ��ImageLoaderǰ����
	 * 
	 * @param count
	 * @return ���óɹ�����true, ���ɹ�����false
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
	 * ���ü���ͼƬ���̳߳أ��÷���Ҫ�ڼ��ص�һ����Ƭǰʹ�ã�����������Ч��
	 * 
	 * @param corePoolSize
	 *            �����߳���
	 * @param maximumPoolSize
	 *            ����߳���
	 * @param keepAliveTime
	 *            �Ǻ����߳����ô��ʱ��
	 * @param QueueType
	 *            �̶߳�������
	 * @param queueCapacity
	 *            �̶߳������������߳�����Ϊ���޶�������ʱ��������
	 * @return trueΪ���óɹ���falseΪ���ò��ɹ���
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
	 * �����磨������һ���̣߳��򱾵��ļ����ڴ�cache�м���ͼƬ�����Ȼ��cache��Ѱ�ң���û�У��ᵽ�ļ�ϵͳ���ң�������û�У���ᵽ������ȡ��
	 * ������ȡ�����󣬻�浽����cache���ļ�ϵͳ�С���cache��ʱ��������ͼƬ�����ͷ����ȼ����ͼƬ��
	 * 
	 * @param url
	 *            ��Ҫ����ͼƬ��url
	 * @param view
	 *            ����������ͼƬҪ�����õ���view������ImageView,�������Ϊsrc;������������view,
	 *            �������Ϊbackground
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
		// ����cache�����Ƿ����ͼƬ
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
		// ����cache�����Ƿ����ͼƬ
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
	 * ����url���������bitmap����ͼƬ
	 * 
	 * @param url
	 *            ͼƬ��url
	 * @return bitmap����ͼƬ
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
	 * ����ui
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
	 * ����ȡͼƬ���߳�
	 * 
	 * @author cpy
	 * 
	 */
	class LoadRunnable implements Runnable {
		public String url; // ��Ҫ����ͼƬ��url

		/**
		 * @param url
		 *            ��runnable��Ҫ����ͼƬ��url
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
	 * �ж�ĳurl�Ƿ���ĳ��view��ǰ���µ�Ҫ����ͼƬ��url <br/>
	 * ��ҪΪListview���
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