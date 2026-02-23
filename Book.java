public class Book implements Comparable<Book> {
    private String title, author, isbn;
    private int copies;

    public Book(String title, String author, String isbn, int copies) throws InvalidISBNException {
        if (isbn.length() != 13 || !isbn.matches("\\d+")) {
            throw new InvalidISBNException("ISBN must be exactly 13 digits.");
        }
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.copies = copies;
    }

    // Getters
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
        return String.format("%s:%s:%s:%d", title, author, isbn, copies);
    }
}