package mesquite.database.lib;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.TextArea;

import mesquite.categ.lib.*;
import mesquite.chromaseq.lib.*;
import mesquite.database.SequenceExportForDatabase.*;
import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.*;

import mesquite.database.UploadSequencesToDatabase.*;

import org.dom4j.*;

//TODO: consider revising blessed and contaminated information a la base call information (i.e. as NameReference info 
// set via the Utilities menu (see )

/**
 * Utility for producing an XML-formatted Document from sequence data. Used for
 * uploading sequences to database via:
 * <ul>
 * <li>An xml file, which is uploaded through browser interface
 * {@link SequenceExportForDatabase}</li>
 * <li>Direct POST to server from within Mesquite
 * {@link UploadSequencesToDatabase}</li>
 * </ul>
 * See {@link #getXMLDocument(CharacterData, Object) getDocument} for the format
 * of this document.
 */
public class XMLForCastor {
	final static String OUTERTAG = "sequenceset";
	final static String SEQTAG = "sequence";
	final static String CHROMTAG = "chromfile";
	final static String SEQSTRINGTAG = "sequencestring";
	final static String NAMETAG = "name";
	final static String STATUSTAG = "status";
	final static String STATUS_ROUGH = "rough";
	final static String STATUS_INTERMEDIATE = "intermediate";
	final static String STATUS_FINAL = "final";
	final static String STATUS_BAD = "bad sequence";
	final static String BLESSEDTAG = "blessed";
	final static String BASECALLFIRSTTAG = "finalBaseCallFirst";
	final static String BASECALLLASTTAG = "finalBaseCallLast";
	final static String GENBANKTAG = "genbank";
	final static String CONTAMINATEDTAG = "contaminated";
	final static String CONTAMNOTESTAG = "contamination_notes";
	public boolean writeOnlySelectedTaxa = false;
	public boolean writeOnlySelectedData = false;
	protected boolean includeGaps = false;
	protected boolean contaminated = false;
	protected boolean blessed = false;
	// protected boolean allowSansChromats = false;
	protected String contaminationNotes;
	protected String status = STATUS_ROUGH;
	protected String sampleCodePrefix = "DNA";
	protected String primerStartToken = "_";

