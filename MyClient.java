package assignment6;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

class MyClient extends Thread{
    private final Socket s;
    //private final Server server;
    private final ServerMain server;
    private String username = null;
    private OutputStream oS;
    private HashSet<String> groupSet = new HashSet<>();
    private Hashtable<String, String> messageHistory = new Hashtable<>();//user/group, contents
    private String divider = "=====================================================\r\n";

    public MyClient(ServerMain server, Socket s){
        this.server = server;
        this.s = s;
    }
    @Override
    public void run(){
        try {
            handleClientSocket();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream iS = s.getInputStream();
        this.oS = s.getOutputStream();
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(iS));
        String line;
        String intro =  divider+
                        "Welcome to CHAT ROOM 3000! This is the commands page:\r\n"+
                        "login [username] --> login with your desired username\r\n"+
                        "logoff/quit --> logoff from server\r\n"+
                        "create ![group] --> create a private group.\r\n" +
                        "You will be the moderator of the group.\r\n"+
                        "join !/#[group] --> join a private/public group \r\n"+
                        "leave !/#[group] --> leave a private/public group \r\n"+
                        "message [user] [text] --> message specific person \r\n"+
                        "message !/#[group] [text] --> message specific group\r\n"+
                        "history [user] --> open chat history with a user \r\n"+
                        "history !/#[group] --> open chat history with a\r\n"+
                        "private/public group\r\n"+
                        "help --> accesses this commands pages again\r\n"+
                        "Happy Chatting!\r\n"+
                        divider;
        oS.write(intro.getBytes());
        int flag = 1;
        while(flag == 1){
            oS.write("Please log in with a username: ".getBytes());
            String input = lineReader.readLine();
            if(input.length() < 1 || input.contains("!") || input.contains("#") || input.contains(" ")){
                oS.write("Invalid username; must be at least 1 character long\r\n".getBytes());
                oS.write("or cannot have special characters '!', '#' and ' '\r\n".getBytes());
            }
            else {
                this.username = input;
                String loginSuccess = "Welcome, " + username + ".\r\n";
                oS.write(loginSuccess.getBytes());
                System.out.println(username + " logged in.\r\n");
                List<MyClient> listClients = server.getListClients();
                for(MyClient client : listClients){
                    if(!username.equalsIgnoreCase(client.getUsername())&&client.getUsername()!=null){
                        send(client.getUsername() + " is online.\r\n");
                    }
                }
                for(MyClient client : listClients){
                    if(!username.equalsIgnoreCase(client.getUsername())){
                        client.send(username + " is logged in.\r\n");
                    }
                }
                flag = 0;
            }
        }
        while((line=lineReader.readLine()) != null){
            String[] arguments = line.split(" ");
            if(arguments != null && arguments.length > 0){
                String command = arguments[0];
                if("logoff".equalsIgnoreCase(command)||"quit".equalsIgnoreCase(line)){
                    clientLogoff(oS, arguments);
                    break;
                }
                //else if("login".equalsIgnoreCase(command)){
                //    clientLogin(oS, arguments);
                //}
                else if("message".equalsIgnoreCase(command)){
                    if(arguments.length > 2) {
                        String[] textArguments = line.split(" ", 3);
                        clientMessage(textArguments);
                    }
                    else
                        oS.write("To send message please provide a user/group and text\r\n".getBytes());
                }
                else if("create".equalsIgnoreCase(command)){
                    if(arguments.length > 1){
                        String group = arguments[1];
                        if(group.charAt(0) == '!'){
                            if(server.addPrivateGroup(group, username)){
                                String success = "You created the private group " + group + "\r\n";
                                oS.write(success.getBytes());
                                messageHistory.put(group, success);
                                groupSet.add(group);
                            }
                            else
                                oS.write("Group already taken. Please select another name\r\n".getBytes());
                        }
                        else{
                            oS.write(("Please create a group with a valid name. Must start with " +
                                    "'!'\r\n").getBytes());
                        }
                    }
                }
                else if("join".equalsIgnoreCase(command)){
                    if(arguments.length > 1){
                        String group = arguments[1];
                        if(group.charAt(0) == '!'){
                            List<MyClient> listClients = server.getListClients();
                            String moderator = server.getPrivateGroups().get(group);
                            for(MyClient client : listClients){
                                if(client.getUsername().equalsIgnoreCase(moderator)){
                                    oS.write("Request to join private group sent\r\n".getBytes());
                                    client.sendRequest(username, group); //send moderator your
                                    // request
                                }
                            }
                        }
                        else {
                            String texts = "You joined the public group " + group + "\r\n";
                            groupSet.add(group);
                            oS.write(texts.getBytes());
                            messageHistory.put(group, texts);
                        }
                    }
                }
                else if("leave".equalsIgnoreCase(command)){
                    if(arguments.length > 1){
                        String group = arguments[1];
                        groupSet.remove(group);
                        String texts = "You left the group " + group + "\r\n";
                        oS.write(texts.getBytes());
                        messageHistory.put(group, texts);
                    }
                }
                else if("history".equalsIgnoreCase(command)){
                    if(arguments.length == 2){
                        String clientHistory =  arguments[1];
                        if(messageHistory.containsKey(clientHistory)){
                            String historyOut = messageHistory.get(clientHistory);
                            oS.write((divider + historyOut + divider).getBytes());
                        }
                        else
                            oS.write("Cannot find user/group in history\r\n".getBytes());
                    }
                }
                else if("request".equalsIgnoreCase(command)){
                    if(arguments.length == 4){
                        String requester = arguments[1];
                        String requestedGroup = arguments[2];
                        String choice = arguments[3];
                        List<MyClient> listClients = server.getListClients();
                        if(choice.equalsIgnoreCase("Y")){
                            for(MyClient client : listClients){
                                if(client.getUsername().equalsIgnoreCase(requester)){
                                    String grant = "You are now in the private group " + requestedGroup + "\r\n";
                                    client.send(grant);
                                    client.messageHistory.put(requestedGroup, grant);
                                    client.groupSet.add(requestedGroup);
                                }
                            }
                        }
                        else if(choice.equalsIgnoreCase("N")){
                            for(MyClient client : listClients){
                                if(client.getUsername().equalsIgnoreCase(requester)){
                                    String grant = "Your request to join " + requestedGroup + " was denied\r\n";
                                    client.send(grant);
                                }
                            }
                        }
                    }
                }
                else if("help".equalsIgnoreCase(command)){
                    oS.write(intro.getBytes());
                }
                else{
                    String message = "Unknown Command: " + command + "\r\n";
                    //String message = "You typed: " + line + "\r\n";
                    oS.write(message.getBytes());
                }
            }
        }
        s.close();
    }

