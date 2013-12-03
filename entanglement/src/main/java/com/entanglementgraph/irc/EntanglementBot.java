/*
 * Copyright 2013 Keith Flanagan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.entanglementgraph.irc;

import com.entanglementgraph.ObjectMarshallerFactory;
import com.entanglementgraph.irc.commands.benchmarks.RunBenchmarksCommand;
import com.entanglementgraph.irc.commands.cursor.*;
import com.entanglementgraph.irc.commands.graph.*;
import com.entanglementgraph.irc.commands.imageexport.ExportGephiCommand;
import com.entanglementgraph.irc.commands.iteration.GuiNearestNeighboursCommand;
import com.entanglementgraph.irc.commands.iteration.ListKnownGraphWalkersCommand;
import com.entanglementgraph.irc.commands.iteration.RunCursorBasedGraphWalkerCommand;
import com.entanglementgraph.irc.commands.restlet.StartRestletCommand;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.scalesinformatics.hazelcast.DefaultHazelcastConfig;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.uibot.BotLoggerIrc;
import com.scalesinformatics.uibot.BotState;
import com.scalesinformatics.uibot.GenericIrcBot;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 16:59
 * To change this template use File | Settings | File Templates.
 */
public class EntanglementBot<T extends EntanglementRuntime> extends GenericIrcBot<T> {
  private static final String USAGE = "Usage:\n"
      + "  * Nickname\n"
      + "  * Server\n"
      + "  * Server password\n"
      + "  * Hazelcast cluster name\n"
      + "  * Channel (optional)\n";

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println(USAGE);
      System.exit(1);
    }

    String nick = args[0];
    String server = args[1];
    String serverPassword = args[2];
    String hazelcastClusterName = args[3];
    String channel = null;
    if (args.length == 5) {
      channel = args[4];
    }

    //FIXME this should work for now, but do this better...
    String[] addresses = new String[] {
        "192.168.*.*", "10.*.*.*", "128.240.*.*"
    };

    String address = InetAddress.getLocalHost().getHostAddress();
    System.out.println("Address: "+address);
    String hostname = InetAddress.getLocalHost().getHostName();
    hostname = hostname.replace('.', '_');

//    StringTokenizer st = new StringTokenizer(InetAddress.getLocalHost().getHostName(), ".");
//    String hostname = st.nextToken();

    System.out.println("Hostname: " + hostname);
    System.out.println("Nick: " + nick);
    System.out.println("Server: " + server);
    System.out.println("Channel: " + channel);


    EntanglementBot<EntanglementRuntime> bot = new EntanglementBot(nick, hazelcastClusterName, addresses);

    // Enable debugging output.
    bot.setVerbose(true);

    // Connect to the IRC server.
    bot.connect(server, 6667, serverPassword);

    if (channel != null) {
      bot.joinChannel(channel);
    }

    bot.setMessageDelay(5); // 5 MS between messages - effectively no delay
  }

  private final String hazelcastClusterName;
  protected HazelcastInstance hzInstance = null;

  public EntanglementBot(String nickname, String hazelcastClusterName, String... bindAddresses)
      throws UnknownHostException {
    super(nickname);

    //Regenerate global config object
    this.hazelcastClusterName = hazelcastClusterName;
    DefaultHazelcastConfig hzConfig = new DefaultHazelcastConfig(hazelcastClusterName, hazelcastClusterName);
    hzConfig.setProperty("hazelcast.rest.enabled", "true");
    if (bindAddresses.length == 0) {
      String hostname = InetAddress.getLocalHost().getHostAddress();
      hzConfig.specifyNetworkInterfaces(hostname);
    } else {
      hzConfig.specifyNetworkInterfaces(bindAddresses);
    }
    hzInstance = Hazelcast.newHazelcastInstance(hzConfig);
    createCustomUserObjectForBotState(null, getGlobalState());


    addCommand("/entanglement/db/connect-mongodb-pool", ConnectMongoDbClusterCommand.class);
    addCommand("/entanglement/db/connect-graph", ConnectGraphCommand.class);
    addCommand("/entanglement/db/use-graph", UseGraphCommand.class);
    addCommand("/entanglement/db/list-connections", ListGraphConnectionsCommand.class);
    addCommand("/entanglement/db/playback-committed-log-items", PlaybackCommittedLogItemsCommand.class);
    addCommand("/entanglement/graph/create-edge", CreateEdgeCommand.class);
    addCommand("/entanglement/graph/create-node", CreateNodeCommand.class);
    addCommand("/entanglement/graph/export-gephi", ExportGephiCommand.class);
    addCommand("/entanglement/graph/list-edges", ListEdgesCommand.class);
    addCommand("/entanglement/graph/list-nodes", ListNodesCommand.class);
    addCommand("/entanglement/graph/show-edge", ShowEdgeCommand.class);
    addCommand("/entanglement/graph/show-node", ShowNodeCommand.class);


    addCommand("/entanglement/graph/import-graph", ImportGraphCommand.class);

    /*
     * Cursor commands
     */
    addCommand("/entanglement/cursor/create", CreateCursorCommand.class);
    addCommand("/entanglement/cursor/list", ListGraphCursorsCommand.class);
    addCommand("/entanglement/cursor/use", UseCursorCommand.class);
    addCommand("/entanglement/cursor/describe", CursorDescribe.class);
    addCommand("/entanglement/cursor/goto", CursorGoto.class);
    addCommand("/entanglement/cursor/step", CursorStepToNode.class);

    /*
     * REST services
     */
    addCommand("/entanglement/rest/start-server", StartRestletCommand.class);

    /*
     * Swing-based commands
     */
//    addCommand("gui-cursor-display-nearest-neighbours", CreateSwingCursorNearestNeighboursCommand.class);
//    addCommand("gui-display-entire-graph", CreateSwingGuiEntireGraphCommand.class);

    addCommand("/entanglement/visualise/nearest-neighbours", GuiNearestNeighboursCommand.class);
    addCommand("/entanglement/analyse/run-graph-walker", RunCursorBasedGraphWalkerCommand.class);
    addCommand("/entanglement/analyse/list-walkers", ListKnownGraphWalkersCommand.class);

    addCommand("/entanglement/benchmarks/run-benchmarks", RunBenchmarksCommand.class);
  }


  @Override
  protected void createCustomUserObjectForBotState(String channel, BotState newBotState) {
    if (hzInstance == null) {
      System.out.println("hzInstance is null. Skipping state creation for now.");
      return;
    }
    ClassLoader cl = EntanglementBot.class.getClassLoader();
    DbObjectMarshaller m = ObjectMarshallerFactory.create(cl);
    BotLogger logger = new BotLoggerIrc(this, channel, EntanglementBot.class.getSimpleName());
    EntanglementRuntime runtime = new EntanglementRuntime(logger, newBotState, cl, m, hzInstance);
    newBotState.setUserObject(runtime);
  }
}
