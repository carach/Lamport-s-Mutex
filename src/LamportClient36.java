import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.PriorityQueue;
import java.util.LinkedList;
import java.util.List;


/*
*  * To change this license header, choose License Headers in Project Properties.
*   * To change this template file, choose Tools | Templates
*    * and open the template in the editor.
*        need to update timestamp from TreeSet
*         */

/**
*  *
*   * @author Mousie
*    */
public class LamportClient36 {
	static int clientID = 36;
	static String[] meta;
	static HashMap<String, TreeSet<Event>> requestSetMap;
	static HashMap<String, PriorityQueue<Event>> replyQueueMap;
	static long timestamp ;
	static Event currEvent;

	public static synchronized Event generateEvent() throws IOException
    {
		System.out.println("Start generating events...");

		boolean duplicate = false;
		Random r = new Random();
		String file = meta[r.nextInt(meta.length)];
		requestSetMap.putIfAbsent(file, new TreeSet<Event>());
        String type = "";
        switch (r.nextInt(3))
        {
            case 0: type = "ENQUIRY";
                break;
            case 1: type = "READ";
                break;
            case 2: type = "WRITE";
                break;
        }

		for (Event ev: requestSetMap.get(file))	// check if request to the same file alrady exists
		{
			if (ev.clientID == clientID)
			{
				duplicate = true;
				System.out.println("No new event generated.");
				break;
			}
		}
		if (!duplicate)
		{
			Event newev = new Event ( type, file, timestamp++, clientID );
			requestSetMap.get(file).add(newev);
			System.out.print("New event generated: ");
			return newev;
		}
		else
			return null;		
	}
	
	public static Event generateEvent(String f) throws IOException
	{
		System.out.println("Releasing resourse...");
		return new Event ( "RELEASE", f, timestamp++, clientID );
	}

	public static synchronized void sendRequest2Peers (Event ev) throws IOException
	{
		String hostName;
		int portNumber;
		
		if ( ev != null)
		{
			for (int i = 36; i <=40; i++)
			{
				hostName = "dc" + Integer.toString(i) + ".utdallas.edu";
				portNumber = 9000 + i;
				if ( i == clientID )
					continue;
				try
				(
					Socket peerSocket = new Socket(hostName, portNumber );
					PrintWriter out = new PrintWriter(peerSocket.getOutputStream(), true);
					BufferedReader in = new BufferedReader(
					new InputStreamReader(peerSocket.getInputStream()))
				)
				{
					out.println(ev.type + ":" + ev.targetFile + ":" + Long.toString(ev.timestamp)
						+ ":" + Integer.toString(ev.clientID));
					String[] inputLine;
					if ((inputLine = in.readLine().split(":",4)) != null)	//process the "GOTIT" message
					{
						String file = inputLine[1];
						timestamp = Math.max(timestamp, Long.parseLong(inputLine[2]) + 1);
						Event newev = new Event(inputLine[0],file, timestamp++, Integer.parseInt(inputLine[3]));

						replyQueueMap.get(file).add(newev);
					}
				}catch (IOException e) {
					System.err.println("Couldn't get I/O for the connection to " + hostName);
				}
			}
		}
	}

	public static void processPeerRequest(Socket peerSocket) throws IOException
	{
		try (
      		PrintWriter out = new PrintWriter(peerSocket.getOutputStream(), true);
      		BufferedReader in = new BufferedReader( new InputStreamReader(peerSocket.getInputStream()))
      	)
      	{
			String[] inputLine;
        	if ((inputLine = in.readLine().split(":",4)) != null)
        	{
				  String file = inputLine[1];
				  int sourceClientID = Integer.parseInt( inputLine[3]);
          		timestamp = Math.max(timestamp, Long.parseLong(inputLine[2]) + 1);
		  		Event newev = new Event(inputLine[0],file, timestamp++, Integer.parseInt(inputLine[3]));

				synchronized(requestSetMap)
				{
					if ( requestSetMap.containsKey(file))
          			{
						TreeSet<Event> ts = requestSetMap.get(file);

						if ( inputLine[0] == "RELEASE")
						ts.pollFirst();
          			}
          			else
          			{
        				requestSetMap.putIfAbsent(file, new TreeSet<Event>());
						requestSetMap.get(file).add(newev);
				  	}
				}
				out.println("GOTIT:" + file + ":" + Long.toString( timestamp++ ) 
				  + ":" + Integer.toString(clientID));
        	}	
      	}catch (IOException e) {
        	System.out.println("Exception caught when trying to listen for a connection");
        	System.out.println(e.getMessage());
      	}
	}

