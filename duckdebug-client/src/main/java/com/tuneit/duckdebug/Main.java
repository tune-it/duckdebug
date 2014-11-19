package com.tuneit.duckdebug;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Main {

    public static void main(String[] args) {

        String host = "";
        int port = 0;

        Socket socket;
        final InputStream is;
        final OutputStream os;

        Options options = new Options();
        options.addOption("h", "host", true, "server host");
        options.addOption("p", "port", true, "server port");

        boolean failed = false;
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine line = parser.parse( options, args );
            host = line.getOptionValue("h");
            port = Integer.parseInt(line.getOptionValue("p"));
        } catch (ParseException exp) {
            System.err.println( exp.getMessage() );
            failed = true;
        } catch (NumberFormatException ex) {
            System.err.println("Incorrect port");
            failed = true;
        } finally {
            if(failed) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ddc", options );
                return;
            }
        }


        try {
            socket = new Socket(InetAddress.getByName(host), port);
            is = socket.getInputStream();
            os = socket.getOutputStream();
        } catch (IOException ex) {
            System.err.println(ex.getLocalizedMessage());
            return;
        }

        Thread recive = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int c;
                    while (true) {
                        c = is.read();
                        if (c == -1) break;

                        System.out.write(c);
                        System.out.flush();
                    }
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        Thread send = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int c;
                    while (true) {
                        c = System.in.read();
                        if (c == -1) break;

                        os.write(c);
                        os.flush();
                    }
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        recive.start();
        send.start();

        try {
            recive.join();
            send.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
