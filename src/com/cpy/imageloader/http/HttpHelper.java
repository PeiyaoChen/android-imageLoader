package com.cpy.imageloader.http;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import android.util.Pair;


/**
 * HTTP helper for HTTP or HTTPS operations
 * @author cpy
 *
 */
public class HttpHelper {
	
	private static final int CONNECT_TIMEOUT = 10000;
	private static final int READ_TIMEOUT = 30000;

	private static boolean IS_GZIP_ENCODE = false;
	public static KeyStore localKeyStore = null;
	private static SSLContext sslContext = null;
	private static boolean hostNameVarify = true;
	private int statusCode = 200;
	
	private HttpURLConnection connection = null;
	
	public static void setHttpsLocalKeyStore(KeyStore keyStore) {
		localKeyStore = keyStore;
		initSSLContext();
	}
	
	private static void initSSLContext() {
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
	}
	
	/**
	 * Set whether enable gzip ecoding
	 * @param isGzipEncode
	 */
	public static void setIsGzipEncode(boolean isGzipEncode) {
		IS_GZIP_ENCODE = isGzipEncode;
	}
	
	public static void disableHostNameVerification() {
		hostNameVarify = false;
	}
	
	public static void enableHostNameVerification() {
		hostNameVarify = true;
	}
	
	public HttpHelper() {
		if(sslContext == null) {
			initSSLContext();
		}
	};
	
	/**
	 * Get inputStream and its length. </br>
	 * Note: If zip-encode is enable(see {@link #setIsGzipEncode(boolean)}), the length is the size after compressed.
	 * Please call disconnect after you have finished using the inputStream.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Pair<Long, InputStream> getInputStream(String url) throws IOException {
		connection = getConnection(url, false);
		InputStream is = connection.getInputStream();
		long length = connection.getContentLength();
		return new Pair<Long, InputStream>(length, is);
	}
	
	public Pair<Long, InputStream> getInputStream(String url, HashMap<String, String> getParams) throws IOException {
		String actualUrl = combineUrlAndParams(url, getParams);
		return getInputStream(actualUrl);
	}
	
	public String getGetResults(final String url, final HashMap<String, String> getParams) throws IOException {
		String actualUrl = combineUrlAndParams(url, getParams);
		InputStream in = getInputStream(actualUrl).second;
		return getString(in);
	}
	
	public String getPostResults(final String url, final HashMap<String, String> getParams,
			final HashMap<String, String> postParams) throws IOException {
		String body = getQuery(postParams);
		return getPostResults(url, getParams, body);
	}
	
	public String getPostResults(String url, HashMap<String, String> getParams, String body) throws IOException {
		String actualUrl = combineUrlAndParams(url, getParams);
		connection = getConnection(actualUrl, true);
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
		bufferedWriter.write(body);
		bufferedWriter.close();
		InputStream in = connection.getInputStream();
		String result = getString(in);
		connection.disconnect();
		return result;
	}
	
	public boolean getFile(final String url, final HashMap<String, String> getParams, File saveFile, 
			OnDataBufferChangedListener onDataBufferChangedListener) throws IOException {
		Pair<Long, InputStream> inPair = getInputStream(url, getParams);
		long l = inPair.first;
		InputStream in = inPair.second;
		DataInputStream dataInputStream = new DataInputStream(in);
		if(!saveFile.getParentFile().exists()) {
			saveFile.getParentFile().mkdirs();
		}
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(saveFile));
		byte[] buffer = new byte[1024];
		int totalCount = 0;
		int readSize = 0;
		while((readSize = dataInputStream.read(buffer)) >= 0 && totalCount < l) {
			out.write(buffer, 0, readSize);
			totalCount += readSize;
			if(onDataBufferChangedListener != null)
				onDataBufferChangedListener.onDataBufferChanged((float) totalCount / l);
		}
		in.close();
		out.close();
		return true;
	}
	
	public void disconnect() {
		if(connection != null)
			connection.disconnect();
	}
	
	//private
	
	private void setConnection(HttpURLConnection connection, boolean isPost) {
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.setReadTimeout(READ_TIMEOUT);
		connection.setInstanceFollowRedirects(true);
		connection.setDoInput(true);
		connection.setDoOutput(isPost);
		if(!IS_GZIP_ENCODE)
			connection.setRequestProperty("Accept-Encoding", "identity");
		else
			connection.setRequestProperty("Accept-Encoding", "identity, gzip");

		if(isPost)
			try {
				//If the exception appear, it is the deveoper's fault. Potocal's name error.
				connection.setRequestMethod("POST");
			} catch (ProtocolException e) {
				e.printStackTrace();
			}
	}
	
	private HttpURLConnection getConnection(String url, boolean isPost) throws IOException {
		URL realurl = null;
		realurl = new URL(url);
		if(realurl.getProtocol().equals("https")) {
			HttpsURLConnection connection = (HttpsURLConnection) realurl.openConnection();
			connection.setSSLSocketFactory(sslContext.getSocketFactory());
			setConnection(connection, isPost);
			if(!hostNameVarify)
				connection.setHostnameVerifier(new AllowAllHostnameVerifier());
			connection.connect();
			statusCode = connection.getResponseCode();
			if(statusCode >= HttpURLConnection.HTTP_OK && statusCode <= HttpURLConnection.HTTP_PARTIAL) 
				return connection;
			else
				throw new IOException("statusCode:" + statusCode);
		}
		else if(realurl.getProtocol().equals("http")) {
			HttpURLConnection connection = (HttpURLConnection) realurl
					.openConnection();
			setConnection(connection, isPost);
			connection.connect();
			if(statusCode >= HttpURLConnection.HTTP_OK && statusCode <= HttpURLConnection.HTTP_PARTIAL) 
				return connection;
			else
				throw new IOException("statusCode:" + statusCode);
		}
		return null;
	}
	
	private String getString(InputStream in) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		while((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line);
		}
		bufferedReader.close();
		return stringBuilder.toString();
	}
	
	private String getQuery(HashMap<String, String> params) throws UnsupportedEncodingException
	{
	    StringBuilder result = new StringBuilder();
	    boolean first = true;

	    for (Entry<String, String> entry : params.entrySet())
	    {
	        if (first)
	            first = false;
	        else
	            result.append("&");

	        result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
	        result.append("=");
	        result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
	    }

	    return result.toString();
	}
	
	private String combineUrlAndParams(String url, HashMap<String, String> getParams) {
		String httpUrl = url;
		if(getParams != null) {
			Iterator<Entry<String, String>> it = getParams.entrySet().iterator();
			boolean first = true;
			
			while (it.hasNext()) {
				Map.Entry<String, String> entry = (Map.Entry<String, String>)it.next();
				Object key = entry.getKey();
				Object val = entry.getValue();
				
				if(first) {
					httpUrl += "?";
					first = false;
				} else
					httpUrl += "&";
				
				httpUrl += (key.toString() + "=" + val.toString());
			}
		}
		return httpUrl;
	}
	
	/**
	 * Callback interface for monitor the progress of file downloading
	 * @author cpy
	 *
	 */
	public interface OnDataBufferChangedListener {
		/**
		 * Called when file downloading progress updates
		 * @param progress progress of file downloading, completed is 1.
		 */
		public void onDataBufferChanged(float progress);
	}
	
}
