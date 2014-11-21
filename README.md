SignalJ
=======

Overview
--------

A port of [SignalR](http://asp.net/signalr) to the PlayFramework using Actors.

SignalJ (SignalR) is a server to client and client to server communication framework. Communication
to browsers using a fallback mechanism. First trying websockets, then server sent events and finally long polling.
Several other clients exist such as for iOs, Android, and .net.

Examples
--------

There is a [Typesafe Activator tutorial](https://github.com/chanan/signalj-chat-java) showing how to built a chat server

Setup Instructions
------------------

Add the following to your build.sbt:

```
resolvers += "release repository" at "http://chanan.github.io/maven-repo/releases/"

resolvers += "snapshot repository" at "http://chanan.github.io/maven-repo/snapshots/"
```

Add to your libraryDependencies:

```
"signalJ" %% "signalj" % "0.5.0"
```

Documentation
------------

Documentation can be found on the [SignalJ website](http://signalj.io/)