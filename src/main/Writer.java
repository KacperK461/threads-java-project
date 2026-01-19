import java.util.Random;

public class Writer extends Thread {
    
    private final Library library;
    private final String writerName;
    private final int minWriteTime;
    private final int maxWriteTime;
    private final int sleepBetweenOperations;
    private final Random random = new Random();
    private volatile boolean running = true;
    
    public Writer(Library library, String writerName) {
        this(library, writerName, 1000, 3000, 500);
    }
    
    public Writer(Library library, String writerName, int minWriteTime, int maxWriteTime, int sleepBetweenOperations) {
        this.library = library;
        this.writerName = writerName;
        this.minWriteTime = minWriteTime;
        this.maxWriteTime = maxWriteTime;
        this.sleepBetweenOperations = sleepBetweenOperations;
        setName(writerName);
    }
    
    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                library.startWriting(writerName);
                
                int writeTime = minWriteTime + random.nextInt(maxWriteTime - minWriteTime + 1);
                Thread.sleep(writeTime);
                
                library.stopWriting(writerName);
                
                if (sleepBetweenOperations > 0) {
                    Thread.sleep(random.nextInt(sleepBetweenOperations));
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void stopRunning() {
        running = false;
        this.interrupt();
    }
    
    public void setRunning(boolean running) {
        this.running = running;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public String getWriterName() {
        return writerName;
    }
    
    public Library getLibrary() {
        return library;
    }
    
    public int getMinWriteTime() {
        return minWriteTime;
    }
    
    public int getMaxWriteTime() {
        return maxWriteTime;
    }
    
    public int getSleepBetweenOperations() {
        return sleepBetweenOperations;
    }
}
