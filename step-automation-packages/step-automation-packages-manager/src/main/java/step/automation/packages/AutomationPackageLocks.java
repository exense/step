package step.automation.packages;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AutomationPackageLocks {

    private final Map<String, ReadWriteLock> locksMap = new ConcurrentHashMap<>();
    private final int readLockTimeoutSeconds;

    public AutomationPackageLocks(int readLockTimeoutSeconds) {
        this.readLockTimeoutSeconds = readLockTimeoutSeconds;
    }

    private ReadWriteLock getLockForAutomationPackage(String automationPackageId) {
        return locksMap.computeIfAbsent(automationPackageId, (k) -> new ReentrantReadWriteLock());
    }

    public boolean tryReadLock(String automationPackageId) throws InterruptedException {
        return getLockForAutomationPackage(automationPackageId).readLock().tryLock(readLockTimeoutSeconds, TimeUnit.SECONDS);
    }

    public void readUnlock(String automationPackageId) {
        getLockForAutomationPackage(automationPackageId).readLock().unlock();
    }

    public void writeLock(String automationPackageId) {
        getLockForAutomationPackage(automationPackageId).writeLock().lock();
    }

    public void writeUnlock(String automationPackageId) {
        getLockForAutomationPackage(automationPackageId).writeLock().unlock();
    }

    public boolean tryWriteLock(String automationPackageId) {
        return getLockForAutomationPackage(automationPackageId).writeLock().tryLock();
    }

    public void releaseAndRemoveLock(String automationPackageId) {
        ReadWriteLock remove = locksMap.remove(automationPackageId);
        remove.writeLock().unlock();
    }

}
