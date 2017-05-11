package edu.ccsu.networking.udp;

import edu.ccsu.networking.main.recMessages;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import edu.ccsu.networking.ui.ServerMenu;
import edu.ccsu.networking.ui.ClientMenu;

/**
 * Simple receiver thread that starts up and endlessly listens for packets on
 * the specified and delivers them. Recall this simple implementation does not
 * handle loss or corrupted packets.

 * 
 * Reworked to use RDT3.0.
 * 
 * @author Ben and Khaled
 */
public class ReceiverUDP implements Runnable {

    private int port; 
    private DatagramSocket receivingSocket = null;
    private String data = "";
    private int currentSeq = 0;
    private boolean open = true;
    public byte[] pktStatus = new byte[3];
    public String status = "";
    private int flag;
    private boolean slowMode = false;    
    private recMessages peer;
    private ClientMenu uic;
    private ServerMenu uis;
    private int caller;
    
    /*
    public ReceiverUDP(ClientMenu ui)
    {
        this.uic = ui;
        this.caller = 1;
    }
    
    public ReceiverUDP(ServerMenu ui)
    {
        this.uis = ui;
        this.caller = 0;
    }
    */
    
    
    public ReceiverUDP(recMessages peer,ClientMenu ui ) {
        this.peer = peer;
        this.uic = ui;
        this.caller = 1;
        
    }
    
       public ReceiverUDP(recMessages peer,ServerMenu ui ) {
        this.peer = peer;
        this.uis = ui;
        this.caller = 0;
        
    }

    // Closes recievers Socket 
    public void stopListening() 
    {
        if (receivingSocket != null) {
            open = false;
            receivingSocket.close();
            //logStatus("RECEIVER - CLOSING!");
          
        }
    }
    
    // Delivers data to String or displays final result 
    public void deliverData(byte[] pkt, String address, String port) 
    {
        flag = pkt[1];
        status = new String(pktStatus);
        
        if(flag == 0)
        {
            pkt = Arrays.copyOfRange(pkt, 5, pkt.length);
            data += new String(pkt);
            logStatus("\n\nRECEIVER - FINAL: " + data + "\n");
            logStatus("RECEIVER - Status: " + status + " With Flag: " + flag);
            this.currentSeq = 0;
            try
            {
                this.peer.breakMessage(status, data, address, port);
            }
            catch(Exception e)
            {
                logStatus("RECEIVER - ERROR: MESSAGE NOT BROKEN UP");
            }
            data = "";
        }
        else
        {
            pkt = Arrays.copyOfRange(pkt, 5, pkt.length);
            logStatus("RECEIVER - Delivered with: " + new String(pkt));
            data += new String(pkt);
        }
    }
    
    public boolean checkPacketSeq(DatagramPacket pkt)
    {
        byte[] pktData = pkt.getData();
        int seq = (int)pktData[0];
        if(seq == currentSeq)
        {
            logStatus("RECEIVER - CORRECT SEQUENCE");
            return true;
        }
        logStatus("RECEIVER - ERROR: WRONG SEQUENCE");
        logStatus("RECEIVER - EXPECTED: " + currentSeq + " RECEIVED: " + seq);
       
        return false;
    }


    public void run() {
        try
        {
            receivingSocket = new DatagramSocket(port);
            logStatus("RECEIVER - SOCKET PORT: " + port);
        }
        catch(Exception e)
        {
            logStatus("RECEIVER - FAIL TO OPEN SOCKET");
        }
        while(open)
        {
            logStatus("RECEIVER - WAITING...");

            byte[] buffer = new byte[128]; // MTU
            DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
            try
            {
                receivingSocket.receive(pkt);
            }
            catch(Exception e)
            {
                logStatus("RECEIVER - PACKET NOT RECEIVED");
            }
            int size = pkt.getLength();

            if(checkPacketSeq(pkt))
            {
                logStatus("RECEIVER - RECEIVED WITH LENGTH: " + size + " bytes.");
                
                this.pktStatus[0] = pkt.getData()[2];
                this.pktStatus[1] = pkt.getData()[3];
                this.pktStatus[2] = pkt.getData()[4];
                logStatus("RECEIVER - PACKET 0: " + pkt.getData()[0]); // seq
                logStatus("RECEIVER - PACKET 1: " + pkt.getData()[1]); // flag 
                logStatus("RECEIVER - PACKET 2: " + new String(pktStatus) ); // status code
                
                byte[] pktData = Arrays.copyOfRange(pkt.getData(), 0, size);
                String pktIP = pkt.getAddress().getHostAddress();
                String pktPort = Integer.toString(pkt.getPort() );

                byte[] seq = {(byte)currentSeq};
                DatagramPacket ack = new DatagramPacket(seq, seq.length, pkt.getAddress(), pkt.getPort());
                try 
                {
                    logStatus("RECEIVER - ACK " + currentSeq + " to IP Address: " + pkt.getAddress() + " at Port: " + pkt.getPort());
                    receivingSocket.send(ack);
                }
                catch(Exception e)
                {
                    logStatus("RECEIVER - ERROR: ACK NOT SENT!");
                }
                
                this.currentSeq = (this.currentSeq ^ 1);
                deliverData(pktData, pktIP, pktPort);
            }
            else
            {
                byte[] seq = {(byte)(currentSeq ^ 1)};
                DatagramPacket ack = new DatagramPacket(seq, seq.length, pkt.getAddress(), pkt.getPort());
                try
                {
                    if(slowMode)
                        Thread.sleep(4000);
                    receivingSocket.send(ack);
                    logStatus("RECEIVER - SENDING ACK: " + (currentSeq ^1) + " TO IP: " + pkt.getAddress().getHostAddress() + " PORT: " + pkt.getPort());
                }
                catch(Exception e)
                {
                    logStatus("RECEIVER - ERROR: ACK NOT SENT!");
                }
            }
        }
    }
     // SETTERS
    public void setPortNum(int port)
    {
        this.port = port;
        logStatus("RECEIVER - PORT: " + this.port);
    }
    
    // SLOW MODE
    public void setSlowMode(boolean slow)
    {
        this.slowMode = slow;
        logStatus("RECEIVER - SLOW MODE SET TO " + this.slowMode);
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