	/**
	 * If {@code data} is an instance of {@link DNAData.class}, returns an
	 * XML-formatted {@link Document}. The general format of the document is:
	 * 
	 * <pre>
	 * 
	 * {@code
	 * <sequenceset matrix = "wg from Phred/Phrap" sample_code_prefix = "DNA" delimiter = "_">
	 * 	<sequence software = "chromaseq" build = "25">
	 * 		<name>Taxon name from matrix</name>
	 * 		<status>final</status>
	 * 		<contaminated>1</contaminated>  (holds a 0 or 1)
	 * 		<contamination_notes>Notes regarding contamination</contamination_notes> (an optional field)
	 * 		<blessed>0</blessed>  (holds a 0 or 1)
	 * 		<final_base_call_first>Firstname</final_base_call_first> (optional; extracted from NameReference "finalBaseCallFirstRef")
	 * 		<final_base_call_last>Lasname</final_base_call_last> (optional; extracted from NameReference "finalBaseCallLastRef")
	 * 		<sequencestring>AAA...TTTATGGAT</sequencestring>
	 * 		<chromfile>E06_E06DNA1626_wgABR_224233.ab1</chromfile>
	 * 		<chromfile>...</chromfile> (as many as necessary)
	 * 	</sequence>
	 * 	<sequence>
	 * 	.
	 * 	.
	 * 	.
	 * 	</sequence>
	 * </sequenceset>
	 * }
	 * </pre>
	 * 
	 * @param data
	 *            the sequence data to format
	 * @param parent
	 *            the module hiring this utility. Should probably pass
	 *            containerOfModule().
	 * @return if formatting is successful, an XML-formatted Document; if
	 *         formatting unsuccessfull, returns {@code null}.
	 */
	public Document getXMLDocument(CharacterData data, Object parent){
		//first do checks to make sure data & taxa are not null, and that data is DNA
		if(data == null){
			return null;
		}
		if(data.getClass() != DNAData.class){
			return null;
		}
		Taxa taxa = data.getTaxa();
		if(taxa == null){
			return null;
		}

		if(queryOptions(parent)){
			if(writeOnlySelectedTaxa && !taxa.anySelected()){
				System.out.println("You have chosen to only upload selected taxa, but none are selected.  Upload will not proceed.");
				return null;
			} else {
				int numTaxa = taxa.getNumTaxa();
				int numChars = data.getNumChars();
				//Instantiate the Document and add a few attributes that will apply to all sequences
				Document sequencesAsXML = DocumentHelper.createDocument();
				Element sequenceSetElement = sequencesAsXML.addElement(OUTERTAG);
				sequenceSetElement.addAttribute("matrix", data.getName());
				sequenceSetElement.addAttribute("sample_code_prefix", sampleCodePrefix);
				sequenceSetElement.addAttribute("delimiter", primerStartToken);

				for(int it = 0; it < numTaxa; it++){
					if((!writeOnlySelectedTaxa || (taxa.getSelected(it))) && taxonHasData(data, it)){
						Element sequenceElement = sequenceSetElement.addElement(SEQTAG);
						int buildInt = ChromaseqUtil.getChromaseqRegistrationBuildOfMatrix((DNAData)data);
						if(buildInt > 0){
							sequenceElement.addAttribute("software", "chromaseq");
							String build = String.valueOf(buildInt);
							sequenceElement.addAttribute("build", build);

						}
						//Some handy utilities in XMLUtil that cut down on code:
						//Instead of:
						//	Element nameElement = sequenceElement.addElement(NAMETAG);
						//	nameElement.addText(getTaxonName(taxa,it));
						//We can just write:
						XMLUtil.addFilledElement(sequenceElement, NAMETAG, getTaxonName(taxa, it));

						//Do the same for status & blessed tags; although in this implementation, the same value is assigned to all sequences in the sequence set,
						//this should remain a property of each individual sequence, NOT the sequence set as a whole
						XMLUtil.addFilledElement(sequenceElement, STATUSTAG, status);
						String blessedString;
						blessedString = blessed ? "1" : "0";
						XMLUtil.addFilledElement(sequenceElement, BLESSEDTAG, blessedString);

						// See if final base call info is attached to this sequence.  Add it if so.
						// See also EditBaseCallPerson for details on how these NameReferences get assigned
						NameReference firstRef = NameReference.getNameReference("finalBaseCallFirstRef");
						Object finalBaseFirst = data.getAssociatedObject(firstRef, it);
						NameReference lastRef = NameReference.getNameReference("finalBaseCallLastRef");
						Object finalBaseLast = data.getAssociatedObject(lastRef, it);
						if (finalBaseFirst != null && finalBaseLast != null) {
							if (finalBaseFirst instanceof String && finalBaseLast instanceof String) {
								String firstString = (String) finalBaseFirst;
								String lastString = (String) finalBaseLast;
								if (firstString.length() > 0 && lastString.length() > 0) {
									XMLUtil.addFilledElement(sequenceElement, BASECALLFIRSTTAG, firstString);
									XMLUtil.addFilledElement(sequenceElement, BASECALLLASTTAG, lastString);
								}
							}
						}

						//Do the same for contaminated tag; although in this implementation, a single value is assigned to all sequences in the sequence set,
						//this should remain a property of each individual sequence, NOT the sequence set as a whole
						String contaminatedString;
						contaminatedString = contaminated ? "1" : "0"; 
						XMLUtil.addFilledElement(sequenceElement, CONTAMINATEDTAG, contaminatedString);
						
						if(contaminationNotes != null){
							XMLUtil.addFilledElement(sequenceElement, CONTAMNOTESTAG, contaminationNotes);
						}
						
						//Now need to fill in sequence
						StringBuffer sequenceBuffer = new StringBuffer(20 + numChars);
						for (int ic = 0; ic<numChars; ic++) {
							if (!writeOnlySelectedData || (data.getSelected(ic))){
								int currentSize = sequenceBuffer.length();
								boolean wroteMoreThanOneSymbol = false;
								if (data.isUnassigned(ic, it)){
									sequenceBuffer.append(getUnassignedSymbol());
								}
								else if (includeGaps || (!data.isInapplicable(ic,it))) {
									data.statesIntoStringBuffer(ic, it, sequenceBuffer, false);
									wroteMoreThanOneSymbol = sequenceBuffer.length()-currentSize>1;
								}
								if (wroteMoreThanOneSymbol) {
									System.out.println("Sorry, this data matrix can't be uploaded to the database (some character states aren't represented by a single symbol [char. " + CharacterStates.toExternal(ic) + ", taxon " + Taxon.toExternal(it) + "])");
									return null;
								}
							}
						}
						XMLUtil.addFilledElement(sequenceElement, SEQSTRINGTAG, sequenceBuffer.toString());

						//Now add chromatogram data, but only the original file name
						Associable taxaInfo = data.getTaxaInfo(false);
						String[] chromatFileNames = ChromaseqUtil.getStringsAssociated(taxaInfo, ChromaseqUtil.origReadFileNamesRef, it);
						//TODO: This would allow sequences to be uploaded even though they cannot be linked to chromatograms...
						if (chromatFileNames != null) {
							for (int ic = 0; ic < chromatFileNames.length; ic++){
								if(ic % 2 != 0){//an odd index, so it is original file name (e.g. E07_DNA3362_MSP1R_763741_055.ab1) not a renamed file name (DNA3362.g.MSP1R.ab1) see lines 486-487 in Contig.java
									XMLUtil.addFilledElement(sequenceElement, CHROMTAG, chromatFileNames[ic]);
								}
							}
						} else { // no chromat file names found for sequences
							String warning = "No chromatogram information found for " + getTaxonName(taxa, it) +
							".  Uploads require information from at least one chromatogram for each sequence " +
							"in order to make connections in database.";
							if (!MesquiteThread.isScripting()) {
								AlertDialog.notice(null, "Error", warning);
							} else {
								System.out.println(warning);
							}
							return null;
							/*
							if (!allowSansChromats) { // only sequences with chromatogram names are allowed
								String warning = "Sorry, you are attempting to upload sequences with no associated chromatograms, but 'Allow " +
										"upload for sequences without chromatograms' is set to false." +
										"\nEither restrict upload to sequences with chromatogram information stored in the current file or " +
										"try again and check the 'Allow upload for sequences without chromatograms' box Upload sequences to " +
										"database dialog.";
								if (!MesquiteThread.isScripting()) {
									AlertDialog.notice(null, "Error", warning);
								} else {
									System.out.println(warning);
								}
								return null;
							}
							*/
						}
					}// end conditional for !writeOnlySelectedTaxa or taxon is selected & has data
				}// end looping over taxa
		
				return sequencesAsXML;
			}
		} else return null;		
	}
	/*.................................................................................................................*/
	/**
	 * Queries user for various options and sets corresponding values.
	 * 
	 * @param parent
	 *            the module hiring this utility.
	 * @return {@code true} if options entered and 'OK' button pressed,
	 *         {@code false} if not
	 */
	private boolean queryOptions(Object parent) {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog uploadDialog = new ExtensibleDialog(parent,
				"Prepare sequences", buttonPressed);
		String helpString = "Choose the options for preparing sequences for local database.";
		helpString += "<br /><br />Links to specimens in the database are based on the names of associated chromatograms.  ";
		helpString += "Be sure the sample code prefix and primer start token accurately match characters used in ";
		helpString += "chromatogram file names.";
		helpString += "<br /><br />For the final base call person, the database must already have a record of this person ";
		helpString += "in the 'Persons' table.";
		uploadDialog.appendToHelpString(helpString);
		uploadDialog.addLabel("Upload options:");
		Checkbox includeGapsCheckBox = uploadDialog.addCheckBox("include gaps",
				includeGaps);
		Checkbox contaminatedCheckBox = uploadDialog.addCheckBox(
				"contaminated sequence", contaminated);
		Checkbox blessedCheckBox = uploadDialog.addCheckBox("blessed sequence",
				blessed);
		Checkbox writeSelectedTaxaBox = uploadDialog.addCheckBox(
				"Upload only selected taxa", writeOnlySelectedTaxa);
		// Checkbox allowSansChromatsBox =
		// uploadDialog.addCheckBox("Allow upload for sequences without chromatograms",
		// allowSansChromats);
		// a drop-down menu for selecting the status value
		String[] statusList = { STATUS_ROUGH, STATUS_INTERMEDIATE,
				STATUS_FINAL, STATUS_BAD };
		Choice statusChoice = uploadDialog.addPopUpMenu("Status:", statusList,
				0);

		uploadDialog.addHorizontalLine(1);

		uploadDialog.addLabel("Chromatogram file parsing rules:");
		SingleLineTextField sampleCodePrefixField = uploadDialog.addTextField(
				"Sample code prefix", sampleCodePrefix, 4, true);
		SingleLineTextField primerStartTokenField = uploadDialog.addTextField(
				"Primer start token", primerStartToken, 4, true);// just called
																	// "delimiter"
																	// on server

		uploadDialog.completeAndShowDialog(true);
		// 'OK' button pressed, so extract the values
		if (buttonPressed.getValue() == 0) {
			status = statusList[statusChoice.getSelectedIndex()];
			includeGaps = includeGapsCheckBox.getState();
			contaminated = contaminatedCheckBox.getState();
			blessed = blessedCheckBox.getState();
			writeOnlySelectedTaxa = writeSelectedTaxaBox.getState();
			// allowSansChromats = allowSansChromatsBox.getState();
			sampleCodePrefix = sampleCodePrefixField.getText();
			primerStartToken = primerStartTokenField.getText();
		}

		// only open Contamination notes dialog if contaminated was set to true
		if (contaminated) {
			MesquiteInteger contaminationButton = new MesquiteInteger(1);
			ExtensibleDialog contaminationDialog = new ExtensibleDialog(parent,
					"Contamination notes", contaminationButton);
			contaminationDialog
					.appendToHelpString("Enter any notes about the contamination.");
			contaminationDialog.addLabel("Contamination notes:");
			TextArea contaminationNotesArea = contaminationDialog.addTextArea(
					"", 4);
			contaminationDialog.completeAndShowDialog(true);
			if (contaminationButton.getValue() == 0) {
				String notes = contaminationNotesArea.getText();
				if (notes.length() > 0) {
					contaminationNotes = new String(notes);
				}
			}
		}

		uploadDialog.dispose();
		return (buttonPressed.getValue() == 0);
	}

