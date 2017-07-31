package sockets;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Line2D;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Server {
	
	//JFrame subclass as user interface
	static class frame extends JFrame {
		public frame(){
	        JPanel panel=new JPanel();
	        getContentPane().add(panel);
	        setSize(1000, 200);
			setTitle("TBA vehicle server");
	        
	        JButton newVehicleButton = new JButton("Instantiate vehicle");
	        newVehicleButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						Server.startVehicleProcess();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
	        });
	        panel.add(newVehicleButton);
	
	        JButton changeDirButton = new JButton("Change direction");
	        changeDirButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (vehicleInt < sockets.size()) {
						try {
							Server.changeVehicleDirection(vehicleInt);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
	        });
	        panel.add(changeDirButton);
	        
	        JLabel label = new JLabel("enter vehicle index:");
	        panel.add(label);
	        
	        final JTextField textField = new JTextField("0");
	        textField.setPreferredSize(new Dimension(60, 30));
	        textField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					update();
				}
				@Override
				public void removeUpdate(DocumentEvent e) {
					update();
				}
				@Override
				public void changedUpdate(DocumentEvent e) {
					update();
				}
	        	void update() {
	        		String text = textField.getText();
	        		try {
	        			vehicleInt = Integer.valueOf(text);
	        		} catch(NumberFormatException e) {
	        			vehicleInt = 0;
	        		}
	        	}
	        });
	        panel.add(textField);
	        
	        this.addWindowListener(new WindowListener() {
				@Override
				public void windowOpened(WindowEvent e) {
				}

				@Override
				public void windowClosing(WindowEvent e) {
					//When closing the window properly exit the socket connections
					Server.exit();
				}

				@Override
				public void windowClosed(WindowEvent e) {
				}

				@Override
				public void windowIconified(WindowEvent e) {
				}

				@Override
				public void windowDeiconified(WindowEvent e) {
				}

				@Override
				public void windowActivated(WindowEvent e) {
				}

				@Override
				public void windowDeactivated(WindowEvent e) {
				}
	        });
	    }

		//Drawing the vehicles
	    public void paint(Graphics g) {
	        super.paint(g);
	        Graphics2D g2 = (Graphics2D) g;
	        Stroke stroke = g2.getStroke();
	        g2.setStroke(new BasicStroke(4));
	        for (Integer pos : vehiclePositions) {
	        	int xPos = (pos*3 + 500) % 1000;
	        	g2.draw(new Line2D.Float(xPos, 100, xPos, 100));
	        }
	        g2.setStroke(stroke);
        	g2.draw(new Line2D.Float(0, 90, 1000, 90));
        	g2.draw(new Line2D.Float(0, 110, 1000, 110));
	        
	    }
	}
	static frame frame = null;
	
	static int vehicleInt = 0;
	static List<Integer> vehiclePositions = new ArrayList<Integer>();
	
	static ServerSocket listener = null;
	static List<Socket> sockets = new ArrayList<Socket>();
	static List<ObjectOutputStream> outs = new ArrayList<ObjectOutputStream>();
	static List<ObjectInputStream> ins = new ArrayList<ObjectInputStream>();
	
	// Port number to bind server to.
	static int portNum = 11113;
	
	//Defines the thread for accepting socket connections from clients
	static Thread acceptingThread = new Thread(new Runnable() {

		@Override
		public void run() {
			// Socket for server to listen at.
			try {
				listener = new ServerSocket(portNum);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			System.out.println("Server is now running at port: " + portNum);
		    while (true) {
		      try {
		        //Blocked till request comes in
		        System.out.println ("ServerSocket - accepting");
		        Socket clientSocket = listener.accept();
		        
		        System.out.println ("ServerSocket - accept done");
		        sockets.add(clientSocket);		        
		        ins.add(new ObjectInputStream(clientSocket.getInputStream()));
		        outs.add(new ObjectOutputStream(clientSocket.getOutputStream()));
				System.out.println ("ServerSocket - Thread started, next client please...");
		      } catch (IOException e) {
		        System.out.println("'accept' on port 11113 failed");
		        System.exit(-1);
		      }
		    }
		}
		
	});
		
    public static void main(String[] args) throws IOException, ClassNotFoundException {

    	acceptingThread.start();
        
    	frame = new frame();
    	new Timer(250, new ActionListener(){
    		  public void actionPerformed(ActionEvent e) {
				vehiclePositions.clear();
				for (int clientInt = 0; clientInt < sockets.size(); clientInt++) {
					try {
						vehiclePositions.add(Server.getVehiclePosition(clientInt));
					} catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				
				}
				frame.repaint();
    		  }
    		}).start();
    	
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	frame.setVisible(true);
    	
        consoleHandling();        
    }

    //Handles the console as optional user interface
	private static void consoleHandling() throws IOException,
			ClassNotFoundException {
		BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in));
        String strInput = null;
        
        while ((strInput = sysIn.readLine()) != null) {
        	if ("newVehicle".equals(strInput)) {
                startVehicleProcess();
        		continue;
        	}
        	String[] splitted = strInput.split(":");
        	int clientInt = Integer.valueOf(splitted[0]);
        	if (clientInt >= outs.size()) {
        		System.out.println("No client available with index " + clientInt);
        		continue;
        	}
        	String input = splitted[1];
        	if ("getPos".equals(input)) {
        		System.out.println("current position: " + getVehiclePosition(clientInt));
        	} else if ("getDir".equals(input)) {
        		System.out.println("current direction: " + getVehicleDirection(clientInt));        		
        	} else if ("changeDir".equals(input)) {
        		changeVehicleDirection(clientInt);
        	}
        }
	}

	public static void exit() {
			try {
				for (ObjectOutputStream out : outs) {
					out.writeObject("exit");
					out.close();
				}
				for (ObjectInputStream in : ins) {
					in.close();
				}
				for (Socket socket : sockets) {
					socket.close();
				}
				listener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private static void changeVehicleDirection(int clientInt) throws IOException {
		outs.get(clientInt).writeObject("changeDir");
	}

	private static int getVehicleDirection(int clientInt) throws IOException,
			ClassNotFoundException {
		outs.get(clientInt).writeObject("getDir");
		return (Integer) ins.get(clientInt).readObject();
	}

	private static int getVehiclePosition(int clientInt) throws IOException,
			ClassNotFoundException {
		outs.get(clientInt).writeObject("getPos");
		return (Integer) ins.get(clientInt).readObject();
	}

	private static void startVehicleProcess() throws IOException {
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome +
		        File.separator + "bin" +
		        File.separator + "java";
		String classpath = System.getProperty("java.class.path");
		String className = "sockets.Client";
		Runtime.getRuntime().exec(javaBin + " -cp " + classpath + " " + className);
	}
}