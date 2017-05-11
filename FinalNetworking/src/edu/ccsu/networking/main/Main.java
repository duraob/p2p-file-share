package edu.ccsu.networking.main;

import edu.ccsu.networking.udp.ReceiverUDP;
import edu.ccsu.networking.udp.SenderUDP;
import edu.ccsu.networking.ui.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.*;

/**
 * Create new StartUp in order to run GUI 
 * 
 * @author Ben and Khaled
 */
public class Main {
    public static void main(String[] args)
    {
        try
        {
            StartUp start = new StartUp();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
