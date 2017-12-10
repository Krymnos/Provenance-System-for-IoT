'use strict';

var _ = require('lodash');
var debug = require('debug')('express-cassandra');

var JanusGraphBuilder = function f(client) {
  this._client = client;
};

JanusGraphBuilder.prototype = {
  create_graph(graphName, callback) {
    debug('creating janus graph: %s', graphName);
    var script = `
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("storage.backend", "cassandrathrift");
      map.put("storage.hostname", cassandraHosts);
      map.put("storage.port", cassandraPort);
      map.put("index.search.backend", "elasticsearch");
      map.put("index.search.hostname", elasticHosts);
      map.put("index.search.port", elasticPort);
      map.put("graph.graphname", graphName);
      ConfiguredGraphFactory.createConfiguration(new MapConfiguration(map));
      ConfiguredGraphFactory.open(graphName).vertices().size();
    `;
    var bindings = {
      cassandraHosts: '127.0.0.1',
      cassandraPort: 9160,
      elasticHosts: '127.0.0.1',
      elasticPort: 9200,
      graphName
    };
    this._client.execute(script, bindings, function (err, results) {
      if (err) {
        callback(err);
        return;
      }

      callback(null, results);
    });
  },

  check_graph_exist(graphName, callback) {
    debug('check for janus graph: %s', graphName);
    var script = `
      ConfiguredGraphFactory.getGraphNames();
    `;
    var bindings = {};
    this._client.execute(script, bindings, function (err, results) {
      if (err) {
        callback(err);
        return;
      }

      if (_.isArray(results) && results.includes(graphName)) {
        callback(null, true);
        return;
      }
      callback(null, false);
    });
  },

  assert_graph(graphName, callback) {
    var _this = this;

    this.check_graph_exist(graphName, function (err, exist) {
      if (err) {
        callback(err);
        return;
      }

      if (!exist) {
        _this.create_graph(graphName, callback);
        return;
      }

      callback();
    });
  },

  drop_graph(graphName, callback) {
    debug('removing janus graph: %s', graphName);
    var script = `
      ConfiguredGraphFactory.drop(graphName);
    `;
    var bindings = {
      graphName
    };
    this._client.execute(script, bindings, function (err, results) {
      if (err) {
        callback(err);
        return;
      }

      callback(null, results);
    });
  },

  put_indexes(graphName, mappingName, indexes, callback) {
    debug('syncing janus graph indexes for: %s', mappingName);
    var script = `
      graph = ConfiguredGraphFactory.open(graphName);
      graph.tx().commit();
      mgmt = graph.openManagement();
    `;
    var bindings = {
      graphName
    };
    // create indexes if not exist
    Object.keys(indexes).forEach(function (index) {
      if (indexes[index].type === 'Composite') {
        script += `if (!mgmt.containsGraphIndex('${index}')) mgmt.buildIndex('${index}', Vertex.class)`;
        indexes[index].keys.forEach(function (key) {
          script += `.addKey(mgmt.getPropertyKey('${key}'))`;
        });
        script += `.indexOnly(mgmt.getVertexLabel('${mappingName}'))`;
        if (indexes[index].unique) {
          script += '.unique()';
        }
        script += '.buildCompositeIndex();';
      } else if (indexes[index].type === 'Mixed') {
        script += `if (!mgmt.containsGraphIndex('${index}')) mgmt.buildIndex('${index}', Vertex.class)`;
        indexes[index].keys.forEach(function (key) {
          script += `.addKey(mgmt.getPropertyKey('${key}'))`;
        });
        script += `.indexOnly(mgmt.getVertexLabel('${mappingName}'))`;
        if (indexes[index].unique) {
          script += '.unique()';
        }
        script += '.buildMixedIndex("search");';
      } else if (indexes[index].type === 'VertexCentric') {
        script += `relationLabel = mgmt.getEdgeLabel('${indexes[index].label}');`;
        script += `if (!mgmt.containsRelationIndex(relationLabel, '${index}')) mgmt.buildEdgeIndex(relationLabel, '${index}', Direction.${indexes[index].direction}, Order.${indexes[index].order}`;
        indexes[index].keys.forEach(function (key) {
          script += `, mgmt.getPropertyKey('${key}')`;
        });
        script += ');';
      }
    });
    script += 'mgmt.commit();';
    // await index for registered or enabled status
    Object.keys(indexes).forEach(function (index) {
      if (indexes[index].type === 'Composite') {
        script += `mgmt.awaitGraphIndexStatus(graph, '${index}').status(SchemaStatus.REGISTERED, SchemaStatus.ENABLED).call();`;
      } else if (indexes[index].type === 'Mixed') {
        script += `mgmt.awaitGraphIndexStatus(graph, '${index}').status(SchemaStatus.REGISTERED, SchemaStatus.ENABLED).call();`;
      } else if (indexes[index].type === 'VertexCentric') {
        script += `mgmt.awaitRelationIndexStatus(graph, '${index}', '${indexes[index].label}').status(SchemaStatus.REGISTERED, SchemaStatus.ENABLED).call();`;
      }
    });
    // enable index if in registered state
    script += 'mgmt = graph.openManagement();';
    Object.keys(indexes).forEach(function (index) {
      if (indexes[index].type === 'Composite') {
        script += `if (mgmt.getGraphIndex('${index}').getIndexStatus(mgmt.getPropertyKey('${indexes[index].keys[0]}')).equals(SchemaStatus.REGISTERED)) mgmt.updateIndex(mgmt.getGraphIndex('${index}'), SchemaAction.ENABLE_INDEX);`;
      } else if (indexes[index].type === 'Mixed') {
        script += `if (mgmt.getGraphIndex('${index}').getIndexStatus(mgmt.getPropertyKey('${indexes[index].keys[0]}')).equals(SchemaStatus.REGISTERED)) mgmt.updateIndex(mgmt.getGraphIndex('${index}'), SchemaAction.ENABLE_INDEX);`;
      } else if (indexes[index].type === 'VertexCentric') {
        script += `if (mgmt.getRelationIndex(mgmt.getEdgeLabel('${indexes[index].label}'), '${index}').getIndexStatus().equals(SchemaStatus.REGISTERED)) mgmt.updateIndex(mgmt.getRelationIndex(mgmt.getEdgeLabel('${indexes[index].label}'), '${index}'), SchemaAction.ENABLE_INDEX);`;
      }
    });
    script += 'mgmt.commit();';
    // await index for enabled status
    Object.keys(indexes).forEach(function (index) {
      if (indexes[index].type === 'Composite') {
        script += `mgmt.awaitGraphIndexStatus(graph, '${index}').status(SchemaStatus.ENABLED).call();`;
      } else if (indexes[index].type === 'Mixed') {
        script += `mgmt.awaitGraphIndexStatus(graph, '${index}').status(SchemaStatus.ENABLED).call();`;
      } else if (indexes[index].type === 'VertexCentric') {
        script += `mgmt.awaitRelationIndexStatus(graph, '${index}', '${indexes[index].label}').status(SchemaStatus.ENABLED).call();`;
      }
    });
    this._client.execute(script, bindings, function (err, results) {
      if (err) {
        callback(err);
        return;
      }

      callback(null, results);
    });
  },

  put_mapping(graphName, mappingName, mappingBody, callback) {
    var _this2 = this;

    debug('syncing janus graph mapping: %s', mappingName);
    var script = `
      graph = ConfiguredGraphFactory.open(graphName);
      graph.tx().commit();
      mgmt = graph.openManagement();
      if (!mgmt.containsVertexLabel(mappingName)) mgmt.makeVertexLabel(mappingName).make();
    `;
    var bindings = {
      graphName,
      mappingName
    };
    Object.keys(mappingBody.relations).forEach(function (relation) {
      script += `
        if (!mgmt.containsEdgeLabel('${relation}')) mgmt.makeEdgeLabel('${relation}').multiplicity(${mappingBody.relations[relation]}).make();
      `;
    });
    Object.keys(mappingBody.properties).forEach(function (property) {
      script += `
        if (!mgmt.containsPropertyKey('${property}')) mgmt.makePropertyKey('${property}').dataType(${mappingBody.properties[property].type}.class).cardinality(Cardinality.${mappingBody.properties[property].cardinality}).make();
      `;
    });
    script += 'mgmt.commit();';
    this._client.execute(script, bindings, function (err, results) {
      if (err) {
        callback(err);
        return;
      }

      if (Object.keys(mappingBody.indexes).length > 0) {
        _this2.put_indexes(graphName, mappingName, mappingBody.indexes, callback);
        return;
      }

      callback(null, results);
    });
  }
};

