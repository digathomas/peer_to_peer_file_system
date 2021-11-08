import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Server implements Runnable {

    public File excel_file = new File("./client_info.xlsx");

	//public static void main(String[] args) throws IOException{
	public void run() {	

        if(!excel_file.isFile()){
            excel_file_creation();
        }

		DatagramSocket ds;
		try {
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

                    // if the info entered is correct -> save the info locally and on the excel sheet 
                    }else if(broken_down_message.length == 4){

                        // IP_address and port number are both extracted from the received msg 
                        client_registration(broken_down_message[2], DpReceive.getAddress(), DpReceive.getPort(), Integer.parseInt(broken_down_message[3]));
                        list_of_clients.add(broken_down_message[2]); // saving the names of all clients locally 
                        
                        String reply = "REGISTERED ; " + RQ_Num + " ; " + broken_down_message[2];
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println(broken_down_message[2] + " ; registered with RQ# ; " + RQ_Num);

                    }
                
                //**************************Client Login with the server***********************************************************************************************/    

                }else if(broken_down_message[0].equalsIgnoreCase("LOGIN")) {
				
                    // less info entered than needed 
                    if(broken_down_message.length < 3){

                        String reply = "LOGIN-DENIED ; " + RQ_Num + " ; missing one or more input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("LOGIN-DENIED ; " + RQ_Num + " ; missing one or more input fields");

                    // more info entered than needed
                    }else if(broken_down_message.length > 3){

                        String reply = "LOGIN-DENIED ; " + RQ_Num + " ; one or more extra input fields";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("LOGIN-DENIED ; " + " - " + RQ_Num + " ; one or more extra input fields");

                    // if the name entered is not taken 
                    }else if(!list_of_clients.contains(broken_down_message[2])){  

                        String reply = "LOGIN-DENIED ; " + RQ_Num + " ; name not found. No action taken";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("LOGIN-DENIED ; " + RQ_Num + " ; name not found. No action taken");

                    // if the info entered is correct -> save the info locally and on the excel sheet 
                    }else if(broken_down_message.length == 3){
                        
                        String reply = "LOGGED-IN ; " + RQ_Num + " ; " + broken_down_message[2];
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println(broken_down_message[2] + " ; logged-in with RQ# ; " + RQ_Num);

                    }
                
                //**************************Client DE-REGISTER with the server***********************************************************************************************/    

                }else if(broken_down_message[0].equalsIgnoreCase("DE-REGISTER")){
            
                    if(!list_of_clients.contains(broken_down_message[2])){  

                        String reply = "DE-REGISTERATION-DENIED " + RQ_Num + " : name not found. No action taken ";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("DE-REGISTERATION-DENIED " + broken_down_message[2] + " - " + RQ_Num + " name not found. No action taken");
                        
                    }else if(broken_down_message.length == 3){

                        // deletes the client from the excel file -> de-registers it 
                        delete_client(broken_down_message[2]);
                        list_of_clients.remove(broken_down_message[2]);
                        
                        String reply = "DE-REGISTERED ; " + RQ_Num;
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println(broken_down_message[2]+ " De-registered with RQ# : " + RQ_Num);

                    }
                
                //**************************Client PUBLISH with the server***********************************************************************************************/
                           
                }else if(broken_down_message[0].equalsIgnoreCase("PUBLISH")){
                
                    if(broken_down_message.length == 3){
 
                         String reply = "PUBLISH-DENIED " + RQ_Num + " : missing input - please enter the file names separated by a ' ; '";
                         message_to_send = reply.getBytes();
 
                         // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                         DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                         ds.send(DpSend); // built in function that sends the info to the client 
                         System.out.println("PUBLISH-DENIED "  + " - " + RQ_Num + " missing input - please enter the file names separated by a ' ; '");
 
                     }else if(!list_of_clients.contains(broken_down_message[2])){  
 
                        String reply = "PUBLISH-DENIED " + RQ_Num + " : name not found. Please register first";
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println("PUBLISH-DENIED " + broken_down_message[2] + " - " + RQ_Num + " name not found. Please register first");
                        
                    }else if(broken_down_message.length > 3){ // assuming that the user inserted the correct info in the field 

                        List<String> published_files ;

                        // retrieves all the already published files by the client 
                        if(!all_clients_with_files.get(broken_down_message[2]).isEmpty()){
                            published_files = new ArrayList<>(all_clients_with_files.get(broken_down_message[2]));
                        }else{
                            published_files = new ArrayList<>();
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
 
                         String reply = "REMOVE-DENIED " + RQ_Num + " : missing input - please enter the file names separated by a ' ; '";
                         message_to_send = reply.getBytes();
 
                         // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                         DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                         ds.send(DpSend); // built in function that sends the info to the client 
                         System.out.println("REMOVE-DENIED "  + " - " + RQ_Num + " missing input - please enter the file names separated by a ' ; '");
 
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
                        update_contact(broken_down_message[2] , InetAddress.getByName(broken_down_message[3]));
                        
                        String reply = "UPDATE-CONFIRMED " + RQ_Num;
                        message_to_send = reply.getBytes();

                        // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                        DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                        ds.send(DpSend); // built in function that sends the info to the client 
                        System.out.println(broken_down_message[2]+ " UPDATED-CONTACT with RQ# : " + RQ_Num);

                    }
                
                }

                 // TODO: if the client name is not found, can i just send back a msg saying to register or should it actually ignore it 

                 else{ // change this to the deregistration

                    String reply = "send more";
                    message_to_send = reply.getBytes();

                    // creating a packet that i will send to he server ( byte array, byte array size, ip address of the server , port number of the server )
                    DpSend = new DatagramPacket(message_to_send,message_to_send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                    ds.send(DpSend); // built in function that sends the info to the client 
                    System.out.println("Server: Send more ");

                }
                
                // else if(byteToString(receive).toString().equalsIgnoreCase("BYE")) {
                //     System.out.println("Server: Client sent bye .... Closing connection between server and client");
                //     break;
                // }else {
                //     String reply = "send more";					
                //     send = reply.getBytes();					
                //     DpSend = new DatagramPacket(send,send.length,DpReceive.getAddress(),DpReceive.getPort()); 
                //     ds.send(DpSend);
                // }
                    
                    
				received_message = new byte[65535]; // clearing the buffer after every message 
				
			}
			
			// ds.close(); 
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



    
    // registering the client by adding its information to an excel file 
    //int RQ, String client_name, InetAddress client_ip_address, int client_UDP_socket , int client_TCP_socket 
	public void client_registration(String client_name, InetAddress client_ip_address, int client_UDP_socket , int client_TCP_socket){
   
        try {
            FileInputStream inputStream = new FileInputStream(excel_file);
            Workbook workbookFactory = WorkbookFactory.create(inputStream);
            Sheet sheet = workbookFactory.getSheetAt(0);
            int number_of_rows = sheet.getLastRowNum();

            Object[][] client_info = {
                    {client_name, client_ip_address, client_UDP_socket, client_TCP_socket}
            };

            //iterating through the row 
            for (Object[] objArr : client_info) {

                Row row = sheet.createRow(++number_of_rows);
                int number_of_columns = 0;  
                Cell cell;
                    
                //iterating through the columns of each row 
                for (Object field : objArr) {
                    cell = row.createCell(number_of_columns++);
                    if (field instanceof String) {
                        cell.setCellValue((String) field);
                    }else if (field instanceof Integer) {
                        cell.setCellValue((Integer) field);
                    }else if (field instanceof InetAddress) {
                        cell.setCellValue(field.toString());
                    }
                }

            }

            inputStream.close();

            FileOutputStream outputStream = new FileOutputStream(excel_file);
            workbookFactory.write(outputStream);
            workbookFactory.close();
            outputStream.close();

        }catch (IOException | EncryptedDocumentException e ) {
            e.printStackTrace();
        }
    }


    // creates the excel file that holds client info if it hasnt been created yet 
    public void excel_file_creation(){

        XSSFWorkbook workbook = new XSSFWorkbook(); // create blank workbook 
        XSSFSheet sheet = workbook.createSheet("Client Info"); // Create a blank sheet
  
        // This data needs to be written (String[])
        Map<String, String[]> data = new TreeMap<String, String[]>();
        data.put("1", new String[]{"Client_Name", "IP_Address", "UDP_Socket", "TCP_Socket", "List of Files" });
  
        // Iterate over data and write to sheet
        Set<String> keyset = data.keySet();
        int rownum = 0;
        for (String key : keyset) {
            // this creates a new row in the sheet
            Row row = sheet.createRow(rownum++);
            String[] objArr = data.get(key);
            int cellnum = 0;
            for (String obj : objArr) {
                Cell cell = row.createCell(cellnum++);// this line creates a cell in the next column of that row
                cell.setCellValue(obj);
            }
        }
        try {
            // this Writes the info to the excel file 
            FileOutputStream out = new FileOutputStream(excel_file);
            workbook.write(out);
            out.close();
            workbook.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // // checks the excel file if the name that was passed exists or not 
    // public boolean name_found_in_excel(String name){

    //     try {

    //         FileInputStream inputStream = new FileInputStream(excel_file);
    //         Workbook workbookFactory = WorkbookFactory.create(inputStream);
    //         Sheet sheet = workbookFactory.getSheetAt(0);
    //         int number_of_rows = sheet.getLastRowNum();

    //         for(int i = 1; i <= number_of_rows ; i++){

    //             if(sheet.getRow(i).getCell(1).getStringCellValue().equalsIgnoreCase(name)){
    //                 workbookFactory.close();
    //                 inputStream.close();
    //                 return true;
    //             }

    //         }

    //         workbookFactory.close();
    //         inputStream.close();
    //     }catch (EncryptedDocumentException | IOException e) {
    //         e.printStackTrace();
    //     }
    //     return false;
        
    // }


    // deletes the information of the client from the excel file 
    public void delete_client(String name){

        try {

            FileInputStream inputStream = new FileInputStream(excel_file);
            Workbook workbookFactory = WorkbookFactory.create(inputStream);
            Sheet sheet = workbookFactory.getSheetAt(0);
            int number_of_rows = sheet.getLastRowNum();

            for(int i = 1; i <= number_of_rows ; i++){

                if(sheet.getRow(i).getCell(0).getStringCellValue().equalsIgnoreCase(name)){
                    
                    // deletes the row by shifting the upwards by one ( overriding the row )
                    if(sheet.getRow(i).getRowNum() > 0 && sheet.getRow(i).getRowNum() < number_of_rows)
                        sheet.shiftRows(sheet.getRow(i).getRowNum()+1 , number_of_rows , -1);
                    else if(sheet.getRow(i).getRowNum() == number_of_rows)
                        sheet.removeRow(sheet.getRow(i));

                    inputStream.close();
                    FileOutputStream out = new FileOutputStream(excel_file);
                    workbookFactory.write(out);
                    out.close();
                    workbookFactory.close();
                    break;

                }

            }

            inputStream.close();
            workbookFactory.close();

        }catch (EncryptedDocumentException | IOException e) {
            e.printStackTrace();
        }
        
    }

    // publishes the list of files of the user by writing them into the excel sheet 
    public List<String> publish_info(String name, List<String> list_of_files_to_publish, List<String> published_files){

        try{

            FileInputStream inputStream = new FileInputStream(excel_file);
            Workbook workbookFactory = WorkbookFactory.create(inputStream);
            Sheet sheet = workbookFactory.getSheetAt(0);
            int number_of_rows = sheet.getLastRowNum();

            for(int i = 1; i <= number_of_rows ; i++){

                if(sheet.getRow(i).getCell(0).getStringCellValue().equalsIgnoreCase(name)){
                    
                    Row row = sheet.getRow(i);
                    int last_column = row.getLastCellNum();
                    Cell cell;
                        
                    //iterating through the columns of that client 
                    for (String field : list_of_files_to_publish) {
                        //System.out.println(" this is the field :" + field);
                        if(published_files.contains(field.toLowerCase())){
                            System.out.println("File name: " + field + " is already published -> no action taken");
                        }else{
                            //System.out.println("i entered here !!");
                            cell = row.createCell(last_column++);
                            cell.setCellValue(field);
                            published_files.add(field);  
                            System.out.println("File name: " + field + " got published");           
                        }
                        
                    }

                    inputStream.close();

                    FileOutputStream outputStream = new FileOutputStream(excel_file);
                    workbookFactory.write(outputStream);
                    workbookFactory.close();
                    outputStream.close();

                    return published_files;
                }
            }

            inputStream.close();
            workbookFactory.close();

        }catch (EncryptedDocumentException | IOException e) {
            e.printStackTrace();
        }

        return published_files;

    }

    // removes the list of files of the user by removing them from the excel sheet 
    public List<String> remove_info(String name, List<String> list_of_files_to_remove, List<String> published_files){
        
        try{

            FileInputStream inputStream = new FileInputStream(excel_file);
            Workbook workbookFactory = WorkbookFactory.create(inputStream);
            Sheet sheet = workbookFactory.getSheetAt(0);
            int number_of_rows = sheet.getLastRowNum();

            for(int i = 1; i <= number_of_rows ; i++){

                if(sheet.getRow(i).getCell(0).getStringCellValue().equalsIgnoreCase(name)){
                    
                    Row row = sheet.getRow(i);
                        
                    //iterating through the columns of that client 
                    for (String field : list_of_files_to_remove) {
                        if(!published_files.contains(field.toLowerCase()) || published_files.isEmpty()){
                            System.out.println("File name: " + field + " is not published -> no action taken");
                        }else{
                            int last_column = row.getLastCellNum();
                            //iterate through the columns starting from column 4 ( the start of where list of files are saved)
                            for(int j = 4; j < last_column ; j++){
                                //if the file is found, it will be removed from both the excel file and the local list of files.
                                if(row.getCell(j).getStringCellValue().equalsIgnoreCase(field)){
                                    row.removeCell(row.getCell(j));
                                    published_files.remove(field);
                                    System.out.println("File name: " + field + " got deleted");

                                    // the rest of the cells to the right of the one deleted will be shifted to the left by 1 ( if the next cell is not null)
                                    for(int k = j ; k < last_column ; k++){
                                        if(row.getCell(k+1) != null){
                                            row.createCell(k).setCellValue(row.getCell(k+1).getStringCellValue());
                                            row.removeCell(row.getCell(k+1));
                                        }
                                    }
                                    break;
                                    
                                }
                            }           
                        }
                        
                    }

                    inputStream.close();
        
                    FileOutputStream outputStream = new FileOutputStream(excel_file);
                    workbookFactory.write(outputStream);
                    workbookFactory.close();
                    outputStream.close();
                    return published_files;
                }
            }

        }catch (EncryptedDocumentException | IOException e) {
            e.printStackTrace();
        }
        return published_files;
    }


    // updates the IP Address of the client 
    public void update_contact(String name, InetAddress new_address){

        try{

            FileInputStream inputStream = new FileInputStream(excel_file);
            Workbook workbookFactory = WorkbookFactory.create(inputStream);
            Sheet sheet = workbookFactory.getSheetAt(0);
            int number_of_rows = sheet.getLastRowNum();

            for(int i = 1; i <= number_of_rows ; i++){
                if(sheet.getRow(i).getCell(0).getStringCellValue() == name){
                    sheet.getRow(i).getCell(1).setCellValue(new_address.toString());;           
                }
            }

            inputStream.close();
            workbookFactory.close();

            FileOutputStream outputStream = new FileOutputStream(excel_file);
            workbookFactory.write(outputStream);
            workbookFactory.close();
            outputStream.close();

        }catch (EncryptedDocumentException | IOException e) {
            e.printStackTrace();
        }

    }


    // retrieve all the name of the clients from the excel file 
    public List<String> get_all_clients(){

        List<String> list_of_clients = new ArrayList<>();
        try {
            FileInputStream inputStream = new FileInputStream(excel_file);
            Workbook workbookFactory = WorkbookFactory.create(inputStream);
            Sheet sheet = workbookFactory.getSheetAt(0);
            int number_of_rows = sheet.getLastRowNum();

            for(int i = 1; i <= number_of_rows ; i++){
                list_of_clients.add(sheet.getRow(i).getCell(0).getStringCellValue());
            }

            workbookFactory.close();
            inputStream.close();
        }catch (EncryptedDocumentException | IOException e) {
            e.printStackTrace();
        }
        return list_of_clients;
        
    }


    // retrieve all the name and files associated to each of the clients from the excel file 
    public HashMap<String, List<String>> get_all_client_file_info(){

        List<String> client_file_list = new ArrayList<>();
        HashMap<String,List<String>> all_client_file_info = new HashMap<>();
        try {
            FileInputStream inputStream = new FileInputStream(excel_file);
            Workbook workbookFactory = WorkbookFactory.create(inputStream);
            Sheet sheet = workbookFactory.getSheetAt(0);
            int number_of_rows = sheet.getLastRowNum();

            for(int i = 1; i <= number_of_rows ; i++){

                Row row = sheet.getRow(i);
                int last_column = row.getLastCellNum();
                System.out.println("column value : " + last_column);

                for(int j = 4 ; j < last_column; j++){
                    client_file_list.add(row.getCell(j).getStringCellValue()); 
                }

                all_client_file_info.put(row.getCell(0).getStringCellValue(), client_file_list);

            }

            workbookFactory.close();
            inputStream.close();
        }catch (EncryptedDocumentException | IOException e) {
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
