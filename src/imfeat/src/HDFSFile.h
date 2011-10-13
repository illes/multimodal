#ifndef __HDFS_FILE_H__
#define __HDFS_FILE_H__

#include <string>
#include <vector>

#include "hdfs.h"

class HDFSFile {

		HDFSFile ();
		HDFSFile (HDFSFile& hfile);
		HDFSFile& operator=(HDFSFile& hfile);
	public:
		HDFSFile (const std::string& path);
		~HDFSFile ();

		bool openRead ();
		int32_t read (std::vector<char>& buf, int64_t pos = 0, int32_t len = -1);
		bool isOpen ();
		int64_t size ();

	private:
		hdfsFS connectPath (const char* uri);

		hdfsFS fileSys;
		hdfsFile hfile;

		std::string filename;
		std::vector<char> data;
};

#endif /* __HDFS_FILE_H__ */

