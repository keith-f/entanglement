
function(doc) {
  if(doc.nodeUpdates) {
    for (u=0; u<doc.nodeUpdates.length; u=u+1) {
      var update = doc.nodeUpdates[u];
      var node = update.node;
      if (node.keys.names && node.keys.names.length >=1) {
        var allNames = node.keys.names;
        for (i=0; i < allNames.length; i=i+1) {
          var outKey = [node.keys.type, allNames[i], 0, allNames];
          emit(outKey, null);
        }
      }
    }
  }

  if(doc.edgeUpdates) {
    for (u=0; u<doc.edgeUpdates.length; u=u+1) {
      var update = doc.edgeUpdates[u];
      var edge = update.edge;
      if (edge.keys.names && edge.keys.names.length >=1) {
        var allEdgeNames = edge.keys.names;
        var allFromNames = edge.from.names;
        for (i=0; i < allFromNames.length; i=i+1) {
          var outKey = [edge.from.type, allFromNames[i], 1, edge.keys.type, allEdgeNames];
          emit(outKey, null);
        }
      }
    }
  }
}
