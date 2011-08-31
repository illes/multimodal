<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <!--
    Description:
    Input:
    	ImageCLEF2011 format XML (articles.xml)
    Output: 
    	custom XML without text() nodes

-->
  <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

  <xsl:template match="record">
    <article>
      	<xsl:attribute name="pmid"><xsl:value-of select="pmid/text()"/></xsl:attribute>
      	<xsl:attribute name="url"><xsl:value-of select="articleURL/text()"/></xsl:attribute>
      	<xsl:attribute name="dir"><xsl:value-of select="imageLocalName/text()"/></xsl:attribute>
    <figure>
      	<xsl:attribute name="id"><xsl:value-of select="figureID/text()"/></xsl:attribute>
      	<xsl:attribute name="caption"><xsl:copy-of select="caption/text()"/></xsl:attribute>
    <graphic>
      	<xsl:attribute name="id"><xsl:value-of select="figureID/text()"/></xsl:attribute>
    </graphic>
    </figure>
    </article>
  </xsl:template>

  <xsl:template match="imageclef">
	<articles>
    	<xsl:apply-templates select="@*|node()"/>
    </articles>
  </xsl:template>

</xsl:stylesheet>
