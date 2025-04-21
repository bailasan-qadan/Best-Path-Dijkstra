import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.*;


public class WorldMap extends Application {
    private Capital[] capitals;
    private int capitalCount = 0;
    private static final int MAX_CAPITALS = 200;
    private ComboBox<Capital> sourceCombo;
    private ComboBox<Capital> targetCombo;
    private boolean isSelectingSource = true;
    private Pane mapPane;
    private ImageView mapView;
    private Group mapGroup;
    private ScrollPane scrollPane;
    private double currentScale = 1.0;
    private Group contentGroup;
    private double initialWidth = 1200;
    private double initialHeight = 700;
    private FlightGraph flightGraph;
    
    private static final double MERCATOR_MAX_LATITUDE = 85.05112878;
    private static final String MAP_IMAGE_PATH = "map.png";
    private static final String CAPITALS_FILE_PATH = "C:\\Users\\HP\\Desktop\\World Map\\Map\\Capital Coordinates.txt";
    
    @Override
    public void start(Stage primaryStage) {
        capitals = new Capital[MAX_CAPITALS];
        loadCapitals(); // Load capitals first
        loadFlightData(); // Add this line to load flight data
        initializeUI(primaryStage); 
        plotAllCapitals();
    }

    private void initializeUI(Stage primaryStage) {
        // Initialize components
        mapGroup = new Group();
        mapPane = new Pane();
        mapPane.setPadding(new Insets(20));
        
        // Set fixed size for mapPane
        mapPane.setPrefWidth(initialWidth);
        mapPane.setPrefHeight(initialHeight);
        mapPane.setMinWidth(initialWidth);
        mapPane.setMinHeight(initialHeight);
        
        // Load map image first
        if (!loadMapImage()) {
            showErrorDialog("Failed to load map image", "Please ensure the map image file exists at: " + MAP_IMAGE_PATH);
            Platform.exit();
            return;
        }
        
        // Setup content hierarchy
        contentGroup = new Group(mapPane);
        
        // Create a container to maintain proper scrolling bounds
        StackPane scrollContent = new StackPane(contentGroup);
        scrollContent.setMinWidth(initialWidth);
        scrollContent.setMinHeight(initialHeight);
        
        // Configure map ScrollPane
        scrollPane = new ScrollPane(scrollContent);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        // Ensure map ScrollPane doesn't force fit content
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        
        // Create main layout container
        VBox mainContainer = createMainContainer();
        
        // Create main ScrollPane for the entire application
        ScrollPane mainScrollPane = new ScrollPane(mainContainer);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        // Setup scene with main ScrollPane
        Scene scene = new Scene(mainScrollPane);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        
        // Setup zoom handlers
        setupZoomHandlers();
        
        primaryStage.setTitle("World Capitals Explorer");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
        
        // Adjust main container padding based on window size
        primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            double padding = Math.max(0, (newVal.doubleValue() - initialWidth) / 2);
            mainContainer.setPadding(new Insets(20, padding, 20, padding));
        });
    }
    
    private VBox createMainContainer() {
        VBox mainContainer = new VBox(20);
        mainContainer.getStyleClass().add("main-container");
        mainContainer.setPadding(new Insets(20));
        
        Label titleLabel = new Label("World Capitals Explorer");
        titleLabel.getStyleClass().add("title-label");
        
        // Create a container for the map and control panel
        HBox contentContainer = new HBox(20);
        contentContainer.setAlignment(Pos.CENTER);
        
        VBox mapContainer = createMapContainer();
        VBox controlPanel = createControlPanel();
        
        // Set preferred width for control panel to prevent it from stretching
        controlPanel.setPrefWidth(300);
        controlPanel.setMaxWidth(300);
        
        contentContainer.getChildren().addAll(mapContainer, controlPanel);
        mainContainer.getChildren().addAll(titleLabel, contentContainer);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        
        return mainContainer;
    }
    
    private VBox createMapContainer() {
        VBox mapContainer = new VBox(10);
        mapContainer.getStyleClass().add("map-container");
        mapContainer.setMinWidth(initialWidth);
        
        HBox zoomControls = createZoomControls();
        mapContainer.getChildren().addAll(scrollPane, zoomControls);
        
        return mapContainer;
    }
     
    
    private HBox createZoomControls() {
        HBox controls = new HBox(10);
        controls.getStyleClass().add("zoom-controls");
        controls.setAlignment(Pos.CENTER);
        
        Button zoomIn = new Button("+");
        Button zoomOut = new Button("-");
        Button resetZoom = new Button("Reset");
        
        zoomIn.setOnAction(e -> zoom(1.2));
        zoomOut.setOnAction(e -> zoom(0.8));
        resetZoom.setOnAction(e -> resetView());
        
        controls.getChildren().addAll(
            new Label("Zoom:"),
            zoomOut,
            resetZoom,
            zoomIn
        );
        
        return controls;
    }
    
    private VBox createControlPanel() {
        VBox controlPanel = new VBox(15);
        controlPanel.getStyleClass().add("control-panel");

        // Search Section
        TextField searchField = new TextField();
        searchField.setPromptText("Search capitals...");

        ListView<Capital> capitalsList = new ListView<>();
        Capital[] capitalsArray = new Capital[capitalCount];
        System.arraycopy(capitals, 0, capitalsArray, 0, capitalCount);
        capitalsList.setItems(FXCollections.observableArrayList(capitalsArray));
        capitalsList.setPrefHeight(100);

        // Path Finding Section
        VBox pathFindingForm = createPathFindingForm();

        // Search functionality
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            Capital[] filteredCapitals = filterCapitals(newText.toLowerCase());
            capitalsList.setItems(FXCollections.observableArrayList(filteredCapitals));
        });

        capitalsList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    highlightCapitalWithoutZoom(newSelection);
                }
            }
        );

        controlPanel.getChildren().addAll(
            new Label("Search Capitals"),
            searchField,
            capitalsList,
            new Separator(),
            pathFindingForm
        );

        return controlPanel;
    }
