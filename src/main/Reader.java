import java.util.Random;

public class Reader extends Thread {
    
    private final Library library;
    private final String readerName;
    private final int minReadTime;
    private final int maxReadTime;
    private final int sleepBetweenOperations;
    private final Random random = new Random();
    private volatile boolean running = true;
    
    public Reader(Library library, String readerName) {
        this(library, readerName, 1000, 3000, 500);
    }
    
    public Reader(Library library, String readerName, int minReadTime, int maxReadTime, int sleepBetweenOperations) {
        this.library = library;
        this.readerName = readerName;
        this.minReadTime = minReadTime;
        this.maxReadTime = maxReadTime;
        this.sleepBetweenOperations = sleepBetweenOperations;
        setName(readerName);
    }
    
    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                library.startReading(readerName);
                
                int readTime = minReadTime + random.nextInt(maxReadTime - minReadTime + 1);
                Thread.sleep(readTime);
                
                library.stopReading(readerName);
                
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
    
    public boolean isRunning() {
        return running;
    }
    
    public String getReaderName() {
        return readerName;
    }
    
    public Library getLibrary() {
        return library;
    }
    
    public int getMinReadTime() {
        return minReadTime;
    }
    
    public int getMaxReadTime() {
        return maxReadTime;
    }
    
    public int getSleepBetweenOperations() {
        return sleepBetweenOperations;
    }
}
