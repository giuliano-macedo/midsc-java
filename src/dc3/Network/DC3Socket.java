package dc3.Network;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.util.concurrent.Semaphore;
import java.io.BufferedInputStream;
import java.io.IOException;
/**
* <p>
* Define um socket MT safe que se comunica com objetos Payload
* </p>
* @author Giuliano Oliveira
*/
public class DC3Socket{
	public Socket socket;
	public int timeout;
	DataInputStream in;
	DataOutputStream out;
	Semaphore semout;
	Semaphore semin;
/**
	 * <p>Equivalente a DC3Socket(s,500)
	 * </p>
	 * @param s objeto Socket
	 */
	public DC3Socket(Socket s)throws IOException{
		this(s,500);
	}
	/**
	 * <p>Define timeout a um socket e define
	 * semaforos para controlar operações de IO
	 * </p>
	 * @param s objeto Socket
	 * @param tim tempo em ms de espera limite
	 */
	public DC3Socket(Socket s,int tim)throws IOException{
		timeout=tim;
		socket=s;
		if(timeout!=0)socket.setSoTimeout(timeout);
		out =  new DataOutputStream( 
            new BufferedOutputStream(socket.getOutputStream())
        );
		in =  new DataInputStream(
            new BufferedInputStream(socket.getInputStream())
        );
        semout=new Semaphore(1);
        semin=new Semaphore(1);
	}
	/**
	 * <p>Recebe objeto Payload de forma MT safe
	 * </p>
	 * @return objeto Payload recebido do socket
	 */
	public Payload recv()throws IOException{
		try{
			semin.acquire();
		}catch(Exception e){
			throw new IOException(e);
		}
		Payload ans=new Payload();
		ans.readFrom(this);
		semin.release();
		return ans;
	}
	/**
	 * <p>envia objetos Payload de forma MT safe
	 * </p>
	 * @param obj payload a ser enviado
	 */
	public void send(Payload obj)throws IOException,ClassNotFoundException{
		try{
			semout.acquire();
		}catch(Exception e){
			throw new IOException(e);
		}
		obj.sendTo(this);
		out.flush(); //removeme
		semout.release();
	}
	/**
	 * <p>retorna string de IP do socket
	 * </p>
	 * @return ip em formato xxx.xxx.xxx
	 */
	public String getIpAddress(){
		return socket.getInetAddress​().toString().replace("/","");
	}
}