module.exports = JanusGraphBuilder;
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9idWlsZGVycy9qYW51c2dyYXBoLmpzIl0sIm5hbWVzIjpbIl8iLCJyZXF1aXJlIiwiZGVidWciLCJKYW51c0dyYXBoQnVpbGRlciIsImYiLCJjbGllbnQiLCJfY2xpZW50IiwicHJvdG90eXBlIiwiY3JlYXRlX2dyYXBoIiwiZ3JhcGhOYW1lIiwiY2FsbGJhY2siLCJzY3JpcHQiLCJiaW5kaW5ncyIsImNhc3NhbmRyYUhvc3RzIiwiY2Fzc2FuZHJhUG9ydCIsImVsYXN0aWNIb3N0cyIsImVsYXN0aWNQb3J0IiwiZXhlY3V0ZSIsImVyciIsInJlc3VsdHMiLCJjaGVja19ncmFwaF9leGlzdCIsImlzQXJyYXkiLCJpbmNsdWRlcyIsImFzc2VydF9ncmFwaCIsImV4aXN0IiwiZHJvcF9ncmFwaCIsInB1dF9pbmRleGVzIiwibWFwcGluZ05hbWUiLCJpbmRleGVzIiwiT2JqZWN0Iiwia2V5cyIsImZvckVhY2giLCJpbmRleCIsInR5cGUiLCJrZXkiLCJ1bmlxdWUiLCJsYWJlbCIsImRpcmVjdGlvbiIsIm9yZGVyIiwicHV0X21hcHBpbmciLCJtYXBwaW5nQm9keSIsInJlbGF0aW9ucyIsInJlbGF0aW9uIiwicHJvcGVydGllcyIsInByb3BlcnR5IiwiY2FyZGluYWxpdHkiLCJsZW5ndGgiLCJtb2R1bGUiLCJleHBvcnRzIl0sIm1hcHBpbmdzIjoiOztBQUFBLElBQU1BLElBQUlDLFFBQVEsUUFBUixDQUFWO0FBQ0EsSUFBTUMsUUFBUUQsUUFBUSxPQUFSLEVBQWlCLG1CQUFqQixDQUFkOztBQUVBLElBQU1FLG9CQUFvQixTQUFTQyxDQUFULENBQVdDLE1BQVgsRUFBbUI7QUFDM0MsT0FBS0MsT0FBTCxHQUFlRCxNQUFmO0FBQ0QsQ0FGRDs7QUFJQUYsa0JBQWtCSSxTQUFsQixHQUE4QjtBQUM1QkMsZUFBYUMsU0FBYixFQUF3QkMsUUFBeEIsRUFBa0M7QUFDaENSLFVBQU0sMEJBQU4sRUFBa0NPLFNBQWxDO0FBQ0EsUUFBTUUsU0FBVTs7Ozs7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LEVBQW9DWixPQUFwQyxDQUE0QyxVQUFDYSxRQUFELEVBQWM7QUFDeERqQyxnQkFBVzt5Q0FDd0JpQyxRQUFTLDZCQUE0QkEsUUFBUyxlQUFjSixZQUFZRyxVQUFaLENBQXVCQyxRQUF2QixFQUFpQ1gsSUFBSyxtQ0FBa0NPLFlBQVlHLFVBQVosQ0FBdUJDLFFBQXZCLEVBQWlDQyxXQUFZO09BRHBOO0FBR0QsS0FKRDtBQUtBbEMsY0FBVSxnQkFBVjtBQUNBLFNBQUtMLE9BQUwsQ0FBYVcsT0FBYixDQUFxQk4sTUFBckIsRUFBNkJDLFFBQTdCLEVBQXVDLFVBQUNNLEdBQUQsRUFBTUMsT0FBTixFQUFrQjtBQUN2RCxVQUFJRCxHQUFKLEVBQVM7QUFDUFIsaUJBQVNRLEdBQVQ7QUFDQTtBQUNEOztBQUVELFVBQUlXLE9BQU9DLElBQVAsQ0FBWVUsWUFBWVosT0FBeEIsRUFBaUNrQixNQUFqQyxHQUEwQyxDQUE5QyxFQUFpRDtBQUMvQyxlQUFLcEIsV0FBTCxDQUFpQmpCLFNBQWpCLEVBQTRCa0IsV0FBNUIsRUFBeUNhLFlBQVlaLE9BQXJELEVBQThEbEIsUUFBOUQ7QUFDQTtBQUNEOztBQUVEQSxlQUFTLElBQVQsRUFBZVMsT0FBZjtBQUNELEtBWkQ7QUFhRDtBQTlNMkIsQ0FBOUI7O0FBaU5BNEIsT0FBT0MsT0FBUCxHQUFpQjdDLGlCQUFqQiIsImZpbGUiOiJqYW51c2dyYXBoLmpzIiwic291cmNlc0NvbnRlbnQiOlsiY29uc3QgXyA9IHJlcXVpcmUoJ2xvZGFzaCcpO1xuY29uc3QgZGVidWcgPSByZXF1aXJlKCdkZWJ1ZycpKCdleHByZXNzLWNhc3NhbmRyYScpO1xuXG5jb25zdCBKYW51c0dyYXBoQnVpbGRlciA9IGZ1bmN0aW9uIGYoY2xpZW50KSB7XG4gIHRoaXMuX2NsaWVudCA9IGNsaWVudDtcbn07XG5cbkphbnVzR3JhcGhCdWlsZGVyLnByb3RvdHlwZSA9IHtcbiAgY3JlYXRlX2dyYXBoKGdyYXBoTmFtZSwgY2FsbGJhY2spIHtcbiAgICBkZWJ1ZygnY3JlYXRpbmcgamFudXMgZ3JhcGg6ICVzJywgZ3JhcGhOYW1lKTtcbiAgICBjb25zdCBzY3JpcHQgPSBgXG4gICAgICBNYXA8U3RyaW5nLCBPYmplY3Q+IG1hcCA9IG5ldyBIYXNoTWFwPFN0cmluZywgT2JqZWN0PigpO1xuICAgICAgbWFwLnB1dChcInN0b3JhZ2UuYmFja2VuZFwiLCBcImNhc3NhbmRyYXRocmlmdFwiKTtcbiAgICAgIG1hcC5wdXQoXCJzdG9yYWdlLmhvc3RuYW1lXCIsIGNhc3NhbmRyYUhvc3RzKTtcbiAgICAgIG1hcC5wdXQoXCJzdG9yYWdlLnBvcnRcIiwgY2Fzc2FuZHJhUG9ydCk7XG4gICAgICBtYXAucHV0KFwiaW5kZXguc2VhcmNoLmJhY2tlbmRcIiwgXCJlbGFzdGljc2VhcmNoXCIpO1xuICAgICAgbWFwLnB1dChcImluZGV4LnNlYXJjaC5ob3N0bmFtZVwiLCBlbGFzdGljSG9zdHMpO1xuICAgICAgbWFwLnB1dChcImluZGV4LnNlYXJjaC5wb3J0XCIsIGVsYXN0aWNQb3J0KTtcbiAgICAgIG1hcC5wdXQoXCJncmFwaC5ncmFwaG5hbWVcIiwgZ3JhcGhOYW1lKTtcbiAgICAgIENvbmZpZ3VyZWRHcmFwaEZhY3RvcnkuY3JlYXRlQ29uZmlndXJhdGlvbihuZXcgTWFwQ29uZmlndXJhdGlvbihtYXApKTtcbiAgICAgIENvbmZpZ3VyZWRHcmFwaEZhY3Rvcnkub3BlbihncmFwaE5hbWUpLnZlcnRpY2VzKCkuc2l6ZSgpO1xuICAgIGA7XG4gICAgY29uc3QgYmluZGluZ3MgPSB7XG4gICAgICBjYXNzYW5kcmFIb3N0czogJzEyNy4wLjAuMScsXG4gICAgICBjYXNzYW5kcmFQb3J0OiA5MTYwLFxuICAgICAgZWxhc3RpY0hvc3RzOiAnMTI3LjAuMC4xJyxcbiAgICAgIGVsYXN0aWNQb3J0OiA5MjAwLFxuICAgICAgZ3JhcGhOYW1lLFxuICAgIH07XG4gICAgdGhpcy5fY2xpZW50LmV4ZWN1dGUoc2NyaXB0LCBiaW5kaW5ncywgKGVyciwgcmVzdWx0cykgPT4ge1xuICAgICAgaWYgKGVycikge1xuICAgICAgICBjYWxsYmFjayhlcnIpO1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIGNhbGxiYWNrKG51bGwsIHJlc3VsdHMpO1xuICAgIH0pO1xuICB9LFxuXG4gIGNoZWNrX2dyYXBoX2V4aXN0KGdyYXBoTmFtZSwgY2FsbGJhY2spIHtcbiAgICBkZWJ1ZygnY2hlY2sgZm9yIGphbnVzIGdyYXBoOiAlcycsIGdyYXBoTmFtZSk7XG4gICAgY29uc3Qgc2NyaXB0ID0gYFxuICAgICAgQ29uZmlndXJlZEdyYXBoRmFjdG9yeS5nZXRHcmFwaE5hbWVzKCk7XG4gICAgYDtcbiAgICBjb25zdCBiaW5kaW5ncyA9IHt9O1xuICAgIHRoaXMuX2NsaWVudC5leGVjdXRlKHNjcmlwdCwgYmluZGluZ3MsIChlcnIsIHJlc3VsdHMpID0+IHtcbiAgICAgIGlmIChlcnIpIHtcbiAgICAgICAgY2FsbGJhY2soZXJyKTtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuXG4gICAgICBpZiAoXy5pc0FycmF5KHJlc3VsdHMpICYmIHJlc3VsdHMuaW5jbHVkZXMoZ3JhcGhOYW1lKSkge1xuICAgICAgICBjYWxsYmFjayhudWxsLCB0cnVlKTtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuICAgICAgY2FsbGJhY2sobnVsbCwgZmFsc2UpO1xuICAgIH0pO1xuICB9LFxuXG4gIGFzc2VydF9ncmFwaChncmFwaE5hbWUsIGNhbGxiYWNrKSB7XG4gICAgdGhpcy5jaGVja19ncmFwaF9leGlzdChncmFwaE5hbWUsIChlcnIsIGV4aXN0KSA9PiB7XG4gICAgICBpZiAoZXJyKSB7XG4gICAgICAgIGNhbGxiYWNrKGVycik7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgaWYgKCFleGlzdCkge1xuICAgICAgICB0aGlzLmNyZWF0ZV9ncmFwaChncmFwaE5hbWUsIGNhbGxiYWNrKTtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuXG4gICAgICBjYWxsYmFjaygpO1xuICAgIH0pO1xuICB9LFxuXG4gIGRyb3BfZ3JhcGgoZ3JhcGhOYW1lLCBjYWxsYmFjaykge1xuICAgIGRlYnVnKCdyZW1vdmluZyBqYW51cyBncmFwaDogJXMnLCBncmFwaE5hbWUpO1xuICAgIGNvbnN0IHNjcmlwdCA9IGBcbiAgICAgIENvbmZpZ3VyZWRHcmFwaEZhY3RvcnkuZHJvcChncmFwaE5hbWUpO1xuICAgIGA7XG4gICAgY29uc3QgYmluZGluZ3MgPSB7XG4gICAgICBncmFwaE5hbWUsXG4gICAgfTtcbiAgICB0aGlzLl9jbGllbnQuZXhlY3V0ZShzY3JpcHQsIGJpbmRpbmdzLCAoZXJyLCByZXN1bHRzKSA9PiB7XG4gICAgICBpZiAoZXJyKSB7XG4gICAgICAgIGNhbGxiYWNrKGVycik7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgY2FsbGJhY2sobnVsbCwgcmVzdWx0cyk7XG4gICAgfSk7XG4gIH0sXG5cbiAgcHV0X2luZGV4ZXMoZ3JhcGhOYW1lLCBtYXBwaW5nTmFtZSwgaW5kZXhlcywgY2FsbGJhY2spIHtcbiAgICBkZWJ1Zygnc3luY2luZyBqYW51cyBncmFwaCBpbmRleGVzIGZvcjogJXMnLCBtYXBwaW5nTmFtZSk7XG4gICAgbGV0IHNjcmlwdCA9IGBcbiAgICAgIGdyYXBoID0gQ29uZmlndXJlZEdyYXBoRmFjdG9yeS5vcGVuKGdyYXBoTmFtZSk7XG4gICAgICBncmFwaC50eCgpLmNvbW1pdCgpO1xuICAgICAgbWdtdCA9IGdyYXBoLm9wZW5NYW5hZ2VtZW50KCk7XG4gICAgYDtcbiAgICBjb25zdCBiaW5kaW5ncyA9IHtcbiAgICAgIGdyYXBoTmFtZSxcbiAgICB9O1xuICAgIC8vIGNyZWF0ZSBpbmRleGVzIGlmIG5vdCBleGlzdFxuICAgIE9iamVjdC5rZXlzKGluZGV4ZXMpLmZvckVhY2goKGluZGV4KSA9PiB7XG4gICAgICBpZiAoaW5kZXhlc1tpbmRleF0udHlwZSA9PT0gJ0NvbXBvc2l0ZScpIHtcbiAgICAgICAgc2NyaXB0ICs9IGBpZiAoIW1nbXQuY29udGFpbnNHcmFwaEluZGV4KCcke2luZGV4fScpKSBtZ210LmJ1aWxkSW5kZXgoJyR7aW5kZXh9JywgVmVydGV4LmNsYXNzKWA7XG4gICAgICAgIGluZGV4ZXNbaW5kZXhdLmtleXMuZm9yRWFjaCgoa2V5KSA9PiB7XG4gICAgICAgICAgc2NyaXB0ICs9IGAuYWRkS2V5KG1nbXQuZ2V0UHJvcGVydHlLZXkoJyR7a2V5fScpKWA7XG4gICAgICAgIH0pO1xuICAgICAgICBzY3JpcHQgKz0gYC5pbmRleE9ubHkobWdtdC5nZXRWZXJ0ZXhMYWJlbCgnJHttYXBwaW5nTmFtZX0nKSlgO1xuICAgICAgICBpZiAoaW5kZXhlc1tpbmRleF0udW5pcXVlKSB7XG4gICAgICAgICAgc2NyaXB0ICs9ICcudW5pcXVlKCknO1xuICAgICAgICB9XG4gICAgICAgIHNjcmlwdCArPSAnLmJ1aWxkQ29tcG9zaXRlSW5kZXgoKTsnO1xuICAgICAgfSBlbHNlIGlmIChpbmRleGVzW2luZGV4XS50eXBlID09PSAnTWl4ZWQnKSB7XG4gICAgICAgIHNjcmlwdCArPSBgaWYgKCFtZ210LmNvbnRhaW5zR3JhcGhJbmRleCgnJHtpbmRleH0nKSkgbWdtdC5idWlsZEluZGV4KCcke2luZGV4fScsIFZlcnRleC5jbGFzcylgO1xuICAgICAgICBpbmRleGVzW2luZGV4XS5rZXlzLmZvckVhY2goKGtleSkgPT4ge1xuICAgICAgICAgIHNjcmlwdCArPSBgLmFkZEtleShtZ210LmdldFByb3BlcnR5S2V5KCcke2tleX0nKSlgO1xuICAgICAgICB9KTtcbiAgICAgICAgc2NyaXB0ICs9IGAuaW5kZXhPbmx5KG1nbXQuZ2V0VmVydGV4TGFiZWwoJyR7bWFwcGluZ05hbWV9JykpYDtcbiAgICAgICAgaWYgKGluZGV4ZXNbaW5kZXhdLnVuaXF1ZSkge1xuICAgICAgICAgIHNjcmlwdCArPSAnLnVuaXF1ZSgpJztcbiAgICAgICAgfVxuICAgICAgICBzY3JpcHQgKz0gJy5idWlsZE1peGVkSW5kZXgoXCJzZWFyY2hcIik7JztcbiAgICAgIH0gZWxzZSBpZiAoaW5kZXhlc1tpbmRleF0udHlwZSA9PT0gJ1ZlcnRleENlbnRyaWMnKSB7XG4gICAgICAgIHNjcmlwdCArPSBgcmVsYXRpb25MYWJlbCA9IG1nbXQuZ2V0RWRnZUxhYmVsKCcke2luZGV4ZXNbaW5kZXhdLmxhYmVsfScpO2A7XG4gICAgICAgIHNjcmlwdCArPSBgaWYgKCFtZ210LmNvbnRhaW5zUmVsYXRpb25JbmRleChyZWxhdGlvbkxhYmVsLCAnJHtpbmRleH0nKSkgbWdtdC5idWlsZEVkZ2VJbmRleChyZWxhdGlvbkxhYmVsLCAnJHtpbmRleH0nLCBEaXJlY3Rpb24uJHtpbmRleGVzW2luZGV4XS5kaXJlY3Rpb259LCBPcmRlci4ke2luZGV4ZXNbaW5kZXhdLm9yZGVyfWA7XG4gICAgICAgIGluZGV4ZXNbaW5kZXhdLmtleXMuZm9yRWFjaCgoa2V5KSA9PiB7XG4gICAgICAgICAgc2NyaXB0ICs9IGAsIG1nbXQuZ2V0UHJvcGVydHlLZXkoJyR7a2V5fScpYDtcbiAgICAgICAgfSk7XG4gICAgICAgIHNjcmlwdCArPSAnKTsnO1xuICAgICAgfVxuICAgIH0pO1xuICAgIHNjcmlwdCArPSAnbWdtdC5jb21taXQoKTsnO1xuICAgIC8vIGF3YWl0IGluZGV4IGZvciByZWdpc3RlcmVkIG9yIGVuYWJsZWQgc3RhdHVzXG4gICAgT2JqZWN0LmtleXMoaW5kZXhlcykuZm9yRWFjaCgoaW5kZXgpID0+IHtcbiAgICAgIGlmIChpbmRleGVzW2luZGV4XS50eXBlID09PSAnQ29tcG9zaXRlJykge1xuICAgICAgICBzY3JpcHQgKz0gYG1nbXQuYXdhaXRHcmFwaEluZGV4U3RhdHVzKGdyYXBoLCAnJHtpbmRleH0nKS5zdGF0dXMoU2NoZW1hU3RhdHVzLlJFR0lTVEVSRUQsIFNjaGVtYVN0YXR1cy5FTkFCTEVEKS5jYWxsKCk7YDtcbiAgICAgIH0gZWxzZSBpZiAoaW5kZXhlc1tpbmRleF0udHlwZSA9PT0gJ01peGVkJykge1xuICAgICAgICBzY3JpcHQgKz0gYG1nbXQuYXdhaXRHcmFwaEluZGV4U3RhdHVzKGdyYXBoLCAnJHtpbmRleH0nKS5zdGF0dXMoU2NoZW1hU3RhdHVzLlJFR0lTVEVSRUQsIFNjaGVtYVN0YXR1cy5FTkFCTEVEKS5jYWxsKCk7YDtcbiAgICAgIH0gZWxzZSBpZiAoaW5kZXhlc1tpbmRleF0udHlwZSA9PT0gJ1ZlcnRleENlbnRyaWMnKSB7XG4gICAgICAgIHNjcmlwdCArPSBgbWdtdC5hd2FpdFJlbGF0aW9uSW5kZXhTdGF0dXMoZ3JhcGgsICcke2luZGV4fScsICcke2luZGV4ZXNbaW5kZXhdLmxhYmVsfScpLnN0YXR1cyhTY2hlbWFTdGF0dXMuUkVHSVNURVJFRCwgU2NoZW1hU3RhdHVzLkVOQUJMRUQpLmNhbGwoKTtgO1xuICAgICAgfVxuICAgIH0pO1xuICAgIC8vIGVuYWJsZSBpbmRleCBpZiBpbiByZWdpc3RlcmVkIHN0YXRlXG4gICAgc2NyaXB0ICs9ICdtZ210ID0gZ3JhcGgub3Blbk1hbmFnZW1lbnQoKTsnO1xuICAgIE9iamVjdC5rZXlzKGluZGV4ZXMpLmZvckVhY2goKGluZGV4KSA9PiB7XG4gICAgICBpZiAoaW5kZXhlc1tpbmRleF0udHlwZSA9PT0gJ0NvbXBvc2l0ZScpIHtcbiAgICAgICAgc2NyaXB0ICs9IGBpZiAobWdtdC5nZXRHcmFwaEluZGV4KCcke2luZGV4fScpLmdldEluZGV4U3RhdHVzKG1nbXQuZ2V0UHJvcGVydHlLZXkoJyR7aW5kZXhlc1tpbmRleF0ua2V5c1swXX0nKSkuZXF1YWxzKFNjaGVtYVN0YXR1cy5SRUdJU1RFUkVEKSkgbWdtdC51cGRhdGVJbmRleChtZ210LmdldEdyYXBoSW5kZXgoJyR7aW5kZXh9JyksIFNjaGVtYUFjdGlvbi5FTkFCTEVfSU5ERVgpO2A7XG4gICAgICB9IGVsc2UgaWYgKGluZGV4ZXNbaW5kZXhdLnR5cGUgPT09ICdNaXhlZCcpIHtcbiAgICAgICAgc2NyaXB0ICs9IGBpZiAobWdtdC5nZXRHcmFwaEluZGV4KCcke2luZGV4fScpLmdldEluZGV4U3RhdHVzKG1nbXQuZ2V0UHJvcGVydHlLZXkoJyR7aW5kZXhlc1tpbmRleF0ua2V5c1swXX0nKSkuZXF1YWxzKFNjaGVtYVN0YXR1cy5SRUdJU1RFUkVEKSkgbWdtdC51cGRhdGVJbmRleChtZ210LmdldEdyYXBoSW5kZXgoJyR7aW5kZXh9JyksIFNjaGVtYUFjdGlvbi5FTkFCTEVfSU5ERVgpO2A7XG4gICAgICB9IGVsc2UgaWYgKGluZGV4ZXNbaW5kZXhdLnR5cGUgPT09ICdWZXJ0ZXhDZW50cmljJykge1xuICAgICAgICBzY3JpcHQgKz0gYGlmIChtZ210LmdldFJlbGF0aW9uSW5kZXgobWdtdC5nZXRFZGdlTGFiZWwoJyR7aW5kZXhlc1tpbmRleF0ubGFiZWx9JyksICcke2luZGV4fScpLmdldEluZGV4U3RhdHVzKCkuZXF1YWxzKFNjaGVtYVN0YXR1cy5SRUdJU1RFUkVEKSkgbWdtdC51cGRhdGVJbmRleChtZ210LmdldFJlbGF0aW9uSW5kZXgobWdtdC5nZXRFZGdlTGFiZWwoJyR7aW5kZXhlc1tpbmRleF0ubGFiZWx9JyksICcke2luZGV4fScpLCBTY2hlbWFBY3Rpb24uRU5BQkxFX0lOREVYKTtgO1xuICAgICAgfVxuICAgIH0pO1xuICAgIHNjcmlwdCArPSAnbWdtdC5jb21taXQoKTsnO1xuICAgIC8vIGF3YWl0IGluZGV4IGZvciBlbmFibGVkIHN0YXR1c1xuICAgIE9iamVjdC5rZXlzKGluZGV4ZXMpLmZvckVhY2goKGluZGV4KSA9PiB7XG4gICAgICBpZiAoaW5kZXhlc1tpbmRleF0udHlwZSA9PT0gJ0NvbXBvc2l0ZScpIHtcbiAgICAgICAgc2NyaXB0ICs9IGBtZ210LmF3YWl0R3JhcGhJbmRleFN0YXR1cyhncmFwaCwgJyR7aW5kZXh9Jykuc3RhdHVzKFNjaGVtYVN0YXR1cy5FTkFCTEVEKS5jYWxsKCk7YDtcbiAgICAgIH0gZWxzZSBpZiAoaW5kZXhlc1tpbmRleF0udHlwZSA9PT0gJ01peGVkJykge1xuICAgICAgICBzY3JpcHQgKz0gYG1nbXQuYXdhaXRHcmFwaEluZGV4U3RhdHVzKGdyYXBoLCAnJHtpbmRleH0nKS5zdGF0dXMoU2NoZW1hU3RhdHVzLkVOQUJMRUQpLmNhbGwoKTtgO1xuICAgICAgfSBlbHNlIGlmIChpbmRleGVzW2luZGV4XS50eXBlID09PSAnVmVydGV4Q2VudHJpYycpIHtcbiAgICAgICAgc2NyaXB0ICs9IGBtZ210LmF3YWl0UmVsYXRpb25JbmRleFN0YXR1cyhncmFwaCwgJyR7aW5kZXh9JywgJyR7aW5kZXhlc1tpbmRleF0ubGFiZWx9Jykuc3RhdHVzKFNjaGVtYVN0YXR1cy5FTkFCTEVEKS5jYWxsKCk7YDtcbiAgICAgIH1cbiAgICB9KTtcbiAgICB0aGlzLl9jbGllbnQuZXhlY3V0ZShzY3JpcHQsIGJpbmRpbmdzLCAoZXJyLCByZXN1bHRzKSA9PiB7XG4gICAgICBpZiAoZXJyKSB7XG4gICAgICAgIGNhbGxiYWNrKGVycik7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgY2FsbGJhY2sobnVsbCwgcmVzdWx0cyk7XG4gICAgfSk7XG4gIH0sXG5cbiAgcHV0X21hcHBpbmcoZ3JhcGhOYW1lLCBtYXBwaW5nTmFtZSwgbWFwcGluZ0JvZHksIGNhbGxiYWNrKSB7XG4gICAgZGVidWcoJ3N5bmNpbmcgamFudXMgZ3JhcGggbWFwcGluZzogJXMnLCBtYXBwaW5nTmFtZSk7XG4gICAgbGV0IHNjcmlwdCA9IGBcbiAgICAgIGdyYXBoID0gQ29uZmlndXJlZEdyYXBoRmFjdG9yeS5vcGVuKGdyYXBoTmFtZSk7XG4gICAgICBncmFwaC50eCgpLmNvbW1pdCgpO1xuICAgICAgbWdtdCA9IGdyYXBoLm9wZW5NYW5hZ2VtZW50KCk7XG4gICAgICBpZiAoIW1nbXQuY29udGFpbnNWZXJ0ZXhMYWJlbChtYXBwaW5nTmFtZSkpIG1nbXQubWFrZVZlcnRleExhYmVsKG1hcHBpbmdOYW1lKS5tYWtlKCk7XG4gICAgYDtcbiAgICBjb25zdCBiaW5kaW5ncyA9IHtcbiAgICAgIGdyYXBoTmFtZSxcbiAgICAgIG1hcHBpbmdOYW1lLFxuICAgIH07XG4gICAgT2JqZWN0LmtleXMobWFwcGluZ0JvZHkucmVsYXRpb25zKS5mb3JFYWNoKChyZWxhdGlvbikgPT4ge1xuICAgICAgc2NyaXB0ICs9IGBcbiAgICAgICAgaWYgKCFtZ210LmNvbnRhaW5zRWRnZUxhYmVsKCcke3JlbGF0aW9ufScpKSBtZ210Lm1ha2VFZGdlTGFiZWwoJyR7cmVsYXRpb259JykubXVsdGlwbGljaXR5KCR7bWFwcGluZ0JvZHkucmVsYXRpb25zW3JlbGF0aW9uXX0pLm1ha2UoKTtcbiAgICAgIGA7XG4gICAgfSk7XG4gICAgT2JqZWN0LmtleXMobWFwcGluZ0JvZHkucHJvcGVydGllcykuZm9yRWFjaCgocHJvcGVydHkpID0+IHtcbiAgICAgIHNjcmlwdCArPSBgXG4gICAgICAgIGlmICghbWdtdC5jb250YWluc1Byb3BlcnR5S2V5KCcke3Byb3BlcnR5fScpKSBtZ210Lm1ha2VQcm9wZXJ0eUtleSgnJHtwcm9wZXJ0eX0nKS5kYXRhVHlwZSgke21hcHBpbmdCb2R5LnByb3BlcnRpZXNbcHJvcGVydHldLnR5cGV9LmNsYXNzKS5jYXJkaW5hbGl0eShDYXJkaW5hbGl0eS4ke21hcHBpbmdCb2R5LnByb3BlcnRpZXNbcHJvcGVydHldLmNhcmRpbmFsaXR5fSkubWFrZSgpO1xuICAgICAgYDtcbiAgICB9KTtcbiAgICBzY3JpcHQgKz0gJ21nbXQuY29tbWl0KCk7JztcbiAgICB0aGlzLl9jbGllbnQuZXhlY3V0ZShzY3JpcHQsIGJpbmRpbmdzLCAoZXJyLCByZXN1bHRzKSA9PiB7XG4gICAgICBpZiAoZXJyKSB7XG4gICAgICAgIGNhbGxiYWNrKGVycik7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgaWYgKE9iamVjdC5rZXlzKG1hcHBpbmdCb2R5LmluZGV4ZXMpLmxlbmd0aCA+IDApIHtcbiAgICAgICAgdGhpcy5wdXRfaW5kZXhlcyhncmFwaE5hbWUsIG1hcHBpbmdOYW1lLCBtYXBwaW5nQm9keS5pbmRleGVzLCBjYWxsYmFjayk7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgY2FsbGJhY2sobnVsbCwgcmVzdWx0cyk7XG4gICAgfSk7XG4gIH0sXG59O1xuXG5tb2R1bGUuZXhwb3J0cyA9IEphbnVzR3JhcGhCdWlsZGVyO1xuIl19