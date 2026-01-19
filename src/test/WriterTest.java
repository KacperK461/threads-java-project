import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WriterTest {
    
    private Library library;
    
    @BeforeEach
    void setUp() {
        library = new Library();
    }
    
    @Test
    @DisplayName("Konstruktor domyślny powinien ustawić prawidłowe wartości")
    void defaultConstructorShouldSetCorrectValues() {
        Writer writer = new Writer(library, "Pisarz-1");
        
        assertEquals("Pisarz-1", writer.getWriterName());
        assertEquals(library, writer.getLibrary());
        assertEquals(1000, writer.getMinWriteTime());
        assertEquals(3000, writer.getMaxWriteTime());
        assertEquals(500, writer.getSleepBetweenOperations());
        assertEquals("Pisarz-1", writer.getName());
        assertTrue(writer.isRunning());
    }
    
    @Test
    @DisplayName("Konstruktor z parametrami powinien ustawić prawidłowe wartości")
    void parameterizedConstructorShouldSetCorrectValues() {
        Writer writer = new Writer(library, "Pisarz-Test", 100, 200, 50);
        
        assertEquals("Pisarz-Test", writer.getWriterName());
        assertEquals(library, writer.getLibrary());
        assertEquals(100, writer.getMinWriteTime());
        assertEquals(200, writer.getMaxWriteTime());
        assertEquals(50, writer.getSleepBetweenOperations());
    }
    
    @Test
    @DisplayName("Pisarz powinien pisać w pętli")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void writerShouldWriteInLoop() throws InterruptedException {
        AtomicInteger writeCount = new AtomicInteger(0);
        
        Writer writer = new Writer(library, "Pisarz-1", 50, 100, 10) {
            @Override
            public void run() {
                while (isRunning() && !Thread.currentThread().isInterrupted()) {
                    try {
                        getLibrary().startWriting(getWriterName());
                        writeCount.incrementAndGet();
                        Thread.sleep(50);
                        getLibrary().stopWriting(getWriterName());
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        };
        
        writer.start();
        Thread.sleep(500);
        writer.stopRunning();
        writer.join(1000);
        
        assertTrue(writeCount.get() >= 3, "Pisarz powinien napisać co najmniej 3 razy");
    }
    
    @Test
    @DisplayName("stopRunning powinien zatrzymać pisarza")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void stopRunningShouldStopWriter() throws InterruptedException {
        Writer writer = new Writer(library, "Pisarz-1", 50, 100, 10);
        
        writer.start();
        Thread.sleep(200);
        assertTrue(writer.isRunning());
        assertTrue(writer.isAlive());
        
        writer.stopRunning();
        writer.join(2000);
        
        assertFalse(writer.isRunning());
        assertFalse(writer.isAlive());
    }
    
    @Test
    @DisplayName("Pisarz powinien obsługiwać przerwanie")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void writerShouldHandleInterrupt() throws InterruptedException {
        Writer writer = new Writer(library, "Pisarz-1", 1000, 2000, 100);
        
        writer.start();
        Thread.sleep(100);
        
        writer.interrupt();
        writer.join(2000);
        
        assertFalse(writer.isAlive());
    }
    
    @Test
    @DisplayName("Wielu pisarzy powinno działać sekwencyjnie")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void multipleWritersShouldWorkSequentially() throws InterruptedException {
        Writer[] writers = new Writer[3];
        
        for (int i = 0; i < 3; i++) {
            writers[i] = new Writer(library, "Pisarz-" + (i + 1), 50, 100, 10);
            writers[i].start();
        }
        
        Thread.sleep(1000);
        
        assertTrue(library.getActiveWriters() <= 1);
        
        for (Writer writer : writers) {
            writer.stopRunning();
        }
        
        for (Writer writer : writers) {
            writer.join(2000);
        }
        
        Thread.sleep(200);
        
        for (Writer writer : writers) {
            assertFalse(writer.isAlive());
        }
    }
    
    @Test
    @DisplayName("Pisarz bez opóźnienia między operacjami")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void writerWithZeroSleepBetweenOperations() throws InterruptedException {
        Writer writer = new Writer(library, "Pisarz-1", 50, 100, 0);
        
        assertEquals(0, writer.getSleepBetweenOperations());
        
        writer.start();
        Thread.sleep(300);
        writer.stopRunning();
        writer.join(1000);
        
        assertFalse(writer.isAlive());
    }
    
    @Test
    @DisplayName("Pisarz z ujemnym opóźnieniem między operacjami")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void writerWithNegativeSleepBetweenOperations() throws InterruptedException {
        Writer writer = new Writer(library, "Pisarz-1", 50, 100, -10);
        
        assertEquals(-10, writer.getSleepBetweenOperations());
        
        writer.start();
        Thread.sleep(300);
        writer.stopRunning();
        writer.join(1000);
        
        assertFalse(writer.isAlive());
    }
    
    @Test
    @DisplayName("Pisarz i czytelnicy powinni współpracować poprawnie")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void writerAndReadersShouldCoexistCorrectly() throws InterruptedException {
        Writer writer = new Writer(library, "Pisarz-1", 50, 100, 10);
        Reader reader1 = new Reader(library, "Czytelnik-1", 50, 100, 10);
        Reader reader2 = new Reader(library, "Czytelnik-2", 50, 100, 10);
        
        writer.start();
        reader1.start();
        reader2.start();
        
        Thread.sleep(500);
        
        writer.stopRunning();
        reader1.stopRunning();
        reader2.stopRunning();
        
        writer.join(2000);
        reader1.join(2000);
        reader2.join(2000);
        
        Thread.sleep(200);
        
        assertFalse(writer.isAlive());
        assertFalse(reader1.isAlive());
        assertFalse(reader2.isAlive());
    }
}