private Capital[] filterCapitals(String searchText) {
        int count = 0;
        Capital[] filtered = new Capital[capitalCount];
        
        for (int i = 0; i < capitalCount; i++) {
            if (capitals[i].name.toLowerCase().contains(searchText)) {
                filtered[count++] = capitals[i];
            }
        }
        
        Capital[] result = new Capital[count];
        System.arraycopy(filtered, 0, result, 0, count);
        return result;
    }

    private VBox createPathFindingForm() {
        VBox form = new VBox(10);
        form.getStyleClass().add("path-finding-form");
        form.setPadding(new Insets(10));
        form.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5;");

        // Create ComboBoxes
        sourceCombo = new ComboBox<>();
        targetCombo = new ComboBox<>();
        
        sourceCombo.setPromptText("Click a capital on map for source");
        targetCombo.setPromptText("Click another capital for target");
        
        sourceCombo.setMaxWidth(Double.MAX_VALUE);
        targetCombo.setMaxWidth(Double.MAX_VALUE);

        // Populate ComboBoxes
        populateComboBoxes();

        // Filter ComboBox
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.setItems(FXCollections.observableArrayList(
            "Shortest Distance",
            "Less Time",
            "Less Cost"
        ));
        filterCombo.setValue("Shortest Distance");
        filterCombo.setMaxWidth(Double.MAX_VALUE);

        // Create other components...
        VBox pathDisplay = new VBox(5);
        pathDisplay.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5; -fx-padding: 5;");
        pathDisplay.setMinHeight(100);

        TextField distanceField = new TextField();
        distanceField.setEditable(false);
        distanceField.setPromptText("Distance");

        TextField costField = new TextField();
        costField.setEditable(false);
        costField.setPromptText("Cost");

        TextField timeField = new TextField();
        timeField.setEditable(false);
        timeField.setPromptText("Time");

        Button runButton = new Button("Run");
        runButton.setMaxWidth(Double.MAX_VALUE);
        runButton.getStyleClass().add("run-button");

        runButton.setOnAction(e -> handleRunButtonAction(
            sourceCombo, targetCombo, filterCombo, 
            pathDisplay, distanceField, costField, timeField
        ));

        form.getChildren().addAll(
            new Label("Source:"),
            sourceCombo,
            new Label("Target:"),
            targetCombo,
            new Label("Filter:"),
            filterCombo,
            runButton,
            new Label("Path:"),
            pathDisplay,
            new Label("Distance:"),
            distanceField,
            new Label("Cost:"),
            costField,
            new Label("Time:"),
            timeField
        );

        return form;
    }
    private void populateComboBoxes() {
        Platform.runLater(() -> {
            ObservableList<Capital> capitalsList = FXCollections.observableArrayList();
            for (int i = 0; i < capitalCount; i++) {
                if (capitals[i] != null) {
                    capitalsList.add(capitals[i]);
                }
            }
            sourceCombo.setItems(capitalsList);
            targetCombo.setItems(FXCollections.observableArrayList(capitalsList));
        });
    }

    private void handleRunButtonAction(
            ComboBox<Capital> sourceCombo, 
            ComboBox<Capital> targetCombo,
            ComboBox<String> filterCombo,
            VBox pathDisplay,
            TextField distanceField,
            TextField costField,
            TextField timeField) {
        
        Capital source = sourceCombo.getValue();
        Capital target = targetCombo.getValue();
        
        // Clear previous results
        pathDisplay.getChildren().clear();
        distanceField.clear();
        costField.clear();
        timeField.clear();

        // Validate selections
        if (source == null || target == null) {
            showErrorDialog("Invalid Selection", "Please select both source and target capitals.");
            return;
        }

        if (source.name.equals(target.name)) {
            showErrorDialog("Domestic Flight", "Domestic flights are not available. Please select different source and target capitals.");
            return;
        }

        String filter = filterCombo.getValue();

        // Clear previous path
        resetMap();
        
        // Calculate and display path
        calculatePath(source, target, filter, pathDisplay, distanceField, costField, timeField);
    }


    private void calculatePath(Capital source, Capital target, String filter, 
    VBox pathDisplay, TextField distanceField, 
    TextField costField, TextField timeField) {
// Clear previous results
pathDisplay.getChildren().clear();

PathResult result = findShortestPath(source.name, target.name, filter);

// Create path text
StringBuilder pathText = new StringBuilder();
for (int i = 0; i < result.pathLength; i++) {
if (i > 0) pathText.append(" → ");
pathText.append(result.path[i]);
}

Label pathLabel = new Label(pathText.toString());
pathLabel.setWrapText(true);
pathDisplay.getChildren().add(pathLabel);

// Draw lines for the path
for (int i = 0; i < result.pathLength - 1; i++) {
Capital sourceCap = findCapitalByName(result.path[i]);
Capital targetCap = findCapitalByName(result.path[i + 1]);
drawPathLine(sourceCap, targetCap);
}

// Update the metrics
double distance = calculateTotalDistance(result.path, result.pathLength);
distanceField.setText(String.format("%.2f km", distance));
costField.setText(String.format("$%.2f", result.totalCost));
timeField.setText(String.format("%d minutes", result.totalDuration));
}

