package step.localadapters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class QueueManager {
	
	private static long QUEUE_POLLING_RATE = 500;
	
	private ConcurrentHashMap<String, LinkedBlockingQueue<Map<String, Object>>> queues = new ConcurrentHashMap<>();
	
	public void putToQueue(String queueName, Map<String, Object> value) {
		LinkedBlockingQueue<Map<String, Object>> queue = getOrCreateQueue(queueName);
		queue.offer(value);
	}
	
	public Map<String, Object> getFromQueue(String queueName, long timeout) throws InterruptedException, TimeoutException {
		LinkedBlockingQueue<Map<String, Object>> queue = getQueue(queueName, timeout);
		Map<String, Object> value = queue.poll(timeout, TimeUnit.MILLISECONDS);
		if(value!=null) {
			return value;
		} else {
			throw new TimeoutException("Timeout occurred while polling the queue " + queueName);
		}
	}
	
	private LinkedBlockingQueue<Map<String, Object>> getOrCreateQueue(String queueName) {
		LinkedBlockingQueue<Map<String, Object>> queue = queues.get(queueName);
		if(queue == null) {
			queues.putIfAbsent(queueName, new LinkedBlockingQueue<Map<String, Object>>());
			queue = queues.get(queueName);
		}
		return queue;
	}

	private LinkedBlockingQueue<Map<String, Object>> getQueue(String queueName, long timeout) throws TimeoutException, InterruptedException {
		LinkedBlockingQueue<Map<String, Object>> queue = queues.get(queueName);
		if(queue == null) {
			if(timeout>0) {
				Thread.sleep(QUEUE_POLLING_RATE);
				return getQueue(queueName, Math.max(0, timeout - QUEUE_POLLING_RATE));
			} else {
				throw new TimeoutException();
			}
		} else {
			return queue;
		}
	}
}
