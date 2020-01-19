package step.core.access;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryUserAccessor extends InMemoryCRUDAccessor<User> implements UserAccessor {

	@Override
	public void remove(String username) {
		remove(new ObjectId(username));
	}

	@Override
	public List<User> getAllUsers() {
		List<User> users = new ArrayList<>();
		getAll().forEachRemaining(u->users.add(u));
		return users;
	}

	@Override
	public User getByUsername(String username) {
		return getAllUsers().stream().filter(u->u.getUsername().equals(username)).findFirst().get();
	}

}
