package org.example;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

/**
 * Connector for integrating with the custom blockchain using Web3j and Hardhat
 */
public class PureChainConnector {
    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final TransactionManager transactionManager;
    private SmartFactoryContract contract;
    
    // Contract address - will be set when deployed
    private String contractAddress;
    
    // Configuration
    private static final String DEFAULT_NODE_URL = "http://43.200.53.250:8548"; // PureChain URL
    
    // Gas configuration
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(6_721_975L);
    
    /**
     * Create a connector with default URL
     * @param privateKey Private key for signing transactions
     */
    public PureChainConnector(String privateKey) {
        this(DEFAULT_NODE_URL, privateKey);
    }
    
    /**
     * Create a connector with custom URL
     * @param nodeUrl Custom blockchain node URL
     * @param privateKey Private key for signing transactions
     */
    public PureChainConnector(String nodeUrl, String privateKey) {
        this.web3j = Web3j.build(new HttpService(nodeUrl));
        this.credentials = Credentials.create(privateKey);
        this.gasProvider = new StaticGasProvider(GAS_PRICE, GAS_LIMIT);
        this.transactionManager = new RawTransactionManager(
            web3j, 
            credentials, 
            BlockchainConfig.CHAIN_ID // Use the correct chain ID for replay protection
        );
        
        System.out.println("Connected to blockchain node: " + nodeUrl);
        System.out.println("Using address: " + credentials.getAddress());
    }
    
    /**
     * Deploy a new Smart Factory Contract
     * @return Contract address
     */
    public String deployContract() {
        try {
            System.out.println("Deploying Smart Factory Contract...");
            
            // Deploy the contract with transaction manager
            contract = SmartFactoryContract.deploy(
                web3j, 
                credentials,
                gasProvider
            );
            
            contractAddress = contract.getContractAddress();
            System.out.println("Contract deployed at: " + contractAddress);
            return contractAddress;
        } catch (Exception e) {
            System.err.println("Error deploying contract: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Load an existing contract
     * @param address Contract address
     * @return true if loaded successfully
     */
    public boolean loadContract(String address) {
        try {
            System.out.println("Loading contract at: " + address);
            this.contractAddress = address;
            
            // Simple verification that the contract exists - make a direct eth_call instead of using the wrapper
            try {
                // Create a call to the owner() function (0x8da5cb5b is the function signature for owner())
                EthCall ethCall = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                        credentials.getAddress(),
                        address,
                        "0x8da5cb5b"
                    ),
                    DefaultBlockParameterName.LATEST
                ).send();
                
                if (ethCall.hasError()) {
                    throw new RuntimeException("Error calling contract: " + ethCall.getError().getMessage());
                }
                
                String result = ethCall.getValue();
                if (result == null || result.equals("0x")) {
                    throw new RuntimeException("No response from contract");
                }
                
                // Extract the address from the result (it's a padded address)
                String ownerAddress = "0x" + result.substring(result.length() - 40);
                System.out.println("Contract owner: " + ownerAddress);
            } catch (Exception e) {
                System.err.println("Warning: Contract verification failed: " + e.getMessage());
                // Continue anyway, as this might be a different contract interface
            }
            
            // Load the contract using our wrapper
            contract = SmartFactoryContract.load(
                address,
                web3j,
                credentials,
                gasProvider
            );
            
            return true;
        } catch (Exception e) {
            System.err.println("Error loading contract: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Record a new data point to the blockchain
     * @param machineId Machine ID
     * @param timestamp Timestamp string
     * @param setpoint Setpoint value
     * @param actualValue Actual measured value
     * @return Transaction hash
     */
    public CompletableFuture<String> recordMeasurement(
            String machineId, 
            String timestamp, 
            BigInteger setpoint, 
            BigInteger actualValue) {
        
        CompletableFuture<String> future = new CompletableFuture<>();
        
        if (contract == null) {
            future.completeExceptionally(new IllegalStateException("Contract not loaded"));
            return future;
        }
        
        System.out.println("Recording measurement for machine: " + machineId);
        
        try {
            contract.recordMeasurement(machineId, timestamp, setpoint, actualValue)
                .sendAsync()
                .thenAccept(receipt -> {
                    System.out.println("Transaction successful: " + receipt.getTransactionHash());
                    future.complete(receipt.getTransactionHash());
                })
                .exceptionally(e -> {
                    System.err.println("Error recording measurement: " + e.getMessage());
                    future.completeExceptionally(e);
                    return null;
                });
        } catch (Exception e) {
            System.err.println("Error sending transaction: " + e.getMessage());
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Record batch data to blockchain (for fog computing nodes)
     * @param machineId Machine ID
     * @param dataHash Hash of processed batch data
     * @param startTime Start timestamp of batch
     * @param endTime End timestamp of batch
     * @param anomalyScore Anomaly score calculated by AI
     * @return Transaction hash
     */
    public CompletableFuture<String> recordBatchData(
            String machineId,
            String dataHash,
            String startTime,
            String endTime,
            BigInteger anomalyScore) {
        
        CompletableFuture<String> future = new CompletableFuture<>();
        
        if (contract == null) {
            future.completeExceptionally(new IllegalStateException("Contract not loaded"));
            return future;
        }
        
        System.out.println("Recording batch data for machine: " + machineId);
        
        try {
            contract.recordBatchData(machineId, dataHash, startTime, endTime, anomalyScore)
                .sendAsync()
                .thenAccept(receipt -> {
                    System.out.println("Batch data recorded: " + receipt.getTransactionHash());
                    future.complete(receipt.getTransactionHash());
                })
                .exceptionally(e -> {
                    System.err.println("Error recording batch data: " + e.getMessage());
                    future.completeExceptionally(e);
                    return null;
                });
        } catch (Exception e) {
            System.err.println("Error sending batch transaction: " + e.getMessage());
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Get the latest measurement for a machine
     * @param machineId Machine ID
     * @return Map containing the measurement data
     */
    public Map<String, Object> getLatestMeasurement(String machineId) {
        try {
            var result = contract.getLatestMeasurement(machineId).send();
            
            // Create a map with the results
            return Map.of(
                "timestamp", result.component1(),
                "setpoint", result.component2(),
                "actualValue", result.component3()
            );
        } catch (Exception e) {
            System.err.println("Error getting latest measurement: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the contract address 
     * @return Contract address
     */
    public String getContractAddress() {
        return contractAddress;
    }
    
    /**
     * Close the web3j connection
     */
    public void shutdown() {
        web3j.shutdown();
        System.out.println("Blockchain connector shut down");
    }
}