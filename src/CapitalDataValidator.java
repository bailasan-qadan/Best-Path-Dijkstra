import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CapitalDataValidator {
    private static final int EXPECTED_COORDINATE_PARTS = 3;
    private static final int EXPECTED_FLIGHT_PARTS = 4;
    private static final Pattern COST_PATTERN = Pattern.compile("\\$\\d+(\\.\\d+)?");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d+min");
    
    private Map<String, Capital> capitalMap = new HashMap<>();
    private List<String> validationErrors = new ArrayList<>();
    
    public class Capital {
        String name;
        Double latitude;
        Double longitude;
        List<Flight> connections = new ArrayList<>();
        
        public Capital(String name, Double latitude, Double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }}