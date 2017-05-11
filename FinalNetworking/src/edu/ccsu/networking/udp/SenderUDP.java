package edu.ccsu.networking.udp;

import edu.ccsu.networking.ui.ServerMenu;
import edu.ccsu.networking.ui.ClientMenu;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
 

/**
 * Sender thread that receives data from above (Main.java) and makes a packet(s)
 * then senders the packet(s) to the receiver and waits for a response ACK.
 * @author Ben and Khaled
 */
public class SenderUDP extends Thread {
    private int recPortNum = 0;
    private int senderPortNum = 0;
    private DatagramSocket socket = null;
    private InetAddress tarAddress = null;
    private int currentSeq = 0;
    private boolean ACKrec = false;
    private long timeout = 100;
    private String status = "";
    private int flag = 1;
    private boolean slowMode = false;
    private ClientMenu uic;
    private ServerMenu uis;
    private int caller;
        
    
    public SenderUDP(ClientMenu ui)
    {
        this.uic = ui;
        this.caller = 1;
    }
    
      public SenderUDP(ServerMenu ui)
    {
        this.uis = ui;
        this.caller = 0;
    }

    public SenderUDP() {
    }

    // Creates socket for sender
    public void startSender() throws SocketException, UnknownHostException 
    {
    
        try { 
            socket = new DatagramSocket(senderPortNum); 
            logStatus("SENDER - PORT OPENED: " + senderPortNum + "\n");
        }
        catch(SocketException se) {
            logStatus("SENDER - ERROR: Socket not opened.");
        } 
    }
    
    public void stopSender(){
        if (socket!=null){
         logStatus("SENDER - Closed.");
            socket.close();
        }
    }
    
    // reliable data transfer - stripping status code from packet then sending data to server
    public void rdtSend(byte[] data) throws SocketException, IOException, InterruptedException {
        byte[] statusCodeArray = {data[0], data[1], data[2]};
        this.status = new String(statusCodeArray);
        logStatus("SENDER - STATUS: " + status);
        
        byte[] newData = new byte[data.length - 3]; // strip header
        try
        {
            System.arraycopy(data, 3, newData, 0, data.length-3);
        }
        catch(Exception e)
        {
           logStatus("SENDER - ERROR: " + e);
            System.exit(0);
        }
       
        ByteArrayInputStream byteStream = new ByteArrayInputStream(newData);

        while (byteStream.available()>0){
            byte[] packetData = makePacketData(byteStream); // data from input stream
            DatagramPacket pkt = makePacket(packetData); // makes packet 
            logStatus("SENDER - SLOW MODE: " + this.slowMode);
            
            sendPacket(pkt); 
        }
        currentSeq = 0;
        flag = 1; // more data to come
       logStatus("SENDER -  DONE SENDING!");
    }
    
    // TIMER
    public void adjustTimeout(long currentRTT)
    {
        double time = ((double)timeout * 0.8) + ((double)currentRTT * 0.2);
        timeout = (long)time;
    }
    
    // ACKNOWLEDGEMENTS
    public boolean checkACK(DatagramPacket ack)
    {
        int seq = (int)ack.getData()[0];
        logStatus("SENDER - ACK = " + seq + ". EXPECTED = " + currentSeq);
        if(seq == currentSeq)
        {
           logStatus("SENDER - EXPECTED ACK RECEIVED");
            currentSeq = (currentSeq ^ 1); // Change Sequence number: ^ is XOR so will either change to 0 or 1
            return true;
        }
       logStatus("SENDER - ERROR DOES NOT MATCH EXPECTED!");
        return false;
    }
    
