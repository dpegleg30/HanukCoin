package il.ac.tau.cs.hanukcoin;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class CaptainAmeriminer {
    public static int NumOfActiveThreads;
    public static String wallet = "captainamerica";
    public static String[] Colors = {"\u001B[31;1m", "\u001B[32m;1", "\u001B[36;1m"};
    public static String A_RESET = "\u001B[0m";
    public static int MainLastBlock = -1;
    public static ArrayList<int[]> Statistics = new ArrayList<>();
    public static int blocksThoughtToBeMined = 0;
    public static boolean running = true;

    public static void main(String[] args) throws Exception {
        NumOfActiveThreads = 6;
        ShowChain showChain = new ShowChain();
        RunningGpuMiner miner = new RunningGpuMiner();
        ShowChain.main(args);
        ChainStore.writeDataToFile();

        doStuffWithTheServerShit();

        // Main loop
        while (running) {
            int EnterThing = 0;
            int waitTime = (int) (500 * Math.log(ChainStore.getLastBlock().getSerialNumber())/Math.log(2));
            // Waiting until someone mines
            while (ChainStore.getLastBlock().getWalletNumber() == HanukCoinUtils.walletCode(wallet)) {
                System.out.print("🔗");
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

            MainLastBlock = ChainStore.getLastBlock().getWalletNumber();
//            if (ChainStore.getLastBlock().getWalletNumber() == HanukCoinUtils.walletCode("captainamerica")) {
//                wallet = "Claude"; //Claude did half the job, deserves credit
//            }
//            else {
//                wallet = "captainamerica";
//            }
            Block newBlock = null;
            double t0 = System.currentTimeMillis();
            System.out.println(blocksThoughtToBeMined);
            while (newBlock == null) {
                newBlock = miner.mineCoin(HanukCoinUtils.walletCode(wallet), ChainStore.getLastBlock(),HanukCoinUtils.numberOfZerosForPuzzle(ChainStore.getLastBlock().getSerialNumber()));
                if (newBlock != null) {
                    System.out.println(newBlock.binDump());
                    double t1 = System.currentTimeMillis();
                    Statistics.add(new int[] {ChainStore.getLastBlock().getSerialNumber(), (int) (t1 - t0), 0});
                }
                RunningGpuMiner.startVal += 4294967295L;
            }
//            Statistics.add(AddToChain.main2(args));
            RunningGpuMiner.startVal = 0L;
            ChainStore.addBlock(newBlock);
            blocksThoughtToBeMined++;
            AddToChain.main(args);
            ChainStore.writeDataToFile();

//            if (ChainStore.getLastBlock().getWalletNumber() == HanukCoinUtils.walletCode("captainamerica")) {
//                wallet = "Claude"; //Claude did half the job, deserves credit
//            }
//            else {
//                wallet = "captainamerica";
//            }



            System.out.println(" ");
            //System.out.println("Last block is not ours anymore, starting to mine...");

            //disable in the Final run - limits the blocks per run to some number.
            //Could be changed as you wish
            if (blocksThoughtToBeMined < 0) {
                System.out.println(Colors[0] + "Sampling session has ended" + A_RESET);
                running = false;
            };
        }
        System.out.println("dunno I think Im done here \n so yeah \n Like & subscribe for more HanukCoin Shei#e by me");
    }
    public static ArrayList<int[]> getStats() {
        return Statistics;
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

    public static String getStatisticsJson() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < Statistics.size(); i++) {
            int[] stat = Statistics.get(i);
            json.append("[").append(stat[0]).append(",")
                    .append(stat[1]).append(",")
                    .append(stat[2]).append("]");
            if (i < Statistics.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    public static void doStuffWithTheServerShit() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/stats", (exchange -> {
            String response = getStatisticsJson();
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        }));
        server.setExecutor(null);
        server.start();
        System.out.println("did smth with the server");
    }
}