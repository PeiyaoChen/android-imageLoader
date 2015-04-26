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
	public static Bitmap getLocalImage(String path) {
		Bitmap bitmap = null;
		File file = new File(path);
		if(file.exists())
			bitmap = BitmapFactory.decodeFile(path);
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
