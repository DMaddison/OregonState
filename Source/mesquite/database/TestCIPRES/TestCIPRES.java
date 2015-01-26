package mesquite.database.TestCIPRES;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import mesquite.lib.CommandChecker;
import mesquite.lib.Debugg;
import mesquite.lib.duties.*;

public class TestCIPRES extends UtilitiesAssistant {

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		addMenuItem(null, "Test CIPRES...", makeCommand("testCIPRES", this));
		return true;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Test CIPRES", null, commandName, "testCIPRES")) {
			testCIPRes();
		}
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}

	public void testCIPRes() {
		String baseURL = "https://www.phylo.org/cipresrest/v1";
		
		String username = "DavidMaddison";
		String password = ""; // we would need to retrieve this from somewhere
		String CIPRESkey = "Mesquite-7C63884588B8438CAE456E115C9643F3";
		
		// URL for a list of jobs
		String listURL = baseURL + "/job/" + username;

		// URL for list of tools (no authentication necessary)
		String toolURL = baseURL + "/tool";

		HttpClient httpclient = HttpClientBuilder.create().build();
        
        try {
        	Debugg.println("||||||||||||||||||||||||||");
        	Debugg.println("tool URL: \n" + toolURL);
        	Debugg.println("list URL: \n" + listURL);
        	HttpGet httpget = new HttpGet(listURL); // Currently returns 404, maybe because missing cipres-appkey header
        	//HttpGet httpget = new HttpGet(toolURL); // This should return 200
            
            // Setting up the value of the Authentication header
            final String plainCreds = username + ":" + password;
            final byte[] plainCredsBytes = plainCreds.getBytes();
            final byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
            final String base64Creds = new String(base64CredsBytes);
            httpget.addHeader("Authentication", "Basic " + base64Creds);
            httpget.addHeader("cipres-appkey", CIPRESkey); //Need a value for this header
            
            Debugg.println("Executing request " + httpget.getRequestLine());
            HttpResponse response = httpclient.execute(httpget);
            try {
            	Debugg.println("-----------------RESPONSE-----------------------");
            	Debugg.println(response.getStatusLine().toString());
                // Would do something with response here...
            	Debugg.println("----------------------------------------------------");

                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
            	Debugg.println("IOException: "+ e.toString());
            	e.printStackTrace();
            } catch (Exception e) {
            	Debugg.println("Exception: "+ e.toString());
            	e.printStackTrace();
            }
        } catch (ClientProtocolException e) {
        	Debugg.println("ClientProtocolException: "+ e.toString());
       	e.printStackTrace();
        } catch (IOException e) {
        	Debugg.println("IOException 2: "+ e.toString());
        	e.printStackTrace();
        } catch (Exception e) {
        	Debugg.println("Exception 2: "+ e.toString());
        	e.printStackTrace();
        }
	}

	public String getName() {
		return "Test CIPRES connection";
	}

}
