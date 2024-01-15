package com.example.brainwave;

public class LocalDataSet {
    static final String [] statues = {
        "Buồn ngủ", "Tỉnh táo"
    };

    static int label2index(String label) {
        int index;
        switch (label) {
            case "Buồn ngủ":
                index = 0;
                break;
            case "Tỉnh táo":
                index = 1;
                break;
            default:
                index = -1;
        }
        return index;
    }
}
