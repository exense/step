package step.core.reports;

public enum ErrorType {

	/**
	 * Represents a unexpected technical error
	 * This is reported as TECHNICAL_ERROR
	 */
	TECHNICAL,
	
	/**
	 * Represents a clearly identified error (mainly in the SUT)
	 * This is reported as FAILED
	 */
	BUSINESS;
}
