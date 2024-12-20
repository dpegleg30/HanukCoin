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
import java.util.concurrent.atomic.AtomicReference;

public class AddToChain {
    public static final int BEEF_BEEF = 0xbeefBeef;
    public static final int DEAD_DEAD = 0xdeadDead;
    private static final List<NodeInfo> nodeList = new ArrayList<>();
    private static final List<Block> blockList = new ArrayList<>();


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

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].contains(":")) {
            println("ERROR - please provide HOST:PORT");
            return;
        }
        String[] parts = args[0].split(":");
        String addr = parts[0];
        int port = Integer.parseInt(parts[1]);

        // send an "empty" message in order to get the nodes and blocks in the server
        sendReceive(addr, port);

        if (blockList.get(blockList.size() - 1).getWalletNumber() == 0xc207e787) {
            return;
        }
        // Get number of available processors
        int numThreads = Runtime.getRuntime().availableProcessors() - 1;
        System.out.println("Starting mining with " + numThreads + " threads. Target block:" + blockList.size());

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicReference<Block> minedBlock = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);


        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    int start_time = (int) (System.currentTimeMillis() / 1000);
                    while (minedBlock.get() == null && !Thread.currentThread().isInterrupted()) {
                        Block attempt = HanukCoinUtils.mineCoinAttempt(
                                HanukCoinUtils.walletCode(CaptainAmeriminer.wallet),
                                blockList.get(blockList.size() - 1),
                                100000000
                        );

                        if (attempt != null) {
                            int end_time = (int) (System.currentTimeMillis() / 1000);
                            if (minedBlock.compareAndSet(null, attempt)) {
                                System.out.println("SUCCESS! Thread " + threadId + " successfully mined block no." + minedBlock.get().getSerialNumber() + " within " + (end_time-start_time) + " seconds");
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
                return;
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
    }

    static class NodeInfo {
        // FRANJI: Discussion - public members - pro/cons. What is POJO

        public String name;
        public String host;
        public int port;
        public int lastSeenTS;
        // TODO(students): add more fields you may need such as number of connection attempts failed
        //  last time connection was attempted, if this node is new ot alive etc.

        public static String readLenStr(DataInputStream dis) throws IOException {
            byte strLen = dis.readByte();
            byte[] strBytes = new byte[strLen];
            dis.readFully(strBytes);
            return new String(strBytes, StandardCharsets.UTF_8);
        }

        public static NodeInfo readFrom(DataInputStream dis) throws IOException {
            NodeInfo n = new NodeInfo();
            n.name = readLenStr(dis);
            n.host = readLenStr(dis);
            n.port = dis.readShort();
            n.lastSeenTS = dis.readInt();
            // TODO(students): update extra fields
            return n;
        }
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
                processResponse(dataInput);
            } catch (IOException e) {
                throw new RuntimeException("send/recieve error", e);
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
                NodeInfo newInfo = NodeInfo.readFrom(dataInput);
                nodeList.add(newInfo);
            }
            int deadDead = dataInput.readInt();
            if (deadDead != DEAD_DEAD) {
                throw new IOException("Bad message no DeadDead");
            }
            int blockCount = dataInput.readInt();
            // FRANJI: discussion - create a new list in memory or update global list?
            blockList.clear();
            for (int bi = 0; bi < blockCount; bi++) {
                Block newBlock = Block.readFrom(dataInput);
                blockList.add(newBlock);
            }
            log("INFO - Successfully received data");
//            printMessage();
        }

        private void printMessage(List<NodeInfo> receivedNodes, List<Block> receivedBlocks) {
            println("==== Nodes ====");
            for (NodeInfo ni : receivedNodes) {
                println("%20s\t%s:%s\t%d", ni.name, ni.host, ni.port, ni.lastSeenTS);
            }
            println("==== Blocks ====");
            for (Block b : receivedBlocks) {
                println("%5d\t0x%08x\t%s", b.getSerialNumber(), b.getWalletNumber(), b.binDump().replace("\n", "  "));
            }
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
            for (NodeInfo node : nodeList) {
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
