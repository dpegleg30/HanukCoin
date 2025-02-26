package il.ac.tau.cs.hanukcoin;

/*
 * Block is 36 byte/288bit long.
 * record/block format:
 * 32-bit serial number
 * 32 bit wallet number
 * 64 bit prev_sig[:8]highest bits  (first half ) of previous block's signature (including all the block)
 * 64 bit puzzle answer
 * 96 bit sig[:12] - md5 - the first 12 bytes of md5 of above fields. need to make last N bits zero
 */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import il.ac.tau.cs.hanukcoin.GpuExmerimentos.CpuMD5;
import org.jocl.*;

/**
 * Class that represents one block in the block chane.
 * The clock hods a 36-bytes array and all operations are performed directly on this array.
 */
public class Block {
    public static final int BLOCK_SZ = 36;
    protected byte[] data;

    /**
     * Creates a block without a signature or puzzle fields.
     * @param serialNumber
     * @param walletNumber
     * @param prevSig8
     * @return new block
     */
    public static Block createNoSig(int serialNumber, int walletNumber, byte[] prevSig8) {
        Block b = new Block();
        b.data = new byte[BLOCK_SZ];
        HanukCoinUtils.intIntoBytes(b.data, 0, serialNumber);
        HanukCoinUtils.intIntoBytes(b.data, 4, walletNumber);
        System.arraycopy(prevSig8, 0, b.data, 8, 8);
        return b;
    }

    public static Block create(int serialNumber, int walletNumber, byte[] prevSig8, byte[] puzzle8, byte[] sig12) {
        Block b = createNoSig(serialNumber, walletNumber, prevSig8);
        System.arraycopy(sig12, 0, b.data, 24, 12);
        System.arraycopy(puzzle8, 0, b.data, 16, 8);
        return b;
    }

    public static Block readFrom(DataInputStream dis) throws IOException {
        Block b = new Block();
        b.data = new byte[BLOCK_SZ];
        dis.readFully(b.data);
        return b;
    }

    public int getSerialNumber() {
        return HanukCoinUtils.intFromBytes(data, 0);
    }

    public int getWalletNumber() {
        return HanukCoinUtils.intFromBytes(data, 4);
    }

    public long getPrevSig() {
        return HanukCoinUtils.longFromBytes(data, 8);
    }

    public long getPuzzle() {
        return HanukCoinUtils.longFromBytes(data, 16);
    }

    public long getStartSig() {
        return HanukCoinUtils.longFromBytes(data, 24);
    }

    public int getFinishSig() {
        return HanukCoinUtils.intFromBytes(data, 32);
    }


    public void writeTo(DataOutputStream dos) throws IOException {
        dos.write(getBytes(), 0, BLOCK_SZ);
    }

    public boolean equals(Block other) {
        return Arrays.equals(other.getBytes(), this.getBytes());
    }

    /**
     * put 8 bytes dat into puzzle field
     * @param longPuzzle - 64 bit puzzle
     */
    public void setLongPuzzle(long longPuzzle) {
        // Treat it as 2 32bit integers
        HanukCoinUtils.intIntoBytes(data, 16, (int) (longPuzzle >> 32));
        HanukCoinUtils.intIntoBytes(data, 20, (int) (longPuzzle));
    }

    public void setLongPuzzleGpu(long longPuzzle) {
        // Write 64-bit value as 8 separate bytes in little-endian order
        for (int i = 0; i < 8; i++) {
            data[16 + i] = (byte)((longPuzzle >> (i * 8)) & 0xFF);
        }
    }

    /**
     * compare this.puzzle - other.puzzle
     * @param other
     * @return 1 if this puzzle bigger, 0 if equal, -1 if this smaller
     */
    public int comparePuzzle(Block other) {
        return HanukCoinUtils.ArraysPartCompare(8, this.getBytes(), 16, other.getBytes(), 16);
    }

    /**
     * given a block signature - take first 12 bytes of it and put into the signature field of this block
     * @param sig
     */
    public void setSignaturePart(byte[] sig) {
        System.arraycopy(sig, 0, data, 24, 12);
    }

    /**
     * calc block signature based on all fields besides signature itself.
     * @return 16 byte MD5 signature
     */
//    public byte[] calcSignature() {
//        try {
//            MessageDigest md = MessageDigest.getInstance("MD5");  // may cause NoSuchAlgorithmException
//            md.update(data, 0, 24);
//            return md.digest();
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException("Internal error - missing MD5");
//        }
//    }

