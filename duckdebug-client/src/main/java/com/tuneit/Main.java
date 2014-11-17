package com.tuneit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws UnknownHostException, IOException {

        Socket socket = new Socket(InetAddress.getByName("localhost"), 4242);

        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();

        while(true) {
            System.out.write(is.read());
        }

    }
}
