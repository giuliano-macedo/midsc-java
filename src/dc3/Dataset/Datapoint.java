package dc3.Dataset;
import java.io.Serializable;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
public class Datapoint implements Serializable{
	public float[] data;
	public int dim;

	public byte[] toByteArr(){        
        ByteBuffer bbuf = ByteBuffer.allocate(data.length * 4);        
        FloatBuffer fbuf = bbuf.asFloatBuffer();
        fbuf.put(data);
        return bbuf.array();
	}
	public Datapoint(float[] d){
		data=d.clone();
		dim=data.length;
	}
	public Datapoint(int d){
		this(new float[d]);
	}
	public Datapoint(byte[] d){
		FloatBuffer buf=ByteBuffer.wrap(d).asFloatBuffer();
		data =new float[buf.remaining()];
		buf.get(data);
		dim=data.length;
	}
	public float dist(Datapoint b){
		float ans=0;
		for(int i=0;i<dim;i++){
			float temp=b.data[i]-data[i];
			ans+=temp*temp;
		}
		return (float)Math.sqrt(ans);
	}
	public void print(){
		for(int i=0;i<dim;i++){
			System.out.print(String.valueOf(data[i])+" ");
		}
		System.out.println();
	}
}