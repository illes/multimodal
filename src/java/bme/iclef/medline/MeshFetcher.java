/**
 * 
 */
package bme.iclef.medline;

import java.util.List;

public interface  MeshFetcher {
	/**
	 * Fetch the mesh terms for a given PMID.
	 * 
	 * @param pmid
	 * @return
	 */
	public List<String> fetchMeshTerms(String pmid);
}