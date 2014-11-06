package mesquite.database.lib;

import mesquite.lib.FileElement;

/**A class to allow authenticated session.*/
public class AuthenticationElement extends FileElement{
	protected String userName = "anonymous";
	protected String password = null;

	public AuthenticationElement (String userName, String password){
		this.userName = userName;
		this.password = password;
	}
	public void setUserName(String newName){
		userName = newName;
	}
	public String getUserName(){
		return userName;
	}
	public void setPassword(String newPass){
		password = newPass;
	}
	public String getPassword(){
		return password;
	}
	
}
