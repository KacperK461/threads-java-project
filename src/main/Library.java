import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Library {
    
    private static final int MAX_READERS = 5;
    
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition canEnter = lock.newCondition();
    
    private final Queue<WaitingEntity> waitingQueue = new LinkedList<>();
    
    private final List<String> readersInLibrary = new ArrayList<>();
    private String writerInLibrary = null;
    
    private int activeReaders = 0;
    private int activeWriters = 0;
    

    private static class WaitingEntity {
        final String name;
        final boolean isWriter;
        final Condition condition;
        boolean canProceed = false;
        
        WaitingEntity(String name, boolean isWriter, Condition condition) {
            this.name = name;
            this.isWriter = isWriter;
            this.condition = condition;
        }
    }
    
    public void startReading(String readerName) {
        lock.lock();
        try {
            Condition myCondition = lock.newCondition();
            WaitingEntity myEntry = new WaitingEntity(readerName, false, myCondition);
            waitingQueue.add(myEntry);
            
            printStatus(readerName + " (czytelnik) chce wejść do czytelni");
            
            while (!canReaderEnter(myEntry)) {
                try {
                    myCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    waitingQueue.remove(myEntry);
                    return;
                }
            }
            
            waitingQueue.remove(myEntry);
            activeReaders++;
            readersInLibrary.add(readerName);
            
            printStatus(readerName + " (czytelnik) WCHODZI do czytelni");
            
            signalNext();
            
        } finally {
            lock.unlock();
        }
    }
    
    public void stopReading(String readerName) {
        lock.lock();
        try {
            activeReaders--;
            readersInLibrary.remove(readerName);
            
            printStatus(readerName + " (czytelnik) WYCHODZI z czytelni");
            
            signalNext();
            
        } finally {
            lock.unlock();
        }
    }
    
    public void startWriting(String writerName) {
        lock.lock();
        try {
            Condition myCondition = lock.newCondition();
            WaitingEntity myEntry = new WaitingEntity(writerName, true, myCondition);
            waitingQueue.add(myEntry);
            
            printStatus(writerName + " (pisarz) chce wejść do czytelni");
            
            while (!canWriterEnter(myEntry)) {
                try {
                    myCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    waitingQueue.remove(myEntry);
                    return;
                }
            }
            
            waitingQueue.remove(myEntry);
            activeWriters = 1;
            writerInLibrary = writerName;
            
            printStatus(writerName + " (pisarz) WCHODZI do czytelni");
            
        } finally {
            lock.unlock();
        }
    }
    
    public void stopWriting(String writerName) {
        lock.lock();
        try {
            activeWriters = 0;
            writerInLibrary = null;
            
            printStatus(writerName + " (pisarz) WYCHODZI z czytelni");
            
            signalNext();
            
        } finally {
            lock.unlock();
        }
    }
    
    private boolean canReaderEnter(WaitingEntity reader) {
        if (activeWriters > 0) {
            return false;
        }
        
        if (activeReaders >= MAX_READERS) {
            return false;
        }
        
        for (WaitingEntity entity : waitingQueue) {
            if (entity == reader) {
                return true;
            }
            if (entity.isWriter) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean canWriterEnter(WaitingEntity writer) {
        if (activeWriters > 0 || activeReaders > 0) {
            return false;
        }
        
        WaitingEntity first = waitingQueue.peek();
        return first == writer;
    }
    
    private void signalNext() {
        for (WaitingEntity entity : waitingQueue) {
            entity.condition.signal();
        }
    }
    
    private void printStatus(String event) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("ZDARZENIE: ").append(event).append("\n");
        sb.append("----------------------------------------\n");
        
        sb.append("W CZYTELNI:\n");
        if (writerInLibrary != null) {
            sb.append("  Pisarze (1): [").append(writerInLibrary).append("]\n");
            sb.append("  Czytelnicy (0): []\n");
        } else {
            sb.append("  Pisarze (0): []\n");
            sb.append("  Czytelnicy (").append(activeReaders).append("): ");
            sb.append(readersInLibrary).append("\n");
        }
        
        sb.append("W KOLEJCE:\n");
        int waitingReaders = 0;
        int waitingWriters = 0;
        List<String> waitingReaderNames = new ArrayList<>();
        List<String> waitingWriterNames = new ArrayList<>();
        
        for (WaitingEntity entity : waitingQueue) {
            if (entity.isWriter) {
                waitingWriters++;
                waitingWriterNames.add(entity.name);
            } else {
                waitingReaders++;
                waitingReaderNames.add(entity.name);
            }
        }
        
        sb.append("  Pisarze (").append(waitingWriters).append("): ");
        sb.append(waitingWriterNames).append("\n");
        sb.append("  Czytelnicy (").append(waitingReaders).append("): ");
        sb.append(waitingReaderNames).append("\n");
        
        sb.append("  Kolejność: ");
        List<String> queueOrder = new ArrayList<>();
        for (WaitingEntity entity : waitingQueue) {
            queueOrder.add(entity.name + (entity.isWriter ? "(P)" : "(C)"));
        }
        sb.append(queueOrder).append("\n");
        
        sb.append("========================================\n");
        
        System.out.print(sb);
    }
    
    public int getActiveReaders() {
        lock.lock();
        try {
            return activeReaders;
        } finally {
            lock.unlock();
        }
    }
    
    public int getActiveWriters() {
        lock.lock();
        try {
            return activeWriters;
        } finally {
            lock.unlock();
        }
    }
    
    public int getQueueSize() {
        lock.lock();
        try {
            return waitingQueue.size();
        } finally {
            lock.unlock();
        }
    }
    
    public int getMaxReaders() {
        return MAX_READERS;
    }
    
    public boolean isEmpty() {
        lock.lock();
        try {
            return activeReaders == 0 && activeWriters == 0;
        } finally {
            lock.unlock();
        }
    }
    
    public List<String> getReadersInLibrary() {
        lock.lock();
        try {
            return new ArrayList<>(readersInLibrary);
        } finally {
            lock.unlock();
        }
    }
    
    public String getWriterInLibrary() {
        lock.lock();
        try {
            return writerInLibrary;
        } finally {
            lock.unlock();
        }
    }
}
