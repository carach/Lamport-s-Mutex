import java.util.Random;

/*
*  * To change this license header, choose License Headers in Project Properties.
*   * To change this template file, choose Tools | Templates
*    * and open the template in the editor.
*     */

/**
*  *
*   * @author Mousie
*    */
public class Event implements Comparable<Event>{
    String type;
    String targetFile;
    long timestamp;
    int clientID;

    public Event(String str1, String str2,long tm, int id)
    {
        this.type = str1;
        this.targetFile = str2;
        this.timestamp = tm;
        this.clientID = id;
    }
	
	public void print()
	{
		if ( type == "READ" || type == "WRITE")
			System.out.println("Event content: " + type + " "+ targetFile+ "@" + timestamp
				+ "," + clientID);
		else
			System.out.println("Event content: " + type + "@" + timestamp
				+ "," + clientID);
	}

    @Override
    public int compareTo(Event o) {

        if (this.timestamp < o.timestamp)
            return -1;
        else if (this.timestamp > o.timestamp)
            return 1;
        else if (this.clientID < o.clientID)
            return -1;
        else if (this.clientID > o.clientID)
            return 1;
        else return 0;
    }
}

