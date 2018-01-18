package io.provenance;

import io.provenance.config.ProvenanceConfig;
import io.provenance.exception.ConfigParseException;
import io.provenance.types.Context;
import io.provenance.types.Datapoint;
import io.provenance.types.InputDatapoint;

public class ProvenanceContext {
	
	private static ProvenanceContext pc;

	private ProvenanceContext() throws ConfigParseException {
		ProvenanceConfig.configure();
	}

	/**
	 * If ProvenanceContext already exits it will simply return otherwise it will first create the ProvenanceContext object.
	 * There can be only one ProvenanceContext object per node. 
	 *
	 * @return      ProvenanceContext object.
	 * @throws		ConfigParseException
	 */
	
	public static ProvenanceContext getOrCreate() throws ConfigParseException {
		if(pc == null) 
			pc = new ProvenanceContext();
		return pc;
	}
	
	/**
	 * Method to save provenance information. it can receive any number of Datapoints.
	 * 
	 * Example Usage:
	 *				ProvenanceContext pc = ProvenanceContext.getOrCreate();
	 *				pc.save(new Datapoint());
	 *
	 * @param  Datapoint...  Datapoints to be pushed.
	 * @return      An array of the IDs of the Datapoint that are successfully pushed to storage.
	 * @throws		InterruptedException
	 * @see         Datapoint
	 */
	
	public String[] save(Datapoint... dps) throws InterruptedException {
		return ProvenanceConfig.getDatapointBuffer().produce(dps);
	}
	
	/**
	 * Method to save send rate.
	 *
	 * @param  double  Current data send rate of the node.
	 * @throws		InterruptedException
	 */
	
	public void sendRate(double sendRate) throws InterruptedException{
		ProvenanceConfig.getMetaDataBuffer().produceSentRate(sendRate);
	}

	/**
	 * Method to save data receive rate.
	 *
	 * @param  double  Current data receive rate of the node.
	 * @throws		InterruptedException
	 */
	
	public void receiveRate(double receiveRate) throws InterruptedException {
		ProvenanceConfig.getMetaDataBuffer().produceReceiveRate(receiveRate);
	}
	
	/**
	 * Method to save data send and receive rates.
	 *
	 * @param  double  Current data send rate of the node.
	 * @param  double  Current data receive rate of the node.
	 * @throws		InterruptedException
	 */
	
	public void rate(double sendRate, double receiveRate) throws InterruptedException {
		ProvenanceConfig.getMetaDataBuffer().produceRates(sendRate, receiveRate);
	}
	
	/**
	 * Method to get a array of all required context attributes.
	 * Currently supported metrics 'loc','line','class','app','ctime','stime','rtime')
	 * 
	 * Example Usage:
	 *				ProvenanceContext pc = ProvenanceContext.getOrCreate();
	 *				pc.getContextParams();
	 * Example Response:
	 * 					["METER", "METRIC", "LOCATION", "LINE", "CLASS", "APPLICATION", "CREATE_TIME", "SEND_TIME", "RECEIVE_TIME"]
	 *
	 * @return      An array of all required context attributes.
	 * @see         Context
	 */
	
	public String[] getContextParams() {
		return ProvenanceConfig.getMetrics();
	}
	
	/**
	 * Returns an array of the InputDatapoint that can then be passed to the Datapoint constructor. 
	 *
	 * @param  inputDatapointsIDs  An array of the IDs of the Datapoint containing all the data points that will contribute to the new Datapoint.
	 * @return      An array of the input data points containing the default contribution tag for all the input data points that will contribute to the new Datapoint.
	 * @see         InputDatapoint
	 */
	
	public InputDatapoint[] getInputDatapoints(String[] inputDatapointsIDs) {
		InputDatapoint[] inputDatapoints = new InputDatapoint[inputDatapointsIDs.length];
		for(int i=0; i<inputDatapointsIDs.length; i++)
			inputDatapoints[i] = new InputDatapoint(inputDatapointsIDs[i], "simple");
		return inputDatapoints;
	}
	
	/**
	 * Returns an array of the InputDatapoint that can then be passed to the Datapoint constructor. 
	 *
	 * @param  inputDatapointsIDs  An array of the IDs of the Datapoint containing all the data points that will contribute to the new Datapoint.
	 * @param  contrIbution        Type of contribution for the input data points to the new data point. i.e. ("Avg", "Min")
	 * @return      An array of the input data points containing the contribution tag for all the input data points that will contribute to the new Datapoint.
	 * @see         InputDatapoint
	 */
	
	public InputDatapoint[] getInputDatapoints(String[] inputDatapointsIDs, String contrIbution) {
		InputDatapoint[] inputDatapoints = new InputDatapoint[inputDatapointsIDs.length];
		for(int i=0; i<inputDatapointsIDs.length; i++)
			inputDatapoints[i] = new InputDatapoint(inputDatapointsIDs[i], contrIbution);
		return inputDatapoints;
	}
	
	/**
	 * Stops all running threads and closes open sessions.
	 */
	
	public void close() {
		ProvenanceConfig.getDatapointIngestor().interrupt();
		ProvenanceConfig.getMetaDataIngestor().interrupt();
	}
}