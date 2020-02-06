package dc3.Docker;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;

import java.io.EOFException;
import dc3.Network.DC3Socket;
import dc3.Network.Payload;
import dc3.Dataset.Datapoint;
import dc3.Dataset.Classifier;
public class Docker{
	public static void main(String[] args)throws Exception{
        DC3Socket socket=new DC3Socket(new ServerSocket(3523).accept(),0);
        Classifier cls=new Classifier();
        while(true){
            Payload pay;
            try{
            	pay=socket.recv();
            }catch(EOFException e){
            	break;
            }catch(Exception e){
            	socket.send(new Payload(Payload.Id.ERR,e.toString()));
            	return;
            }
            if(pay==null){
            	break;
            }
            switch(pay.id){
            	case DATAPOINTS:
	            	ArrayList<Datapoint> points=(ArrayList<Datapoint>)pay.obj;
	            	if(points!=null){
	            		cls.addPoints(points);
	            	}
	            	else{
	            		socket.send(new Payload(Payload.Id.ERR,"points are null"));
	            	}
	            	break;
	            case REQ_SSQ:
	            	socket.send(new Payload(Payload.Id.SSQ,cls.avgssq()));
	            	break;
	            case END:
	            default:
	            	return;
            }
        }
	}
}