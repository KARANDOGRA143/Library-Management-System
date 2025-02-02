module com.example {
    requires transitive java.sql; // Changed to transitive
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;
    requires transitive javafx.graphics;

    exports com.example;

    opens com.example to javafx.fxml;
}