class PathResult {
    String[] path;
    int pathLength;
    double totalCost;
    int totalDuration;
    
    public PathResult(int maxSize) {
        path = new String[maxSize];
        pathLength = 0;
        totalCost = 0;
        totalDuration = 0;
    }
}