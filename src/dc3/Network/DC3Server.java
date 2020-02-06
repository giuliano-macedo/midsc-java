package dc3.Network;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLServerSocketFactory;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.LogRecord;
import dc3.Network.DC3Socket;

/**
* <p>
* Cria um servidor que gerencia n clientes paralelamente,
* todos trocando mensagens de tipo Payload
* e também tem um logger único
* </p>
* @author Giuliano Oliveira
*/
public class DC3Server{
	class ClientAcceptHandler extends Thread{
		public Semaphore sem;
		DC3Server dc3;
		ClientAcceptHandler(DC3Server inst){
			dc3=inst;
			sem=new Semaphore(1);
		}
		public void run(){
            while(true){
            	try{
            		DC3Socket socket = new DC3Socket(dc3.serverSocket.accept());
            		dc3.acceptClient(socket);
            	}catch(SocketTimeoutException e){
            		if(!sem.tryAcquire())break;
            		else sem.release();
            	}catch(IOException e){
            		dc3.log.log(Level.SEVERE,"Error Accepting socket in server");
            		System.exit(-1);
            	}
        	}
		}
	}
	class ClientSendHandler extends Thread{
		Payload pay;
		public DC3Socket s;
		public Exception err;
		public ClientSendHandler(DC3Socket s,Payload pay){
			this.s=s;
			this.pay=pay;
			err=null;
		}
		public void run(){
			try{
				s.send(pay);
			}catch(Exception e){
				err=e;
			}
		}
	}
	class ClientRecvHandler extends Thread{
		Payload msg;
		public DC3Socket s;
		Hashtable<Integer,Payload> ans;
		DC3Server dc3;
		Integer key;
		Semaphore sem;
		int length;

