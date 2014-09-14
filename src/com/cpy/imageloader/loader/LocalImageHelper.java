package com.cpy.imageloader.loader;

import java.io.File;
import java.io.FileOutputStream;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

/**
 * �����װ�˱���ͼƬ�Ĵ�ȡ�ӿ�
 * ͷ�������ͼƬͳһ����
 */
public class LocalImageHelper {
	
	
	/**
	 * ��bitmap��ʽ����һ�ű���ͼƬ
	 * @param imgName Ҫ������ͼƬ�ļ���
	 * @return ��Ҫ��bitmap
	 */
	public static Bitmap getLocalImage(final String imgName, String path) {
		Bitmap bitmap = null;
		File file = new File(path + "/" + imgName);
		if(file.exists())
			bitmap = BitmapFactory.decodeFile(path + "/" + imgName);
		
		return bitmap;
	}
	
	/**
	 * ��һ��ͼƬ��bitmap��ʽ���뱾��
	 * void returning, storing fail is ok
	 * @param imgName ͼƬ�ļ���
	 * @param img ͼƬ��bitmap��Դ
	 */
	public static void storeImage(final String imgName, Bitmap img, String path) {
		File file = new File(path + "/" + imgName);
		createImgFilePath(path);
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
	 * rename the photo if it's name return by the server is not
	 * consistent with the name determined by the system(by system date)
	 * @param oldName
	 * @param newName
	 */
	public static void renamePhoto(String oldName, String newName, String path) {
		File file = new File(path + "/" + oldName);
		if(file.exists())
			file.renameTo(new File(path + "/" + newName));
	}
	
	private static void createImgFilePath(String path) {
		File file = new File(path);
		if(!file.exists())
			file.mkdirs();
	}
	
}