	/*.................................................................................................................*/
	/**
	 * Checks to see if taxon {@code it} has character data.
	 * 
	 * @param data
	 *            the {@link CharacterData} object to check
	 * @param it
	 *            the taxon number to check
	 * @return {@code true} of the taxon {@code it} has at least some data (and
	 *         data that are selected if {@code  writeOnlySelectedData == true})
	 *         that are assigned and applicable; {@code false} if there are no
	 *         assigned nor applicable data for taxon {@code it} in the data
	 *         being investigated (which may be a subset of all data in
	 *         {@code CharacterData} if {@code writeOnlySelectedData == true}.
	 */
	protected boolean taxonHasData(CharacterData data, int it) {
		for (int ic = 0; ic < data.getNumChars(); ic++) {
			if (!writeOnlySelectedData || (data.getSelected(ic))) {
				if (!data.isUnassigned(ic, it) && !data.isInapplicable(ic, it))
					return true;
			}
		}
		return false;
	}
	/*.................................................................................................................*/
	/**
	 * Returns the name of taxa {@code it} in the block {@code taxa}
	 * 
	 * @return the name of the <i>it<sup>th</sup></i> taxa as a {@code String}
	 */
	protected String getTaxonName(Taxa taxa, int it) {// TODO: can add more here, if we want to try simplifying or parsing names to some rule or another
		return taxa.getTaxonName(it);// could add suffixes et al. See InterpretFasta.getExportOptions
	}
	/*.................................................................................................................*/
	/** @return N */
	protected String getUnassignedSymbol() {
		return "N";
	}
	/*.................................................................................................................*/
	public boolean isPrerelease() {
		return true;
	}
	/*.................................................................................................................*/
	public boolean isSubstantive() {
		return false;
	}
}
