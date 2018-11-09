package mesquite.oregonstate.InterpretFASTACustomDRM;

import mesquite.categ.lib.MolecularData;
import mesquite.lib.characters.CharacterData;
import mesquite.io.InterpretFastaDNA.InterpretFastaDNA;
import mesquite.lib.*;

public class InterpretFASTACustomDRM extends InterpretFastaDNA {
	
	
	/*.................................................................................................................*/
	public void processFileName(String fileName, CharacterData data, Taxa taxa, int taxonNumber) {
		// e.g., &vDRMDNA0772_&g28S_&pPUB006_&aAF438104_&nOodes amaroides.fas
		
		if (StringUtil.notEmpty(fileName)  && data!=null) {
			Parser parser = new Parser(fileName);
			parser.setPunctuationString("_");
			String token = parser.getFirstToken();
			while (StringUtil.notEmpty(token)) {
				if (token.startsWith("&v")) {  // voucher token
					String code = token.substring(2);
					if (code.startsWith("DRMDNA")) {
						code = "DRM" + code.substring(6);
					}
					taxa.setAssociatedObject(VoucherInfoFromOTUIDDB.voucherCodeRef, taxonNumber, code);
				} else if (token.startsWith("&a")) {  // accession number token
					String accession = token.substring(2);
					Taxon taxon = data.getTaxa().getTaxon(taxonNumber);
					Associable tInfo = data.getTaxaInfo(true);
					if (tInfo != null && taxon != null) {
						tInfo.setAssociatedObject(MolecularData.genBankNumberRef, taxonNumber, accession);
					}
				} else if (token.startsWith("&p")) {  // accession number token
					String pubCode = token.substring(2);
					if (!pubCode.equalsIgnoreCase("PUB000") && !pubCode.equalsIgnoreCase("PUB00"))
						data.setPublicationCode(taxonNumber, pubCode);
				} 
				token = parser.getNextToken();
			}
		}
		
	}
	

	
	
	/*.................................................................................................................*/
	 public String getName() {
	return "FASTA (DNA/RNA) [DRM Lab Custom]";
	 }
/*.................................................................................................................*/
/** returns an explanation of what the module does.*/
public String getExplanation() {
	return "Imports and exports FASTA files that consist of DNA/RNA sequence data, and interprets file names from DRM Lab workflow" ;
	 }


}
