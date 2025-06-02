# 📁 Distributed File Synchronization System

This project is a Java-based **Distributed File Synchronization System** that simulates multiple nodes in a network synchronizing files with each other. It uses **RMI (Remote Method Invocation)** and **sockets** to achieve file distribution and consistency.

---

## ⚙️ How It Works

- **Nodes**: Each node holds departmental folders (like `development`, `qa`, `design`) containing files.
- **Coordinator**: A central service that keeps track of all nodes and helps with coordination.
- **RMI**: Used for communication between the coordinator and nodes.
- **SyncServer**: Each node has a TCP socket-based server to respond to sync requests.
- **HeartbeatChecker**: Periodically checks if nodes are alive and healthy.
- **Daily Sync**: A scheduled sync runs at a specific time to ensure all nodes are up to date.
- **Immediate Sync**: Can be triggered manually for testing or debugging.

---

## 🖼️ Architecture Diagram

![Distributed File Sync](A_presentation_slide_titled_"Distributed_File_Sync.png")

---

## 📦 Features

- File synchronization between nodes.
- Automatic file copy/delete operations.
- Daily scheduled synchronization.
- Heartbeat monitoring for node health.
- RMI-based service registry and communication.

---

## 📁 Technologies

- Java
- RMI
- Sockets (ServerSocket/Socket)
- Multithreading

---

## 🧪 How to Compile & Run

To run the project from the terminal, follow these steps:

### 🖥️ Run the Server

```bash
cd src
javac -encoding UTF-8 *.java
java Server
```
### 🖥️ Run the Client

```bash
cd src
javac -encoding UTF-8 *.java
java Client
