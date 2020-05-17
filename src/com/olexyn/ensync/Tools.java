package com.olexyn.ensync;

import com.olexyn.ensync.artifacts.SyncFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tools {

    private final Execute x;

    public Tools() {
        x = new Execute();
    }


    /**
     * Convert BufferedReader to String.
     *
     * @param br BufferedReader
     * @return String
     */
    public String brToString(BufferedReader br) {
        StringBuilder sb = new StringBuilder();
        Object[] br_array = br.lines().toArray();
        for (int i = 0; i < br_array.length; i++) {
            sb.append(br_array[i].toString() + "\n");
        }
        return sb.toString();
    }


    /**
     * Convert BufferedReader to List of Strings.
     *
     * @param br BufferedReader
     * @return List
     */
    public List<String> brToListString(BufferedReader br) {
        List<String> list = new ArrayList<>();
        Object[] br_array = br.lines().toArray();
        for (int i = 0; i < br_array.length; i++) {
            list.add(br_array[i].toString());
        }
        return list;
    }


    public List<String> fileToLines(File file) {
        String filePath = file.getPath();
        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }


    public Map<String, SyncFile> mapMinus(Map<String, SyncFile> fromA, Map<String, SyncFile> substractB) {

        Map<String, SyncFile> difference = new HashMap<>();
        for (Map.Entry<String, SyncFile> entry : fromA.entrySet()) {
            String key = entry.getKey();

            if (fromA.containsKey(key) && !substractB.containsKey(key)) {
                SyncFile file = fromA.get(key);
                difference.put(key, file);
            }

        }
        return difference;
    }


    public StringBuilder stringListToSb(List<String> list) {
        StringBuilder sb = new StringBuilder();

        for (String line : list) {
            sb.append(line + "\n");
        }
        return sb;
    }

    /**
     * Write sb to file at path .
     *
     * @param path <i>String</i>
     * @param sb   <i>StringBuilder</i>
     */
    public void writeSbToFile(String path, StringBuilder sb) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path)));
            bw.write(sb.toString());
            bw.close();
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
        File file = new File(path);
        File parent = new File(file.getParent());
        if (!parent.exists()) {

            x.execute(new String[]{"mkdir",
                                   "-p",
                                   parent.getPath()});
        }


        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path)));
            StringBuilder sb = stringListToSb(list);
            bw.write(sb.toString());
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String stateFilePath(String path) {
        return "/tmp/ensync/state" + path.replace("/", "-");
    }
}
