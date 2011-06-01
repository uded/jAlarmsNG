---
layout: index
---

Goal
----
The goal of this project is to provide Java application developers with a tool to easily implement an alarm mechanism on server applications. 
Server applications usually have to be monitored constantly to check for any problems, but sometimes a condition arises (like some exception 
that you know only happens as the result of something very bad) that someone should know about ASAP. So instead of having to write and test 
a bunch of code to send an email notification or a SMS to the sysadmin, you can just use this library.

How it works
------------
This diagram shows how jAlarms works; it's very simple. Your components invoke the sendAlarm() method on the AlarmSender component, which 
in turn sends the corresponding alarm messages through its available channels. You can create your own custom channels or just use the ones 
included with the library, configuring them the way it best fits your application.

![Basic work](basic_work.png "Basic work")
 
Example
-------
Suppose you have an email account set up just to send alarm notifications. You can also have a MSN account for the same purpose. So now you 
set up an AlarmSender with a MsnChannel and a MailChannel; you inject this AlarmSender into any component that might need to send an alarm, 
and then your code could look like this:

	private AlarmSender alarmer;
	.
	.
	.
	try {
	    //your code goes here
	} catch (SomeException ex) {
	    //here goes whatever you need to do to handle SomeException
	    //then you send the alarm
	    alarmer.sendAlarm("OMG the sky is falling", null);
	}

The message OMG the sky is falling will be sent via email to the recipients configured in the MailChannel, and also to everyone in the contact 
list of the account used by the MsnChannel.

If you later have access to some other service, like a SMPP server, you can configure the SmppChannel and add it to the channels used by the 
AlarmSender, so that now the right people can receive notification via SMS on their mobile devices, and you don't have to change anything in 
the application code, just the configuration of the AlarmSender, which can be easily managed by Spring (optional).