package Managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import Analyzer.ClustersAnalyzer;
import Models.Proposal;
import Models.Word;

public class ProposalManager {
	private static ProposalManager instance = null;
	private Set<Proposal> proposalSet = new HashSet<>();
	public static ProposalManager getInstance(){
		if(instance == null)
			instance = new ProposalManager();
		return instance;
	}
	private ProposalManager() {
		// TODO Auto-generated constructor stub
	} 
	
}
