#include "HDFSFile.h"

#include "hadoop/SerialUtils.hh"

HDFSFile::HDFSFile (const std::string& name) 
	: 
	fileSys (0),
	hfile (0), 
	filename (name)
{
	fileSys = connectPath (filename.c_str ());

	HADOOP_ASSERT (fileSys != NULL, "failed connect to HDFS");
}

HDFSFile::~HDFSFile () {
	if (hfile) {
		hdfsCloseFile (fileSys, hfile);
		hfile = 0;
	}

	if (fileSys) {
		hdfsDisconnect (fileSys);
		fileSys = 0;
	}
}

bool HDFSFile::isOpen () {

	return ((hfile != NULL) ? true : false);
}

int64_t HDFSFile::size () {
	int64_t size = -1;
	if (fileSys) {
		hdfsFileInfo* pFileInfo = hdfsGetPathInfo (fileSys, filename.c_str());
		if (pFileInfo != NULL) {
			size = pFileInfo->mSize;
			hdfsFreeFileInfo(pFileInfo, 1);
		}
	}

	return size;
}

bool HDFSFile::openRead () {
	if (!fileSys) {
		fileSys = connectPath (filename.c_str ());
	}
	hfile = hdfsOpenFile (fileSys, filename.c_str (), O_RDONLY, 0, 0, 0);

	return ((hfile != NULL) ? true : false);
}

hdfsFS HDFSFile::connectPath (const char* uri) {
	return hdfsConnect ("default", 0);

}

int32_t HDFSFile::read (std::vector<char>& buf, int64_t pos, int32_t len) {
	if (hfile) {
		int32_t toRead = (len == -1) ? this->size () : len;
		if (buf.size () < toRead) buf.resize (toRead);
		void *buffer = &buf[0];
		int32_t read = hdfsPread (fileSys, hfile, pos, buffer, toRead); 

		return read;
	}

	return -1;
}

