package edu.ccsu.networking.main;

import edu.ccsu.networking.udp.*;
import edu.ccsu.networking.ui.ClientMenu;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.net.*;
import java.util.Arrays;

/**
 *
 * @author Ben and Khaled
 */
public class Client implements recMessages {
    SenderUDP sender;
    ReceiverUDP receiver;
    String expectedMessage = "";

    private int targetPortNum;
    private int portNum;
    private InetAddress targetIP;
    private Thread receiverThread;
    private String[] connectionInfo;
    String[] columns = {"File Name", "File Size", "Host IP", "Host Port"};
    String[] localColumns = {"File Name", "File Size", "Location"};
    String[][] resultsData = {};
    DefaultTableModel searchResults = new DefaultTableModel(resultsData,columns);
    DefaultTableModel localTable = new DefaultTableModel(resultsData, localColumns);
    private String[] previousMessage = new String[4];

    ClientMenu ui;
    
    
    public Client(ClientMenu ui)
    {
        this.ui = ui;
    }
    
    // Star sender and setting up all variables 
    public void startSenderUDP(String targetIP, String targetPort, String clientPort) throws Exception{
        sender = new SenderUDP(ui);
        try {
            this.targetIP = Inet4Address.getByName(targetIP);
        }
        catch(Exception e){
          ui.jTextArea1.append("\nCLIENT - Cannot get IP\n");
        }

        this.targetPortNum = Integer.parseInt(targetPort);
        this.portNum = Integer.parseInt(clientPort) + 1000;

        sender.setTarIPAddress(this.targetIP);
        sender.setTarPort(this.targetPortNum);
        sender.setPortNum(this.portNum);
        sender.startSender();
        
        ui.jTextArea1.append("CLIENT - Client Started\n");
    }

    public void startReceiverUDP(String port){
        receiver = new ReceiverUDP(this, ui);
        receiver.setPortNum(Integer.parseInt(port));
        
        //Start a new Receiver for all the UDP messages that the Client will get from the Server
        receiverThread = new Thread(receiver);
        receiverThread.start();
       
        ui.jTextArea1.append("CLIENT - Receiver Thread Started\n");
    }

    // Slow Mode for packet loss
    public void slowMode(boolean slow){
        sender.startSlowMode(slow);
    }
    
    public void informAndUpdate(String dataToSend) throws Exception {
        //dataByteArray = 200#hostName(20 bytes- padded with spaces if < 20)#data$
        byte[] dataByteArray = new byte[dataToSend.getBytes().length+3];
        //Inform and Update message code == 200
        System.arraycopy("001".getBytes(),0,dataByteArray,0,"001".getBytes().length);
        System.arraycopy(dataToSend.getBytes(),0,dataByteArray,3,dataToSend.getBytes().length);
        //Send the packet to the Transportation layer
        this.expectedMessage = "200";
        sender.rdtSend(dataByteArray);
    }

    // Same thing as informAndUpdate but different codes 
    public void search(String key) throws Exception{
        byte[] dataByteArray = new byte[key.length()+3]; // pad for new header
        System.arraycopy("002".getBytes(),0,dataByteArray,0,"002".getBytes().length);
        System.arraycopy(key.getBytes(),0,dataByteArray,3,key.getBytes().length);
        this.expectedMessage = "300";
        sender.rdtSend(dataByteArray);
    }

    // Same thing as informAndUpdate but different codes 
    public void download(String fileReq) throws Exception{
        byte[] dataByteArray = new byte[fileReq.length()+3];
        System.arraycopy("003".getBytes(),0,dataByteArray,0,"003".getBytes().length);
        System.arraycopy(fileReq.getBytes(),0,dataByteArray,3,fileReq.getBytes().length);
        this.expectedMessage = "500";
        sender.rdtSend(dataByteArray);
    }

