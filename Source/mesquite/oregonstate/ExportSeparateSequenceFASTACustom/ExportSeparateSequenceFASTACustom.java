package mesquite.oregonstate.ExportSeparateSequenceFASTACustom;

import java.awt.Checkbox;

import mesquite.categ.lib.MolecularData;
import mesquite.chromaseq.ExportSeparateSequenceFASTA.*;
import mesquite.chromaseq.lib.ChromaseqUtil;
import mesquite.lib.Associable;
import mesquite.lib.ExporterDialog;
import mesquite.lib.IntegerField;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.NameReference;
import mesquite.lib.RadioButtons;
import mesquite.lib.SingleLineTextField;
import mesquite.lib.StringUtil;
import mesquite.lib.Taxa;
import mesquite.lib.Taxon;
import mesquite.lib.characters.CharacterData;

public class ExportSeparateSequenceFASTACustom extends ExportSeparateSequenceFASTA {
	String publication = "PUB000";
	String COIFragmentName = "COIBC";
	String CADFragmentName = "CAD4";
	static int seqID = 1;
	String prefixForTaxonName = "DRMDNA";
	
	public boolean getExportOptions(boolean dataSelected, boolean taxaSelected){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExporterDialog exportDialog = new ExporterDialog(this,containerOfModule(), "Single-sequence FASTA export [DRM Lab]", buttonPressed);
		exportDialog.setSuppressLineEndQuery(true);
		exportDialog.setDefaultButton(null);
		exportDialog.addLabel("Saving each sequence in a separate FASTA file [DRM Lab]");

		SingleLineTextField publicationField= exportDialog.addTextField("publication", publication, 8);
		SingleLineTextField prefixField= exportDialog.addTextField("Prefix for Taxon Name", prefixForTaxonName, 8);
		SingleLineTextField COIField= exportDialog.addTextField("COI Fragment", COIFragmentName, 8);
		SingleLineTextField CADField= exportDialog.addTextField("CAD Fragment", CADFragmentName, 8);
		IntegerField SeqIDField= exportDialog.addIntegerField("Starting sequence ID number", seqID, 8);

		exportDialog.completeAndShowDialog(dataSelected, taxaSelected);

		boolean ok = (exportDialog.query(dataSelected, taxaSelected)==0);

		//		convertAmbiguities = convertToMissing.getState();
		if (ok) {
			publication = publicationField.getText();
			prefixForTaxonName = prefixField.getText();
			COIFragmentName = COIField.getText();
			CADFragmentName = CADField.getText();
			int tempSeqID=SeqIDField.getValue();
			if (MesquiteInteger.isCombinable(tempSeqID))
				seqID=tempSeqID;
		}

		voucherPrefix="DRMDNA";
		buildFileName=true;
		exportDialog.dispose();
		return ok;
	}	

	public String getGenBankForTaxon (Taxa taxa, int it, CharacterData data){
		if (data==null || taxa==null)
			return "";
		Taxon taxon = data.getTaxa().getTaxon(it);
		Associable tInfo = data.getTaxaInfo(false);
		if (tInfo != null && taxon != null) {
			return (String)tInfo.getAssociatedObject(MolecularData.genBankNumberRef, it);
		}
		return "";
	}

	/*.................................................................................................................*/
	public String getIdentifierString() {
		String idString= "SEQID"+StringUtil.getIntegerAsStringWithLeadingZeros(seqID,8);
		seqID++;
		return idString;
	}
	

	/*.................................................................................................................*/
	public String getListForTaxon(Taxa taxa, int it, CharacterData data, NameReference nr, int start, int increment){
		if (data==null || taxa==null)
			return "";
		Associable as = data.getTaxaInfo(false);
		if (as==null)
			return "";
		String[] list = ChromaseqUtil.getStringsAssociated(as,nr, it);
		String s = "";

		if (list!=null) {
			for (int i=start; i<list.length; i+=increment) {
				if (i>start)
					s+=", "+list[i];
				else
					s+= list[i];
			}
			return s;
		}
		return "";
	}

