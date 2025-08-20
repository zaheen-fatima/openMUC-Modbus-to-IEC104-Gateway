# Modbus to IEC 104 Bridge (OpenMUC)

This project implements a bridge between **Modbus** and **IEC 60870-5-104** using the [OpenMUC framework](https://www.openmuc.org/).  
It reads data from a Modbus channel and forwards it to an IEC 104 client in real time.

---

## ✨ Features
- Built on **OpenMUC** data access framework
- Reads values from Modbus register (`register1`)
- Runs an **IEC 104 Server** on port `2404`
- Forwards Modbus values to IEC 104 as **M_ME_NA_1 (normalized values)**
- Handles **General Interrogation (C_IC_NA_1)** requests from clients
- Logs all communication events (connect, disconnect, ASdu received, values sent)

---

## 🏗️ Project Structure
- **`Iec104Server.java`** → IEC 104 server implementation  
- **`ModbusApp.java`** → OpenMUC application component that listens to Modbus channel and forwards to IEC 104  

---

## ⚙️ Prerequisites
- Java 11+  
- Maven/Gradle build system  
- OpenMUC framework (installed and configured with `channels.xml`)  
- Modbus device or simulator  
- IEC 104 client (e.g. Freyr, Wireshark for monitoring)

---

## 🚀 How to Run
1. Start OpenMUC framework.  
2. Ensure `channels.xml` defines a Modbus channel `register1`.  
3. Deploy this app bundle in OpenMUC.  
4. Start an IEC 104 client (e.g. Freyr, port 2404).  
5. Observe logs:
   - When client connects/disconnects
   - When Modbus values update
   - When IEC 104 values are sent

---

## 📡 Example Flow
1. Modbus register updates → OpenMUC notifies via `RecordListener`  
2. `ModbusApp` receives the new value  
3. `Iec104Server.updateValue(ioa, value)` sends the update to IEC 104 client. 
