class Capital {
    String name;
    double latitude;
    double longitude;
    
    public Capital(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
