import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;

public class LamportServer35 {
	static final int serverID = 35;
	static final int portNumber = 9035;

    	public static void main(String[] args) throws IOException {

            try 
	    {
	    	ServerSocket serverSocket = new ServerSocket( portNumber );
	    	while(true)
		{
			Socket clientSocket = serverSocket.accept();
			System.out.println("New request received.");
			new Thread( new ServerConnectionHandler(clientSocket,serverID)).start();
		}
	    } catch (IOException e) {
			System.out.println("Exception caught when trying to listen on port" + portNumber + " or listening for a connection");
			System.out.println(e.getMessage());
			}
		}	
	}
