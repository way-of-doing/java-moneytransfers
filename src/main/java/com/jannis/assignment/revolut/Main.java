package com.jannis.assignment.revolut;

import com.jannis.assignment.revolut.api.ApiServer;

public class Main {

    public static void main(String[] args)
    {
        System.out.println("Initializing...");

        try {
            if (ApiServer.start().get()) {
                System.out.println("Server is listening, press ENTER to exit.");
                System.in.read();
            }
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        } finally {
            ApiServer.stop();
        }
    }
}
