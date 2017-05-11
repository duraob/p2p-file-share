package edu.ccsu.networking.main;

import edu.ccsu.networking.ui.*; 
import edu.ccsu.networking.udp.*;
import javax.swing.table.DefaultTableModel;
import java.net.InetAddress;
import java.util.ArrayList;


/*
* @author Ben and Khaled
*/



public class Server implements recMessages {
    // VARIABLES
    private SenderUDP sender;
    private ReceiverUDP receiver;
    private Thread receiverThread;
    String[] columns = {"File Name", "File Size", "Host IP", "Host Port"};
    String[][] directoryData = {};
    DefaultTableModel directory = new DefaultTableModel(directoryData, columns);
    DefaultTableModel temp = new DefaultTableModel(directoryData, columns);
    ArrayList<String> clients = new ArrayList<String>();
    ReceiverUDP setCaller;
    
    ServerMenu ui;
    
        
    // CONSTRUCTOR
    public Server(ServerMenu ui)
    {
        this.ui = ui;

    }

    // SERVER METHODS
    // UDP
    public void startSenderUDP(String port) throws Exception{
        this.sender = new SenderUDP(ui);
        this.sender.setPortNum(Integer.parseInt(port) + 1000); // ADD 1000 to split rcvr and sender ports
        this.sender.startSender();
       // setCaller.caller = 0;

        ui.jTextArea1.append("\nSERVER - Ready to Send\n");
    }
    
    public void startReceiverUDP(String port){
        receiver = new ReceiverUDP(this, ui);
        receiver.setPortNum(Integer.parseInt(port)); // split rcvr and sender ports
        receiverThread = new Thread(receiver); // new thread to handle multiple users
        receiverThread.start();

        ui.jTextArea1.append("SERVER - Ready to Receive\n");
    }    

    // Changes target to send to appropriate info
    public void updateSender(String ip, String port){
        try {

            ui.jTextArea1.append("SERVER - Update Target to: \n" + ip + " Port: " + port);
            sender.setTarIPAddress(InetAddress.getByName(ip));
            sender.setTarPort(Integer.parseInt(port) - 1000); 
        }
        catch(Exception e){
       
            ui.jTextArea1.append("SERVER - ERROR: Failed to update\n");
        }
    }

    // Slow mode for testing packet loss
    public void setSlowMode(boolean slow){
        receiver.setSlowMode(slow);
    }

    // update Directory
    public void refreshDirectory(){
        this.directory.fireTableDataChanged();
        ui.jTextArea1.append("SERVER - Table refreshed.\n");
    }

    // increase count of users on ServerMenu 
    public void addClient(String ip, String port){
        if(!(this.clients.contains(ip+port))){ // make sure client has correct connection info
            clients.add(ip+port);
            this.ui.refreshUsers(clients.size());
        }
    }

    // decrease count of users on ServerMenu
    public void rmvClient(String ip, String port){
        for(int i = 0; i < clients.size(); i++){
            if(this.clients.get(i).equals(ip+port)){
                this.clients.remove(i);
            }
        }
        this.ui.refreshUsers(clients.size());
    }

    // fresh table for any changes
    public void updateTableModel(DefaultTableModel oldT, DefaultTableModel newT){
        clearTableModel(oldT);
        for(int r = 0; r < newT.getRowCount(); r++){
            String[] tempData = {(newT.getValueAt(r,0).toString()),
                (newT.getValueAt(r,1).toString()),
                (newT.getValueAt(r,2).toString()),
                (newT.getValueAt(r,3).toString())};
            oldT.addRow(tempData);
        }
        oldT.fireTableDataChanged();
    }

    // clears table of all data
    public void clearTableModel(DefaultTableModel myTableModel){
        if (myTableModel.getRowCount() > 0) {
            for (int i = myTableModel.getRowCount() - 1; i > -1; i--) {
                myTableModel.removeRow(i);
            }
        }
    }
    
