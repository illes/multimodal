package bme.iclef.medline;

import java.util.List;

/**
 * 
 * Retrieve MeSH terms from database.
 * 
 * @author Illes Solt
 * 
 */
public class DatabaseMeshFetcher implements MeshFetcher {

    final MedlineDatabase medline;

    public DatabaseMeshFetcher(MedlineDatabase medline) {

	if (medline == null)
	    throw new NullPointerException();

	this.medline = medline;
    }

    /**
     * Returns a list of MeSH terms for the given PubMed ID. <br>
     * <br>
     * Returns an empty list of {@link #medline} was not properly initialized (=
     * <tt>null</tt>).
     * 
     * @param pmid
     */
    @Override
    public List<String> fetchMeshTerms(String pmid) {
	if (pmid == null)
	    throw new NullPointerException("PMID shouldn't be null.");

	return medline.getMeshTermsForPubMedID(pmid);
    }

}
