package step.grid.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgumentParser {
	private HashMap<String, String> options = new HashMap<String, String>();
	private Pattern p = Pattern.compile("-(.+?)(?:=(.+?))?$");

	public ArgumentParser(String[] paramArrayOfString) {
		Matcher localMatcher = null;
		for (int i = 0; i < paramArrayOfString.length; i++) {
			if (!(localMatcher = this.p.matcher(paramArrayOfString[i])).find())
				continue;
			this.options.put(localMatcher.group(1).toLowerCase(),
					localMatcher.group(2));
		}
	}

	public boolean hasOption(String paramString) {
		return this.options.containsKey(paramString.toLowerCase());
	}

	public String getOption(String paramString) {
		return (String) this.options.get(paramString.toLowerCase());
	}

	public Set<Entry<String, String>> entrySet() {
		return options.entrySet();
	}

	public Map<String, String> getOptions() {
		return options;
	}
}