    // Waits for reciever to send packet containing seq # and calls check ack 
    public void recACK(DatagramPacket packet) throws SocketException, IOException, InterruptedException
    {
        logStatus("SENDER - WAITING ON ACK");
        while(!ACKrec)
        {
            try
            {
                byte[] buffer = new byte[16];
                DatagramPacket ack = new DatagramPacket(buffer, buffer.length);
                socket.receive(ack);
                
                logStatus("SENDER - RECEIVED ACK " + (int)ack.getData()[0]);
                logStatus("SENDER - RECEIVED FROM: " + tarAddress + " PORT: " + recPortNum);
                
                if(checkACK(ack))
                {
                    ACKrec = true;
                }
                else
                {
                    ACKrec = false;
                    break;
                }
            }
            catch(NullPointerException e)
            {
                logStatus("SENDER - NULL ACK");
                ACKrec = false;
            }
        }
    }
    
    // Makes packet containing length of packet, seq num, and data from byte stream following
    public DatagramPacket makePacket(byte[] data)
    {
        
        byte[] pktData = new byte[data.length + 5];
        pktData[0] = (byte)currentSeq;
        pktData[1] = (byte)flag;
        byte[] methodArray = status.getBytes();
        System.arraycopy(methodArray, 0, pktData, 2, methodArray.length);
        System.arraycopy(data, 0, pktData, 5, data.length);
        logStatus("SENDER - Building Packet");
        logStatus("SENDER - Packet Size: " + pktData.length + " bytes.");
        logStatus("SENDER - Sequence Number: " + currentSeq);
        DatagramPacket packet = new DatagramPacket(pktData, pktData.length, tarAddress, recPortNum);
        
        return packet;
    }
    
    // Data from inputstream turned into array of bytes to create packet
    public byte[] makePacketData(ByteArrayInputStream byteStream) throws SocketException, IOException, InterruptedException
    {
        byte[] pktData = new byte[123];
        int bytesRead = byteStream.read(pktData);
        if(bytesRead<pktData.length)
        {
            flag = 0; // EOF
            pktData = Arrays.copyOf(pktData, bytesRead);
        }
        
        return pktData;
    }
    
    // SEND: Starts the timer and waits for ACK after sending the packet 
    public void sendPacket(DatagramPacket pkt) throws SocketException, IOException, InterruptedException
    {
        while(!ACKrec)
        {
            logStatus("SENDER - Sending Packet to Address: " + tarAddress + " Port: " + recPortNum);
            if(slowMode)
                Thread.sleep(4000);
            socket.send(pkt);
            long startTimer = System.currentTimeMillis();
            socket.setSoTimeout((int)timeout);
           logStatus("SENDER - TIMEOUT SET:" + timeout + " ms");
            try
            {
                recACK(pkt);
                long rtt = System.currentTimeMillis() - startTimer;
                adjustTimeout(rtt);
                logStatus("SENDER - RTT: " + rtt + " ms");
            }
            catch(SocketTimeoutException e)
            {
                logStatus("SENDER  - ERROR: TIMEOUT");
                ACKrec = false;
            }
        }
        ACKrec = false;
    }
    
    // SETTERS
    public void setPortNum(int port)
    {
        this.senderPortNum = port;
       logStatus("\nSENDER - Port Num: " + this.senderPortNum);
    }
    
    public void setTarPort(int port)
    {
        this.recPortNum = port;
        logStatus("SENDER - Target Port: " + this.recPortNum);
    }
    
    public void setTarIPAddress(InetAddress tarIP)
    {
        this.tarAddress = tarIP;
        logStatus("\nSENDER - Target Address: " + this.tarAddress);
    }
    
    public void setSenderPort(int port)
    {
        this.senderPortNum = port;
        logStatus("SENDER - Sender Port: " + this.senderPortNum);
    }
    
    // SLOW MODE
    public void startSlowMode(boolean slow)
    {
        this.slowMode = slow;
        logStatus("SENDER - SLOW MODE SET TO " + this.slowMode);
    }
    
       
    // print log status
    public void logStatus(String Str1)
    {
        if (caller == 1)
             uic.jTextArea1.append(Str1 + "\n");
        if (caller == 0)
            uis.jTextArea1.append(Str1 + "\n");
    }
}
