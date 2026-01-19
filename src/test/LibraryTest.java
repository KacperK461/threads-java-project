import org.junit.jupiter.api.BeforeEach;
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

class LibraryTest {
    
    private Library library;
    
    @BeforeEach
    void setUp() {
        library = new Library();
    }
    
    @Test
    @DisplayName("Nowa czytelnia powinna być pusta")
    void newLibraryShouldBeEmpty() {
        assertTrue(library.isEmpty());
        assertEquals(0, library.getActiveReaders());
        assertEquals(0, library.getActiveWriters());
        assertEquals(0, library.getQueueSize());
        assertNull(library.getWriterInLibrary());
        assertTrue(library.getReadersInLibrary().isEmpty());
    }
    
    @Test
    @DisplayName("Maksymalna liczba czytelników powinna wynosić 5")
    void maxReadersShouldBe5() {
        assertEquals(5, library.getMaxReaders());
    }
    
    @Test
    @DisplayName("Pojedynczy czytelnik powinien móc wejść i wyjść")
    void singleReaderShouldEnterAndExit() {
        library.startReading("Czytelnik-1");
        
        assertEquals(1, library.getActiveReaders());
        assertEquals(0, library.getActiveWriters());
        assertFalse(library.isEmpty());
        assertTrue(library.getReadersInLibrary().contains("Czytelnik-1"));
        
        library.stopReading("Czytelnik-1");
        
        assertEquals(0, library.getActiveReaders());
        assertTrue(library.isEmpty());
        assertFalse(library.getReadersInLibrary().contains("Czytelnik-1"));
    }
    
    @Test
    @DisplayName("Pojedynczy pisarz powinien móc wejść i wyjść")
    void singleWriterShouldEnterAndExit() {
        library.startWriting("Pisarz-1");
        
        assertEquals(0, library.getActiveReaders());
        assertEquals(1, library.getActiveWriters());
        assertFalse(library.isEmpty());
        assertEquals("Pisarz-1", library.getWriterInLibrary());
        
        library.stopWriting("Pisarz-1");
        
        assertEquals(0, library.getActiveWriters());
        assertTrue(library.isEmpty());
        assertNull(library.getWriterInLibrary());
    }
    
    @Test
    @DisplayName("Maksymalnie 5 czytelników powinno móc wejść jednocześnie")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void maxFiveReadersShouldEnterSimultaneously() throws InterruptedException {
        CountDownLatch allEntered = new CountDownLatch(5);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    startSignal.await();
                    library.startReading("Czytelnik-" + id);
                    allEntered.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(t);
            t.start();
        }
        
        startSignal.countDown();
        assertTrue(allEntered.await(5, TimeUnit.SECONDS));
        assertEquals(5, library.getActiveReaders());
        
        for (int i = 1; i <= 5; i++) {
            library.stopReading("Czytelnik-" + i);
        }
        