    public byte[] calcSignature() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");  // may cause NoSuchAlgorithmException
            md.update(data, 0, 24);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Internal error - missing MD5");
        }
    }

    public static byte[] calcSignatureStatic(byte[] puzzle) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");  // may cause NoSuchAlgorithmException
            md.update(puzzle, 0, 24);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Internal error - missing MD5");
        }
    }

    /**
     * getter for internal bytes of block
     */
    public byte[] getBytes() {
        return data;
    }

    /**
     * calc signature for the block and see if in has the required number of zeros and matches signature written to the block
     * @return BlockError: SIG_NO_ZEROS or SIG_BAD or OK
     */
    public BlockError checkSignature() {
        byte[] sig = calcSignature();
        int serialNum = getSerialNumber();
        int nZeros = HanukCoinUtils.numberOfZerosForPuzzle(serialNum);
        if (!HanukCoinUtils.checkSignatureZeros(sig, nZeros)) {
            return BlockError.SIG_NO_ZEROS;
        }
        if (!HanukCoinUtils.ArraysPartEquals(12, data, 24, sig, 0)) {
            return BlockError.SIG_BAD;
        }
        return BlockError.OK;
    }

    /**
     * given a block previous to this one - check if this one is valid.
     * @param prevBlock
     * @return BlockError
     */
    public BlockError checkValidNext(Block prevBlock) {
        if (getSerialNumber() != prevBlock.getSerialNumber() + 1) {
            return BlockError.BAD_SERIAL_NO;  // bad serial number - should be prev + 1
        }
        if (getWalletNumber() == prevBlock.getWalletNumber()) {
            return BlockError.SAME_WALLET_PREV;  // don't allow two consequent blocks with same wallet
        }
        if (!HanukCoinUtils.ArraysPartEquals(8, data, 8, prevBlock.data, 24)) {
            return BlockError.NO_PREV_SIG;  // check prevSig field is indeed siganute of prev block
        }
        return checkSignature();
    }

    /**
     * String with HEX dump of block for debugging.
     * @return string - hex dump
     */
    public String binDump() {
        StringBuilder dump = new StringBuilder();
        for (int i = 0; i < BLOCK_SZ; i++) {
            if ((i % 4) == 0) {
                dump.append(" ");
            }
            if ((i % 8) == 0) {
                dump.append("\n");
            }
            dump.append(String.format("%02X ", data[i]));
        }
        return dump.toString();
    }

    public Block clone() {
        Block b = new Block();
        b.data = Arrays.copyOf(this.getBytes(), BLOCK_SZ);
        return b;
    }

    public static boolean two_blocks_equal(Block block, Block block2) {
        ArrayList<Object> coolblock = HanukCoinUtils.MakeBlockReadableToHumanCreatures(block2);
        ArrayList<Object> otherblock = HanukCoinUtils.MakeBlockReadableToHumanCreatures(block);
        for (int i = 0; i < 5; i++) {
            if (!coolblock.get(i).equals(otherblock.get(i))) {
                return false;
            }
        }
        return true;
    }

    private int serNum;      // 4 bytes
    private int walletNum;   // 4 bytes
    private long prevSig;    // 8 bytes
    private long puzzle;     // 8 bytes
    private byte[] signature; // 12 bytes

    public Block Block2(int serNum, int walletNum, long prevSig, long puzzle, byte[] signature) {
        this.serNum = serNum;
        this.walletNum = walletNum;
        this.prevSig = prevSig;
        this.puzzle = puzzle;
        this.signature = signature;
        return this;
    }

    public byte[] getSignature() {
        byte[] sig = new byte[12];
        System.arraycopy(this.data, 16, sig, 0, 8);
        return sig;
    }
    public int getSerNum() { return serNum; }
    public int getWalletNum() { return walletNum; }

    public byte[] getData() {
        return data;
    }

    public enum BlockError {OK, BAD_SERIAL_NO, SAME_WALLET_PREV, NO_PREV_SIG, SIG_NO_ZEROS, SIG_BAD}

    public static void main(String[] args) {
        System.out.println(HanukCoinUtils.numberOfZerosForPuzzle(1080));
        byte[] bytes = CpuMD5.getBlock24bytes("00 00 04 44 56 75 47 4f 43 82 91 24 2e b6 6b b5 " +
                "43 42 74 58 01 00 00 00 b9 94 37 43 5f 15 da 49 33 62 cf e9 ");

        CpuMD5.binDump2(bytes);
        System.out.println();
        CpuMD5.binDump2(calcSignatureStatic(bytes));
        System.out.println();
        System.out.println(HanukCoinUtils.checkSignatureZeros(calcSignatureStatic(bytes), HanukCoinUtils.numberOfZerosForPuzzle(796)));
    }
}