//package il.ac.tau.cs.hanukcoin;
//
//import java.lang.reflect.Array;
//import java.util.ArrayList;
//import java.util.List;
//
//public class Main {
//    public static List<Block> activeBlocks;
//    public static void main(String[] args) {
////        activeBlocks = AddToChain.main(args);
////        System.out.println(activeBlocks.size());
////        MineBlocks.main(args);
//    }
//
//    public static Block getLastBlock() {
//        return activeBlocks.get(activeBlocks.size() - 1);
//    }
//
//    public static boolean validate_chain(ArrayList<Block> chain) {
//        if (!Block.two_blocks_equal(chain.get(0), HanukCoinUtils.createBlock0forTestStage())) {
//            return false;
//            // Check if the first block in the chain is the same as the genesis block
//        }
//        for (int i = 1; i < chain.size() - 1; i++) {
//            if (chain.get(i).getSerialNumber() + 1 != chain.get(i + 1).getSerialNumber()){
//                return false;
//            }
//            if (chain.get(i).getWalletNumber() == chain.get(i + 1).getWalletNumber()){
//                return false;
//                // Ensure each serial number is 1 more than the previous one, and no one won
//                // 2 blocks in a row
//            }
//
//            if (chain.get(i).checkSignature() != Block.BlockError.OK) {
//                return false;
//                // Check if the sig is valid
//            }
//            if (!HanukCoinUtils.ArraysPartEquals(8, chain.get(i + 1).data, 8, chain.get(i).data, 24)){
//                return false;
//                // Check if the first 8 bits in the sig field is the same as
//                // The prevSig field in the next block
//            }
//        }
//
//        return true;
//    }
//}