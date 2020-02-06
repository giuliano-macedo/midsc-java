package dc3.Master;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;

import java.io.IOException;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Hashtable;

import dc3.Master.Stream;
import dc3.Dataset.Datapoint;
import dc3.Dataset.Classifier;

import dc3.Network.DC3Server;
import dc3.Network.Payload;
public class Master{
	private Options options = new Options();
	
	int streamSpeed=200;
	Boolean safe=false;
	Boolean verbose=false;
	String csvFilename;

	public Master(){
	}
	private void help(){
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("master [OPTIONS] ... [dataset.csv]", options);
		System.exit(2);
	}
	private void cliSetup(String[] args){
		options.addOption("h", "help", false, "show help.");
		options.addOption(
			OptionBuilder.withLongOpt("stream_speed")
			.withArgName("No. of points per time unit, default 200")
            .withType(Number.class)
            .hasArg()
            .withDescription( "set stream speed" )
            .create( "s" )
        );
        options.addOption("S","ssl",false,"enable SSL");
        options.addOption("v","verbose",false,"enable verbose");
        CommandLine cmd=null;
		CommandLineParser parser = new PosixParser();

		try{
			cmd = parser.parse(options, args);
		}catch (ParseException e) {
		   System.out.println("Failed to parse comand line properties");
		   help();
  		}
  		if(cmd.hasOption("h"))help();
  		if(cmd.hasOption("S"))safe=true;
  		if(cmd.hasOption("v"))verbose=true;
  		if(cmd.hasOption("s")){
  			streamSpeed=Integer.valueOf(cmd.getOptionValue("s"));
  			if(streamSpeed<=0){
  				System.out.println("Invalid stream_speed!");
  				System.exit(-1);
  			}
  		}
  		String[] rmnArgs=cmd.getArgs();
  		if(rmnArgs.length<1){
			System.out.println("No database.csv set!");
			help();
		}
		else if(rmnArgs.length>1){
			System.out.println("Too much arguments!");
			help();
		}
		csvFilename=rmnArgs[0];

  	}

	static public void main(String[] args)throws IOException{
		Master master=new Master();
		master.cliSetup(args);	
		Stream stream=null;
		try{
			stream = new Stream(master.csvFilename,master.streamSpeed);
		}catch(Exception e){
			System.out.println("Error opening "+master.csvFilename);
			System.exit(-1);
		}
		DC3Server server =new DC3Server(master.safe);
		server.setVerbose(master.verbose);
		System.out.println("Waiting for clients");
        server.waitForClients();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press enter to stop waiting");
        scanner.nextLine();
        server.stopWaitForClients();

        System.out.println("DC3Server Ready");
		Hashtable<Integer,Payload> ssqs=null;
		try{
			while(stream.hasPoints()){
				server.sendAll(new Payload(Payload.Id.DATAPOINTS,stream.getPoints()));
			}
			server.sendAll(new Payload(Payload.Id.REQ_SSQ,null));
			ssqs=server.recvAll();
		}
		catch(Exception e){
			System.out.println("Server error:"+e.toString());
			System.exit(-1);
		}
		if(ssqs==null){
			System.out.println("No ssq response");
			return;
		}
		Integer minid=(Integer)ssqs.keySet().toArray()[0];
		Float minval=(Float)ssqs.get(minid).obj;
		for(Integer id:ssqs.keySet()){
			Float val=(Float)ssqs.get(id).obj;
			System.out.printf("%s ssq: %f\n",server.clientToString(id),val);
			if(minval>val){
				minval=val;
				minid=id;
			}
		}
		System.out.printf("min ssq is from %s ssq: %f\n",
			server.clientToString(minid),minval);
	}

}