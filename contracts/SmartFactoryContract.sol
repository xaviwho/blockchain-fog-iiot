// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title SmartFactoryContract
 * @dev Contract for storing and managing manufacturing data from smart factories
 */
contract SmartFactoryContract {
    address public owner;
    
    // Events for logging
    event MeasurementRecorded(string machineId, string timestamp, uint256 setpoint, uint256 actualValue);
    event BatchDataRecorded(string machineId, string dataHash, string startTime, string endTime, uint256 anomalyScore);
    event AnomalyDetected(string machineId, string timestamp, uint256 anomalyScore, string details);
    
    // Struct for individual measurements
    struct Measurement {
        string timestamp;
        uint256 setpoint;
        uint256 actualValue;
    }
    
    // Struct for batch data
    struct BatchData {
        string dataHash;
        string startTime;
        string endTime;
        uint256 anomalyScore;
    }
    
    // Mapping of machine IDs to their latest measurements
    mapping(string => Measurement) public latestMeasurements;
    
    // Mapping of machine IDs to batch data array indices
    mapping(string => uint256[]) public machineBatchIndices;
    
    // Array of all batch data records
    BatchData[] public batchRecords;
    
    // Access control
    mapping(address => bool) public authorizedClients;
    
    /**
     * @dev Constructor sets the owner and adds them as authorized
     */
    constructor() {
        owner = msg.sender;
        authorizedClients[msg.sender] = true;
    }
    
    /**
     * @dev Modifier to restrict access to owner
     */
    modifier onlyOwner() {
        require(msg.sender == owner, "Only the owner can call this function");
        _;
    }
    
    /**
     * @dev Modifier to restrict access to authorized clients
     */
    modifier onlyAuthorized() {
        require(authorizedClients[msg.sender], "Not authorized");
        _;
    }
    
    /**
     * @dev Add a new authorized client
     * @param client Address of the client to authorize
     */
    function addAuthorizedClient(address client) public onlyOwner {
        authorizedClients[client] = true;
    }
    
    /**
     * @dev Remove an authorized client
     * @param client Address of the client to remove
     */
    function removeAuthorizedClient(address client) public onlyOwner {
        require(client != owner, "Cannot remove owner");
        authorizedClients[client] = false;
    }
    
    /**
     * @dev Record a new measurement for a machine
     * @param machineId ID of the machine
     * @param timestamp Timestamp of the measurement
     * @param setpoint Setpoint value
     * @param actualValue Actual measured value
     */
    function recordMeasurement(
        string memory machineId,
        string memory timestamp,
        uint256 setpoint,
        uint256 actualValue
    ) public onlyAuthorized {
        // Update latest measurement
        latestMeasurements[machineId] = Measurement(timestamp, setpoint, actualValue);
        
        // Emit event
        emit MeasurementRecorded(machineId, timestamp, setpoint, actualValue);
    }
    
    /**
     * @dev Record batch data for a machine
     * @param machineId ID of the machine
     * @param dataHash Hash of the batch data
     * @param startTime Start timestamp of batch
     * @param endTime End timestamp of batch
     * @param anomalyScore Anomaly score (0-1000, scaled from float 0-1)
     */
    function recordBatchData(
        string memory machineId,
        string memory dataHash,
        string memory startTime,
        string memory endTime,
        uint256 anomalyScore
    ) public onlyAuthorized {
        // Add batch record
        uint256 batchIndex = batchRecords.length;
        batchRecords.push(BatchData(dataHash, startTime, endTime, anomalyScore));
        
        // Associate with machine
        machineBatchIndices[machineId].push(batchIndex);
        
        // Emit event
        emit BatchDataRecorded(machineId, dataHash, startTime, endTime, anomalyScore);
        
        // If anomaly score is high, emit anomaly event
        if (anomalyScore > 750) { // 0.75 on 0-1 scale
            emit AnomalyDetected(machineId, endTime, anomalyScore, "High anomaly score detected");
        }
    }
    
    /**
     * @dev Get the latest measurement for a machine
     * @param machineId ID of the machine
     * @return timestamp The timestamp of the measurement
     * @return setpoint The setpoint value
     * @return actualValue The actual measured value
     */
    function getLatestMeasurement(string memory machineId) public view returns (
        string memory timestamp,
        uint256 setpoint,
        uint256 actualValue
    ) {
        Measurement memory measurement = latestMeasurements[machineId];
        return (measurement.timestamp, measurement.setpoint, measurement.actualValue);
    }
    
    /**
     * @dev Get batch data for a machine
     * @param machineId ID of the machine
     * @param startIndex Start index for pagination
     * @param count Number of records to retrieve
     * @return dataHashes Array of data hash strings
     * @return startTimes Array of start time strings
     * @return endTimes Array of end time strings
     * @return anomalyScores Array of anomaly scores
     */
    function getMachineBatchData(
        string memory machineId,
        uint256 startIndex,
        uint256 count
    ) public view returns (
        string[] memory dataHashes,
        string[] memory startTimes,
        string[] memory endTimes,
        uint256[] memory anomalyScores
    ) {
        uint256[] memory indices = machineBatchIndices[machineId];
        
        // Validate indices
        if (startIndex >= indices.length) {
            return (new string[](0), new string[](0), new string[](0), new uint256[](0));
        }
        
        // Calculate actual count (handling bounds)
        uint256 actualCount = count;
        if (startIndex + count > indices.length) {
            actualCount = indices.length - startIndex;
        }
        
        // Initialize arrays
        dataHashes = new string[](actualCount);
        startTimes = new string[](actualCount);
        endTimes = new string[](actualCount);
        anomalyScores = new uint256[](actualCount);
        
        // Fill arrays
        for (uint256 i = 0; i < actualCount; i++) {
            uint256 batchIndex = indices[startIndex + i];
            BatchData memory batch = batchRecords[batchIndex];
            
            dataHashes[i] = batch.dataHash;
            startTimes[i] = batch.startTime;
            endTimes[i] = batch.endTime;
            anomalyScores[i] = batch.anomalyScore;
        }
        
        return (dataHashes, startTimes, endTimes, anomalyScores);
    }
    
    /**
     * @dev Get the count of batch records for a machine
     * @param machineId ID of the machine
     * @return Count of batch records
     */
    function getMachineBatchCount(string memory machineId) public view returns (uint256) {
        return machineBatchIndices[machineId].length;
    }
    
    /**
     * @dev Report an anomaly directly
     * @param machineId ID of the machine
     * @param timestamp Timestamp of the anomaly
     * @param anomalyScore Anomaly score (0-1000)
     * @param details Details of the anomaly
     */
    function reportAnomaly(
        string memory machineId,
        string memory timestamp,
        uint256 anomalyScore,
        string memory details
    ) public onlyAuthorized {
        emit AnomalyDetected(machineId, timestamp, anomalyScore, details);
    }
}