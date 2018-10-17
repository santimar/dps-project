This is the code of my project made for "Distributed and Pervasive Systems" exam


The project consists in a network of sensors that collects data about pollution in a virtual city.  
There are four modules in this project:

### Server
There's a server that collects all the statistics about pollution for a later analysis.  
It's also the entry point for all the nodes, so that they can know each other  
Nodes are organized on a 100x100 grid, and nodes must be at least 20 units away from each other  
This is a REST server

### Node
Nodes form a network, where they can enter and exit  
One of the nodes is the coordinator, and it aggregate the datas received by other nodes and then send them to the the server.  
At first, the node try to connect to the server in a random generated grid position (the server can deny the insertion if another node is too close)  
A node (coordinator included) receives pollution data by sensors, store them in memory and calculate a mean by the sliding window with partial overlap technique  
Then the regular nodes try to send the mean of the measurements to the coordinator  

Nodes can turn off, so the coordinator may be missing at some point.  
Nodes must self organize and elect a new coordinator via bully election  
All comunication are made via [protocol-buffer](https://github.com/protocolbuffers/protobuf)  

### Sensor
A sensor produces datas about pollution, and sends them to the closest node (information given by the server)

### Analyst
A simple client that can connect to the server and read the collected stats
