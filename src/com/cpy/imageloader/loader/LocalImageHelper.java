package com.cpy.imageloader.loader;

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * �����װ�˱���ͼƬ�Ĵ�ȡ�ӿ�
 */
public class LocalImageHelper {
	
	
	/**
	 * ��bitmap��ʽ����һ�ű���ͼƬ
	 * @param imgName Ҫ������ͼƬ�ļ���
	 * @return ��Ҫ��bitmap
	 */
	public static Bitmap getLocalImage(String path) {
		Bitmap bitmap = null;
		File file = new File(path);
		if(file.exists())
			bitmap = BitmapFactory.decodeFile(path);
		return bitmap;
	}
	
	/**
	 * ��һ��ͼƬ��bitmap��ʽ���뱾��
	 * @param imgName ͼƬ�ļ���
	 * @param img ͼƬ��bitmap��Դ
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
	 * ��path�����ڣ��򴴽�
	 * @param path
	 */
	private static void createImgParentFile(String path) {
		File file = new File(path);
		if(!file.getParentFile().exists())
			file.getParentFile().mkdirs();
	}
	
}
