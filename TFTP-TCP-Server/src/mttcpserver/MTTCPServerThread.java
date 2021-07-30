package mttcpserver;
import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MTTCPServerThread extends Thread {

    private Socket slaveSocket = null;
    private OutputStream output;
    private InputStream input;
    private static final byte RRQ = 1;                  //read the file
    private static final byte WRQ = 2;
    private static final byte DAT = 3;                  //file data
    private static final byte ERROR = 5;
    private final int maxSize=512+4;

    public MTTCPServerThread(Socket socket) {
        super("MTTCPServerThread");
        this.slaveSocket = socket;
    }

    @Override
    public void run() {
        System.out.println();
        theList();
        String line;
        byte[] buffer=new byte[maxSize];
        while(true)
        try {
            output = slaveSocket.getOutputStream();
            input =slaveSocket.getInputStream();
            input.read(buffer);
            if(buffer[1]==RRQ){
                String fileName="";
                int i=2;
                while(buffer[i]!=0){
                    fileName+=(char)buffer[i];
                    i++;
                }
                System.out.println("Read request: "+fileName);
                if(fileExist(fileName)){
                    byte[] content=findFile(fileName);
                    sendFile(content);
                    buffer=new byte[maxSize];
                    //slaveSocket.close();
                    //break;
                }
                else{
                    byte noFile=1;
                    createError(noFile);
                    System.err.println("No this file, error message has been sended...");
                    buffer=new byte[maxSize];
                    //slaveSocket.close();
                    //break;
                }
            }
            else if(buffer[1]==WRQ){
                String fileName="";
                int i=2;
                while(buffer[i]!=0){
                    fileName+=(char)buffer[i];
                    i++;
                }
                System.out.println("Write request: "+fileName);
                if(!fileExist(fileName)){
                    output.write(buffer);
                    ByteArrayOutputStream byteOut = receiveFile();
                    writeFile(byteOut,fileName);
                    System.out.println("The file has been store in server dictionary");
                    System.out.println(new File("dictionary/"+fileName).toURI().toString());
                    buffer=new byte[maxSize];
                }
                else{
                    byte fileE=6;
                    createError(fileE);
                    System.err.println("file is exist: "+new File("dictionary/"+fileName).toURI().toString());
                    buffer=new byte[maxSize];
                    //slaveSocket.close();
                    //break;
                }
            }
        } catch (IOException e) {
            System.err.println(e);
            break;
        }
        
    }
    
    private void theList(){
        System.out.println();
        System.out.println("here are all files in server dictionary:");
        File file = new File("dictionary");
        String[] fileList = file.list();
        for(String fileName:fileList){
            System.out.println(fileName);
        }
        System.out.println();
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
    
    private ByteArrayOutputStream receiveFile() throws IOException {
        ByteArrayOutputStream byteOutOS = new ByteArrayOutputStream();
        int block = 1;
         //set buffer area
         byte [] buf= new byte[maxSize];
         int len=0;
         do{
            len=input.read(buf);
            System.out.println("Have recived "+block+" pakage.");   
            block++;
            byte[] opCode = { buf[0], buf[1] };   //get opcode
            if (opCode[1] == DAT ) {
                DataOutputStream dos = new DataOutputStream(byteOutOS);
                dos.write(buf,4,buf.length-4);
            }
         }
        while (len==516);
        System.out.println("All file have been recived！！");
        return byteOutOS;
    } 
    
    
    private void createError(byte errorCode){
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
        byte[] transfer=msg.getBytes();
        int byteLength=4+transfer.length+1;
        byte[] errorArray=new byte[byteLength];
        //byte[] errorArray={0,ERROR,0,errorCode};
        errorArray[0]=0;errorArray[1]=ERROR;errorArray[2]=0;errorArray[3]=errorCode;
        
        for(int i=0; i<transfer.length; i++){
            errorArray[4+i]=transfer[i];
        }
        errorArray[byteLength-1]=0;
        try {
            output.write(errorArray);
        } catch (IOException ex) {
            Logger.getLogger(MTTCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void sendFile(byte[] temp){
        int block=1;
        int position;
        byte zeroByte = 0;
        byte single = 1;
        byte tenth=0;
        int current=0;
        while(current+1<temp.length){
            position=0;
            if(temp.length-current-1>512){
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
                    sended[position] = temp[current];
                    //System.out.println("current " +current);
                    current++;
                    position++;
                }
                try {
                    output.write(sended);
                    System.out.println("sending " +block+" package");
                    block++;
                } catch (IOException ex) {
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
                int ByteLength = temp.length-current+4;//
                //System.out.println(ByteLength);
                byte[] sended=new byte[ByteLength];
                sended[position]=zeroByte;
                position++;
                sended[position]=DAT;
                position++;
                sended[position]=tenth;
                position++;
                sended[position]=single;
                position++;
                for (int i = 0; i < temp.length-current; i++) {
                    sended[position] = temp[current+i];  
                    position++;
                }
                current=temp.length-1;
                if(single==9){
                    single=0;
                    tenth++;
                }
                else{
                    single++;
                }
                try {
                    output.write(sended);
                    
                    System.out.println("sending " +block+" package");
                    block++;
                    
                } catch (IOException ex) {
                   
                }
            }
        }
        byte[] cancel=new byte[1];
        try {
            output.write(cancel);
        } catch (IOException ex) {
            Logger.getLogger(MTTCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
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
}
