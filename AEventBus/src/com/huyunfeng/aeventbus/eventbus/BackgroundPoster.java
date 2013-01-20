package com.huyunfeng.aeventbus.eventbus;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.huyunfeng.aeventbus.eventbus.EventBus.EventPacket;

@SuppressLint("HandlerLeak")
public class BackgroundPoster {
	private PriorityBlockingQueue<EventPacket> queue;
	private PosterHandler posterHandler;
	private DefaultThreadPool threadPool;

	private static BackgroundPoster instance;
	
	public static BackgroundPoster getInstance() {
		if (instance == null) {
			instance = new BackgroundPoster();
		}
		
		return instance;
	}
	
	private BackgroundPoster() {
		threadPool = DefaultThreadPool.getInstance();
		queue = new PriorityBlockingQueue<EventPacket>(16, sortComparator);
		threadPool.execute(new BackgroundThread());
	}

	public void post(List<EventPacket> eventPackets) {
		queue.addAll(eventPackets);
		if (posterHandler != null) {
			posterHandler.sendMessage(posterHandler.obtainMessage());
		}
	}

	private class BackgroundThread implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (Looper.myLooper() == null) {
				Looper.prepare();
			}
			
			posterHandler = new PosterHandler();
			
			Looper.loop();
		}
	}

	private class PosterHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			EventPacket packet = queue.poll();
			try {
				EventBus.runMethod(packet);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static Comparator<EventPacket> sortComparator = new Comparator<EventBus.EventPacket>() {

		@Override
		public int compare(EventPacket lhs, EventPacket rhs) {
			// TODO Auto-generated method stub
			if (lhs.level < rhs.level) {
				return 1;
			} else if (lhs.level == rhs.level) {
				return 0;
			} else {
				return -1;
			}
		}
	};
}
