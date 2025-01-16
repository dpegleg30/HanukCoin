package il.ac.tau.cs.hanukcoin;

import il.ac.tau.cs.hanukcoin.Block;
import il.ac.tau.cs.hanukcoin.GpuExmerimentos.CpuMD5;
import il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import org.jocl.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

import static org.jocl.CL.*;

public class RunningGpuMiner {
    private static final int BLOCK_SIZE = 64;
    private static final int INPUT_SIZE = 24;
    private static final int WORK_GROUP_SIZE = 256;
    private static final int NUM_WORK_GROUPS = 1024;
    private static final int TOTAL_THREADS = WORK_GROUP_SIZE * NUM_WORK_GROUPS;
    public static long startVal = 0;
    public static String[] Colors = {"\u001B[31;1m", "\u001B[32;1m", "\u001B[36;1m"};
    public static String A_RESET = "\u001B[0m";

    private cl_context context;
    private cl_command_queue queue;
    private cl_kernel kernel;
    private cl_mem inputBuffer;
    private cl_mem hashesBuffer;
    private cl_mem resultBuffer;

    public RunningGpuMiner() throws IOException {
        initializeOpenCL();
    }

    private void initializeOpenCL() throws IOException {
        // Enable exceptions
        CL.setExceptionsEnabled(true);

        // Get platform and device
        cl_platform_id[] platforms = new cl_platform_id[1];
        clGetPlatformIDs(1, platforms, null);

        cl_device_id[] devices = new cl_device_id[1];
        clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_GPU, 1, devices, null);

        // Create context and command queue
        context = clCreateContext(null, 1, devices, null, null, null);
        queue = clCreateCommandQueueWithProperties(context, devices[0], null, null);

        // Load and build kernel
        String kernelSource = new String(Files.readAllBytes(Paths.get("src/main/java/il/ac/tau/cs/hanukcoin/MyLoserKernel.cl")));
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{kernelSource}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        kernel = clCreateKernel(program, "mainFunc", null);

        // Create buffers
        inputBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY, INPUT_SIZE * TOTAL_THREADS, null, null);
        hashesBuffer = clCreateBuffer(context, CL_MEM_WRITE_ONLY, 16 * TOTAL_THREADS, null, null);
        resultBuffer = clCreateBuffer(context, CL_MEM_WRITE_ONLY, Sizeof.cl_long, null, null);    }

    public Block mineCoin(int myWalletNum, Block prevBlock, int requiredZeros) {
        int newSerialNum = prevBlock.getSerialNumber() + 1;
        System.out.println("Attempting mine as: " + Colors[2] + CaptainAmeriminer.wallet + A_RESET);

        // Don't mine if previous block was from same wallet
        if (prevBlock.getWalletNumber() == myWalletNum) {
            return null;
        }

        // Create new block template
        byte[] prevSig = new byte[8];
        System.arraycopy(prevBlock.getBytes(), 24, prevSig, 0, 8);
        Block template = Block.createNoSig(newSerialNum, myWalletNum, prevSig);

        // Prepare input data for all threads
        byte[] inputData = new byte[INPUT_SIZE * TOTAL_THREADS];
        for (int i = 0; i < TOTAL_THREADS; i++) {
            System.arraycopy(template.getBytes(), 0, inputData, i * INPUT_SIZE, 16);
            // Last 8 bytes will be set as puzzle value by kernel
        }

        try {
            // Write input data to GPU
            clEnqueueWriteBuffer(queue, inputBuffer, CL_TRUE, 0, INPUT_SIZE * TOTAL_THREADS,
                    Pointer.to(inputData), 0, null, null);

            // Set kernel arguments
            int[] zeros = new int[]{requiredZeros};
            long[] startValue = new long[]{startVal};
            clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(inputBuffer));
            clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(hashesBuffer));
            clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(zeros));
            clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(resultBuffer));
            clSetKernelArg(kernel, 4, Sizeof.cl_long, Pointer.to(startValue));

            // Execute kernel
            clEnqueueNDRangeKernel(queue, kernel, 1, null,
                    new long[]{TOTAL_THREADS}, new long[]{WORK_GROUP_SIZE},
                    0, null, null);
            clFinish(queue);
            // Check if we found a solution
            long[] result = new long[1];
            clEnqueueReadBuffer(queue, resultBuffer, CL_TRUE, 0, Sizeof.cl_long,
                    Pointer.to(result), 0, null, null);

            if (result[0] != 0) {
                //System.out.println("Coin mine " + result[0]);
                // Get the successful hash and puzzle value
                byte[] hashes = new byte[16 * TOTAL_THREADS];
                clEnqueueReadBuffer(queue, hashesBuffer, CL_TRUE, 0, 16 * TOTAL_THREADS,
                        Pointer.to(hashes), 0, null, null);

                // Create final block with found solution
                Block please = Block.createNoSig(newSerialNum, myWalletNum, prevSig);
                for (int i = 0; i < 8; i++) {
                    please.data[16 + i] = (byte)((result[0] >>> (i * 8)) & 0xFF);
                    //System.out.printf("%02x", (byte)((result[0] >>> (i * 8)) & 0xFF));
                }
                System.out.println();
                please.setSignaturePart(please.calcSignature());
                //CpuMD5.binDump2(please.data);
                System.out.println();
                //CpuMD5.binDump2(Block.calcSignatureStatic(please.data));

                if (please.checkSignature() == Block.BlockError.OK) {
                    System.out.println(Colors[1] + "SUCCESS!" + A_RESET);
                    return please;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void cleanup() {
        clReleaseMemObject(inputBuffer);
        clReleaseMemObject(hashesBuffer);
        clReleaseMemObject(resultBuffer);
        clReleaseKernel(kernel);
        clReleaseCommandQueue(queue);
        clReleaseContext(context);
    }

    public static void main(String[] args) throws IOException {
        RunningGpuMiner miner = new RunningGpuMiner();
        try {
            // Example usage
            Block genesis = HanukCoinUtils.createBlock0forTestStage();
            int walletNum = HanukCoinUtils.walletCode("TEST_MINER");

            Block minedBlock = miner.mineCoin(walletNum, genesis,
                    HanukCoinUtils.numberOfZerosForPuzzle(genesis.getSerialNumber() + 1));

            if (minedBlock != null) {
                System.out.println("Successfully mined block:");
                System.out.println(minedBlock.binDump());
            } else {
                System.out.println("Mining failed");
            }
        } finally {
            miner.cleanup();
        }
    }
}