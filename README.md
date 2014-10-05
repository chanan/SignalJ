SignalJ
=======

Overview
--------

A port of [SignalR](http://asp.net/signalr) to the PlayFramework using Actors.

Examples
--------

Jump right to some examples:

* [Test page of various SignalJ functions](http://localhost:9000/test)
* [A port of the Playframework websocket-chat sample app](http://localhost:9000/chat)

Setup Instructions
------------------

Add the following to your build.sbt:

```
resolvers += "release repository" at "http://chanan.github.io/maven-repo/releases/"

resolvers += "snapshot repository" at "http://chanan.github.io/maven-repo/snapshots/"
```

Add to your libraryDependencies:

```
"signalJ" %% "signalj" % "0.2.1"
```

Instructions
------------

*Note:* This is a work in progress! These are not the full instructions

### Interface

Create an interface for the javascript functions in your page (all functions should return void). The method params can be complex types.

For example see: ChatPage.java or FirstTestPage.java in the hubs package.

### Hubs

Create a hub class that extends `Hub<TInterface>` where TInterface is the interface created above. You must also override
`getInterface()` and return the interface class.

The methods you create in the Hub class can be called from the javascript in your page with the syntax `hub_method()`.

Calling back to the page
------------------------
    
The methods of the interface you defined become methods you can call from the hub. For example, if you defined a method in your interface
    called "myMethod()":

* `clients().all.myMethod()` - executes `myMethod` on all clients
* `clients().others.myMethod()` - executes `myMethod` on all clients other than the caller
* `clients().caller.myMethod()` - executes `myMethod` on the caller
* `clients().client(conenctionId).myMethod()` - executes `myMethod` on a specific client
* `clients().allExcept.myMethod()` - executes `myMethod` on a all clients excepts the specified list of clients
    
Hub classes are instantiated on every call. Therefore, they are thread safe. Hubs can't store data unless it is in a static variable.
In that case you need to ensure thread safety of the data. A future version of SignalJ may allow Actors to be hubs.

### Groups

#### Group management

Groups can be used to group user together. Groups get created when the first user joins and get deleted when the last user leaves.

Groups can be managed server side from within the hub:

* `groups().add(getConnectionId(), group)` - Adds a connection to a group
* `groups().remove(getConnectionId(), group)` - Removes a connection to a group
    
Group membership can also be accessed from javascript (*Note:* the javascript syntax *will* change
in future versions prior to 1.0 release.):

* `groupAdd(group)` - Adds the current caller to a group
* `groupRemove(group)` - Removes the current caller to a group
    
### Group communication

You can invoke javascript functions to pages in a group with the following commands (as before calling `myMethod()`):

* `clients().group(group).myMethod()` - Invokes `myMethod` on clients that are part of the group
* `clients().group(group, getConnectionId()).myMethod()` - Invokes `myMethod` on clients that are part of the group except the listed connections
* `clients().inGroupExcept(group, getConnectionId()).myMethod()` - Alias for above syntax
* `clients().othersInGroup(group).myMethod()` - Invokes `myMethod` on clients that are part of the group other than the caller
    
Examples
--------

Examples of how hubs and page can talk to each other using group and client communication can be found in the hubs.Helloworld which
can be accessed at: [http://localhost:9000/test](http://localhost:9000/test) and hubs.Chat which can be accessed at
[http://localhost:9000/Chat](http://localhost:9000/Chat) (Which is a rewrite of the playframework websocket-chat example.</p>

Accessing the hub methods from outside the hub
----------------------------------------------

You can access the hub from outside the hub by calling `getHub` on the `GlobalHost`. You can see this for example,
in the actors.Robot which is used in the Chat example:

`HubContext<ChatPage> hub = GlobalHost.getHub(Chat.class);`

Dependency Injection
--------------------

You may use dependency injection for your hub classes. To do so, implement `DependencyResolver` (for example:
        hubs.GuiceDependencyResolver). In your app's `Global.onStart` set the resolver in the GlobalHost:

```java
GuiceDependencyResolver resolver = new GuiceDependencyResolver(injector);
GlobalHost.setDependencyResolver(resolver);
```

Future changes
--------------
    
* Add supervision
* Make sure SignalJ works in a webfarm
* Make the javascript sane
* Verify feature parity with SignalR