    // Same thing as informAndUpdate but different codes 
    public void exit() throws Exception{
        try {
            
            ui.jTextArea1.append("CLIENT - Leave p2p network\n");
            String exit = "Client Exit";
            byte[] dataByteArray = new byte[exit.length() + 3];
            System.arraycopy("004".getBytes(), 0, dataByteArray, 0, "004".getBytes().length);
            System.arraycopy(exit.getBytes(), 0, dataByteArray, 3, exit.getBytes().length);
            this.expectedMessage = "100";
            sender.rdtSend(dataByteArray);
        }
        catch(Exception e){
            
            ui.jTextArea1.append("CLIENT - ERROR: Failed to leave.\n");
        }
    }
    
    public void clearTableModel(DefaultTableModel tbl){
        if (tbl.getRowCount() > 0) {
            for (int i = tbl.getRowCount() - 1; i > -1; i--) {
                tbl.removeRow(i);
            }
        }
    }    

    // RESPONSES
    public void rcvServerOk(){
        
        ui.jTextArea1.append("CLIENT - Server OK!\n");
    }

    public void rcvServerErr(){
        
        ui.jTextArea1.append("CLIENT - Server Error!\n");
    }

    public void rcvSearchResponse(String data){
        try {
            clearTableModel(searchResults);
            String[] files = data.split("\\?"); //split by ? for rows to build file path
            for (String file : files) {
                String[] row = file.split("#");
                
                 ui.jTextArea1.append(row[0] + " " + row[1] +" " + row[2] + " " + row[3] + ".\n"); // prints out file info segments
                searchResults.addRow(row);
                searchResults.fireTableDataChanged();
            }
            this.ui.updateResults(searchResults);
        }
        catch(Exception e){
            
            ui.jTextArea1.append("CLIENT - ERROR: No search response.\n");
        }
    }
    
    // Server says it is ok to leave p2p network
    public void rcvExitResponse(){
        this.ui.hideWindow();
        System.exit(0);
    }    

