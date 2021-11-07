# zookeeper
zookeeper，服务器动态上下线监听的快速开发使用

常用命令：`ls、get、create、delete`

Client对zk的操作API：
- 连接：
```java
    public void init() throws IOException {
        zkClient = new ZooKeeper(connectString, sessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
            }
        });
    }
```
- 创建节点，可指定创建的节点的类型：
```java
    public void create() throws InterruptedException, KeeperException {
        String nodeCreated = zkClient.create("/tommys", "tommys".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }
```
- 获取到某路径下的子节点，能在这里进行监听：
```java
    public void getChildren() throws InterruptedException, KeeperException {
    // 第二个参数true代表开启监听，可以在true位置new一个watcher或者在上面的获取连接
    // 处的watcher进行监听，将代码体放入即可进行监听。
        List<String> childs = zkClient.getChildren("/servers", true);
        System.out.println("------------------------------");
        List<String> servers = new ArrayList<>();
        for (String child : childs) {
            byte[] data = zkClient.getData("/servers/" + child, false, null);
            servers.add(new String(data));
        }
        System.out.println(servers);
        System.out.println("------------------------------");
        Thread.sleep(Long.MAX_VALUE);
    }
```
- 判断某节点是否存在：
```java
    public void exist() throws InterruptedException, KeeperException {
        Stat exists = zkClient.exists("/tommy", true);
        System.out.println(exists == null ? "not exists" : "exists");
        Thread.sleep(Long.MAX_VALUE);
    }
```
- `Thread.sleep(Long.MAX_VALUE);` 这个方法用于监听时让程序一直处于执行状态，防止任务过早结束而导致无法继续监听。
- 增强代码健壮性：在连接之后利用：`CountDownLatch connectLatch = new CountDownLatch(1);`的await方法，如果连接不上就进制往下执行，直到连接才释放通道。（可以单独创建一个方法，在构造连接方法下面紧接着调用，然后在监听器中时刻的对连接状态进行监听，如果一旦连接状态成功，立刻释放通道执行下一步）
```java
//....略
private CountDownLatch connectLatch = new CountDownLatch(1);
//....略
    public void getConnection() throws IOException {
        zk = new ZooKeeper(connectString, sessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                // 根据连接状态来判断是否释放通道
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    connectLatch.countDown();
                }
        });
    }
```
