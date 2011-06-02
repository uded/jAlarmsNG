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

For snapshot release add this to pom.xml under `<project>`, under `<repositories>`
	
	<repository>
	  <id>jalarms-repository</id>
	  <url>http://maven.radical.com.pl/snapshots</url>
	</repository>	

Under `<project>`, under `<dependencies>`

	<dependency>
	  <groupId>net.sourceforge.java-jml</groupId>
	  <artifactId>jml</artifactId>
	  <version>1.0-SNAPSHOTS</version>
	</dependency>