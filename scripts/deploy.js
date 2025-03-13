// Deploy script for Smart Factory Contract
const hre = require("hardhat");

async function main() {
  console.log("Deploying Smart Factory Contract to PureChain...");

  // Get the contract factory
  const SmartFactoryContract = await hre.ethers.getContractFactory("SmartFactoryContract");
  
  // Deploy the contract
  const smartFactory = await SmartFactoryContract.deploy();
  
  // Wait for deployment to finish
  await smartFactory.waitForDeployment();
  
  // Get the contract address
  const contractAddress = await smartFactory.getAddress();
  
  console.log(`Smart Factory Contract deployed to: ${contractAddress}`);
  console.log("Transaction hash:", smartFactory.deploymentTransaction().hash);
  
  console.log("\nCopy this address to use in your Java application!");
}

// Execute the deployment
main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });