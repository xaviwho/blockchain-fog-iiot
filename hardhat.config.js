require("@nomicfoundation/hardhat-toolbox");

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: "0.8.19",
  networks: {
    hardhat: {
      chainId: 1337, // Use a specific chainId for compatibility
      mining: {
        auto: true,
        interval: 1000 // Block time in milliseconds
      }
    },
    localhost: {
      url: "http://127.0.0.1:8545",
      accounts: [
        "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80" // Default hardhat account #0
      ]
    },
    purechain: {
      url: "http://43.200.53.250:8548",
      chainId: 900520900520,
      accounts: [
        "a471850a08d06bcc47850274208275f1971c9f5888bd0a08fbc680ed9701cfda"
      ]
    }
  },
  paths: {
    sources: "./contracts",
    tests: "./test",
    cache: "./cache",
    artifacts: "./artifacts"
  }
};