package il.ac.tau.cs.hanukcoin;

import com.sun.tools.javac.Main;

import java.util.ArrayList;

public class MiningStats {
    public static void printData(ArrayList<int[]> StatistixaData) {
        System.out.println(StatistixaData.size());
        System.out.println("const sampleData = [");
        int[] currData = new int[]{1, 2, 3};
        for (int i = 0; i < StatistixaData.size(); i++) {
            currData = StatistixaData.get(i);
            System.out.print("\t{name: 'Block " + currData[0] + "', time: "
            + currData[1] + ", thread: " + currData[2] + "}");
            if (i == StatistixaData.size() - 1) {
                System.out.println();
            }
            else {
                System.out.println(",");
            }
        }
        System.out.println("];");
    }
}