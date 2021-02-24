import java.io.*;
import java.net.*;
import java.util.*;


public class Server {



    public static String CREDENTIAL_FILE = "credentials.txt";
    public static HashMap<String,String> credentials = new HashMap<String,String>();
    public static HashMap<String, Integer> thread_metadata = new HashMap<String,Integer>();
    public static Set<String> activeUsers = new HashSet<String>();
    public static String adminPassword; 
    public static ServerSocket serverSocket;
    //this is thread of execution, not forum threads
    public static ArrayList<Thread> threads = new ArrayList<Thread>();
    public static void main(String[] args) throws Exception{
        
        int port = Integer.parseInt(args[0]);
        serverSocket = new ServerSocket(port);
        adminPassword=args[1];

        serverInit();
        System.out.println("Waiting for clients");
        while(true){

            
            try {

                Socket connectionSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(connectionSocket);
                threads.add(clientHandler);
                clientHandler.start();
            } catch (Exception e) {
                return;
            }


        }

    }

    private static void serverInit(){
        //scan credentials into hashmap
        try{
            Scanner credential_scanner
                = new Scanner(new File(CREDENTIAL_FILE));
            while(credential_scanner.hasNextLine()){
                String[] line = credential_scanner.nextLine().split(" ");
                credentials.put(line[0], line[1]);
            }
            credential_scanner.close();
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }

    }


}

class ClientHandler extends Thread{

    DataInputStream in;
    DataOutputStream out;
    Socket socket;

    public ClientHandler(Socket socket){
        try {
            this.socket=socket;
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

        } catch (Exception e) {

        }
    }

    public void run(){

        try {
            //tries to authenticate until a successful one
            while(!authenticate());
            System.out.println("Client connected");

            while(!Thread.currentThread().isInterrupted()){
                try{
                    handleCommand();
                } catch (InterruptedException e){
                    return;
                }
                
            }

        } catch (Exception e) {
            return;
        }


    }

    public boolean authenticate() throws Exception{
        String username = in.readUTF();
        String password;

        //check if user already logged on
        if(Server.activeUsers.contains(username)){
            out.writeUTF("Fail: already active");
            return false;
        }


        //check if username exist
        if(Server.credentials.containsKey(username)){

            out.writeUTF("Username OK");
            //check if username/password pair exist in the hashmap
            password = in.readUTF();
            if(Server.credentials.get(username).equals(password)){
                out.writeUTF("AUTH OK");
            }else{//if wrong password then return false signaling auth failed
                out.writeUTF("AUTH FAIL");
                return false;
            }

        }else{//create new account if username invalid
            out.writeUTF("Invalid Username");
            password = in.readUTF();
            createAccount(username, password);
            out.writeUTF("AUTH OK");
        }
        Server.activeUsers.add(username);
        System.out.println(username+" successful login");
        return true;
    }
    public void createAccount(String username,String password){
        try {
            //open credential file in append mode
            BufferedWriter bw = new BufferedWriter(new FileWriter(Server.CREDENTIAL_FILE, true));
            //write new record
            bw.write(username + " " + password);
            bw.newLine();
            bw.close();

            //create new hashmap entry
            Server.credentials.put(username, password);


        } catch (Exception e) {
            System.out.println("FILE ERROR");
        }


    }

    public void handleCommand() throws Exception{
        String[] command;
        try {
            command = in.readUTF().split(" ");
        } catch (Exception e) {
            return;
        }
        String username =command[command.length-1];
        switch(command[0]){
            case "CRT":
                createThread(command[1], username);
                break;
            case "DLT":
                deleteMessage(command[1], command[2], username);
                break;
            case "MSG":
                postMessage(command[1], getMessage(command,2), username);
                break;
            case "EDT":
                editMessage(command[1], command[2],getMessage(command, 3),username);
                break;
            case "LST":
                listThreads(username);
                break;
            case "RDT":
                readThread(command[1], username);
                break;
            case "UPD":
                uploadThread(command[1],command[2], username);
                break;
            case "DWN":
                downloadThread(command[1],command[2], username);
                break;
            case "RMV":
                removeThread(command[1], username);
                break;
            case "XIT":
                exitClient(username);
                break;
            case "SHT":
                shutDown(command[1],username);
        }

    }

