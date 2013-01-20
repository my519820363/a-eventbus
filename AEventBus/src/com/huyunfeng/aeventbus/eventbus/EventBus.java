package com.huyunfeng.aeventbus.eventbus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.text.TextUtils;
import android.util.Log;

public class EventBus {
	private static final String TAG = "eventbus";
	public enum ThreadMode {
		Main, Async, Background, Post
	}

	private static final long TIMEOUT = 300;
	private static EventBus defaultBus;

	private MainPoster mainPoster;
	private AsyncPoster asyncPoster;
	private BackgroundPoster backgroundPoster;

	// Object为 注册的对象, Method为 注册的方法， Class<?> 为事件
	private HashMap<Class<?>, HashMap<Object, ArrayList<Method>>> eventPool;
	private HashMap<String, Long> stickyMap;
	private static HashMap<String, EventBus> eventBusMap;

	public synchronized static EventBus getDefault() {
		if (defaultBus == null) {
			defaultBus = new EventBus();
		}

		return defaultBus;
	}

	public synchronized static EventBus getDefault(String key) {
		if (eventBusMap == null) {
			eventBusMap = new HashMap<String, EventBus>();
		}

		if (eventBusMap.containsKey(key)) {
			return eventBusMap.get(key);
		} else {
			EventBus bus = EventBus.newEventBus();
			eventBusMap.put(key, bus);
			return bus;
		}
	}

	public static EventBus newEventBus() {
		return new EventBus();
	}

	private EventBus() {
		eventPool = new HashMap<Class<?>, HashMap<Object, ArrayList<Method>>>();
		stickyMap = new HashMap<String, Long>();

		mainPoster = MainPoster.getInstance();
		asyncPoster = AsyncPoster.getInstance();
		backgroundPoster = BackgroundPoster.getInstance();
	}

	private boolean isDebug = false;

	public void setDebug(boolean debug) {
		isDebug = debug;
	}

