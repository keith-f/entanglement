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

package com.entanglementgraph.irc.commands;

import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 14/05/2013
 * Time: 19:41
 * To change this template use File | Settings | File Templates.
 */
public class ParamParser {
  private static final String KEY_VAL_DELIM = "=";

  public static String findStringValueOf(String[] args, String paramName) {
    for (String arg : args) {
      if (arg.startsWith(paramName) &&
          arg.contains(KEY_VAL_DELIM) &&
          arg.length() > paramName.length()+KEY_VAL_DELIM.length()) {
        return arg.substring(arg.indexOf(KEY_VAL_DELIM)+1);
      }
    }
    return null;
  }

  public static Integer findIntegerValueOf(String[] args, String paramName) {
    for (String arg : args) {
      if (arg.startsWith(paramName) &&
          arg.contains(KEY_VAL_DELIM) &&
          arg.length() > paramName.length()+KEY_VAL_DELIM.length()) {
        return Integer.parseInt(arg.substring(arg.indexOf(KEY_VAL_DELIM)+1));
      }
    }
    return null;
  }

  public static Integer findIntegerValueOf(String[] args, String paramName, Integer defaultVal) {
    Integer parsed = findIntegerValueOf(args, paramName);
    return parsed != null ? parsed : defaultVal;
  }
}
