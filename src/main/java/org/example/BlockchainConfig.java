package org.example;

/**
 * Configuration for blockchain connection
 * Store all blockchain-related configuration in one place
 */
public class BlockchainConfig {
    // PureChain configuration
    public static final String RPC_URL = "http://43.200.53.250:8548";
    public static final long CHAIN_ID = 900520900520L;
    public static final String CURRENCY_SYMBOL = "PCC";
    
    // Replace this with your private key from MetaMask
    // WARNING: Never commit your actual private key to version control!
    // For production, load this from an environment variable or secure config file
    private static final String DEFAULT_PRIVATE_KEY = "a471850a08d06bcc47850274208275f1971c9f5888bd0a08fbc680ed9701cfda";
    
    // Contract address - replace with your deployed contract address
    private static final String DEFAULT_CONTRACT_ADDRESS = "0x3E506b370353fBEe960d7bd446833079340410a6";
    
    /**
     * Get the RPC URL
     * @return Blockchain RPC URL
     */
    public static String getRPC_URL() {
        String envUrl = System.getenv("BLOCKCHAIN_RPC_URL");
        return (envUrl != null && !envUrl.isEmpty()) ? envUrl : RPC_URL;
    }
    
    /**
     * Get the private key, preferring environment variable if set
     * @return Private key for blockchain transactions
     */
    public static String getPrivateKey() {
        String envKey = System.getenv("BLOCKCHAIN_PRIVATE_KEY");
        return (envKey != null && !envKey.isEmpty()) ? envKey : DEFAULT_PRIVATE_KEY;
    }
    
    /**
     * Get the contract address, preferring environment variable if set
     * @return Contract address on blockchain
     */
    public static String getContractAddress() {
        String envAddress = System.getenv("SMART_FACTORY_CONTRACT_ADDRESS");
        return (envAddress != null && !envAddress.isEmpty()) ? envAddress : DEFAULT_CONTRACT_ADDRESS;
    }
}