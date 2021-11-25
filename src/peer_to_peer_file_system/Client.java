package peer_to_peer_file_system;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Client implements Runnable {

	private int RQ_Number = 0;
	private String list_of_possible_function[] = { "COMMANDS" , "REGISTER" ,"DE-REGISTER", "PUBLISH", "REMOVE" ,"RETRIEVE-ALL","RETRIEVE-INFOT", "SEARCH-FILE" , "UPDATE-CONTACT" };
	private String reduced_list_of_possible_function[] = { "COMMANDS" , "REGISTER" , "UPDATE-CONTACT"};

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
			//String close_connection = null;
			boolean registered_client = false;
			
			// RQ Number will be sent and kept track of it from the client side => server will only store that RQ number and reference the msg from it 
			while(true) {

				String input;

				// client is either not registered 
				if(client_name == null){

					System.out.println("Please register first or update your contact information. ( Type commands to see all the available commands )");

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
							
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("UPDATE-CONTACT")){ // assume that the sockets never change and the user can only update their IP Address
				
							System.out.println("Please enter the name you want to update your contact info with");

							input = input + " ; " + (RQ_Number++) + " ; " + sc.nextLine(); // function ; RQ Number ; name ; 
		
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
								System.out.println("You are already registered. Use one of the other commands ( Type commands to see all the available commands )");
								continue;
							}else{
								System.out.println("Please enter the name you want to register with, and the your TCP socket ( separated by a semicolumn (;) )");	
								input = input + " ; " + (RQ_Number++) + " ; " + sc.nextLine(); // function ; RQ number ; name ; TCP Socket
							}
	
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("DE-REGISTER")){
	
							input = input + " ; " + (RQ_Number++) + " ; " + client_name; // function ; RQ Number ; name 
							client_name = null;
							RQ_Number = 0 ; // reset the RQ number 

							buf = input.getBytes() ; //changes the input of the user to bytes and stores it in the byte array 
							DatagramPacket DpSend = new DatagramPacket(buf,buf.length,server_address,10000); // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
							ds.send(DpSend); // built in function that sends the info to the server 							
							System.out.println("Client: Info sent to server ");
							
							continue;

							// System.out.println("would you like to close connection to server ? (T/F)");
							// close_connection = sc.nextLine();
		
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("PUBLISH")){ // assume that the user inserted all the values as lowercase
		
							System.out.println("Please enter the list of files that you want to publish ( separated by a semicolumn (;) , and all in lowercase )"); 
		
							input = input + " ; " + (RQ_Number++) + " ; " + client_name + " ; "+ sc.nextLine(); // function ; RQ Number ; name ; list of file to add... each separated by a " ; " 
		
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("REMOVE")){
		
							System.out.println("Please enter the list of files that you want to remove ( separated by a semicolumn (;) , and all in lowercase )");
		
							input = input + " ; " + (RQ_Number++) + " ; " + client_name + " ; "+ sc.nextLine(); // function ; RQ Number ; name ; list of file to remove ... each separated by a " ; " 
		
						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("RETRIEVE-ALL")){
				
							input = input + " ; " + (RQ_Number++); // function ; RQ Number ; 

						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("RETRIEVE-INFOT")){

							System.out.println("Please enter the name of the client you want to retrieve info about");
				
							input = input + " ; " + (RQ_Number++) + " ; " + sc.nextLine(); // function ; RQ Number ; peer-client-name ;

						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("SEARCH-FILE")){

							System.out.println("Please enter the name of the file that you want to search");
				
							input = input + " ; " + (RQ_Number++) + " ; " + sc.nextLine(); // function ; RQ Number ; File-name ;

						}else if(input.replaceAll("\\s+","").equalsIgnoreCase("UPDATE-CONTACT")){ // assume that the sockets never change and the user can only update their IP Address
				
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

				// TODO: Add timers so that the client doesnt wait forever when the server doesnt respond ( server crashed )

				//***************************This is used to save the client name once registered***************************************************
				if(byteToString(received_message).toString().replaceAll("\\s+","").split(";")[0].equalsIgnoreCase("Registered")){

					client_name = input.toString().replaceAll("\\s+","").split(";")[2];
					registered_client = true;

				//***************************************************************This is used to retrieve all the information about a specific client*********************************************
				}else if(byteToString(received_message).toString().replaceAll("\\s+","").split(";")[0].equalsIgnoreCase("RETRIEVE-INFOT")){

					String[] temp = byteToString(received_message).toString().replaceAll("\\s+","").split(";");

					System.out.println("RETRIEVE-INFOT ; " + temp[1] + " ; All the client info: \n" + temp[2]);

					continue; // so that it doesnt re-print the received msg from the server again ( from the below printout )

				//***************************This is used to retrieve all the information about all the clients registered with the server***************************************************
				}else if(byteToString(received_message).toString().replaceAll("\\s+","").split(";")[0].equalsIgnoreCase("RETRIEVE")){

					String[] temp = byteToString(received_message).toString().replaceAll("\\s+","").split(";");

					System.out.println("RETRIEVE ; " + temp[1] + " ; All the clients with their respective information: \n" );
					for (int i = 2 ; i < temp.length ; i++){
						System.out.println(temp[i]);
					}

					continue; // so that it doesnt re-print the received msg from the server again ( from the below printout )
				
				//***************************This is used to return all the clients offering that file to be donwloaded***************************************************
				}else if(byteToString(received_message).toString().replaceAll("\\s+","").split(";")[0].equalsIgnoreCase("SEARCH-FILE")){

					String[] temp = byteToString(received_message).toString().replaceAll("\\s+","").split(";");

					System.out.println("SEARCH-FILE ; " + temp[1] + " ; All the clients that offer the file: \n" );
					for (int i = 2 ; i < temp.length ; i++){
						System.out.println(temp[i]);
					}

					continue; // so that it doesnt re-print the received msg from the server again ( from the below printout )

				//***************************This is used to update the Ip address of the client***************************************************
				}else if(byteToString(received_message).toString().replaceAll("\\s+","").split(";")[0].equalsIgnoreCase("UPDATE-CONFIRMED")){

					client_name = input.toString().replaceAll("\\s+","").split(";")[2];
					registered_client = true;

				}
				//***********************************************************************************************************************************/		

					

				System.out.println("Client: Data Received -> " + byteToString(received_message).toString());



				// //P2P file transfer
				// if (get_value_at_column(received_message, 0).equalsIgnoreCase("SEARCH-FILE")){
				// 	//prompt user if they want to download the file
				// 	System.out.println("Would you like to download the file? (y/n)");
				// 	if(sc.nextLine().equalsIgnoreCase("y")){
				// 		List<String> info = new ArrayList<String>();
				// 		//add all users that have the file to info List<String>
				// 		//TODO: what happens if different files have the same name?
				// 		for (int i = 2; i < get_length_of_message(received_message); i++){
				// 			info.add(get_value_at_column(received_message, i));
				// 		}
				// 		P2P_Client(info);
				// 	}
				// }




				received_message = new byte[65535]; // clearing the buffer after every message 
			
				
			}
			
			// sc.close();
			// ds.close();
			

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




	// /*
	// Listener handle on TCP connection to send file if requested
	//  */
	// public static void P2P_Server() throws IOException {
	// 	//TODO: thread listener for peers that want to establish a connection
	// 	String file = "";

	// 	//TODO: sends "DOWNLOAD-ERROR ; RQ# ; Reason" if the requested file doesn't exist
	// 	//checks if file exists and reads file
	// 	String file_content = read_file(file);
	// 	if (file_content == null){
	// 		//TODO: sends "DOWNLOAD-ERROR ; RQ# ; Reason"
	// 	}
	// 	List<String> chunks = new ArrayList<String>();
	// 	Integer number_of_chunks = 0;
	// 	while(chunks.size()*200 < file_content.length()){
	// 		if(chunks.size()*200+200 > file_content.length()){
	// 			chunks.add(new String(file_content.substring(chunks.size()*200,file_content.length())));
	// 		}else{
	// 			chunks.add(new String(file_content.substring(chunks.size()*200,chunks.size()*200+200)));
	// 		}
	// 	}

	// 	//sends all chunks
	// 	for(int i = 0; i < chunks.size(); i++){
	// 		Integer chunk_number = i;
	// 		Integer checksum = checksum(chunks.get(i));
	// 		String text = chunks.get(i);

	// 		//on last chunk to send
	// 		if(i == chunks.size()-1){
	// 			//TODO: sends "FILE-END ; RQ# ; File-name ; Chunk# ; Text" on the last chunk
	// 		}else{
	// 			//TODO: sends all chunks to Peer1: "FILE ; RQ# ; File-name ; Chunk# ; Checksum ; Text"
	// 		}
	// 	}

	// 	//TODO: sends "FILE ; RQ# ; File-name ; Chunk# ; Checksum ; Text" of chunks that have been Rejected or Timed-out
	// 	//TODO: Peer2 ends the connection with Peer1 on Time-out
	// }

	// public static String read_file(String file_name) throws IOException {
	// 	BufferedReader br = new BufferedReader(new FileReader(file_name));
	// 	String file_content = null;
	// 	try {
	// 		StringBuilder sb = new StringBuilder();
	// 		String line = br.readLine();

	// 		while (line != null) {
	// 			sb.append(line);
	// 			sb.append(System.lineSeparator());
	// 			line = br.readLine();
	// 		}
	// 		file_content = sb.toString();
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 	} finally {
	// 		br.close();
	// 		return file_content;
	// 	}
	// }

	// public static Integer checksum(String chunk){
	// 	Integer checksum = 0;
	// 	byte[] byteArray = chunk.getBytes();
	// 	for (int i = 0; i < byteArray.length; i++){
	// 		checksum += byteArray[i];
	// 	}
	// 	return checksum;
	// }

	// /*
	// Establishes a TCP connection, requests a file, and downloads the file if available.
	//  */
	// public static void P2P_Client(List<String> info) {
	// 	//TODO: prompt user to choose which user they want the file from
	// 	for(int i = 0; i < info.size(); i++){
	// 		//get name, IP address and TCP socket for user[i]
	// 		String name = info.get(i).replaceAll("\\s+","").split(":")[0];
	// 		String ip = info.get(i).replaceAll("\\s+","").split(":")[1];
	// 		String tcp_socket = info.get(i).replaceAll("\\s+","").split(":")[2];
	// 		System.out.println("Sending request to:\nName: " + name + "\nIP address: " + ip + "\nTCP socket: " + tcp_socket);
	// 		//TODO: establish TCP connection with peer
	// 		//TODO: Peer1 sends "DOWNLOAD ; RQ# ; File-name" to Peer2
	// 		//TODO: receives all chunks from Peer2 and sends: "REJ ; File-name ; Chunk#" after not receiving a chunk (time-out) or no-checksum equality
	// 		//TODO: ends the connection with Peer2 on Time-out and deletes incomplete file
	// 		//TODO: Peer1 ends the connection with Peer2 when file complete up till "FILE-END"
	// 	}
	// }

	// /*
	// Get value at column where columns are separated by ";" character
	//  */
	// public static String get_value_at_column(byte[] s, Integer column){
	// 	return byteToString(s).toString().replaceAll("\\s+","").split(";")[column];
	// }

	// /*
	// Get length
	//  */
	// public static Integer get_length_of_message(byte[] s){
	// 	return byteToString(s).toString().replaceAll("\\s+","").split(";").length;
	// }







}
