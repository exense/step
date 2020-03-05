package step.core.execution;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;

import step.core.artefacts.reports.ReportNode;

public class EventManager {

	private ConcurrentHashMap<ObjectId,List<ReportNodeEventListener>> listeners = new ConcurrentHashMap<>();
	
	public void addReportNodeEventListener(ReportNode node, ReportNodeEventListener listener) {
		List<ReportNodeEventListener> listenerList = listeners.get(node.getId());
		if(listenerList==null) {
			listenerList = new LinkedList<ReportNodeEventListener>();
			List<ReportNodeEventListener> previousValue = listeners.putIfAbsent(node.getId(), listenerList);
			if(previousValue!=null) {
				listenerList = previousValue;
			}
		}
		synchronized (listenerList) {
			listenerList.add(listener);			
		}
	}
	
	public void notifyReportNodeUpdated(ReportNode node) {
		List<ReportNodeEventListener> listenerList = listeners.get(node.getId());
		if(listenerList!=null) {
			synchronized (listenerList) {
				for(ReportNodeEventListener listener:listenerList) {
					listener.onUpdate();
				}
			}
		}
	}
	
	public void notifyReportNodeDestroyed(ReportNode node) {
		List<ReportNodeEventListener> listenerList = listeners.remove(node.getId());
		if(listenerList!=null) {
			synchronized (listenerList) {
				for(ReportNodeEventListener listener:listenerList) {
					listener.onDestroy();
				}
			}
		}
	}
}
