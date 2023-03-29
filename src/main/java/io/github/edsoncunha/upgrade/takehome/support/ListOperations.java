package io.github.edsoncunha.upgrade.takehome.support;

import java.util.ArrayList;

public class ListOperations {
    public static <T> T last(ArrayList<T> list) {
        return list.get(list.size() - 1);
    }
}
