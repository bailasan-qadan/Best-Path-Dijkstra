# 🌍 Dijkstra's Algorithm for Optimizing Air Routes

This project was developed for the **Design and Analysis of Algorithms (COM336)** course , and implements **Dijkstra’s Algorithm** to find the most efficient routes between world capitals based on flight cost or travel time.

## 📌 Project Objective

To build a system that:
- Represents the network of world capitals as a graph
- Allows users to find the optimal air route between two cities
- Minimizes either **travel cost** or **travel time**
- Provides an interactive and visual user interface

## ✈️ Features

- Real-world data with at least **50 countries**
- Graph structure representing cities and direct flight routes
- Dual-weighted edges based on:
  - 💰 **Cost**
  - ⏱️ **Time**
- Dijkstra’s Algorithm to compute shortest paths
- User interface to input source and destination cities
- **Route visualization** on a world map (optional feature)
- Ability to prioritize shortest time or lowest cost

## 🛠️ Technologies Used

- **Java** for backend logic
- **JavaFX** for GUI and user interaction
- **CSS** for styling the JavaFX interface
- **Dijkstra’s Algorithm** for pathfinding
- Graph representation via **adjacency matrix**

## 📄 Input File Structure
Input File Structure:

[Number of Capitals]
CapitalName₁, Latitude₁, Longitude₁
CapitalName₂, Latitude₂, Longitude₂
...
CapitalNameₙ, Latitudeₙ, Longitudeₙ

[Number of Flight Routes]
SourceCapital₁, DestinationCapital₁, $Price₁, Duration₁min
SourceCapital₂, DestinationCapital₂, $Price₂, Duration₂min
...
SourceCapitalₘ, DestinationCapitalₘ, $Priceₘ, Durationₘmin

## 🧠 Algorithm Details

Dijkstra’s algorithm finds the shortest path from a source node to all other nodes in a graph with non-negative edge weights. In this project, the algorithm is adapted to prioritize:
- Minimum **cost**
- Minimum **travel time**

The user can select which priority to use.
## 🧮 Calculations

### 📍 Coordinate Conversion
Capital coordinates (latitude and longitude) are converted to x/y positions on the map using a modified Mercator projection:

- **Longitude to x**:  
  `(longitude + 180) * (mapWidth / 360) + xOffset`

- **Latitude to y**:  
  `(mapHeight / 2) - (mapWidth * ln(tan(π/4 + latitudeRadians/2)) / (2π)) + yOffset`

Where:
- `latitudeRadians = Math.toRadians(latitude)`
- `xOffset` and `yOffset` are constants used to adjust map alignment

---

### 📏 Distance Calculation (Haversine Formula)
The geographic distance between two capitals is computed using the Haversine formula:


- `Δlat = lat₂ − lat₁` and `Δlon = lon₂ − lon₁` in **radians**  
- `EARTH_RADIUS_KM = 6371`

---

### 💰 Cost & ⏱️ Time Accumulation
For each computed route, the following totals are calculated:

- **Total Cost** = Sum of all flight segment costs (in USD)
- **Total Time** = Sum of all flight durations (in minutes)

These values are extracted directly from the input data and accumulated during pathfinding.

---

### 🧠 Pathfinding Criteria
Users can select their optimization priority. Based on the selected filter, Dijkstra’s Algorithm uses:

- **Less Cost** → Uses cumulative cost as the weight
- **Less Time** → Uses cumulative time as the weight
- **Shortest Distance** → Uses geographic distance (calculated via Haversine formula) as the weight

## 🖼️ Screenshots of the program
