package com.huyunfeng.aeventbus.eventbus;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池 、缓冲队列
 */
public class DefaultThreadPool {

	/**
	 * BaseRequest任务队列
	 */
	private LinkedBlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<Runnable>();

	/**
	 * 线程池
	 */
	private AbstractExecutorService pool = new ThreadPoolExecutor(10, 30, 15L,
			TimeUnit.SECONDS, blockingQueue,
			new ThreadPoolExecutor.CallerRunsPolicy());

	private static DefaultThreadPool instance = null;

	public static DefaultThreadPool getInstance() {
		if (instance == null) {
			instance = new DefaultThreadPool();
		}
		return instance;
	}

	private DefaultThreadPool() {

	}

	public void execute(Runnable r) {
		if (null != pool && null != r) {
			Thread t = new Thread(r);
			// 设置线程的优先级别，让线程先后顺序执行（级别越高，抢到cpu执行的时间越多）
			t.setPriority(Thread.NORM_PRIORITY - 1);
			t.setDaemon(true);
			pool.execute(t);
		}
	}

	/**
	 * 关闭，并等待任务执行完成，不接受新任务
	 */
	public void shutdown() {
		if (null != pool) {
			pool.shutdown();
		}
	}

	/**
	 * 关闭，立即关闭，并挂起所有正在执行的线程，不接受新任务
	 */
	public void shutdownRightnow() {
		if (null != pool) {
			// List<Runnable> tasks =pool.shutdownNow();
			pool.shutdownNow();
			try {
				// 设置超时极短，强制关闭所有任务
				pool.awaitTermination(1, TimeUnit.MICROSECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			clearTaskFromQueue();
			instance = null;
			pool = null;
		}
	}

	public void clearTaskFromQueue() {
		blockingQueue.clear();
	}
}
