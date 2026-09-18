package step.ide.api;

public interface IDEExecutorDelegateFactory {

    IDEExecutorDelegate createDelegate(IDEExecutionRequest request);

}
