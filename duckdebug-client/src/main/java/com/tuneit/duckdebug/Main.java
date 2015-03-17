package com.tuneit.duckdebug;

import org.apache.commons.cli.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Main {

    public static final byte INTERACTIVE_MODE_CODE   = (byte) 0xff;
    public static final byte FILE_MODE_CODE          = (byte) 0x00;

    public static void main(String[] args) throws IOException {

        String host = "";
        int port = 0;
        String scriptFile = null;
        String key = "";


        Socket socket = null;
        final InputStream sis;
        final OutputStream sos;

        Options options = new Options();
        Option opt;

        opt = new Option("h", "host", true, "server host");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("p", "port", true, "server port");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("k", "key",  true, "binding key");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("f", "file", true, "script file");
        options.addOption(opt);

        boolean failed = false;
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine line = parser.parse( options, args );
            host = line.getOptionValue("h");
            port = Integer.parseInt(line.getOptionValue("p"));
            key = line.getOptionValue("k");
            if(line.hasOption("f")) {
                scriptFile = line.getOptionValue("f");
            }
        } catch (ParseException exp) {
            System.err.println(exp.getMessage());
            failed = true;
        } catch (NumberFormatException ex) {
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
            sis = socket.getInputStream();
            sos = socket.getOutputStream();

            DataOutputStream dos = new DataOutputStream(sos);
            dos.writeInt(key.getBytes().length);
            dos.write(key.getBytes());

            if (scriptFile == null) {

                dos.writeByte(INTERACTIVE_MODE_CODE);
                interactiveMode(sis, sos);

            } else {

                dos.writeByte(FILE_MODE_CODE);
                fileMode(sis, sos, scriptFile);

            }

        } catch (IOException ex) {
            System.err.println(ex.getLocalizedMessage());
        } finally {
            if(socket != null) {
                socket.close();
            }
        }
    }

    public static void interactiveMode(
            final InputStream sis, final OutputStream sos) {

        Thread recive = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int s;
                    byte[] buf = new byte[1024];
                    while (true) {
                        s = sis.read(buf);
                        if (s == -1) break;
                        System.out.write(buf, 0, s);
                        System.out.flush();
                    }
                } catch(IOException ex) { }
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
                        sos.write(c);
                        sos.flush();
                    }
                } catch(IOException ex) { }
            }
        });

        recive.start();
        send.start();
        try {
            recive.join();
            System.in.close();
            send.join();
        } catch (InterruptedException | IOException ex) {
            System.err.println(ex.getLocalizedMessage());
        }
    }

    public static void fileMode(
            final InputStream sis, final OutputStream sos, String fileName) {

        File file = new File(fileName);
        byte[] buf = new byte[(int)file.length()];

        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(buf, 0, (int) file.length());
            sos.write(buf);
            sos.flush();
        } catch(IOException ex) {
            System.err.println(ex.getLocalizedMessage());
        }
    }
}