    public void createThread(String threadTitle, String username) throws Exception{
        System.out.println(username+" issued CRT command");

        //fail if file already exist
        if(fileExist(threadTitle)){
            out.writeUTF("CRT FAIL");
            System.out.println("CRT failed. Thread name already exist");
            return;
        }
        // create file and write the username
        PrintWriter writer = new PrintWriter(threadTitle+".txt", "UTF-8");
        writer.println(username);
        writer.close();
        // initialise metadata
        Server.thread_metadata.put(threadTitle, 0);

        out.writeUTF("CRT SUCCESS");
        System.out.println("Thread "+threadTitle+" created");
    }

    public void postMessage(String threadTitle, String message, String username)throws Exception{
        System.out.println(username+" issued MSG command");
        // fail if thread dont exist
        if(!fileExist(threadTitle)){
            out.writeUTF("MSG FAIL");
            System.out.println("MSG failed. Thread name don't exist");
            return;
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(threadTitle+".txt", true));
        // increase message number by one
        Integer messageNumber = Server.thread_metadata.get(threadTitle) +1; 
        Server.thread_metadata.put(threadTitle, messageNumber);
        //write the message and message number to thread file
        bw.write(Integer.toString(messageNumber)+" "+username+": "+message);
        bw.newLine();
        bw.close();

        out.writeUTF("MSG SUCCESS");
        System.out.println("Message posted to "+threadTitle+" thread");
    }

    public void deleteMessage(String threadTitle, String messageNumber, String username) throws Exception{
        System.out.println(username+" issued DLT command");   
        // fail if thread dont exist 
        if(!fileExist(threadTitle)){
            out.writeUTF("DLT FILE FAIL");
            System.out.println("DLT failed. Thread name don't exist");
            return;
        }
        // fail if messageNumber invalid
        if( (Integer.parseInt(messageNumber) > Server.thread_metadata.get(threadTitle))  
        || (Integer.parseInt(messageNumber) < 1) ){
            out.writeUTF("DLT MN FAIL");
            System.out.println("DLT failed. Message number invalid");
            return;
        }
        //fail if thread owner is not user
        if(!isMessageOwner(username, threadTitle,messageNumber)){
            out.writeUTF("DLT AUTH FAIL");
            System.out.println("DLT failed. "+username+" is not the author of this message");
            return;
        }
        //actually does the dirty work of deleting message
        deleteMessageNumber(threadTitle, messageNumber);

        out.writeUTF("DLT SUCCESS");
        System.out.println("Message number "+messageNumber+" deleted from thread "+threadTitle);
    }

    public void editMessage(String threadTitle, String messageNumber,String newMessage,String username) throws Exception{
        System.out.println(username+" issued EDT command");   
        // fail if thread dont exist 
        if(!fileExist(threadTitle)){
            out.writeUTF("EDT FILE FAIL");
            System.out.println("EDT failed. Thread name don't exist");
            return;
        }
        // fail if messageNumber invalid
        if( (Integer.parseInt(messageNumber) > Server.thread_metadata.get(threadTitle))  
        || (Integer.parseInt(messageNumber) < 1) ){
            out.writeUTF("EDT MN FAIL");
            System.out.println("EDT failed. Message number invalid");
            return;
        }
        //fail if thread owner is not user
        if(!isMessageOwner(username, threadTitle,messageNumber)){
            out.writeUTF("EDT AUTH FAIL");
            System.out.println("EDT failed. "+username+" is not the author of this message");
            return;
        }
        //actually does the dirty work
        editMessageNumber(threadTitle,messageNumber,newMessage,username);
        out.writeUTF("EDT SUCCESS");
        System.out.println("Message number "+messageNumber+" updated from thread "+threadTitle);
    }

    public void listThreads(String username) throws Exception{
        System.out.println(username+" issued LST command"); 
        String response ="";
        for(String key : Server.thread_metadata.keySet() ){
            response = response + key+" ";
        }
        out.writeUTF(response);
    }

    public void readThread(String threadTitle, String username) throws Exception{
        System.out.println(username+" issued RDT command"); 
        if(!Server.thread_metadata.containsKey(threadTitle)){
            out.writeUTF("RDT FAIL");
            return;
        }
        FileInputStream fis=new FileInputStream(threadTitle+".txt");  
        Scanner sc=new Scanner(fis);
        StringBuffer buffer = new StringBuffer();
        String curr;
        boolean firsttime = true;
        while(sc.hasNextLine()){
            curr=sc.nextLine();
            if(!firsttime){
                buffer.append(curr+System.lineSeparator());
            }
            firsttime=false;
        }
        sc.close();
        String content =buffer.toString();
        out.writeUTF(content);
    }


