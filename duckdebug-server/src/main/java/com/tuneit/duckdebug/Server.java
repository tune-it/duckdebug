package com.tuneit.duckdebug;

import groovy.lang.Binding;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by tegoo on 11/14/14.
 */
public class Server {

    private int port;
    private ServerSocket serverSocket;

    private int poolSize;
    private ExecutorService workers;

    public Server(int port, int poolSize) {
        this.port = port;
        this.poolSize = poolSize;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        workers = Executors.newFixedThreadPool(poolSize);
        workers.execute(new Listener());
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException ex) { }
        workers.shutdownNow();
    }

    public void waitStop(long timeout, TimeUnit timeUnit) {
        try {
            workers.awaitTermination(timeout, timeUnit);
        } catch (InterruptedException ex) {}
    }

    private class Listener implements Runnable {

        @Override
        public void run() {
            while(true) {
                try {
                    Server.this.workers.execute(new Worker(
                            Server.this.serverSocket.accept()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    private class Worker implements Runnable {
        private final Socket socket;

        public Worker(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            OutputStream out;
            InputStream in;

            try {
                this.socket.setSoTimeout(60 * 60 * 1000);
                out = socket.getOutputStream();
                in = socket.getInputStream();
                out.write(duck.getBytes());
                out.flush();
            } catch(IOException ex) {
                tryClose();
                return;
            }

            Binding binding = new Binding();
            Groovysh shell = new Groovysh(binding, new IO(in, out, out));
            shell.run("");

            tryClose();
        }

        public void tryClose() {
            try {
                this.socket.close();
            } catch(IOException ex) { }
        }
    }

    private static final String duck =
            "       ..---..\n" +
                    "     .'  _    `.\n" +
                    " __..'  (o)    :\n" +
                    "`..__          ;\n" +
                    "     `.       /\n" +
                    "       ;      `..---...___\n" +
                    "     .'                   `~-. .-')\n" +
                    "    .                         ' _.'\n" +
                    "   :     DUCK                  :\n" +
                    "   \\         DEBUG             '\n" +
                    "    +                         J\n" +
                    "     `._                   _.'\n" +
                    "        `~--....___...---~'\n";
}
