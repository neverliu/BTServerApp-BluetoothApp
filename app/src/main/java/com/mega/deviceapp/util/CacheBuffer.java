package com.mega.deviceapp.util;

import java.util.ArrayList;
import java.util.List;

public class CacheBuffer {

	private byte[] buffer;
	private int length=512;
	private int currentIndex=0;
	private List<byte[]> mListByte = new ArrayList<>();
	
	public CacheBuffer(){
		buffer = new byte[length];
		mListByte.clear();
		currentIndex=0;
	}
	
	public byte[] getBuffer(){
		if(buffer==null){
			buffer = new byte[length];
			currentIndex=0;
		}
		return buffer;
	}
	
	public void append(byte[] buf){
		if(buf==null){
			return;
		}
		if(buffer==null){
			buffer = new byte[length];
			currentIndex=0;
		}
		System.arraycopy(buf, 0, buffer, currentIndex, buf.length);
		currentIndex += buf.length;
	}
	
	public int getCurrentIndex(){
		return currentIndex;
	}
	
	public void clearBuffer(){
		currentIndex = 0;
	}

	public void clearBufferList(){
		mListByte.clear();
	}

	public void appendByte(byte[] buf, int length){
		if(buf == null){
			return;
		}
		if(mListByte == null){
			mListByte = new ArrayList<>();
		}
		byte[] data = new byte[length];
		System.arraycopy(buf, 2, data, 0, length);
		mListByte.add(data);
	}

	public String ListByteToString(){
		if(mListByte == null)
			return null;

		int allSize = 0;
		for(byte[] a :mListByte){
			allSize += a.length;
		}
		byte[] data = new byte[allSize];
		int currentSize = 0;
		for(int i = 0 ; i < mListByte.size() ; i++){
			System.arraycopy(mListByte.get(i), 0, data, currentSize, mListByte.get(i).length);
			currentSize += mListByte.get(i).length;
		}
		String dataList = new String(data);
		return dataList;
	}
}
