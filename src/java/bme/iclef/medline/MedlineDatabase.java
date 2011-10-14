package bme.iclef.medline;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Joerg Hakenberg
 * @author Illes Solt
 * 
 */
public class MedlineDatabase {

    // final String dbCitationTable = "medline_citation";
    final String dbMeshTable = "medline_mesh_heading";
    
    public static final String URL_MYSQL_LOCALHOST = "jdbc:mysql://localhost:3306/medline?characterEncoding=UTF8";

    Connection connection = null;

    // PreparedStatement titleAbstractForPmid = null;
    PreparedStatement selectMeshTermsForPmid = null;
    PreparedStatement insertMeshTermsForPmid = null;

    /**
     * 
     * @param url
     *            a database url of the form <i>jdbc:subprotocol:subname</i>, e.g., {@link URL_MYSQL_LOCALHOST}
     * @throws SQLException
     * @see {@link DriverManager.getConnection}
     */
    public MedlineDatabase(String dbUrl, String dbUser, String dbPass)
	    throws SQLException {
	this(DriverManager.getConnection(dbUrl, dbUser, dbPass));
    }

    public MedlineDatabase(Connection connection) throws SQLException {
	this.connection = connection;
	// titleAbstractForPmid = connection
	// .prepareStatement("SELECT article_title, abstract_text FROM "
	// + dbCitationTable + " WHERE pmid = ?");

	selectMeshTermsForPmid = connection
		.prepareStatement("SELECT descriptor_name FROM " + dbMeshTable
			+ " WHERE pmid = ?");
	insertMeshTermsForPmid = connection.prepareStatement("INSERT INTO "
		+ dbMeshTable + " (pmid, descriptor_name) VALUES (?, ?)");

    }

    public void close() {
	try {
	    if (null != insertMeshTermsForPmid)
		insertMeshTermsForPmid.close();
	} catch (Exception e) {
	}
	try {
	    if (null != selectMeshTermsForPmid)
		selectMeshTermsForPmid.close();
	} catch (Exception e) {
	}
	try {
	    if (connection != null)
		connection.close();
	} catch (Exception e) {
	}
    }

    /**
     * Returns MeSH terms of a PubMed citation.
     * 
     * @param pubmedID
     * @return
     */
    public List<String> getMeshTermsForPubMedID(String pubmedID) {
	ResultSet resultset = null;
	final ArrayList<String> res = new ArrayList<String>();
	try {
	    selectMeshTermsForPmid.setString(1, pubmedID);

	    resultset = selectMeshTermsForPmid.executeQuery();

	    for (resultset.first(); resultset.next();) {
		final String meshTerm = resultset.getString("descriptor_name");
		res.add(meshTerm);
	    }// end while loop

	    return res;
	} catch (SQLException sqle) {
	    System.err.println("No MeSH terms found for " + pubmedID);
	    sqle.printStackTrace();
	    return null;
	} catch (Exception e) {
	    throw new RuntimeException(e);
	} finally {
	    try {
		if (resultset != null)
		    resultset.close();
	    } catch (Exception e) {
	    }
	}
    }

    /**
     * Insert new MeSH terms into database.
     * 
     * @param pubmedID
     * @param meshTerms
     */
    public void addMeshTermsForPubMedID(String pubmedID,
	    Collection<String> meshTerms) {
	try {
        	for (String mesh : meshTerms)
        	{
                	insertMeshTermsForPmid.setLong(1, Long.parseLong(pubmedID));
                	insertMeshTermsForPmid.setString(2, mesh);
                	insertMeshTermsForPmid.addBatch();
        	}
        	int[] updateCounts = insertMeshTermsForPmid.executeBatch();
        	// TODO check update counts
	} catch (BatchUpdateException e) {
	    throw new IllegalStateException(e);
	} catch (SQLException e) {
	    throw new RuntimeException("Error inserting " + meshTerms.size() + " MeSH terms for '" + pubmedID +"'", e);
	}
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
	try {
	    System.out.println("TESTING");
	    MedlineDatabase pma = new MedlineDatabase(args[0], args[1], args[2]);
	    String pmid = args.length > 3 ? args[3] : "16144449";
	    // String text = pma.getAbstractForPubMedID("16144449");
	    List<String> mesh = pma.getMeshTermsForPubMedID(pmid);
	    System.out.println(pmid + "\n" + mesh);
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
    }

}
