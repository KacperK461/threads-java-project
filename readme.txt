================================================================================
        PROBLEM CZYTELNIKÓW I PISARZY - DOKUMENTACJA PROJEKTU
================================================================================

1. OMÓWIENIE ALGORYTMU
--------------------------------------------------------------------------------

Implementacja klasycznego problemu synchronizacji wielowątkowej z użyciem
mechanizmów Java: ReentrantLock, Condition oraz kolejki FIFO.

ZASADY DOSTĘPU DO CZYTELNI:
- Maksymalnie 5 czytelników może jednocześnie przebywać w czytelni
- Tylko 1 pisarz może przebywać w czytelni (na wyłączność)
- Pisarz wchodzi tylko gdy czytelnia jest całkowicie pusta
- Czytelnik nie może wejść jeśli pisarz jest w środku lub w kolejce przed nim

MECHANIZM SYNCHRONIZACJI:
- ReentrantLock(true) - zamek z gwarancją kolejności FIFO (fair lock)
- Condition - mechanizm await/signal dla wątków oczekujących
- Kolejka FIFO (LinkedList<WaitingEntity>) - przechowuje kolejność zgłoszeń

KOMUNIKACJA MIĘDZY WĄTKAMI:
1. Wątek wywołuje startReading() lub startWriting()
2. Dodaje się do kolejki FIFO z własnym obiektem Condition
3. Sprawdza warunki wejścia (canReaderEnter/canWriterEnter)
4. Jeśli NIE może wejść: await() na swoim Condition (czeka)
5. Gdy inny wątek wychodzi: wywołuje signalNext() - budzi wszystkie wątki
6. Obudzone wątki ponownie sprawdzają warunki
7. Jeśli MOŻE wejść: usuwa się z kolejki, zwiększa licznik, wchodzi
8. Po zakończeniu: wywołuje stopReading/stopWriting, budzi innych

ZAPOBIEGANIE ZAGŁODZENIU:
- Kolejka FIFO gwarantuje obsługę w kolejności zgłoszeń
- Czytelnicy sprawdzają czy nie ma pisarza przed nimi w kolejce
- Pisarze muszą być pierwsi w kolejce aby wejść

BEZPIECZEŃSTWO WĄTKOWE:
- Wszystkie operacje na współdzielonych zasobach chronione lock.lock()
- Liczniki atomowe: activeReaders, activeWriters
- volatile boolean running dla bezpiecznego zatrzymania wątków


2. SPOSÓB URUCHOMIENIA
--------------------------------------------------------------------------------

KOMPILACJA:
  mvn clean package

URUCHOMIENIE:
  java -jar target/readers-writers-1.0-SNAPSHOT.jar

PRZYKŁADY:
  java -jar target/readers-writers-1.0-SNAPSHOT.jar 5 2
  java -jar target/readers-writers-1.0-SNAPSHOT.jar 10 3 500 2000 300

PARAMETRY (opcjonalne):
  [1] liczbaCzytelników  - liczba wątków czytelników (domyślnie: 10)
  [2] liczbaPisarzy      - liczba wątków pisarzy (domyślnie: 3)
  [3] minCzas            - minimalny czas w czytelni w ms (domyślnie: 1000)
  [4] maxCzas            - maksymalny czas w czytelni w ms (domyślnie: 3000)
  [5] czasSnu            - czas przerwy między operacjami w ms (domyślnie: 500)

ZATRZYMANIE:
  Ctrl+C - graceful shutdown z zamknięciem wszystkich wątków
