package dc3.Container;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import dc3.Container.Container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

import java.util.UUID;//fixme

import dc3.Network.Payload;

/**
* <p>
* Gerencia vários objetos Container
* podendo definir o numero de containers e enviar e receber mensagens
* a todos eles
* </p>
* @author Giuliano Oliveira
*/
public class Ship{
	DockerClient dockerClient;
	DataInputStream cachein;
	DataOutputStream cacheout;
	RandomAccessFile cacheraf;
	ArrayList<Container> cargo;
	String ip;
	String image;
	/**
	 * <p>
	 * Cria o dockerClient para a classe
	 * </p>
	 * @param image imagem existente no docker atual
	 */
	public Ship(String image)throws IOException{
		this.image=image;
		cargo=new ArrayList<Container>();
		cacheraf=new RandomAccessFile("/tmp/dc3.txt","rw");
		ip=UUID.randomUUID().toString().substring(0,8);//fixme
		cachein=new DataInputStream(
			new BufferedInputStream(
				new FileInputStream(cacheraf.getFD())
			)
		);
		cacheout=new DataOutputStream(
			new BufferedOutputStream(
				new FileOutputStream(cacheraf.getFD())
			)
		);
		dockerClient = DockerClientBuilder.getInstance().build();
	}
	/**
	 * <p>Define o numero de containers no Ship
	 * </p>
	 * @param n número de containers a ser definido
	 */
	public void setCapacity(int n){
		int l=cargo.size();
		if(l==n)return;
		if(n>l){
			for(int i=l;i<n;i++){
				Container c=new Container(dockerClient,image,"dc3_"+ip+"_"+i);
				String id=c.c.getId();
				try{
					c.start();
				}catch(Exception e){
					System.out.println("Error starting container :"+e.toString());
					try{dockerClient.killContainerCmd(id).exec();}catch(Exception e2){}
					dockerClient.removeContainerCmd(id).exec();
					continue;
				}
				cargo.add(c);
			}
			return;
		}
	}
	/**
	 * <p>Envia objetos Payload a todos os containers
	 * </p>
	 * @param msg Payload a ser enviado a todos os containers
	 */
	public void sendAll(Payload msg)throws IOException,ClassNotFoundException{
		for(Container c:cargo){
			c.send(msg);
		}
	}
	/**
	 * <p>Mata e remove todos os containers atuais
	 * </p>
	 */
	public void krAll(){
		for(Container c:cargo){
			c.kr();
		}
		cargo=new ArrayList<Container>();
	}
	/**
	 * <p>Recebe todos os payloads de todos os containers
	 * </p>
	 *@return ArrayList de payloads com a resposta de todos os containers
	 */
	public ArrayList<Payload> recvAll()throws IOException{
		ArrayList<Payload> ans=new ArrayList<Payload>();
		for(Container c:cargo){
			ans.add(c.recv());
		}
		return ans;
	}
}