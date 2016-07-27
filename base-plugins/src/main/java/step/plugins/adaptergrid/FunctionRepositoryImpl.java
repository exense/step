package step.plugins.adaptergrid;

import java.util.Iterator;
import java.util.Map;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;

import step.functions.Function;
import step.functions.FunctionRepository;

public class FunctionRepositoryImpl implements FunctionRepository {

	private MongoCollection functions;
		
	public FunctionRepositoryImpl(MongoCollection functions) {
		super();
		this.functions = functions;
	}

	@Override
	public Function getFunctionById(String id) {
		return functions.findOne(new ObjectId(id)).as(Function.class);
	}

	@Override
	public Function getFunctionByAttributes(Map<String, String> attributes) {		
		String query = "{attributes.name:'"+attributes.get("name")+"'}";
		
		Iterator<Function> it = functions.find(query).as(Function.class).iterator();
		if(it.hasNext()) {
			Function function = it.next();
			
			if(it.hasNext()) {
				// TODO warning
			} 
			
			return function;
		} else {
			throw new RuntimeException("Unable to find function with attributes "+attributes.toString());
		}
	}

	@Override
	public void addFunction(Function function) {
		functions.insert(function);
	}

	@Override
	public void deleteFunction(String functionId) {
		functions.remove(new ObjectId(functionId));
	}

}
