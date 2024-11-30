package com.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
//import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
//import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LibraryApp extends Application {
    private TableView<Book> bookTable;
    private TableView<IssuedBook> issuedBookTable;
    private MyLibrary library;
    private Tab booksTab;
    private static final Logger logger = LogManager.getLogger(LibraryApp.class);
    private ObservableList<Book> booksList;
    private FilteredList<Book> filteredBooks;
    private ObservableList<IssuedBook> issuedBooksList;
    private FilteredList<IssuedBook> filteredIssuedBooks;

    @Override
    public void start(Stage primaryStage) {
        try {
            library = new MyLibrary();

            TabPane tabPane = new TabPane();
            tabPane.setTabMinWidth(100);
            tabPane.setTabMaxWidth(200);

            booksTab = new Tab("Books");
            Tab issueTab = new Tab("Issue Book");
            Tab issuedBooksTab = new Tab("Issued Books");

            booksTab.setContent(createBooksTab());
            issueTab.setContent(createIssueTab());
            issuedBooksTab.setContent(createIssuedBooksTab());

            Label booksLabel = new Label("Books");
            booksLabel.setGraphic(new Text("📚"));
            booksTab.setGraphic(booksLabel);
            booksTab.setClosable(false);

            Label issueLabel = new Label("Issue Book");
            issueLabel.setGraphic(new Text("📖"));
            issueTab.setGraphic(issueLabel);
            issueTab.setClosable(false);

            Label issuedBooksLabel = new Label("Issued Books");
            issuedBooksLabel.setGraphic(new Text("📋"));
            issuedBooksTab.setGraphic(issuedBooksLabel);
            issuedBooksTab.setClosable(false);

            tabPane.getTabs().addAll(booksTab, issueTab, issuedBooksTab);

            Scene scene = new Scene(tabPane, 900, 700);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

            primaryStage.setTitle("Library Management System");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (LibraryException e) {
            logger.error("Failed to initialize library", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to initialize library: " + e.getMessage());
        }
    }

    private VBox createBooksTab() {
        VBox booksLayout = new VBox(10);
        booksLayout.setPadding(new Insets(10));

        // Initialize lists first
        booksList = FXCollections.observableArrayList();
        filteredBooks = new FilteredList<>(booksList);

        // Add New Book section
        TitledPane addBookPane = new TitledPane();
        addBookPane.setText("Add New Book");
        GridPane addBookGrid = new GridPane();
        addBookGrid.setHgap(10);
        addBookGrid.setVgap(5);
        addBookGrid.setPadding(new Insets(10));

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        Label authorLabel = new Label("Author:");
        TextField authorField = new TextField();
        Label categoryLabel = new Label("Category:");
        ComboBox<String> categoryComboBox = new ComboBox<>();

        try {
            categoryComboBox.getItems().addAll(library.getCategories());
        } catch (LibraryException e) {
            showError("Error loading categories", e.getMessage());
        }

        Button addButton = new Button("Add Book");
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            String author = authorField.getText();
            String category = categoryComboBox.getValue();

            if (name.isEmpty() || author.isEmpty() || category == null) {
                showError("Invalid Input", "Please fill in all fields");
                return;
            }

            try {
                if (library.addBook(name, author, category)) {
                    showSuccess("Success", "Book added successfully");
                    nameField.clear();
                    authorField.clear();
                    categoryComboBox.setValue(null);
                    refreshBookTable();
                }
            } catch (LibraryException ex) {
                showError("Error", ex.getMessage());
            }
        });

        addBookGrid.add(nameLabel, 0, 0);
        addBookGrid.add(nameField, 1, 0);
        addBookGrid.add(authorLabel, 0, 1);
        addBookGrid.add(authorField, 1, 1);
        addBookGrid.add(categoryLabel, 0, 2);
        addBookGrid.add(categoryComboBox, 1, 2);
        addBookGrid.add(addButton, 1, 3);

        addBookPane.setContent(addBookGrid);

        // Books List Section
        TitledPane booksListPane = new TitledPane();
        booksListPane.setText("Available Books");
        VBox booksListContainer = new VBox(5);

        TextField searchField = new TextField();
        searchField.setPromptText("Search books...");

        bookTable = new TableView<>();
        bookTable.getStyleClass().add("table-view");
        setupBookTableColumns();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredBooks.setPredicate(book -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return book.getName().toLowerCase().contains(lowerCaseFilter) ||
                        book.getAuthor().toLowerCase().contains(lowerCaseFilter) ||
                        book.getCategory().toLowerCase().contains(lowerCaseFilter);
            });
        });

        Button removeButton = new Button("Remove Selected Book");
        removeButton.setDisable(true);
        removeButton.getStyleClass().add("action-button");

        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            removeButton.setDisable(newSelection == null);
        });

        removeButton.setOnAction(e -> {
            Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
            if (selectedBook == null) {
                showError("Error", "Please select a book to remove");
                return;
            }

            try {
                if (library.removeBook(selectedBook.getName(), selectedBook.getAuthor())) {
                    showSuccess("Success", "Book removed successfully");
                    refreshBookTable();
                }
            } catch (LibraryException ex) {
                showError("Error", ex.getMessage());
            }
        });

        booksListContainer.getChildren().addAll(searchField, bookTable, removeButton);
        booksListPane.setContent(booksListContainer);

        booksLayout.getChildren().addAll(addBookPane, booksListPane);
        refreshBookTable();

        return booksLayout;
    }

    private VBox createIssueTab() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));

        VBox formContainer = new VBox(15);
        formContainer.getStyleClass().add("form-container");

        Label titleLabel = new Label("Issue Book");
        titleLabel.getStyleClass().add("section-title");

        TextField bookIdField = new TextField();
        bookIdField.setPromptText("Enter Book ID");

        TextField bookNameField = new TextField();
        bookNameField.setPromptText("Book name will appear here");
        bookNameField.setEditable(false);

        TextField authorField = new TextField();
        authorField.setPromptText("Author name will appear here");
        authorField.setEditable(false);

        TextField issuedToField = new TextField();
        issuedToField.setPromptText("Enter borrower name");

        DatePicker returnDatePicker = new DatePicker();
        returnDatePicker.setPromptText("Select return date");
        returnDatePicker.setValue(LocalDate.now().plusDays(1));

        StringConverter<LocalDate> converter = new StringConverter<>() {
            private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                }
                return "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                }
                return null;
            }
        };
        returnDatePicker.setConverter(converter);

        returnDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate today = LocalDate.now();
                setDisable(empty || date.compareTo(today.plusDays(1)) < 0);
            }
        });

        bookIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.trim().isEmpty()) {
                try {
                    int bookId = Integer.parseInt(newValue);
                    Book book = library.searchBook(bookId);
                    if (book != null) {
                        bookNameField.setText(book.getName());
                        authorField.setText(book.getAuthor());
                    } else {
                        bookNameField.clear();
                        authorField.clear();
                    }
                } catch (NumberFormatException e) {
                    bookNameField.clear();
                    authorField.clear();
                } catch (LibraryException e) {
                    showError("Error", e.getMessage());
                }
            } else {
                bookNameField.clear();
                authorField.clear();
            }
        });

        Button issueButton = new Button("Issue Book");
        issueButton.setOnAction(e -> {
            String bookName = bookNameField.getText();
            String author = authorField.getText();
            String issuedTo = issuedToField.getText();
            LocalDate returnDate = returnDatePicker.getValue();

            if (bookName.isEmpty() || author.isEmpty() || issuedTo.isEmpty() || returnDate == null) {
                showError("Invalid Input", "Please fill all fields");
                return;
            }

            try {
                // Convert to database format (yyyy-MM-dd)
                String formattedReturnDate = returnDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String formattedIssuedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                if (library.issueBook(bookName, author, issuedTo, formattedReturnDate, formattedIssuedDate)) {
                    showSuccess("Success", "Book issued successfully");
                    bookIdField.clear();
                    bookNameField.clear();
                    authorField.clear();
                    issuedToField.clear();
                    returnDatePicker.setValue(LocalDate.now().plusDays(1));
                    refreshIssuedBooksTable();
                    refreshBookTable();
                }
            } catch (LibraryException ex) {
                showError("Error", ex.getMessage());
            }
        });

        formContainer.getChildren().addAll(
                titleLabel,
                new Label("Book ID:"), bookIdField,
                new Label("Book Name:"), bookNameField,
                new Label("Author:"), authorField,
                new Label("Issue To:"), issuedToField,
                new Label("Return Date:"), returnDatePicker,
                issueButton);

        mainContainer.getChildren().add(formContainer);
        return mainContainer;
    }

    private VBox createIssuedBooksTab() {
        VBox issuedBooksLayout = new VBox(10);
        issuedBooksLayout.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPromptText("Search issued books...");

        issuedBookTable = new TableView<>();
        issuedBookTable.getStyleClass().add("table-view");
        setupIssuedBookTableColumns();

        issuedBooksList = FXCollections.observableArrayList();
        filteredIssuedBooks = new FilteredList<>(issuedBooksList);
        issuedBookTable.setItems(filteredIssuedBooks);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredIssuedBooks.setPredicate(book -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return book.getName().toLowerCase().contains(lowerCaseFilter) ||
                        book.getAuthor().toLowerCase().contains(lowerCaseFilter) ||
                        book.getCategory().toLowerCase().contains(lowerCaseFilter) ||
                        book.getIssuedTo().toLowerCase().contains(lowerCaseFilter);
            });
        });

        Button returnButton = new Button("Return Selected Book");
        returnButton.getStyleClass().add("action-button");

        returnButton.setOnAction(e -> {
            IssuedBook selectedBook = issuedBookTable.getSelectionModel().getSelectedItem();
            if (selectedBook == null) {
                showError("Error", "Please select a book to return");
                return;
            }
            try {
                if (library.returnBook(selectedBook.getName(), selectedBook.getAuthor())) {
                    showSuccess("Success", "Book returned successfully");
                    refreshIssuedBooksTable();
                    refreshBookTable();
                }
            } catch (LibraryException ex) {
                showError("Error", ex.getMessage());
            }
        });

        VBox tableContainer = new VBox(5);
        tableContainer.getChildren().addAll(searchField, issuedBookTable, returnButton);

        TitledPane issuedBooksPane = new TitledPane();
        issuedBooksPane.setText("Issued Books");
        issuedBooksPane.setContent(tableContainer);
        issuedBooksPane.setExpanded(true);

        issuedBooksLayout.getChildren().add(issuedBooksPane);
        refreshIssuedBooksTable();
        return issuedBooksLayout;
    }

    private void setupBookTableColumns() {
        TableColumn<Book, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<Book, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<Book, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<Book, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setSortType(TableColumn.SortType.ASCENDING);

        bookTable.getColumns().add(idColumn);
        bookTable.getColumns().add(nameColumn);
        bookTable.getColumns().add(authorColumn);
        bookTable.getColumns().add(categoryColumn);

        SortedList<Book> sortedData = new SortedList<>(filteredBooks);
        sortedData.comparatorProperty().bind(bookTable.comparatorProperty());
        bookTable.setItems(sortedData);
    }

    private void setupIssuedBookTableColumns() {
        TableColumn<IssuedBook, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);
        idColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<IssuedBook, String> nameColumn = new TableColumn<>("Book Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(200);
        nameColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<IssuedBook, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorColumn.setPrefWidth(150);
        authorColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<IssuedBook, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setPrefWidth(100);
        categoryColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<IssuedBook, String> issuedToColumn = new TableColumn<>("Issued To");
        issuedToColumn.setCellValueFactory(new PropertyValueFactory<>("issuedTo"));
        issuedToColumn.setPrefWidth(150);
        issuedToColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<IssuedBook, String> issuedOnColumn = new TableColumn<>("Issued On");
        issuedOnColumn.setCellValueFactory(new PropertyValueFactory<>("issuedOn"));
        issuedOnColumn.setPrefWidth(100);
        issuedOnColumn.setSortType(TableColumn.SortType.ASCENDING);

        TableColumn<IssuedBook, String> returnDateColumn = new TableColumn<>("Return Date");
        returnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        returnDateColumn.setPrefWidth(100);
        returnDateColumn.setSortType(TableColumn.SortType.ASCENDING);

        issuedBookTable.getColumns().add(idColumn);
        issuedBookTable.getColumns().add(nameColumn);
        issuedBookTable.getColumns().add(authorColumn);
        issuedBookTable.getColumns().add(categoryColumn);
        issuedBookTable.getColumns().add(issuedToColumn);
        issuedBookTable.getColumns().add(issuedOnColumn);
        issuedBookTable.getColumns().add(returnDateColumn);

        // Enable column resizing
        issuedBookTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void refreshBookTable() {
        try {
            booksList.clear();
            booksList.addAll(library.getAvailableBooks());
        } catch (LibraryException e) {
            showError("Error", "Failed to refresh books list: " + e.getMessage());
        }
    }

    private void refreshIssuedBooksTable() {
        try {
            issuedBooksList.clear();
            issuedBooksList.addAll(library.getIssuedBooks());
        } catch (LibraryException e) {
            showError("Error", "Failed to refresh issued books list: " + e.getMessage());
        }
    }

    private void showSuccess(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
        logger.info("{}: {}", title, message);
    }

    private void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
        logger.error("{}: {}", title, message);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        try {
            if (library != null) {
                library.close();
            }
        } catch (LibraryException e) {
            logger.error("Error closing library", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}