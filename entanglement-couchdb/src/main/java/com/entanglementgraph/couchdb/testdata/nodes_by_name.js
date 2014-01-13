
function(doc) {
  if(doc.nodeUpdates) {
    for (u=0; u<doc.nodeUpdates.length; u=u+1) {
      entry = doc.nodeUpdates[u];
      node = entry.node;
      keys = node.keys;
      if (keys.names) {
        for (i=0; i<keys.names.length; i=i+1) {
          outKey = [keys.names[i], doc.timestamp];
          //outKey = [doc.timestamp, keys.names[i]];
          emit(outKey, entry.node);
        }
      }
    }
  }
}

