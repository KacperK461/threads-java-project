import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    
    @Test
    @DisplayName("Pełny scenariusz z wieloma czytelnikami i pisarzami")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void fullScenarioWithMultipleReadersAndWriters() throws InterruptedException {
        Library library = new Library();
        List<Reader> readers = new ArrayList<>();
        List<Writer> writers = new ArrayList<>();
        
        for (int i = 1; i <= 10; i++) {
            readers.add(new Reader(library, "Czytelnik-" + i, 50, 100, 20));
        }
        for (int i = 1; i <= 3; i++) {
            writers.add(new Writer(library, "Pisarz-" + i, 50, 100, 20));
        }
        
        for (Reader reader : readers) {
            reader.start();
        }
        for (Writer writer : writers) {
            writer.start();
        }
        
        Thread.sleep(2000);
        
        for (Reader reader : readers) {
            reader.stopRunning();
        }
        for (Writer writer : writers) {
            writer.stopRunning();
        }
        
        for (Reader reader : readers) {
            reader.join(5000);
        }
        for (Writer writer : writers) {
            writer.join(5000);
        }
        
        Thread.sleep(500);
        
        for (Reader reader : readers) {
            assertFalse(reader.isAlive(), "Czytelnik " + reader.getReaderName() + " powinien się zakończyć");
        }
        for (Writer writer : writers) {
            assertFalse(writer.isAlive(), "Pisarz " + writer.getWriterName() + " powinien się zakończyć");
        }
    }
    
    @Test
    @DisplayName("Test sprawiedliwości - wszyscy powinni mieć dostęp")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void fairnessTest() throws InterruptedException {
        Library library = new Library();
        
        AtomicInteger reader1Count = new AtomicInteger(0);
        AtomicInteger reader2Count = new AtomicInteger(0);
        AtomicInteger writerCount = new AtomicInteger(0);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);
        
        Thread reader1 = new Thread(() -> {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                return;
            }
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                library.startReading("Czytelnik-1");
                reader1Count.incrementAndGet();
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                library.stopReading("Czytelnik-1");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        Thread reader2 = new Thread(() -> {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                return;
            }
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                library.startReading("Czytelnik-2");
                reader2Count.incrementAndGet();
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                library.stopReading("Czytelnik-2");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        Thread writer = new Thread(() -> {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                return;
            }
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                library.startWriting("Pisarz-1");
                writerCount.incrementAndGet();
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                library.stopWriting("Pisarz-1");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        reader1.start();
        reader2.start();
        writer.start();
        
        startLatch.countDown();
        
        Thread.sleep(3000);
        
        running.set(false);
        
        reader1.interrupt();
        reader2.interrupt();
        writer.interrupt();
        
        reader1.join(2000);
        reader2.join(2000);
        writer.join(2000);
        
        assertTrue(reader1Count.get() > 5, "Czytelnik 1 powinien wejść więcej niż 5 razy, wejść: " + reader1Count.get());
        assertTrue(reader2Count.get() > 5, "Czytelnik 2 powinien wejść więcej niż 5 razy, wejść: " + reader2Count.get());
        assertTrue(writerCount.get() > 5, "Pisarz powinien wejść więcej niż 5 razy, wejść: " + writerCount.get());
    }
    
    @Test
    @DisplayName("Test kolejności FIFO - szczegółowy")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void detailedFifoTest() throws InterruptedException {
        Library library = new Library();
        List<String> entryOrder = Collections.synchronizedList(new ArrayList<>());
        
        library.startWriting("Pisarz-Bloker");
        
        Thread r1 = new Thread(() -> {
            library.startReading("R1");
            entryOrder.add("R1");
        });
        
        Thread w1 = new Thread(() -> {
            library.startWriting("W1");
            entryOrder.add("W1");
        });
        
        Thread r2 = new Thread(() -> {
            library.startReading("R2");
            entryOrder.add("R2");
        });
        
        Thread r3 = new Thread(() -> {
            library.startReading("R3");
            entryOrder.add("R3");
        });
        
        r1.start();
        Thread.sleep(50);
        w1.start();
        Thread.sleep(50);
        r2.start();
        Thread.sleep(50);
        r3.start();
        Thread.sleep(100);
        
        library.stopWriting("Pisarz-Bloker");
        
        Thread.sleep(200);
        
        assertEquals("R1", entryOrder.get(0));
        
        Thread.sleep(100);
        library.stopReading("R1");
        
        Thread.sleep(200);
        assertEquals("W1", entryOrder.get(1));
        
        library.stopWriting("W1");
        
        Thread.sleep(200);
        
        assertTrue(entryOrder.contains("R2"));
        assertTrue(entryOrder.contains("R3"));
        
        library.stopReading("R2");
        library.stopReading("R3");
        
        r1.join(1000);
        w1.join(1000);
        r2.join(1000);
        r3.join(1000);
    }
    
    @Test
    @DisplayName("Test maksymalnej liczby czytelników")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void maxReadersTest() throws InterruptedException {
        Library library = new Library();
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        CountDownLatch allReady = new CountDownLatch(7);
        CountDownLatch canExit = new CountDownLatch(1);
        
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 1; i <= 7; i++) {
            final String name = "R" + i;
            Thread t = new Thread(() -> {
                library.startReading(name);
                maxConcurrent.updateAndGet(v -> Math.max(v, library.getActiveReaders()));
                allReady.countDown();
                try {
                    canExit.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                library.stopReading(name);
            });
            threads.add(t);
            t.start();
        }
        
        Thread.sleep(500);
        
        assertEquals(5, library.getActiveReaders());
        assertEquals(2, library.getQueueSize());
        assertTrue(maxConcurrent.get() <= 5);
        
        canExit.countDown();
        
        for (Thread t : threads) {
            t.join(2000);
        }
        
        assertTrue(library.isEmpty());
    }
    
    @Test
    @DisplayName("Test wyłączności pisarza")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void writerExclusivityTest() throws InterruptedException {
        Library library = new Library();
        AtomicBoolean violationDetected = new AtomicBoolean(false);
        
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            final String name = "R" + i;
            Thread t = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    library.startReading(name);
                    if (library.getActiveWriters() > 0) {
                        violationDetected.set(true);
                    }
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    library.stopReading(name);
                }
            });
            threads.add(t);
        }
        
        for (int i = 1; i <= 2; i++) {
            final String name = "W" + i;
            Thread t = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    library.startWriting(name);
                    if (library.getActiveReaders() > 0 || library.getActiveWriters() > 1) {
                        violationDetected.set(true);
                    }
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    library.stopWriting(name);
                }
            });
            threads.add(t);
        }
        
        for (Thread t : threads) {
            t.start();
        }
        
        for (Thread t : threads) {
            t.join(10000);
        }
        
        assertFalse(violationDetected.get(), "Wykryto naruszenie wyłączności pisarza");
    }
}
