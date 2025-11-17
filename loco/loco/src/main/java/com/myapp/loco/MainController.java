package com.myapp.loco;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML
    private ComboBox<String> logChannelComboBox;
    @FXML
    private Spinner<Integer> eventCountSpinner;
    @FXML
    private TextField xpathQueryField;
    @FXML
    private Button getLogsButton;
    @FXML
    private ToggleButton autoRefreshToggle;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private TableView<LogEvent> logTableView;
    @FXML
    private TableColumn<LogEvent, String> eventIdColumn;
    @FXML
    private TableColumn<LogEvent, String> timeColumn;
    @FXML
    private TableColumn<LogEvent, String> providerColumn;
    @FXML
    private TableColumn<LogEvent, String> levelColumn;
    @FXML
    private TableColumn<LogEvent, String> descriptionColumn;

    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        logChannelComboBox.setItems(FXCollections.observableArrayList(
                "Application",
                "Security",
                "System",
                "Microsoft-Windows-Sysmon/Operational"
        ));
        logChannelComboBox.setValue("Application");

        eventIdColumn.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeCreated"));
        providerColumn.setCellValueFactory(new PropertyValueFactory<>("providerName"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // details click
        logTableView.setRowFactory(tv -> {
            TableRow<LogEvent> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    LogEvent rowData = row.getItem();
                    showLogDetails(rowData);
                }
            });
            return row;
        });
        //auto refresh
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> handleGetLogs()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);

        autoRefreshToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                autoRefreshTimeline.play();
                getLogsButton.setDisable(true);
            } else {
                autoRefreshTimeline.stop();
                getLogsButton.setDisable(false);
            }
        });
        logTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    private void handleGetLogs() {
        String logChannel = logChannelComboBox.getValue();
        int eventCount = eventCountSpinner.getValue();
        String xpathQuery = xpathQueryField.getText();

        Task<List<LogEvent>> task = new Task<>() {
            @Override
            protected List<LogEvent> call() throws Exception {
                Platform.runLater(() -> loadingIndicator.setVisible(true));

                List<String> command = new ArrayList<>();
                command.add("wevtutil");
                command.add("qe"); // qe =7 query-events
                command.add(logChannel);
                command.add("/c:" + eventCount); // c = count
                command.add("/rd:true"); // rd = reverse direction (mới nhất trước)
                command.add("/f:xml"); // f = format (XML)

                // Thêm lọc XPath nếu có
                if (xpathQuery != null && !xpathQuery.trim().isEmpty()) {
                    command.add("/q:" + xpathQuery);
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder xmlOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        xmlOutput.append(line).append(System.lineSeparator());
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("wevtutil exited with code: " + exitCode + "\nOutput: " + xmlOutput);
                }

                return parseLogEvents(xmlOutput.toString());
            }
        };

        task.setOnSucceeded(e -> {
            logTableView.setItems(FXCollections.observableArrayList(task.getValue()));
            loadingIndicator.setVisible(false);
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            showError("Lỗi Lấy Logs", "Không thể thực thi lệnh 'wevtutil'.", "Hãy đảm bảo bạn đang chạy ứng dụng với quyền Administrator.\n\n" + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private List<LogEvent> parseLogEvents(String xmlOutput) throws Exception {
        List<LogEvent> events = new ArrayList<>();
        String validXml = "<Events>" + xmlOutput + "</Events>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(validXml));
        Document doc = builder.parse(is);

        NodeList eventNodes = doc.getElementsByTagName("Event");
        for (int i = 0; i < eventNodes.getLength(); i++) {
            Node eventNode = eventNodes.item(i);
            if (eventNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eventElement = (Element) eventNode;

                Element systemElement = (Element) eventElement.getElementsByTagName("System").item(0);
                String eventId = systemElement.getElementsByTagName("EventID").item(0).getTextContent();
                String timeCreated = ((Element) systemElement.getElementsByTagName("TimeCreated").item(0)).getAttribute("SystemTime");
                String providerName = ((Element) systemElement.getElementsByTagName("Provider").item(0)).getAttribute("Name");
                String level = systemElement.getElementsByTagName("Level").item(0).getTextContent();

                Element eventDataElement = (Element) eventElement.getElementsByTagName("EventData").item(0);
                String fullDetails = ""; // Dữ liệu đầy đủ cho cửa sổ pop-up
                String description; // Dữ liệu tóm tắt cho bảng

                if ("Microsoft-Windows-Sysmon".equals(providerName)) {
                    fullDetails = parseSysmonEventData(eventDataElement);
                } else {
                    fullDetails = parseGenericEventData(eventDataElement);
                }

                description = fullDetails.split("\n")[0] + " [...]";

                events.add(new LogEvent(eventId, timeCreated, providerName, level, description, fullDetails));
            }
        }
        return events;
    }

    private String parseSysmonEventData(Element eventDataElement) {
        if (eventDataElement == null) return "No EventData";
        StringBuilder details = new StringBuilder();
        NodeList dataNodes = eventDataElement.getElementsByTagName("Data");
        String eventId = ((Element) eventDataElement.getParentNode()).getElementsByTagName("EventID").item(0).getTextContent();

        switch (eventId) {
            case "1": details.append("[Process Create]\n"); break;
            case "3": details.append("[Network Connect]\n"); break;
            case "7": details.append("[Image Load]\n"); break;
            case "8": details.append("[Create Remote Thread]\n"); break;
            case "10": details.append("[Process Access]\n"); break;
            case "11": details.append("[File Create]\n"); break;
            case "22": details.append("[DNS Query]\n"); break;
            default: details.append("[Sysmon Event ID: ").append(eventId).append("]\n");
        }

        for (int j = 0; j < dataNodes.getLength(); j++) {
            Node dataNode = dataNodes.item(j);
            String name = dataNode.getAttributes().getNamedItem("Name").getNodeValue();
            String value = dataNode.getTextContent();
            details.append(name).append(": ").append(value).append("\n");
        }
        return details.toString().trim();
    }

    private String parseGenericEventData(Element eventDataElement) {
        if (eventDataElement == null) return "No EventData";
        StringBuilder details = new StringBuilder();
        NodeList dataNodes = eventDataElement.getElementsByTagName("Data");
        for (int j = 0; j < dataNodes.getLength(); j++) {
            Node dataNode = dataNodes.item(j);
            Node nameAttribute = dataNode.getAttributes().getNamedItem("Name");

            if (nameAttribute != null) {
                String name = nameAttribute.getNodeValue();
                String value = dataNode.getTextContent();
                details.append(name).append(": ").append(value).append("\n");
            } else {
                String value = dataNode.getTextContent();
                details.append(value).append("\n");
            }
        }
        return details.toString().trim();
    }


    private void showLogDetails(LogEvent logEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết Log Event: " + logEvent.getEventId());
        alert.setHeaderText("Provider: " + logEvent.getProviderName() + "\nTime: " + logEvent.getTimeCreated());

        TextArea textArea = new TextArea(logEvent.getFullDetails());
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.getDialogPane().setExpanded(true);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(400);

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);

        alert.showAndWait();
    }

    private void showError(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}