    //TCP
    // sets up client as receiver for file
    public void startReceiverTCP(String data){
        
        try {
            //Thread.sleep(2000);
               //Initialize socket
            ui.jTextArea1.append("RECEIVER - Initialized!\n");
            String[] fileInfo = data.split("#");
            Socket socket = new Socket(fileInfo[2], 6002); //ip address
           ui.jTextArea1.append("RECEIVER - Socket started!\n");
            InputStream fromServer = socket.getInputStream(); // begin input from server
            ui.jTextArea1.append("RECEIVER - Input stream created from the socket!\n");

            String fileName = fileInfo[0]; // get file name
            
            File file = new File(fileName);
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
            
             ui.jTextArea1.append("RECEIVER - Output stream to the server created by the receiver!\n");
             ui.jTextArea1.append("RECEIVER - Output stream to a file created by the receiver!\n");
            
            int bytesRead = 0; 
            int total_bytes = 0;
            int packetNo = 0;

            byte[] buffer = new byte[128]; // buffer for our input stream

            while ((bytesRead = fromServer.read(buffer)) != -1) {
                buffer = Arrays.copyOf(buffer, bytesRead);
                output.write(buffer);
             ui.jTextArea1.append("RECEIVER - Read packet " + packetNo + "\n");
             ui.jTextArea1.append("RECEIVER - Read " + bytesRead + " bytes from the input stream!\n");
             total_bytes  = bytesRead + total_bytes;
             packetNo++;
            }
            
            ui.jTextArea1.append("Total KB read: " + total_bytes + " KB\n");
            ui.jTextArea1.append("File received and saved!\n");
            
            fromServer.close();
            output.flush();
            socket.close();
        }
        catch(Exception e){
           
            ui.jTextArea1.append("CLIENT - ERROR: Could not receive TCP file\n");
        }
        
        
        
        
        
    }
    // sets up client to send file to other peer that requested through download at server
        public void startSenderTCP(String fileToDownload)
        {
        
            try {
            ui.jTextArea1.append("TCP Sender is started!\n");
            ServerSocket serverSocket = new ServerSocket(6002);
            Socket clientSocket = serverSocket.accept();
            
            String[] fileInfo = getFileInfo(fileToDownload);
            File file = new File(fileInfo[2]); // location

            BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(file));
            OutputStream outStream = clientSocket.getOutputStream();
            int count = 0;
            
            byte[] ArrayContents;
            long fileLength = Long.parseLong(fileInfo[1]);
            long current = 0;
            int countPacket = 0;
            long packetSize;
            
            packetSize = fileLength / 128;
            
            while(current!=fileLength){ 
            int size = 128;
            if(fileLength - current >= size)
            {
                current += size;    
                countPacket++;    
            }
            else{ 
                size = (int)(fileLength - current); 
                current = fileLength;
            } 
            ArrayContents = new byte[size]; 
            fileInputStream.read(ArrayContents, 0, size); 
            outStream.write(ArrayContents);
            ui.jTextArea1.append("SENDER - Sending packet " + countPacket + " out of " + packetSize + ": \n");
            ui.jTextArea1.append("SENDER - Sending ... "+ current + " bytes .. \n");
            ui.jTextArea1.append("SENDER - Sending file ... "+(current*100)/fileLength+"% complete! \n");
        }   
            
            fileInputStream.close();
            outStream.close();
            outStream.flush();
            serverSocket.close();
            clientSocket.close();
             System.out.println("SENDER - File transferred!");
        }
        catch(Exception e){
            
            ui.jTextArea1.append("CLIENT - ERROR: Failed to establish TCP.\n");
        }
            
    }
    
    public void updateTableModel(DefaultTableModel newT){
        clearTableModel(this.localTable);
        for(int r = 0; r < newT.getRowCount(); r++){
            String[] tempData = {(newT.getValueAt(r,0).toString()),
                (newT.getValueAt(r,1).toString()),
                (newT.getValueAt(r,2).toString())};
            this.localTable.addRow(tempData);
        }
        this.localTable.fireTableDataChanged();
    }

    public String[] getFileInfo(String keyword){
        try {
            this.localTable.fireTableDataChanged();
            String fileName = keyword.split("#")[0];
            System.out.println(this.localTable.getRowCount());
            for (int r = 0; r < this.localTable.getRowCount(); r++) {
                if (this.localTable.getValueAt(r, 0).toString().equalsIgnoreCase(fileName)) {
                    String[] info = {(this.localTable.getValueAt(r, 0).toString()),
                        (this.localTable.getValueAt(r, 1).toString()),
                        (this.localTable.getValueAt(r, 2).toString())};
                    return info;
                }
            }
        }
        catch(Exception e){
            
            ui.jTextArea1.append("CLIENT - ERROR: Failed to get file info.\n");
            e.printStackTrace();
        }
        return null;
    }

    @Override public void breakMessage(String status, String data, String ip, String port) throws Exception{
        if(expectedStatusCode(status)){
            switch(status) {
                case "100":
                    rcvExitResponse();
                    break;
                case "200":
                    rcvServerOk();
                    break;
                case "400":
                    rcvServerErr();
                    break;
                case "300":
                    rcvSearchResponse(data);
                    break;
                case "500":
                    startReceiverTCP(data);
                    break;
                case "600":
                    startSenderTCP(data);
                    break;
            }
        }
        else{
          
            ui.jTextArea1.append("CLIENT - ERROR: Status Code Expected: " + this.expectedMessage + ", actual: " + status +"\n");

        }
    }
    
    // prompt for expected Status Code after we send request to server if not give an error code used in breakMessage
    private boolean expectedStatusCode(String status){

        if(status.equalsIgnoreCase("400") || status.equalsIgnoreCase("600")){
            return true;
        }
        else{
            return (status.equalsIgnoreCase(this.expectedMessage));
        }
    }
}