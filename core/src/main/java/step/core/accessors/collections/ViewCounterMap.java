package step.core.accessors.collections;

public class ViewCounterMap  extends ThresholdMap<String, Integer> {

	private static final long serialVersionUID = -5842315205753972877L;
	
	public ViewCounterMap(){
		super(500, "Other");
	}
	
	public ViewCounterMap(int threshold, String defaultKey){
		super(threshold, defaultKey);
	}
	
	public void incrementForKey(String key){
		Integer current = get(key);
		if(current == null){
			put(key, 1);
		}
		else{
			put(key, current + 1);
		}
	}
}
