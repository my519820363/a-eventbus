package com.huyunfeng.aeventbus.eventbus;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.huyunfeng.aeventbus.eventbus.EventBus.EventPacket;

public class MainPoster extends Handler {
	private PriorityBlockingQueue<EventPacket> queue;
	private static MainPoster instance;
	
	public static MainPoster getInstance() {
		if (instance == null) {
			instance = new MainPoster();
		}
		
		return instance;
	}
	
	private MainPoster() {
		super(Looper.getMainLooper());
		queue = new PriorityBlockingQueue<EventPacket>(32, sortComparator);
	}
	
	public void post(List<EventPacket> eventPackets) {
		queue.addAll(eventPackets);
		sendMessage(obtainMessage());
	}
	
	@Override
    public void handleMessage(Message msg) {
		EventPacket packet = queue.poll();
		try {
			EventBus.runMethod(packet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
