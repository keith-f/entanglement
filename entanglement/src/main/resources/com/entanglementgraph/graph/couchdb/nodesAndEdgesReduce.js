


// Should be run with grouping = level 2
function(keys, values, rereduce) {
  if (rereduce) {
    //TODO!
    return sum(values);
  } else {
    var result = {};
    result.nodeNames = [];
    result.edgeNames = [];

    for (k=0; k<keys.length; k++) {
      // 'keys' contains elements that are a list of [original emitted key, _id]
      // eg: [["has-pillow", "j", 1, ["j", "k"]], "3db0bdb22b0a469e9bda5b03ed103a83"]
      // Here, we extract the k'th key entry, and then the first element (orig emitted key)
      var key = keys[k][0];

      var type = key[0];
      var name = key[1];
      var entryType = key[2]; //the 0 or 1 that represents a node/from edge

      if (entryType == 0) {
        var additionalNamesList = key[3]; // eg: ["j", "k"]
        for (a=0; a<additionalNamesList.length; a++) {
          if (result.nodeNames.indexOf(additionalNamesList[a]) == -1) {
            result.nodeNames.push(additionalNamesList[a]);
          }
        }

      } else if (entryType == 1) {
        var edgeType = key[3];
        var additionalNamesList = key[4]; // eg: ["j", "k"]
        for (a=0; a<additionalNamesList.length; a++) {
          if (result.edgeNames.indexOf(additionalNamesList[a]) == -1) {
            result.edgeNames.push(additionalNamesList[a]);
          }
        }
        result.edgeType = edgeType;
      }
    }
    return result;
  }
}
