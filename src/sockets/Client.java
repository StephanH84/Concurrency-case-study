package sockets;

import java.io.*;
import java.net.*;

//Client process simulating the vehicle
public class Client {

	static int pos = 0;
	static int direction = 1;
	
	//Simulates the movement of a vehicle (its position is the saved as pos)
	static Thread vehicleThread = new Thread(new Runnable(){

		@Override
		public void run() {
			while(true) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				pos += direction;
			}
		}
	
	});
	
    public static void main(String arg[]) throws IOException, ClassNotFoundException {

        int portNum = 11113;
        vehicleThread.start();
        
        Socket socket = new Socket("localhost", portNum);

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        
        //Response loop for server requests
        while(true) {
        	String request = (String) in.readObject();
        	//System.out.println(request);
	        if ("getPos".equals(request)) {
	        	out.writeObject(pos);
	        } else if ("getDir".equals(request)) {
		        	out.writeObject(direction);
	        } else if ("changeDir".equals(request)) {
	        	direction *= -1;
	        } else if ("exit".equals(request)) {
	        	break;
	        }
        }
        out.close();
        in.close();
        socket.close();
        vehicleThread.stop();
    }
}