package dc3.Slave;
import java.net.Socket;
import java.util.ArrayList;

import java.io.EOFException;
import dc3.Network.DC3Socket;
import dc3.Network.Payload;
import dc3.Dataset.Datapoint;
import dc3.Dataset.Classifier;
import dc3.Container.Ship;
public class Slave{
	public static void main(String[] args)throws Exception{
		if(args.length!=1){
			System.out.println("[Usage] slave.jar [master ip]");
			System.exit(2);
		}
		String masterIp=args[0];
		// System.setProperty("javax.net.ssl.trustStore","keystore");
        // System.setProperty("javax.net.debug","all");
        // System.setProperty("javax.net.ssl.trustPassword","password");
         
        // SSLSocketFactory sslSocketFactory =(SSLSocketFactory)SSLSocketFactory.getDefault();
        // DC3Socket socket = new DC3Socket(sslSocketFactory.createSocket("10.0.0.1", port),0);
        DC3Socket socket=new DC3Socket(new Socket(masterIp,3523),0);
        System.out.println("Setting docker up...");
        Ship ship=new Ship("openjdk:dc3");

        ship.setCapacity(5);
        System.out.println("Pronto");
        Classifier cls=new Classifier();
        while(true){
            Payload pay;
            try{
            	System.out.println("Recv data...");
            	pay=socket.recv();
            }catch(EOFException e){
            	System.out.println("Lost connection");
            	break;
            }catch(Exception e){
            	System.out.println("recv err:"+e.toString());
            	break;
            }
            if(pay==null){
            	break;
            }
            ship.sendAll(pay);
            switch(pay.id){
            	case DATAPOINTS:
	            	ArrayList<Datapoint> points=(ArrayList<Datapoint>)pay.obj;
	            	if(points!=null){
	            		cls.addPoints(points);
	            	}
	            	else{
	            		System.out.println("points are null");
	            	}
	            	break;
	            case REQ_SSQ:
                    float minssq=cls.avgssq();
                    int ch=-1;
                    int i=0;
                    System.out.println("slave ssq:"+minssq);
                    for(Payload p:ship.recvAll()){
                        float curssq=(float)p.obj;
                        System.out.printf("container_%d ssq:%f\n",i,curssq);
                        if(curssq<minssq){
                            minssq=curssq;
                            ch=i;
                        }
                        i++;
                    }
                    if(ch==-1){
                        System.out.print("smallest ssq was from this slave");
                    }
                    else{
                        System.out.print("smallest ssq was from container_"+ch);
                    }
                    System.out.println(": "+minssq);
	            	socket.send(new Payload(Payload.Id.SSQ,minssq));
	            	break;
	            case END:
	            default:
                    ship.krAll();
                    return;
            }
        }
        ship.krAll();
	}
}