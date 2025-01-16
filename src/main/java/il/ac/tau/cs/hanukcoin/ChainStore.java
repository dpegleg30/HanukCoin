package il.ac.tau.cs.hanukcoin;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class ChainStore {
    public static ArrayList<Block> blocklist = new ArrayList<>();
    public static ArrayList<ShowChain.NodeInfo> nodelist = new ArrayList<>();
    public static String addr = "35.239.122.216";
    public static int port = 8080;

    public static void setBlocklist(ArrayList<Block> newBlocklist) {
        blocklist = newBlocklist;
    }

    public static void setNodelist(ArrayList<ShowChain.NodeInfo> newNodelist) {
        nodelist = newNodelist;
    }

    public static ArrayList<Block> getBlocklist() {
        return blocklist;
    }

    public static void addBlock(Block block) {
        blocklist.add(block);
    }

    public static ArrayList<ShowChain.NodeInfo> getNodelist() {
        return nodelist;
    }

    public static Block getLastBlock() {
        return blocklist.get(blocklist.size() - 1);
    }

    public static void writeDataToFile() throws IOException {
        String filePath = "C:\\Users\\itamar\\Desktop\\Shiurei bait\\HanukCoin1\\LastBlockData.txt";
        File file = new File(filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            byte[] thisData = getLastBlock().getData();
            for (int i = 0; i < 32; i++) {
                // Write each byte as a two-character hexadecimal string
                bw.write(String.format("%02X", thisData[i]));
                bw.newLine();
            }
        }
    }

    public static byte[] readData() throws IOException {
        String filePath = "C:\\Users\\itamar\\Desktop\\Shiurei bait\\HanukCoin1\\LastBlockData.txt";
        byte[] thisData = new byte[32];
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            for (int i = 0; i < 32; i++) {
                // Read each line as a hexadecimal string and parse it as a byte
                String line = br.readLine();
                if (line == null) {
                    throw new IOException("File has insufficient data");
                }
                thisData[i] = (byte) Integer.parseInt(line, 16);
            }
        }
        return thisData;
    }

}
