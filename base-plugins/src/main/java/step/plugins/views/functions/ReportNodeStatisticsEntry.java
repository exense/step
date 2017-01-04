package step.plugins.views.functions;

import java.util.HashMap;
import java.util.Map;

public class ReportNodeStatisticsEntry {

	int count;
	
	long sum;

	Map<String, Statistics> byFunctionName = new HashMap<>();

	public int getCount() {
		return count;
	}
	
	public long getSum() {
		return sum;
	}

	public Map<String, Statistics> getByFunctionName() {
		return byFunctionName;
	}
	
	public static class Statistics {
		int count;
		
		long sum;

		public Statistics() {
			super();
		}

		public Statistics(int count, long sum) {
			super();
			this.count = count;
			this.sum = sum;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public long getSum() {
			return sum;
		}

		public void setSum(long sum) {
			this.sum = sum;
		}
	}

}