        for (Thread t : threads) {
            t.join(1000);
        }
    }
    
    @Test
    @DisplayName("Szósty czytelnik powinien czekać gdy jest 5 czytelników")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void sixthReaderShouldWaitWhenFiveReadersAreIn() throws InterruptedException {
        for (int i = 1; i <= 5; i++) {
            library.startReading("Czytelnik-" + i);
        }
        
        assertEquals(5, library.getActiveReaders());
        
        AtomicBoolean sixthEntered = new AtomicBoolean(false);
        Thread sixthReader = new Thread(() -> {
            library.startReading("Czytelnik-6");
            sixthEntered.set(true);
        });
        sixthReader.start();
        
        Thread.sleep(200);
        assertFalse(sixthEntered.get());
        assertEquals(1, library.getQueueSize());
        
        library.stopReading("Czytelnik-1");
        
        Thread.sleep(200);
        assertTrue(sixthEntered.get());
        assertEquals(5, library.getActiveReaders());
        
        for (int i = 2; i <= 6; i++) {
            library.stopReading("Czytelnik-" + i);
        }
        
        sixthReader.join(1000);
    }
    
    @Test
    @DisplayName("Pisarz powinien czekać gdy są czytelnicy")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void writerShouldWaitWhenReadersAreIn() throws InterruptedException {
        library.startReading("Czytelnik-1");
        
        AtomicBoolean writerEntered = new AtomicBoolean(false);
        Thread writerThread = new Thread(() -> {
            library.startWriting("Pisarz-1");
            writerEntered.set(true);
        });
        writerThread.start();
        
        Thread.sleep(200);
        assertFalse(writerEntered.get());
        assertEquals(1, library.getQueueSize());
        
        library.stopReading("Czytelnik-1");
        
        Thread.sleep(200);
        assertTrue(writerEntered.get());
        assertEquals(1, library.getActiveWriters());
        
        library.stopWriting("Pisarz-1");
        writerThread.join(1000);
    }
    
    @Test
    @DisplayName("Czytelnik powinien czekać gdy jest pisarz")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void readerShouldWaitWhenWriterIsIn() throws InterruptedException {
        library.startWriting("Pisarz-1");
        
        AtomicBoolean readerEntered = new AtomicBoolean(false);
        Thread readerThread = new Thread(() -> {
            library.startReading("Czytelnik-1");
            readerEntered.set(true);
        });
        readerThread.start();
        
        Thread.sleep(200);
        assertFalse(readerEntered.get());
        assertEquals(1, library.getQueueSize());
        
        library.stopWriting("Pisarz-1");
        
        Thread.sleep(200);
        assertTrue(readerEntered.get());
        assertEquals(1, library.getActiveReaders());
        
        library.stopReading("Czytelnik-1");
        readerThread.join(1000);
    }
    
    @Test
    @DisplayName("Drugi pisarz powinien czekać gdy jest pierwszy pisarz")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void secondWriterShouldWaitWhenFirstWriterIsIn() throws InterruptedException {
        library.startWriting("Pisarz-1");
        
        AtomicBoolean secondWriterEntered = new AtomicBoolean(false);
        Thread secondWriter = new Thread(() -> {
            library.startWriting("Pisarz-2");
            secondWriterEntered.set(true);
        });
        secondWriter.start();
        
        Thread.sleep(200);
        assertFalse(secondWriterEntered.get());
        assertEquals(1, library.getQueueSize());
        
        library.stopWriting("Pisarz-1");
        
        Thread.sleep(200);
        assertTrue(secondWriterEntered.get());
        assertEquals(1, library.getActiveWriters());
        assertEquals("Pisarz-2", library.getWriterInLibrary());
        
        library.stopWriting("Pisarz-2");
        secondWriter.join(1000);
    }
    
    @Test
    @DisplayName("Kolejka FIFO powinna być zachowana - pisarz przed czytelnikami")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void fifoOrderShouldBeRespectedWriterBeforeReaders() throws InterruptedException {
        List<String> entryOrder = Collections.synchronizedList(new ArrayList<>());
        
        library.startReading("Czytelnik-1");
        
        Thread writerThread = new Thread(() -> {
            library.startWriting("Pisarz-1");
            entryOrder.add("Pisarz-1");
        });
        writerThread.start();
        Thread.sleep(100);
        
        Thread reader2Thread = new Thread(() -> {
            library.startReading("Czytelnik-2");
            entryOrder.add("Czytelnik-2");
        });
        reader2Thread.start();
        Thread.sleep(100);
        
        assertEquals(2, library.getQueueSize());
        
        library.stopReading("Czytelnik-1");
        
        Thread.sleep(300);
        
        assertEquals(1, entryOrder.size());
        assertEquals("Pisarz-1", entryOrder.get(0));
        
        library.stopWriting("Pisarz-1");
        
        Thread.sleep(200);
        
        assertEquals(2, entryOrder.size());
        assertEquals("Czytelnik-2", entryOrder.get(1));
        
        library.stopReading("Czytelnik-2");
        
        writerThread.join(1000);
        reader2Thread.join(1000);
    }
    
    @Test
    @DisplayName("Test przerywania wątku czytelnika podczas oczekiwania")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void interruptedReaderShouldLeaveQueue() throws InterruptedException {
        library.startWriting("Pisarz-1");
        
        Thread readerThread = new Thread(() -> {
            library.startReading("Czytelnik-1");
        });
        readerThread.start();
        
        Thread.sleep(100);
        assertEquals(1, library.getQueueSize());
        
        readerThread.interrupt();
        Thread.sleep(100);
        
        assertEquals(0, library.getQueueSize());
        
        library.stopWriting("Pisarz-1");
        readerThread.join(1000);
    }
    
    @Test
    @DisplayName("Test przerywania wątku pisarza podczas oczekiwania")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void interruptedWriterShouldLeaveQueue() throws InterruptedException {
        library.startReading("Czytelnik-1");
        
        Thread writerThread = new Thread(() -> {
            library.startWriting("Pisarz-1");
        });
        writerThread.start();
        
        Thread.sleep(100);
        assertEquals(1, library.getQueueSize());
        
        writerThread.interrupt();
        Thread.sleep(100);
        
        assertEquals(0, library.getQueueSize());
        
        library.stopReading("Czytelnik-1");
        writerThread.join(1000);
    }
    
    @Test
    @DisplayName("Test równoczesnego dostępu wielu wątków")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void concurrentAccessTest() throws InterruptedException {
        int numReaders = 10;
        int numWriters = 3;
        int iterations = 5;
        
        List<Thread> threads = new ArrayList<>();
        AtomicInteger maxConcurrentReaders = new AtomicInteger(0);
        AtomicInteger maxConcurrentWriters = new AtomicInteger(0);
        AtomicBoolean violationDetected = new AtomicBoolean(false);
        
        for (int i = 1; i <= numReaders; i++) {
            final String name = "Czytelnik-" + i;
            Thread t = new Thread(() -> {
                for (int j = 0; j < iterations && !Thread.currentThread().isInterrupted(); j++) {
                    library.startReading(name);
                    
                    int readers = library.getActiveReaders();
                    int writers = library.getActiveWriters();
                    
                    if (readers > 5) {
                        violationDetected.set(true);
                    }
                    if (writers > 0 && readers > 0) {
                        violationDetected.set(true);
                    }
                    
                    maxConcurrentReaders.updateAndGet(v -> Math.max(v, readers));
                    
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    library.stopReading(name);
                    
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            threads.add(t);
        }
        
        for (int i = 1; i <= numWriters; i++) {
            final String name = "Pisarz-" + i;
            Thread t = new Thread(() -> {
                for (int j = 0; j < iterations && !Thread.currentThread().isInterrupted(); j++) {
                    library.startWriting(name);
                    
                    int readers = library.getActiveReaders();
                    int writers = library.getActiveWriters();
                    
                    if (writers > 1) {
                        violationDetected.set(true);
                    }
                    if (readers > 0) {
                        violationDetected.set(true);
                    }
                    
                    maxConcurrentWriters.updateAndGet(v -> Math.max(v, writers));
                    
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    library.stopWriting(name);
                    
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            threads.add(t);
        }
        
        for (Thread t : threads) {
            t.start();
        }
        
        for (Thread t : threads) {
            t.join(20000);
        }
        
        assertFalse(violationDetected.get(), "Wykryto naruszenie zasad czytelni");
        assertTrue(maxConcurrentReaders.get() <= 5, "Zbyt wielu czytelników jednocześnie");
        assertTrue(maxConcurrentWriters.get() <= 1, "Zbyt wielu pisarzy jednocześnie");
        assertTrue(library.isEmpty(), "Czytelnia powinna być pusta po zakończeniu testów");
    }
    
    @Test
    @DisplayName("Test braku zagłodzenia pisarza")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void writerShouldNotStarve() throws InterruptedException {
        AtomicBoolean writerEntered = new AtomicBoolean(false);
        List<Thread> readerThreads = new ArrayList<>();
        AtomicBoolean keepReading = new AtomicBoolean(true);
        
        for (int i = 1; i <= 3; i++) {
            final String name = "Czytelnik-" + i;
            Thread t = new Thread(() -> {
                while (keepReading.get() && !Thread.currentThread().isInterrupted()) {
                    library.startReading(name);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    library.stopReading(name);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            readerThreads.add(t);
            t.start();
        }
        
        Thread.sleep(200);
        
        Thread writerThread = new Thread(() -> {
            library.startWriting("Pisarz-1");
            writerEntered.set(true);
            library.stopWriting("Pisarz-1");
        });
        writerThread.start();
        
        writerThread.join(10000);
        
        keepReading.set(false);
        
        for (Thread t : readerThreads) {
            t.interrupt();
            t.join(1000);
        }
        
        assertTrue(writerEntered.get(), "Pisarz powinien był wejść do czytelni");
    }
    
    @Test
    @DisplayName("Test braku zagłodzenia czytelnika")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void readerShouldNotStarve() throws InterruptedException {
        AtomicBoolean readerEntered = new AtomicBoolean(false);
        List<Thread> writerThreads = new ArrayList<>();
        AtomicBoolean keepWriting = new AtomicBoolean(true);
        
        for (int i = 1; i <= 2; i++) {
            final String name = "Pisarz-" + i;
            Thread t = new Thread(() -> {
                while (keepWriting.get() && !Thread.currentThread().isInterrupted()) {
                    library.startWriting(name);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    library.stopWriting(name);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            writerThreads.add(t);
            t.start();
        }
        
        Thread.sleep(200);
        
        Thread readerThread = new Thread(() -> {
            library.startReading("Czytelnik-1");
            readerEntered.set(true);
            library.stopReading("Czytelnik-1");
        });
        readerThread.start();
        
        readerThread.join(10000);
        
        keepWriting.set(false);
        
        for (Thread t : writerThreads) {
            t.interrupt();
            t.join(1000);
        }
        
        assertTrue(readerEntered.get(), "Czytelnik powinien był wejść do czytelni");
    }
    
    @Test
    @DisplayName("canReaderEnter powinien zwrócić true gdy brak czytelników w kolejce przed danym czytelnikiem")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void canReaderEnterShouldReturnTrueWhenNoReadersBeforeInQueue() throws InterruptedException {
        Thread readerThread = new Thread(() -> {
            library.startReading("Czytelnik-1");
            library.stopReading("Czytelnik-1");
        });
        
        readerThread.start();
        readerThread.join(2000);
        
        assertTrue(library.isEmpty());
    }
    
    @Test
    @DisplayName("canReaderEnter powinien zwrócić true gdy w kolejce są tylko czytelnicy")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void canReaderEnterShouldReturnTrueWhenOnlyReadersInQueue() throws InterruptedException {
        CountDownLatch reader1Entered = new CountDownLatch(1);
        CountDownLatch reader2Entered = new CountDownLatch(1);
        CountDownLatch reader3Entered = new CountDownLatch(1);
        
        Thread reader1 = new Thread(() -> {
            library.startReading("Czytelnik-1");
            reader1Entered.countDown();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            library.stopReading("Czytelnik-1");
        });
        
        Thread reader2 = new Thread(() -> {
            try {
                reader1Entered.await();
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            library.startReading("Czytelnik-2");
            reader2Entered.countDown();
            library.stopReading("Czytelnik-2");
        });
        
        Thread reader3 = new Thread(() -> {
            try {
                reader1Entered.await();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            library.startReading("Czytelnik-3");
            reader3Entered.countDown();
            library.stopReading("Czytelnik-3");
        });
        
        reader1.start();
        reader2.start();
        reader3.start();
        
        reader1.join(3000);
        reader2.join(3000);
        reader3.join(3000);
        
        assertTrue(reader1Entered.getCount() == 0);
        assertTrue(reader2Entered.getCount() == 0);
        assertTrue(reader3Entered.getCount() == 0);
    }
    
    @Test
    @DisplayName("canWriterEnter powinien zwrócić false gdy pisarz nie jest pierwszy w kolejce")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void canWriterEnterShouldReturnFalseWhenNotFirst() throws InterruptedException {
        CountDownLatch reader1Entered = new CountDownLatch(1);
        CountDownLatch writer1Blocked = new CountDownLatch(1);
        AtomicBoolean writerEntered = new AtomicBoolean(false);
        
        Thread reader1 = new Thread(() -> {
            library.startReading("Czytelnik-1");
            reader1Entered.countDown();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            library.stopReading("Czytelnik-1");
        });
        
        Thread writer1 = new Thread(() -> {
            try {
                reader1Entered.await();
                Thread.sleep(50);
                writer1Blocked.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            library.startWriting("Pisarz-1");
            writerEntered.set(true);
            library.stopWriting("Pisarz-1");
        });
        
        reader1.start();
        writer1.start();
        
        writer1Blocked.await(2000, TimeUnit.MILLISECONDS);
        
        assertFalse(writerEntered.get());
        
        reader1.join(3000);
        writer1.join(3000);
        
        assertTrue(writerEntered.get());
    }
    
    @Test
    @DisplayName("Czytelnia z pojedynczym pisarzem w kolejce")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void libraryShouldHandleSingleWriterInQueue() throws InterruptedException {
        AtomicBoolean writerEntered = new AtomicBoolean(false);
        
        Thread writer = new Thread(() -> {
            library.startWriting("Pisarz-1");
            writerEntered.set(true);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            library.stopWriting("Pisarz-1");
        });
        
        writer.start();
        writer.join(2000);
        
        assertTrue(writerEntered.get());
        assertTrue(library.isEmpty());
    }
    
    @Test
    @DisplayName("stopReading powinien wywołać signalNext z pustą kolejką")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void stopReadingShouldCallSignalNextWithEmptyQueue() throws InterruptedException {
        library.startReading("Czytelnik-1");
        assertEquals(1, library.getActiveReaders());
        
        library.stopReading("Czytelnik-1");
        
        assertEquals(0, library.getActiveReaders());
        assertEquals(0, library.getQueueSize());
        assertTrue(library.isEmpty());
    }
    
    @Test
    @DisplayName("stopWriting powinien wywołać signalNext z pustą kolejką")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void stopWritingShouldCallSignalNextWithEmptyQueue() throws InterruptedException {
        library.startWriting("Pisarz-1");
        assertEquals(1, library.getActiveWriters());
        
        library.stopWriting("Pisarz-1");
        
        assertEquals(0, library.getActiveWriters());
        assertEquals(0, library.getQueueSize());
        assertTrue(library.isEmpty());
    }
}
