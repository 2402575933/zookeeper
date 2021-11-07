/**
  * 监听的是/server节点下面的子节点的data数据
  */  
package com.tommy.case1;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DistributeClientTest {
    // 获取连接的基本信息
    private String connectString = "192.168.20.151:2181,192.168.20.152:2181,192.168.20.153:2181";;
    private int sessionTimeout = 2000000;

    private ZooKeeper zk;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        DistributeClientTest distributeClientTest = new DistributeClientTest();
        // 获取和zk集群的连接
        distributeClientTest.getConnection();
        // 注册监听的核心逻辑
        distributeClientTest.getChildrenList();
        // 业务逻辑，进程睡眠
        distributeClientTest.session();
    }

    public void getConnection() throws IOException {
        zk = new ZooKeeper(connectString, sessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                try {
                    getChildrenList();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (KeeperException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getChildrenList() throws InterruptedException, KeeperException {
        /**
         * 此处注册监听，监听节点/servers下子节点的数据
         */
        List<String> children = zk.getChildren("/servers", true);
        ArrayList<String> datas = new ArrayList<>();
        for (String child : children) {
            System.out.println(child);
            byte[] data = zk.getData("/servers/" + child, false, null);
            datas.add(new String(data));
        }
        System.out.println(datas);
    }

    public void session() throws InterruptedException {
        /**
         * 为防止进程的快速结束而达不到监听的效果，这里使用Long类型的最大值作为进程的睡眠时间
         */
        Thread.sleep(Long.MAX_VALUE);
    }
}
