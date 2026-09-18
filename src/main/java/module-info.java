module se.se233_termproject_ {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;

    opens se233.se233_termproject_2026.controller to javafx.fxml;
    exports se233.se233_termproject_2026;
}