	/**
	 * 查找object下所有的参数为BaseEvent的方法，并注册到map中去
	 * 
	 * @param object
	 *            需要Bus连接的对象
	 */
	public boolean register(Object object) {
		if (object == null) {
			return false;
		}

		Method[] methods = null;
		Class<?> clazz = null;
		try {
			// 获取该对象所有的方法
			clazz = object.getClass();
			methods = clazz.getDeclaredMethods();
		} catch (SecurityException e) {
			e.printStackTrace();
			return false;
		}

		// 记录下参数为BaseEvent的方法
		for (Method method : methods) {
			try {
				Class<?>[] clzzs = method.getParameterTypes();
				if (clzzs.length == 1) {
					if (findAllSuperClass(clzzs[0], BaseEvent.class)) {
						if (!method.isAccessible()) {
							method.setAccessible(true);
						}
						putEventToEventPool(object, method, clzzs[0]);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		if (isDebug) {
			logEventPool();
		}
		return true;
	}

	/**
	 * 查找object对象下所有参数为event的方法，并注册到map中去
	 * 
	 * @param object
	 *            需要Bus连接的对象
	 * @param event
	 *            需要Bus监听的事件
	 */
	public boolean register(Object object, Class<?> event) {
		if (object == null || event == null) {
			return false;
		}

		Method[] methods = null;
		Class<?> clazz = null;
		try {
			// 获取该对象所有的方法
			clazz = object.getClass();
			methods = clazz.getDeclaredMethods();
		} catch (SecurityException e) {
			e.printStackTrace();
			return false;
		}

		// 记录下参数为BaseEvent的方法
		for (Method method : methods) {
			try {
				Class<?>[] clzzs = method.getParameterTypes();
				if (clzzs.length == 1) {
					if (findAllClass(clzzs[0], event)) {
						if (!method.isAccessible()) {
							method.setAccessible(true);
						}
						putEventToEventPool(object, method, clzzs[0]);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		if (isDebug) {
			logEventPool();
		}

		return true;
	}

	/**
	 * 注册object对象下，名为methodName,参数为event事件的方法到map中
	 * 
	 * @param object
	 *            需要Bus连接的对象
	 * @param methodName
	 *            需要Bus连接的方法
	 * @param event
	 *            需要Bus监听的事件
	 */
	public boolean register(Object object, String methodName, Class<?> event) {
		if (object == null || TextUtils.isEmpty(methodName) || event == null) {
			return false;
		}

		Method method = null;
		Class<?> clazz = null;
		try {
			// 获取该对象所有的方法
			clazz = object.getClass();
			method = clazz.getDeclaredMethod(methodName, event);
			if (!method.isAccessible()) {
				method.setAccessible(true);
			}

			putEventToEventPool(object, method, event);

			if (isDebug) {
				logEventPool();
			}
			return true;
		} catch (SecurityException e) {
			e.printStackTrace();
			return false;
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 取消obj对象上所有的事件监听
	 * 
	 * @param obj
	 */
	public boolean unRegisterObject(Object obj) {
		boolean flag = removeObjectToEventPool(obj);
		if (isDebug) {
			logEventPool();
		}

		return flag;
	}

	/**
	 * 取消所有已注册对象上的event事件监听
	 * 
	 * @param event
	 */
	public boolean unRegisterEvent(Class<?> event) {
		boolean flag = removeEventToEventPool(event);
		if (isDebug) {
			logEventPool();
		}

		return flag;
	}

	/**
	 * 将事件发送给所有注册到当前Bus的对象，在Bus所在线程
	 * 
	 * @param event
	 */
	public void post(Object event) {
		post(event, ThreadMode.Post);
	}

	/**
	 * 将事件发送给所有注册到当前Bus的对象，并指定事件发生的线程环境
	 * 
	 * @param event
	 * @param threadMode
	 */
	public void post(Object event, ThreadMode threadMode) {
		post(null, event, threadMode);
	}

	/**
	 * 将事件发送给注册到当前Bus的指定对象，在Bus所在线程
	 * 
	 * @param register
	 * @param event
	 */
	public void post(Object register, Object event) {
		post(register, event, ThreadMode.Post);
	}

	/**
	 * 将事件发送给注册到当前Bus的指定对象，并指定事件发生的线程环境
	 * 
	 * @param register
	 * @param event
	 * @param threadMode
	 */
	public void post(Object register, Object event, ThreadMode threadMode) {
		List<EventPacket> eventPackets = getEventPackets(register, event);
		if (eventPackets == null || eventPackets.size() == 0) {
			return;
		}

		switch (threadMode) {
		case Main:
			runMethodsInMain(eventPackets);
			break;
		case Async:
			runMethodsInAsync(eventPackets);
			break;
		case Background:
			runMethodsInBackground(eventPackets);
			break;
		case Post:
			runMethodsInPost(eventPackets);
			break;
		default:
			break;
		}
	}

	public void postSticky(Object event) {
		if (timeout(event)) {
			return;
		}

		post(event, ThreadMode.Post);
	}

	public void postSticky(Object event, ThreadMode threadMode) {
		if (timeout(event)) {
			return;
		}

		post(event, threadMode);
	}

	public void postSticky(Object register, Object event) {
		if (timeout(event)) {
			return;
		}

		post(register, event, ThreadMode.Post);
	}

	public void postSticky(Object register, Object event, ThreadMode threadMode) {
		if (timeout(event)) {
			return;
		}

		post(register, event, threadMode);
	}

	private boolean timeout(Object event) {
		Long time = stickyMap.get(event.getClass().getName());
		if (time != null) {
			long c = System.currentTimeMillis() - time.longValue();
			if (c < TIMEOUT) {
				return true;
			}
		}

		stickyMap.put(event.getClass().getName(), System.currentTimeMillis());
		return false;
	}

	private void runMethodsInMain(List<EventPacket> eventPackets) {
		mainPoster.post(eventPackets);
	}

	private void runMethodsInBackground(List<EventPacket> eventPackets) {
		backgroundPoster.post(eventPackets);
	}

	private void runMethodsInPost(List<EventPacket> eventPackets) {
		runMethods(eventPackets);
	}

	private void runMethodsInAsync(final List<EventPacket> eventPackets) {
		asyncPoster.post(eventPackets);
	}

	public static void runMethods(List<EventPacket> eventPackets) {
		try {
			for (EventPacket eventPacket : eventPackets) {
				runMethod(eventPacket);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void runMethod(EventPacket eventPacket)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		eventPacket.method.invoke(eventPacket.register, eventPacket.event);
	}

	/**
	 * 根据指定对象，获取该对象的事件包
	 * 
	 * @param register
	 * @param event
	 * @return
	 */
	private ArrayList<EventPacket> getEventPackets(Object register, Object event) {
		HashMap<Object, ArrayList<Method>> ommap = eventPool.get(event
				.getClass());
		ArrayList<EventPacket> packetList = null;
		if (ommap != null && ommap.size() > 0) {
			packetList = new ArrayList<EventBus.EventPacket>();
			if (register != null) {
				ArrayList<Method> methods = ommap.get(register);
				if (methods != null && methods.size() > 0) {
					for (Method method : methods) {
						EventPacket packet = new EventPacket();
						packet.register = register;
						packet.method = method;
						packet.event = event;
						packetList.add(packet);
					}
				}
			} else {
				Set<Entry<Object, ArrayList<Method>>> cmset = ommap.entrySet();
				for (Entry<Object, ArrayList<Method>> entry : cmset) {
					Object key = entry.getKey();
					ArrayList<Method> value = entry.getValue();
					if (value != null && value.size() > 0) {
						for (Method method : value) {
							EventPacket packet = new EventPacket();
							packet.register = key;
							packet.method = method;
							packet.event = event;
							packetList.add(packet);
						}
					}
				}
			}
		}

		return packetList;
	}

	static class EventPacket {
		public Object register;
		public Object event;
		public Method method;
		public int level = 0;
	}

	private boolean removeObjectToEventPool(Object obj) {
		if (obj == null) {
			return false;
		}

		Set<Entry<Class<?>, HashMap<Object, ArrayList<Method>>>> cmset = eventPool
				.entrySet();
		if (cmset != null && cmset.size() > 0) {
			for (Entry<Class<?>, HashMap<Object, ArrayList<Method>>> entry : cmset) {
				HashMap<Object, ArrayList<Method>> om = entry.getValue();
				if (om != null && !om.isEmpty()) {
					if (om.containsKey(obj)) {
						om.get(obj).clear();
						om.remove(obj);
					}

					if (om.isEmpty()) {
						eventPool.remove(entry.getKey());
					}
				}
			}
		}

		return true;
	}

	private boolean removeEventToEventPool(Class<?> event) {
		if (event == null) {
			return false;
		}

		if (eventPool.containsKey(event)) {
			eventPool.remove(event);
		}

		return true;
	}

	/**
	 * 将事件加入到Event池
	 * 
	 * @param obj
	 * @param method
	 * @param event
	 */
	private synchronized void putEventToEventPool(Object obj, Method method,
			Class<?> event) {
		// 取出对象的事件集
		HashMap<Object, ArrayList<Method>> om = eventPool.get(event);
		if (om == null) {
			om = new HashMap<Object, ArrayList<Method>>();
			eventPool.put(event, om);
		}

		ArrayList<Method> methods = om.get(obj);
		if (methods != null) {
			if (!methods.contains(method)) {
				methods.add(method);
			}
		} else {
			methods = new ArrayList<Method>();
			methods.add(method);
			om.put(obj, methods);
		}
	}

	/**
	 * 判断 srcClazz 是否继承自 destClazz
	 * 
	 * @param srcClazz
	 * @param destClazz
	 * @return
	 */
	private boolean findAllSuperClass(Class<?> srcClazz, Class<?> destClazz) {
		if (srcClazz == null || destClazz == null) {
			return false;
		}

		if (srcClazz.getName().equals(destClazz.getName())) {
			return true;
		}

		Class<?> superClass = srcClazz.getSuperclass();
		while (superClass != null) {
			if (superClass.getName().equals(destClazz.getName())) {
				return true;
			} else {
				superClass = superClass.getSuperclass();
				continue;
			}
		}

		return false;
	}

	/**
	 * 判断 srcClazz 是否和 destClazz 同一个类型
	 * 
	 * @param srcClazz
	 * @param destClazz
	 * @return
	 */
	private boolean findAllClass(Class<?> srcClazz, Class<?> destClazz) {
		if (srcClazz == null || destClazz == null) {
			return false;
		}

		if (srcClazz.getName().equals(destClazz.getName())) {
			return true;
		} else {
			return false;
		}
	}

	public void logEventPool() {
		Set<Entry<Class<?>, HashMap<Object, ArrayList<Method>>>> set = eventPool
				.entrySet();
		Log.d(TAG, "logEventPool----------------------" + eventPool.size());
		for (Entry<Class<?>, HashMap<Object, ArrayList<Method>>> entry : set) {
			Log.d(TAG, "Class<?>:" + entry.getKey().getName());
			Set<Entry<Object, ArrayList<Method>>> set2 = entry.getValue()
					.entrySet();
			for (Entry<Object, ArrayList<Method>> entry2 : set2) {
				Object key = entry2.getKey();
				ArrayList<Method> value = entry2.getValue();
				for (Method method : value) {
					Log.d(TAG, "Object:" + key.getClass().getName());
					Log.d(TAG, "Method:" + method.getName());
				}
			}
		}
		Log.d(TAG, "logEventPool----------------------");
	}

	public void logEventPoolCount(String flag) {
		Log.d(TAG, "EventPool Count----------" + flag + "------------"
				+ eventPool.size());
	}

}
