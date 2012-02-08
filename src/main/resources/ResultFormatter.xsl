<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="xs" version="2.0">
	<xsl:output media-type="text" encoding="UTF-8" method="text" xml:space="default"/>

	<xsl:template match="/">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="deployment_information">
		<xsl:text>Customer: </xsl:text>
		<xsl:value-of select="normalize-space(customer)"/>
		<!-- newline: -->
		<xsl:text>&#xA;</xsl:text>
		<xsl:text>Hostname: </xsl:text>
		<xsl:value-of select="normalize-space(hostname)"/>
		<!-- newline: -->
		<xsl:text>&#xA;</xsl:text>
		<xsl:text>Timestamp: </xsl:text>
		<xsl:value-of select="normalize-space(timestamp)"/>
		<!-- newline: -->
		<xsl:text>&#xA;</xsl:text>
		<xsl:text>&#xA;</xsl:text>
	</xsl:template>
	
	<xsl:template match="report">
		<xsl:value-of select="./@type"/>
		<!-- newline: -->
		<xsl:text>&#xA;</xsl:text>
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="query">
		<xsl:apply-templates/>
		<!-- newline: -->
		<xsl:text>&#xA;</xsl:text>
	</xsl:template>
	
	<xsl:template match="resultset">
		<xsl:apply-templates/>
		<!-- newline: -->
		<xsl:text>&#xA;</xsl:text>
	</xsl:template>
	
	<xsl:template match="row">
		<xsl:choose>
			<xsl:when test="count(preceding-sibling::*) = 0">
				<xsl:call-template name="headerRow"/>
				<xsl:call-template name="dataRow"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="dataRow"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="caught_exception">
		<xsl:value-of select="."/>
		<!-- newline: -->
		<xsl:text>&#xA;</xsl:text>
		<xsl:text>&#xA;</xsl:text>
	</xsl:template>
	
	<xsl:template name="headerRow">
		<xsl:variable name="columnNames" select="./column/name" as="xs:string*"/>
		<xsl:value-of select="$columnNames" separator=","/>
		<!-- newline: -->
		<xsl:text>&#xA;</xsl:text>
	</xsl:template>
	
	<xsl:template name="dataRow">
		<xsl:variable name="colValues" select="./column/value" as="xs:string*"/>
		<xsl:value-of select="$colValues" separator=","/>
		<!-- newline: -->
		<xsl:text>&#xA;</xsl:text> 
	</xsl:template>
	
</xsl:stylesheet>