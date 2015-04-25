package com.cpy.imageloader.loader;

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * 该类封装了本地图片的存取接口
 */
public class LocalImageHelper {
	
	
	/**
	 * 以bitmap方式读出一张本地图片
	 * @param imgName 要读出的图片文件名
	 * @return 所要的bitmap
	 */
	public static Bitmap getLocalImage(String path) {
		Bitmap bitmap = null;
		File file = new File(path);
		if(file.exists())
			bitmap = BitmapFactory.decodeFile(path);
		return bitmap;
	}
	
	/**
	 * 把一张图片以bitmap形式存入本地
	 * @param imgName 图片文件名
	 * @param img 图片的bitmap资源
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
	 * 若path不存在，则创建
	 * @param path
	 */
	private static void createImgParentFile(String path) {
		File file = new File(path);
		if(!file.getParentFile().exists())
			file.getParentFile().mkdirs();
	}
	
}
