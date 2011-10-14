package bme.iclef.medline;

import java.util.List;

public class UpdatingDatabaseMeshFetcher extends DatabaseMeshFetcher {

    final EUtilsMeshFetcher eutils;

    public UpdatingDatabaseMeshFetcher(MedlineDatabase medline) {
	super(medline);
	eutils = new EUtilsMeshFetcher();
    }

    @Override
    public List<String> fetchMeshTerms(String pmid) {
	List<String> res = super.fetchMeshTerms(pmid);

	if (res == null || res.size() == 0) {
	    res = eutils.fetchMeshTerms(pmid);
	    if (res != null && res.size() != 0) {
		System.err.println("Updating DB...");
		medline.addMeshTermsForPubMedID(pmid, res);
	    }
	}
	return res;
    }

    public static void main(String[] args) {
	try {
	    System.out.println("TESTING");
	    MeshFetcher mf = new UpdatingDatabaseMeshFetcher(
		    new MedlineDatabase(args[0], args[1], args[2]));
	    String pmid = args.length > 3 ? args[3] : "16144449";
	    // String text = pma.getAbstractForPubMedID("16144449");
	    List<String> mesh = mf.fetchMeshTerms(pmid);
	    System.out.println(pmid + "\n" + mesh);
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
    }

}
