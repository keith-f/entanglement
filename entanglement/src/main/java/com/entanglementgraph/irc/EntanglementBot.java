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
import com.scalesinformatics.uibot.GenericIrcBot;

import java.net.InetAddress;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 16:59
 * To change this template use File | Settings | File Templates.
 */
public class EntanglementBot extends GenericIrcBot {
  private static final String USAGE = "Usage:\n"
      + "  * Nickname\n"
      + "  * Server\n"
      + "  * Channel (optional)\n";

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println(USAGE);
      System.exit(1);
    }

    String nick = args[0];
    String server = args[1];
    String channel = null;
    if (args.length == 3) {
      channel = args[2];
    }

    String hostname = InetAddress.getLocalHost().getHostName();
    hostname = hostname.replace('.', '_');

//    StringTokenizer st = new StringTokenizer(InetAddress.getLocalHost().getHostName(), ".");
//    String hostname = st.nextToken();

    System.out.println("Hostname: "+hostname);
    System.out.println("Nick: "+nick);
    System.out.println("Server: "+server);
    System.out.println("Channel: "+channel);


    // Now start our bot up.
    ClassLoader classLoader = EntanglementBot.class.getClassLoader();
    EntanglementRuntime runtime = new EntanglementRuntime(
        classLoader, ObjectMarshallerFactory.create(classLoader));
    EntanglementBot bot = new EntanglementBot(nick, runtime);

    // Enable debugging output.
    bot.setVerbose(true);

    // Connect to the IRC server.
    bot.connect(server);

    if (channel != null) {
      bot.joinChannel(channel);
    }

    bot.setMessageDelay(5); // 5 MS between messages - effectively no delay
  }

//  private final ScheduledThreadPoolExecutor exe;
  private final EntanglementRuntime runtime;

  public EntanglementBot(String nickname, EntanglementRuntime runtime) {
    super(nickname, runtime);
    this.runtime = runtime;

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

  }
}
