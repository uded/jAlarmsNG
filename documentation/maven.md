---
layout: documentation
title: Maven
---

Use with Maven
==============

> At this point we are not supporting Maven 1 - it's deprecated anyway.

Stable releases
---------------

At this moment stable release is unavailable!
 

Snapshot, development release
-----------------------------

For snapshot release add this to pom.xml:
	
	<project>
		...
		<repositories>
			<repository>
	  			<id>jalarms-repository</id>
	  			<name>jAlarms snapshots repository</name>
				<url>http://maven.radical.com.pl/snapshots</url>
				<snapshots>
					<enabled>false</enabled>
				</snapshots>
			</repository>
		</repositories>
		...
		<dependencies>
				<dependency>
					<groupId>net.sourceforge.java-jml</groupId>
					<artifactId>jml</artifactId>
					<version>1.0-SNAPSHOTS</version>
				</dependency>
				...
		</dependencies>
		...
	</project>
