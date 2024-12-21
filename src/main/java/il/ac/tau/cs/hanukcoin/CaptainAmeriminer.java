package il.ac.tau.cs.hanukcoin;

import java.util.ArrayList;

public class CaptainAmeriminer {
    public static int NumOfActiveThreads;
    public static String wallet = "CaptainAmerica";
    public static String[] Colors = {"\u001B[31;1m", "\u001B[32m;1", "\u001B[36;1m"};
    public static String A_RESET = "\u001B[0m";
    public static int MainLastBlock = -1;
    public static ArrayList<int[]> Statistics = new ArrayList<>();
    public static int blocksThoughtToBeMined = 0;

    public static void main(String[] args) throws Exception {
        NumOfActiveThreads = 5; // I chose 5, cuz I have total 8
        AddToChain addToChain = new AddToChain();
        ShowChain showChain = new ShowChain();
        ShowChain.main(args);
        // Main loop
        while (true) {
            MainLastBlock = ShowChain.lastBlock.getWalletNumber();
            Statistics.add(addToChain.main2(args));
            blocksThoughtToBeMined++;
            ShowChain.main(args);
            int EnterThing = 0;
            int waitTime = (int) (500 * Math.log(ShowChain.lastBlock.getSerialNumber())/Math.log(2));
            // Waiting until someone mines
            if (wallet.equals("CaptainAmerica")) {
                wallet = "captainAmerica";
            }
            else {
                wallet = "CaptainAmerica";
            }
            while (ShowChain.lastBlock.getWalletNumber() == HanukCoinUtils.walletCode(wallet)) {
                if (wallet.equals("CaptainAmerica")) {
                    wallet = "captainAmerica";
                }
                else {
                    wallet = "CaptainAmerica";
                }
                //System.out.print("🔗");
                EnterThing++;
                if (EnterThing > 20) {
                    EnterThing = 0;
                    System.out.println(" : 20 attempts :" + Colors[0] + " no activity" + A_RESET);
                    waitTime = waitTime + 300;
                }

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    System.out.println("Wait interrupted: " + e.getMessage());
                }

                showChain.main(args);
            }
            System.out.println(" ");
            //System.out.println("Last block is not ours anymore, starting to mine...");
            if (blocksThoughtToBeMined % 5 == 0) {
                MiningStats.printData(Statistics);
            }

            if (blocksThoughtToBeMined > 29) {
                throw new Exception("Sampling is over");
            }
        }
    }
    public static ArrayList<int[]> getStats() {
        return Statistics;
    }

    public static void main2(String[] args) throws Exception {

    }

    public static boolean validate_chain(ArrayList<Block> chain) {
        if (!Block.two_blocks_equal(chain.get(0), HanukCoinUtils.createBlock0forTestStage())) {
            return false;
            // Check if the first block in the chain is the same as the genesis block
        }
        for (int i = 1; i < chain.size() - 1; i++) {
            if (chain.get(i).getSerialNumber() + 1 != chain.get(i + 1).getSerialNumber()){
                return false;
            }
            if (chain.get(i).getWalletNumber() == chain.get(i + 1).getWalletNumber()){
                return false;
                // Ensure each serial number is 1 more than the previous one, and no one won
                // 2 blocks in a row
            }

            if (chain.get(i).checkSignature() != Block.BlockError.OK) {
                return false;
                // Check if the sig is valid
            }
            if (!HanukCoinUtils.ArraysPartEquals(8, chain.get(i + 1).data, 8, chain.get(i).data, 24)){
                return false;
                // Check if the first 8 bits in the sig field is the same as
                // The prevSig field in the next block
            }
        }

        return true;
    }
}