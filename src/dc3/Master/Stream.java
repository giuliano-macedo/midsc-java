package dc3.Master;
import dc3.Dataset.Datapoint;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.BufferedReader;
import java.lang.NumberFormatException;
import java.util.ArrayList;
import java.util.Iterator;
public class Stream{
	int spd;
	int rs=0;
	Boolean first=true;
	Iterator<CSVRecord> recs;
	ArrayList<ArrayList<String>> freqstrs;
	public Stream(String filename,Integer speed)throws Exception{
		spd=speed;
		BufferedReader br=new BufferedReader(new FileReader(filename));
		CSVParser parser=CSVParser.parse(br,CSVFormat.RFC4180);
		recs = parser.iterator();
		freqstrs=new ArrayList<ArrayList<String>>();
		
	}
	public Boolean hasPoints(){
		return recs.hasNext();
	}
	public ArrayList<Datapoint> getPoints() throws Exception{
		 ArrayList<Datapoint> ans=new ArrayList<Datapoint>();
		 CSVRecord temp=null;
		 if(first){
		 	if(!recs.hasNext()){
		 		throw new Exception("Unexpected EOF");
		 	}
		 	temp=recs.next();
			rs=temp.size();
			for(int i=0;i<rs;i++){
				freqstrs.add(new ArrayList<String>());
			}
		}
		for(int i=0;i<spd;i++){
			Datapoint point=new Datapoint(rs);
			if(!first){
				if(!recs.hasNext())break;
				temp=recs.next();
			}
			else first=false;
			if(temp.size()!=rs){
				throw new Exception("Invalid CSV format");
			}

			int j=0;
			for(String str:temp){
				float f=0;
				Boolean noerr=true;
				try{
					f=Float.valueOf(str);
				}catch(NumberFormatException e){
					noerr=false;
					ArrayList<String> currfreq=freqstrs.get(j);
					int ind=currfreq.indexOf(str);
					if(ind==-1){
						ind=currfreq.size();
						currfreq.add(str);
					}
					f=(float)ind;
				}
				if(noerr){
					if(!freqstrs.get(j).isEmpty())throw new Exception("Invalid CSV format:inconsistency on element "+j);
				}
				point.data[j]=f;
				j++;
			}
			ans.add(point);
		}
		return ans;
	}
}