
package mesquite.oregonstate.lib;


import mesquite.lib.*;

/* ======================================================================== */
public class PCRToSampleTranslationFile { 
	String [] PCRCodes;
	String [] sampleNames;
	int numCodes;

	public PCRToSampleTranslationFile(String PCRList) {
		readTappedPCRCodeFile(PCRList);
	}

	private void initializeArrays(int numCodes) {
		PCRCodes = new String[numCodes];
		sampleNames = new String[numCodes];
	}
	
	public void readTappedPCRCodeFile(String PCRList) {
		String onePCRCode="";
		Parser parser = new Parser();
		parser.setString(PCRList);
		Parser subParser = new Parser();
		String line = parser.getRawNextDarkLine();

		numCodes = 0;
		while (!StringUtil.blank(line)) {
			numCodes ++;
			line = parser.getRawNextDarkLine();
		}
		if (numCodes==0){
			MesquiteMessage.discreetNotifyUser("File is empty.");
			return;
		}
			
		initializeArrays(numCodes);

		int count = -1;
		parser.setPosition(0);
		subParser.setPunctuationString("\t");
		line = parser.getRawNextDarkLine();
		String token = "";
		
		while (!StringUtil.blank(line)) {
			count ++;
			subParser.setString(line);
			token = subParser.getFirstToken();
			if (StringUtil.notEmpty(token)) {
				PCRCodes[count] = token;
				token = subParser.getNextToken();
				if (StringUtil.notEmpty(token)) {
					sampleNames[count]=token;
				}
			}
			line = parser.getRawNextDarkLine();
		}
	}


	/*.................................................................................................................*/
	public String getSampleName(String PCRCode) {
		if (!StringUtil.blank(PCRCode)) {
			for (int i=0; i<PCRCodes.length; i++) {
				if (PCRCode.trim().equalsIgnoreCase(PCRCodes[i])) {
					return sampleNames[i];
				}
			}
		}
		return "";
	}
}


