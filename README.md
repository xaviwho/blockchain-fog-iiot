# Lightweight Blockchain-Integrated Fog Computing for Smart Factory Data Processing

This project implements a smart factory data processing system that combines blockchain technology with fog computing. It processes continuous flow manufacturing data, detects anomalies, and stores critical data on a blockchain for security and transparency.

## System Architecture

The system consists of the following components:

1. **Data Processing Layer**
   - `DataLoader`: Loads and parses the factory data CSV
   - `HistoricalDataProcessor`: Analyzes historical data for insights
   - `AIAnomalyDetector`: Detects anomalies in the manufacturing process

2. **Fog Computing Layer**
   - `FactoryFogTopology`: Manages the fog computing nodes
   - `FogNode`: Individual fog computing node (one per machine/process)

3. **Blockchain Layer**
   - `PureChainConnector`: Connects to the PureChain blockchain
   - `BlockchainLogger`: Logs important data to the blockchain
   - `SmartFactoryContract`: Solidity smart contract for on-chain data

## Data Flow

1. Factory sensor data is collected from machines (simulated using CSV data)
2. Data is processed through fog computing nodes at the edge
3. Anomalies are detected using statistical methods
4. Critical data and anomalies are recorded on the blockchain
5. Historical data is analyzed for process improvement recommendations

## Setup Instructions

### Prerequisites

- Java 11 or higher
- Node.js 14 or higher
- NPM or Yarn
- Hardhat for blockchain development
- MetaMask with access to the PureChain network

### PureChain Configuration

1. Configure MetaMask to connect to PureChain network:
   - Network Name: PureChain
   - RPC URL: http://43.200.53.250:8548
   - Chain ID: 900520900520
   - Currency Symbol: PCC

2. Install Hardhat and dependencies:
   ```
   npm install --save-dev hardhat @nomicfoundation/hardhat-toolbox
   ```

3. Copy the smart contract to the contracts directory:
   ```
   mkdir -p contracts
   cp SmartFactoryContract.sol contracts/
   ```

4. Update the private key in `BlockchainConfig.java` with your MetaMask private key:
   ```java
   private static final String DEFAULT_PRIVATE_KEY = "YOUR_PRIVATE_KEY_HERE";
   ```
   
   > **IMPORTANT**: Never commit your private key to version control! For production use,
   > set it using the environment variable `BLOCKCHAIN_PRIVATE_KEY` instead.

5. Deploy the smart contract to PureChain:
   ```
   npx hardhat run scripts/deploy.js --network purechain
   ```
   
6. Update the contract address in `BlockchainConfig.java`:
   ```java
   private static final String DEFAULT_CONTRACT_ADDRESS = "YOUR_DEPLOYED_CONTRACT_ADDRESS";
   ```
   
   > For production use, set it using the environment variable `SMART_FACTORY_CONTRACT_ADDRESS`.

### Building and Running

1. Build the Java project:
   ```
   ./gradlew build
   ```

2. Run the simulation:
   ```
   ./gradlew run
   ```

## Implementation Details

### Key Components

- **Smart Contract**: The `SmartFactoryContract.sol` defines the on-chain data structure and access control
- **Data Models**: Java classes modeling the factory data structure
- **Fog Computing**: The topology of fog nodes processing data at the edge
- **Anomaly Detection**: Statistical methods to detect process anomalies
- **Historical Analysis**: Finding correlations and making recommendations

### Performance Considerations

- **Blockchain Usage Optimization**: Only critical data is stored on-chain
- **Batching**: Data is processed in batches to reduce blockchain transactions
- **Fog Computing**: Processing is distributed to edge nodes for efficiency
- **Asynchronous Processing**: Non-blocking operations for better throughput

## Future Improvements

1. Implement machine learning models for more advanced anomaly detection
2. Add real-time visualization dashboard
3. Enhance smart contract with automated response to critical anomalies
4. Implement IPFS for storing larger data sets with only hashes on-chain
5. Add support for multiple factories with federated learning across sites

## License

This project is licensed under the MIT License - see the LICENSE file for details.