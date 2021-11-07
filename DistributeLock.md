## ZooKeeper分布式锁

所有想要获取到锁的请求都在lock永久节点的下面创建临时的带序号的节点，每个节点代表每个请求，判断当前的请求对应到`/lock`子节点下面的临时带序号的节点的顺序是否排在第一位（最小），是则直接获取到锁，否则进行监听上一个节点，当上一个节点被删除时，下一个进程获取到锁，递归重复上述的操作即可实现分布式锁的原理。
本质上是利用了zookeeper强大的监听功能，结合进程阻塞实现独占锁。

**实现逻辑：**

- 客户端连接zk集群，创建lock节点
- 加锁的逻辑（实际上是创建临时带序号的节点）
- 解锁（节点的删除）

代码实现如下：
```java
package com.tommy.case2;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DistributeLock002 {
// 连接时需要的必要参数
    private final String connectString = "192.168.20.151:2181,192.168.20.152:2181,192.168.20.153:2181";
    private final int sessionTimeout = 2000000;
    ZooKeeper zk;

// 进行阻塞进程的参数
    private CountDownLatch connectLatch = new CountDownLatch(1);
    private CountDownLatch waitLatch = new CountDownLatch(1);
    private String currentNode;
    private int count;
    private int index;
    private String waitPath;

// 建立连接zk集群，并在此处编写监听逻辑
    public DistributeLock002() throws IOException, InterruptedException, KeeperException {
        zk = new ZooKeeper(connectString, sessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    connectLatch.countDown();
                }
                if (watchedEvent.getType() == Event.EventType.NodeDeleted && watchedEvent.getPath().equals(waitPath)) {
                    connectLatch.countDown();
                }
            }
        });
// 如果连接成功则connectLatch被释放，此处不会被阻塞，否则将会被阻塞进程，有助于增强代码的健壮性
        connectLatch.await();
// 查看lock节点是否存在，不存在就创建一个
        if (zk.exists("/locks", false) == null) {
            zk.create("/locks", "locks".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

// 加锁的逻辑代码，实质上就是创建locks的临时带序号的子节点
    public void zkLock() throws InterruptedException, KeeperException {
        currentNode = zk.create("/locks/" + "seq-", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        List<String> children = zk.getChildren("/locks", false, null);
        count = children.size();
        if (count == 1) {
            return;
        } else {
            Collections.sort(children);
            index = children.indexOf(currentNode.substring("/locks/".length()));
            if (index == -1) {
                System.out.println("ERROR");
            } else if (index == 0) {
                return;
            } else {
                waitPath = children.get(index - 1);
                zk.getData("/locks/" + waitPath, true, null);
                waitLatch.await();
                return;
            }
        }
    }

// 释放锁，上一个节点被删除意味着上一个节点独占锁被释放，下一个节点的监听将会获得释放的消息从而得到加锁权限
    public void deleteNode() throws InterruptedException, KeeperException {
        zk.delete(waitPath, -1);
    }
}
```

上述的代码中涉及到的进程阻塞能完美的体现出独占锁的特性，利用监听和阻塞的结合，在监听的逻辑代码中通过判断是否符合下一个节点获得锁的条件从而判断是继续等待还是获取到锁




