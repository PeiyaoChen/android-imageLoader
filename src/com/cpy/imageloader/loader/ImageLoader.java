package com.cpy.imageloader.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

	private static ImageLoader instance = null;
	private Handler handler = new Handler(Looper.getMainLooper());

	// ���ڷ�ͼƬ��cache����cache��ʱ���ͼƬ�����ͷ�ǰ���ͼƬ��Դ
	public LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);
	// imageAddedָ�������ڽ��̳����汻���ػ�ȴ������ص�ͼƬurl,�ñ���Ϊ�˱����ظ�����
	public HashSet<String> imageAdded = new HashSet<String>();
	// ������¼��ĳ��ӦurlͼƬ���������Ҫ���µ�view
	public HashMap<String, Set<View>> urlMapViews = new HashMap<String, Set<View>>();
	// ��view�ļ���url����ʱ������ɵļ���ͼƬ��Ϊ��ʾ
	private Map<View, String> viewToUrls = Collections
			.synchronizedMap(new WeakHashMap<View, String>());

	private String path;
	private Context mContext;
	private boolean hasInitSize = false;
	private boolean hasUsed = false;

	// MyDiscardOldestPolicy���һ���ص��������󣬵�ĳ���̱߳������̶߳���ʱ����imageAdded��ȥ�����̵߳�url��
	// /**
	private DiscardCallback dcb = new DiscardCallback() {
		@Override
		public void processDiscard(Runnable head, Runnable r) {
			// TODO Auto-generated method stub
//			imageAdded.remove(((LoadFromFileOrInternet) head).url);
			imageAdded.remove(((LoadRunnable)head).url);
			Log.v("cpy", "threadfull");
		}
	};
	// **/
	// �̳߳�
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

	private ImageLoader(Context context) {
		mContext = context.getApplicationContext();
		path = mContext.getDir("", Context.MODE_PRIVATE).getParent()
				+ "/ImageCache";

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
		doLoadImage(url, view, false);
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
	 * @param flag
	 *            �Ƿ�������΢��
	 */
	public void loadImage(final String url, View view, boolean flag) {
		doLoadImage(url, view, flag);
	}

	private void doLoadImage(final String url, View view, boolean flag) {
		hasUsed = true;
		Log.v("cpy", "load image");
		// ����cache�����Ƿ����ͼƬ
		if (url == null || url.equals("") || url.equals("NULL")) {
			return;
		}
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
//		threadPool.execute(new LoadFromFileOrInternet(url, flag));
		new Thread(new LoadFromFileOrInternet(url, flag)).start();
	}

	private Drawable bitmapToDrawable(Bitmap bitmap) {
		return new BitmapDrawable(mContext.getResources(), bitmap);
	}

	/**
	 * ����url���������drawable����ͼƬ
	 * 
	 * @param url
	 *            ͼƬ��url
	 * @return drawable����ͼƬ
	 */
	public Drawable loadDrawableFromUrl(String url) {
		// ContactslistActivity.AICount++;

		URL m;
		InputStream i = null;
		try {
			m = new URL(url);
			i = (InputStream) m.getContent();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (i != null) {
			Drawable d = Drawable.createFromStream(i, "src");
			Log.v("download successful", "cpy");
			return d;
		} else {
			Log.v("download fail", "cpy");
			return null;
		}

	}

	// load the bitmap from the url
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
				Log.v("download successful", "cpy");
			} else {
				Log.v("download fail", "cpy");
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
			// for(int i = 0; urlMapViews.get(url) != null && i <
			// urlMapViews.get(url).size(); i++)
			// {
			// View view = urlMapViews.get(url).get(i);
			// if(view != null && (!isUrlOld(view, url)))
			// {
			// if(view instanceof ImageView)
			// {
			// ((ImageView) view).setImageBitmap(bitmap);
			// }
			// else
			// {
			// view.setBackgroundDrawable(bitmapToDrawable(bitmap));
			// }
			// }
			// c++;
			// }
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
			imageAdded.remove(url);
			cache.put(url, bitmap);
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
		public boolean flag = false;

		/**
		 * @param url
		 *            ��runnable��Ҫ����ͼƬ��url
		 */
		public LoadRunnable(String url) {
			this.url = url;
		}

		/**
		 * 
		 * @param url
		 *            ��runnable��Ҫ����ͼƬ��url
		 * @param flag
		 *            �Ƿ�������΢��
		 */
		public LoadRunnable(String url, boolean flag) {
			this.url = url;
			this.flag = flag;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				Log.v("thread start:" + url, "thread");
				// get the bitmap from the internet
				Bitmap bitmap = loadBitmapFromUrl(url);
				// imageAdded.remove(url);
				if (bitmap != null) {
					// cache.put(url, bitmap);
					// Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
					if (!flag)
						LocalImageHelper.storeImage(urlToFilename(url), bitmap,
								path);
					handler.post(new DisplayWaitingViews(url, bitmap));
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * �ж�ĳurl�Ƿ���ĳ��view��ǰ���µ�Ҫ����ͼƬ��url
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
	 * ��urlӳ�䵽�ļ���
	 * 
	 * @param url
	 * @return
	 */
	private String urlToFilename(String url) {
		// int index;
		// int dotindex;
		// index = url.lastIndexOf('/');
		// dotindex = url.lastIndexOf('.');
		// if(dotindex <= index)
		// {
		// int formerIndex = url.substring(0, index).lastIndexOf("/");
		// String aa = url.substring(formerIndex + 1, index) + "_" +
		// url.substring(index + 1);
		// return url.substring(formerIndex + 1, index) + "_" +
		// url.substring(index + 1);
		// }
		// String aa = url.substring(index + 1, dotindex);
		// return url.substring(index + 1, dotindex);
		return url.replace("/", "");
	}

	class UpdateSingleView implements Runnable {

		Bitmap bitmap;
		View view;

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

	class LoadFromFileOrInternet implements Runnable {

		private String url;
		private boolean flag;

		public LoadFromFileOrInternet(String url, boolean flag) {
			this.url = url;
			this.flag = flag;
		}

		@Override
		public void run() {
			synchronized (LoadFromFileOrInternet.class) {

				Bitmap bitmap = null;
				if (!flag)
					bitmap = LocalImageHelper.getLocalImage(urlToFilename(url),
							path);
				if (bitmap != null) {
					Log.v("cpy", "load from local");
					// cache.put(url, bitmap);
					handler.post(new DisplayWaitingViews(url, bitmap));
					return;
				}

				Log.v("thread submit:" + url, "thread");

				LoadRunnable loadRunnable = new LoadRunnable(url, flag);

				 threadPool.execute(loadRunnable);
				//loadRunnable.run();

			}
		}

	}
}