package dc3.Network;
import java.io.Serializable;
import java.util.ArrayList;
import java.io.IOException;
import dc3.Network.DC3Socket;
import dc3.Dataset.Datapoint;
/**
* <p>
* Define uma estrutura de uma payload a ser enviado por um socket
* se consiste de um byte indicando o tipo de payload e em seguida dados
* </p>
* @author Giuliano Oliveira
*/
public class Payload implements Serializable{
	public enum Id{
		OK,
		ERR,
		DATAPOINTS,
		REQ_SSQ,
		SSQ,
		END;
	};
	public Id id;
	static private final Id[] enums=Id.values();
	public Object obj;
	public Payload(){
		id=Id.OK;
		obj=null;
	}
	/**
	 * <p>Instancia um objeto Payload com um id e dados
	 * </p>
	 * @param id id do payload
	 * @param obj dados do payload
	 */
	public Payload(Id id,Object obj){
		this.id=id;
		this.obj=obj;
	}
	/**
	 * <p>Equivalente a Payload(id,null)
	 * </p>
	 * @param id id do payload
	 */
	public Payload(Id id){
		this(id,null);
	}
	/**
	 * <p>Processa e le dados de um DC3Socket de forma que os dados façam
	 	sentido com o id
	 * </p>
	 * @param sock socket a ser lido
	 */
	public void readFrom(DC3Socket sock)throws IOException{
		int b=0;
		try{
			b=sock.in.readByte();
			id=enums[(int)b];
		}catch(ArrayIndexOutOfBoundsException e){
			throw new IOException("Invalid packet id : "+b);
		}
		switch(id){
			case DATAPOINTS:
				int pointsSize=sock.in.readInt();
				ArrayList<Datapoint> points
				=new ArrayList<Datapoint>(pointsSize);
				
				int dim=sock.in.readInt();
				byte[] buff=new byte[dim*4];
				for(int i=0;i<pointsSize;i++){
					sock.in.read(buff,0,dim*4);
					Datapoint p=new Datapoint(buff);
					points.add(p);
				}
				obj=points;
				break;
			case SSQ:
				obj=sock.in.readFloat();
				break;
				
		}
	}
	/**
	 * <p>Processa dados e envia através 
	 	de um DC3Socket de forma que os dados façam
	 	sentido com o id
	 * </p>
	 * @param sock socket a ser enviado
	 */
	public void sendTo(DC3Socket sock)throws ClassNotFoundException,IOException{
		sock.out.writeByte(id.ordinal());
		switch(id){
			case DATAPOINTS:
				ArrayList<Datapoint> points=(ArrayList<Datapoint>)obj;
				sock.out.writeInt(points.size());
				sock.out.writeInt(points.get(0).dim);
				for(Datapoint p:points){
					byte[]b = p.toByteArr();
					sock.out.write(b,0,p.dim*4);
				}
				break;
			case SSQ:
				sock.out.writeFloat((Float)obj);
				break;
		}
	}
}