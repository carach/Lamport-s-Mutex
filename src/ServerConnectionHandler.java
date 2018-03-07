import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Mousie
 */
public class ServerConnectionHandler implements Runnable{
    Socket clientSocket;
    int serverID;
    public ServerConnectionHandler(Socket sck,int id)
    {
	this.clientSocket = sck;
	this.serverID = id;
    }
   @Override
    public void run() {

	try(
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);          
	    
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
	    )
	{
        String[] inputLine = new String[4];
        String tmp = "";
//        while ((inputLine = in.readLine().split(":",4)) != null) {
        while ((tmp = in.readLine()) != null) {
            System.out.println("Request content: " + tmp);
            inputLine = tmp.split(":",4);
            System.out.println("Request type: " + inputLine[0] + " from " + inputLine[3]);
                switch (inputLine[0]){
                    case "ENQUIRY":
                        File folder = new File("../data" + serverID + "/");
                        File[] listOfFiles = folder.listFiles();

/*                       for (File listOfFile : listOfFiles) 
                        {
                            if (listOfFile.isFile()) 
                            {
                                System.out.println("File " + listOfFile.getName());
                            } 
                            else if (listOfFile.isDirectory()) 
                            {
                                System.out.println("Directory " + listOfFile.getName());
                            }
                        }
*/                        out.println(Arrays.toString(listOfFiles));
                        break;
                        
                    case "READ":
                        try(
                            BufferedReader br = new BufferedReader(new FileReader("../data" + serverID + "/" + inputLine[1])))
                            {
                                String currLine, lastLine = "";
                                while ((currLine = br.readLine()) != null)
                                    lastLine = currLine;
                                out.println(lastLine);
                            }
                        catch (IOException e) {
                            System.err.println(e);
                        }
                        break; 
                        
                    case "WRITE":
                        try(
                            PrintWriter append = new PrintWriter(new FileWriter("../data" + serverID + "/" + inputLine[1],true)))
                        {
                            append.println(inputLine[2] + ", " + inputLine[3]);
				            out.println("Write from " + inputLine[3] + " is done.");
                        }
                        catch (IOException e) {
                            System.err.println("Write is not done.");
                        }
                        break;
                        
                    default:
                        System.out.println("Invalid Request.");
                        break;
                }
            }
	    clientSocket.close();
	    }catch (IOException e) {
	                System.out.println("Exception caught when trying to listen for a connection");
			            System.out.println(e.getMessage());
	}

    }
    }
