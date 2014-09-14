android-imageLoader
===================

a lib to download image through http and put it to your view

FEATURES
===================

* Images are downloaded and saved to cache via a pool of background threads.
* The image will be identified by the url, if the image of the same url exists in the cache(memory or file system), it will be fetch from the cache instead the internet.
* The image will be set to the percific view's backround or image property after downloaded.
* Implements 4 kinds of thread queue:
    1. Finite FIFO
    2. Infinite FIFO
    3. Finite LIFO
    4. Infinite LIFO

USAGE
===================
    //init cache size(options)
    if(ImageLoader.getInstance(this).initCacheSizeByByte(1024 * 1024 * 20)) {
      Log.v("init", "true");
    }
    else {
      Log.v("init", "false");
    }
    //init thread pool(potions)
    ImageLoader.getInstance(this).initThreadPool(1, 3, 7, 
      ImageLoader.QUEUE_TYPE_LIFO_FINITE, 10);
    
    ....
    
    //init an ImageView
    ImageView imageView = (ImageView)view.findViewById(R.id.imageview);
    //set the url
    String url = "http://img03.taobaocdn.com/tps/i3/T1lh1IXClcXXajoXZd-205-130.jpg"
    //load the image to the ImageView
    ImageLoader.getInstance(getApplicationContext()).loadImage(url, imageView);
    
