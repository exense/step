package step.core.scheduler;

public interface ExecutionSchedulerHook {
    
    public void onRemoveExecutionTask(String taskExecutionId);
}
