package mesquite.oregonstate.lib;

import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteString;
import mesquite.lib.Parser;
import mesquite.lib.StringUtil;

public class CodesGenBankFileProcessor {

	String sampleCodeListPath = null;
	String sampleCodeListFile = null;
	String sampleCodeList = "";
	Parser sampleCodeListParser = null;

	
	public boolean chooseCodeFile() {
		MesquiteString dnaNumberListDir = new MesquiteString();
		MesquiteString dnaNumberListFile = new MesquiteString();
		String s = MesquiteFile.openFileDialog("Choose file containing sample codes", dnaNumberListDir, dnaNumberListFile);
		if (!StringUtil.blank(s)) {
			sampleCodeListPath = s;
			sampleCodeListFile = dnaNumberListFile.getValue();
			return processCodesFile();
		}
		return false;
	}
	/*.................................................................................................................*/
	public boolean processCodesFile() {
		if (!StringUtil.blank(sampleCodeListPath)) {
			sampleCodeList = MesquiteFile.getFileContentsAsString(sampleCodeListPath);

			if (!StringUtil.blank(sampleCodeList)) {
				sampleCodeListParser = new Parser(sampleCodeList);
				return true;
			}
		}	
		return false;

	}
	
	
	/** This scans the file listing the accessions, and sees if a particular entry is in the list.  The file format
	 * should be as follows:  Tab-delimited file, four columns.  
	 * First column is sample code.
	 * Second column is gene name
	 * Third column is fragment name
	 * Fourth column is GenBank accession number. 
	 * */

	public String codeIsInCodeListFile(String codeToMatch, String geneToMatch, String fragmentToMatch) {
		if (sampleCodeListParser==null)
			return null;
		if (StringUtil.blank(codeToMatch)||StringUtil.blank(geneToMatch))
			return null;
		sampleCodeListParser.setPosition(0);
		Parser subParser = new Parser();
		String line = sampleCodeListParser.getRawNextDarkLine();
		while (StringUtil.notEmpty(line)) {
			subParser.setString(line);
			MesquiteInteger pos = new MesquiteInteger(0);
			String code = StringUtil.getNextTabbedToken(line, pos);
			String gene = StringUtil.getNextTabbedToken(line, pos);
			String fragment = StringUtil.getNextTabbedToken(line, pos); 
			fragment = StringUtil.stripBoundingWhitespace(fragment);
			if (StringUtil.notEmpty(fragmentToMatch)) {
				if (codeToMatch.equalsIgnoreCase(code) && geneToMatch.equalsIgnoreCase(gene)&& fragmentToMatch.equalsIgnoreCase(fragment)) {
					return line;
				}
			}
			else if (codeToMatch.equalsIgnoreCase(code) && geneToMatch.equalsIgnoreCase(gene)) {
				return line;
			}
			line = sampleCodeListParser.getRawNextDarkLine();
		}
		return null;
	}

	public String getGenBankNumberFromCodeFileLine(String line) {
		Parser subParser = new Parser(line);
		MesquiteInteger pos = new MesquiteInteger(0);
		String code = StringUtil.getNextTabbedToken(line, pos);
		String gene = StringUtil.getNextTabbedToken(line, pos);
		String fragment = StringUtil.getNextTabbedToken(line, pos); 
		String number = StringUtil.getNextTabbedToken(line, pos); 
		number = StringUtil.stripBoundingWhitespace(number);
		return number;
	}



	
}