private void drawPathLine(Capital source, Capital target) {
    double[] sourcePos = calculateCapitalPosition(source);
    double[] targetPos = calculateCapitalPosition(target);
    
    Line pathLine = new Line(sourcePos[0], sourcePos[1], targetPos[0], targetPos[1]);
    pathLine.setStroke(Color.RED);
    pathLine.setStrokeWidth(2);
    pathLine.getStrokeDashArray().addAll(10.0, 5.0);
    
    mapPane.getChildren().add(pathLine);
}

private double calculateDistance(Capital source, Capital target) {
    final int R = 6371; // Earth's radius in kilometers

    double lat1 = Math.toRadians(source.latitude);
    double lon1 = Math.toRadians(source.longitude);
    double lat2 = Math.toRadians(target.latitude);
    double lon2 = Math.toRadians(target.longitude);

    double dLat = lat2 - lat1;
    double dLon = lon2 - lon1;

    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
               Math.cos(lat1) * Math.cos(lat2) *
               Math.sin(dLon/2) * Math.sin(dLon/2);
    
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    
    return R * c;
}

private void resetMap() {
    mapPane.getChildren().clear();
    mapPane.getChildren().add(mapView);
    plotAllCapitals();
}
    
    private boolean loadMapImage() {
        try {
            Image mapImage = new Image(new FileInputStream("C:\\Users\\HP\\Desktop\\World Map\\Map\\src\\map.png"));
            if (mapImage.isError()) {
                return false;
            }
            
            mapView = new ImageView(mapImage);
            mapView.setFitWidth(1200);
            mapView.setFitHeight(700);
            mapView.setPreserveRatio(true);
            
            mapPane.getChildren().clear();
            mapPane.getChildren().add(mapView);
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void loadCapitals() {
        try (BufferedReader br = new BufferedReader(new FileReader(CAPITALS_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null && capitalCount < MAX_CAPITALS) {
                try {
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        // The first part is the name
                        String name = parts[0].trim();
                        
                        // Make sure we have valid coordinates
                        try {
                            double lat = Double.parseDouble(parts[1].trim());
                            double lon = Double.parseDouble(parts[2].trim());
                            capitals[capitalCount++] = new Capital(name, lat, lon);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid coordinates for capital: " + name);
                            continue;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing line: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            showErrorDialog("Error Loading Capitals", 
                          "Failed to load capitals data from: " + CAPITALS_FILE_PATH + "\nError: " + e.getMessage());}
        }
    private void plotAllCapitals() {
        for (int i = 0; i < capitalCount; i++) {
            plotCapital(capitals[i], Color.PURPLE, 2);
        }
    }
    
    private void plotCapital(Capital capital, Color color, double radius) {
    double[] coords = calculateCapitalPosition(capital);
    double x = coords[0];
    double y = coords[1];
    
    Circle point = createCapitalPoint(x, y, radius, color);
    Label nameLabel = createCapitalLabel(capital.name, x, y, radius);
    Tooltip tooltip = createCapitalTooltip(capital);
    
    Tooltip.install(point, tooltip);
    setupCapitalInteractions(point, nameLabel, radius, color, capital);
    
    mapPane.getChildren().addAll(point, nameLabel);
    bindScaling(point, nameLabel);
}
    
    private double[] calculateCapitalPosition(Capital capital) {
        double mapWidth = 1200;
        double mapHeight = 700;
        
        double lat = Math.max(Math.min(capital.latitude, MERCATOR_MAX_LATITUDE), -MERCATOR_MAX_LATITUDE);
        double lon = normalizeLongitude(capital.longitude);
        
        // Add offset values to adjust position
        double xOffset = -80;  // Negative moves points left, positive moves right
        double yOffset = 106;   // Positive moves points down, negative moves up
        
        // Calculate x coordinate with offset
        double x = (lon + 180) * (mapWidth / 360) + xOffset;
        
        // Calculate y coordinate with Mercator projection and offset
        double latRad = Math.toRadians(lat);
        double mercN = Math.log(Math.tan((Math.PI/4) + (latRad/2)));
        double y = (mapHeight/2) - (mapWidth * mercN/(2*Math.PI)) + yOffset;
        
        return new double[]{x, y};
    }
    
    private double normalizeLongitude(double lon) {
        while (lon > 180) lon -= 360;
        while (lon < -180) lon += 360;
        return lon;
    }
    
    private Circle createCapitalPoint(double x, double y, double radius, Color color) {
        Circle point = new Circle(x, y, radius, color);
        point.setStroke(Color.WHITE);
        point.setStrokeWidth(1);
        return point;
    }
    
    private Label createCapitalLabel(String name, double x, double y, double radius) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("capital-label");
        nameLabel.setTextFill(Color.BLACK);
        
        Platform.runLater(() -> {
            nameLabel.setLayoutX(x - nameLabel.getWidth() / 2);
            nameLabel.setLayoutY(y - radius - 20);
        });
        
        return nameLabel;
    }
    
    private Tooltip createCapitalTooltip(Capital capital) {
        return new Tooltip(String.format("%s\nLatitude: %.2f°%s\nLongitude: %.2f°%s",
            capital.name,
            Math.abs(capital.latitude), (capital.latitude >= 0 ? "N" : "S"),
            Math.abs(capital.longitude), (capital.longitude >= 0 ? "E" : "W")
        ));
    }
    
    private void setupCapitalInteractions(Circle point, Label nameLabel, double radius, Color color, Capital capital) {
        point.setOnMouseClicked(e -> {
            if (isSelectingSource) {
                // Set source
                sourceCombo.setValue(capital);
                isSelectingSource = false;
            } else {
                // Set target if it's not the same as source
                if (capital != sourceCombo.getValue()) {
                    targetCombo.setValue(capital);
                    isSelectingSource = true;
                }
            }
        });
        
        // Simple hover effect just for visual feedback
        point.setOnMouseEntered(e -> {
            point.setRadius(radius * 1.5);
            nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        });
        
        point.setOnMouseExited(e -> {
            point.setRadius(radius);
            nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        });
    }
    
    private void bindScaling(Circle point, Label nameLabel) {
        point.scaleXProperty().bind(mapGroup.scaleXProperty().multiply(-0.2).add(1));
        point.scaleYProperty().bind(mapGroup.scaleYProperty().multiply(-0.2).add(1));
        nameLabel.scaleXProperty().bind(mapGroup.scaleXProperty().multiply(-0.2).add(1));
        nameLabel.scaleYProperty().bind(mapGroup.scaleYProperty().multiply(-0.2).add(1));
    }
    
    private void setupZoomHandlers() {
        scrollPane.setOnScroll(event -> {
            if (event.isControlDown()) {
                event.consume();
                double zoomFactor = event.getDeltaY() > 0 ? 1.2 : 0.8;
                zoomAtPoint(zoomFactor, event.getSceneX(), event.getSceneY());
            }
        });
    }
    
    private void zoomAtPoint(double factor, double x, double y) {
        currentScale *= factor;
        currentScale = Math.max(1.0, Math.min(currentScale, 5.0));
    
        // Calculate new dimensions for the content
        double newWidth = initialWidth * currentScale;
        double newHeight = initialHeight * currentScale;
    
        // Update the dimensions of the contentGroup and scrollContent
        contentGroup.setScaleX(currentScale);
        contentGroup.setScaleY(currentScale);
    
        // Update scrollContent's minWidth and minHeight for scrollable area
        ((StackPane) scrollPane.getContent()).setMinWidth(newWidth);
        ((StackPane) scrollPane.getContent()).setMinHeight(newHeight);
    
        // Calculate scroll pane viewport center
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
    
        // Get mouse position relative to content
        Point2D mousePoint = contentGroup.sceneToLocal(x, y);
    
        // Calculate new scroll positions to maintain zoom center
        double scrollH = (mousePoint.getX() * currentScale - viewportWidth / 2) / 
                         (newWidth - viewportWidth);
        double scrollV = (mousePoint.getY() * currentScale - viewportHeight / 2) / 
                         (newHeight - viewportHeight);
    
        // Apply scroll positions
        scrollPane.setHvalue(Math.max(0, Math.min(1, scrollH)));
        scrollPane.setVvalue(Math.max(0, Math.min(1, scrollV)));
    
        // Force layout update
        scrollPane.layout();
    }
    

    private void zoom(double factor) {
        // Get the center of the viewport
        double centerX = scrollPane.getViewportBounds().getWidth() / 2;
        double centerY = scrollPane.getViewportBounds().getHeight() / 2;
        
        // Convert viewport center to scene coordinates
        Point2D center = scrollPane.localToScene(centerX, centerY);
        
        // Use the center point for zooming
        zoomAtPoint(factor, center.getX(), center.getY());
    }

    private void resetView() {
        currentScale = 1.0;
        contentGroup.setScaleX(1.0);
        contentGroup.setScaleY(1.0);
        contentGroup.setTranslateX(0);
        contentGroup.setTranslateY(0);
        
        // Center the content
        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);
        
        scrollPane.layout();
    }
    
    private void highlightCapitalWithoutZoom(Capital capital) {
        mapPane.getChildren().clear();
        mapPane.getChildren().add(mapView);
        plotAllCapitals();
        plotCapital(capital, Color.BLUE, 6);
    }
    
   // Add this helper method if not already present
private void showErrorDialog(String header, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Error");
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.showAndWait();
}

private void loadFlightData() {
    flightGraph = new FlightGraph();
    try (BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\HP\\Desktop\\World Map\\Map\\Capital Coordinates.txt"))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length >= 4) {
                String source = parts[0].trim();
                String destination = parts[1].trim();
                double cost = Double.parseDouble(parts[2].replace("$", "").trim());
                int duration = Integer.parseInt(parts[3].replace("min", "").trim());
                
                flightGraph.addFlight(source, destination, cost, duration);
            }
        }
    } catch (IOException e) {
        showErrorDialog("Error Loading Flight Data", "Failed to load flight data");
    }
}

private PathResult findShortestPath(String source, String destination, String criteria) {
    int n = flightGraph.getCityCount();
    double[] distances = new double[n];
    boolean[] visited = new boolean[n];
    int[] previous = new int[n];
    double[] costs = new double[n];
    int[] durations = new int[n];
    
    // Initialize arrays
    for (int i = 0; i < n; i++) {
        distances[i] = Double.POSITIVE_INFINITY;
        previous[i] = -1;
        costs[i] = 0;
        durations[i] = 0;
    }
    
    int sourceIdx = flightGraph.getCityIndexByName(source);
    int destIdx = flightGraph.getCityIndexByName(destination);
    
    // Validate source and destination
    if (sourceIdx == -1 || destIdx == -1) {
        System.err.println("Invalid source or destination city");
        System.err.println("Source: " + source + " (index: " + sourceIdx + ")");
        System.err.println("Destination: " + destination + " (index: " + destIdx + ")");
        return new PathResult(0); // Return empty path
    }
    
    distances[sourceIdx] = 0;
    
    while (true) {
        double minDist = Double.POSITIVE_INFINITY;
        int current = -1;
        
        for (int i = 0; i < n; i++) {
            if (!visited[i] && distances[i] < minDist) {
                minDist = distances[i];
                current = i;
            }
        }
        
        if (current == -1 || current == destIdx) break;
        
        visited[current] = true;
        
        for (int next = 0; next < n; next++) {
            Flight flight = flightGraph.getDirectFlight(current, next);
            if (flight != null && !visited[next]) {
                String currentCity = flightGraph.getCity(current);
                String nextCity = flightGraph.getCity(next);
                
                // Debug logging
                System.out.println("Checking flight: " + currentCity + " -> " + nextCity);
                
                Capital currentCapital = findCapitalByName(currentCity);
                Capital nextCapital = findCapitalByName(nextCity);
                
                // Skip if either capital is not found
                if (currentCapital == null || nextCapital == null) {
                    System.err.println("Warning: Could not find capital data for " + 
                        (currentCapital == null ? currentCity : nextCity));
                    continue;
                }
                
                double newDistance;
                switch (criteria) {
                    case "Less Cost":
                        newDistance = costs[current] + flight.cost;
                        break;
                    case "Less Time":
                        newDistance = durations[current] + flight.duration;
                        break;
                    default: // Shortest Distance
                        newDistance = distances[current] + calculateDistance(currentCapital, nextCapital);
                        break;
                }
                
                if (newDistance < distances[next]) {
                    distances[next] = newDistance;
                    previous[next] = current;
                    costs[next] = costs[current] + flight.cost;
                    durations[next] = durations[current] + flight.duration;
                }
            }
        }
    }
    
    // Validate path exists
    if (distances[destIdx] == Double.POSITIVE_INFINITY) {
        System.err.println("No valid path found between " + source + " and " + destination);
        return new PathResult(0); // Return empty path

    }
    
    // Reconstruct path
    PathResult result = new PathResult(n);
    int current = destIdx;
    
    while (current != -1) {
        String cityName = flightGraph.getCity(current);
        // Validate city exists in capitals
        if (findCapitalByName(cityName) == null) {
            System.err.println("Warning: City in path not found in capitals: " + cityName);
        }
        result.path[result.pathLength++] = cityName;
        current = previous[current];
    }
    
    // Reverse the path
    for (int i = 0; i < result.pathLength / 2; i++) {
        String temp = result.path[i];
        result.path[i] = result.path[result.pathLength - 1 - i];
        result.path[result.pathLength - 1 - i] = temp;
    }
    
    result.totalCost = costs[destIdx];
    result.totalDuration = durations[destIdx];
    
    return result;
}

// Update findCapitalByName to be case-insensitive and trim whitespace
private Capital findCapitalByName(String name) {
    if (name == null) return null;
    String searchName = name.trim();
    for (int i = 0; i < capitalCount; i++) {
        if (capitals[i].name.trim().equalsIgnoreCase(searchName)) {
            return capitals[i];
        }
    }
    System.err.println("Capital not found: " + name);
    return null;
}

private double calculateTotalDistance(String[] path, int pathLength) {
    double totalDistance = 0;
    for (int i = 0; i < pathLength - 1; i++) {
        Capital source = findCapitalByName(path[i]);
        Capital target = findCapitalByName(path[i + 1]);
        totalDistance += calculateDistance(source, target);
    }
    return totalDistance;
}
    public static void main(String[] args) {
        launch(args);
    }
}