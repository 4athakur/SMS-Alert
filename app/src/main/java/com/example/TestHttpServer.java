package com.example;

public class TestHttpServer {
    public static void test() {
        try {
            Class.forName("com.sun.net.httpserver.HttpServer");
            System.out.println("Class found");
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
        }
    }
}
