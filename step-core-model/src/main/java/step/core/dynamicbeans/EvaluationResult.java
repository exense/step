package step.core.dynamicbeans;

public class EvaluationResult {

	Object resultValue;
	
	Exception evaluationException;

	public EvaluationResult() {
		super();
	}

	public EvaluationResult(Object resultValue) {
		super();
		this.resultValue = resultValue;
	}

	public Object getResultValue() {
		return resultValue;
	}

	public void setResultValue(Object resultValue) {
		this.resultValue = resultValue;
	}

	public Exception getEvaluationException() {
		return evaluationException;
	}

	public void setEvaluationException(Exception evaluationException) {
		this.evaluationException = evaluationException;
	}
}