		public ClientRecvHandler(
			DC3Server dc3,
			Integer key,
			Semaphore sem,
			Hashtable<Integer,Payload> ans){
			this.dc3=dc3;
			this.sem=sem;
			this.ans=ans;
			this.key=key;
			this.s=dc3.clients.get(key);
			if(!sem.tryAcquire(1))
				fatalErr("error acquiring parent semaphore in crh");
		}
		public void run(){
			Payload pay=null;
			try{
				pay=s.recv();
				if(pay==null)throw new Exception("recv is null");
				ans.put(key,pay);
			}catch(Exception e){
				dc3.log.log(Level.WARNING,
					String.format("Lost connection to %s because %s",
					s.getIpAddress(),
					e.toString()
				));
				dc3.clients.remove(s);
			}
			sem.release(1);
		}
	}
	public int timeoutPerClientBatch=3;
	public ServerSocket serverSocket;
	public Hashtable<Integer,DC3Socket> clients;
	Logger log;
	int port;
	ClientAcceptHandler cah;
	Random r;
	/**
	 * <p>Começa a ouvir por clientes paralelamente
	 * </p>
	 * @param safe TODO, define se usa TLS ou não
	 * @param port número do port a ser ouvido
	 */
	public DC3Server(Boolean safe,int port)throws IOException{
		this.port=port;
		java.util.logging.LogManager.getLogManager().reset();
		log=Logger.getLogger(String.format("dc3Server(%d)",port));
		ConsoleHandler console = new ConsoleHandler();
		console.setLevel(Level.ALL);

		System.setProperty("java.util.logging.SimpleFormatter.format", 
        "");
		console.setFormatter(new SimpleFormatter(){
		public synchronized String format(LogRecord record){
				return  String.format("[%s][%s]%s\n",
					record.getLoggerName​(),record.getLevel(),record.getMessage());
			}
		});
		log.addHandler(console);
		log.setLevel(Level.WARNING);
		r= new Random(System.currentTimeMillis());
		clients=new Hashtable<Integer,DC3Socket>();
		cah=new ClientAcceptHandler(this);
		if(safe){
			System.setProperty("javax.net.ssl.keyStore","keystore");
			System.setProperty("javax.net.ssl.keyStorePassword","password");
			// System.setProperty("javax.net.debug","ssl:handshake");
			// System.setProperty("javax.net.debug","all");
			SSLServerSocketFactory sslServerSocketFactory =(SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			serverSocket = sslServerSocketFactory.createServerSocket(port);
		}
		else serverSocket = new ServerSocket(port);

	}
	/**
	 * <p>Equivalente a DC3Server(false,3523)
	 * </p>
	 */
	public DC3Server()throws IOException{
		this(false,3523);
	}
	/**
	 * <p>Equivalente a DC3Server(safe,3523)
	 * </p>
	 * @param safe TODO, define se usa TLS ou não
	 */

	public DC3Server(Boolean safe)throws IOException{
		this(safe,3523);
	}
	/**
	 * <p>Define a prioridade do logger
	 * </p>
	 * @param verbose verdadeiro define o level para todos, e falso para warning
	 */
	public void setVerbose(Boolean verbose){
		if(verbose){
			log.setLevel(Level.ALL);
		}
		else{
			log.setLevel(Level.WARNING);
		}
	}
	void fatalErr(String msg){
		log.log(Level.SEVERE,String.format("fatal error: %s",msg));
		System.exit(-1);
	}
	public void acceptClient(DC3Socket s){
		//..test if valid connection
		int n;
		do{
			n=r.nextInt();
		}while(clients.containsKey(n));
		clients.put(n,s);
		log.log(Level.INFO,String.format("%s connected with id %x",
					s.getIpAddress(),n));
	}
	/**
	 * <p>espera por clientes paralelamente
	 * </p>
	 */
	public void waitForClients()throws IOException{
		//wait 500ms check if request to stop, then continue
		serverSocket.setSoTimeout(500);
		cah.start();
	}
	/**
	 * <p>para de esperar por clientes paralelamente
	 * </p>
	 */
	public void stopWaitForClients()throws IOException{
		try{cah.sem.acquire();}
		catch(InterruptedException e){
			fatalErr("Failed aquire cah semaphore, thread is interupted");
		}
		try{Thread.sleep(500);}
		catch(InterruptedException e){
			fatalErr("Failed sleeping, current thread is interupted");
		}
		if(cah.isAlive())fatalErr("cah still alive");
	}
	/**
	 * <p>envia objeto Payload a todos os clientes conectados paralelamente
	 * </p>
	 * @param pay objeto Payload a ser enviado a todos os clientes
	 */
	public void sendAll(Payload pay){
		if(clients.isEmpty())return;
		for(Integer i:clients.keySet()){
			new ClientSendHandler(clients.get(i),pay).start();
		}
		log.log(Level.INFO,String.format("Sent payload to %d clients",
					clients.size()));
	}
	//maybe concurrentLinkedQueue?
	/**
	 * <p>recebe objeto Payload de todos os clientes conectados
	 * </p>
	 * @return hashtable de id,Payload dos clientes
	 */
	public Hashtable<Integer,Payload> recvAll(){
		if(clients.isEmpty())return null;
		Hashtable<Integer,Payload> ans = new Hashtable<Integer,Payload>();
		int clientsSize=clients.size();
		Semaphore sem=new Semaphore(clientsSize);
		int msl=500;
		for(Integer i:clients.keySet()){
			new ClientRecvHandler(this,i,sem,ans).start();
			msl=clients.get(i).timeout;
		}
		int i=timeoutPerClientBatch;
		msl+=i;
		for(;i<msl;i+=timeoutPerClientBatch){
			try{Thread.sleep(timeoutPerClientBatch);}
			catch(Exception e){fatalErr("failed sleeping, thread is interrupted");}
			if(sem.tryAcquire(clientsSize))break;
		}
		if(i>=msl&&!sem.tryAcquire(clientsSize))
			fatalErr(String.format("There are still %d crh threads running",
				clientsSize-sem.availablePermits()));
		return ans;
	}
	/**
	 * <p>envia objeto Payload a todos os clientes conectados e espera confirmação
	 * </p>
	 * @param pay objeto Payload a ser enviado a todos os clientes
	 */
	public void sendAllAndWait(Payload msg){
		if(clients.isEmpty())return;
		LinkedList<ClientSendHandler> cshs=new LinkedList<ClientSendHandler>();
		int msl=500;
		for(Integer i:clients.keySet()){
			ClientSendHandler chs=new ClientSendHandler(clients.get(i),msg);
			cshs.addFirst(chs);
			chs.start();
			msl=clients.get(i).timeout;
		}
		int i=timeoutPerClientBatch;
		msl+=i;
		for(;i<msl;i+=timeoutPerClientBatch){
			try{Thread.sleep(timeoutPerClientBatch);}
			catch(Exception e){fatalErr("failed sleeping, thread is interrupted");}
			Iterator<ClientSendHandler> it = cshs.iterator();
			Boolean remove=false;
			ClientSendHandler csh=null;
			while(it.hasNext()){
				if(!remove)csh=it.next();
				else{
					Iterator<ClientSendHandler> temp=it;
					csh=it.next();
					temp.remove();
					remove=false;
				}
				if(csh.isAlive()){
					if(i>=msl)fatalErr("Unexpected , csh is stil alive");					
				}
				else{
					if(csh.err!=null){
						log.log(Level.WARNING,String.format("Lost connection to %s because %s",
							csh.s.getIpAddress(),
							csh.err.toString()
							));
						clients.remove(csh.s);
					}
					else 
						log.log(Level.INFO,String.format("%s recieved packets",csh.s.getIpAddress()));
					remove=true;
				}
			}
			if(remove)cshs.removeLast();
			if(cshs.isEmpty())break;
		}
		if(!cshs.isEmpty())
			fatalErr(String.format("Unexpected ,cshs is not empty (%d)",cshs.size()));
		log.log(Level.INFO,String.format("Sent to %d clients and waited %d ms",
					clients.size(),i));
	}
	public String clientToString(Integer id){
		DC3Socket s=clients.get(id);
		if(s==null){
			return "(null)";
		}
		return String.format("%s(%x)",s.getIpAddress(),id);
	}

 }