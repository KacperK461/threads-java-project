import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReaderTest {
    
    private Library library;
    
    @BeforeEach
    void setUp() {
        library = new Library();
    }
    
    @Test
    @DisplayName("Konstruktor domyślny powinien ustawić prawidłowe wartości")
    void defaultConstructorShouldSetCorrectValues() {
        Reader reader = new Reader(library, "Czytelnik-1");
        
        assertEquals("Czytelnik-1", reader.getReaderName());
        assertEquals(library, reader.getLibrary());
        assertEquals(1000, reader.getMinReadTime());
        assertEquals(3000, reader.getMaxReadTime());
        assertEquals(500, reader.getSleepBetweenOperations());
        assertEquals("Czytelnik-1", reader.getName());
        assertTrue(reader.isRunning());
    }
    
    @Test
    @DisplayName("Konstruktor z parametrami powinien ustawić prawidłowe wartości")
    void parameterizedConstructorShouldSetCorrectValues() {
        Reader reader = new Reader(library, "Czytelnik-Test", 100, 200, 50);
        
        assertEquals("Czytelnik-Test", reader.getReaderName());
        assertEquals(library, reader.getLibrary());
        assertEquals(100, reader.getMinReadTime());
        assertEquals(200, reader.getMaxReadTime());
        assertEquals(50, reader.getSleepBetweenOperations());
    }
    
    @Test
    @DisplayName("Czytelnik powinien czytać w pętli")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void readerShouldReadInLoop() throws InterruptedException {
        AtomicInteger readCount = new AtomicInteger(0);
        
        Reader reader = new Reader(library, "Czytelnik-1", 50, 100, 10) {
            @Override
            public void run() {
                while (isRunning() && !Thread.currentThread().isInterrupted()) {
                    try {
                        getLibrary().startReading(getReaderName());
                        readCount.incrementAndGet();
                        Thread.sleep(50);
                        getLibrary().stopReading(getReaderName());
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        };
        
        reader.start();
        Thread.sleep(500);
        reader.stopRunning();
        reader.join(1000);
        
        assertTrue(readCount.get() >= 3, "Czytelnik powinien przeczytać co najmniej 3 razy");
    }
    
    @Test
    @DisplayName("stopRunning powinien zatrzymać czytelnika")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void stopRunningShouldStopReader() throws InterruptedException {
        Reader reader = new Reader(library, "Czytelnik-1", 50, 100, 10);
        
        reader.start();
        Thread.sleep(200);
        assertTrue(reader.isRunning());
        assertTrue(reader.isAlive());
        
        reader.stopRunning();
        reader.join(2000);
        
        assertFalse(reader.isRunning());
        assertFalse(reader.isAlive());
    }
    
    @Test
    @DisplayName("Czytelnik powinien obsługiwać przerwanie")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void readerShouldHandleInterrupt() throws InterruptedException {
        Reader reader = new Reader(library, "Czytelnik-1", 1000, 2000, 100);
        
        reader.start();
        Thread.sleep(100);
        
        reader.interrupt();
        reader.join(2000);
        
        assertFalse(reader.isAlive());
    }
    
    @Test
    @DisplayName("Wielu czytelników powinno działać równocześnie")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void multipleReadersShouldWorkConcurrently() throws InterruptedException {
        Reader[] readers = new Reader[5];
        
        for (int i = 0; i < 5; i++) {
            readers[i] = new Reader(library, "Czytelnik-" + (i + 1), 50, 100, 10);
            readers[i].start();
        }
        
        Thread.sleep(500);
        
        boolean anyActive = false;
        for (Reader reader : readers) {
            if (reader.isAlive()) {
                anyActive = true;
                break;
            }
        }
        assertTrue(anyActive);
        
        for (Reader reader : readers) {
            reader.stopRunning();
        }
        
        for (Reader reader : readers) {
            reader.join(2000);
        }
        
        Thread.sleep(200);
        
        for (Reader reader : readers) {
            assertFalse(reader.isAlive());
        }
    }
    
    @Test
    @DisplayName("Czytelnik bez opóźnienia między operacjami")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void readerWithZeroSleepBetweenOperations() throws InterruptedException {
        Reader reader = new Reader(library, "Czytelnik-1", 50, 100, 0);
        
        assertEquals(0, reader.getSleepBetweenOperations());
        
        reader.start();
        Thread.sleep(300);
        reader.stopRunning();
        reader.join(1000);
        
        assertFalse(reader.isAlive());
    }
    
    @Test
    @DisplayName("Czytelnik z ujemnym opóźnieniem między operacjami")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void readerWithNegativeSleepBetweenOperations() throws InterruptedException {
        Reader reader = new Reader(library, "Czytelnik-1", 50, 100, -10);
        
        assertEquals(-10, reader.getSleepBetweenOperations());
        
        reader.start();
        Thread.sleep(300);
        reader.stopRunning();
        reader.join(1000);
        
        assertFalse(reader.isAlive());
    }
}