    public void uploadThread(String threadTitle, String filename, String username)throws Exception{
        System.out.println(username+" issued UPD command"); 
        if(!Server.thread_metadata.containsKey(threadTitle)){
            out.writeUTF("UPD NO THREAD");
            return;
        }
        out.writeUTF("UPD THREAD OK");
        
        byte[] mybytearray = new byte[1024];
        InputStream is = socket.getInputStream();
        FileOutputStream fos = new FileOutputStream(threadTitle+"-"+filename);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        //write the bytes recieved into file
        int bytesRead = is.read(mybytearray, 0, mybytearray.length);
        bos.write(mybytearray, 0, bytesRead);
        bos.close();
        //update message in the thread
        BufferedWriter bw = new BufferedWriter(new FileWriter(threadTitle+".txt", true));
        bw.write(username+" uploaded "+filename);
        bw.newLine();
        bw.close();
        System.out.println(username+" uploaded "+filename+" to "+threadTitle);
    }

    public void downloadThread(String threadTitle, String filename, String username)throws Exception{
        System.out.println(username+" issued DWN command"); 
        if(!Server.thread_metadata.containsKey(threadTitle)){
            out.writeUTF("DWN NO THREAD");
            return;
        }
        //check if file was uploaded to thread
        if(!fileinThread(threadTitle, filename)){
            out.writeUTF("DWN NO FILE");
            return;
        }
        out.writeUTF("DWN THREAD FILE OK");
        //upload to client
        File myFile = new File(threadTitle+"-"+filename);
        byte[] mybytearray = new byte[(int) myFile.length()];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
        //read file into bytes array
        bis.read(mybytearray, 0, mybytearray.length);
        OutputStream os = socket.getOutputStream();
        //write bytearray to client
        os.write(mybytearray, 0, mybytearray.length);
        os.flush();
        bis.close();
        System.out.println("uploaded "+filename+" from "+threadTitle+" to "+username);
    }

    public void removeThread(String threadTitle, String username) throws Exception{
        System.out.println(username+" issued RMV command"); 
        if(!Server.thread_metadata.containsKey(threadTitle)){
            out.writeUTF("RMV NO THREAD");
            return;
        }
        if(!isOwner(username,threadTitle)){
            out.writeUTF("RMV BAD AUTH");
            return;
        } 
        //actual function that does the dirty work
        deleteThread(threadTitle);
        //remove metadata
        Server.thread_metadata.remove(threadTitle);

        out.writeUTF("RMV THREAD OK");
        System.out.println("Thread "+ threadTitle+" removed");

    }

    public void exitClient(String username) throws Exception{
        System.out.println(username+" has logged out");
        Server.activeUsers.remove(username);
    }

    public void shutDown(String adminpass, String username) throws Exception{
        System.out.println(username+" issued SHT command");
        if(!Server.adminPassword.equals(adminpass)){
            out.writeUTF("SHT BAD AUTH");
            return;
        }

        for(Thread t :Server.threads){
            t.interrupt();
        }

        // delete all thread related objects and threads
        for(String threadname: Server.thread_metadata.keySet()){
            deleteThread(threadname);
        }
        //delete credential file
        File creds = new File("credentials.txt");
        creds.delete();

        out.writeUTF("SHT SUCCESS");
        System.out.println("Server shutting down");
        Server.serverSocket.close();
        System.exit(0);


    }



//////////////////////////////helpers////////////////////////////////////////

    //was file uploaded in thread
    private boolean fileinThread(String threadTitle, String filename) throws Exception{

        FileInputStream fis=new FileInputStream(threadTitle+".txt");  
        Scanner sc=new Scanner(fis);
        String list[];
        while(sc.hasNextLine()){
            list=sc.nextLine().split(" ");

            if(list.length < 3) continue;

            if(list[2].equals(filename) && list[1].equals("uploaded")){
                sc.close();
                return true;
            }
        }

        sc.close();
        return false;


    }

