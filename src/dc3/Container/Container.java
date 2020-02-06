
package dc3.Container;

import java.net.Socket;

import java.util.concurrent.TimeUnit;

import java.io.IOException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.KillContainerCmd;
import com.github.dockerjava.api.model.NetworkSettings;

import dc3.Network.DC3Socket;
import dc3.Network.Payload;
/**
* <p>
* Cria e mata containers também
* recebe e envia mensagens por um socket 
* </p>
* @author Giuliano Oliveira
*/
public class Container{
	DC3Socket s;
	DockerClient dc;

	CreateContainerResponse c;
	/**
	 * <p>cria e executa o .jar que deve estar na pasta /home da imagem
	 * </p>
	 * @param dockerClient objeto cliente da api do docker
	 * @param image imagem existente no docker atual
	 * @param name nome do container a ser criado
	 */
	public Container(DockerClient dockerClient,String image,String name){
		dc=dockerClient;
		c = dc.createContainerCmd(image)
		    .withCmd("java","-jar","/home/docker.jar",Integer.toString(3523))
		    .withName(name)
		    .withTty(true)
		    .exec();
		
	}
	/**
	 * <p>
	 Conecta ao socket do container criado
	 @throws Exception caso tente mais que 15 vezes a contectar
	 * </p>
	 */
	public void start()throws Exception{
		dc.startContainerCmd(c.getId()).exec();
  		String ip=dc.inspectContainerCmd(c.getId()).exec().getNetworkSettings().getIpAddress();
		
		for(int i=0;i<15;i++){
			System.out.printf("Connecting a %s:%d (%d)\n",ip,3523,i+1);	
			try{
				s=new DC3Socket(new Socket(ip,3523));
				break;
			}
			catch(Exception e){
				System.out.println("Trying again...");
			}
			TimeUnit.MILLISECONDS.sleep(300);
		}
		if(s==null)throw new Exception("Number of tries expired");
		System.out.println("Connection established");
	}
	/**
	 * <p>
	 	recebe Payload do socket
	 * </p>
	 * @return objeto Payload do socket
	 */
	public Payload recv()throws IOException{
		return s.recv();
	}
	/**
	 * <p>
	 	envia payload do socket
	 * </p>
	 * @param pay objeto Payload a ser enviado
	 */
	public void send(Payload pay)throws IOException,ClassNotFoundException{
		s.send(pay);
	}
	/**
	 * <p>
	 	mata e remove container
	 * </p>
	 */
	public void kr(){
		dc.killContainerCmd(c.getId()).exec();
		dc.removeContainerCmd(c.getId()).exec();
	}
}