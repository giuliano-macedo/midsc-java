package dc3.Dataset;
import dc3.Dataset.Datapoint;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;
public class Classifier{
	ArrayList<Integer> labels;
	ArrayList<Datapoint> dataset;
	ArrayList<Datapoint> centroids;
	Random r;
	int k=5;
	Datapoint[] dists=new Datapoint[k];
	Boolean first=true;
	public Classifier(){
		r= new Random(System.currentTimeMillis());
		centroids=new ArrayList<Datapoint>();
		dataset=new ArrayList<Datapoint>();
		labels=new ArrayList<Integer>();
	}
	private static final float safeAdd(float left, float right) {
		// System.out.println("hi");
		// System.out.printf("%f+%f\n",left,right);
		// if (right > 0.0 ? left > Float.MAX_VALUE - right
		// 			: left < Float.MIN_VALUE - right) {
		// 	throw new ArithmeticException("Float overflow");
	 //  }
	  return left + right;
	}
	public void addPoints(ArrayList<Datapoint> ps){
		int dim=ps.get(0).dim;
		if(first){
			for(int i=0;i<k;i++){
				dists[i]=new Datapoint(dim);
			}
			float[] t=new float[dim];
			Arrays.fill(t,Float.POSITIVE_INFINITY);
			Datapoint min=new Datapoint(t);
			Arrays.fill(t,Float.NEGATIVE_INFINITY);
			Datapoint max=new Datapoint(t);
			for(Datapoint p :ps){
				for(int i=0;i<dim;i++){
					min.data[i]=Math.min(min.data[i],p.data[i]);
					max.data[i]=Math.max(max.data[i],p.data[i]);
				}
			}
			for(int i=0;i<k;i++){
				Datapoint c=new Datapoint(dim);
				for(int j=0;j<dim;j++){
					c.data[j]=min.data[j]+(r.nextFloat()*(max.data[j]-min.data[j]));
				}
				centroids.add(c);
			}
		}
		dataset.addAll(ps);
		int unused;
		int s=ps.size();
		for(int i=0;i<s;i++)labels.add(0);
		long[] nos=new long[k];
		s=dataset.size();
		for(int i=0;i<s;i++){
			Datapoint p=dataset.get(i);
			float md=Float.POSITIVE_INFINITY;
			int mi=0;
			for(int j=0;j<k;j++){
				Datapoint c=centroids.get(j);
				float d=c.dist(p);
				if(d<md){
					md=d;
					mi=j;
				}
			}
			Datapoint dd=dists[mi];
			for(int j=0;j<dim;j++){
				dd.data[j]=safeAdd(dd.data[j],p.data[j]);
			}
			nos[mi]++;
			labels.set(i,mi);
		}
		for(int i=0;i<k;i++){
			Datapoint c=centroids.get(i);
			for(int j=0;j<dim;j++){
				if(nos[i]!=0.0)c.data[j]+=dists[i].data[j]/nos[i];
			}
			Arrays.fill(dists[i].data,0);
		}
	}
	public float avgssq(){
		float ans=0;
		int s=dataset.size();
		for(int i=0;i<s;i++){
			ans+=dataset.get(i).dist(centroids.get(labels.get(i)));
		}
		return ans/s;
	}
}