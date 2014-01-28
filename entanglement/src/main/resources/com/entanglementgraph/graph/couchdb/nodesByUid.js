
function(doc) {
  if(doc.nodeUpdates) {
    for (u=0; u<doc.nodeUpdates.length; u=u+1) {
      update = doc.nodeUpdates[u];
      node = update.node;
      keys = node.keys;
      if (keys.uids) {
        for (i=0; i<keys.uids.length; i=i+1) {
          outKey = [keys.uids[i], doc.timestamp];
          //outKey = [doc.timestamp, keys.names[i]];
          emit(outKey, update);
        }
      }
    }
  }
}

