
/*
 * A CouchDB View 'reduce' function that iterates over a set of keys/values.
 * Should be run with grouping = level 4.
 * Key items have the one of the following structures:
 * [ <type>, "U|N", <UID|Name>, [0|1], [ all UIDS ], [all Names]
 *
 * Values are either of the one of the following types:
 * TODO document each value type
 *
 * @author Keith Flanagan
 */

function(keys, values, rereduce) {
  if (rereduce) {
    return doReReduce(keys, values);
  } else {
    var result = {};

    for (k=0; k<keys.length; k++) {
      // 'keys' contains elements that are a list of [original emitted key, _id]
      // eg: [["has-pillow", "j", 1, ["j", "k"]], "3db0bdb22b0a469e9bda5b03ed103a83"]
      // Here, we extract the k'th key entry, and then the first element (orig emitted key)
      var key = keys[k][0];

      var nodeType = key[0];      // The type name of the node
      var uidOrName = key[1];     // 'U' or 'N' to indicate UID or Name follows
      var nodeUidOrName = key[2]; // Either a UID or Name depending on 'U' or 'N' above
      var entryType = key[3];     //the 0 or 1 that represents a node/from edge

      var allNodeUids = key[4];
      var allNodeNames = key[5];

      // Initialise datastructures if this is the first key
      if (k==0) {
        result.nodeUids = [];
        result.nodeNames = [];

        // Items specific to entries that tell us about nodes
        if (entryType == 0) {
          result.nodeUpdates = [];
        }

        // Items specific to entries that tell us about edges FROM a node.
        if (entryType == 1) {
          result.edgeUids = [];
          result.edgeNames = [];
          result.edgeUpdates = [];
        }
      }

      // Items common to all view entries
      combineLists(result.nodeUids, allNodeUids);
      combineLists(result.nodeNames, allNodeNames);

      // Items specific to 'node' view entries
      if (entryType == 0) {
        result.nodeUpdates.push(values[k]);
      }
      // Items specific to 'edge from node' view entries
      else if (entryType == 1) {
        result.edgeUpdates.push(values[k]);

        result.edgeType = key[6];

        var allEdgeUids = key[7];
        var allEdgeNames = key[8];
        combineLists(result.edgeUids, allEdgeUids);
        combineLists(result.edgeNames, allEdgeNames);
      }
    }
    return result;
  }
}

function doReReduce(keys, values) {
  /*
   * Here, we assume all the keys are equal (or at least, equal to the grouping level we care about).
   * What we need to do next is to merge each of the values into a big 'final' result.
   */
  var final = {};
  for (v=0; v<values.length; v++) {
    partial = values[v];

    // Combine all lists
    if (partial.nodeUids) {
      combineLists(final.nodeUids, partial.nodeUids);
    }
    if (partial.nodeNames) {
      combineLists(final.nodeNames, partial.nodeNames);
    }
    if (partial.nodeUpdates) {
      combineLists(final.nodeUpdates, partial.nodeUpdates);
    }

    if (partial.edgeUids) {
      combineLists(final.edgeUids, partial.edgeUids);
    }
    if (partial.edgeNames) {
      combineLists(final.edgeNames, partial.edgeNames);
    }
    if (partial.edgeUpdates) {
      combineLists(final.edgeUpdates, partial.edgeUpdates);
    }
  }
  return final;
}

function combineLists(list1, list2) {
  for (i=0; i<list2.length; i++) {
    if (list1.indexOf(list2[i]) == -1) {
      list1.push(list2[i]);
    }
  }
}