    private boolean isOwner(String username,String threadTitle) throws Exception{
        FileInputStream fis=new FileInputStream(threadTitle+".txt");  
        Scanner sc=new Scanner(fis);
        boolean result = sc.nextLine().equals(username);
        sc.close();
        return result;
    }
    private boolean isMessageOwner(String username,String threadTitle,String messageNumber) throws Exception{
        FileInputStream fis=new FileInputStream(threadTitle+".txt");  
        Scanner sc=new Scanner(fis);
        String[] curr;
        while(sc.hasNextLine()){
            curr = sc.nextLine().split(" ");
            if(curr[0].equals(messageNumber)){
                if(curr[1].equals(username+":")){
                    sc.close();
                    return true;
                }
            }
        }
        sc.close();
        return false;
    }

    private void deleteMessageNumber(String threadTitle, String messageNumber) throws Exception{
        FileInputStream fis=new FileInputStream(threadTitle+".txt");  
        Scanner sc=new Scanner(fis);
        PrintWriter writer = new PrintWriter(threadTitle+"temp0.txt", "UTF-8");
        File originalThread = new File(threadTitle+".txt");
        File newThread = new File(threadTitle+"temp0.txt");

        String[] currlist;
        String curr;
        boolean found=false;
        Integer new_number;
        
        while(sc.hasNextLine()){

            curr=sc.nextLine();
            currlist=curr.split(" ",2);
            //flag when the messageNumber to delete is found
            if(currlist[0].equals(messageNumber)){
                found = true;
            }
            //if the current line is not the one to delete and the line to delete was found
            if(found && !currlist[0].equals(messageNumber)){
                //write this line to the new temp file with line number adjusted
                new_number=Integer.parseInt(currlist[0])-1;
                writer.println(new_number.toString()+" "+currlist[1]);
            
            //if the current line is not the one to delete and the line to delete was not found
            }else if(!currlist[0].equals(messageNumber)){
                //write this line to new temp file untouched
                writer.println(curr);
            }

        }
        //replace old thread file with temp file
        originalThread.delete();
        File rename = new File(threadTitle+".txt");
        newThread.renameTo(rename);
        //update metadata
        Server.thread_metadata.put(threadTitle, Server.thread_metadata.get(threadTitle)-1);
        writer.close();
        sc.close();
    }
    private void editMessageNumber(String threadTitle, String messageNumber,String newMessage,String username) throws Exception{
        System.out.println(threadTitle+" "+messageNumber+" "+ newMessage+" "+username);
        FileInputStream fis=new FileInputStream(threadTitle+".txt");  
        Scanner sc=new Scanner(fis);
        String oldline =null;
        String[] currlist;
        String curr;
        //making the valid message format
        String augMessage = messageNumber+" "+username+": "+newMessage;

        StringBuffer buffer = new StringBuffer();
        //append the file in buffer and search for the line to update
        while(sc.hasNextLine()){
            curr=sc.nextLine();
            buffer.append(curr+System.lineSeparator());
            currlist=curr.split(" ");
            //this is the line to update
            if(currlist[0].equals(messageNumber)){
                oldline=curr;
            }
        }

        //replace the oldline with the new message in the buffer using regex
        String content = buffer.toString();
        content = content.replaceAll(oldline, augMessage);
        sc.close();

        //flush the content of the buffer to the thread file
        String filelocation = System.getProperty("user.dir")+"/"+threadTitle+".txt";
        FileWriter writer = new FileWriter(filelocation);
        writer.append(content);
        writer.flush();
        writer.close();
    }

    private void deleteThread(String threadTitle) throws Exception{

        //delete thread related files
        String namesplit[];
        //search through directory to delete uploaded files
        File dir = new File(System.getProperty("user.dir"));
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                namesplit=child.getName().split("-");
                //if length < 2 means file name dont contain "-" therefore skip it
                if(namesplit.length < 2){
                    continue;
                }
                //check if a file is uploaded to that thread
                if(fileinThread(threadTitle, namesplit[1]) && namesplit[0].equals(threadTitle)){
                    child.delete();
                }
            }
        }
        //delete the thread
        File thread = new File(threadTitle+".txt");
        thread.delete();
    }


    private boolean fileExist(String fileName){
        File file = new File(System.getProperty("user.dir")+"/"+fileName+".txt");
        return file.exists();
    }
    private String getMessage(String[] command,int start){
        String message = "";
        for(int i=start; i < (command.length-1);i++){
            message = message+command[i]+" ";
        }
        return message;
    }

}