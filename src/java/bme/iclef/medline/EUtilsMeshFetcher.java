package bme.iclef.medline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 * Fetch MeSH terms from NCBI's eutils access point.
 * 
 * @author Joerg Hakenberg
 * @author Illes Solt
 * 
 */
public class EUtilsMeshFetcher implements MeshFetcher {

    static final String URL_EUTILS_FETCHMEDLINERECORD_BYPMID = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&report=medline&mode=file&id=";
    final HttpClient httpClient;

    public EUtilsMeshFetcher() {
	httpClient = new DefaultHttpClient();
	final HttpParams httpParams = httpClient.getParams();
	HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
	HttpConnectionParams.setSoTimeout(httpParams, 3000);
    }

    /**
     * Turns to EUtils rest interface to fetch the MEDLINE record.
     * 
     * @returns list of MeSH terms or <code>null</code> on network error.
     */
    @Override
    public List<String> fetchMeshTerms(String pmid) {
	if (pmid == null)
	    throw new IllegalArgumentException("PMID shouldn't be null.");

	final String queryUrl = URL_EUTILS_FETCHMEDLINERECORD_BYPMID + pmid;

	// get the Medline entry for the specified PubMed ID
	final String content;
	try {
	    content = retrieveURL(queryUrl);
	} catch (IOException e) {
	    return null;
	}

	// Example data:
	//
	// RN - EC 2.7.11.13 (Protein Kinase C)
	// SB - IM
	// MH - Amino Acid Sequence
	// MH - Animals
	// MH - *Down-Regulation
	// MH - Enzyme Activation
	// MH - Humans
	// MH - Lung Neoplasms/enzymology/*genetics
	// MH - Molecular Sequence Data
	// MH - Protein Kinase C
	// MH - Protein-Serine-Threonine Kinases/*genetics/metabolism
	// MH - Sequence Homology, Amino Acid
	// MH - Tumor Cells, Cultured
	// EDAT- 1996/08/09
	// MHDA- 1996/08/09 00:01

	if (!content.startsWith("PMID- ")) {
	    // if (content.indexOf("Error") >= 0)
	    // throw new RuntimeException("Error fetching `" + queryUrl
	    // +"', got '" + content +"'");
	    // else
	    // throw new RuntimeException("Unknown format fetched from `" +
	    // queryUrl +"', got:\n" + content);
	    return new ArrayList<String>();
	}

	ArrayList<String> meshTerms = new ArrayList<String>();

	String[] lines = content.split("[\r\n]+");
	// get all lines starting with MH: entries with MeSH terms
	for (String line : lines) {
	    if (line.startsWith("MH  - ")) {
		String meshTerm = line.substring(6);
		if (meshTerm.isEmpty())
		    throw new IllegalStateException("failed to parse line '"
			    + line + "', got: '" + meshTerm + "'");

		meshTerms.add(meshTerm);
	    }
	}
	return meshTerms;
    }

    String retrieveURL(String url) throws IOException {

	// URLConnection connection = null;
	// connection = new URL(url).openConnection();
	// Scanner scanner = new Scanner(connection.getInputStream());
	// scanner.useDelimiter("\\Z");
	// return scanner.next();

	HttpGet httpget = new HttpGet(url);
	HttpResponse response = httpClient.execute(httpget);
	HttpEntity entity = response.getEntity();

	return EntityUtils.toString(entity);
    }

    public static void main(String[] args) {
	System.out.println("TESTING");
	MeshFetcher mf = new EUtilsMeshFetcher();
	String pmid = args.length > 0 ? args[0] : "16144449";
	List<String> mesh = mf.fetchMeshTerms(pmid);
	System.out.println(pmid + "\n" + mesh);
    }

}
