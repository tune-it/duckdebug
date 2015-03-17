package com.tuneit.duckdebug;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.fusesource.jansi.Ansi;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by tegoo on 11/14/14.
 */
public class Server {

    public static final byte INTERACTIVE_MODE_CODE   = (byte) 0xff;
    public static final byte FILE_MODE_CODE          = (byte) 0x00;

    private int port;
    private ServerSocket serverSocket;

    private int poolSize;
    private ExecutorService workers;

    private Map<String, Object> context;

    private Map<String, Binding> bindings = new Hashtable<>();

    public Server(int port, int poolSize, Map<String, Object> context) {
        this.port = port;
        this.poolSize = poolSize;
        this.context = context;
    }

    public Server(int port, int poolSize) {
        this(port, poolSize, new HashMap<String, Object>());
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
            final OutputStream sos;
            final InputStream sis;

            try {
                socket.setSoTimeout(60 * 60 * 1000);

                sos = socket.getOutputStream();
                sis = socket.getInputStream();

                DataInputStream dis = new DataInputStream(sis);

                int keySize = dis.readInt();
                byte[] keyBuf = new byte[keySize];
                dis.readFully(keyBuf);
                String key = new String(keyBuf);

                boolean interactive =dis.readByte() == INTERACTIVE_MODE_CODE;

                final Binding binding;
                if(bindings.containsKey(key)) {
                    binding = bindings.get(key);
                } else {
                    binding = new Binding();
                    bindings.put(key, binding);
                    for (Map.Entry<String, Object> entry : context.entrySet()) {
                        binding.setProperty(entry.getKey(), entry.getValue());
                    }
                    binding.setProperty("startTime", new Date());
                    binding.setProperty("bindings", bindings);
                }

                if (interactive) {

                    class DuckClosure extends Closure {
                        public DuckClosure( Object owner, Object thisObject ) {
                            super( owner, thisObject ) ;
                        }

                        public DuckClosure( Object owner ) {
                            super( owner ) ;
                        }

                        public Object call() {
                            try {
                                sos.write(Ansi.ansi().render("@|YELLOW " + duck + "|@")
                                        .toString().getBytes());
                                sos.flush();
                            } catch(IOException ex) { }
                            return null;
                        }
                    }

                    Groovysh shell = new Groovysh(binding, new IO(sis, sos, sos));
                    binding.setProperty("out", sos);
                    binding.setProperty("duck", new DuckClosure(shell));
                    shell.run("");
                    
                } else {
                    
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int s;
                    while((s = sis.read(buf)) != -1) {
                        baos.write(buf, 0, s);
                    }

                    Thread execution = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                GroovyShell shell = new GroovyShell(binding);
                                shell.evaluate(new String(baos.toByteArray()));
                            } catch(Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    binding.setProperty("thread", execution);
                    execution.start();
                }
            } catch (IOException ex) {

            } finally {
                tryClose();
            }
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
