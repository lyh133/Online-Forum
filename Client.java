import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

        static DataOutputStream outToServer;
        static BufferedReader inFromUser;
        static DataInputStream inFromServer;
        static String serverMessage;
        static String command_string;
        static String username;
        static Socket clientSocket;

    public static void main(String[] args) throws Exception {

        //connect to server
        int port = Integer.parseInt(args[1]);
        clientSocket = new Socket(args[0], port);

        outToServer = 
            new DataOutputStream(clientSocket.getOutputStream());
		inFromUser =
            new BufferedReader(new InputStreamReader(System.in));
        inFromServer = 
            new DataInputStream(clientSocket.getInputStream());
        
        //init a new thread to check if server is On, if not then exit client
        checkServer(args[0],port);

        //tries to authorise untill a successful one
        while(!handleAuth());

        
        System.out.println("Welcome to the forum");
        //handles user command
        while(true){
            handleCommand();
        }

    }

    private static void handleCommand()throws Exception{
        String[] command;
        System.out.print("Enter one of the following commands: CRT, MSG, DLT, EDT, LST, RDT, UPD, DWN, RMV, XIT, SHT: ");
        command_string=inFromUser.readLine();
        command=command_string.split(" ");
        
        switch(command[0]){
            case "CRT":
                handleCRT(command);
                break;
            case "DLT":
                handleDLT(command);
                break;     
            case "MSG":
                handleMSG(command);
                break;
            case "EDT":
                handleEDT(command);
                break;
            case "LST":
                handleLST(command);
                break;
            case "RDT":
                handleRDT(command);
                break;
            case "UPD":
                handleUPD(command);
                break;
            case "DWN":
                handleDWN(command);
                break;
            case "RMV":
                handleRMV(command);
                break;
            case "XIT":
                handleXIT(command);
                break;
            case "SHT":
                handleSHT(command);
                break;
            default:
                System.out.println("Invalid command");
        }
    }

    private static boolean handleAuth() throws Exception{

        //get username and password
        String password;
        System.out.print("Enter username: ");
        username = inFromUser.readLine();
        //send username to server 
        outToServer.writeUTF(username);


        //recieve server response    
        serverMessage = inFromServer.readUTF();



        if(serverMessage.equals("Username OK")){
            //get password from user and send it to server
            System.out.print("Enter password: ");
            password = inFromUser.readLine();
            outToServer.writeUTF(password);
            //get AUTH response from server
            serverMessage = inFromServer.readUTF();


            if(serverMessage.equals("AUTH OK")){
                System.out.println("You have logged on");
            }else if(serverMessage.equals("AUTH FAIL")){
                System.out.println("Invalid Username/Password combination");
                return false;
            }

        }else if(serverMessage.equals("Invalid Username")){
            System.out.println("Username does not exist");
            System.out.print("Enter a password to make a new account with this username: ");
            password = inFromUser.readLine();
            outToServer.writeUTF(password);
            
            serverMessage = inFromServer.readUTF();
            if(serverMessage.equals("AUTH OK")){
                System.out.println("Account created and logged on");
            }


        }else if (serverMessage.equals("Fail: already active")){
            System.out.println("Username already logged on");
            return false;
        }

        return true;
    }
    private static void handleCRT(String[] command) throws Exception{
    
        if(command.length != 2){
            System.out.println("Incorrect syntax for CRT. Usage: 'CRT threadtitle'");
            return;
        }
        //write the string appended by username to server
        outToServer.writeUTF(command_string+" "+username);

        //read response and print out information
        serverMessage = inFromServer.readUTF();
        if(serverMessage.equals("CRT FAIL")){
            System.out.println("CRT failed, thread with name '"+command[1]+"' already Exist.");
            return;
        }else{
            System.out.println("Thread "+command[1]+" created");
        }

    }
    private static void handleMSG(String[] command) throws Exception{

        if(command.length < 3){
            System.out.println("Incorrect syntax for MSG. Usage: 'MSG threadtitle message'");
            return;
        }
        //write the string appended by username to server
        outToServer.writeUTF(command_string+" "+username);

        serverMessage =  inFromServer.readUTF();
        if(serverMessage.equals("MSG FAIL")){
            System.out.println("MSG failed, no thread named '"+command[1]+"' exist");
            return;
        }else{
            System.out.println("Message posted to "+command[1]+" thread");
        }

    }
    private static void handleDLT(String[] command) throws Exception{

        if(command.length != 3){
            System.out.println("Incorrect syntax for DLT. Usage: 'DLT threadtitle messageNumber'");
            return;
        }
        //write the string appended by username to server
        outToServer.writeUTF(command_string+" "+username);
        serverMessage =  inFromServer.readUTF();
        if(serverMessage.equals("DLT FILE FAIL")){
            System.out.println("DLT failed. Thread name don't exist");
            return;
        }
        if(serverMessage.equals("DLT MN FAIL")){
            System.out.println("DLT failed. Message number invalid");
            return;
        }
        if(serverMessage.equals("DLT AUTH FAIL")){
            System.out.println("DLT failed. You are not the author of this message");
            return;
        }
        
        System.out.println("Message number "+command[2]+" deleted from thread "+command[1]);

    }
    private static void handleEDT(String[] command) throws Exception{
        if(command.length < 4){
            System.out.println("Incorrect syntax for EDT. Usage: 'EDT threadtitle messagenumber message'");
            return;
        }
        outToServer.writeUTF(command_string+" "+username);
        serverMessage =  inFromServer.readUTF();

        if(serverMessage.equals("EDT FILE FAIL")){
            System.out.println("EDT failed. Thread name don't exist");
            return;
        }
        if(serverMessage.equals("EDT MN FAIL")){
            System.out.println("EDT failed. Message number invalid");
            return;
        }
        if(serverMessage.equals("EDT AUTH FAIL")){
            System.out.println("EDT failed. You are not the author of this message");
            return;
        } 
        System.out.println("Message number "+command[2]+" updated from thread "+command[1]);
    }
    private static void handleLST(String[] command)throws Exception{

        if(command.length != 1){
            System.out.println("Incorrect syntax for LST. Usage: LST");
            return;
        }

        outToServer.writeUTF(command_string+" "+username);
        serverMessage = inFromServer.readUTF();

        if(serverMessage.equals("")){
            System.out.println("No threads to show");
            return;
        }
        System.out.println("The list of active threads:");
        for(String threads : serverMessage.split(" ")){
            System.out.println(threads);
        }
    }
    private static void handleRDT(String[] command)throws Exception{
        if(command.length != 2){
            System.out.println("Incorrect syntax for RDT. Usage: RDT threadtitle");
            return;
        }

        outToServer.writeUTF(command_string+" "+username);
        serverMessage = inFromServer.readUTF();

        if(serverMessage.equals("RDT FAIL")){
            System.out.println("RDT FAIL, thread name '"+command[1]+"' don't exist");
            return;
        }

        System.out.println(serverMessage);

    }
    private static void handleUPD(String[] command)throws Exception{
        if(command.length != 3){
            System.out.println("Incorrect syntax for UPD. Usage: UPD threadtitle filename");
            return;
        }
        outToServer.writeUTF(command_string+" "+username);
        serverMessage = inFromServer.readUTF();
        if(serverMessage.equals("UPD NO THREAD")){
            System.out.println("UPD ERROR: No such thread to upload to");
            return;
        }

        //upload file to server
        if(serverMessage.equals("UPD THREAD OK")){

            File myFile = new File(command[2]);
            byte[] mybytearray = new byte[(int) myFile.length()];
            //read file into bytes
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
            bis.read(mybytearray, 0, mybytearray.length);
            //write bytes to server
            OutputStream os = clientSocket.getOutputStream();
            os.write(mybytearray, 0, mybytearray.length);
            os.flush();
            bis.close();
        }
        System.out.println("uploaded"+command[2]+" to "+command[1]);
    }
    private static void handleDWN(String[] command)throws Exception{
        if(command.length != 3){
            System.out.println("Incorrect syntax for DWN. Usage: DWN threadtitle filename");
            return;
        }
        outToServer.writeUTF(command_string+" "+username);
        serverMessage = inFromServer.readUTF();
        if(serverMessage.equals("DWN NO THREAD")){
            System.out.println("DWN ERROR: No such thread to download from");
            return;
        }
        if(serverMessage.equals("DWN NO FILE")){
            System.out.println("DWN ERROR: thread "+command[1]+" does not contain "+command[2]);
            return;
        }
        //download from server
        if(serverMessage.equals("DWN THREAD FILE OK")){
            byte[] mybytearray = new byte[1024];
            InputStream is = clientSocket.getInputStream();
            FileOutputStream fos = new FileOutputStream(command[2]);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            //read into byte array
            int bytesRead = is.read(mybytearray, 0, mybytearray.length);
            //write bytes to file
            bos.write(mybytearray, 0, bytesRead);
            bos.close();
        }
        System.out.println("downloaded "+command[2]+" from "+command[1]);
    }

    private static void handleRMV(String[] command)throws Exception{

        if(command.length != 2){
            System.out.println("Incrorrect syntax for RMV. Usage: RMV threadtitle");
            return;
        }
        outToServer.writeUTF(command_string+" "+username);
        serverMessage = inFromServer.readUTF();
        if(serverMessage.equals("RMV NO THREAD")){
            System.out.println("RMV ERROR: No such thread to remove");
            return;
        }
        if(serverMessage.equals("RMV BAD AUTH")){
            System.out.println("RMV ERROR: You are not the author of the thread");
            return;
        }
        System.out.println("Thread "+ command[1]+" removed");

    }

    private static void handleXIT(String[] command)throws Exception{

        if(command.length != 1){
            System.out.println("Incorrect syntax for XIT. Usage: XIT");
            return;
        }
        outToServer.writeUTF(command_string+" "+username);
        clientSocket.close();
        System.out.println("Goodbye and have a nice day");
        System.exit(0);
    }
    private static void handleSHT(String[] command)throws Exception{

        if(command.length != 2){
            System.out.println("Incorrect syntax for SHT. Usage: SHT admin_password");
            return;
        }
        outToServer.writeUTF(command_string+" "+username);
        serverMessage = inFromServer.readUTF();
        if(serverMessage.equals("SHT BAD AUTH")){
            System.out.println("SHT ERROR, admin_password is incorrect");
            return;
        }

        System.out.println("Goodbye Server shutting down");
        System.exit(0);

    }

    //init a new thread to checks if the Server is on, if not then exit
    private static void checkServer(String address, int port){
        exitHandler eh = new exitHandler(address,port);
        eh.start();
    }


}

//checks if the Server is running every 1 second, if not then exit
class exitHandler extends Thread{

    int port;
    String address;
    public exitHandler(String address,int port){
        this.port=port;
        this.address=address;
    }

    public void run(){
        try {
            while(true){

                Socket s = new Socket(address,port);
                s.close();
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.out.println("Goodbye, Server shutting down !");
            System.exit(0);
        }


    }

}
