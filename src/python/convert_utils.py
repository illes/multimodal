def readSet (filename):
	fd = open (filename)
	idList = []
	for l in fd:
		idList.append (l.strip ())
	fd.close ()
	inSet = set (idList)
	return inSet

def readClassMap (filename):
	fd = open (filename)
	classMap = {}
	for l in fd:
		l = l.partition (" ")
		classMap[l[0]] = l[2].strip ()
	fd.close ()
	return classMap