    // SETUP TCP - STATUS CODE: 600 - initiates host to start TCP
    public void sendHostClientReq(String info){
        try {
            
            ui.jTextArea1.append("SERVER - Send Host TCP info\n");
            byte[] dataByteArray = new byte[info.length() + 3]; // pads info for new headers

            System.arraycopy("600".getBytes(),0,dataByteArray,0,"600".getBytes().length); // add status code to header
            System.arraycopy(info.getBytes(), 0, dataByteArray, 3, info.getBytes().length);
            this.sender.rdtSend(dataByteArray);
        }
        catch(Exception e){
            
            ui.jTextArea1.append("SERVER - ERROR: Failed to initiate host TCP request\n");
            informAndUpdateResponse("400");
        }
    }    
    
    // STATUS CODE: 500 - get TCP info to pair clients
    public void clientConnectionInfo(String info){
        try {
            
            ui.jTextArea1.append("SERVER -  Get TCP info.");
            byte[] dataByteArray = new byte[info.length() + 3]; // pads info for new header

            System.arraycopy("500".getBytes(),0,dataByteArray,0,"500".getBytes().length); // add status code to header
            System.arraycopy(info.getBytes(), 0, dataByteArray, 3, info.getBytes().length);
            this.sender.rdtSend(dataByteArray);
        }
        catch(Exception e){
            
            ui.jTextArea1.append("SERVER:: ERROR: Failed to send client connection info.\n");
            informAndUpdateResponse("400");
        }
    }    

    // SERVER RESPONSES:
    // when Server gets a informAndUpdate - break the message by delimiters for appropriate functions
    public void rcvInformAndUpdate(String data, String ip, String port) throws Exception{
       
        ui.jTextArea1.append("SERVER - Client wants to informAndUpdate.\n");
        try {
            String[] files = data.split("\\?"); //split by ? for rows
            for (String file : files) {
                String[] temp = file.split("#"); // split by # for columns
                String[] row = {temp[0], temp[1], ip, port}; // FILE NAME, FILE SIZE, IP ADDR, PORT
                directory.addRow(row); // update table
                directory.fireTableDataChanged(); // notifies table that a row or row data has changed and needs to refresh
            }
            this.ui.refreshDir(directory); // method from ServerMenu - updates table data
            addClient(ip, port); // increase count of users on ServerMenu
            informAndUpdateResponse("200"); // STATUS CODE: OK
        }
        catch(Exception e){
            informAndUpdateResponse("400"); // STATUS CODE: ERROR!
        }
    }    
    
    // when Server gets a query - traverse through list of files for match to String queried
    public void rcvClientSearchReq(String data, String ip, String port) throws Exception{
        
        ui.jTextArea1.append("SERVER - Client wants to Query.\n");
        String results = "";
        for(int r = 0; r < directory.getRowCount(); r++){
            if((directory.getValueAt(r, 0).toString()).toLowerCase().contains(data.toLowerCase())){
                results += directory.getValueAt(r,0).toString() + "#" + 
                        directory.getValueAt(r,1).toString() + "#" + 
                        directory.getValueAt(r,2).toString() + "#" + 
                        directory.getValueAt(r,3).toString() + "?";
            }
        }
        System.out.println("SERVER - Results: " + results);
        clientSearchResponse(results);
        results = "";
    }

    // when Server gets a download request - split packet for appropriate data and search file for
    // the selected file to match the download request
    public void rcvDownloadReq(String data, String ip, String port){
        try {
            
            ui.jTextArea1.append("SERVER - Attempting to retrieve the connection info for the requested file.\n");
            directory.fireTableDataChanged();
            String[] row = data.split("#"); //column
            String info = "";
            for (int r = 0; r < directory.getRowCount(); r++) 
            {
                if (directory.getValueAt(r, 0).equals(row[0]) && directory.getValueAt(r, 1).equals(row[1])) 
                {
                    info = directory.getValueAt(r,0).toString() + "#" +
                            directory.getValueAt(r,1).toString() + "#" +
                            directory.getValueAt(r,2).toString() + "#" +
                            directory.getValueAt(r,3).toString();
                    updateSender(directory.getValueAt(r,2).toString(),
                            directory.getValueAt(r,3).toString());
                    r = directory.getRowCount();
                }
            }
            sendHostClientReq(info); // Sends info to ready client for TCP
            Thread.sleep(3000);
            updateSender(ip,port); // change target to appropriate info
            clientConnectionInfo(info); // gets client info
        }
        catch(Exception e){
            //System.out.println("SERVER - ERROR: Failed to setup client for TCP.");
            ui.jTextArea1.append("SERVER - ERROR: Failed to setup client for TCP.\n");
            informAndUpdateResponse("400");
        }
    }

