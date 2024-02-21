package step.automation.packages;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AutomationPackageLocks {

    Map<String, ReadWriteLock> locksMap = new ConcurrentHashMap<>();

    private ReadWriteLock getLockForAutomationPackage(String automationPackageId) {
        return locksMap.computeIfAbsent(automationPackageId, (k) -> new ReentrantReadWriteLock());
    }

    public void readLock(String automationPackageId) {
        getLockForAutomationPackage(automationPackageId).readLock().lock();
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

    public void removeLock(String automationPackageId) {
        ReadWriteLock readWriteLock = locksMap.get(automationPackageId);
        if (readWriteLock != null) {
            readWriteLock.writeLock().lock();
            ReadWriteLock remove = locksMap.remove(automationPackageId);
            if (remove != null) {
                remove.writeLock().unlock();
            }
        }
    }
}
