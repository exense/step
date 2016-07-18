package step.grid.reports;

import step.grid.Token;

public class TokenAssociation {

	Token token;
	
	Object owner;

	public TokenAssociation(Token token, Object owner) {
		super();
		this.token = token;
		this.owner = owner;
	}

	public Token getToken() {
		return token;
	}

	public Object getOwner() {
		return owner;
	}
}
