import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    
    @BeforeEach
    void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }
    
    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    @DisplayName("Domyślne wartości powinny być prawidłowe")
    void defaultValuesShouldBeCorrect() {
        assertEquals(10, Main.getDefaultReaders());
        assertEquals(3, Main.getDefaultWriters());
        assertEquals(1000, Main.getDefaultMinTime());
        assertEquals(3000, Main.getDefaultMaxTime());
        assertEquals(500, Main.getDefaultSleepTime());
    }
    
    @Test
    @DisplayName("parseArgument powinien zwrócić wartość domyślną dla null")
    void parseArgumentShouldReturnDefaultForNull() {
        int result = Main.parseArgument(null, 0, 42, "test");
        assertEquals(42, result);
    }
    
    @Test
    @DisplayName("parseArgument powinien zwrócić wartość domyślną dla pustej tablicy")
    void parseArgumentShouldReturnDefaultForEmptyArray() {
        int result = Main.parseArgument(new String[]{}, 0, 42, "test");
        assertEquals(42, result);
    }
    
    @Test
    @DisplayName("parseArgument powinien zwrócić wartość domyślną dla indeksu poza zakresem")
    void parseArgumentShouldReturnDefaultForIndexOutOfBounds() {
        int result = Main.parseArgument(new String[]{"1"}, 5, 42, "test");
        assertEquals(42, result);
    }
    
    @Test
    @DisplayName("parseArgument powinien sparsować prawidłową wartość")
    void parseArgumentShouldParseValidValue() {
        int result = Main.parseArgument(new String[]{"123"}, 0, 42, "test");
        assertEquals(123, result);
    }
    
    @Test
    @DisplayName("parseArgument powinien zwrócić wartość domyślną dla nieprawidłowej wartości")
    void parseArgumentShouldReturnDefaultForInvalidValue() {
        int result = Main.parseArgument(new String[]{"abc"}, 0, 42, "test");
        assertEquals(42, result);
        assertTrue(errContent.toString().contains("Nieprawidłowa wartość"));
    }
    
    @Test
    @DisplayName("parseArgument powinien obsługiwać wiele argumentów")
    void parseArgumentShouldHandleMultipleArguments() {
        String[] args = {"10", "5", "2000", "4000", "1000"};
        
        assertEquals(10, Main.parseArgument(args, 0, 0, "readers"));
        assertEquals(5, Main.parseArgument(args, 1, 0, "writers"));
        assertEquals(2000, Main.parseArgument(args, 2, 0, "minTime"));
        assertEquals(4000, Main.parseArgument(args, 3, 0, "maxTime"));
        assertEquals(1000, Main.parseArgument(args, 4, 0, "sleepTime"));
    }
    
    @Test
    @DisplayName("parseArgument powinien obsługiwać liczby ujemne")
    void parseArgumentShouldHandleNegativeNumbers() {
        int result = Main.parseArgument(new String[]{"-5"}, 0, 42, "test");
        assertEquals(-5, result);
    }
    
    @Test
    @DisplayName("parseArgument powinien obsługiwać zero")
    void parseArgumentShouldHandleZero() {
        int result = Main.parseArgument(new String[]{"0"}, 0, 42, "test");
        assertEquals(0, result);
    }
    
    @Test
    @DisplayName("parseArgument powinien obsługiwać duże liczby")
    void parseArgumentShouldHandleLargeNumbers() {
        int result = Main.parseArgument(new String[]{"999999"}, 0, 42, "test");
        assertEquals(999999, result);
    }
    
    @Test
    @DisplayName("parseArgument powinien obsługiwać wartość z białymi znakami")
    void parseArgumentShouldHandleWhitespace() {
        int result = Main.parseArgument(new String[]{" 123"}, 0, 42, "test");
        assertEquals(42, result);
    }
    
    @Test
    @DisplayName("parseArgument powinien obsługiwać wartość zmiennoprzecinkową jako błąd")
    void parseArgumentShouldHandleFloatAsError() {
        int result = Main.parseArgument(new String[]{"12.5"}, 0, 42, "test");
        assertEquals(42, result);
        assertTrue(errContent.toString().contains("Nieprawidłowa wartość"));
    }
}
