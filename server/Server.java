import java.net.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Server class
 *
 * File transfer server where clients can request to list or transfer available
 * files
 */
public class Server {

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private int serverPort;

    public Server(int port) {
      serverPort = port;
      executorService = Executors.newFixedThreadPool(10);
    }

    // Main server loop, accept connections and submit them to Executor
    private void runServer() {

      try {
        System.out.println("Starting Server on port " + serverPort);
        serverSocket = new ServerSocket(serverPort);

        while(true) {
          System.out.println("\nWaiting for clients...");
          try {
              Socket s = serverSocket.accept();
              System.out.println("Processing request from " + s.getInetAddress());
              executorService.submit(new RequestHandler(s));
          }
          catch(IOException e) {
              System.out.println("Error accepting connection");
              e.printStackTrace();
          }
        }
      } catch(IOException e) {
          System.out.println("Error starting Server on " + serverPort);
          e.printStackTrace();
      }
    }

    /**
     * RequestHandler class
     *
     * A runnable class that receives incoming requests from clients and responds
     * accordingly
     */
    private class RequestHandler implements Runnable {

      private Socket socket;
      private PrintWriter socketWriter;
      private InputStream in;
      private OutputStream out;
      private DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

      public RequestHandler(Socket sckt) {
        socket = sckt;
        try {
          in = socket.getInputStream();
          out = socket.getOutputStream();
          socketWriter = new PrintWriter(out, true);
        }
        catch (IOException e) {
          System.out.println("Error opening input/output stream");
        }
      }

      /**
       * Read and log client requests, then determine what information to send
       *
       * Byte-flag 0 is used when sending plain text
       * Byte-flag 1 is used when sending file(s)
       */
      public void run() {

        try {
          BufferedReader socketReader = new BufferedReader(new InputStreamReader(in));

          String command;

          while ((command = socketReader.readLine()) != null && !command.equals("exit")) {

            if (command.startsWith("list")) {
              log(command);

              out.write(0);
              out.flush();

              if (command.equals("list")) {
                listContents(".", false);
              }
              // First check if directory exists
              else if (new File(command.replace("list ", "")).isDirectory()) {
                listContents(command.replace("list ", ""), true);
              }
              else {
                socketWriter.println("Directory not found!\n");
              }
            }
            else if (command.startsWith("download")) {
              log(command);
              // First check if directory exists
              if (new File(command.replace("download ", "")).isDirectory()) {
                out.write(1);
                out.flush();

                File dir = new File(command.replace("download ", ""));
                sendDirectory(dir);
              }
              else {
                out.write(0);
                out.flush();
                socketWriter.println("Directory not found!\n");
              }
            }
            else {
              out.write(0);
              out.flush();
              socketWriter.println("Invalid command!\n");
            }
          }
        }
        catch (IOException e) {
          e.printStackTrace();
        }
        closeConnection();
      }

      /**
       * Read in and send all files from requested directory
       *
       * In order to separate data properly, it is sent with the following format:
       * (1) Directory name
       * (2) Number of files
       * (3) File size
       * (4) File name
       * (5) File data
       * Repeat (3), (4) and (5) as necessary
       *
       * @param {String} dirName directory to be sent
       */
      private void sendDirectory(File dir) throws IOException {

        List<File> files = new ArrayList<>();;
        for (File file : dir.listFiles()) {
          if (file.isFile()) {
            files.add(file);
          }
          else if (file.isDirectory()) {
            // sendDirectory(file);
            // out.write(1);
          }
        }

        BufferedOutputStream bos = new BufferedOutputStream(out);
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeUTF(dir.getName());

        dos.writeInt(files.size());

        for (File file : files) {
          long length = file.length();
          dos.writeLong(length);

          String fileName = file.getName();
          dos.writeUTF(fileName);

          FileInputStream fis = new FileInputStream(file);
          BufferedInputStream bis = new BufferedInputStream(fis);

          int theByte = 0;
          while((theByte = bis.read()) != -1) {
            bos.write(theByte);
          }
          bis.close();
        }
        dos.flush();
      }

      /**
       * List all files/directories in requested location, endOfText token
       * notifies client that text response is over
       *
       * @param {String}  dir       requested directory
       * @param {Boolean} showFiles true for files and directories, false for
       *                            directories only
       */
      private void listContents(String dir, Boolean showFiles) {

        File folder = new File(dir);

        for (File curFile : folder.listFiles()) {
          if (curFile.isDirectory()) {
            socketWriter.println("Directory: " + curFile.getName());
          }
          else if (curFile.isFile() && showFiles) {
            socketWriter.println("File: " + curFile.getName());
          }
        }
        socketWriter.println("endOfText");
      }

      /**
       * Log client requests to a file with format:
       *
       * <date> <time> <IP address> <request>
       */
      private void log(String command) {

        try {
          Date date = new Date();
          PrintWriter fileWriter = new PrintWriter(new BufferedWriter(new FileWriter("requests.log", true)));
          fileWriter.println(sdf.format(date) + " " + socket.getInetAddress().toString().replace("/", "") + " " + command);
          fileWriter.close();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }

      // Close socket connection to client
      private void closeConnection() {

        log("exit");

        try {
          socket.close();
          System.out.println("Connection to " + socket.getInetAddress().toString().replace("/", "") + " closed\n\nWaiting for clients...");
        }
        catch (IOException e) {
          System.out.println("Error closing client connection");
        }
      }
    }

    public static void main(String[] args) throws IOException {

        Server server = new Server(Integer.parseInt(args[0]));
        server.runServer();
    }
}
