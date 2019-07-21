package com.example.scl_server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class StreetLight {
	private String ip;
	private String port;
	
	public StreetLight(String ip, String port) {
		this.ip = ip;
		this.port = port;
	}
	
	public String getIP() {
		return ip;
	}
	
	
	public void resetTimer() {
		URL url = null;
		try {
			url = new URL("http://" + ip + ":" + port + "/LED");
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		URLConnection con;
		try {
			con = url.openConnection();
			HttpURLConnection http = (HttpURLConnection)con;
			http.setRequestMethod("POST"); // PUT is another valid option
			
			System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
