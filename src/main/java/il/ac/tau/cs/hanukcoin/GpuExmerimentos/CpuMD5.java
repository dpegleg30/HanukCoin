package il.ac.tau.cs.hanukcoin.GpuExmerimentos;

import il.ac.tau.cs.hanukcoin.ChainStore;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CpuMD5 {
    public static final byte MyByte = (byte) 0x80;

    public static void main(String[] args) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");  // may cause NoSuchAlgorithmException
            md.update(ChainStore.readData(), 0, 24);
            //md.update(new byte[]{0x01, 0x02, 0x03}, 0, 3);
            binDump2(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Internal error - missing MD5");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void binDump2(byte[] whatIsDisSheisse) {
        for (int i = 0; i < whatIsDisSheisse.length; i++) {
            System.out.print(String.format("%02x", whatIsDisSheisse[i]) + " ");
        }
    }

    public static byte[] getBlock24bytes(String block) {
        byte[] blockBytes = new byte[24];
        String blockReduced = block.replace(" ", "").replace("\n", "").replace("\r", "");
        for (int i = 0; i < 48; i += 2) {
            blockBytes[i / 2] = (byte) Integer.parseInt(blockReduced.substring(i, i + 2), 16);
        }
        return blockBytes;
    }
}
