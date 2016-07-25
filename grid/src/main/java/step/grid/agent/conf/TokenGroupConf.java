package step.grid.agent.conf;

public class TokenGroupConf {

	int capacity;
	
	TokenConf tokenConf;

	public TokenGroupConf() {
		super();
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public TokenConf getTokenConf() {
		return tokenConf;
	}

	public void setTokenConf(TokenConf tokenConf) {
		this.tokenConf = tokenConf;
	}
}
