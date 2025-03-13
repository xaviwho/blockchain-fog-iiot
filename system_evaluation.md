# System Evaluation Report

Generated: 2025-03-12 22:58:55

## Functional Evaluation

- Data points processed: 14088
- Anomalies detected: 210
- Anomaly rate: 1.49%
- Blockchain transactions: 210
- Blockchain success rate: 100.00%

## Performance Evaluation

- Run time: 2821.14 seconds
- Processing rate: 4.99 points/second
- Average processing time: 0.26 ms/point
- Used memory: 660.26 MB
- Maximum memory: 3988.00 MB
- CPU usage: 1.49609375

## Architecture Evaluation

### Fog Computing Effectiveness

The fog computing topology effectively distributes processing across nodes, with each node handling specific data relevant to its assigned machine or process stage. This approach reduces central processing load and network traffic by processing data at the edge.

### Blockchain Integration

The system correctly identifies critical data (anomalies and key measurements) for blockchain storage, while keeping high-volume raw data processing at the edge. This demonstrates appropriate use of blockchain technology for immutable record-keeping without overwhelming the blockchain with unnecessary data.

### System Resilience

The system demonstrates resilience by continuing to process data even when blockchain connectivity issues occur. The fog node architecture ensures that processing continues independently at each node, with results being stored locally until they can be transmitted to the blockchain.

