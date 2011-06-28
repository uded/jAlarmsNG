---
layout: documentation
title: Available channels
---

Channel - what and when?
========================

A Channel is an implementation of an <code>AlarmSender</code> interface that is capable of sending alarms via specific channel (medium, API, etc.).
It might be a SMS channel, that will send notifications via your cellular network or email channel, which will do the same as plain, old email. 
It might be something fancier, like GTalk channel. Or more complex, like JIRA channel - which will automatically create a new or reopen an existing
issue report.   


List of available channels
--------------------------

- Email channel
- Jabber channel
- MSN channel
- SMS channels: 
 - SMPP channel
 - Notifo channel
 - Clickatell channel
- Twitter channel
- Nagios channel
- Growl channel
- Bug tracking channels:
 - JIRA channel
 - Mantis channel
 - Trac channel
 - Google Code channel
 - GitHub channel
