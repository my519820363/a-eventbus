package com.huyunfeng.aeventbus.eventbus;

import java.util.List;

import com.huyunfeng.aeventbus.eventbus.EventBus.EventPacket;

public class AsyncPoster {
	private DefaultThreadPool threadPool;

	private static AsyncPoster instance;

	public static AsyncPoster getInstance() {
		if (instance == null) {
			instance = new AsyncPoster();
		}

		return instance;
	}

	private AsyncPoster() {
		threadPool = DefaultThreadPool.getInstance();
	}

	public void post(final List<EventPacket> eventPackets) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					EventBus.runMethods(eventPackets);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		threadPool.execute(r);
	}

}
