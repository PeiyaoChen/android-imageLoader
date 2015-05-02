package com.cpy.imageloader.loader;

import java.io.File;
import java.io.FileOutputStream;

import android.R.integer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;

/**
 * This class provide methods to get image or store image in local file system
 * @author cpy
 *
 */
public class LocalImageHelper {
	
	/**
	 * Get image from local
	 * @param path image's path
	 * @return
	 */
	public static Bitmap getLocalImage(String path, Integer width, Integer height) {
		Bitmap bitmap = null;
		File file = new File(path);
		if(file.exists()) {
			bitmap = BitmapFactory.decodeFile(path);
			if(bitmap != null && width != null && height != null && width > 0 && height > 0)  {
				float wScale = (float)width / (float)bitmap.getWidth();
				if((int) (wScale * bitmap.getHeight()) <= height) {
					if((int)wScale * bitmap.getHeight() <= 0 || width <= 0) {
						int a = 0; 
						a++;
					}
					bitmap = Bitmap.createScaledBitmap(bitmap, width, (int)(wScale * bitmap.getHeight()), false);
				}
				else {
					float hScale = (float)height / (float)bitmap.getHeight();
					bitmap = Bitmap.createScaledBitmap(bitmap, (int)(hScale * bitmap.getWidth()), height, false);
				}
			}
		}
		return bitmap;
	}
	
	
	/**
	 * Store image in local (Save as JPEG) (Not used in current version) 
	 * @param filePath file path
	 * @param img image to be saved
	 */
	public static void storeImage(final String filePath, Bitmap img) {
		File file = new File(filePath);
		createImgParentFile(filePath);
		try {
			FileOutputStream outputStream = new FileOutputStream(file);
			if(img.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
				outputStream.flush();
				outputStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * create path's parent directories
	 * @param path
	 */
	private static void createImgParentFile(String path) {
		File file = new File(path);
		if(!file.getParentFile().exists())
			file.getParentFile().mkdirs();
	}
	
}