	/*.................................................................................................................*/
	public String getTitleLineForTabbedFile() {
		String s = "identifier";
		s+= "\t" + "sampleCode";
		s+= "\t" + "gene";
		s+= "\t" + "fragment";
		s+= "\t" + "publication";
		s+= "\t" + "taxonName";
		s+= "\t" + "primers";
		s+= "\t" + "originalChromatogramNames";
		s+="\r";
		return s;
	}
	/*.................................................................................................................*/
	public String getLineForTabbedFile(Taxa taxa, int it, CharacterData data, int index, String voucherID, String identifierString) {
		StringBuffer sb = new StringBuffer();
		sb.append(identifierString);
		sb.append("\t"+voucherPrefix+voucherID);

		String s = ChromaseqUtil.getFragmentName(data, index);
		String geneInfo = "\t";
		if (StringUtil.notEmpty(s)) 
			geneInfo = s+"\t";
		else {
			if ("CAD1".equalsIgnoreCase(data.getName())) {
				geneInfo = "CAD\tCAD2";
			} else if ("CAD2".equalsIgnoreCase(data.getName())) {
				geneInfo = "CAD\tCAD2";
			} else if ("CAD3".equalsIgnoreCase(data.getName())) {
				geneInfo = "CAD\tCAD4";
			} else if ("CAD4".equalsIgnoreCase(data.getName())) {
				geneInfo = "CAD\tCAD4";
			} else {
				geneInfo = data.getName()+"\t";
				if ("COI".equalsIgnoreCase(data.getName()) && StringUtil.notEmpty(COIFragmentName))
					geneInfo += COIFragmentName;
				else if ("CAD".equalsIgnoreCase(data.getName()) && StringUtil.notEmpty(CADFragmentName))
					geneInfo += CADFragmentName;
			}
			sb.append("\t"+geneInfo);

		}

		if (StringUtil.notEmpty(publication)) 
			sb.append("\t"+publication);
		else
			sb.append("t");
		sb.append("\t"+taxa.getName(it));

		sb.append("\t"+getListForTaxon(taxa, it, data,ChromaseqUtil.primerForEachReadNamesRef,1,2));
		sb.append("\t"+getListForTaxon(taxa, it, data, ChromaseqUtil.origReadFileNamesRef, 1,2));

		return sb.toString()+"\r";
	}

	/*.................................................................................................................*/
	public String getFileName(Taxa taxa, int it, CharacterData data, int index, String voucherID, String identifierString) {
		String fileName="";
		
		fileName += "&v" + StringUtil.cleanseStringOfFancyChars(voucherPrefix+voucherID,false,true);

		String s = ChromaseqUtil.getFragmentName(data, index);
		if (StringUtil.notEmpty(s))   //TODO: is this correct????  DAVIDCHECK [DRM added]
			fileName += "_&g"+StringUtil.cleanseStringOfFancyChars(s,false,true);
		else {
			if ("CAD1".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCAD_&fCAD2";
			} else if ("CAD2".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCAD_&fCAD2";
			} else if ("CAD3".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCAD_&fCAD4";
			} else if ("CAD4".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCAD_&fCAD4";
			} else if ("CAD234".equalsIgnoreCase(data.getName()) || "CADAll".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCAD_&fCAD234";
			} else if ("COIBC".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCOI_&fCOIBC";
			} else if ("COIPJ".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCOI_&fCOIPJ";
			} else if ("COIAll".equalsIgnoreCase(data.getName())) {
				fileName += "_&gCOI_&fCOIAll";
			} else {
				fileName += "_&g"+StringUtil.cleanseStringOfFancyChars(data.getName(),false,true);
				if ("COI".equalsIgnoreCase(data.getName()) && StringUtil.notEmpty(COIFragmentName))
					fileName += "_&f"+StringUtil.cleanseStringOfFancyChars(COIFragmentName,false,true);
				else if ("CAD".equalsIgnoreCase(data.getName()) && StringUtil.notEmpty(CADFragmentName))
					fileName += "_&f"+StringUtil.cleanseStringOfFancyChars(CADFragmentName,false,true);
			}

		}
		String genBank = getGenBankForTaxon(taxa, it, data);
		if (StringUtil.notEmpty(genBank)) 
			fileName+="_&a"+genBank;

		if (StringUtil.notEmpty(publication)) 
			fileName+="_&p"+publication;
		fileName+="_&n"+StringUtil.cleanseStringOfFancyChars(taxa.getName(it),false,true);
		if (StringUtil.notEmpty(identifierString)) 
			fileName+="_&i"+identifierString;


		fileName += ".fas";

		return fileName;
	}
	/*.................................................................................................................*/
	public String getSequenceName(Taxa taxa, int it, String voucherID) {
		return prefixForTaxonName+voucherID;
	}

	/*.................................................................................................................*/
	public String getName() {
		return "Single-sequence FASTA Files [DRM Lab Custom]";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Exports each sequence as a separate FASTA file." ;
	}

}
