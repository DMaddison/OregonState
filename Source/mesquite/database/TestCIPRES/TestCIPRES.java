package mesquite.database.TestCIPRES;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import mesquite.lib.Taxa;
import mesquite.lib.TreeVector;
import mesquite.lib.duties.*;

public class TestCIPRES extends TreeSearcher {

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}

	public boolean initialize(Taxa taxa) {
		return true;
	}

	public void fillTreeBlock(TreeVector treeList) {
		String username = "jcoliver";
		String password = ""; // we would need to retrieve this from somewhere

		String host = "www.phylo.org";
		// URL for a list of jobs
		String listURL = "https://" + host + "/cipresrest/v1/" + username + "/job";

		// URL for list of tools (no authentication necessary)
		String toolURL = "https://" + host + "/cipresrest/v1/tool";

		HttpClient httpclient = HttpClientBuilder.create().build();
        
        try {
        	//HttpGet httpget = new HttpGet(listURL); // Currently returns 404, maybe because missing cipres-appkey header
            HttpGet httpget = new HttpGet(toolURL); // This should return 200
            
            // Setting up the value of the Authentication header
            final String plainCreds = username + ":" + password;
            final byte[] plainCredsBytes = plainCreds.getBytes();
            final byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
            final String base64Creds = new String(base64CredsBytes);
            httpget.addHeader("Authentication", "Basic " + base64Creds);
            // httpget.addHeader("cipres-appkey", ""); Need a value for this header
            
            System.out.println("Executing request " + httpget.getRequestLine());
            HttpResponse response = httpclient.execute(httpget);
            try {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                // Would do something with response here...
                
                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
            	e.printStackTrace();
            } catch (Exception e) {
            	e.printStackTrace();
            }
        } catch (ClientProtocolException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}

	public String getName() {
		return "Test CIPRES connection";
	}

}
