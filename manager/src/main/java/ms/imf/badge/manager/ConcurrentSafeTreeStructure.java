package ms.imf.badge.manager;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @see TreeStructure
 *
 * @author f_ms
 * @date 19-7-31
 */
class ConcurrentSafeTreeStructure<Node, Data> extends TreeStructure<Node, Data> {

    private final ReadWriteLock locker = new ReentrantReadWriteLock();

    @Override
    void put(Data data, Iterable<Node> path) {
        Lock lock = locker.writeLock();
        lock.lock();
        try {
            super.put(data, path);
        } finally {
            lock.unlock();
        }
    }

    @Override
    void putMore(Data data, Iterable<? extends Iterable<Node>> paths) {
        Lock lock = locker.writeLock();
        lock.lock();
        try {
            super.putMore(data, paths);
        } finally {
            lock.unlock();
        }
    }

    @Override
    void remove(Data data, Iterable<Node> path) {
        Lock lock = locker.writeLock();
        lock.lock();
        try {
            super.remove(data, path);
        } finally {
            lock.unlock();
        }
    }

    @Override
    void removeMore(Data data, Iterable<? extends Iterable<Node>> paths) {
        Lock lock = locker.writeLock();
        lock.lock();
        try {
            super.removeMore(data, paths);
        } finally {
            lock.unlock();
        }
    }

    @Override
    void clear() {
        Lock lock = locker.writeLock();
        lock.lock();
        try {
            super.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    Set<Data> getMatchPathData(Iterable<Node> path) {
        Lock lock = locker.readLock();
        lock.lock();
        try {
            return super.getMatchPathData(path);
        } finally {
            lock.unlock();
        }
    }

    @Override
    Set<Data> getPathRangeAllData(Iterable<Node> path) {
        Lock lock = locker.readLock();
        lock.lock();
        try {
            return super.getPathRangeAllData(path);
        } finally {
            lock.unlock();
        }
    }

    @Override
    Set<Data> getPathsRangeAllData(Iterable<? extends Iterable<Node>> paths) {
        Lock lock = locker.readLock();
        lock.lock();
        try {
            return super.getPathsRangeAllData(paths);
        } finally {
            lock.unlock();
        }
    }

    @Override
    Set<Data> getMatchPathSubData(Iterable<Node> path) {
        Lock lock = locker.readLock();
        lock.lock();
        try {
            return super.getMatchPathSubData(path);
        } finally {
            lock.unlock();
        }
    }

    @Override
    Set<Data> getMatchPathsSubData(Iterable<? extends Iterable<Node>> paths) {
        Lock lock = locker.readLock();
        lock.lock();
        try {
            return super.getMatchPathsSubData(paths);
        } finally {
            lock.unlock();
        }
    }

    @Override
    Set<Data> getLongestPathData() {
        Lock lock = locker.readLock();
        lock.lock();
        try {
            return super.getLongestPathData();
        } finally {
            lock.unlock();
        }
    }

    @Override
    Set<Data> getShortestPathData() {
        Lock lock = locker.readLock();
        lock.lock();
        try {
            return super.getShortestPathData();
        } finally {
            lock.unlock();
        }
    }
}
