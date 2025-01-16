package il.ac.tau.cs.hanukcoin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;


public class AddToChain {
    public static final int BEEF_BEEF = 0xbeefBeef;
    public static final int DEAD_DEAD = 0xdeadDead;
    public static String[] Colors = {"\u001B[31;1m", "\u001B[32;1m", "\u001B[36;1m", "\u001B[1;3m\u001b[38;2;138;200;200m"};
    public static String A_RESET = "\u001B[0m";
    public static ArrayList<Block> blockList = ChainStore.getBlocklist();
    public static ArrayList<ShowChain.NodeInfo> nodeList = ChainStore.getNodelist();


    public static void log(String fmt, Object... args) {
        //println(fmt, args);
    }

    public static void println(String fmt, Object... args) {
        System.out.format(fmt + "\n", args);
    }

    public static void sendReceive(String host, int port) {
        try {
            log("INFO - Sending request message to %s:%d", host, port);
            Socket soc = new Socket(host, port);
            ClientConnection connection = new ClientConnection(soc);
            connection.sendReceive();
        } catch (IOException e) {
            log("WARN - open socket exception connecting to %s:%d: %s", host, port, e.toString());
        }
    }

    public static int[] main2(String[] args) throws Exception {
        if (args.length != 1 || !args[0].contains(":")) {
            println("ERROR - please provide HOST:PORT");
            return new int[3];
        }
        AtomicInteger threadId2 = new AtomicInteger();
        AtomicLong miningTime2 = new AtomicLong();
        String[] parts = args[0].split(":");
        String addr = parts[0];
        int port = Integer.parseInt(parts[1]);

        // send an "empty" message in order to get the nodes and blocks in the server
        sendReceive(addr, port);

        //last block is ours?
        if (CaptainAmeriminer.MainLastBlock == HanukCoinUtils.walletCode(CaptainAmeriminer.wallet)) {
            return new int[3];
        }
        // Get number of available processors
        int numThreads = CaptainAmeriminer.NumOfActiveThreads;
        if (numThreads > Runtime.getRuntime().availableProcessors()) {
            throw new Exception("I am not happy\n why you try to use " + numThreads + " threads?!");
        }
        System.out.println("Starting mining with " + numThreads + " threads as " + CaptainAmeriminer.wallet + ". Target block : " + Colors[2] + (blockList.get(blockList.size() - 1).getSerialNumber()+1)  + A_RESET);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicReference<Block> minedBlock = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);


        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                try {
                    long start_time = (System.currentTimeMillis());
                    while (minedBlock.get() == null && !Thread.currentThread().isInterrupted()) {
                        Block attempt = HanukCoinUtils.mineCoinAttempt(
                                HanukCoinUtils.walletCode(CaptainAmeriminer.wallet),
                                blockList.get(blockList.size() - 1),
                                100000000
                        );

                        if (attempt != null) {
                            if (minedBlock.compareAndSet(null, attempt)) {
                                long end_time = (System.currentTimeMillis());
                                threadId2.set(threadId);
                                miningTime2.set(((end_time - start_time)));
                                System.out.println(Colors[1] + "SUCCESS! Thread " + threadId + " successfully mined block no." + minedBlock.get().getSerialNumber() + " within " + Colors[3] + (end_time-start_time) + " milliseconds" + A_RESET);
                                latch.countDown();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Thread " + threadId + " encountered error: " + e.getMessage());
                }
            });
        }

