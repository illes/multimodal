<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <!--
    Description:
    Input:
    	ImageCLEF2011 format XML (articles.xml)
    Output: 
    	custom XML without text() nodes

-->
  <xsl:output method="xml" indent="no" encoding="UTF-8"/>

  <xsl:template match="article">
    <xsl:copy>
      	<xsl:attribute name="dir"><xsl:value-of select="@fulltext-filename"/></xsl:attribute>
      	<xsl:attribute name="id"><xsl:value-of select="@url"/></xsl:attribute>
        <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="figures">
         <xsl:apply-templates select="@*|node()"/>
  </xsl:template>

  <xsl:template match="figure">
    <figure>
   	<xsl:copy-of select="@*"/>
      	<xsl:attribute name="id"><xsl:value-of select="@iri"/></xsl:attribute>
      	<xsl:attribute name="caption"><xsl:copy-of select="caption/text()"/></xsl:attribute>
    <graphic>
      	<xsl:attribute name="id"><xsl:value-of select="@iri"/></xsl:attribute>
    	<xsl:copy-of select="@*"/>
    </graphic>
    </figure>
  </xsl:template>

  <!-- catch all -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
