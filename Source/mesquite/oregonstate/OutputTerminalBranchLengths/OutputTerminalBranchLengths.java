/* Mesquite source code, Treefarm package.  Copyright 1997 and onward, W. Maddison, D. Maddison and P. Midford. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/
package mesquite.oregonstate.OutputTerminalBranchLengths;
/*~~  */

import java.util.*;
import java.awt.*;
import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.lists.lib.*;

/* ======================================================================== */
public class OutputTerminalBranchLengths extends TreeListUtility { 
	
	double[] bl;
	
	
	
	public String getName() {
		return "Output Terminal Branch Lengths";
   	 }
   	 public String getExplanation() {
		return "Outputs branch lengths of selected trees.";
   	 }
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		return true;
	}
	
	
   	/** if returns true, then requests to remain on even after operateData is called.  Default is false*/
   	public boolean pleaseLeaveMeOn(){
   		return false;
   	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
   	public boolean isPrerelease(){
   		return true;
   	}
   	public boolean requestPrimaryChoice(){
   		return true;  
   	}
   	
   	double maxLength = 0.0;
   	
    private void storeTerminalBranchLengths(MesquiteTree tree, int node) {
        double currentLength = tree.getBranchLength(node);
        if (MesquiteDouble.isCombinable(currentLength)){
        	if (tree.nodeIsTerminal(node)) {
        		int taxonNumber = tree.taxonNumberOfNode(node);
        		if (taxonNumber>=0 && taxonNumber<bl.length) {
        			if (tree.descendantOf(node, tree.getRoot()) && (tree.numberOfDaughtersOfNode(tree.getRoot())<=2)) {  // have to add in other branch
        		       double otherLength = tree.getBranchLength(tree.nextSisterOfNode(node));
        		        if (MesquiteDouble.isCombinable(otherLength))
        		        	currentLength+= otherLength;
        			}
        			bl[taxonNumber]= currentLength;
        			if (maxLength<currentLength)
        				maxLength=currentLength;
        		}
        	}
        }
        for (int daughter=tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter) ) {
        	storeTerminalBranchLengths(tree, daughter);
        }
    }

	/*.................................................................................................................*/
   	public boolean operateOnTrees(TreeVector trees){
   		if (trees == null)
   			return false;
   		Taxa taxa = trees.getTaxa();
   		bl= new double[taxa.getNumTaxa()];
   		int numTrees = trees.size();
   		boolean doAll = !trees.anySelected();
   		log("\nWriting Branch Lengths ");
   		int dotFreq = 1;
   		if (numTrees >100 && numTrees<500)
   			dotFreq=5;
   		else if (numTrees>500)
   			dotFreq=10;

   		StringBuffer sb = new StringBuffer();
   		sb.append("\t");
   		for (int i=0; i<bl.length; i++) 
   			sb.append("\t"+taxa.getTaxonName(i));
		sb.append("\tMaximum Length");
   		sb.append("\n");

		for (int j=0; j<numTrees; j++){
			if (doAll || trees.getSelected(j)){
				Tree tree = trees.getTree(j);
				maxLength = 0.0;
		   		for (int i=0; i<bl.length; i++)
		   			bl[i]=0.0;

				if (tree!=null && tree instanceof MesquiteTree) {
					CommandRecord.tick("Writing branch lengths of tree " + j + " of " + numTrees);
			   		if (j % dotFreq == 0)
			   			log(".");
			   		storeTerminalBranchLengths((MesquiteTree)tree, tree.getRoot());
			   		sb.append(tree.getName()+"\t");
			   		for (int i=0; i<bl.length; i++)
			   			sb.append("\t"+bl[i]);
		   			sb.append("\t"+maxLength);
			   		sb.append("\n");

 				}
			}
		}
		MesquiteFile.putFileContentsQuery("", sb.toString(), true);
		
		return true;
		
	}
}

	


