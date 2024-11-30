package com.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class MyLibrary {
    private Connection connection;
    private static final Logger logger = LogManager.getLogger(MyLibrary.class);

    public MyLibrary() throws LibraryException {
        try {
            connection = DBHelper.getConnection();
        } catch (SQLException e) {
            throw new LibraryException("Failed to establish database connection: " + e.getMessage());
        }
    }

    public List<Book> getAvailableBooks() {
        List<Book> books = new ArrayList<>();
        String query = "SELECT b.id, b.name, b.author, bc.category_name " +
                "FROM books b " +
                "LEFT JOIN book_categories bc ON b.category_id = bc.id " +
                "WHERE b.is_issued = false " +
                "ORDER BY b.id";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("author"),
                        rs.getString("category_name")));
            }
            logger.info("Retrieved {} available books", books.size());
        } catch (SQLException e) {
            logger.error("Error retrieving available books", e);
            throw new LibraryException("Failed to retrieve available books: " + e.getMessage());
        }
        return books;
    }

    public List<IssuedBook> getIssuedBooks() {
        List<IssuedBook> issuedBooks = new ArrayList<>();
        String query = "SELECT b.id, b.name, b.author, bc.category_name, b.issued_to, b.return_date, b.issued_on " +
                "FROM books b " +
                "LEFT JOIN book_categories bc ON b.category_id = bc.id " +
                "WHERE b.is_issued = true " +
                "ORDER BY b.id";

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                String returnDate = resultSet.getDate("return_date") != null
                        ? LocalDate.parse(resultSet.getDate("return_date").toString(), dbFormatter)
                                .format(displayFormatter)
                        : "";
                String issuedOn = resultSet.getDate("issued_on") != null
                        ? LocalDate.parse(resultSet.getDate("issued_on").toString(), dbFormatter)
                                .format(displayFormatter)
                        : "";

                issuedBooks.add(new IssuedBook(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getString("author"),
                        resultSet.getString("category_name"),
                        resultSet.getString("issued_to"),
                        returnDate,
                        issuedOn));
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to retrieve issued books: " + e.getMessage());
        }
        return issuedBooks;
    }

    // Add to MyLibrary.java
    public void addBookCategory(String category) {
        try {
            String checkQuery = "SELECT COUNT(*) FROM book_categories WHERE LOWER(category_name) = LOWER(?)";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, category);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new LibraryException("Category already exists");
                }
            }

            String query = "INSERT INTO book_categories (category_name) VALUES (?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, category);
                stmt.executeUpdate();
                logger.info("Added new category: {}", category);
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to add category: " + e.getMessage());
        }
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        String query = "SELECT category_name FROM book_categories ORDER BY category_name";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                categories.add(rs.getString("category_name"));
            }
            return categories;
        } catch (SQLException e) {
            throw new LibraryException("Failed to retrieve categories: " + e.getMessage());
        }
    }

    public void assignBookCategory(int bookId, String category) {
        try {
            connection.setAutoCommit(false);
            String query = "UPDATE books SET category_id = " +
                    "(SELECT id FROM book_categories WHERE category_name = ?) " +
                    "WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, category);
                stmt.setInt(2, bookId);
                int result = stmt.executeUpdate();
                if (result > 0) {
                    connection.commit();
                    logger.info("Assigned category {} to book ID {}", category, bookId);
                } else {
                    connection.rollback();
                    throw new LibraryException("Book or category not found");
                }
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                logger.error("Failed to rollback transaction", ex);
            }
            throw new LibraryException("Failed to assign category: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Failed to reset auto-commit", e);
            }
        }
    }

    public boolean addBook(String name, String author, String category) throws LibraryException {
        try {
            // Check for duplicate books (case insensitive)
            String checkQuery = "SELECT COUNT(*) FROM books WHERE LOWER(name) = LOWER(?) AND LOWER(author) = LOWER(?)";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, name);
                checkStmt.setString(2, author);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new LibraryException("Book already exists in the library");
                }
            }

            // Get category ID
            String categoryQuery = "SELECT id FROM book_categories WHERE LOWER(category_name) = LOWER(?)";
            int categoryId;
            try (PreparedStatement categoryStmt = connection.prepareStatement(categoryQuery)) {
                categoryStmt.setString(1, category);
                ResultSet categoryRs = categoryStmt.executeQuery();
                if (categoryRs.next()) {
                    categoryId = categoryRs.getInt("id");
                } else {
                    throw new LibraryException("Category does not exist");
                }
            }

            // Insert book with category
            connection.setAutoCommit(false);
            String query = "INSERT INTO books (name, author, category_id) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, name);
                stmt.setString(2, author);
                stmt.setInt(3, categoryId);

                int result = stmt.executeUpdate();
                connection.commit();
                logger.info("Added new book: {} by {}", name, author);
                return result > 0;
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Failed to rollback transaction: {}", rollbackEx.getMessage());
            }
            throw new LibraryException("Failed to add book: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Failed to reset auto-commit: {}", e.getMessage());
            }
        }
    }

    public double calculateFine(int bookId) {
        try {
            String query = "SELECT b.return_date, b.issued_to, " +
                    "DATEDIFF(CURRENT_DATE, b.return_date) as days_overdue " +
                    "FROM books b WHERE b.id = ? AND b.is_issued = true";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, bookId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int daysOverdue = rs.getInt("days_overdue");
                        double finePerDay = 1.5; // Can be made configurable
                        double fine = daysOverdue > 0 ? daysOverdue * finePerDay : 0;

                        if (fine > 0) {
                            logger.info("Fine calculated for book ID {}: Rs.{} ({} days overdue)",
                                    bookId, fine, daysOverdue);
                        }
                        return fine;
                    }
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new LibraryException("Failed to calculate fine: " + e.getMessage());
        }
    }

    public Book searchBook(int bookId) throws LibraryException {
        String query = "SELECT b.id, b.name, b.author, bc.category_name " +
                "FROM books b " +
                "LEFT JOIN book_categories bc ON b.category_id = bc.id " +
                "WHERE b.id = ? AND b.is_issued = false";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Book book = new Book(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("author"),
                        rs.getString("category_name"));
                logger.info("Found book with ID {}: {} by {}", bookId, book.getName(), book.getAuthor());
                return book;
            }
            logger.info("No available book found with ID: {}", bookId);
            return null;
        } catch (SQLException e) {
            logger.error("Error searching for book with ID {}: {}", bookId, e.getMessage());
            throw new LibraryException("Failed to search for book: " + e.getMessage());
        }
    }

    public boolean issueBook(String name, String author, String issuedTo, String returnDate, String issuedOn) {
        try {
            // Check if user has already issued a book (case insensitive)
            String checkQuery = "SELECT COUNT(*) FROM books WHERE LOWER(issued_to) = LOWER(?) AND is_issued = true";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, issuedTo);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new LibraryException("User has already issued a book");
                }
            }

            // Check if book exists and is available
            String bookQuery = "SELECT id FROM books WHERE name = ? AND author = ? AND is_issued = false";
            try (PreparedStatement bookStmt = connection.prepareStatement(bookQuery)) {
                bookStmt.setString(1, name);
                bookStmt.setString(2, author);
                ResultSet rs = bookStmt.executeQuery();
                if (!rs.next()) {
                    throw new LibraryException("Book not available for issue");
                }
            }

            connection.setAutoCommit(false);
            String query = "UPDATE books SET is_issued = true, issued_to = ?, " +
                    "return_date = STR_TO_DATE(?, '%Y-%m-%d'), " +
                    "issued_on = STR_TO_DATE(?, '%Y-%m-%d') " +
                    "WHERE name = ? AND author = ? AND is_issued = false";

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, issuedTo);
                stmt.setString(2, returnDate);
                stmt.setString(3, issuedOn);
                stmt.setString(4, name);
                stmt.setString(5, author);

                int result = stmt.executeUpdate();
                if (result > 0) {
                    connection.commit();
                    logger.info("Book issued successfully: {} to {}", name, issuedTo);
                    return true;
                } else {
                    connection.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                logger.error("Failed to rollback transaction", ex);
            }
            throw new LibraryException("Failed to issue book: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Failed to reset auto-commit", e);
            }
        }
    }

    public boolean returnBook(String name, String author) {
        try {
            connection.setAutoCommit(false);
            String query = "UPDATE books SET is_issued = false, issued_to = NULL, " +
                    "return_date = NULL, issued_on = NULL WHERE name = ? AND author = ? AND is_issued = true";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, name);
                statement.setString(2, author);
                int result = statement.executeUpdate();
                connection.commit();
                return result > 0;
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new LibraryException("Failed to rollback transaction: " + rollbackEx.getMessage());
            }
            throw new LibraryException("Failed to return book: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new LibraryException("Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    public boolean isBookExistsInLibrary(String name, String author) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) as count FROM books WHERE name = ? AND author = ?")) {
            statement.setString(1, name);
            statement.setString(2, author);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("count") > 0;
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to check book existence: " + e.getMessage());
        }
    }

    public boolean isBookInLibrary(String name, String author) {
        return isBookExistsInLibrary(name, author);
    }

    public boolean removeBook(String name, String author) {
        try {
            connection.setAutoCommit(false);

            // Get the ID of the book to be removed
            int bookIdToRemove = -1;
            String getIdQuery = "SELECT id FROM books WHERE name = ? AND author = ?";
            try (PreparedStatement stmt = connection.prepareStatement(getIdQuery)) {
                stmt.setString(1, name);
                stmt.setString(2, author);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    bookIdToRemove = rs.getInt("id");
                }
            }

            // Check if book exists
            if (bookIdToRemove == -1) {
                throw new LibraryException("Book not found");
            }

            // Check for ID conflicts
            if (isBookIdConflict(bookIdToRemove)) {
                throw new LibraryException("Cannot remove book as it is currently issued");
            }

            // Delete the book
            String deleteQuery = "DELETE FROM books WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
                stmt.setInt(1, bookIdToRemove);
                int result = stmt.executeUpdate();

                if (result > 0) {
                    // Get all books with higher IDs that are not issued
                    String getHigherIdsQuery = "SELECT id FROM books WHERE id > ? ORDER BY id";
                    try (PreparedStatement higherIdsStmt = connection.prepareStatement(getHigherIdsQuery)) {
                        higherIdsStmt.setInt(1, bookIdToRemove);
                        ResultSet rs = higherIdsStmt.executeQuery();

                        while (rs.next()) {
                            int currentId = rs.getInt("id");
                            // Check if new ID would conflict with an issued book
                            if (!isBookIdConflict(currentId - 1)) {
                                // Update ID if no conflict
                                String updateIdQuery = "UPDATE books SET id = ? WHERE id = ?";
                                try (PreparedStatement updateStmt = connection.prepareStatement(updateIdQuery)) {
                                    updateStmt.setInt(1, currentId - 1);
                                    updateStmt.setInt(2, currentId);
                                    updateStmt.executeUpdate();
                                }
                            }
                        }
                    }

                    // Reset auto increment
                    String maxIdQuery = "SELECT MAX(id) as max_id FROM books";
                    try (Statement maxStmt = connection.createStatement();
                            ResultSet rs = maxStmt.executeQuery(maxIdQuery)) {
                        if (rs.next()) {
                            int maxId = rs.getInt("max_id");
                            String resetAutoIncrQuery = "ALTER TABLE books AUTO_INCREMENT = ?";
                            try (PreparedStatement resetStmt = connection.prepareStatement(resetAutoIncrQuery)) {
                                resetStmt.setInt(1, maxId + 1);
                                resetStmt.executeUpdate();
                            }
                        }
                    }
                }

                connection.commit();
                logger.info("Book removed and IDs resequenced successfully");
                return result > 0;
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                throw new LibraryException("Failed to rollback transaction", ex);
            }
            throw new LibraryException("Failed to remove book: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new LibraryException("Failed to reset auto-commit", e);
            }
        }
    }

    private boolean isBookIdConflict(int id) {
        String query = "SELECT COUNT(*) FROM books WHERE id = ? AND is_issued = true";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new LibraryException("Failed to check book ID conflict: " + e.getMessage());
        }
    }

    public boolean isBookIssued(String name, String author) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT is_issued FROM books WHERE name = ? AND author = ?")) {
            statement.setString(1, name);
            statement.setString(2, author);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean("is_issued");
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to check if book is issued: " + e.getMessage());
        }
    }

    public Book getBookById(int id) {
        String query = "SELECT b.id, b.name, b.author, bc.category_name " +
                "FROM books b " +
                "LEFT JOIN book_categories bc ON b.category_id = bc.id " +
                "WHERE b.id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Book(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("author"),
                            rs.getString("category_name"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving book by ID: {}", id, e);
            throw new LibraryException("Failed to retrieve book: " + e.getMessage());
        }
        return null;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed successfully");
            } catch (SQLException e) {
                throw new LibraryException("Failed to close database connection", e);
            }
        }
    }
}