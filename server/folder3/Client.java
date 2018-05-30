
import java.net.*;
import java.io.*;

// Minimal client for the nio server
// Note: we can use io on this side
// MW 16/11/15

public class Client {

  public static void main( String[] args ) {
    try { 
      Socket connection = new Socket( "localhost", 8989 );

      PrintWriter p = new PrintWriter( connection.getOutputStream(), true );
      BufferedReader r = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );

      // read a string from the command line to send 
      System.out.println(" Sending: " + args[0] );
      p.println( args[0] );

      // read back the welcome and echo string
      System.out.println(" Received: " + r.readLine()) ;
      System.out.println(" Received: " + r.readLine()) ;

      connection.close();
    }
    catch( IOException e ) {
      e.printStackTrace();
    }
  }
}