    private void sendRequest(String username, String group) throws IOException {
        String request = username + " would like to join your private group " + group + ". Do you" +
                " accept? Type 'request [user] [group] [Y/N]'\r\n";
        send(request);
    }

    private boolean isMemberOfGroup(String group){
        return groupSet.contains(group);
    }

    private void clientMessage(String[] arguments) throws IOException{
        String sendToUsername = arguments[1];
        String messageBody = arguments[2];

        if(arguments.length != 3)
            return;
        if(sendToUsername.charAt(0) == '#' && !isMemberOfGroup(sendToUsername)){
            String error = "Sorry, you are not a member of " + sendToUsername + ".\r\n";
            send(error);
            return;
        }
        if(sendToUsername.charAt(0) == '!' && !isMemberOfGroup(sendToUsername)){
            String error = "Sorry, you are not a member of the private group " + sendToUsername +
                    ".\r\n";
            send(error);
            return;
        }
        if(messageHistory.containsKey(sendToUsername)){
            String texts = messageHistory.get(sendToUsername);
            texts = texts + "You: " + messageBody + "\r\n";
            messageHistory.put(sendToUsername, texts);
        }
        else{
            String texts = "You: " + messageBody + "\r\n";
            messageHistory.put(sendToUsername, texts);
        }
        List<MyClient> listClients = server.getListClients();
        for(MyClient client : listClients){
            if(sendToUsername.charAt(0) == '#' || sendToUsername.charAt(0) == '!'){
                if(client.isMemberOfGroup(sendToUsername)){
                    String message = "In group " + sendToUsername + ", " + username + " says: "
                            + messageBody + "\r\n";
                    client.send(message);
                    if(!client.getUsername().equalsIgnoreCase(username)){
                        String texts = client.messageHistory.get(sendToUsername);
                        texts = texts + username + ": " + messageBody + "\r\n";
                        client.messageHistory.put(sendToUsername, texts);
                    }
                }
            }
            else if(client.getUsername().equalsIgnoreCase(sendToUsername)){
                String message = username + " says: " + messageBody + "\r\n";
                client.send(message);
                if(client.messageHistory.containsKey(username)){
                        String texts = client.messageHistory.get(username);
                        texts = texts + username + ": " + messageBody + "\r\n";
                        client.messageHistory.put(username, texts);
                }
                else{
                    String texts = username + ": " + messageBody + "\r\n";
                    client.messageHistory.put(username, texts);
                }
            }
        }
    }

    public String getUsername(){
        return username;
    }
    private void clientLogoff(OutputStream oS, String[] arguments) throws IOException{
        String message = "Logging off...\r\n";
        oS.write(message.getBytes());
        server.deleteClient(this);
        List<MyClient> listClients = server.getListClients();
        for(MyClient client : listClients){
            if(!username.equalsIgnoreCase(client.getUsername())){
                client.send(username + " has logged off.\r\n");
            }
        }
        System.out.println(username + " logged off.\r\n");
        s.close();

    }
    private void clientLogin(OutputStream oS, String[] arguments) throws IOException{
        if(arguments.length == 2){
            String username = arguments[1];
            //String password = arguments[2];
            String loginSuccess = "Welcome, " + username + ".\r\n";
            this.username = username;
            oS.write(loginSuccess.getBytes());
            System.out.println(username + " logged in.\r\n");

            List<MyClient> listClients = server.getListClients();
            for(MyClient client : listClients){
                if(!username.equalsIgnoreCase(client.getUsername())&&client.getUsername()!=null){
                    send(client.getUsername() + " is online.\r\n");
                }
            }
            for(MyClient client : listClients){
                if(!username.equalsIgnoreCase(client.getUsername())){
                    client.send(username + " is logged in.\r\n");
                }
            }
        }
    }

    private void send(String s) throws IOException{
        oS.write(s.getBytes());
    }
}