    // when server gets request to exit p2p network - update tables for peer's shared files
    public void rcvExitReq(String data, String ip, String port){
        try {
           
            ui.jTextArea1.append("SERVER - Removing user from p2p\n");
            directory.fireTableDataChanged();
            int rows = directory.getRowCount();
            for (int r = 0; r < rows; r++){
                if (!(directory.getValueAt(r,2).toString()).equalsIgnoreCase(ip) && !(directory.getValueAt(r,3).toString()).equalsIgnoreCase(port)){
                   String[] row = {directory.getValueAt(r,0).toString(),
                       directory.getValueAt(r,1).toString(),
                       directory.getValueAt(r,2).toString(),
                       directory.getValueAt(r,3).toString()};
                    temp.addRow(row);
                }
            }
            this.updateTableModel(directory, temp);
            this.ui.refreshDir(directory);
            rmvClient(ip,port);
            exitResponse();
        }
        catch(Exception e){
          
            ui.jTextArea1.append("SERVER - ERROR: Couldn't remove peer.\n");
            informAndUpdateResponse("400");
        }
    }

    // SERVER RESPONSES
    // server tells client results of query
    public void clientSearchResponse(String results){
        try {
        
             ui.jTextArea1.append("SERVER - Sending search results to peer.\n");
            byte[] dataByteArray = new byte[results.length() + 3]; // pad for header

            System.arraycopy("300".getBytes(),0,dataByteArray,0,"300".getBytes().length); // add 300 for query code
            System.arraycopy(results.getBytes(), 0, dataByteArray, 3, results.getBytes().length);
            this.sender.rdtSend(dataByteArray);
        }
        catch(Exception e){
            
             ui.jTextArea1.append("SERVER - ERROR: Could not sent search results.\n");
            informAndUpdateResponse("400");
        }
    }

    // server tells client it has updated its directory to include shared files
    public void informAndUpdateResponse(String status){
        try {
          
             ui.jTextArea1.append("SERVER - Directory updated with peer's shared files.\n");
            String server = "Server Error";
            if (status.equalsIgnoreCase("200")) {
                server = "Server OK";
            }

            byte[] dataByteArray = new byte[server.length() + 3]; // pad header
            System.arraycopy(status.getBytes(),0,dataByteArray,0,status.getBytes().length); // add 200 status code
            System.arraycopy(server.getBytes(),0,dataByteArray,3,server.getBytes().length);

            this.sender.rdtSend(dataByteArray);
        }
        catch(Exception e){
         
             ui.jTextArea1.append("SERVER - ERROR: Directory failed to update.\n");
        }
    }

    // server tells client that it is ok to leavve peer to peer network
    public void exitResponse(){
        try {
       
             ui.jTextArea1.append("SERVER - Telling peer it can successfully leave network.\n");
            String exit = "Exit OK";
            byte[] dataByteArray = new byte[exit.length() + 3];

            System.arraycopy("100".getBytes(),0,dataByteArray,0,"100".getBytes().length);
            System.arraycopy(exit.getBytes(), 0, dataByteArray, 3, exit.getBytes().length);
            this.sender.rdtSend(dataByteArray);
        }
        catch(Exception e){
       
             ui.jTextArea1.append("SERVER - ERROR: Peer not succesfully leaving network\n");
            informAndUpdateResponse("400");
        }
    }


    @Override public void breakMessage(String status, String data, String ip, String port) throws Exception {
        this.updateSender(ip,port); //update the sender info so that we send to the address that we just got a message from
        switch(status) {
            case "001":
                rcvInformAndUpdate(data,ip,port);
                break;
            case "002":
                rcvClientSearchReq(data,ip,port);
                break;
            case "003":
                rcvDownloadReq(data,ip,port);
                break;
            case "004":
                rcvExitReq(data,ip,port);
                break;
        }
    }
}
