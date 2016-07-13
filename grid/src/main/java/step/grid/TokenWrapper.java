package step.grid;

import java.util.Map;

import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;

public class TokenWrapper implements Identity {
	
	private final Token token;
	
	private Object currentOwner;
	
	public TokenWrapper(Token token) {
		super();
		this.token = token;		
	}

	@Override
	public Map<String, String> getAttributes() {
		return token.getAttributes();
	}

	@Override
	public Map<String, Interest> getInterests() {
		return token.getSelectionPatterns();
	}

	@Override
	public String getID() {
		return token.getId();
	}

	public Token getToken() {
		return token;
	}

	public Object getCurrentOwner() {
		return currentOwner;
	}

	public void setCurrentOwner(Object currentOwner) {
		this.currentOwner = currentOwner;
	}

	@Override
	public String toString() {
		return "AdapterToken [id=" + getID() + ", attributes=" + getAttributes() + ", interests=" + getInterests() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getAttributes() == null) ? 0 : getAttributes().hashCode());
		result = prime * result + ((getInterests() == null) ? 0 : getInterests().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TokenWrapper other = (TokenWrapper) obj;
		if (getAttributes() == null) {
			if (other.getAttributes() != null)
				return false;
		} else if (!getAttributes().equals(other.getAttributes()))
			return false;
		if (getInterests() == null) {
			if (other.getInterests() != null)
				return false;
		} else if (!getInterests().equals(other.getInterests()))
			return false;
		return true;
	}
}
