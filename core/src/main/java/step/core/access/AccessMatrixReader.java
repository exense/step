package step.core.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessMatrixReader {
	
	public Map<String, List<String>> readAccessMatrix(File file) throws IOException {		
		Map<String, List<String>> result = new HashMap<>();

        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        	Map<Integer, String> indexToRole = new HashMap<>();
        	String headerLine = br.readLine();
        	String[] roles = headerLine.split(cvsSplitBy);
        	for(int i=1;i<roles.length;i++) {
        		indexToRole.put(i, roles[i]);
        		result.put(roles[i], new ArrayList<>());
        	}
        	
        	String line = "";
            while ((line = br.readLine()) != null) {
            	String[] split = line.split(cvsSplitBy);
                String right = split[0];
                for(int i=1;i<split.length;i++) {
                	if(split[i].equals("x")) {
	            		String role = indexToRole.get(i);
	            		result.get(role).add(right);
                	}
            	}
            }

            return result;
        } catch (IOException e) {
            throw e;
        }
	}

}
