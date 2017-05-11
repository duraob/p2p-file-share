/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ccsu.networking.main;

/**
 *
 * @author admin
 */
public interface recMessages {
    public void breakMessage(String method, String data, String ip, String port) throws Exception;
}
