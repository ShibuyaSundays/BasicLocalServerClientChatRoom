package assignment6;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class ServerMain {
    private ArrayList<MyClient> listClients = new ArrayList<>();
    private Hashtable<String, String> privateGroups = new Hashtable<>();  //groupname, mods
    public ServerMain(){ }
    public static void main(String[] args){
        ServerMain server = new ServerMain();
        //server.start();
        try{
            ServerSocket ss=new ServerSocket(6666); //ss is server socket, s is client socket
            while(true){
                System.out.println("Awaiting Client Connection...");
                Socket s = ss.accept();
                System.out.println("Accepted connection from " + s);

                //DataInputStream dis=new DataInputStream(s.getInputStream());
                //String str=(String)dis.readUTF();
                //ZSystem.out.println("message= "+str);

                MyClient client = new MyClient(server, s);
                server.listClients.add(client);
                client.start();
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    public List<MyClient> getListClients(){
        return listClients;
    }
    public void deleteClient(MyClient myClient) {
        listClients.remove(myClient);
    }
    public boolean addPrivateGroup(String groupName, String moderator){
        if(!privateGroups.containsKey(groupName)) {
            privateGroups.put(groupName, moderator);
            return true;
        }
        return false;
    }
    public Hashtable<String, String> getPrivateGroups() {
        return privateGroups;
    }
}


