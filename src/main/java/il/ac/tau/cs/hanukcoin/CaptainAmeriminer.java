package il.ac.tau.cs.hanukcoin;

import java.util.ArrayList;

public class CaptainAmeriminer {
    public static int NumOfActiveThreads;
    public static String wallet = "CaptainAmerica";

    public static void main(String[] args) throws InterruptedException {
        NumOfActiveThreads = 7; // I chose 7, cuz I have total 8
        AddToChain addToChain = new AddToChain();
        ShowChain showChain = new ShowChain();

        while (true) {
            AddToChain.main(args);
            showChain.main(args);

            while (ShowChain.lastBlock.getWalletNumber() == HanukCoinUtils.walletCode(wallet)) {
                int waitTime = (int) (500 * Math.log(showChain.lastBlock.getSerialNumber())/Math.log(2));
                System.out.print("🔗");

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    System.out.println("Wait interrupted: " + e.getMessage());
                }

                showChain.main(args);
            }
            System.out.println(" ");
            System.out.println("Last block is not ours anymore, starting to mine...");
        }
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