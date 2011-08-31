<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <!--
    Description:
    Input:
    	ImageCLEF2011 format XML (articles.xml)
    Output: 
    	custom XML without text() nodes

-->
  <xsl:output method="text" indent="no" encoding="UTF-8"/>

  <xsl:template match="graphic">
<xsl:value-of select="@id"/><xsl:text> </xsl:text><xsl:value-of select="../../@doi"/><xsl:text>&#10;</xsl:text>
  </xsl:template>

</xsl:stylesheet>
