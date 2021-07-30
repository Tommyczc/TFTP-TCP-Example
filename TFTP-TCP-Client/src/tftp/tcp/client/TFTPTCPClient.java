/*
 * To change this license header, choose License Headers input Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template input the editor.
 */
package tftp.tcp.client;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TFTPTCPClient {

    private Socket echoSocket;
    private final String adress= "127.0.0.1";
    private final int portNumber=69;
    private final int maxSize=512+4;
    private OutputStream output;
    private InputStream input;
    private byte[] buffer;
    private static final byte RRQ = 1;                  //read the file
    private static final byte WRQ = 2;
    private static final byte DAT = 3;                  //file data                  
    private static final byte ERROR = 5;
    private BufferedReader stdIn;
    
    public void run () {
        
        try {
            buffer=new byte[maxSize];
            echoSocket = new Socket(adress, portNumber);
            output = echoSocket.getOutputStream();
            input = echoSocket.getInputStream();
            stdIn = new BufferedReader(new InputStreamReader(System.in));
            
            while (true) {

                System.out.println("Type 1 for write file, 2 for read file");
                String choice=stdIn.readLine();
                //System.output.println();
                if(choice.equals("1")){
                    byte[] request;
                    theList();
                    System.out.println("Please enter the file name:");
                    String fileName=stdIn.readLine();
                    if(fileExist(fileName)){
                        request=createRequest(WRQ, fileName);
                        output.write(request);
                        input.read(buffer);
                        if(buffer[1]==ERROR){
                            System.err.println("Error code: "+buffer[3]+" Error message: "+reportError(buffer[3]));
                        }
                        else{
                            byte[] content=findFile(fileName);
                            sendFile(content);
                        }
                    }
                    else{
                        System.err.println("the file not find, try again..");
                    }
                }
                
                else if(choice.equals("2")){
                    byte[] request;
                    System.out.println("Please enter the file name:");
                    String fileName=stdIn.readLine();
                    if(!fileExist(fileName)){
                        request=createRequest(RRQ, fileName);
                        output.write(request);
                        ByteArrayOutputStream byteOut = receiveFile();
                        writeFile(byteOut, fileName);
                        System.out.println("the file is saved");
                        System.out.println(new File("dictionary/"+fileName).toURI().toString());
                        System.out.println();
                    } 
                    else{
                        System.err.println("the file is exist: "+new File("dictionary/"+fileName).toURI().toString());
                    }
                }
                else{System.err.println("Wrong input: "+stdIn.readLine());}
            }
            
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + adress);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + adress + ". Is the server running?");
            System.exit(1);
        }
    }
    
    private void theList(){
        System.out.println();
        System.out.println("here are all files in client dictionary:");
        File file = new File("dictionary");
        String[] fileList = file.list();
        for(String fileName:fileList){
            System.out.println(fileName);
        }
        System.out.println();
    }
    
    private ByteArrayOutputStream receiveFile() throws IOException {
        ByteArrayOutputStream byteOutOS = new ByteArrayOutputStream();
        int block = 1;
         //set buffer area
         byte [] buf= new byte[maxSize];
         int len=0;
        do {
            len=input.read(buf);
            System.out.println("Have recived "+block+" pakage.");   
            block++;
            byte[] opCode = { buf[0], buf[1] }; 
            if (opCode[1] == ERROR) {
                System.err.println("Error code:"+buf[3]+" Error message:"+reportError(buf[3]));
            } 
            else if (opCode[1] == DAT ) {
                DataOutputStream dos = new DataOutputStream(byteOutOS);
                dos.write(buf,4,buf.length-4);
                //System.out.println(isLastPacket(buf));
                //if(isLastPacket(buf)){break;}
            }
        } 
        while (len==516);
        System.out.println("All file have been recived！！");
        return byteOutOS;
    } 
    
    private void writeFile(ByteArrayOutputStream b, String fileName){
            try {
                OutputStream outputStream = new FileOutputStream("dictionary/"+fileName);
                b.writeTo(outputStream);  // 将此 byte 数组输出流的全部内容写入到指定的输出流参数中
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    
    private void sendFile(byte[] content){
        int block=1;
        int position;
        byte zeroByte = 0;
        byte single = 1;
        byte tenth=0;
        int current=0;
        while(current+1!=content.length){
            position=0;
            if(content.length-current-1>512){
                
                int ByteLength = 512+4;//
                byte[] sended=new byte[ByteLength];
                sended[position]=zeroByte;
                position++;
                sended[position]=DAT;
                position++;
                sended[position]=tenth;
                position++;
                sended[position]=single;
                position++;
                for (int i = 0; i < 512; i++) {
                    sended[position] = content[current];
                    //System.output.println(position+"  "+current);
                    current++;
                    position++;
 
                }
                try {
                    output.write(sended);
                    //System.out.println(sended[511]);
                    System.out.println("sending " +block+" package");
                    block++;
                } catch (IOException ex) {
                    Logger.getLogger(TFTPTCPClient.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("Couldn't get I/O for the connection to " + adress + ". Is the server running?");
                }
                if(single==9){
                    single=0;
                    tenth++;
                }
                else{
                    single++;
                }
            }
                
            else{//while the left data is small than 512
                int ByteLength = content.length-current+4;//
                //System.output.println(ByteLength);
                byte[] sended=new byte[ByteLength];
                sended[position]=zeroByte;
                position++;
                sended[position]=DAT;
                position++;
                sended[position]=tenth;
                position++;
                sended[position]=single;
                position++;
                for (int i = 0; i < content.length-current; i++) {
                    sended[position] = content[i];  
                    //System.output.println(i+current);
                    position++;
                }
                current=content.length-1;
                //System.output.println(current+"  "+position);
                if(single==9){
                    single=0;
                    tenth++;
                }
                else{
                    single++;
                }
                try {
                    output.write(sended);
                    byte[] cancel=new byte[1];
                    output.write(cancel);
                    System.out.println("sending " +block+" package");
                    block++;
                } catch (IOException ex) {
                    Logger.getLogger(TFTPTCPClient.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("Couldn't get I/O for the connection to " + adress + ". Is the server running?");
                }
            }
        }
    }
    
    private byte[] createRequest(final byte opCode, final String fileName) 
    {
        final String mode="octet";
        byte zeroByte = 0;  
        int ByteLength = 2 + fileName.length() + 1 + mode.length() + 1; //
        byte[] ByteArray = new byte[ByteLength];
        int position = 0;      
        ByteArray[position] = zeroByte;
        position++;
        ByteArray[position] = opCode;            //设置操作码（读或写）
        position++;
        for (int i = 0; i < fileName.length(); i++) {
            ByteArray[position] = (byte) fileName.charAt(i);  //返回指定索引处的 char 值,强转为byte类型
            position++;
        }
        ByteArray[position] = zeroByte;       //文件名以0字节作为终止
        position++;
        for (int i = 0; i < mode.length(); i++) {
            ByteArray[position] = (byte) mode.charAt(i);  //返回指定索引处的 char 值,强转为byte类型
            position++;
        }
        ByteArray[position] = zeroByte;       //模式以0字节作为终止
        return ByteArray;
    }
    
    private byte[] findFile(String fileName) throws IOException{
        byte[] input=Files.readAllBytes(Paths.get("dictionary/"+fileName));
        //byte[] input=new byte[1024];
        return input;
    }
    
    private boolean fileExist(String fileName){
        File tmpDir = new File("dictionary/"+fileName);
        boolean exists =false;
        if(tmpDir.exists() && tmpDir.isFile()){
            exists=true;
        }
        return exists;
    }
    

    
    private String reportError(byte errorCode){
        String msg="";
        if(errorCode==0){
            msg="Not defined, see error message (if any)."; 
        }
        else if(errorCode==1){
            msg="File not found.";
        }
        else if(errorCode==2){
            msg="Access violation.";
        }
        else if(errorCode==3){
            msg="Disk full or allocation exceeded.";
        }
        else if(errorCode==4){
            msg="Illegal TFTP operation.";
        }
        else if(errorCode==5){
            msg="Unknown transfer ID.";
        }
        else if(errorCode==6){
            msg="File already exists.";
        }
        else if(errorCode==7){
            msg="No such user.";
        }
        return msg;
    }
    
    
    
    public static void main(String[] args) throws Exception {
         TFTPTCPClient client=new TFTPTCPClient();
         client.run();
    }

}
