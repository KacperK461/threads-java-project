import java.util.ArrayList;
import java.util.List;

public class Main {
    
    private static final int DEFAULT_READERS = 10;
    private static final int DEFAULT_WRITERS = 3;
    private static final int DEFAULT_MIN_TIME = 1000;
    private static final int DEFAULT_MAX_TIME = 3000;
    private static final int DEFAULT_SLEEP_TIME = 500;
    
    public static void main(String[] args) {
        int numReaders = parseArgument(args, 0, DEFAULT_READERS, "liczba czytelników");
        int numWriters = parseArgument(args, 1, DEFAULT_WRITERS, "liczba pisarzy");
        int minTime = parseArgument(args, 2, DEFAULT_MIN_TIME, "minimalny czas");
        int maxTime = parseArgument(args, 3, DEFAULT_MAX_TIME, "maksymalny czas");
        int sleepTime = parseArgument(args, 4, DEFAULT_SLEEP_TIME, "czas snu");
        
        if (numReaders < 0 || numWriters < 0) {
            System.err.println("Liczba czytelników i pisarzy musi być nieujemna!");
            System.exit(1);
        }
        
        if (minTime > maxTime) {
            System.err.println("Minimalny czas nie może być większy niż maksymalny!");
            System.exit(1);
        }
        
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║     PROBLEM CZYTELNIKÓW I PISARZY - DEMONSTRACJA       ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║ Parametry:                                             ║");
        System.out.printf("║   Liczba czytelników: %-33d ║%n", numReaders);
        System.out.printf("║   Liczba pisarzy: %-37d ║%n", numWriters);
        System.out.printf("║   Czas w czytelni: %d-%d ms %-23s ║%n", minTime, maxTime, "");
        System.out.printf("║   Czas snu między operacjami: %-25d ║%n", sleepTime);
        System.out.println("║                                                        ║");
        System.out.println("║ Zasady czytelni:                                       ║");
        System.out.println("║   - Maksymalnie 5 czytelników jednocześnie             ║");
        System.out.println("║   - Tylko 1 pisarz na wyłączność                       ║");
        System.out.println("║   - Kolejkowanie FIFO (brak zagłodzenia)               ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║ Naciśnij Ctrl+C aby zakończyć program                  ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        
        Library library = new Library();
        
        List<Reader> readers = new ArrayList<>();
        List<Writer> writers = new ArrayList<>();
        
        for (int i = 1; i <= numReaders; i++) {
            Reader reader = new Reader(library, "Czytelnik-" + i, minTime, maxTime, sleepTime);
            readers.add(reader);
        }
        
        for (int i = 1; i <= numWriters; i++) {
            Writer writer = new Writer(library, "Pisarz-" + i, minTime, maxTime, sleepTime);
            writers.add(writer);
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\nZatrzymywanie wątków...");
            
            for (Reader reader : readers) {
                reader.stopRunning();
            }
            for (Writer writer : writers) {
                writer.stopRunning();
            }
            
            for (Reader reader : readers) {
                try {
                    reader.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            for (Writer writer : writers) {
                try {
                    writer.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            System.out.println("Program zakończony.");
        }));
        
        for (Reader reader : readers) {
            reader.start();
        }
        for (Writer writer : writers) {
            writer.start();
        }
        
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    static int parseArgument(String[] args, int index, int defaultValue, String name) {
        if (args == null || args.length <= index) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            System.err.println("Nieprawidłowa wartość dla parametru '" + name + "': " + args[index]);
            System.err.println("Używam wartości domyślnej: " + defaultValue);
            return defaultValue;
        }
    }
    
    public static int getDefaultReaders() {
        return DEFAULT_READERS;
    }
    
    public static int getDefaultWriters() {
        return DEFAULT_WRITERS;
    }
    
    public static int getDefaultMinTime() {
        return DEFAULT_MIN_TIME;
    }
    
    public static int getDefaultMaxTime() {
        return DEFAULT_MAX_TIME;
    }
    
    public static int getDefaultSleepTime() {
        return DEFAULT_SLEEP_TIME;
    }
}
