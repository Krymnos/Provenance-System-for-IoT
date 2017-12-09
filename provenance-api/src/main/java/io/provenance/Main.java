package io.provenance;

import io.provenance.ProvenanceContext;
import io.provenance.exception.ConfigParseException;

public class Main {

	public static void main(String[] args) {
		try {
			System.out.println(System.getenv("provenance.properties"));
			ProvenanceContext pc = ProvenanceContext.getOrCreate();
			
		} catch (ConfigParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
