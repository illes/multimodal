package bme.iclef.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Maps
{

	public static <K,V> Map<K, V> resolveKeys(Map<K,V> m, Map<K,K> alias)
	{
		return resolveKeys(m, alias, new HashMap<K, V>(m.size()));
	}
	
	public static <K,V> Map<K, V> resolveKeys(Map<K,V> m, Map<K,K> alias, Map<K,V> target)
	{
		for (Entry<K, V> e : m.entrySet())
			target.put(resolve(alias, e.getKey()), e.getValue());
		return target;
	}
	
	public static <T> T resolve(Map<T,T> alias, T key)
	{
		T val = alias.get(key);
		return (val == null) ? key : val;
	}
	
	/**
	 * Skips comments (lines starting with #).
	 * 
	 * @param path
	 * @param charset
	 * @param delimiter
	 * @param target
	 * @throws IOException
	 */
	public static Map<String, String> readStringStringMapFromFile(
			File path, 
			Charset charset, 
			String delimiter, 
			Map<String, String> target) throws IOException
	{
		return readStringStringMapFromFile(path, charset, delimiter, target, false);
	}

	public static Map<String, String> readStringStringMapFromFile(
			File path,
			Charset charset,
			String delimiter,
			Map<String, String> target,
			boolean intern) throws IOException
	{
		return readStringStringMapFromFile(path, charset, delimiter, target, intern, 0);
	}
	
	public static Map<String, String> readStringStringMapFromFile(
		File path,
		Charset charset,
		String delimiter,
		Map<String, String> target,
		boolean intern, int skip) throws IOException
	{
		final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), charset));
		
		String line;
		int lineno = 0;
		while ((line = br.readLine()) != null)
		{
			lineno++;
			
			// skip comments
			if (line.startsWith("#") || lineno < skip + 1)
				continue;
			String[] fields = line.split(delimiter);
			if (fields.length < 2)
				throw new IllegalArgumentException("Unrecognized line '" + line +"' (expected 2 columns got " + fields.length +")");
			target.put(fields[0], intern ? fields[1].intern() : fields[1]);
		}
		return target;
	}	
}
