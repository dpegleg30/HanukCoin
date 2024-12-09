//package il.ac.tau.cs.hanukcoin;
//
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//
//public class MineBlocks {
//    public static final int BEEF_BEEF = 0xbeefBeef;
//    public static final int DEAD_DEAD = 0xdeadDead;
//    private static List<NodeInfo> activeNodes = new ArrayList<>();
//    private static List<Block> activeBlocks = new ArrayList<>();
//
//
//    public static void log(String fmt, Object... args) {
//        println(fmt, args);
//    }
//
//    public static void println(String fmt, Object... args) {
//        System.out.format(fmt + "\n", args);
//    }
//
//    public static void sendReceive(String host, int port) {
//        try {
//            log("INFO - Sending request message to %s:%d", host, port);
//            Socket soc = new Socket(host, port);
//            ClientConnection connection = new ClientConnection(soc);
//            connection.sendReceive();
//        } catch (IOException e) {
//            log("WARN - open socket exception connecting to %s:%d: %s", host, port, e.toString());
//        }
//    }
//
//    public static void main(String[] argv) {
//        if (argv.length != 1 || !argv[0].contains(":")) {
//            println("ERROR - please provide HOST:PORT");
//            return;
//        }
//        String[] parts = argv[0].split(":");
//        String addr = parts[0];
//        int port = Integer.parseInt(parts[1]);
//        sendReceive(addr, port);
//        sendReceive(addr, port);
//    }
//
//    static class NodeInfo {
//        // FRANJI: Discussion - public members - pro/cons. What is POJO
//
//        public String name;
//        public String host;
//        public int port;
//        public int lastSeenTS;
//        // TODO(students): add more fields you may need such as number of connection attempts failed
//        //  last time connection was attempted, if this node is new ot alive etc.
//
//        public static String readLenStr(DataInputStream dis) throws IOException {
//            byte strLen = dis.readByte();
//            byte[] strBytes = new byte[strLen];
//            dis.readFully(strBytes);
//            return new String(strBytes, StandardCharsets.UTF_8);
//        }
//
//        public static NodeInfo readFrom(DataInputStream dis) throws IOException {
//            NodeInfo n = new NodeInfo();
//            n.name = readLenStr(dis);
//            n.host = readLenStr(dis);
//            n.port = dis.readShort();
//            n.lastSeenTS = dis.readInt();
//            // TODO(students): update extra fields
//            return n;
//        }
//    }
//
//    static class ClientConnection {
//        private final DataInputStream dataInput;
//        private final DataOutputStream dataOutput;
//
//        public ClientConnection(Socket connectionSocket) {
//            try {
//                dataInput = new DataInputStream(connectionSocket.getInputStream());
//                dataOutput = new DataOutputStream(connectionSocket.getOutputStream());
//
//            } catch (IOException e) {
//                throw new RuntimeException("FATAL = cannot create data streams", e);
//            }
//        }
//
//        public void sendReceive() {
//            try {
//                Block lastBlock = Main.getLastBlock();
//                Block minedBlock = null;
//                int myWallet = HanukCoinUtils.walletCode("Ztest");
//                int indexNow = 0;
//                while (minedBlock == null) {
//                    indexNow++;
//                    System.out.println(indexNow);
//                    minedBlock = HanukCoinUtils.mineCoinAttempt(myWallet, lastBlock, 100000000);
//                }
//                addBlocks(1, dataOutput, lastBlock);
//                parseMessage(dataInput);
//                System.out.println(activeNodes);
//                for (coolBlock : activeBlocks) {}
//                    System.out.println(activeBlocks[i].get);
//            } catch (IOException e) {
//                throw new RuntimeException("send/recieve error", e);
//            }
//        }
//
//
//        public void parseMessage(DataInputStream dataInput) throws IOException {
//            int cmd = dataInput.readInt(); // skip command field
//
//            int beefBeef = dataInput.readInt();
//            if (beefBeef != BEEF_BEEF) {
//                throw new IOException("Bad message no BeefBeef");
//            }
//            int nodesCount = dataInput.readInt();
//            // FRANJI: discussion - create a new list in memory or update global list?
//            activeNodes.clear();
//            for (int ni = 0; ni < nodesCount; ni++) {
//                NodeInfo newInfo = NodeInfo.readFrom(dataInput);
//                activeNodes.add(newInfo);
//            }
//            int deadDead = dataInput.readInt();
//            if (deadDead != DEAD_DEAD) {
//                throw new IOException("Bad message no DeadDead");
//            }
//            int blockCount = dataInput.readInt();
//            // FRANJI: discussion - create a new list in memory or update global list?
//            activeBlocks.clear();
//            for (int bi = 0; bi < blockCount; bi++) {
//                Block newBlock = Block.readFrom(dataInput);
//                activeBlocks.add(newBlock);
//            }
//            System.out.println("Parsed successfully");
////            printMessage();
//        }
//
//        private void printMessage(List<NodeInfo> receivedNodes, List<Block> receivedBlocks) {
//            println("==== Nodes ====");
//            for (NodeInfo ni : receivedNodes) {
//                println("%20s\t%s:%s\t%d", ni.name, ni.host, ni.port, ni.lastSeenTS);
//            }
//            println("==== Blocks ====");
//            for (Block b : receivedBlocks) {
//                println("%5d\t0x%08x\t%s", b.getSerialNumber(), b.getWalletNumber(), b.binDump().replace("\n", "  "));
//            }
//        }
//
//        private void sendRequest(int cmd, DataOutputStream dos) throws IOException {
//            dos.writeInt(cmd);
//            dos.writeInt(BEEF_BEEF);
//            int activeNodes = 0;
//            // TODO(students): calculate number of active (not new) nodes
//            dos.writeInt(activeNodes);
//            // TODO(students): sendRequest data of active (not new) nodes
//            dos.writeInt(DEAD_DEAD);
//            int blockChain_size = 0;
//            dos.writeInt(blockChain_size);
//            // TODO(students): sendRequest data of blocks
//        }
//
//        private void addBlocks(int cmd, DataOutputStream dos, Block block2) throws IOException {
//            dos.writeInt(cmd);
//            dos.writeInt(BEEF_BEEF);
//            dos.writeInt(activeNodes.size());
//            for (NodeInfo node : activeNodes) {
//                dos.writeByte(node.name.length());
//                dos.write(node.name.getBytes());
//                dos.writeByte(node.host.length());
//                dos.write(node.host.getBytes());
//                dos.writeShort(node.port);
//                dos.writeInt((int) (System.currentTimeMillis() / 1000));
//            }
//            dos.writeInt(DEAD_DEAD);
//            dos.writeInt(activeBlocks.size() + 1);
//            activeBlocks.add(block2);
//            for (Block block : activeBlocks) {
//                dos.write(block.data);
////                dos.writeInt(block.getSerialNumber());
////                dos.writeInt(block.getSerialNumber());
////                dos.writeLong(block.getPrevSig());
////                dos.writeLong(block.getPuzzle());
////                dos.writeLong(block.getStartSig());
////                dos.writeInt(block.getFinishSig());
//            }
//        }
//    }
//}