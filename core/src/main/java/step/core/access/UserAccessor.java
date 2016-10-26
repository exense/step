package step.core.access;

import org.bson.types.ObjectId;
import org.jongo.MongoCollection;

import com.mongodb.MongoClient;

import step.core.accessors.MongoDBAccessorHelper;

public class UserAccessor {

	MongoCollection users;
	
	public UserAccessor() {
		super();
	}

	public UserAccessor(MongoClient client) {
		super();
		users = MongoDBAccessorHelper.getCollection(client, "users");
	}

	public void save(User node) {
		users.save(node);
	}
	
	public User get(ObjectId nodeId) {
		return users.findOne(nodeId).as(User.class);
	}
	
	public User getByUsername(String username) {
		assert username != null;
		return users.findOne("{username: #}", username).as(User.class);
	}

}
