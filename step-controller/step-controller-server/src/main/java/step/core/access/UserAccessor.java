package step.core.access;

import java.util.List;

import step.core.accessors.CRUDAccessor;

public interface UserAccessor extends CRUDAccessor<User> {

	void remove(String username);

	List<User> getAllUsers();

	User getByUsername(String username);

}