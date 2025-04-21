// Graph representation using adjacency matrix
class FlightGraph {
    private String[] cities;
    private Flight[][] flights;
    private int cityCount;
    private static final int MAX_CITIES = 200;
    
    public FlightGraph() {
        cities = new String[MAX_CITIES];
        flights = new Flight[MAX_CITIES][MAX_CITIES];
        cityCount = 0;
    }
    
    private int getCityIndex(String city) {
        for (int i = 0; i < cityCount; i++) {
            if (cities[i].equals(city)) {
                return i;
            }
        }
        cities[cityCount] = city;
        return cityCount++;
    }
    
    public void addFlight(String source, String destination, double cost, int duration) {
        int sourceIdx = getCityIndex(source);
        int destIdx = getCityIndex(destination);
        
        flights[sourceIdx][destIdx] = new Flight(source, destination, cost, duration);
        flights[destIdx][sourceIdx] = new Flight(destination, source, cost, duration);
    }
    
    public Flight getDirectFlight(int sourceIdx, int destIdx) {
        return flights[sourceIdx][destIdx];
    }
    
    public int getCityCount() {
        return cityCount;
    }
    
    public String getCity(int index) {
        return cities[index];
    }
    
    public int getCityIndexByName(String city) {
        for (int i = 0; i < cityCount; i++) {
            if (cities[i].equals(city)) {
                return i;
            }
        }
        return -1;
    }
}
