package bme.iclef.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class CSVMerge {
	
	
	public static void main(String[] args) {

		
		if(args.length == 0)
		{
			System.err.println("Usage: java ... <id_data1.csv> [<id_data2.csv> ...]");
			System.exit(2);
		}
		try {
			String field = null;
			boolean noMissingLabel = false;		
			Map<String, List<String>> data = new LinkedHashMap<String, List<String>>();
			List<String> outputColumnNames = new ArrayList<String>();
			for (String csvFileName : args) {
				
				if ("--no-missing-label".equals(csvFileName))
				{
					noMissingLabel = true;
					continue;
				}
				CSVReader csv = new CSVReader(new BufferedReader(new FileReader(csvFileName)));
				
				// extract column names
				final String[] columnNames = csv.readNext();
				if (columnNames == null)
					throw new IllegalArgumentException("File " + csvFileName + " empty.");
				
				// look for the join column
				if (field == null)
				{
					field = columnNames[0];
					outputColumnNames.add(field);
				}
				else if (!field.equals(columnNames[0]))
					throw new IllegalStateException("In file " + csvFileName + " expected first column " + field +" got " + columnNames[0]);

				// append column names
				for (int i = 1; i < columnNames.length; i++)
					outputColumnNames.add(columnNames[i]);

				Set<String> seen = new HashSet<String>();
				
				// process rows
				String[] row;
				while ((row = csv.readNext()) != null)
				{
					// get or create a new entry
					final String key = row[0];
					if (!seen.add(key))
					{
						System.err.println("WARNING: Duplicate '" + field + "' value '" + key +"' in file '"+ csvFileName + "', skipping.");
						continue;
					}
					if (!data.containsKey(key))
					{
						List<String> value = new ArrayList<String>(outputColumnNames.size());
						value.add(key);
						data.put(key, value);
					}
					final List<String> entry =  data.get(key);
					
					// pad entry
					for (int i = entry.size(); i < outputColumnNames.size() - columnNames.length + 1; i++) {
						entry.add(null);
					}
					
					// append data, skip key
					for (int i = 1; i < row.length; i++)
					{
						entry.add(row[i]);
						if (row[i].contains("cluster_"))
							throw new IllegalStateException(Arrays.toString(row));
					}
				}
				csv.close();
			}
			
			CSVWriter out = new CSVWriter(new PrintWriter(System.out));
			String[] row = outputColumnNames.toArray(new String[outputColumnNames.size()]);
			out.writeNext(row);
			for (List<String> entry : data.values())
			{
				Arrays.fill(row, null);
				row = entry.toArray(row);
				if (!noMissingLabel || (row[row.length-1] != null && row[row.length-1].length() > 0))
					out.writeNext(row);
			}
			out.close();
		} catch (Exception e) {
			System.err.println("FATAL: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

}
