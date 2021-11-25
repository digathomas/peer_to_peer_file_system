package peer_to_peer_file_system;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import org.apache.commons.lang3.ArrayUtils;



public class Server implements Runnable {

    public File csv_file = new File("client_info.csv");

	//public static void main(String[] args) throws IOException{
	public void run() {	

		DatagramSocket ds;
		try {

            // File will get created if it doesnt already exist
            if(csv_file.createNewFile()){
                csv_file_initialization();
            }

            //create a static socket for the server so that the clients can know how to talk to server
			ds = new DatagramSocket(10000);
		
			byte[] received_message = new byte[65535];
			DatagramPacket DpReceive = null;
			DatagramPacket DpSend = null;
			byte[] message_to_send = null; // creating a byte array
            int RQ_Num;
            List<String> list_of_clients = new ArrayList<>(get_all_clients()); // holds all the clients registered 
            HashMap<String,List<String>> all_clients_with_files = new HashMap<>(get_all_client_file_info()); // hold all the clients that have files published 

            // for(int i = 0 ; i < list_of_clients.size() ; i++){
            //     System.out.println("List of clients initially has: ");
            //     System.out.println(list_of_clients.get(i));
            // }

            System.out.println("all clients with their corresponding files at the beginning of the program");
            all_clients_with_files.entrySet().forEach(entry -> {
                System.out.println(entry.getKey() + " " + entry.getValue());
            });
			
			while(true) {
                
				DpReceive = new DatagramPacket(received_message, received_message.length);
				ds.receive(DpReceive); //blocking call until server receives sth from a client 
				
				System.out.println("Client Message: " + byteToString(received_message));

                String[] broken_down_message = byteToString(received_message).toString().replaceAll("\\s+","").split(";");

                RQ_Num = Integer.parseInt(broken_down_message[1]);

                //*************************  Client REGISTER with the server - assumption that a client can only register if they input the correct fields************************
                // Assuption: client only enters on console -> Function , name , and TCP socket 
				if(broken_down_message[0].equalsIgnoreCase("REGISTER")) {
				
                    // less info entered than needed 
                    if(broken_down_message.length < 4){

                        String reply = "REGISTERED-DENIED ; " + RQ_Num + " ; missing one or more input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("REGISTERED-DENIED ; " + RQ_Num + " ; missing one or more input fields");

                    // more info entered than needed
                    }else if(broken_down_message.length > 4){

                        String reply = "REGISTERED-DENIED ; " + RQ_Num + " ; one or more extra input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("REGISTERED-DENIED ; " + " - " + RQ_Num + " ; one or more extra input fields");

                    // if the name entered is not taken 
                    }else if(list_of_clients.contains(broken_down_message[2])){  

                        String reply = "REGISTERED-DENIED ; " + RQ_Num + " ; name already exists/taken";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("REGISTERED-DENIED ; " + RQ_Num + " ; name already exists/taken");

                    // if the info entered is correct -> save the info locally and on the csv sheet 
                    }else if(broken_down_message.length == 4){

                        // IP_address and port number are both extracted from the received msg 
                        client_registration(broken_down_message[2], DpReceive.getAddress().getHostAddress(), Integer.toString(DpReceive.getPort()), broken_down_message[3]);
                        list_of_clients.add(broken_down_message[2]); // saving the names of all clients locally 
                        all_clients_with_files.put(broken_down_message[2], null);

                        //System.out.println("name put into the hashamp: " + all_clients_with_files.get(broken_down_message[2]));
                        
                        String reply = "REGISTERED ; " + RQ_Num + " ; " + broken_down_message[2];
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println(broken_down_message[2] + " ; registered with RQ# ; " + RQ_Num);

                    }
                    
                //**************************Client DE-REGISTER with the server***********************************************************************************************/    

                }else if(broken_down_message[0].equalsIgnoreCase("DE-REGISTER")){
            
                    if(!list_of_clients.contains(broken_down_message[2])){  

                        System.out.println("DE-REGISTERATION-DENIED " + broken_down_message[2] + " - " + RQ_Num + " name not found. No action taken");
                        
                    }else if(broken_down_message.length == 3){

                        // deletes the client from the csv file -> de-registers it 
                        delete_client(broken_down_message[2]);
                        list_of_clients.remove(broken_down_message[2]);
                        all_clients_with_files.remove(broken_down_message[2]);

                        System.out.println(broken_down_message[2]+ " De-registered with RQ# : " + RQ_Num);

                    }
                
                //**************************Client PUBLISH with the server***********************************************************************************************/
                           
                }else if(broken_down_message[0].equalsIgnoreCase("PUBLISH")){
                
                    if(broken_down_message.length == 3){
 
                         String reply = "No input found " + RQ_Num + " -> nothing is published ";
                         message_to_send = reply.getBytes();
 
                         // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                         DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                         ds.send(DpSend); // built in function that sends the info to the client 
                         System.out.println("No input found "  + " - " + RQ_Num + " -> nothing is published");
 
                     }else if(!list_of_clients.contains(broken_down_message[2])){  
 
                        String reply = "PUBLISH-DENIED " + RQ_Num + " : name not found. Please register first";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("PUBLISH-DENIED " + broken_down_message[2] + " - " + RQ_Num + " name not found. Please register first");
                        
                    }else if(broken_down_message.length > 3){ // assuming that the user inserted the correct info in the field 

                        List<String> published_files = new ArrayList<>() ;

                        System.out.println("value of a the hashmap:" + all_clients_with_files.get(broken_down_message[2]));

                        // retrieves all the already published files by the client 
                        if(all_clients_with_files.get(broken_down_message[2]) != null ){
                            published_files = new ArrayList<>(all_clients_with_files.get(broken_down_message[2]));
                        }
                        
                        List<String> list_of_files_to_publish = new ArrayList<>();

                        for(int i = 3 ; i < broken_down_message.length ; i++){
                            list_of_files_to_publish.add(broken_down_message[i]);
                        }

                        //publishing the files and return the total list of files published 
                        published_files = publish_info(broken_down_message[2], list_of_files_to_publish, published_files);
                        
                        //saving the files to clients locally for easier access 
                        all_clients_with_files.put(broken_down_message[2], published_files);
 
                        String reply = "PUBLISHED " + RQ_Num ;
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println(broken_down_message[2] + " PUBLISHED files with RQ# : " + RQ_Num);
 
                     }

                //**************************Client REMOVE with the server***********************************************************************************************/
                
                }else if(broken_down_message[0].equalsIgnoreCase("REMOVE")){
                
                    if(broken_down_message.length == 3){
 
                         String reply = "No input found " + RQ_Num + " -> nothing is removed";
                         message_to_send = reply.getBytes();
 
                         // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                         DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                         ds.send(DpSend); // built in function that sends the info to the client 
                         System.out.println("No input found "  + " - " + RQ_Num + " -> nothing is removed");
 
                     }else if(!list_of_clients.contains(broken_down_message[2])){  
 
                        String reply = "REMOVE-DENIED " + RQ_Num + " : name not found. Please register first";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("REMOVE-DENIED " + broken_down_message[2] + " - " + RQ_Num + " name not found. Please register first");
                        
                    }else if(broken_down_message.length > 3){ // assuming that the user inserted the correct info in the field 

                        List<String> published_files ;

                        // retrieves all the already published files by the client 
                        if(!all_clients_with_files.get(broken_down_message[2]).isEmpty()){
                            published_files = new ArrayList<>(all_clients_with_files.get(broken_down_message[2]));
                        }else{
                            published_files = new ArrayList<>();
                        }
                        List<String> list_of_files_to_remove = new ArrayList<>();

                        for(int i = 3; i < broken_down_message.length ; i++){
                            list_of_files_to_remove.add(broken_down_message[i]);
                        }

                        //removing the files and returning the rest of the published files
                        published_files = remove_info(broken_down_message[2], list_of_files_to_remove, published_files);
                        
                        //saving the files to clients locally for easier access 
                        all_clients_with_files.put(broken_down_message[2], published_files);

                        String reply = "REMOVED " + RQ_Num ;
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println(broken_down_message[2] + " Removed files with RQ# : " + RQ_Num);
 
                    }

                //**************************Client RETRIEVE-INFOT with the server***********************************************************************************************/

                }else if(broken_down_message[0].equalsIgnoreCase("RETRIEVE-INFOT")){

                    if(broken_down_message.length < 3){
                        String reply = "RETRIEVE-ERROR ; " + RQ_Num + " ; missing one or more input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort());
                        ds.send(DpSend); // built in function that sends the info to the client
                        System.out.println("RETRIEVE-ERROR ; " + RQ_Num + " ; missing one or more input fields");

                    }else if(broken_down_message.length > 3){

                        String reply = "RETRIEVE-ERROR ; " + RQ_Num + " ; one or more extra input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort());
                        ds.send(DpSend); // built in function that sends the info to the client
                        System.out.println("RETRIEVE-ERROR ; " + " - " + RQ_Num + " ; one or more extra input fields");

                    }else if(broken_down_message.length == 3){

                        // while the string is not empty ( file not offered by any client)
                        if(!retrieve_infot(broken_down_message[2]).equals("")){

                            String reply = "RETRIEVE-INFOT ; " + RQ_Num + " ; " + retrieve_infot(broken_down_message[2]);
                            message_to_send = reply.getBytes();

                            // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                            DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort());
                            ds.send(DpSend); // built in function that sends the info to the client

                            System.out.println("RETRIEVE-INFOT ; " + RQ_Num + " ; All the client info: \n" + retrieve_infot(broken_down_message[2]));                          
                            
                        }else{

                            String reply = "RETRIEVE-ERROR ; " + RQ_Num + " ; client not found";
                            message_to_send = reply.getBytes();

                            // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                            DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort());
                            ds.send(DpSend); // built in function that sends the info to the client
                            System.out.println("RETRIEVE-ERROR ; " + RQ_Num + " ; client not found");
                        }
                            
                    }

                //**************************Client RETRIEVE-ALL with the server***********************************************************************************************/

                }else if(broken_down_message[0].equalsIgnoreCase("RETRIEVE-ALL")){

                    if(broken_down_message.length == 2){

                        // while the string is not empty ( file not offered by any client)
                        if(!retrieve_all().equals("")){

                            String reply = "RETRIEVE ; " + RQ_Num + " ; " + retrieve_all();
                            message_to_send = reply.getBytes();

                            // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                            DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort());
                            ds.send(DpSend); // built in function that sends the info to the client

                            String[] temp = retrieve_all().split(";");
                            System.out.println("RETRIEVE ; " + RQ_Num + " ; All clients registered with the server and their respective information: " );
                            for (int i = 0 ; i < temp.length ; i++){
                                System.out.println(temp[i]);
                            }
                            
                        }

                    }

                //**************************Client SEARCH-FILE with the server***********************************************************************************************/

                }else if(broken_down_message[0].equalsIgnoreCase("SEARCH-FILE")){

                    if(broken_down_message.length < 3){
                        String reply = "SEARCH-ERROR ; " + RQ_Num + " ; missing one or more input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort());
                        ds.send(DpSend); // built in function that sends the info to the client
                        System.out.println("SEARCH-ERROR ; " + RQ_Num + " ; missing one or more input fields");

                    }else if(broken_down_message.length > 3){

                        String reply = "SEARCH-ERROR ; " + RQ_Num + " ; one or more extra input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort());
                        ds.send(DpSend); // built in function that sends the info to the client
                        System.out.println("SEARCH-ERROR ; " + " - " + RQ_Num + " ; one or more extra input fields");

                    }else if(broken_down_message.length == 3){

                        // while the string is not empty ( file not offered by any client)
                        if(!search_info(broken_down_message[2]).equals("")){

                            String reply = "SEARCH-FILE ; " + RQ_Num + " ; " + search_info(broken_down_message[2]);
                            message_to_send = reply.getBytes();

                            // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                            DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort());
                            ds.send(DpSend); // built in function that sends the info to the client

                            String[] temp = search_info(broken_down_message[2]).split(";");
                            System.out.println("SEARCH-FILE ; " + RQ_Num + " ; All the clients that offer the file: " );
                            for (int i = 0 ; i < temp.length ; i++){
                                System.out.println(temp[i]);
                            }
                            
                        }else{

                            String reply = "SEARCH-ERROR ; " + RQ_Num + " ; file not found";
                            message_to_send = reply.getBytes();

                            // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                            DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort());
                            ds.send(DpSend); // built in function that sends the info to the client
                            System.out.println("SEARCH-ERROR ; " + RQ_Num + " ; file not found");
                        }

                    }

                //**************************Client UPDATE-CONTACT with the server***********************************************************************************************/

                }else if(broken_down_message[0].equalsIgnoreCase("UPDATE-CONTACT")){
            
                    if(broken_down_message.length < 3){

                        String reply = "UPDATE-DENIED ; " + RQ_Num + " ; missing one or more input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("UPDATE-DENIED ; " + RQ_Num + " ; missing one or more input fields");

                    // more info entered than needed
                    }else if(broken_down_message.length > 3){

                        String reply = "UPDATE-DENIED ; " + RQ_Num + " ; one or more extra input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("UPDATE-DENIED ; " + " - " + RQ_Num + " ; one or more extra input fields");

                    // if the name entered is not taken 
                    }else if(!list_of_clients.contains(broken_down_message[2])){  

                        String reply = "UPDATE-DENIED " + RQ_Num + " : name not found. No action taken ";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("UPDATE-DENIED " + broken_down_message[2] + " - " + RQ_Num + " name not found. No action taken");
                        
                    }else if(broken_down_message.length == 3){

                        // updates the client's IP Address to the one provided by the client 
                        update_contact(broken_down_message[2] , DpReceive.getAddress().getHostAddress());
                        
                        String reply = "UPDATE-CONFIRMED ; " + RQ_Num + " ; " + broken_down_message[2];
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println(broken_down_message[2]+ " UPDATED-CONTACT with RQ# : " + RQ_Num);

                    }
                
                }                    
                    
				received_message = new byte[65535]; // clearing the buffer after every message 
				
			}
			
			// ds.close(); 
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    
    // registering the client by adding its information to an csv file 
    //int RQ, String client_name, InetAddress client_ip_address, int client_UDP_socket , int client_TCP_socket 
	public void client_registration(String client_name, String client_ip_address, String client_UDP_socket , String client_TCP_socket){
   
        try {

            CSVWriter writer = new CSVWriter(new FileWriter(csv_file, true));

            // writing the header of the file 
            String[] header = {client_name, client_ip_address, client_UDP_socket, client_TCP_socket};

            writer.writeNext(header);
            writer.flush();
            writer.close();

        }catch (IOException e ) {
            e.printStackTrace();
        }
    }


    // creates the csv file that holds client info if it hasnt been created yet 
    public void csv_file_initialization(){

        try{

            CSVWriter writer = new CSVWriter(new FileWriter(csv_file));

            // writing the header of the file 
            String[] header = {"Client_Name", "IP_Address", "UDP_Socket", "TCP_Socket", "List of Files"};

            writer.writeNext(header);
            writer.flush();
            writer.close();

        }catch(IOException e){
            e.printStackTrace();
        }
        
    }

    // deletes the information of the client from the csv file 
    public void delete_client(String name){

        try {

            CSVReader csvReader = new CSVReader(new FileReader(csv_file));
            
            // reads all the content of the file and stores each row in an index of the list 
            List<String[]> csvBody = csvReader.readAll();

            //iterates through every row ( skiping first row cause its a header )
            for(int i = 1 ; i < csvBody.size() ; i++){

               if(csvBody.get(i)[0].equalsIgnoreCase(name)){
                   csvBody.remove(i); // removes the whole row
                   
                   csvReader.close();
                   CSVWriter csvWriter = new CSVWriter(new FileWriter(csv_file));
                   csvWriter.writeAll(csvBody);
                   csvWriter.flush();
                   csvWriter.close();

                   return;
               }

            }
            csvReader.close();
            

        }catch (IOException | CsvException e ) {
            e.printStackTrace();
        }
        
    }
    

    // publishes the list of files of the user by writing them into the csv sheet 
    public List<String> publish_info(String name, List<String> list_of_files_to_publish, List<String> published_files){

          try {

            CSVReader csvReader = new CSVReader(new FileReader(csv_file));
            
            // reads all the content of the file and stores each row in an index of the list 
            List<String[]> csvBody = csvReader.readAll();

            //iterates through every row ( skiping first row cause its a header )
            for(int i = 1 ; i < csvBody.size() ; i++){

                if(csvBody.get(i)[0].equalsIgnoreCase(name)){

                    String[] temp2= new String[list_of_files_to_publish.size()];
                    int counter = 0;

                    for(int j = 0 ; j < list_of_files_to_publish.size() ; j++){
                    
                        if(published_files.contains(list_of_files_to_publish.get(j).toLowerCase())){
                            System.out.println("File name: " + list_of_files_to_publish.get(j) + " is already published -> no action taken");
                        }else{
                            published_files.add(list_of_files_to_publish.get(j));
                            temp2[counter++] = list_of_files_to_publish.get(j); // stores all the new info to be added
                            System.out.println("File name: " + list_of_files_to_publish.get(j) + " got published"); 
                        }

                    }

                    String[] temp = csvBody.get(i); // gets all the old info that was found in the file 

                    String[] both = ArrayUtils.addAll(temp, temp2); // appends the old info with the new info 

                    csvBody.set(i, both); // replaces the old index of information with the new index of information 

                    csvReader.close();
                    CSVWriter csvWriter = new CSVWriter(new FileWriter(csv_file));
                    csvWriter.writeAll(csvBody);
                    csvWriter.flush();
                    csvWriter.close();

                    return published_files;
                    
                }


            }
            
            csvReader.close();

        }catch (IOException | CsvException e ) {
            e.printStackTrace();
        }

        return published_files;

    }

    // removes the list of files of the user by removing them from the csv sheet 
    public List<String> remove_info(String name, List<String> list_of_files_to_remove, List<String> published_files){
        
        try {

            CSVReader csvReader = new CSVReader(new FileReader(csv_file));
            
            // reads all the content of the file and stores each row in an index of the list 
            List<String[]> csvBody = csvReader.readAll();

            //iterates through every row ( skiping first row cause its a header )
            for(int i = 1 ; i < csvBody.size() ; i++){

                if(csvBody.get(i)[0].equalsIgnoreCase(name)){

                    for(int j = 0 ; j < list_of_files_to_remove.size() ; j++){
                    
                        if(!published_files.contains(list_of_files_to_remove.get(j).toLowerCase())){
                            System.out.println("File name: " + list_of_files_to_remove.get(j) + " is not published -> no action taken");
                        }else{
                            published_files.remove(list_of_files_to_remove.get(j));
                            System.out.println("File name: " + list_of_files_to_remove.get(j) + " got deleted"); 
                        }

                    }
                    
                    String[] temp = new String[4];
                    for(int k = 0 ; k < 4 ; k++){
                        temp[k] = csvBody.get(i)[k]; // gets all the old info that was found in the file ( without published file names )
                    }

                    String[] temp2 = published_files.toArray(new String[published_files.size()]); // gets all the published files ( after removing )
                    
                    String[] both = ArrayUtils.addAll(temp, temp2); // appends the old info with the new info  

                    csvBody.set(i, both); // replaces the old index of information with the new index of information 

                    csvReader.close();
                    CSVWriter csvWriter = new CSVWriter(new FileWriter(csv_file));
                    csvWriter.writeAll(csvBody);
                    csvWriter.flush();
                    csvWriter.close();

                    return published_files;
                    
                }


            }  
            
            csvReader.close();

        }catch (IOException | CsvException e ) {
            e.printStackTrace();
        }

        return published_files;
    }


    // searches through all the clients that are offering this file, then returns their names , IP Addresses , and TCP Sockets 
    public String search_info(String file_name){

        String list_of_clients = "";

        try{

           CSVReader csvReader = new CSVReader(new FileReader(csv_file));
           List <String[]> csvBody = csvReader.readAll();

           for (int i = 1 ; i < csvBody.size(); i++){
               for (int j = 4; j < csvBody.get(i).length; j++){
                   if(csvBody.get(i)[j].equalsIgnoreCase(file_name))
                   {
                       // concatenates "name ; ip_address ;  tcp_socket "
                       list_of_clients = list_of_clients + csvBody.get(i)[0] + " - " + csvBody.get(i)[1] + " - " + csvBody.get(i)[3] + " ; ";
                       break;
                   }
               }
           }



           csvReader.close();
        }catch (IOException | CsvException e ) {
            e.printStackTrace();
        }

        return list_of_clients;

    }


    // searches through all the clients that are registered and retrieve each client's info 
    public String retrieve_all(){

        String list_of_clients = "";

        try{

           CSVReader csvReader = new CSVReader(new FileReader(csv_file));
           List <String[]> csvBody = csvReader.readAll();

            for (int i = 1 ; i < csvBody.size(); i++){

                // concatenates "name ; ip_address ;  tcp_socket ; list of files"
                list_of_clients = list_of_clients + csvBody.get(i)[0] + " - " + csvBody.get(i)[1] + " - " + csvBody.get(i)[3];

                for (int j = 4; j < csvBody.get(i).length; j++){

                    list_of_clients = list_of_clients + " - " + csvBody.get(i)[j];
                    
                }
                
                list_of_clients = list_of_clients + " ; " ;
            }

           csvReader.close();

        }catch (IOException | CsvException e ) {
            e.printStackTrace();
        }

        return list_of_clients;

    }


    // retrieve all the information of the chosen client 
    public String retrieve_infot(String name){

        String client_info = "";

        try {

            CSVReader csvReader = new CSVReader(new FileReader(csv_file));
            
            // reads all the content of the file and stores each row in an index of the list 
            List<String[]> csvBody = csvReader.readAll();

            //iterates through every row ( skiping first row cause its a header )
            for(int i = 1 ; i < csvBody.size() ; i++){

                if(csvBody.get(i)[0].equalsIgnoreCase(name)){

                    client_info = csvBody.get(i)[0] + " - " + csvBody.get(i)[1] + " - " + csvBody.get(i)[3];

                    for(int j = 4 ; j < csvBody.get(i).length ; j++){
                        client_info = client_info + " - " + csvBody.get(i)[j];
                    }

                    csvReader.close();
                    return client_info;
                    
                }
            }
            
            csvReader.close();
            

        }catch (IOException | CsvException e ) {
            e.printStackTrace();
        }

        return client_info;

    }



    // updates the IP Address of the client 
    public void update_contact(String name, String new_address){

        try {

            CSVReader csvReader = new CSVReader(new FileReader(csv_file));
            
            // reads all the content of the file and stores each row in an index of the list 
            List<String[]> csvBody = csvReader.readAll();

            //iterates through every row ( skiping first row cause its a header )
            for(int i = 1 ; i < csvBody.size() ; i++){

                if(csvBody.get(i)[0].equalsIgnoreCase(name)){

                    csvBody.get(i)[1] = new_address; // replaces old address with the new address that the client is talking to us from

                    csvReader.close();
                    CSVWriter csvWriter = new CSVWriter(new FileWriter(csv_file));
                    csvWriter.writeAll(csvBody);
                    csvWriter.flush();
                    csvWriter.close();

                    return;
                    
                }


            }
            
            csvReader.close();

        }catch (IOException | CsvException e ) {
            e.printStackTrace();
        }

    }


    // retrieve all the name of the clients from the csv file 
    public List<String> get_all_clients(){

        List<String> list_of_clients = new ArrayList<>();

        try {

            CSVReader csvReader = new CSVReader(new FileReader(csv_file));
            
            // reads all the content of the file and stores each row in an index of the list 
            List<String[]> csvBody = csvReader.readAll();

            //iterates through every row ( skiping first row cause its a header )
            for(int i = 1 ; i < csvBody.size() ; i++){

                // adds the name into the array list
                list_of_clients.add(csvBody.get(i)[0]);

            }

            csvReader.close();

        }catch (IOException | CsvException e ) {
            e.printStackTrace();
        }

        return list_of_clients;
        
    }


    // retrieve all the name and files associated to each of the clients from the csv file 
    public HashMap<String, List<String>> get_all_client_file_info(){

        HashMap<String,List<String>> all_client_file_info = new HashMap<>();

        try {

            CSVReader csvReader = new CSVReader(new FileReader(csv_file));
            
            // reads all the content of the file and stores each row in an index of the list 
            List<String[]> csvBody = csvReader.readAll();

            //iterates through every row ( skiping first row cause its a header )
            for(int i = 1 ; i < csvBody.size() ; i++){
                
                List<String> client_file_list = new ArrayList<>();

                //iterating through every column ( starting from 4 because thats where the file names are saved )
                for(int j = 4 ; j < csvBody.get(i).length ; j++){
                    client_file_list.add(csvBody.get(i)[j]);
                }

                // inserting all the information of the user ( name + list of files ) into a hashmap
                all_client_file_info.put(csvBody.get(i)[0], client_file_list);

            }

            csvReader.close();            

        }catch (IOException | CsvException e ) {
            e.printStackTrace();
        }

        return all_client_file_info;
        
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
