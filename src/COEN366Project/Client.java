package COEN366Project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class Client implements Runnable {

	private int RQ_Number = 0;
	private String list_of_possible_function[] = { "COMMANDS" , "REGISTER" , "LOGIN" ,"DE-REGISTER", "PUBLISH", "REMOVE" ,"RETRIEVE-ALL","RETRIEVE","RETRIEVE-INFOT", "SEARCH-FILE" , "UPDATE-CONTACT" };
	private String reduced_list_of_possible_function[] = { "COMMANDS" , "REGISTER" , "LOGIN"};

	public void run() {	

		Scanner sc = new Scanner(System.in); // take an input from the user 
		//list_of_possible_function.add("REGISTER");

		DatagramSocket ds;
		try {
			// port is specified in the case client shutdown and brought back to life again and chose another port than what it was using before 
			ds = new DatagramSocket(20000);

			// USE THIS WHEN YOU ARE DONE WITH THE CODE AND YOU WILL TEST FOR REAL => THIS IS TO TAKE THE IP ADDRESS OF THE SERVER AS AN INPUT FROM THE USER 
			// System.out.println("Please enter the IP_Address of the server that you want to communicate with");
			// String server_ip = sc.nextLine();
			// InetAddress server_address = InetAddress.getByName(server_ip); // saving the address of the server 

			InetAddress server_address = InetAddress.getByName("10.0.0.237"); // saving the address of the server 
			
			
			byte[] buf = null; // creating a byte array 
			String client_name = null;
			String close_connection = null;
			boolean registered_client = false;
			
			// RQ Number will be sent and kept track of it from the client side => server will only store that RQ number and reference the msg from it 
			while(true) {

				String input;

				// client is either not registered or not logged in 
				if(client_name == null){

					System.out.println("Please register or log-in if you are already registered. ( Type commands to see all the available commands )");

					input = sc.nextLine();

					// if the input of the user is one of the allowed reduced functions
					if(Arrays.asList(reduced_list_of_possible_function).contains(input.replaceAll("\\s+","").toUpperCase())){

						if(input.replaceAll("\\s+","").equalsIgnoreCase("COMMANDS")){ // assume that the sockets never change and the user can only update their IP Address

							System.out.println("List of commands : ");
							for(int i = 0 ; i < reduced_list_of_possible_function.length ; i++ ){
								System.out.println(reduced_list_of_possible_function[i]);
							}
							continue;
		
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("REGISTER")){
	
							System.out.println("Please enter the name you want to register with, and the your TCP socket ( separated by a semicolumn (;) )");	
							input = input + " ; " + (RQ_Number++) + " ; " + sc.nextLine(); // function ; RQ number ; name ; TCP Socket
							
	
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("LOGIN")){
	
							System.out.println("Please enter your name to login ");	
							input = input + " ; " + (RQ_Number++) + " ; " + sc.nextLine(); // function ; RQ number ; name 				
		
						}
					
					}else{
						System.out.println("Please use one of the following commands to talk to the server : ");
						for(int i = 0 ; i < reduced_list_of_possible_function.length ; i++ ){
							System.out.println(reduced_list_of_possible_function[i]);
						}
						continue;
					}			

				}else{

					System.out.println("Client: Enter Info to be sent to server");

					input = sc.nextLine();
					
					// if the input of the user is one of the allowed reduced function
					if(Arrays.asList(list_of_possible_function).contains(input.replaceAll("\\s+","").toUpperCase())){

						if(input.replaceAll("\\s+","").equalsIgnoreCase("COMMANDS")){ // assume that the sockets never change and the user can only update their IP Address

							System.out.println("List of commands : ");
							for(int i = 0 ; i < list_of_possible_function.length ; i++ ){
								System.out.println(list_of_possible_function[i]);
							}
							continue;
		
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("REGISTER")){
	
							if(registered_client){
								System.out.println("You are already registered. Use one of the other commands ( Type commands to see all the available commands)");
								continue;
							}else{
								System.out.println("Please enter the name you want to register with, and the your TCP socket ( separated by a semicolumn (;) )");	
								input = input + " ; " + (RQ_Number++) + " ; " + sc.nextLine(); // function ; RQ number ; name ; TCP Socket
							}
	
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("LOGIN")){
	
							if(registered_client){
								System.out.println("You are already logged-in. Use one of the other commands ( Type commands to see all the available commands)");
								continue;
							}else{
								System.out.println("Please enter your name to login ");	
								input = input + " ; " + (RQ_Number++) + " ; " + sc.nextLine(); // function ; RQ number ; name 
							}
		
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("DE-REGISTER")){
	
							input = input + " ; " + (RQ_Number++) + " ; " + client_name; // function ; RQ Number ; name 
							System.out.println("would you like to close connection to server ? (T/F)");
							close_connection = sc.nextLine();
		
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("PUBLISH")){ // assume that the user inserted all the values as lowercase
		
							System.out.println("Please enter the list of files that you want to publish ( separated by a semicolumn (;) , and all in lowercase )"); 
		
							input = input + " ; " + (RQ_Number++) + " ; " + client_name + " ; "+ sc.nextLine(); // function ; RQ Number ; name ; list of file to add... each separated by a " ; " 
		
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("REMOVE")){
		
							System.out.println("Please enter the list of files that you want to remove ( separated by a semicolumn (;) , and all in lowercase )");
		
							input = input + " ; " + (RQ_Number++) + " ; " + client_name + " ; "+ sc.nextLine(); // function ; RQ Number ; name ; list of file to remove ... each separated by a " ; " 
		
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("UPDATE-CONTACT")){ // assume that the sockets never change and the user can only update their IP Address
		
							System.out.println("Please enter your name to change your old IP address to the current one )");
		
							input = input + " ; " + (RQ_Number++) + " ; "+ client_name; // function ; RQ Number ; name ; 
		
						}

					}else{
						System.out.println("Please use one of the following commands to talk to the server : ");
						for(int i = 0 ; i < list_of_possible_function.length ; i++ ){
							System.out.println(list_of_possible_function[i]);
						}
						continue;
					}
				}

				buf = input.getBytes() ; //changes the input of the user to bytes and stores it in the byte array 
				
				DatagramPacket DpSend = new DatagramPacket(buf,buf.length,server_address,10000); // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
				
				ds.send(DpSend); // built in function that sends the info to the server 
				
				System.out.println("Client: Info sent to server ");
								
				byte[] received_message = new byte[65534];
				
				DatagramPacket DpReceive = new DatagramPacket(received_message, received_message.length);
				
				ds.receive(DpReceive); // built in function to receive info from the server. This is also a blocking call meaning that the thread will get blocked here until the server replies back 

				//***************************This is used to save the client name once registered***************************************************
				if(byteToString(received_message).toString().replaceAll("\\s+","").split(";")[0].equalsIgnoreCase("Registered")){

					client_name = input.toString().replaceAll("\\s+","").split(";")[2];
					registered_client = true;

				//***************************This is used to login the client and save their name ***************************************************
				}else if(byteToString(received_message).toString().replaceAll("\\s+","").split(";")[0].equalsIgnoreCase("Logged-in")){

					client_name = input.toString().replaceAll("\\s+","").split(";")[2];
					registered_client = true;

				//***************************This is used to end the connection with the server. ***************************************************
				}else if(byteToString(received_message).toString().replaceAll("\\s+","").split(";")[0].equalsIgnoreCase("De-Registered")){

					client_name = null;
					if(close_connection.equalsIgnoreCase("T")){
						break;
					}

				}
				//***********************************************************************************************************************************/		

					

				System.out.println("Client: Data Received -> " + byteToString(received_message).toString());
				received_message = new byte[65535]; // clearing the buffer after every message 
			
				
			}
			
			sc.close();
			ds.close();
			

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // creating a socket using whtvr is free on my computer rn 
		catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}





	// converts the byte array data to a string 
	public static StringBuilder byteToString(byte[] a) {
		
		if(a == null)
			return null;
		StringBuilder ret = new StringBuilder();
		int i=0;
		while(a[i]!=0) {
			ret.append((char) a[i]);
			i++;
		}
		return ret;
		
	}


}
