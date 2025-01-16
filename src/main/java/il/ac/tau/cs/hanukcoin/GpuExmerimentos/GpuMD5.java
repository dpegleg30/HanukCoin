package il.ac.tau.cs.hanukcoin.GpuExmerimentos;

import il.ac.tau.cs.hanukcoin.ChainStore;
import org.jocl.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

import static org.jocl.CL.*;

public class GpuMD5 {
    private static final int BLOCK_SIZE = 64;
    private static final int INPUT_SIZE = 24;

    public static byte[] computeMD5(byte[] input) throws IOException {
        if (input.length != INPUT_SIZE) {
            throw new IllegalArgumentException("Input must be exactly 24 bytes");
        }

        CL.setExceptionsEnabled(true);

        // Initialize OpenCL
        cl_platform_id[] platforms = new cl_platform_id[1];
        clGetPlatformIDs(1, platforms, null);

        cl_device_id[] devices = new cl_device_id[1];
        clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_GPU, 1, devices, null);

        cl_context context = clCreateContext(null, 1, devices, null, null, null);
        cl_command_queue queue = clCreateCommandQueueWithProperties(context, devices[0], null, null);

        // Load and build kernel
        String kernelSource = new String(Files.readAllBytes(Paths.get("src/main/java/il/ac/tau/cs/hanukcoin/md5Kernel.cl")));
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{kernelSource}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel kernel = clCreateKernel(program, "mainFunc", null);

        // Create output buffer for hash
        cl_mem hashesBuffer = clCreateBuffer(context, CL_MEM_WRITE_ONLY, 16, null, null);
        cl_mem inputBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY, INPUT_SIZE, null, null);

        // Write input data
        clEnqueueWriteBuffer(queue, inputBuffer, CL_TRUE, 0, INPUT_SIZE, Pointer.to(input), 0, null, null);

        // Set kernel arguments
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(inputBuffer));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(hashesBuffer));

        // Execute kernel
        clEnqueueNDRangeKernel(queue, kernel, 1, null, new long[]{1}, null, 0, null, null);
        clFinish(queue);

        // Read result
        byte[] hash = new byte[16];
        clEnqueueReadBuffer(queue, hashesBuffer, CL_TRUE, 0, 16, Pointer.to(hash), 0, null, null);

        // Cleanup
        clReleaseMemObject(inputBuffer);
        clReleaseMemObject(hashesBuffer);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(queue);
        clReleaseContext(context);

        return hash;
    }

    public static void main(String[] args) throws IOException {
        byte[] input = new byte[24];
        System.arraycopy(CpuMD5.getBlock24bytes(
                "00 00 03 80  56 75 47 4F  \n" +
                        "FC F8 E5 3A  F3 FA 5A F3  \n" +
                        "FF FF FF FF  F9 18 C1 52  \n" +
                        "A7 FC E4 CB  19 6F D7 49  \n" +
                        "BA BA 6A 0F"), 0, input, 0, 24);

        byte[] hash = computeMD5(input);
        CpuMD5.binDump2(hash);
        System.out.println();
    }
}