package signalJ.services;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import play.Logger;
import signalJ.SignalJPlugin;
import signalJ.models.Messages;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class GroupsActor extends AbstractActor {
    private final Map<String, Map<String, Set<UUID>>> groups = new HashMap<>();
    private final Map<String, Map<UUID, Set<String>>> usersInGroup = new HashMap<>();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();

    public GroupsActor() {
        receive(
            ReceiveBuilder.match(Messages.GroupJoin.class, groupJoin -> {
                groups.computeIfAbsent(groupJoin.hubname, k -> new HashMap<>()).computeIfAbsent(groupJoin.groupname, k -> new HashSet<>());
                groups.get(groupJoin.hubname).get(groupJoin.groupname).add(groupJoin.uuid);

                usersInGroup.computeIfAbsent(groupJoin.hubname, k -> new HashMap<>()).computeIfAbsent(groupJoin.uuid, k -> new HashSet<>());
                usersInGroup.get(groupJoin.hubname).get(groupJoin.uuid).add(groupJoin.groupname);

                Logger.debug(groupJoin.uuid + " joined group: " + groupJoin.groupname);
            }).match(Messages.GroupLeave.class, groupLeave -> {
                leaveGroup(groupLeave.hubname, groupLeave.uuid, groupLeave.groupname);
                Logger.debug(groupLeave.uuid + " left group: " + groupLeave.groupname);
            }).match(Messages.Quit.class, quit -> {
                final Set<String> hubs = getKeys(groups.keySet());
                hubs.stream().forEach(hubname -> {
                    final Set<String> groupnames = getKeys(groups.get(hubname).keySet());
                    groupnames.forEach(group -> leaveGroup(hubname, quit.uuid, group));
                });
            }).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                switch (clientFunctionCall.sendType) {
                    case All:
                    case Others:
                    case Caller:
                    case Clients:
                    case AllExcept:
                        throw new IllegalStateException("Only Groups should be handled by the Group Actor. SendType: " + clientFunctionCall.sendType);
                    case Group:
                        if (groups.containsKey(clientFunctionCall.hubName) && groups.get(clientFunctionCall.hubName).containsKey(clientFunctionCall.groupName)) {
                            signalJActor.forward(new Messages.ClientFunctionCall(
                                    clientFunctionCall.method,
                                    clientFunctionCall.hubName,
                                    null,
                                    Messages.SendType.Clients,
                                    clientFunctionCall.name,
                                    clientFunctionCall.args,
                                    groups.get(clientFunctionCall.hubName).get(clientFunctionCall.groupName).toArray(new UUID[0]),
                                    clientFunctionCall.allExcept,
                                    clientFunctionCall.groupName
                            ), getContext());
                        }
                        break;
                    case InGroupExcept:
                        final List<UUID> inGroupExcept = Arrays.asList(clientFunctionCall.allExcept);
                        if (groups.containsKey(clientFunctionCall.hubName) && groups.get(clientFunctionCall.hubName).containsKey(clientFunctionCall.groupName)) {
                            final List<UUID> sendTo = (groups.get(clientFunctionCall.hubName).get(clientFunctionCall.groupName).stream()
                                    .filter(uuid -> !inGroupExcept.contains(uuid)).collect(Collectors.toList()));
                            signalJActor.forward(new Messages.ClientFunctionCall(
                                    clientFunctionCall.method,
                                    clientFunctionCall.hubName,
                                    null,
                                    Messages.SendType.Clients,
                                    clientFunctionCall.name,
                                    clientFunctionCall.args,
                                    sendTo.toArray(new UUID[0]),
                                    clientFunctionCall.allExcept,
                                    clientFunctionCall.groupName
                            ), getContext());
                        }
                        break;
                }
            }).build()
        );
    }

    private Set<String> getKeys(Set<String> strings) {
        return strings.stream().collect(Collectors.toSet());
    }

    private void leaveGroup(String hubname, UUID uuid, String group) {
        if(groups.containsKey(hubname) && groups.get(hubname).containsKey(group)) {
            final Set<UUID> uuids = groups.get(hubname).get(group);
            uuids.remove(uuid);
            if(uuids.isEmpty()) groups.get(hubname).remove(group);
            if(groups.get(hubname).isEmpty()) groups.remove(hubname);
        }
        if (usersInGroup.containsKey(hubname) && usersInGroup.get(hubname).containsKey(uuid)) {
            final Set<String> userGroups = usersInGroup.get(hubname).get(uuid);
            userGroups.remove(group);
            if (userGroups.isEmpty()) usersInGroup.get(hubname).remove(uuid);
            if (usersInGroup.get(hubname).isEmpty()) usersInGroup.remove(hubname);
        }
    }
}