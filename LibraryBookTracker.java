import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LibraryBookTracker {
    private static int validRecords = 0;
    private static int searchResults = 0;
    private static int booksAdded = 0;
    private static int errorCount = 0;

    public static void main(String[] args) {
        List<Book> catalog = new ArrayList<>();
        
        try {
            // 1. التحقق من المعاملات (args)
            if (args.length < 2) throw new InsufficientArgumentsException("Fewer than two arguments provided.");
            if (!args[0].endsWith(".txt")) throw new InvalidFileNameException("First argument does not end with .txt");

            File catalogFile = new File(args[0]);
            if (!catalogFile.exists()) {
                catalogFile.createNewFile();
            }

            // 2. قراءة الملف
            loadCatalog(catalogFile, catalog);

            // 3. تنفيذ العملية المطلوبة
            String op = args[1];
            if (op.contains(":") && op.split(":").length == 4) {
                addNewBook(op, catalog, catalogFile);
            } else if (op.length() == 13 && op.matches("\\d+")) {
                searchByISBN(op, catalog);
            } else {
                searchByTitle(op, catalog);
            }

            // 4. طباعة الإحصائيات
            printStatistics();

        } catch (Exception e) {
            logError("Main Process", e);
            System.err.println("Critical Error: " + e.getMessage());
        } finally {
            System.out.println("\nThank you for using the Library Book Tracker.");
        }
    }

    private static void loadCatalog(File file, List<Book> catalog) {
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.trim().isEmpty()) continue;
                try {
                    String[] p = line.split(":");
                    if (p.length != 4) throw new MalformedBookEntryException(line);
                    catalog.add(new Book(p[0], p[1], p[2], Integer.parseInt(p[3])));
                    validRecords++;
                } catch (Exception e) {
                    logError(line, e);
                }
            }
        } catch (IOException e) {
            logError("File Access", e);
        }
    }

    private static void searchByTitle(String keyword, List<Book> catalog) {
        printHeader();
        for (Book b : catalog) {
            if (b.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                printBookRow(b);
                searchResults++;
            }
        }
    }

    private static void searchByISBN(String isbn, List<Book> catalog) throws DuplicateISBNException {
        List<Book> found = new ArrayList<>();
        for (Book b : catalog) {
            if (b.getIsbn().equals(isbn)) found.add(b);
        }
        if (found.size() > 1) throw new DuplicateISBNException("ISBN: " + isbn);
        
        printHeader();
        if (!found.isEmpty()) {
            printBookRow(found.get(0));
            searchResults = 1;
        }
    }

    private static void addNewBook(String data, List<Book> catalog, File file) {
        try {
            String[] p = data.split(":");
            Book b = new Book(p[0], p[1], p[2], Integer.parseInt(p[3]));
            catalog.add(b);
            Collections.sort(catalog);
            saveToFile(file, catalog);
            printHeader();
            printBookRow(b);
            booksAdded = 1;
        } catch (Exception e) {
            logError(data, e);
        }
    }

    private static void saveToFile(File file, List<Book> catalog) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (Book b : catalog) pw.println(b.toString());
        }
    }

    private static void logError(String text, Exception e) {
        errorCount++;
        try (PrintWriter out = new PrintWriter(new FileWriter("errors.log", true))) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.printf("[%s] ERROR in: %s - %s\n", time, text, e.toString());
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