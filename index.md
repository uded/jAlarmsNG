---
layout: index
---

What it is and why You need this
--------------------------------
The idea is very simple - to create a simple, lightweight tool/library to ease implementation of an alarm mechanism for both server and other 
types applications. No matter what you do, you may want Your application to report any problems that requires human intervention ASAP. Server
applications are the obvious case here, since most of them have to be monitored 24/7. But You may want to built in a simple report mechanism
to other type of applications as well. So instead of making anyone write their own code or reinvent the appenders for different loggers, one
can use jAlarmsNG.

I'm not gonna lie to you - it's not new! jAlarmsNG are based on great [jAlarms](http://jalarms.sourceforge.net/) library created by 
[Enrique Zamudio Lopez](http://javamexico.org/blogs/ezamudio). As I said - it's great. But I wanted more. A lot more. And a completly different 
approach to alarms themselves. So I decided to fork his solution, clean things up and rewrite the parts I see different. So... *welcome to 
jAlarmsNG project!*

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