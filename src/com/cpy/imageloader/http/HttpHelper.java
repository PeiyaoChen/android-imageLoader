package com.cpy.imageloader.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

public class HttpHelper {
	
	public static KeyStore localKeyStore = null;
	private static HttpHelper instance;
	private SSLContext sslContext;
	private static boolean hostNameVarify = true;
	
	public static void init(KeyStore keyStore) {
		localKeyStore = keyStore;
	}
	
	public static void disableHostNameVerification() {
		hostNameVarify = false;
	}
	
	public static void enableHostNameVerification() {
		hostNameVarify = true;
	}
	
	private HttpHelper() {
		MyTrustManager myTrustManager = new MyTrustManager(localKeyStore);
		TrustManager[] trustManagers = {myTrustManager};
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagers, null);
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	};
	
	public static HttpHelper getInstance() {
		if(instance == null) {
			synchronized (HttpHelper.class) {
				if(instance == null)
					instance = new HttpHelper();
			}
		}
		return instance;
	}
	
	public InputStream getInputStream(String url) throws IOException {
		URL realurl = null;
		realurl = new URL(url);
		InputStream is = null;
		if(realurl.getProtocol().equals("https")) {
			HttpsURLConnection connection = (HttpsURLConnection) realurl.openConnection();
			connection.setSSLSocketFactory(sslContext.getSocketFactory());
			connection.setDoInput(true);
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(30000);
			connection.setInstanceFollowRedirects(true);
			if(!hostNameVarify)
				connection.setHostnameVerifier(new AllowAllHostnameVerifier());
			connection.connect();
			is = connection.getInputStream();
		}
		else if(realurl.getProtocol().equals("http")) {
			HttpURLConnection connection = (HttpURLConnection) realurl
					.openConnection();
			connection.setDoInput(true);
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(30000);
			connection.setInstanceFollowRedirects(true);
			connection.connect();

			is = connection.getInputStream();
		}
		return is;
	}
	
}
