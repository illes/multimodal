#!/usr/bin/env python
# -*- coding: utf-8 -*-
#   Simple script to query pubmed for a DOI
#   (c) Simon Greenhill, 2007
#   http://simon.net.nz/
import urllib
import re
from xml.dom import minidom

def get_citation_from_doi(query, email='YOUR EMAIL GOES HERE', tool='SimonsPythonQuery', database='pubmed'):
    params = {
            'db':database,
            'tool':tool,
            'email':email,
            'term':query,
            'usehistory':'y',
            'retmax':1
            }
    # try to resolve the PubMed ID of the DOI
    url = 'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?' + urllib.urlencode(params)
    data = urllib.urlopen(url).read()
    # parse XML output from PubMed...
    xmldoc = minidom.parseString(data)
    ids = xmldoc.getElementsByTagName('Id')
    # nothing found, exit
    if len(ids) == 0:
        raise "DoiNotFound"
    # get ID
    id = ids[0].childNodes[0].data
    # remove unwanted parameters
    params.pop('term')
    params.pop('usehistory')
    params.pop('retmax')
    # and add new ones...
    params['id'] = id
    params['retmode'] = 'xml'
    # get citation info:
    url = 'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?' + urllib.urlencode(params)
    data = urllib.urlopen(url).read()
    return data

def text_output(xml, id):
    """Makes a simple text output from the XML returned from efetch"""
    #print xml
    xmldoc = minidom.parseString(xml)
    terms = []
    meshterms = xmldoc.getElementsByTagName('MeshHeading')
    for meshterm in meshterms:
        descriptor = meshterm.getElementsByTagName('DescriptorName')[0]
        terms.append(descriptor.childNodes[0].data)
        #print "MESH %s" % (descriptor.childNodes[0].data) #, descriptor.attributes["id"])
        qualifiers = meshterm.getElementsByTagName('QualifierName')
        for qualifier in qualifiers:
            #print "MESH %s:%s" % (descriptor.childNodes[0].data, qualifier.childNodes[0].data) #, descriptor.attributes["id"])
            terms.append("%s:%s" % (descriptor.childNodes[0].data, qualifier.childNodes[0].data)) #, descriptor.attributes["id"])
            terms.append(":%s" % (qualifier.childNodes[0].data)) #, descriptor.attributes["id"])
            
    
    
    #title = xmldoc.getElementsByTagName('ArticleTitle')[0]
    #title = title.childNodes[0].data
    #abstract = xmldoc.getElementsByTagName('AbstractText')[0]
    #abstract = abstract.childNodes[0].data
    #authors = xmldoc.getElementsByTagName('AuthorList')[0]
    #authors = authors.getElementsByTagName('Author')
    #authorlist = []
    #for author in authors:
        #LastName = author.getElementsByTagName('LastName')[0].childNodes[0].data
        #Initials = author.getElementsByTagName('Initials')[0].childNodes[0].data
        #author = '%s, %s' % (LastName, Initials)
        #authorlist.append(author)
    #journalinfo = xmldoc.getElementsByTagName('Journal')[0]
    #journal = journalinfo.getElementsByTagName('Title')[0].childNodes[0].data
    #journalinfo = journalinfo.getElementsByTagName('JournalIssue')[0]
    #volume = journalinfo.getElementsByTagName('Volume')[0].childNodes[0].data
    #issue = journalinfo.getElementsByTagName('Issue')[0].childNodes[0].data
    #year = journalinfo.getElementsByTagName('Year')[0].childNodes[0].data
    ## this is a bit odd?
    #pages = xmldoc.getElementsByTagName('MedlinePgn')[0].childNodes[0].data
    output = []
    output.append(id)
    output.append('\t') #empty line
    output.append('|'.join([re.sub(' ', '_', t) for t in terms]))
    return ''.join(output)

if __name__ == '__main__':
    from sys import argv, exit
    if len(argv) == 1:
        print 'Usage: %s <query>' % argv[0]
        print ' e.g. %s 10.1038/ng1946' % argv[0]
        exit()
    citation = get_citation_from_doi(argv[1])
    filename = ("doi_%s.xml" % (re.sub('/', '_', argv[1])))
    FILE = open(filename,"w")
    FILE.write(citation)
    FILE.close()
    print text_output(citation, argv[1])
