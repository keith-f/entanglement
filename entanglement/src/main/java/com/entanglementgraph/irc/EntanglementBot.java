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
import com.entanglementgraph.irc.commands.*;
import com.entanglementgraph.irc.commands.cursor.*;
import com.entanglementgraph.irc.commands.graph.*;
import com.entanglementgraph.irc.commands.imageexport.ExportGephiCommand;
import com.entanglementgraph.irc.commands.imageexport.ExportJGraphXCommand;
import com.entanglementgraph.irc.commands.swing.CreateSwingCursorTrackerCommand;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.scalesinformatics.hazelcast.DefaultHazelcastConfig;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
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
public class EntanglementBot extends GenericIrcBot<EntanglementRuntime> {
  private static final String USAGE = "Usage:\n"
      + "  * Nickname\n"
      + "  * Server\n"
      + "  * Hazelcast cluster name\n"
      + "  * Channel (optional)\n";

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println(USAGE);
      System.exit(1);
    }

    String nick = args[0];
    String server = args[1];
    String hazelcastClusterName = args[2];
    String channel = null;
    if (args.length == 4) {
      channel = args[3];
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


    EntanglementBot bot = new EntanglementBot(nick, hazelcastClusterName, addresses);

    // Enable debugging output.
    bot.setVerbose(true);

    // Connect to the IRC server.
    bot.connect(server);

    if (channel != null) {
      bot.joinChannel(channel);
    }

    bot.setMessageDelay(5); // 5 MS between messages - effectively no delay
  }

  private final String hazelcastClusterName;
  private HazelcastInstance hzInstance = null;

  public EntanglementBot(String nickname, String hazelcastClusterName, String... bindAddresses)
      throws UnknownHostException {
    super(nickname);

    //Regenerate global config object
    this.hazelcastClusterName = hazelcastClusterName;
    DefaultHazelcastConfig hzConfig = new DefaultHazelcastConfig(hazelcastClusterName, hazelcastClusterName);
    if (bindAddresses.length == 0) {
      String hostname = InetAddress.getLocalHost().getHostAddress();
      hzConfig.specifyNetworkInterfaces(hostname);
    } else {
      hzConfig.specifyNetworkInterfaces(bindAddresses);
    }
    hzInstance = Hazelcast.newHazelcastInstance(hzConfig);
    createCustomUserObjectForBotState(null, getGlobalState());


    addCommand("connect-graph", ConnectGraphCommand.class);
    addCommand("create-edge", CreateEdgeCommand.class);
    addCommand("create-node", CreateNodeCommand.class);
    addCommand("export-gephi", ExportGephiCommand.class);
    addCommand("export-jgraphx", ExportJGraphXCommand.class);
    addCommand("list-edges", ListEdgesCommand.class);
    addCommand("list-connections", ListGraphConnectionsCommand.class);
    addCommand("list-nodes", ListNodesCommand.class);
    addCommand("playback-committed-log-items", PlaybackCommittedLogItemsCommand.class);
    addCommand("show-edge", ShowEdgeCommand.class);
    addCommand("show-node", ShowNodeCommand.class);
    addCommand("use", UseGraphCommand.class);

    addCommand("import-graph", ImportGraphCommand.class);

    /*
     * Cursor commands
     */
    addCommand("create-cursor", CreateCursorCommand.class);
    addCommand("list-cursors", ListGraphCursorsCommand.class);
    addCommand("use-cursor", UseCursorCommand.class);
    addCommand("cDescribe", CursorDescribe.class);
    addCommand("cGoto", CursorGoto.class);
    addCommand("cStep", CursorStepToNode.class);

    /*
     * Swing-based commands
     */
    addCommand("gui-create-cursor-tracker", CreateSwingCursorTrackerCommand.class);
  }


  @Override
  protected void createCustomUserObjectForBotState(String channel, BotState<EntanglementRuntime> newBotState) {
    if (hzInstance == null) {
      System.out.println("hzInstance is null. Skipping state creation for now.");
      return;
    }
    ClassLoader cl = EntanglementBot.class.getClassLoader();
    DbObjectMarshaller m = ObjectMarshallerFactory.create(cl);
    EntanglementRuntime runtime = new EntanglementRuntime(this, channel, cl, m, hzInstance);
    newBotState.setUserObject(runtime);
  }
}
