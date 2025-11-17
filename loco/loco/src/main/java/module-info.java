module com.myapp.loco {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;

    opens com.myapp.loco to javafx.fxml;
    exports com.myapp.loco;
}

