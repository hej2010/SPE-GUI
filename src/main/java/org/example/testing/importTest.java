package org.example.testing;

import component.operator.in1.map.MapFunction;
import query.Query;

public class importTest {


    public static void main(String[] args) {
        Query q = new Query();
        q.addMapOperator("fd", new MapFunction<String, Integer>() {
            @Override
            public Integer apply(String s) {
                s = "dfdf";
                return Integer.valueOf(s);
            }
        });
    }
}
