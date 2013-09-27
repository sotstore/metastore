package org.apache.hadoop.hive.metastore;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.metastore.HiveMetaStore.HMSHandler.MSSessionState;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.ServerContext;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.transport.TTransport;

public class HiveMetaStoreServerEventHandler implements TServerEventHandler {
  public static final Log LOG = LogFactory.getLog(HiveMetaStoreServerEventHandler.class);
  public static final Map<Long, HiveMetaStoreServerContext> sessions = new HashMap<Long, HiveMetaStoreServerContext>();

  public static HiveMetaStoreServerContext getServerContext(Long sessionId) {
    return sessions.get(sessionId);
  }

  @Override
  public void preServe() {
    // TODO Auto-generated method stub

  }

  @Override
  public ServerContext createContext(TProtocol input, TProtocol output) {
    HiveMetaStoreServerContext sc = new HiveMetaStoreServerContext();
    sessions.put(sc.getSessionId(), sc);
    LOG.debug("Receive a new connection, set its sessionId to " + sc.getSessionId());
    return sc;
  }

  @Override
  public void deleteContext(ServerContext serverContext, TProtocol input, TProtocol output) {
    sessions.remove(serverContext);
  }

  @Override
  public void processContext(ServerContext serverContext, TTransport inputTransport,
      TTransport outputTransport) {
    HiveMetaStoreServerContext sc = (HiveMetaStoreServerContext)serverContext;
    // set the session ID to this thread?
    MSSessionState msss = new MSSessionState();
    msss.setSessionid(sc.getSessionId());
  }

}
