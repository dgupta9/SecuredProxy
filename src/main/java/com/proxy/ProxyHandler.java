package com.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.sql.ParameterMetaData;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.print.URIException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

/**
 * Servlet implementation class ProxyHandler
 */
@WebServlet(value="/",name="proxy-handler" )
public class ProxyHandler extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String REMORE_SCHEME = "https";
	private static final String REMORE_ADDRESS = "40.80.150.131";
	private static final String REMORE_CONTEXT = "webconsole";
	private static final String HOST_HEADER_NAME = "host";
	private static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
	private static final String COOKIE2_HEADER_NAME = "Cookie2";
	private static enum HTTPRequestType{
		GET,POST,PUT,DELETE
	};
	private static Boolean status; 
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ProxyHandler() {
        super();
        ProxyHandler.status = false;
        if(isRemoteURLAlive())
        	ProxyHandler.status = true;
    }
    
    private boolean isRemoteURLAlive() {
		try {
			StringBuilder url = new StringBuilder(ProxyHandler.REMORE_SCHEME + "://" +  ProxyHandler.REMORE_ADDRESS + "/");
			if(!ProxyHandler.REMORE_CONTEXT.equals(""))
				url.append(ProxyHandler.REMORE_CONTEXT + "/api/");
			HttpResponse resp = connect(url.toString(),HTTPRequestType.GET,null,null,null);
			System.out.println("Resp for init is :"+resp.getStatusLine().getStatusCode());
			if((resp!=null)&(resp.getStatusLine().getStatusCode()!=501)){
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	private static HttpResponse connect(String url,HTTPRequestType reqType,List<Header> headerParam,Map<String, String> urlParams,String body) throws URISyntaxException, NoHttpResponseException, ConnectTimeoutException{
		HttpResponse resp=null;
		HttpUriRequest req=null;
		if(url==null){
			throw new URISyntaxException(url,"Empty URL not allowed");
		}
		System.out.println("Request URL is "+url);
		try{
			HttpClient client = new DefaultHttpClient();
			if(reqType == HTTPRequestType.GET){
				
				// Add URL parameters
				if((urlParams !=null)&&(urlParams.isEmpty())){
					StringBuilder paramUrl = new StringBuilder(url);
					if(!url.contains("?")){
						paramUrl.append("?");
					}
					
					// Add each parameter
					for (Map.Entry<String, String> entry : urlParams.entrySet()) {
							paramUrl.append("&"+entry.getKey()+"="+entry.getValue());
					}
					url = paramUrl.toString();
				}
				
				req = new HttpGet(url);
			}else if(reqType == HTTPRequestType.POST){
				req = new HttpPost(url);
				StringEntity bodyEntity = new StringEntity(body);
				((HttpPost) req).setEntity(bodyEntity);
			}
			
			//add Headers
			if(headerParam!=null){
				for(Header header : headerParam){
					System.out.println("Headers ["+header.getName()+"] = "+header.getValue());
					if(header.getName().equals(ProxyHandler.HOST_HEADER_NAME))
						req.addHeader(header.getName(), ProxyHandler.REMORE_ADDRESS);
					else if(!header.getName().equalsIgnoreCase(ProxyHandler.CONTENT_LENGTH_HEADER_NAME))
						req.addHeader(header.getName(), header.getValue());
					//req.setHeader(header);
				}
			}
			
			//send the request
			resp = client.execute(req);
			
		}catch(NoHttpResponseException e){
			e.printStackTrace();
			throw e;
		}catch(ConnectTimeoutException e){
			e.printStackTrace();
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		
    	return resp;
    }

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp);
		return;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		processRequest(req, resp);
		return;
	}

	
	private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(!ProxyHandler.status){
			resp.setStatus(501);
			return;
		}
		
	    String queryString = req.getQueryString();
	    StringBuilder url = new StringBuilder(ProxyHandler.REMORE_SCHEME + "://" +  ProxyHandler.REMORE_ADDRESS );
	    
	    
	    String temp = req.getContextPath();
	    if((temp!=null)&&(!temp.isEmpty())){
	    	url.append(temp);
	    }
	    
	    temp = req.getServletPath();
	    if((temp!=null)&&(!temp.isEmpty())){
	    	url.append(temp);
	    }
	    
	    temp = req.getPathInfo();
	    if((temp!=null)&&(!temp.isEmpty())){
	    	url.append(temp);
	    }
	    
		//if(!ProxyHandler.REMORE_CONTEXT.equals(""))
		//	url.append(ProxyHandler.REMORE_CONTEXT + "/");
		
		if (queryString != null) 
			url.append('?').append(queryString);
		
		System.out.println("Remote url build is "+url.toString());
		
	    HTTPRequestType reqType = null;
	    StringBuilder body = new StringBuilder();
	    if(req.getMethod().equals("GET"))
	    	reqType = HTTPRequestType.GET;
	    else if(req.getMethod().equals("POST")){
	    	reqType = HTTPRequestType.POST;
	    	InputStream inputStream = req.getInputStream();
	    	 if (inputStream != null) {
	    		 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	             char[] charBuffer = new char[128];
	             int bytesRead = -1;
	             while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
	            	 body.append(charBuffer, 0, bytesRead);
	             }
	         }
	    	 
	    }
	    ArrayList<Header> headerList = new ArrayList<Header>();
	    Enumeration<String> headerNamesList = req.getHeaderNames();
	    while(headerNamesList.hasMoreElements()){
	    	String headerName = headerNamesList.nextElement();
	    	headerList.add(new BasicHeader(headerName, req.getHeader(headerName)));
	    	System.out.println("Headers ["+headerName+"] = "+req.getHeader(headerName));
	    }
	    
	    headerList.add(new BasicHeader(ProxyHandler.COOKIE2_HEADER_NAME, "QSDK 309bfb1337a5d92a2f0e667be83c036fd77a4f2f6cafc4444c597c635aeb0491ef2230723febac76b5519bbca935bc4cc0c659d150a9a720064eafa8575eb9b3b1f99676e44a6e126c50ad1f7fafb6b19afcde0b21b1af3f54dc2e2b7ae18fde7f908232331cb14675203d5b971829f6572c620c6be86b9f3cff9787c903493313a31c8b51b9964a7a2654e29fbdc2a3236bb33574ca77e7de4dc1778894f48a56949c471c22cbdb9321ce11f0168483e19751cc9da20e4dca15b93b8f2eaf72f97eb4a6bb3b5acca01eb5f79bb29f15c744a03eb36330603e2386866e8a1a5b9eb8fa6e4cbc6a3be79e4fecc2b553617761346268fc233aa41e9378c26383ee1"));
	    
	    
	    try {
	    	
			HttpResponse remoteResp = connect(url.toString(),reqType,headerList,null,body.toString());
			System.out.println(remoteResp.getStatusLine().getStatusCode() + " - " + remoteResp.getStatusLine().getReasonPhrase());
			
			resp.setStatus(remoteResp.getStatusLine().getStatusCode(), remoteResp.getStatusLine().getReasonPhrase());
			Header[] respHeaderList = remoteResp.getAllHeaders();
			for(Header header : respHeaderList){
				resp.setHeader(header.getName(), header.getValue());
			}
			
			
			byte[] buffer = new byte[10240];
			
			InputStream input = remoteResp.getEntity().getContent();
			OutputStream output = resp.getOutputStream();
			
			for (int length = 0; (length = input.read(buffer)) > 0;) {
		        output.write(buffer, 0, length);
		        //System.out.println(new String(buffer));
		    }
			
			output.flush();
			output.close();
			input.close();
			
			
			/*
			ReadableByteChannel inputChannel = Channels.newChannel(input);
	        WritableByteChannel outputChannel = Channels.newChannel(output);
			
	        ByteBuffer buffer = ByteBuffer.allocateDirect(10240);
	        long size = 0;
	        
	        while (inputChannel.read(buffer) != -1) {
	            buffer.flip();
	            size += outputChannel.write(buffer);
	            buffer.clear();
	        }
	        
	        output.flush();
	        input.close();
	        output.close();
	        */
	        //resp.setContentLength((int) size);
				
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	    
	}

}
