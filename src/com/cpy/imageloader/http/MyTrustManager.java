package com.cpy.imageloader.http;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class MyTrustManager implements X509TrustManager{
	
	private X509TrustManager defaultTrustManager;
	private X509TrustManager localTrustManager;
	
	boolean isLocal = false;
	
	public MyTrustManager(KeyStore localKeyStore) {
		initDefaultTrustManager();
		initLocalTrustManager(localKeyStore);
//		initAcceptedIssuers();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		try {
			defaultTrustManager.checkClientTrusted(chain, authType);
		} catch (CertificateException e) {
			localTrustManager.checkClientTrusted(chain, authType);
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		try {
			isLocal = false;
			defaultTrustManager.checkServerTrusted(chain, authType);
		} catch (CertificateException e) {
			try {
				isLocal = true;
				localTrustManager.checkServerTrusted(chain, authType);
			} catch (CertificateException e2) {
				throw e2;
			}
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		if(isLocal) return localTrustManager.getAcceptedIssuers();
		else return defaultTrustManager.getAcceptedIssuers();
	}
	
	private void initLocalTrustManager(KeyStore localKeyStore) {
		TrustManagerFactory tmf;
		try {
			tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(localKeyStore);
			localTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
	}
	
	private void initDefaultTrustManager() {
		TrustManagerFactory tmf;
		try {
			tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore)null);
			defaultTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
	}
}
