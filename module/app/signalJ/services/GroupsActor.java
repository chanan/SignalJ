package signalJ.services;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import play.Logger;
import signalJ.SignalJPlugin;

import java.util.*;

public class GroupsActor extends AbstractActor {
    private final Map<String, List<UUID>> groups = new HashMap<>();
    private final Map<UUID, List<String>> usersInGroup = new HashMap<>();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();

    public GroupsActor() {
        receive(
                ReceiveBuilder.match(
                        SignalJActor.GroupJoin.class, groupJoin -> {
                            if (!groups.containsKey(groupJoin.groupname)) {
                                groups.put(groupJoin.groupname, new ArrayList<UUID>());
                            }
                            final List<UUID> uuids = groups.get(groupJoin.groupname);
                            if (!uuids.contains(groupJoin.uuid)) {
                                uuids.add(groupJoin.uuid);
                            }
                            if (!usersInGroup.containsKey(groupJoin.uuid)) {
                                usersInGroup.put(groupJoin.uuid, new ArrayList<String>());
                            }
                            final List<String> userGroups = usersInGroup.get(groupJoin.uuid);
                            if (!userGroups.contains(groupJoin.groupname)) {
                                userGroups.add(groupJoin.groupname);
                            }
                            Logger.debug(groupJoin.uuid + " joined group: " + groupJoin.groupname);
                        }
                ).match(
                        SignalJActor.GroupLeave.class, groupLeave -> {
                            leaveGroup(groupLeave.uuid, groupLeave.groupname);
                            if (usersInGroup.containsKey(groupLeave.uuid)) {
                                final List<String> userGroups = usersInGroup.get(groupLeave.uuid);
                                userGroups.remove(groupLeave.groupname);
                                if (userGroups.isEmpty()) usersInGroup.remove(groupLeave.uuid);
                            }
                            Logger.debug(groupLeave.uuid + " left group: " + groupLeave.groupname);
                        }
                ).match(
                        SignalJActor.Quit.class, quit -> {
                            if (usersInGroup.containsKey(quit.uuid)) {
                                final List<String> userGroups = usersInGroup.get(quit.uuid);
                                for (String group : userGroups) {
                                    leaveGroup(quit.uuid, group);
                                }
                                usersInGroup.remove(quit.uuid);
                            }
                        }
                ).match(HubActor.ClientFunctionCall.class, clientFunctionCall -> {
                    switch (clientFunctionCall.sendType) {
                        case All:
                        case Others:
                        case Caller:
                        case Clients:
                        case AllExcept:
                            throw new IllegalStateException("Only Groups should be handled by the Group Actor. SendType: " + clientFunctionCall.sendType);
                        case Group:
                            if (groups.containsKey(clientFunctionCall.groupName)) {
                                signalJActor.forward(new HubActor.ClientFunctionCall(
                                        clientFunctionCall.method,
                                        clientFunctionCall.hubName,
                                        null,
                                        HubActor.ClientFunctionCall.SendType.Clients,
                                        clientFunctionCall.name,
                                        clientFunctionCall.args,
                                        groups.get(clientFunctionCall.groupName).toArray(new UUID[0]),
                                        clientFunctionCall.allExcept,
                                        clientFunctionCall.groupName
                                ), getContext());
                            }
                            break;
                        case InGroupExcept:
                            final List<UUID> inGroupExcept = Arrays.asList(clientFunctionCall.allExcept);
                            if (groups.containsKey(clientFunctionCall.groupName)) {
                                final List<UUID> sendTo = new ArrayList<>();
                                for (final UUID uuid : groups.get(clientFunctionCall.groupName)) {
                                    if (!inGroupExcept.contains(uuid)) sendTo.add(uuid);
                                }
                                signalJActor.forward(new HubActor.ClientFunctionCall(
                                        clientFunctionCall.method,
                                        clientFunctionCall.hubName,
                                        null,
                                        HubActor.ClientFunctionCall.SendType.Clients,
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

    private void leaveGroup(final UUID uuid, final String group) {
        if(groups.containsKey(group)) {
            final List<UUID> uuids = groups.get(group);
            uuids.remove(uuid);
            if(uuids.isEmpty()) groups.remove(group);
        }
    }
}