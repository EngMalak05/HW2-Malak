import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class BookCatalogException extends Exception {
    public BookCatalogException(String message) { super(message); }
}

class InvalidISBNException extends BookCatalogException {
    public InvalidISBNException(String message) { super(message); }
}

class MalformedBookEntryException extends BookCatalogException {
    public MalformedBookEntryException(String message) { super(message); }
}

class Book implements Comparable<Book> {
    private String title, author, isbn;
    private int copies;

    public Book(String title, String author, String isbn, int copies) throws InvalidISBNException {
        if (isbn.length() != 13 || !isbn.matches("\\d+")) {
            throw new InvalidISBNException("ISBN must be 13 digits.");
        }
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.copies = copies;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getCopies() { return copies; }

    @Override
    public int compareTo(Book other) {
        return this.title.compareToIgnoreCase(other.title);
    }

    @Override
    public String toString() {
        return title + ":" + author + ":" + isbn + ":" + copies;
    }
}

public class LibraryBookTracker {
    private static List<Book> catalog = Collections.synchronizedList(new ArrayList<>());
    private static String fileName;
    private static String operation;
    private static int validRecords = 0;
    private static int searchResults = 0;
    private static int booksAdded = 0;
    private static int errorCount = 0;

    public static void main(String[] args) {
        try {
            if (args.length < 2) throw new Exception("Usage: java LibraryBookTracker <file.txt> <operation>");
            fileName = args[0];
            operation = args[1];

            Thread fileThread = new Thread(new FileReaderTask());
            Thread opThread = new Thread(new OperationTask());

            fileThread.start();
            fileThread.join();

            opThread.start();
            opThread.join();

            printStatistics();

        } catch (Exception e) {
            logError("Main", e);
        } finally {
            System.out.println("\nThank you for using the Library Book Tracker.");
        }
    }

    static class FileReaderTask implements Runnable {
        public void run() {
            File file = new File(fileName);
            if (!file.exists()) {
                try { file.createNewFile(); } catch (IOException ignored) {}
            }
            try (Scanner sc = new Scanner(file)) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    if (line.trim().isEmpty()) continue;
                    try {
                        String[] p = line.split(":");
                        if (p.length != 4) throw new MalformedBookEntryException(line);
                        catalog.add(new Book(p[0], p[1], p[2], Integer.parseInt(p[3].trim())));
                        validRecords++;
                    } catch (Exception e) {
                        logError(line, e);
                    }
                }
            } catch (FileNotFoundException ignored) {}
        }
    }

    static class OperationTask implements Runnable {
        public void run() {
            if (operation.contains(":") && operation.split(":").length == 4) {
                addNewBook(operation);
            } else if (operation.length() == 13 && operation.matches("\\d+")) {
                searchByISBN(operation);
            } else {
                searchByTitle(operation);
            }
        }
    }

    private static void searchByTitle(String keyword) {
        printHeader();
        synchronized (catalog) {
            for (Book b : catalog) {
                if (b.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                    printBookRow(b);
                    searchResults++;
                }
            }
        }
    }

    private static void searchByISBN(String isbn) {
        printHeader();
        synchronized (catalog) {
            for (Book b : catalog) {
                if (b.getIsbn().equals(isbn)) {
                    printBookRow(b);
                    searchResults = 1;
                    break;
                }
            }
        }
    }

    private static void addNewBook(String data) {
        try {
            String[] p = data.split(":");
            Book newBook = new Book(p[0], p[1], p[2], Integer.parseInt(p[3].trim()));
            catalog.add(newBook);
            Collections.sort(catalog);
            saveToFile();
            printHeader();
            printBookRow(newBook);
            booksAdded = 1;
        } catch (Exception e) {
            logError(data, e);
        }
    }

    private static void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            synchronized (catalog) {
                for (Book b : catalog) pw.println(b.toString());
            }
        } catch (IOException ignored) {}
    }

    private static void logError(String text, Exception e) {
        errorCount++;
        try (PrintWriter out = new PrintWriter(new FileWriter("errors.log", true))) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.printf("[%s] ERROR: %s - %s\n", time, text, e.toString());
        } catch (IOException ignored) {}
    }

    private static void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s\n", "Title", "Author", "ISBN", "Copies");
        System.out.println("---------------------------------------------------------------------------");
    }

    private static void printBookRow(Book b) {
        System.out.printf("%-30s %-20s %-15s %5d\n", b.getTitle(), b.getAuthor(), b.getIsbn(), b.getCopies());
    }

    private static void printStatistics() {
        System.out.println("\n--- FINAL STATISTICS ---");
        System.out.println("Records Processed: " + validRecords);
        System.out.println("Search Results: " + searchResults);
        System.out.println("Books Added: " + booksAdded);
        System.out.println("Errors Logged: " + errorCount);
    }
}