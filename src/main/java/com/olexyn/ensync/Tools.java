package com.olexyn.ensync;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tools {

    public static BufferedReader reader(FileChannel fc) {
        return new BufferedReader(Channels.newReader(fc, StandardCharsets.UTF_8));
    }

    public static BufferedWriter writer(FileChannel fc) {
        return new BufferedWriter(Channels.newWriter(fc, StandardCharsets.UTF_8));
    }


    public static List<String> fileToLines(FileChannel fc) {
        List<String> lines = new ArrayList<>();
        try (var br = reader(fc)) {
            String line;
            while((line=br.readLine())!=null)
            {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static String fileToString(FileChannel fc){
        var lineList = fileToLines(fc);
        StringBuilder sb = new StringBuilder();
        for (String line : lineList){
            sb.append(line).append("\n");
        }
        return sb.toString();
    }


    public static Set<String> setMinus(Set<String> fromA, Set<String> subtractB) {
        Set<String> difference = new HashSet<>();
        for (var key : fromA) {
            if (fromA.contains(key) && !subtractB.contains(key)) {
                difference.add(key);
            }
        }
        return difference;
    }


    public StringBuilder stringListToSb(List<String> list) {
        var sb = new StringBuilder();
        for (String line : list) {
            sb.append(line).append("\n");
        }
        return sb;
    }

    /**
     * Write sb to file at path .
     *
     * @param path <i>String</i>
     * @param sb   <i>StringBuilder</i>
     */
    public void writeSbToPath(String path, StringBuilder sb) {
        writeSbToFile(new File(path), sb);
    }

    public void writeSbToFile(File file, StringBuilder sb) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
            bw.write(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Write List of String to file at path .
     *
     * @param path <i>String</i>
     * @param list <i>StringBuilder</i>
     */
    public void writeStringListToFile(String path, List<String> list) {
        try (var bw = new BufferedWriter(new FileWriter(path))) {
            var sb = stringListToSb(list);
            bw.write(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