        try {
            boolean mined = latch.await(10, TimeUnit.MINUTES);

            if (!mined) {
                System.out.println("Mining timed out after 10 minutes");
                executor.shutdownNow();
                return new int[3];
            }

            Block block = minedBlock.get();
            if (block != null) {
                blockList.add(block);

//                NodeInfo node = new NodeInfo();
//                node.name = "CaptainAmerica";
//                node.host = "82ca-89-139-41-77.ngrok-free.app";
//                node.port = 80;
//                node.lastSeenTS = (int)(System.currentTimeMillis() / 1000);
//
//                nodeList.add(node);

                // send the new node to the server
                sendReceive(addr, port);
            }

        } catch (InterruptedException e) {
            System.out.println("Mining was interrupted: " + e.getMessage());
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.out.println("Error shutting down thread pool: " + e.getMessage());
            }
        }

        return new int[] {minedBlock.get().getSerialNumber(), (int) (miningTime2.get()), threadId2.get()};

    }

    public static void main(String argv[]) {
        if (argv.length != 1 || !argv[0].contains(":")){
            println("ERROR - please provide HOST:PORT");
            return;
        }
        String[] parts = argv[0].split(":");
        String addr = parts[0];
        int port = Integer.parseInt(parts[1]);
        sendReceive(addr, port);
    }



    static class ClientConnection {
        private final DataInputStream dataInput;
        private final DataOutputStream dataOutput;

        public ClientConnection(Socket connectionSocket) {
            try {
                dataInput = new DataInputStream(connectionSocket.getInputStream());
                dataOutput = new DataOutputStream(connectionSocket.getOutputStream());

            } catch (IOException e) {
                throw new RuntimeException("FATAL = cannot create data streams", e);
            }
        }

        public void sendReceive() {
            try {
                sendRequest(1, dataOutput);
//                processResponse(dataInput);
            } catch (IOException e) {
                throw new RuntimeException("send/recieve error", e);
//                log("send/recieve error");
            }
        }


        public void processResponse(DataInputStream dataInput) throws IOException {
            int cmd = dataInput.readInt(); // skip command field

            int beefBeef = dataInput.readInt();
            if (beefBeef != BEEF_BEEF) {
                throw new IOException("Bad message no BeefBeef");
            }
            int nodesCount = dataInput.readInt();
            // FRANJI: discussion - create a new list in memory or update global list?
            nodeList.clear();
            for (int ni = 0; ni < nodesCount; ni++) {
                ShowChain.NodeInfo newInfo = ShowChain.NodeInfo.readFrom(dataInput);
                nodeList.add(newInfo);
            }
            int deadDead = dataInput.readInt();
            if (deadDead != DEAD_DEAD) {
                throw new IOException("Bad message no DeadDead");
            }
            int blockCount = dataInput.readInt();
            // FRANJI: discussion - create a new list in memory or update global list?
            ArrayList<Block> recievedBlocks = new ArrayList<>();
            blockList.clear();
            for (int bi = 0; bi < blockCount; bi++) {
                Block newBlock = Block.readFrom(dataInput);
                recievedBlocks.add(newBlock);
            }
            blockList = recievedBlocks;

            ChainStore.setBlocklist(recievedBlocks);
            log("INFO - Successfully received data");
//            printMessage();
        }


        private void sendRequest(int cmd, DataOutputStream dos) throws IOException {
            // send cmd and BEEF_BEEF
            dos.writeInt(cmd);
            dos.writeInt(BEEF_BEEF);

            // send nodes data
            sendNodes(dos);

            // write DEAD_DEAD
            dos.writeInt(DEAD_DEAD);

            // send blocks data
            sendBlocks(dos);
        }

        private void sendBlocks(DataOutputStream dos) throws IOException {
            // calculate the blockchain size
            int blockChainSize = blockList.size();
            dos.writeInt(blockChainSize);

            // send data of blocks
            for (Block block : blockList) {
                dos.write(block.data);
            }
        }

        private void sendNodes(DataOutputStream dos) throws IOException {
            // calculate number of nodes
            int activeNodesCount = nodeList.size();
            dos.writeInt(activeNodesCount);

            // send the data of all the nodes
            for (ShowChain.NodeInfo node : nodeList) {
                dos.writeByte(node.name.length());
                dos.write(node.name.getBytes());
                dos.writeByte(node.host.length());
                dos.write(node.host.getBytes());
                dos.writeShort(node.port);
                dos.writeInt(node.lastSeenTS);
            }
        }
    }
}