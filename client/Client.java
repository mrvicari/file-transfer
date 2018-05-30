import java.net.*;
import java.io.*;
import java.util.Scanner;
import java.util.NoSuchElementException;

/**
 * Client class
 *
 * Application that can query a server for available files or download said files
 */
public class Client {

  private Socket socket;
  private InputStream in;
  private OutputStream out;
  private Scanner socketIn, keyboardInput;
  private PrintWriter socketOut;

  public Client (String host, int port) {
    try {
      socket = new Socket(host, port);
      in = socket.getInputStream();
      out = socket.getOutputStream();
    }
    catch (IOException e) {
      // e.printStackTrace();
      System.out.println("Error connecting to server");
    }
  }

  // Send keyboard input to server as a command
  private void talkToServer() {

    Scanner keyboardInput = new Scanner(System.in);
    PrintWriter socketOut = new PrintWriter(out, true);

    String command;

    Thread readerThread = new Thread(new Reader());
    readerThread.start();

    while ((command = keyboardInput.nextLine()) != null && !command.equals("exit")) {
      socketOut.println(command);
    }

    closeConnection();
  }

  // Close socket connection to server
  private void closeConnection() {

    try {
      socket.close();
      System.out.println("\nConnection closed\n");
      System.exit(0);
    }
    catch (IOException e) {
      System.out.println("Error closing client connection");
    }
  }

  /**
   * Reader class
   *
   * A runnable class that calls helper methods to read text or files based on
   * byte-flag
   */
  private class Reader implements Runnable {

    // Read byte-flag and call the appropriate method
    public void run() {

      while(true) {
        try {
          int fileFlag = in.read();

          if (fileFlag == 0) {
            readText();
          }
          else if (fileFlag == 1) {
            receiveDirectory();
          }
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    // Print text received from server
    private void readText() {

      Scanner socketIn = new Scanner(in);

      String response;

      // Wait for endOfText token to break while loop
      while ((response = socketIn.nextLine()) != null && !response.equals("endOfText")) {
        System.out.println(response);
      }
      System.out.println();
    }

    /**
     * Receive and save files coming from server
     *
     * Multiple file transfer is handled as follows:
     * (1) Read directory name and create it as an empty directory
     * (2) Read the number of files to be received
     * (3) Read the file size
     * (4) Read the file name
     * (5) Save file into new directory
     * Repeat (3), (4) and (5) as necessary
     */
    private void receiveDirectory() throws IOException {

      BufferedInputStream bis = new BufferedInputStream(in);
      DataInputStream dis = new DataInputStream(bis);

      String dirName = dis.readUTF();
      File dir = new File(dirName);
      dir.mkdirs();

      int fileCount = dis.readInt();
      File[] files = new File[fileCount];

      for (int i = 0; i < fileCount; i++) {
        long fileLength = dis.readLong();
        String fileName = dis.readUTF();

        files[i] = new File(dirName + "/" + fileName);

        FileOutputStream fos = new FileOutputStream(files[i]);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        for (int j = 0; j < fileLength; j++) {
          bos.write(bis.read());
        }
        bos.flush();
        System.out.printf("%d/%d files dowloaded\n", i+1, fileCount);
      }

      System.out.println("\n" + dirName + " downloaded!\n");
    }
  }

  public static void main(String[] args) {

    System.out.println();
    System.out.println("list <directory>");
    System.out.println("download <directory>");
    System.out.println();

    Client client = new Client(args[0], Integer.parseInt(args[1]));
    client.talkToServer();
  }
}
