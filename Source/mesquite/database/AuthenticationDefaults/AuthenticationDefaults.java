package mesquite.database.AuthenticationDefaults;

import mesquite.database.lib.*;
import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.lib.ui.*;

public class AuthenticationDefaults extends DefaultsAssistant{

	public boolean startJob(String arguments, Object condition,	boolean hiredByName) {
		addMenuItemToDefaults( "Set Authentication Credentials...", makeCommand("setCredentials",  this));
		return true;
	}
	/*.................................................................................................................*/
	public void endJob(){
		super.endJob();
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(getClass(), "Sets the authorization credentials for this project session", null, commandName, "setCredentials")) {
			Projects ps = MesquiteTrunk.mesquiteTrunk.getProjectList();
			int numProjects = ps.getNumProjects();
			if(numProjects > 0){
//			if(getProject() != null){
				MesquiteInteger buttonPressed = new MesquiteInteger(1);
				ExtensibleDialog credentialsDialog = new ExtensibleDialog(containerOfModule(), "Authorization details", buttonPressed);
				credentialsDialog.addLabel("Enter credentials:");
				SingleLineTextField userNameField = credentialsDialog.addTextField("username (email)", "",30);
				SingleLineTextField passwordField = credentialsDialog.addTextField("password", "", 30);
				passwordField.setEchoChar('*');
				credentialsDialog.completeAndShowDialog(true);
				if(buttonPressed.getValue() == 0){
					String username = userNameField.getText();
					String password = passwordField.getText();
					AuthenticationElement authElement = new AuthenticationElement(username,password);
					for(int ip = 0; ip < numProjects; ip++){
						//first remove any AuthenticationElements that are already associated with project
						MesquiteProject projectI = ps.getProject(ip);
						Listable authElementsToRemove[] = projectI.getFileElements(AuthenticationElement.class);
						int numElements = authElementsToRemove.length;
						if(numElements > 0){//at least one AuthenticationElement.  Get it and remove it
							for(int ie = 0; ie < numElements; ie++){
								AuthenticationElement toRemove = (AuthenticationElement)authElementsToRemove[ie];
								projectI.removeFileElement(toRemove);
							}
						}
						//now that they've been removed, add this new AuthenticationElement
						ps.getProject(ip).addFileElement(authElement);
					}
					//getProject().addFileElement(authElement);
				}
				return (buttonPressed.getValue() == 0);
			}
			else {
				alert("Authentication credentials only apply to open projects.  Please open a project to enter credentials");
				return null;
			}
		}
		else
			return  super.doCommand(commandName, arguments, checker);
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Set Authorization Credentials";
	}
	/*.................................................................................................................*/
	public String getExplanation(){
		return "Sets authorization credentials for this project; does not save any information in the file.  Credentials expire when file is closed.";
	}

}
