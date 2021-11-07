/**
无论是客户端还是服务端，对于zk而言二者都是客户端Client的身份，所以第一步要做的就是和zookeeper 集群建立连接

*/

package com.tommy.case1;

import org.apache.zookeeper.*;

import java.io.IOException;

public class DistributeServer {
    private String connectString = "192.168.20.151:2181,192.168.20.152:2181,192.168.20.153:2181";
    private int sessionTime = 20000000;
    ZooKeeper zk = null;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        // 1. get zk connections
        DistributeServer distributeServer = new DistributeServer();
        distributeServer.getConnection();

        // 2. register server --> create node
        distributeServer.register();

        // 3. start transaction (sleep)
        distributeServer.business();

    }

    private void getConnection() throws IOException {
        zk = new ZooKeeper(connectString, sessionTime, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {

            }
        });
    }

    private void register() throws InterruptedException, KeeperException {
        // 创建节点/servers 有序号且永久的节点
        String create = zk.create("/servers", "hadoop101".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("hadoop101" + " is online");
    }

    private void business() throws InterruptedException {
        Thread.sleep(Long.MAX_VALUE);
    }
}