	public static void listen2Peers() throws IOException
	{
		try
		(
			ServerSocket serverSocket = new ServerSocket(clientID + 9000);
		)
		{	
			while(true)
			{
				Socket peerSocket = serverSocket.accept();
				Thread t = new Thread( new Runnable(){
					public void run() {
						try{
						processPeerRequest(peerSocket);
						}
						catch (IOException e) {
							System.err.println("Couldn't get I/O for the connection" );
						}
						return;
					}
					});
				t.start();
			}

		}catch (IOException e) {
			System.out.println("Exception caught when trying to listen on port" + (clientID + 9000) + " or listening for a connection");
			System.out.println(e.getMessage());
		}	
	}

	
// check if the condition of entering the critical section is satisfied for a specific file
	public static boolean getTurn(String file)
	{
//		PriorityBlockingQueue<Event> q = replyQueueMap.get(file);
		if (replyQueueMap.get(file) == null)
			return false;

// delete expired reply messages;
		while (!replyQueueMap.get(file).isEmpty() && replyQueueMap.get(file).peek().timestamp < requestSetMap.get(file).first().timestamp)
			replyQueueMap.get(file).poll();

		List<Integer> lst = new LinkedList<Integer>();

		for (Event ev: replyQueueMap.get(file))
			if (!lst.contains(ev.clientID))
				lst.add(ev.clientID);
					
		System.out.println(lst.size() + " replies received.");
		if (lst.size() >= 4 && requestSetMap.get(file).first().clientID == clientID)
			return true;
		else return false;		
	}

	public static void executeEvent(Event ev, int serverID) throws IOException
	{
		String hostName = "dc" + Integer.toString(serverID) + ".utdallas.edu";
		int portNumber = 9000 + serverID;
		try (
			Socket clientSocket = new Socket(hostName, portNumber );
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
		)
		{
			if ( ev != null) 
			{
				out.println(ev.type + ":" + ev.targetFile + ":" + Long.toString (ev.timestamp)
					+ ":" + Integer.toString (ev.clientID));
				System.out.println(in.readLine());
				// to do: update meta
			}
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection" );
			}
	}
	
	public static void main( String args[]) throws IOException, InterruptedException
	{ 
		timestamp = clientID;
		requestSetMap = new HashMap<String, TreeSet<Event>>();
		replyQueueMap = new HashMap<String, PriorityQueue<Event>>();
		meta = new String[]{"tx1","tx2","tx3"};
		
		for (int i = 0; i < meta.length; i++)
		{
			requestSetMap.put(meta[i], new TreeSet<Event>());
			replyQueueMap.put(meta[i], new PriorityQueue<Event>());
		}

		new Thread(new Runnable(){
			public void run(){
				try
					{
						System.out.println("Starting listener...");
						listen2Peers();
					}
				catch (IOException e) {
				System.err.println("Couldn't get I/O for the connection" );
				}
			}
			}).start();

		System.out.println("Initialization finished.");
		Thread.sleep(6000);

		while(true)
			{
				sendRequest2Peers(generateEvent());
				
				for (String file: requestSetMap.keySet())
				{
					TreeSet<Event> ts = requestSetMap.get(file);
					if ( !ts.isEmpty() )
					{
// condition to enter its critical session
						if ( getTurn(file))
						{							
							Event ev = ts.pollFirst();
							System.out.print("Entering critical session...");
							ev.print();


							if(ev.type == "ENQUIRY" || ev.type == "READ")
							{
								Random rd = new Random();
								int serverID = rd.nextInt(3) + 33;
								System.out.println("Connecting Server: dc" + serverID + "...");

								executeEvent(ev,serverID);
							}
						
							else if (ev.type == "WRITE")
							{
								for (int i = 33; i <=35; i++)
								{
									System.out.println("Connecting Server: dc" + i + "...");
									executeEvent(ev,i);
								}
							}
					
							new Thread(new Runnable(){
								public void run(){
									try
										{
											sendRequest2Peers(generateEvent(file));	//send release message
										}
									catch (IOException e) {
									System.err.println("Couldn't get I/O for the connection" );
									}
								}
								}).start();
							
							
							break;
						}
				}
			}
		}
	}
}
