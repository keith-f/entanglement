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

import com.hazelcast.core.HazelcastInstance;
import com.scalesinformatics.hazelcast.ScalesHazelcastInstanceFactory;
import org.apache.commons.cli.*;
import org.jibble.pircbot.IrcException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.cli.OptionBuilder.withLongOpt;

/**
 * @author Keith Flanagan
 */
public class EntanglementBotLauncher {

  private static final String DEFAULT_IRC_NICK_PREFIX = "ent_";
  private static final int IRC_NICK_MAX_LEN = 15;

  private static void printHelpExit(Options options)
  {
    HelpFormatter formatter = new HelpFormatter();
    String cmdSyntax = "entanglement_irc.sh";
    String header = "";
    String footer = "";
    int width = 80;
    //formatter.printHelp( "notification.sh", options );
    formatter.printHelp(width, cmdSyntax, header, options, footer);
    System.exit(0);
  }

  public static void main(String[] args) throws Exception {

    CommandLineParser parser = new PosixParser();

    Options options = new Options();
    options.addOption("i", "irc-server", true,
        "Specifies the IRC server hostname to use");

    options.addOption("p", "irc-server-password", true,
        "Specifies the IRC server password (optional)");

    options.addOption("c", "irc-channel", true,
        "The IRC channel to join on startup (optional)");

    options.addOption("h", "hz-cluster-name", true,
        "The Entanglement Hazelcast cluster");

    options.addOption(
        withLongOpt("hz-bind-addresses")
//            .withArgName("property=value" )
//            .hasArgs(2)
//            .withValueSeparator()
            .hasArgs()
            .withDescription("Specifies which network cards Hazelcast should use by specifying one or more IP ranges. "
                + "If none are specified, we attempt to use common local networks (10.*.*.*; 192.168.*.*).")
            .create( "b" ));

//    options.addOption("d", "domain-id", true,
//        "The name of the configuration domain to use.");

    options.addOption("n", "irc-nickname", true,
        "The IRC nickname to use for this bot (max length "+IRC_NICK_MAX_LEN+").");

    if (args.length == 0)
    {
      printHelpExit(options);
    }

    String ircServer = null;
    String ircPassword = null;
    String hzClusterName = null;
    String ircChannel = null;
    Set<String> hzBindAddrs = new HashSet<>();

//    String domainId = null;
    String nickname = null;

    try {
      CommandLine line = parser.parse(options, args);

      hzClusterName = line.getOptionValue("hz-cluster-name", "microbase-dev");
      if (line.hasOption("hz-bind-addresses")) {
        for (String val : line.getOptionValues("hz-bind-addresses")) {
          hzBindAddrs.add(val);
        }
      }
      ircServer = line.getOptionValue("irc-server", null);
      ircPassword = line.getOptionValue("irc-server-password", null);
      ircChannel = line.getOptionValue("irc-channel", null);

//      domainId = line.getOptionValue("domain-id", null);
      nickname = line.getOptionValue("irc-nickname", null);

    } catch (Exception e) {
      e.printStackTrace();
      printHelpExit(options);
    }

    String address = InetAddress.getLocalHost().getHostAddress();
    System.out.println("Address: "+address);
    String myHostname = InetAddress.getLocalHost().getHostName();
    System.out.println("This Hostname: " + myHostname);

    if (nickname == null) {
      // Generate a nickname if none was specified
      nickname = myHostname.contains(".") ? myHostname.substring(0, myHostname.indexOf('.')) : myHostname;
      nickname = DEFAULT_IRC_NICK_PREFIX + nickname;
//      if (nickname.length() > IRC_NICK_MAX_LEN - DEFAULT_IRC_NICK_PREFIX.length() - 3) {
//        nickname = nickname.substring(0, IRC_NICK_MAX_LEN - DEFAULT_IRC_NICK_PREFIX.length() - 3);
//      }
//      nickname = nickname+"_"+Math.round(Math.random() * 99);
      if (nickname.length() > IRC_NICK_MAX_LEN - DEFAULT_IRC_NICK_PREFIX.length()) {
        nickname = nickname.substring(0, IRC_NICK_MAX_LEN - DEFAULT_IRC_NICK_PREFIX.length());
      }
    }
    String ircNick = nickname;

    if (nickname.length() > IRC_NICK_MAX_LEN) {
      throw new EntanglementBotException("IRC nickname is too long: "+nickname
          +". Names should be "+IRC_NICK_MAX_LEN+" characters or less");
    }

    System.out.println("Connecting to Hazelcast ...");
    HazelcastInstance hzInstance = ScalesHazelcastInstanceFactory
        .createInstanceBoundToLocalNets(hzClusterName, hzClusterName);
    System.out.println("Connected to Hazelcast");

    EntanglementBot bot = connectIrc(ircServer, ircPassword, ircNick, ircChannel, hzInstance);
    System.out.println("Entanglement IRC bot started.");
  }

  private static EntanglementBot connectIrc(String ircServer, String ircPassword, String ircNick, String ircChannel,
                                         HazelcastInstance hzInstance)
      throws IOException, IrcException {
    System.out.println("IRC Nick: " + ircNick);
    System.out.println("IRC Server: " + ircServer);
    System.out.println("IRC Channel: " + ircChannel);

    EntanglementBot ircBot = new EntanglementBot(ircNick, hzInstance);
    // Enable debugging output.
    ircBot.setVerbose(true);

    // Connect to the IRC server.
    ircBot.connect(ircServer, 6667, ircPassword);

    if (ircChannel != null) {
      ircBot.joinChannel(ircChannel);
    }

    ircBot.setMessageDelay(5); // 5 MS between messages - effectively no delay
    return ircBot;
  }
}
