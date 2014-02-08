
function(doc) {
  if(doc.nodeUpdates) {
    for (u=0; u<doc.nodeUpdates.length; u=u+1) {
      update = doc.nodeUpdates[u];
      node = update.node;
      keys = node.keys;
      if (keys.names && keys.names.length >=1) {
        for (int i=0; i<keys.names.length; i=i+1) {
          outKey = [keys.type, keys.names[i]];
          emit(outKey, keys.names);
        }
      }
    }
  }
}

// If grouping == exact:
function(keys, values, rereduce) {
  return values[0];
}

//Works(!)
function(keys, values, rereduce) {
  if (rereduce) {
    //return sum(values);
    return "foo";
  } else {
    var set = [];
    //console.log("foo");
    for (i=0; i<values.length; i=i+1) {
      names = values[i];
      for (j=0; j<names.length; j=j+1) {
        if ( set.indexOf(names[j]) < 0) {
          set.push(names[j]);
          set.push(set.length);
        }
      }
    }
    return set;
  }
}

function(keys, values, rereduce) {
  if (rereduce) {
    //return sum(values);
    return "foo";
  } else {
    var set = Object.create(null); // create an object with no properties
    //console.log("foo");
    for (i=0; i<values.length; i=i+1) {
      vals2 = values[i];
      for (j=0; j<vals2[j].length; j=j+1) {
        set[vals2[j]] = true; // add the item if it is not already present
      }
    }
    return set;
  }
}

function(keys, values, rereduce) {
  if (rereduce) {
    return sum(values);
  } else {
    return values.length;
  }
}
